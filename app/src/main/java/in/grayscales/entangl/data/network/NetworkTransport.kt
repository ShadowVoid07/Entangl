package `in`.grayscales.entangl.data.network

import `in`.grayscales.entangl.core.crypto.KeyPairGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Data packet transported across online devices.
 * Contains only encrypted ciphertext — no plaintext or keys are exposed to the transport relay.
 */
@OptIn(ExperimentalEncodingApi::class)
data class TransportEnvelope(
    val id: String,
    val type: String = TYPE_MESSAGE, // "MESSAGE", "DELIVERY_ACK", "SCAN_PING", "SCAN_ACCEPT"
    val senderUid: String,
    val senderIdentityPub: ByteArray,
    val senderOnion: String,
    val recipientUid: String,
    val ciphertext: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val senderUsername: String = "",
    val senderProfileColor: String = "",
    val signature: ByteArray = ByteArray(0)
) {
    /**
     * Canonical binary representation of envelope data for cryptographic signature verification.
     * Prevents forgery of headers, sender/recipient IDs, or tampering with the ciphertext.
     */
    fun getCanonicalData(): ByteArray {
        val timestampBytes = ByteArray(8) { i -> (timestamp ushr (56 - i * 8)).toByte() }
        return id.encodeToByteArray() +
            type.encodeToByteArray() +
            senderUid.encodeToByteArray() +
            recipientUid.encodeToByteArray() +
            timestampBytes +
            ciphertext
    }

    companion object {
        const val TYPE_MESSAGE = "MESSAGE"
        const val TYPE_DELIVERY_ACK = "DELIVERY_ACK"
        const val TYPE_SCAN_PING = "SCAN_PING"
        const val TYPE_SCAN_ACCEPT = "SCAN_ACCEPT"
        const val TYPE_IDENTITY_ROTATION = "IDENTITY_ROTATION"
        const val FIXED_PAYLOAD_SIZE = 4096

        fun padPayload(data: ByteArray, targetSize: Int = FIXED_PAYLOAD_SIZE): ByteArray {
            if (data.size + 4 > targetSize) {
                val totalLen = ((data.size + 4 + (targetSize - 1)) / targetSize) * targetSize
                val padded = ByteArray(totalLen)
                val len = data.size
                padded[0] = (len ushr 24).toByte()
                padded[1] = (len ushr 16).toByte()
                padded[2] = (len ushr 8).toByte()
                padded[3] = len.toByte()
                System.arraycopy(data, 0, padded, 4, len)
                return padded
            }
            val padded = ByteArray(targetSize)
            val len = data.size
            padded[0] = (len ushr 24).toByte()
            padded[1] = (len ushr 16).toByte()
            padded[2] = (len ushr 8).toByte()
            padded[3] = len.toByte()
            System.arraycopy(data, 0, padded, 4, len)
            return padded
        }

        fun unpadPayload(padded: ByteArray): ByteArray {
            if (padded.size < 4) return padded
            val len = ((padded[0].toInt() and 0xFF) shl 24) or
                      ((padded[1].toInt() and 0xFF) shl 16) or
                      ((padded[2].toInt() and 0xFF) shl 8) or
                      (padded[3].toInt() and 0xFF)
            if (len in 0..(padded.size - 4)) {
                val data = ByteArray(len)
                System.arraycopy(padded, 4, data, 0, len)
                return data
            }
            return padded
        }

        fun fromJson(jsonStr: String): TransportEnvelope? {
            return try {
                val id = extractJsonField(jsonStr, "id") ?: return null
                val recipientUid = extractJsonField(jsonStr, "recipientUid") ?: return null
                val type = extractJsonField(jsonStr, "type") ?: TYPE_MESSAGE
                val senderUid = extractJsonField(jsonStr, "senderUid") ?: ""
                val senderUsername = extractJsonField(jsonStr, "senderUsername") ?: ""
                val senderProfileColor = extractJsonField(jsonStr, "senderProfileColor") ?: ""
                val senderOnion = extractJsonField(jsonStr, "senderOnion") ?: ""
                val pubStr = extractJsonField(jsonStr, "senderPub") ?: ""
                val pubBytes = if (pubStr.isNotEmpty()) Base64.decode(pubStr) else ByteArray(0)
                val cipherStr = extractJsonField(jsonStr, "ciphertext") ?: ""
                val cipherBytes = if (cipherStr.isNotEmpty()) Base64.decode(cipherStr) else ByteArray(0)
                val sigStr = extractJsonField(jsonStr, "senderSig") ?: ""
                val sigBytes = if (sigStr.isNotEmpty()) Base64.decode(sigStr) else ByteArray(0)
                val timestamp = extractJsonLong(jsonStr, "timestamp") ?: System.currentTimeMillis()

                TransportEnvelope(
                    id = id,
                    type = type,
                    senderUid = senderUid,
                    senderIdentityPub = pubBytes,
                    senderOnion = senderOnion,
                    recipientUid = recipientUid,
                    ciphertext = cipherBytes,
                    timestamp = timestamp,
                    senderUsername = senderUsername,
                    senderProfileColor = senderProfileColor,
                    signature = sigBytes
                )
            } catch (_: Exception) {
                null
            }
        }

        fun extractJsonField(json: String, field: String): String? {
            val pattern = "\"$field\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"".toRegex()
            val match = pattern.find(json) ?: return null
            return unescapeJson(match.groupValues[1])
        }

        fun extractJsonLong(json: String, field: String): Long? {
            val pattern = "\"$field\"\\s*:\\s*(\\d+)".toRegex()
            val match = pattern.find(json) ?: return null
            return match.groupValues[1].toLongOrNull()
        }

        private fun escapeJson(s: String): String = buildString {
            for (c in s) {
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
        }

        private fun unescapeJson(s: String): String = buildString {
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    when (val next = s[i + 1]) {
                        '"' -> append('"')
                        '\\' -> append('\\')
                        '/' -> append('/')
                        'b' -> append('\b')
                        'f' -> append('\u000C')
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        else -> append(next)
                    }
                    i += 2
                } else {
                    append(c)
                    i++
                }
            }
        }
    }

    fun toJson(): String {
        return buildString {
            append("{")
            append("\"id\":\"").append(escapeJson(id)).append("\",")
            append("\"type\":\"").append(escapeJson(type)).append("\",")
            append("\"senderUid\":\"").append(escapeJson(senderUid)).append("\",")
            append("\"senderUsername\":\"").append(escapeJson(senderUsername)).append("\",")
            append("\"senderProfileColor\":\"").append(escapeJson(senderProfileColor)).append("\",")
            append("\"senderPub\":\"").append(Base64.encode(senderIdentityPub)).append("\",")
            append("\"senderOnion\":\"").append(escapeJson(senderOnion)).append("\",")
            append("\"recipientUid\":\"").append(escapeJson(recipientUid)).append("\",")
            append("\"ciphertext\":\"").append(Base64.encode(ciphertext)).append("\",")
            append("\"senderSig\":\"").append(Base64.encode(signature)).append("\",")
            append("\"timestamp\":").append(timestamp)
            append("}")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransportEnvelope

        if (id != other.id) return false
        if (type != other.type) return false
        if (senderUid != other.senderUid) return false
        if (senderUsername != other.senderUsername) return false
        if (senderProfileColor != other.senderProfileColor) return false
        if (!senderIdentityPub.contentEquals(other.senderIdentityPub)) return false
        if (senderOnion != other.senderOnion) return false
        if (recipientUid != other.recipientUid) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!signature.contentEquals(other.signature)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + senderUid.hashCode()
        result = 31 * result + senderUsername.hashCode()
        result = 31 * result + senderProfileColor.hashCode()
        result = 31 * result + senderIdentityPub.contentHashCode()
        result = 31 * result + senderOnion.hashCode()
        result = 31 * result + recipientUid.hashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + signature.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Real-time, zero-knowledge network transport for Entangl.
 * Relays Double-Ratchet encrypted payloads across devices on cellular or Wi-Fi networks
 * using an anonymous pub-sub inbox topic.
 */
class NetworkTransport(
    private val keyPairGenerator: KeyPairGenerator? = null
) {

    /**
     * Signs an envelope with the device's Keystore identity key if it is not already signed.
     */
    fun signEnvelope(envelope: TransportEnvelope): TransportEnvelope {
        val kpg = keyPairGenerator ?: return envelope
        val identityPub = if (envelope.senderIdentityPub.isNotEmpty()) {
            envelope.senderIdentityPub
        } else {
            kpg.getStoredIdentityPublicKey() ?: ByteArray(0)
        }
        val target = if (envelope.senderIdentityPub.isEmpty()) envelope.copy(senderIdentityPub = identityPub) else envelope
        val canonical = target.getCanonicalData()
        val sig = kpg.sign(canonical)
        return target.copy(signature = sig)
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val listeningJobs = mutableListOf<Job>()
    private var isRunning = false

    private val listeners = mutableListOf<suspend (TransportEnvelope) -> Unit>()
    private val ackListeners = mutableListOf<suspend (messageId: String) -> Unit>()

    // Deduplication: track recently processed message IDs to prevent
    // duplicate decryption, notifications, and ACKs from overlapping poll windows.
    private val processedIds = java.util.Collections.synchronizedSet(
        java.util.LinkedHashSet<String>()
    )

    @Volatile
    private var currentRelayIndex = 0

    val activeRelay: String
        get() = RELAY_SERVERS[currentRelayIndex.coerceIn(0, RELAY_SERVERS.lastIndex)]

    fun addListener(listener: suspend (TransportEnvelope) -> Unit) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun addAckListener(listener: suspend (messageId: String) -> Unit) {
        synchronized(ackListeners) {
            ackListeners.add(listener)
        }
    }

    /**
     * Start background subscription to the local device's encrypted inbox topic.
     * Uses an active primary relay with automatic failover across the relay pool and
     * exponential backoff with jitter on connection drops to conserve radio power and battery.
     */
    fun startListening(localUid: String) {
        if (isRunning) return
        isRunning = true
        val topic = getTopicForUid(localUid)
        log("Starting NetworkTransport battery-optimized listener for UID $localUid on topic $topic")

        synchronized(listeningJobs) {
            listeningJobs.clear()
            val job = scope.launch {
                var backoffCount = 0
                while (isActive && isRunning) {
                    val relay = RELAY_SERVERS[currentRelayIndex % RELAY_SERVERS.size]
                    val streamCompletedCleanly = try {
                        streamInbox(relay, topic, localUid)
                    } catch (e: Exception) {
                        log("Transport stream encounter on $relay: ${e.message}")
                        false
                    }

                    if (!isActive || !isRunning) break

                    if (streamCompletedCleanly) {
                        backoffCount = 0
                    } else {
                        backoffCount++
                        currentRelayIndex = (currentRelayIndex + 1) % RELAY_SERVERS.size
                        log("Failing over to relay ${activeRelay} (attempt $backoffCount)")
                    }

                    // Battery optimization: Exponential backoff with jitter on errors
                    // Prevents keeping cellular radio in high-power state when disconnected or server is down
                    val delayMs = if (backoffCount == 0) {
                        1500L
                    } else {
                        val base = (2500L * (1L shl (backoffCount - 1).coerceAtMost(5))).coerceAtMost(60_000L)
                        val jitter = kotlin.random.Random.nextLong(0, 1000)
                        base + jitter
                    }
                    delay(delayMs)
                }
            }
            listeningJobs.add(job)
        }
    }

    fun stopListening() {
        isRunning = false
        synchronized(listeningJobs) {
            listeningJobs.forEach { it.cancel() }
            listeningJobs.clear()
        }
    }

    fun restartListening(localUid: String) {
        log("Restarting NetworkTransport listeners...")
        stopListening()
        startListening(localUid)
    }

    /**
     * Transmit an encrypted envelope to the recipient's anonymous inbox topic.
     * Concurrently broadcasts (multi-casts) the envelope across all configured relays
     * to eliminate cross-relay partition / split-brain.
     */
    suspend fun sendEnvelope(envelope: TransportEnvelope): Boolean = withContext(Dispatchers.IO) {
        val signedEnvelope = if (envelope.signature.isEmpty()) signEnvelope(envelope) else envelope
        val topic = getTopicForUid(signedEnvelope.recipientUid)
        val payloadJson = signedEnvelope.toJson()

        log("Broadcasting envelope ${envelope.id} (type=${envelope.type}) to ${RELAY_SERVERS.size} relays for topic $topic")

        val results = coroutineScope {
            RELAY_SERVERS.map { relay ->
                async {
                    postToRelay(relay, topic, payloadJson, envelope.id, envelope.type)
                }
            }.awaitAll()
        }

        val successCount = results.count { it }
        log("Transmission summary for ${envelope.id}: $successCount/${RELAY_SERVERS.size} relays succeeded")
        successCount > 0
    }

    private fun postToRelay(
        relay: String,
        topic: String,
        payloadJson: String,
        envelopeId: String,
        envelopeType: String
    ): Boolean {
        val url = URL("$relay/$topic")
        var conn: HttpURLConnection? = null
        return try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Title", "entangl")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payloadJson)
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                log("Successfully sent envelope $envelopeId ($envelopeType) to $relay/$topic (HTTP $code)")
                true
            } else {
                log("Relay $relay returned HTTP $code for envelope $envelopeId")
                false
            }
        } catch (e: Exception) {
            log("Failed sending envelope $envelopeId to $relay: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Send a delivery acknowledgment back to the original sender.
     */
    suspend fun sendDeliveryAck(messageId: String, senderUid: String, localUid: String) {
        val ackEnvelope = TransportEnvelope(
            id = messageId,
            type = TransportEnvelope.TYPE_DELIVERY_ACK,
            senderUid = localUid,
            senderIdentityPub = ByteArray(0),
            senderOnion = "",
            recipientUid = senderUid,
            ciphertext = ByteArray(0)
        )
        sendEnvelope(ackEnvelope)
    }

    /**
     * Send a network ping to peer informing them that their QR code was scanned.
     */
    suspend fun sendScanPing(
        recipientUid: String,
        localUid: String,
        localUsername: String,
        localIdentityPub: ByteArray,
        localOnion: String,
        localProfileColor: String = ""
    ): Boolean {
        val envelope = TransportEnvelope(
            id = java.util.UUID.randomUUID().toString(),
            type = TransportEnvelope.TYPE_SCAN_PING,
            senderUid = localUid,
            senderUsername = localUsername,
            senderProfileColor = localProfileColor,
            senderIdentityPub = localIdentityPub,
            senderOnion = localOnion,
            recipientUid = recipientUid,
            ciphertext = ByteArray(0)
        )
        return sendEnvelope(envelope)
    }

    /**
     * Send an acceptance ping back to the peer acknowledging their connection request.
     */
    suspend fun sendScanAccept(
        recipientUid: String,
        localUid: String,
        localUsername: String,
        localIdentityPub: ByteArray = ByteArray(0),
        localOnion: String = "",
        localProfileColor: String = ""
    ): Boolean {
        val envelope = TransportEnvelope(
            id = java.util.UUID.randomUUID().toString(),
            type = TransportEnvelope.TYPE_SCAN_ACCEPT,
            senderUid = localUid,
            senderUsername = localUsername,
            senderProfileColor = localProfileColor,
            senderIdentityPub = localIdentityPub,
            senderOnion = localOnion,
            recipientUid = recipientUid,
            ciphertext = ByteArray(0)
        )
        return sendEnvelope(envelope)
    }

    private suspend fun streamInbox(relay: String, topic: String, localUid: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("$relay/$topic/json?since=10m")
        var conn: HttpURLConnection? = null
        var streamedAnyData = false

        try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 90000 // 90s read timeout; ntfy keepalives arrive every 15-30s
            }

            val code = conn.responseCode
            if (code in 200..299) {
                log("Connected stream to $relay/$topic")
                BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                    while (isActive && isRunning) {
                        val line = reader.readLine() ?: break
                        streamedAnyData = true
                        val current = line.trim()
                        if (current.isEmpty()) continue
                        try {
                            val messageContent = TransportEnvelope.extractJsonField(current, "message") ?: ""
                            if (messageContent.isNotEmpty()) {
                                val envelope = TransportEnvelope.fromJson(messageContent)
                                if (envelope != null && envelope.recipientUid.trim().equals(localUid.trim(), ignoreCase = true)) {
                                    dispatchEnvelope(envelope)
                                }
                            }
                        } catch (e: Exception) {
                            log("Error parsing stream message from $relay: ${e.message}")
                        }
                    }
                }
                streamedAnyData
            } else {
                log("Stream connect to $relay/$topic failed with HTTP $code")
                false
            }
        } catch (e: Exception) {
            log("Stream disconnect/error on $relay: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun dispatchEnvelope(envelope: TransportEnvelope) {
        // Deduplication: skip if we've already processed this envelope ID + type
        val deduplicationKey = "${envelope.type}:${envelope.id}"
        if (!processedIds.add(deduplicationKey)) {
            log("Skipping duplicate envelope: $deduplicationKey")
            return
        }
        // Cap the set size to prevent unbounded memory growth
        if (processedIds.size > MAX_PROCESSED_IDS) {
            synchronized(processedIds) {
                val iterator = processedIds.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }

        log("Dispatched incoming envelope: id=${envelope.id}, type=${envelope.type}, from=${envelope.senderUid}")

        if (envelope.type == TransportEnvelope.TYPE_DELIVERY_ACK) {
            val callbacks = synchronized(ackListeners) { ackListeners.toList() }
            for (cb in callbacks) {
                try {
                    cb(envelope.id)
                } catch (e: Exception) {
                    log("Error in ACK callback: ${e.message}")
                }
            }
        } else {
            val callbacks = synchronized(listeners) { listeners.toList() }
            for (cb in callbacks) {
                try {
                    cb(envelope)
                } catch (e: Exception) {
                    log("Error in envelope callback: ${e.message}")
                }
            }
        }
    }

    fun getTopicForUid(uid: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("Entangl-Topic-Blind-v2".toByteArray())
        val hash = digest.digest(uid.toByteArray())
        val hex = hash.joinToString("") { "%02x".format(it) }
        return "entangl-v2-" + hex.take(24)
    }

    private fun log(msg: String) {
        if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
            try {
                android.util.Log.d("NetworkTransport", msg)
            } catch (_: Throwable) {
                // Silently swallow in tests/non-android environments
            }
        }
    }

    companion object {
        val RELAY_SERVERS = listOf(
            "https://ntfy.tedomum.fr",
            "https://ntfy.envs.net",
            "https://ntfy.adminforge.de"
        )
        private const val MAX_PROCESSED_IDS = 500
    }
}
