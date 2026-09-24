package `in`.grayscales.entangl.core.crypto

/**
 * RFC 5869 HMAC-based Extract-and-Expand Key Derivation Function (HKDF-SHA256).
 */
expect object Hkdf {
    fun deriveKey(
        ikm: ByteArray,
        salt: ByteArray = ByteArray(0),
        info: ByteArray = ByteArray(0),
        length: Int = 32
    ): ByteArray
}
