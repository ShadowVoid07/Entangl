package `in`.grayscales.entangl.data.network

import `in`.grayscales.entangl.core.crypto.KeyPairGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    val signature: ByteArray = ByteArray(0),
    /** Ephemeral TTL: remaining milliseconds until vaporization, null = keeps. */
    val ttlMs: Long? = null
) {
    /**
     * Canonical binary representation of envelope data for cryptographic signature verification.
     * Prevents forgery of headers, sender/recipient IDs, TTL, or tampering with the ciphertext.
     */
    fun getCanonicalData(): ByteArray {
        val timestampBytes = ByteArray(8) { i -> (timestamp ushr (56 - i * 8)).toByte() }
        val ttl = ttlMs ?: -1L
        val ttlBytes = ByteArray(8) { i -> (ttl ushr (56 - i * 8)).toByte() }
        return id.encodeToByteArray() +
            type.encodeToByteArray() +
            senderUid.encodeToByteArray() +
            recipientUid.encodeToByteArray() +
            timestampBytes +
            ttlBytes +
            ciphertext
    }

    companion object {
        const val TYPE_MESSAGE = "MESSAGE"
        const val TYPE_DELIVERY_ACK = "DELIVERY_ACK"
        const val TYPE_SCAN_PING = "SCAN_PING"
        const val TYPE_SCAN_ACCEPT = "SCAN_ACCEPT"
        const val TYPE_IDENTITY_ROTATION = "IDENTITY_ROTATION"
        const val FIXED_PAYLOAD_SIZE = 2048

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
                val ttlMs = extractJsonLong(jsonStr, "ttlMs")

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
                    signature = sigBytes,
                    ttlMs = ttlMs
                )
            } catch (_: Exception) {
                null
            }
        }

        fun extractJsonField(json: String, field: String): String? {
            val key = "\"$field\""
            var keyIdx = json.indexOf(key)
            while (keyIdx != -1) {
                var colonIdx = keyIdx + key.length
                while (colonIdx < json.length && json[colonIdx].isWhitespace()) {
                    colonIdx++
                }
                if (colonIdx < json.length && json[colonIdx] == ':') {
                    var valStart = colonIdx + 1
                    while (valStart < json.length && json[valStart].isWhitespace()) {
                        valStart++
                    }
                    if (valStart < json.length && json[valStart] == '"') {
                        var curr = valStart + 1
                        val sb = StringBuilder()
                        var escaped = false
                        while (curr < json.length) {
                            val c = json[curr]
                            if (escaped) {
                                when (c) {
                                    '"' -> sb.append('"')
                                    '\\' -> sb.append('\\')
                                    '/' -> sb.append('/')
                                    'b' -> sb.append('\b')
                                    'f' -> sb.append('\u000C')
                                    'n' -> sb.append('\n')
                                    'r' -> sb.append('\r')
                                    't' -> sb.append('\t')
                                    else -> sb.append(c)
                                }
                                escaped = false
                            } else if (c == '\\') {
                                escaped = true
                            } else if (c == '"') {
                                return sb.toString()
                            } else {
                                sb.append(c)
                            }
                            curr++
                        }
                    }
                }
                keyIdx = json.indexOf(key, keyIdx + 1)
            }
            return null
        }

        fun extractJsonLong(json: String, field: String): Long? {
            val key = "\"$field\""
            var keyIdx = json.indexOf(key)
            while (keyIdx != -1) {
                var colonIdx = keyIdx + key.length
                while (colonIdx < json.length && json[colonIdx].isWhitespace()) {
                    colonIdx++
                }
                if (colonIdx < json.length && json[colonIdx] == ':') {
                    var valStart = colonIdx + 1
                    while (valStart < json.length && json[valStart].isWhitespace()) {
                        valStart++
                    }
                    var valEnd = valStart
                    while (valEnd < json.length && (json[valEnd].isDigit() || json[valEnd] == '-')) {
                        valEnd++
                    }
                    if (valEnd > valStart) {
                        return json.substring(valStart, valEnd).toLongOrNull()
                    }
                }
                keyIdx = json.indexOf(key, keyIdx + 1)
            }
            return null
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
            if (ttlMs != null) {
                append(",\"ttlMs\":").append(ttlMs)
            }
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
        if (ttlMs != other.ttlMs) return false

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
        result = 31 * result + (ttlMs?.hashCode() ?: 0)
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
        val storedKey = kpg.getStoredIdentityPublicKey() ?: kpg.generateIdentityKeyPair()
        // If envelope already has a valid EC identity key (size != 32 bytes for raw X25519), keep it; otherwise use Keystore identity key
        val identityPub = if (envelope.senderIdentityPub.isNotEmpty() && envelope.senderIdentityPub.size != 32) {
            envelope.senderIdentityPub
        } else {
            storedKey
        }
        val target = envelope.copy(senderIdentityPub = identityPub)
        val canonical = target.getCanonicalData()
        val sig = kpg.sign(canonical)
        return target.copy(signature = sig)
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val listeningJobs = mutableListOf<Job>()
    @Volatile
    private var _isRunning = false

    val isRunning: Boolean
        get() = _isRunning

    private val listeners = mutableListOf<suspend (TransportEnvelope) -> Unit>()
    private val ackListeners = mutableListOf<suspend (messageId: String) -> Unit>()

    // Deduplication: track recently processed message IDs to prevent
    // duplicate decryption, notifications, and ACKs from overlapping poll windows.
    private val processedIds = java.util.Collections.synchronizedSet(
        java.util.LinkedHashSet<String>()
    )

    @Volatile
    private var currentRelayIndex = 0

    @Volatile
    private var _isForeground = true

    val isForeground: Boolean
        get() = _isForeground

    private val wakeSignal = Channel<Unit>(Channel.CONFLATED)

    @Volatile
    private var currentStreamConn: HttpURLConnection? = null

    val activeRelay: String
        get() = RELAY_SERVERS[currentRelayIndex.coerceIn(0, RELAY_SERVERS.lastIndex)]

    /**
     * Updates foreground/background lifecycle state.
     * When backgrounded, the persistent SSE stream is closed so the transport switches to
     * coalesced burst polling, allowing the cellular baseband to drop into low-power idle/DRX (~10-20mA).
     * When returning to foreground, wakeSignal is triggered to instantly connect persistent streaming.
     */
    fun setForeground(inForeground: Boolean) {
        val changed = _isForeground != inForeground
        _isForeground = inForeground
        if (changed) {
            log("NetworkTransport lifecycle state changed: isForeground=$inForeground")
            if (inForeground) {
                wakeSignal.trySend(Unit)
            } else {
                try {
                    currentStreamConn?.disconnect()
                } catch (_: Exception) {}
            }
        }
    }

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
     * Start subscription to the local device's encrypted inbox topic.
     * In foreground: uses persistent SSE streaming for instant (<100ms) message delivery.
     * In background: switches to coalesced burst polling with 30s duty-cycling to conserve cellular radio battery.
     */
    fun startListening(localUid: String) {
        if (_isRunning) return
        _isRunning = true
        val topic = getTopicForUid(localUid)
        log("Starting NetworkTransport battery-optimized listener for UID $localUid on topic $topic")

        synchronized(listeningJobs) {
            listeningJobs.clear()
            val job = scope.launch {
                var backoffCount = 0
                while (isActive && _isRunning) {
                    val inForeground = _isForeground
                    val relay = RELAY_SERVERS[currentRelayIndex % RELAY_SERVERS.size]
                    val fetchSuccess = try {
                        streamInbox(relay, topic, localUid, isStreaming = inForeground)
                    } catch (e: Exception) {
                        log("Transport stream encounter on $relay: ${e.message}")
                        false
                    }

                    if (!isActive || !_isRunning) break

                    if (fetchSuccess) {
                        backoffCount = 0
                    } else if (inForeground) {
                        backoffCount++
                        currentRelayIndex = (currentRelayIndex + 1) % RELAY_SERVERS.size
                        log("Failing over to relay ${activeRelay} (attempt $backoffCount)")
                    }

                    // Battery optimization:
                    // If in foreground: reconnect after short delay (or exponential backoff on error)
                    // If in background: enter low-power sleep for 30s burst interval, or wake immediately on wakeSignal
                    if (inForeground) {
                        val delayMs = if (backoffCount == 0) {
                            1500L
                        } else {
                            val base = (2500L * (1L shl (backoffCount - 1).coerceAtMost(5))).coerceAtMost(60_000L)
                            val jitter = kotlin.random.Random.nextLong(0, 1000)
                            base + jitter
                        }
                        delay(delayMs)
                    } else {
                        // Radio duty-cycling: wait up to 30s or wake instantly if user returns to foreground
                        withTimeoutOrNull(30_000L) {
                            wakeSignal.receive()
                        }
                    }
                }
            }
            listeningJobs.add(job)
        }
    }

    fun stopListening() {
        _isRunning = false
        try {
            currentStreamConn?.disconnect()
        } catch (_: Exception) {}
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
        var inStream: java.io.InputStream? = null
        var errStream: java.io.InputStream? = null
        var outStream: java.io.OutputStream? = null
        return try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                setRequestProperty("Title", "entangl")
            }

            outStream = conn.outputStream
            OutputStreamWriter(outStream, "UTF-8").use { writer ->
                writer.write(payloadJson)
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                inStream = conn.inputStream
                log("Successfully sent envelope $envelopeId ($envelopeType) to $relay/$topic (HTTP $code)")
                true
            } else {
                errStream = conn.errorStream
                log("Relay $relay returned HTTP $code for envelope $envelopeId")
                false
            }
        } catch (e: Exception) {
            log("Failed sending envelope $envelopeId to $relay: ${e.message}")
            false
        } finally {
            try { outStream?.close() } catch (_: Exception) {}
            try { inStream?.close() } catch (_: Exception) {}
            try { errStream?.close() } catch (_: Exception) {}
            try { conn?.inputStream?.close() } catch (_: Exception) {}
            try { conn?.errorStream?.close() } catch (_: Exception) {}
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    /**
     * Send a delivery acknowledgment back to the original sender.
     */
    suspend fun sendDeliveryAck(messageId: String, senderUid: String, localUid: String) {
        val identityPub = keyPairGenerator?.getStoredIdentityPublicKey()
            ?: keyPairGenerator?.generateIdentityKeyPair()
            ?: ByteArray(0)
        val ackEnvelope = TransportEnvelope(
            id = messageId,
            type = TransportEnvelope.TYPE_DELIVERY_ACK,
            senderUid = localUid,
            senderIdentityPub = identityPub,
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

    private suspend fun streamInbox(
        relay: String,
        topic: String,
        localUid: String,
        isStreaming: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val url = if (isStreaming) {
            URL("$relay/$topic/json?since=24h")
        } else {
            // Coalesced burst poll: fetches buffered messages in one quick round-trip and disconnects immediately
            URL("$relay/$topic/json?poll=1&since=24h")
        }
        var conn: HttpURLConnection? = null
        var inStream: java.io.InputStream? = null
        var errStream: java.io.InputStream? = null

        try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = if (isStreaming) 90000 else 10000
            }
            if (isStreaming) {
                currentStreamConn = conn
            }

            val code = conn.responseCode
            if (code in 200..299) {
                log("Connected ${if (isStreaming) "stream" else "burst-poll"} to $relay/$topic")
                inStream = conn.inputStream
                BufferedReader(InputStreamReader(inStream, "UTF-8")).use { reader ->
                    while (isActive && _isRunning) {
                        val line = reader.readLine() ?: break
                        val current = line.trim()
                        if (current.isEmpty()) continue
                        try {
                            val messageContent = TransportEnvelope.extractJsonField(current, "message") ?: ""
                            val envelope = if (messageContent.isNotEmpty()) {
                                TransportEnvelope.fromJson(messageContent)
                            } else {
                                TransportEnvelope.fromJson(current)
                            } ?: TransportEnvelope.fromJson(current)

                            if (envelope != null && envelope.recipientUid.trim().equals(localUid.trim(), ignoreCase = true)) {
                                dispatchEnvelope(envelope)
                            }
                        } catch (e: Exception) {
                            log("Error parsing stream message from $relay: ${e.message}")
                        }
                    }
                }
                true
            } else {
                errStream = conn.errorStream
                log("${if (isStreaming) "Stream" else "Poll"} connect to $relay/$topic failed with HTTP $code")
                false
            }
        } catch (e: Exception) {
            log("Stream/poll encounter on $relay: ${e.message}")
            false
        } finally {
            if (isStreaming && currentStreamConn == conn) {
                currentStreamConn = null
            }
            try { inStream?.close() } catch (_: Exception) {}
            try { errStream?.close() } catch (_: Exception) {}
            try { conn?.inputStream?.close() } catch (_: Exception) {}
            try { conn?.errorStream?.close() } catch (_: Exception) {}
            try { conn?.disconnect() } catch (_: Exception) {}
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

        // ACKs fan out to BOTH paths: legacy ack callbacks (kept for API compat)
        // and normal listeners so MessageRepositoryImpl can signature-verify the
        // envelope and confirm the message is really ours before marking DELIVERED.
        // Previously ACKs bypassed all verification — any relay observer could forge
        // DELIVERED receipts with a bare message id.
        if (envelope.type == TransportEnvelope.TYPE_DELIVERY_ACK) {
            val ackCallbacks = synchronized(ackListeners) { ackListeners.toList() }
            for (cb in ackCallbacks) {
                try {
                    cb(envelope.id)
                } catch (e: Exception) {
                    log("Error in ACK callback: ${e.message}")
                }
            }
        }
        val callbacks = synchronized(listeners) { listeners.toList() }
        for (cb in callbacks) {
            try {
                cb(envelope)
            } catch (e: Exception) {
                log("Error in envelope callback: ${e.message}")
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
            "https://ntfy.sh",
            "https://ntfy.tedomum.fr",
            "https://ntfy.envs.net",
            "https://ntfy.adminforge.de"
        )
        private const val MAX_PROCESSED_IDS = 500
    }
}
