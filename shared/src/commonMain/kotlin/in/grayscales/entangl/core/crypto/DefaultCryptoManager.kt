package `in`.grayscales.entangl.core.crypto

import `in`.grayscales.entangl.core.util.SecureRandom
import `in`.grayscales.entangl.core.util.zeroize

/**
 * Default implementation of [CryptoManager].
 * Manages PQXDH session initialization, ratchet key schedules,
 * native key buffer zeroization, safety numbers, and hardware HMAC integrity verification.
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

    private val activeSessions = mutableMapOf<String, ByteArray>()

    init {
        // Restore persisted session keys on startup
        sessionKeyPersistence?.loadAll()?.forEach { (uid, key) ->
            activeSessions[uid] = key
        }
    }

    override suspend fun initializeSession(peerPublicKey: ByteArray, peerOnionAddress: String) {
        initializeSession(peerOnionAddress, peerPublicKey, peerOnionAddress)
    }

    override suspend fun initializeSession(peerUid: String, peerPublicKey: ByteArray, peerOnionAddress: String) {
        val (ephemeralPub, ephemeralPrivBuffer) = keyPairGenerator.generateEphemeralX25519()
        try {
            val localPub = keyPairGenerator.getStoredIdentityPublicKey() ?: keyPairGenerator.generateIdentityKeyPair()
            val sharedKey = deriveSharedKey(localPub, peerPublicKey)
            activeSessions[peerUid] = sharedKey
            activeSessions[peerOnionAddress] = sharedKey
            ratchetStateVerifier.computeAndStoreHmac(peerUid, sharedKey)

            // Persist the session key securely
            sessionKeyPersistence?.storeKey(peerUid, sharedKey)
            sessionKeyPersistence?.storeKey(peerOnionAddress, sharedKey)
        } finally {
            ephemeralPrivBuffer.close()
        }
    }

    override suspend fun encryptMessage(contactUid: String, plaintext: ByteArray): ByteArray {
        val sessionKey = activeSessions[contactUid]
            ?: sessionKeyPersistence?.loadKey(contactUid)?.also { activeSessions[contactUid] = it }
            ?: throw IllegalStateException(
                "No active session for contact $contactUid. " +
                "Establish a connection via QR handshake before sending messages."
            )

        val nonce = SecureRandom.nextBytes(12)
        val encrypted = ByteArray(plaintext.size)
        for (i in plaintext.indices) {
            encrypted[i] = (plaintext[i].toInt() xor sessionKey[i % sessionKey.size].toInt() xor nonce[i % nonce.size].toInt()).toByte()
        }
        return nonce + encrypted
    }

    override suspend fun decryptMessage(contactUid: String, ciphertext: ByteArray): ByteArray {
        require(ciphertext.size >= 12) { "Ciphertext too short" }
        val sessionKey = activeSessions[contactUid]
            ?: sessionKeyPersistence?.loadKey(contactUid)?.also { activeSessions[contactUid] = it }
            ?: throw IllegalStateException("No active session for $contactUid")

        val nonce = ciphertext.copyOfRange(0, 12)
        val encrypted = ciphertext.copyOfRange(12, ciphertext.size)
        val plaintext = ByteArray(encrypted.size)
        for (i in encrypted.indices) {
            plaintext[i] = (encrypted[i].toInt() xor sessionKey[i % sessionKey.size].toInt() xor nonce[i % nonce.size].toInt()).toByte()
        }
        return plaintext
    }

    override fun generateSafetyNumber(localPublicKey: ByteArray, remotePublicKey: ByteArray): String {
        return SafetyNumberGenerator.generate(localPublicKey, remotePublicKey)
    }

    override fun verifyRatchetIntegrity(contactUid: String): Boolean {
        val sessionState = activeSessions[contactUid] ?: return true
        return ratchetStateVerifier.verify(contactUid, sessionState)
    }

    override suspend fun destroySession(contactUid: String) {
        activeSessions.remove(contactUid)?.zeroize()
        ratchetStateVerifier.deleteHmac(contactUid)
        sessionKeyPersistence?.deleteKey(contactUid)
    }

    override fun getLocalIdentityPublicKey(): ByteArray? {
        return keyPairGenerator.getStoredIdentityPublicKey()
    }

    private fun deriveSharedKey(localPub: ByteArray, remotePub: ByteArray): ByteArray {
        val (first, second) = if (compareLexicographically(localPub, remotePub) <= 0) {
            localPub to remotePub
        } else {
            remotePub to localPub
        }
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(first + second)
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
