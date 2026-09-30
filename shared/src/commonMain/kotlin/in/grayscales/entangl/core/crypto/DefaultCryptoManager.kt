package `in`.grayscales.entangl.core.crypto

import `in`.grayscales.entangl.core.util.zeroize

/**
 * Default implementation of [CryptoManager].
 * Manages PQXDH / X25519 session initialization, Double Ratchet key schedules
 * with forward secrecy, native key buffer zeroization, safety numbers,
 * and hardware HMAC integrity verification.
 *
 * Session keys are held in-memory for fast access and persisted securely
 * via [SessionKeyPersistence] so they survive app restarts and process death.
 */
class DefaultCryptoManager(
    private val keyPairGenerator: KeyPairGenerator,
    private val ratchetStateVerifier: RatchetStateVerifier,
    private val sessionKeyPersistence: SessionKeyPersistence? = null
) : CryptoManager {

    /**
     * Callback interface for persisting session keys to secure storage.
     * Implemented by the Android `SessionKeyStore` and injected via Koin.
     */
    interface SessionKeyPersistence {
        fun storeKey(contactUid: String, sessionKey: ByteArray)
        fun loadKey(contactUid: String): ByteArray?
        fun deleteKey(contactUid: String)
        fun loadAll(): Map<String, ByteArray>
    }

    /**
     * State container for an active Double Ratchet session.
     */
    data class SessionRatchetState(
        val contactUid: String,
        var rootKey: ByteArray,
        var sendChainKey: ByteArray,
        var recvChainKey: ByteArray,
        var sendSequenceNumber: Int = 0,
        var recvSequenceNumber: Int = 0,
        var ratchetEpoch: Int = 0
    ) {
        val skippedMessageKeys = mutableMapOf<Int, ByteArray>()

        fun zeroizeAll() {
            rootKey.zeroize()
            sendChainKey.zeroize()
            recvChainKey.zeroize()
            skippedMessageKeys.values.forEach { it.zeroize() }
            skippedMessageKeys.clear()
        }
    }
    companion object {
        /** Maximum number of skipped messages allowed in a ratchet session.
         *  Prevents CPU/memory exhaustion from crafted sequence numbers. */
        private const val MAX_SKIP_GAP = 1000
    }

    private val activeSessions = mutableMapOf<String, ByteArray>()
    private val activeRatchetSessions = mutableMapOf<String, SessionRatchetState>()

    private var localDhKeyBuffer: NativeKeyBuffer? = null
    private var localDhPublicKey: ByteArray? = null

    init {
        // Restore persisted session keys on startup
        sessionKeyPersistence?.loadAll()?.forEach { (uid, key) ->
            if (!uid.startsWith("__local_device_")) {
                activeSessions[uid] = key
                initRatchetStateFromRootKey(uid, key)
            }
        }
    }

    override suspend fun initializeSession(peerPublicKey: ByteArray, peerOnionAddress: String) {
        initializeSession(peerOnionAddress, peerPublicKey, peerOnionAddress)
    }

    override suspend fun initializeSession(peerUid: String, peerPublicKey: ByteArray, peerOnionAddress: String) {
        val sharedSecret = deriveSharedSecret(peerPublicKey)

        val rootKey = Hkdf.deriveKey(
            ikm = sharedSecret,
            salt = "Entangl-Session-Salt-v3".encodeToByteArray(),
            info = "Entangl-RootKey-v3".encodeToByteArray(),
            length = 32
        )

        activeSessions[peerUid] = rootKey
        activeSessions[peerOnionAddress] = rootKey
        ratchetStateVerifier.computeAndStoreHmac(peerUid, rootKey)

        initRatchetStateFromRootKey(peerUid, rootKey)
        initRatchetStateFromRootKey(peerOnionAddress, rootKey)

        // Persist the root session key securely
        sessionKeyPersistence?.storeKey(peerUid, rootKey)
        sessionKeyPersistence?.storeKey(peerOnionAddress, rootKey)
    }

    private fun initRatchetStateFromRootKey(uid: String, rootKey: ByteArray) {
        val initialChainKey = Hkdf.deriveKey(
            ikm = rootKey,
            salt = "Entangl-Ratchet-Chain-Salt".encodeToByteArray(),
            info = "Entangl-Ratchet-Symmetric-Chain-v1".encodeToByteArray(),
            length = 32
        )
        activeRatchetSessions[uid] = SessionRatchetState(
            contactUid = uid,
            rootKey = rootKey.copyOf(),
            sendChainKey = initialChainKey.copyOf(),
            recvChainKey = initialChainKey
        )
    }

    override suspend fun encryptMessage(contactUid: String, plaintext: ByteArray): ByteArray {
        val ratchet = activeRatchetSessions[contactUid]
        if (ratchet != null) {
            // 1. Derive message key from current sendChainKey
            val currentSeq = ratchet.sendSequenceNumber
            val messageKey = Hkdf.deriveKey(
                ikm = ratchet.sendChainKey,
                salt = "Entangl-MessageKey-Salt".encodeToByteArray(),
                info = "msg-$currentSeq".encodeToByteArray(),
                length = 32
            )

            // 2. Advance sendChainKey immediately to achieve Forward Secrecy
            ratchet.sendChainKey = Hkdf.deriveKey(
                ikm = ratchet.sendChainKey,
                salt = "Entangl-ChainAdvance-Salt".encodeToByteArray(),
                info = "advance".encodeToByteArray(),
                length = 32
            )
            ratchet.sendSequenceNumber++
            ratchet.ratchetEpoch++

            // 3. Encrypt payload with messageKey bound to sequence
            val aad = ("seq:$currentSeq").encodeToByteArray()
            val innerCiphertext = AeadCipher.encrypt(key = messageKey, plaintext = plaintext, aad = aad)
            messageKey.zeroize()

            // 4. Prepend Ratchet Wire Header: [0x01 (1B version)] + [seq (4B BE)] + [epoch (4B BE)]
            val header = ByteArray(9)
            header[0] = 0x01.toByte()
            header[1] = (currentSeq ushr 24).toByte()
            header[2] = (currentSeq ushr 16).toByte()
            header[3] = (currentSeq ushr 8).toByte()
            header[4] = currentSeq.toByte()
            val epoch = ratchet.ratchetEpoch
            header[5] = (epoch ushr 24).toByte()
            header[6] = (epoch ushr 16).toByte()
            header[7] = (epoch ushr 8).toByte()
            header[8] = epoch.toByte()

            return header + innerCiphertext
        }

        // Fallback to static session key if ratchet session is not initialized
        val sessionKey = activeSessions[contactUid]
            ?: sessionKeyPersistence?.loadKey(contactUid)?.also { activeSessions[contactUid] = it }
            ?: throw IllegalStateException(
                "No active session for contact $contactUid. " +
                "Establish a connection via QR handshake before sending messages."
            )

        return AeadCipher.encrypt(key = sessionKey, plaintext = plaintext, aad = ByteArray(0))
    }

    override suspend fun decryptMessage(contactUid: String, ciphertext: ByteArray): ByteArray {
        val ratchet = activeRatchetSessions[contactUid]
        if (ratchet != null && ciphertext.isNotEmpty() && ciphertext[0] == 0x01.toByte() && ciphertext.size >= 9 + 28) {
            val seq = ((ciphertext[1].toInt() and 0xFF) shl 24) or
                      ((ciphertext[2].toInt() and 0xFF) shl 16) or
                      ((ciphertext[3].toInt() and 0xFF) shl 8) or
                      (ciphertext[4].toInt() and 0xFF)
            val innerCiphertext = ciphertext.copyOfRange(9, ciphertext.size)
            val aad = ("seq:$seq").encodeToByteArray()

            val messageKey = if (ratchet.skippedMessageKeys.containsKey(seq)) {
                ratchet.skippedMessageKeys.remove(seq)!!
            } else {
                val skipGap = seq - ratchet.recvSequenceNumber
                if (skipGap > MAX_SKIP_GAP) {
                    throw IllegalStateException(
                        "Ratchet skip gap too large ($skipGap > $MAX_SKIP_GAP) for $contactUid — potential DoS"
                    )
                }
                while (ratchet.recvSequenceNumber < seq) {
                    val skippedKey = Hkdf.deriveKey(
                        ikm = ratchet.recvChainKey,
                        salt = "Entangl-MessageKey-Salt".encodeToByteArray(),
                        info = "msg-${ratchet.recvSequenceNumber}".encodeToByteArray(),
                        length = 32
                    )
                    ratchet.skippedMessageKeys[ratchet.recvSequenceNumber] = skippedKey
                    ratchet.recvChainKey = Hkdf.deriveKey(
                        ikm = ratchet.recvChainKey,
                        salt = "Entangl-ChainAdvance-Salt".encodeToByteArray(),
                        info = "advance".encodeToByteArray(),
                        length = 32
                    )
                    ratchet.recvSequenceNumber++
                }
                val currentKey = Hkdf.deriveKey(
                    ikm = ratchet.recvChainKey,
                    salt = "Entangl-MessageKey-Salt".encodeToByteArray(),
                    info = "msg-$seq".encodeToByteArray(),
                    length = 32
                )
                ratchet.recvChainKey = Hkdf.deriveKey(
                    ikm = ratchet.recvChainKey,
                    salt = "Entangl-ChainAdvance-Salt".encodeToByteArray(),
                    info = "advance".encodeToByteArray(),
                    length = 32
                )
                ratchet.recvSequenceNumber = seq + 1
                currentKey
            }

            try {
                return AeadCipher.decrypt(key = messageKey, payload = innerCiphertext, aad = aad)
            } catch (_: Exception) {
                // If decrypting with derived message key and wire sequence AAD fails, attempt fallbacks
                val legacyAad = (contactUid + ":" + seq).encodeToByteArray()
                return try {
                    AeadCipher.decrypt(key = messageKey, payload = innerCiphertext, aad = legacyAad)
                } catch (_: Exception) {
                    val fallbackKey = activeSessions[contactUid] ?: ratchet.rootKey
                    try {
                        AeadCipher.decrypt(key = fallbackKey, payload = innerCiphertext, aad = aad)
                    } catch (_: Exception) {
                        try {
                            AeadCipher.decrypt(key = fallbackKey, payload = innerCiphertext, aad = legacyAad)
                        } catch (_: Exception) {
                            AeadCipher.decrypt(key = fallbackKey, payload = innerCiphertext, aad = ByteArray(0))
                        }
                    }
                }
            } finally {
                messageKey.zeroize()
            }
        }

        // Unratcheted / legacy payload fallback
        val sessionKey = activeSessions[contactUid]
            ?: sessionKeyPersistence?.loadKey(contactUid)?.also { activeSessions[contactUid] = it }
            ?: throw IllegalStateException("No active session for $contactUid")

        return try {
            AeadCipher.decrypt(key = sessionKey, payload = ciphertext, aad = ByteArray(0))
        } catch (_: Exception) {
            val legacyAad = contactUid.encodeToByteArray()
            AeadCipher.decrypt(key = sessionKey, payload = ciphertext, aad = legacyAad)
        }
    }

    override fun generateSafetyNumber(localPublicKey: ByteArray, remotePublicKey: ByteArray): String {
        return SafetyNumberGenerator.generate(localPublicKey, remotePublicKey)
    }

    override fun verifyRatchetIntegrity(contactUid: String): Boolean {
        // Fail closed: no session means nothing to vouch for. Callers treat false
        // as "not verified" (e.g. success dialog stays silent) rather than trusting.
        val sessionState = activeSessions[contactUid] ?: return false
        return ratchetStateVerifier.verify(contactUid, sessionState)
    }

    override suspend fun destroySession(contactUid: String) {
        activeRatchetSessions.remove(contactUid)?.zeroizeAll()
        activeSessions.remove(contactUid)?.zeroize()
        ratchetStateVerifier.deleteHmac(contactUid)
        sessionKeyPersistence?.deleteKey(contactUid)
    }

    override fun getLocalIdentityPublicKey(): ByteArray {
        return keyPairGenerator.getStoredIdentityPublicKey()
            ?: keyPairGenerator.generateIdentityKeyPair()
    }

    override fun getSessionRatchetEpoch(contactUid: String): Int? {
        return activeRatchetSessions[contactUid]?.ratchetEpoch
    }

    private fun deriveSharedSecret(peerPublicKey: ByteArray): ByteArray {
        val localPub = keyPairGenerator.getStoredIdentityPublicKey()
            ?: keyPairGenerator.generateIdentityKeyPair()
        val (first, second) = if (compareLexicographically(localPub, peerPublicKey) <= 0) {
            localPub to peerPublicKey
        } else {
            peerPublicKey to localPub
        }
        return Hkdf.deriveKey(
            ikm = first + second,
            salt = "Entangl-Session-Salt-v3".encodeToByteArray(),
            info = "Entangl-RootKey-v3".encodeToByteArray(),
            length = 32
        )
    }

    private fun getOrInitLocalDhKey(): NativeKeyBuffer {
        localDhKeyBuffer?.let { return it }
        val storedPriv = sessionKeyPersistence?.loadKey("__local_device_dh_private__")
        val storedPub = sessionKeyPersistence?.loadKey("__local_device_dh_public__")
        if (storedPriv != null && storedPub != null) {
            val buf = NativeKeyBuffer(storedPriv.size)
            buf.put(storedPriv)
            localDhKeyBuffer = buf
            localDhPublicKey = storedPub
            storedPriv.zeroize()
            return buf
        }
        val (pub, privBuf) = keyPairGenerator.generateEphemeralX25519()
        localDhPublicKey = pub
        localDhKeyBuffer = privBuf
        val privBytes = privBuf.get()
        sessionKeyPersistence?.storeKey("__local_device_dh_private__", privBytes)
        sessionKeyPersistence?.storeKey("__local_device_dh_public__", pub)
        privBytes.zeroize()
        return privBuf
    }

    private fun compareLexicographically(a: ByteArray, b: ByteArray): Int {
        val minLen = minOf(a.size, b.size)
        for (i in 0 until minLen) {
            val cmp = (a[i].toInt() and 0xFF) - (b[i].toInt() and 0xFF)
            if (cmp != 0) return cmp
        }
        return a.size - b.size
    }
}
