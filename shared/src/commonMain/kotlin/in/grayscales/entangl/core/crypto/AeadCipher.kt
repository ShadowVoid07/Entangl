package `in`.grayscales.entangl.core.crypto

/**
 * Standard Authenticated Encryption with Associated Data (AEAD) using AES-256-GCM.
 * Cryptographically binds ciphertext to metadata (AAD) and ensures integrity with a 128-bit authentication tag.
 */
expect object AeadCipher {
    /**
     * Encrypts plaintext using AES-256-GCM.
     * Output format: 12-byte IV + Ciphertext + 16-byte GCM Authentication Tag.
     */
    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray

    /**
     * Decrypts and authenticates AES-256-GCM payload.
     * Throws an exception if the authentication tag does not verify or payload is tampered.
     */
    fun decrypt(key: ByteArray, payload: ByteArray, aad: ByteArray = ByteArray(0)): ByteArray
}
