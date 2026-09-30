package `in`.grayscales.entangl.data.repository

import android.util.Log
import `in`.grayscales.entangl.core.crypto.CryptoManager
import `in`.grayscales.entangl.core.crypto.KeyPairGenerator
import `in`.grayscales.entangl.core.crypto.SuccessionCertificate
import `in`.grayscales.entangl.core.identity.NodeIdentityManager
import `in`.grayscales.entangl.data.local.dao.ContactDao
import `in`.grayscales.entangl.data.local.dao.MessageDao
import `in`.grayscales.entangl.data.local.entity.ContactEntity
import `in`.grayscales.entangl.data.local.entity.MessageEntity
import `in`.grayscales.entangl.data.network.NetworkTransport
import `in`.grayscales.entangl.data.network.TransportEnvelope
import `in`.grayscales.entangl.data.notification.EntanglNotificationManager
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.domain.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * Android implementation of [MessageRepository].
 * Enforces double encryption: messages are Double Ratchet encrypted in-memory
 * before being written to the SQLCipher database.
 * Transmits and receives encrypted payloads via [NetworkTransport] across online devices.
 */
class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val cryptoManager: CryptoManager,
    private val networkTransport: NetworkTransport,
    private val nodeIdentityManager: NodeIdentityManager,
    private val notificationManager: EntanglNotificationManager,
    private val keyPairGenerator: KeyPairGenerator? = null
) : MessageRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val decryptedCache = ConcurrentHashMap<String, String>()

    init {
        // 1. Listen for Delivery ACKs
        networkTransport.addAckListener { messageId ->
            Log.i("MessageRepository", "Delivery ACK received for message $messageId")
            messageDao.updateStatus(messageId, 2) // 2 = DELIVERED
        }

        // 2. Listen for Incoming Messages
        networkTransport.addListener { envelope ->
            handleIncomingEnvelope(envelope)
        }

        // 3. Start listening on persistent local inbox topic
        networkTransport.startListening(nodeIdentityManager.localUid)

        // 4. Retry any failed outgoing messages
        scope.launch {
            retryFailedMessages()
        }
    }

    private suspend fun handleIncomingEnvelope(envelope: TransportEnvelope) {
        val senderUid = envelope.senderUid
        if (senderUid.isBlank()) return

        // Anti-replay: reject envelopes with timestamps more than 10 minutes old or in the future
        val now = System.currentTimeMillis()
        val envelopeAge = now - envelope.timestamp
        if (envelopeAge < -ENVELOPE_TIME_DRIFT_MS || envelopeAge > ENVELOPE_MAX_AGE_MS) {
            if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
                Log.w("MessageRepository", "Dropping envelope ${envelope.id} with stale/future timestamp (age=${envelopeAge}ms)")
            }
            return
        }

        // Authenticate envelope cryptographic signature (SEC-NET-04)
        val kpg = keyPairGenerator
        if (kpg != null) {
            if (envelope.signature.isEmpty()) {
                if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
                    Log.w("MessageRepository", "Dropping unsigned envelope ${envelope.id} (type=${envelope.type}) from $senderUid")
                }
                return
            }

            val senderPub = envelope.senderIdentityPub
            if (senderPub.isEmpty()) {
                if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
                    Log.w("MessageRepository", "Dropping envelope ${envelope.id} with empty sender public key")
                }
                return
            }

            val existingContact = contactDao.getByUid(senderUid)
            if (existingContact != null && existingContact.publicKey.isNotEmpty()) {
                if (envelope.type != TransportEnvelope.TYPE_IDENTITY_ROTATION && !existingContact.publicKey.contentEquals(senderPub)) {
                    if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
                        Log.e("MessageRepository", "Spoofing rejected: Sender public key does not match trusted key for $senderUid")
                    }
                    return
                }
            }

            val isSignatureValid = try {
                kpg.verify(
                    publicKey = senderPub,
                    data = envelope.getCanonicalData(),
                    signature = envelope.signature
                )
            } catch (_: Exception) {
                false
            }

            if (!isSignatureValid) {
                if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
                    Log.e("MessageRepository", "Signature verification FAILED for envelope ${envelope.id} from $senderUid — packet dropped!")
                }
                return
            }
        }

        if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
            Log.d("MessageRepository", "handleIncomingEnvelope: type=${envelope.type}, senderUid=$senderUid")
        }

        // Anti-replay: skip if already processed and stored in database
        if (envelope.type == TransportEnvelope.TYPE_MESSAGE && messageDao.existsById(envelope.id)) {
            if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
                Log.d("MessageRepository", "Skipping already persisted message ${envelope.id}")
            }
            return
        }

        when (envelope.type) {
            TransportEnvelope.TYPE_IDENTITY_ROTATION -> {
                val cert = SuccessionCertificate.fromByteArray(envelope.ciphertext)
                if (cert != null && kpg != null && SuccessionCertificate.verify(cert, kpg)) {
                    val existing = contactDao.getByUid(senderUid)
                    if (existing != null) {
                        try {
                            val oldPubBytes = kotlin.io.encoding.Base64.decode(cert.oldIdentityPubKey)
                            if (existing.publicKey.isEmpty() || existing.publicKey.contentEquals(oldPubBytes)) {
                                val newPubBytes = kotlin.io.encoding.Base64.decode(cert.newIdentityPubKey)
                                val updated = existing.copy(publicKey = newPubBytes)
                                contactDao.insertOrUpdate(updated)
                                cryptoManager.initializeSession(senderUid, newPubBytes, existing.onionAddress)
                                if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
                                    Log.i("MessageRepository", "Successfully rotated key for $senderUid via verified SuccessionCertificate")
                                }
                            } else {
                                if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
                                    Log.w("MessageRepository", "Ignored rotation cert: old key mismatch for $senderUid")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MessageRepository", "Failed updating key for identity rotation: ${e.message}")
                        }
                    }
                }
            }
            TransportEnvelope.TYPE_SCAN_PING -> {
                // Rate-limit: ignore duplicate pings within 30s (replay/spam resistance)
                val existing = contactDao.getByUid(senderUid)
                if (existing != null && (now - (existing.lastSeenAt ?: 0)) < 30_000L) {
                    // Still update lastSeen silently but suppress duplicate notification/dialog storm
                    // by skipping re-insert churn. Keep flags intact.
                    return
                }
                val displayName = envelope.senderUsername.ifBlank { "Peer " + senderUid.take(6).uppercase() }
                val profileColor = envelope.senderProfileColor.ifBlank { null }
                // Key pinning: if we already optically verified a different key, reject rotation without cert
                if (existing != null && existing.publicKey.isNotEmpty() &&
                    envelope.senderIdentityPub.isNotEmpty() &&
                    !existing.publicKey.contentEquals(envelope.senderIdentityPub) &&
                    existing.hasScannedPeer
                ) {
                    if (`in`.grayscales.entangl.BuildConfig.DEBUG) {
                        Log.e("MessageRepository", "Key change without succession for $senderUid — SCAN_PING rejected")
                    }
                    return
                }
                // Inbound proof: peer scanned our QR. Preserve outbound scan state.
                // NEVER set isAccepted here — mutual requires explicit confirmMutualHandshake().
                val contactEntity = existing?.copy(
                    displayName = displayName,
                    publicKey = if (envelope.senderIdentityPub.isNotEmpty()) envelope.senderIdentityPub else existing.publicKey,
                    onionAddress = if (envelope.senderOnion.isNotBlank()) envelope.senderOnion else existing.onionAddress,
                    lastSeenAt = envelope.timestamp,
                    isAccepted = false,
                    profileColor = profileColor ?: existing.profileColor,
                    hasBeenScanned = true
                ) ?: ContactEntity(
                    uid = senderUid,
                    publicKey = envelope.senderIdentityPub,
                    onionAddress = envelope.senderOnion,
                    safetyNumber = "Pending reciprocal verification",
                    displayName = displayName,
                    createdAt = System.currentTimeMillis(),
                    lastSeenAt = envelope.timestamp,
                    isAccepted = false,
                    profileColor = profileColor,
                    hasScannedPeer = false,
                    hasBeenScanned = true
                )
                contactDao.insertOrUpdate(contactEntity)
                notificationManager.showScanPingNotification(senderUid, displayName)
            }

            TransportEnvelope.TYPE_SCAN_ACCEPT -> {
                val peerName = envelope.senderUsername.ifBlank { "Peer " + senderUid.take(6).uppercase() }
                val profileColor = envelope.senderProfileColor.ifBlank { null }
                val existing = contactDao.getByUid(senderUid)
                
                // Security Gate: remote envelopes must NEVER transition to isAccepted=true.
                // SCAN_ACCEPT is only a receipt, not optical proof. Preserve mutual flags, stay unaccepted.
                val updatedContact = if (existing != null) {
                    val updatedPub = if (existing.publicKey.isEmpty() && envelope.senderIdentityPub.isNotEmpty()) {
                        envelope.senderIdentityPub
                    } else existing.publicKey

                    existing.copy(
                        displayName = peerName,
                        isAccepted = false,
                        publicKey = updatedPub,
                        profileColor = profileColor ?: existing.profileColor,
                        lastSeenAt = envelope.timestamp
                    )
                } else {
                    ContactEntity(
                        uid = senderUid,
                        publicKey = envelope.senderIdentityPub,
                        onionAddress = envelope.senderOnion,
                        safetyNumber = "Pending reciprocal verification",
                        displayName = peerName,
                        createdAt = envelope.timestamp,
                        lastSeenAt = envelope.timestamp,
                        isAccepted = false, // Must remain false until local user verifies
                        profileColor = profileColor,
                        hasScannedPeer = false,
                        hasBeenScanned = false
                    )
                }
                contactDao.insertOrUpdate(updatedContact)

                // Do NOT init session here — wait for mutual optical + safety confirm.
                // Surface as ping receipt so user stays on handshake profile to scan back.
                notificationManager.showScanPingNotification(senderUid, peerName)

                // 2. Insert an in-chat status notice in the conversation stream
                val noticeId = "accept-${envelope.id}"
                val noticeText = "$peerName accepted your connection request"
                decryptedCache[noticeId] = noticeText
                val entity = MessageEntity(
                    id = noticeId,
                    contactUid = senderUid,
                    ciphertext = NOTICE_PAYLOAD_SCAN_ACCEPT.encodeToByteArray(),
                    direction = 0, // INCOMING
                    status = 2,    // DELIVERED
                    timestamp = envelope.timestamp,
                    selfDestructAt = null
                )
                messageDao.insertOrUpdate(entity)
            }

            TransportEnvelope.TYPE_MESSAGE -> {
                val contactEntity = contactDao.getByUid(senderUid)
                val rawCiphertext = TransportEnvelope.unpadPayload(envelope.ciphertext)

                if (contactEntity != null && contactEntity.isAccepted) {
                    // Established and accepted contact!
                    try {
                        val plaintextBytes = cryptoManager.decryptMessage(senderUid, rawCiphertext)
                        val plaintext = plaintextBytes.decodeToString()
                        decryptedCache[envelope.id] = plaintext

                        val entity = MessageEntity(
                            id = envelope.id,
                            contactUid = senderUid,
                            ciphertext = rawCiphertext,
                            direction = 0, // INCOMING
                            status = 2,    // DELIVERED
                            timestamp = envelope.timestamp,
                            selfDestructAt = null
                        )
                        messageDao.insertOrUpdate(entity)
                        notificationManager.showIncomingMessageNotification(senderUid)

                        // Send delivery ACK back to sender
                        networkTransport.sendDeliveryAck(envelope.id, senderUid, nodeIdentityManager.localUid)
                    } catch (e: Exception) {
                        Log.w("MessageRepository", "Decryption deferred for incoming message: ${e.message}")
                        val entity = MessageEntity(
                            id = envelope.id,
                            contactUid = senderUid,
                            ciphertext = rawCiphertext,
                            direction = 0, // INCOMING
                            status = 0,    // PENDING
                            timestamp = envelope.timestamp,
                            selfDestructAt = null
                        )
                        messageDao.insertOrUpdate(entity)
                    }
                } else {
                    // Unilateral scan case: Peer A scanned Peer B, Peer B hasn't accepted / scanned back yet.
                    // Store encrypted message in Room and create/keep pending contact entry
                    val entity = MessageEntity(
                        id = envelope.id,
                        contactUid = senderUid,
                        ciphertext = rawCiphertext,
                        direction = 0, // INCOMING
                        status = 0,    // 0 = PENDING / WAITING FOR MUTUAL HANDSHAKE
                        timestamp = envelope.timestamp,
                        selfDestructAt = null
                    )
                    messageDao.insertOrUpdate(entity)

                    if (contactEntity == null) {
                        val displayName = envelope.senderUsername.ifBlank { "Peer " + senderUid.take(6).uppercase() }
                        val pendingContact = ContactEntity(
                            uid = senderUid,
                            publicKey = envelope.senderIdentityPub,
                            onionAddress = envelope.senderOnion,
                            safetyNumber = "Pending reciprocal verification",
                            displayName = displayName,
                            createdAt = System.currentTimeMillis(),
                            lastSeenAt = System.currentTimeMillis(),
                            isAccepted = false,
                            hasScannedPeer = false,
                            hasBeenScanned = false
                        )
                        contactDao.insertOrUpdate(pendingContact)
                    }
                    notificationManager.showIncomingMessageNotification(senderUid)
                }
            }
        }
    }

    override suspend fun send(contactUid: String, plaintext: String) {
        val id = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // Military-grade mutual gate: no outbound messaging until both optical
        // verifications + safety confirmation (isAccepted==true).
        val gateContact = contactDao.getByUid(contactUid)
            ?: throw IllegalStateException("Unknown contact $contactUid — complete mutual QR handshake first")
        if (!gateContact.isAccepted || !gateContact.hasScannedPeer || !gateContact.hasBeenScanned) {
            throw IllegalStateException(
                "Mutual handshake incomplete for $contactUid " +
                    "(scanned=${gateContact.hasScannedPeer}, beenScanned=${gateContact.hasBeenScanned}, " +
                    "accepted=${gateContact.isAccepted}) — A must scan B and B must scan A while on handshake profile"
            )
        }

        // Auto-session recovery: if process restarted or session key was uninitialized,
        // recover from contactDao public key before encrypting
        val contact = gateContact
        if (contact.publicKey.isNotEmpty()) {
            try {
                cryptoManager.initializeSession(contactUid, contact.publicKey, contact.onionAddress)
            } catch (e: Exception) {
                Log.w("MessageRepository", "Session auto-init in send encountered: ${e.message}")
            }
        }

        // Double Ratchet inner encryption
        val ciphertext = cryptoManager.encryptMessage(contactUid, plaintext.encodeToByteArray())

        // Cache plaintext in memory only
        decryptedCache[id] = plaintext

        val entity = MessageEntity(
            id = id,
            contactUid = contactUid,
            ciphertext = ciphertext,
            direction = 1, // OUTGOING
            status = 0,    // PENDING
            timestamp = timestamp,
            selfDestructAt = null
        )
        messageDao.insertOrUpdate(entity)

        // Transmit over real-time network transport with retry
        scope.launch {
            // Retrieve real identity public key for the outgoing envelope
            val identityPub = try {
                cryptoManager.getLocalIdentityPublicKey() ?: ByteArray(0)
            } catch (_: Exception) {
                ByteArray(0)
            }

            val envelope = TransportEnvelope(
                id = id,
                type = TransportEnvelope.TYPE_MESSAGE,
                senderUid = nodeIdentityManager.localUid,
                senderIdentityPub = identityPub,
                senderOnion = nodeIdentityManager.localOnion,
                recipientUid = contactUid,
                ciphertext = TransportEnvelope.padPayload(ciphertext),
                timestamp = timestamp
            )

            // Retry with exponential backoff (3 attempts: 0s, 2s, 4s)
            var sent = false
            for (attempt in 0 until MAX_SEND_RETRIES) {
                if (attempt > 0) {
                    kotlinx.coroutines.delay((RETRY_BASE_DELAY_MS * (1L shl (attempt - 1))).milliseconds)
                }
                sent = networkTransport.sendEnvelope(envelope)
                if (sent) break
                Log.w("MessageRepository", "Send attempt ${attempt + 1}/$MAX_SEND_RETRIES failed for message $id")
            }

            if (sent) {
                messageDao.updateStatus(id, 1) // 1 = SENT
            } else {
                Log.e("MessageRepository", "All $MAX_SEND_RETRIES send attempts failed for message $id — message stuck PENDING")
            }
        }
    }

    override suspend fun unlockPendingMessages(contactUid: String) {
        val messages = messageDao.getMessagesForContact(contactUid)
        for (msg in messages) {
            if (msg.direction == 0) { // incoming
                val cached = decryptedCache[msg.id]
                if (cached == null || cached == "Encrypted message" || msg.status == 0) {
                    try {
                        val plaintextBytes = cryptoManager.decryptMessage(contactUid, msg.ciphertext)
                        val plaintext = plaintextBytes.decodeToString()
                        decryptedCache[msg.id] = plaintext
                        messageDao.updateStatus(msg.id, 2) // DELIVERED
                        networkTransport.sendDeliveryAck(msg.id, contactUid, nodeIdentityManager.localUid)
                    } catch (e: Exception) {
                        Log.w("MessageRepository", "Could not unlock message ${msg.id}: ${e.message}")
                    }
                }
            }
        }
    }

    suspend fun retryFailedMessages() {
        val pendingOutgoing = messageDao.getPendingOutgoingMessages()
        if (pendingOutgoing.isEmpty()) return

        Log.i("MessageRepository", "Retrying ${pendingOutgoing.size} stuck pending outgoing messages")

        val identityPub = try {
            cryptoManager.getLocalIdentityPublicKey() ?: ByteArray(0)
        } catch (_: Exception) {
            ByteArray(0)
        }

        for (msg in pendingOutgoing) {
            val envelope = TransportEnvelope(
                id = msg.id,
                type = TransportEnvelope.TYPE_MESSAGE,
                senderUid = nodeIdentityManager.localUid,
                senderIdentityPub = identityPub,
                senderOnion = nodeIdentityManager.localOnion,
                recipientUid = msg.contactUid,
                ciphertext = TransportEnvelope.padPayload(msg.ciphertext),
                timestamp = msg.timestamp
            )

            // Attempt one send right away; if it succeeds, mark SENT; if it fails, it will just stay pending for the next network event
            val sent = networkTransport.sendEnvelope(envelope)
            if (sent) {
                messageDao.updateStatus(msg.id, 1) // 1 = SENT
            } else {
                Log.w("MessageRepository", "Retry failed for stuck message ${msg.id}")
            }
        }
    }

    override suspend fun receiveAndStore(message: Message) {
        decryptedCache[message.id] = message.plaintext
        val ciphertext = try {
            cryptoManager.encryptMessage(message.contactUid, message.plaintext.encodeToByteArray())
        } catch (_: Exception) {
            // For protocol events, system notices, or pre-session messages where ratchet session is not yet active
            message.plaintext.encodeToByteArray()
        }
        val entity = MessageEntity.fromDomain(message, ciphertext)
        messageDao.insertOrUpdate(entity)
    }

    override suspend fun markDelivered(messageId: String) {
        messageDao.updateStatus(messageId, 2) // DELIVERED
    }

    override suspend fun markRead(messageId: String) {
        messageDao.updateStatus(messageId, 3) // READ
    }

    override fun observeForContact(contactUid: String): Flow<List<Message>> {
        return messageDao.observeForContact(contactUid).map { entities ->
            entities.map { entity ->
                val cached = decryptedCache[entity.id]
                val plaintext = if (cached != null && cached != "Encrypted message") {
                    cached
                } else {
                    if (entity.id.startsWith("accept-") || entity.id.startsWith("status-") || entity.id.startsWith("system-")) {
                        val rawNotice = try {
                            entity.ciphertext.decodeToString()
                        } catch (_: Exception) {
                            "Notice"
                        }
                        if (rawNotice == NOTICE_PAYLOAD_SCAN_ACCEPT) {
                            val contact = contactDao.getByUid(contactUid)
                            val name = contact?.displayName?.ifBlank { null } ?: "Peer"
                            val resolved = "$name accepted your connection request"
                            decryptedCache[entity.id] = resolved
                            resolved
                        } else {
                            val resolved = rawNotice.ifBlank { "Notice" }
                            decryptedCache[entity.id] = resolved
                            resolved
                        }
                    } else {
                        try {
                            val decrypted = cryptoManager.decryptMessage(contactUid, entity.ciphertext).decodeToString()
                            decryptedCache[entity.id] = decrypted
                            decrypted
                        } catch (_: Exception) {
                            "Encrypted message"
                        }
                    }
                }
                entity.toDomain(plaintext)
            }
        }
    }

    override suspend fun deleteExpired() {
        val now = System.currentTimeMillis()
        messageDao.deleteExpired(now)
    }

    override suspend fun getPendingCount(): Int {
        return messageDao.getPendingCount()
    }

    companion object {
        private const val MAX_SEND_RETRIES = 3
        private const val RETRY_BASE_DELAY_MS = 2000L
        /** Maximum age of an incoming envelope timestamp (24 hours) before it's rejected as stale replay */
        private const val ENVELOPE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
        /** Maximum allowed clock drift into the future (15 minutes) */
        private const val ENVELOPE_TIME_DRIFT_MS = 15 * 60 * 1000L
        /** Non-identifying token stored in database ciphertext column for accepted connection notices (SEC-08) */
        private const val NOTICE_PAYLOAD_SCAN_ACCEPT = "STATUS_NOTICE:CONNECTION_ACCEPTED"
    }
}
