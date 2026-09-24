package `in`.grayscales.entangl.core.crypto

/**
 * Platform-specific key pair generator.
 * Android: backed by Android Keystore (StrongBox preferred).
 * iOS: backed by Secure Enclave / Keychain.
 */
expect class KeyPairGenerator() {

    /**
     * Generate a new Ed25519 identity key pair.
     * The private key is stored in the platform's secure element.
     *
     * @return The public key bytes. The private key never leaves secure storage.
     */
    fun generateIdentityKeyPair(): ByteArray

    /**
     * Generate an ephemeral X25519 key pair for QR handshake payload.
     * Unlike identity keys, ephemeral keys are short-lived and may be
     * held in native memory (not the secure element) for performance.
     *
     * @return Pair of (publicKey, privateKeyHandle as NativeKeyBuffer).
     */
    fun generateEphemeralX25519(): Pair<ByteArray, NativeKeyBuffer>

    /**
     * Retrieve the stored Ed25519 identity public key.
     *
     * @return The public key bytes, or null if no identity has been generated yet.
     */
    fun getStoredIdentityPublicKey(): ByteArray?

    /**
     * Sign data with the Ed25519 identity private key.
     * The private key never leaves the secure element.
     *
     * @param data The bytes to sign.
     * @return The Ed25519 signature bytes.
     */
    fun sign(data: ByteArray): ByteArray

    /**
     * Verify an Ed25519 signature against a public key.
     *
     * @param publicKey The signer's Ed25519 public key.
     * @param data The original data that was signed.
     * @param signature The signature to verify.
     * @return true if the signature is valid.
     */
    fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean

    /**
     * Compute X25519 Diffie-Hellman shared secret between local ephemeral private key
     * and peer's ephemeral public key.
     */
    fun computeX25519KeyAgreement(privateKeyBuffer: NativeKeyBuffer, peerPublicKey: ByteArray): ByteArray
}
