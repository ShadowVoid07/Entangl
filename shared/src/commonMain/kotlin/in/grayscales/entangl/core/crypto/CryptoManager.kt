package `in`.grayscales.entangl.core.crypto

/**
 * Platform-agnostic interface for the cryptographic engine.
 * Orchestrates key generation, PQXDH session initialization,
 * Double Ratchet encryption/decryption, and safety number generation.
 */
interface CryptoManager {

    /**
     * Initialize a new ratchet session with a peer after QR handshake.
     * Uses PQXDH (X25519 + ML-KEM-768) key agreement.
     *
     * @param peerPublicKey The peer's Ed25519 identity public key from the QR payload.
     * @param peerOnionAddress The peer's .onion v3 address from the QR payload.
     */
    suspend fun initializeSession(peerPublicKey: ByteArray, peerOnionAddress: String)

    /**
     * Initialize a new ratchet session with a peer indexed by their UID.
     */
    suspend fun initializeSession(peerUid: String, peerPublicKey: ByteArray, peerOnionAddress: String)

    /**
     * Encrypt a plaintext message using the Double Ratchet.
     * Returns an opaque ciphertext blob suitable for network transport and local storage.
     * The internal ratchet state advances after each call (forward secrecy).
     *
     * @param contactUid The unique ID of the peer contact.
     * @param plaintext The raw message bytes to encrypt.
     * @return Encrypted ciphertext blob.
     */
    suspend fun encryptMessage(contactUid: String, plaintext: ByteArray): ByteArray

    /**
     * Decrypt a ciphertext blob received from a peer.
     * The internal ratchet state advances after each call.
     *
     * @param contactUid The unique ID of the peer contact.
     * @param ciphertext The encrypted blob received over the Tor P2P tunnel.
     * @return Decrypted plaintext bytes.
     */
    suspend fun decryptMessage(contactUid: String, ciphertext: ByteArray): ByteArray

    /**
     * Generate a Safety Number from both identity public keys.
     * Used for post-connection MITM verification.
     *
     * @return 60-digit numeric fingerprint (12 groups of 5 digits).
     */
    fun generateSafetyNumber(localPublicKey: ByteArray, remotePublicKey: ByteArray): String

    /**
     * Verify the integrity of the stored ratchet state using
     * a hardware-backed HMAC.
     *
     * @return true if the ratchet state has not been tampered with.
     */
    fun verifyRatchetIntegrity(contactUid: String): Boolean

    /**
     * Destroy all session state for a contact.
     * Zeroes all key material from memory.
     */
    suspend fun destroySession(contactUid: String)

    /**
     * Get the local device's identity public key.
     * Used to populate outgoing transport envelopes so the recipient
     * can derive the shared session key from a unilateral incoming message.
     *
     * @return The public key bytes, or null if no identity has been generated yet.
     */
    fun getLocalIdentityPublicKey(): ByteArray?
}
