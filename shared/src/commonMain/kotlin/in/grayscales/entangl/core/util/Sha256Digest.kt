package `in`.grayscales.entangl.core.util

/**
 * Platform-independent SHA-256 digest provider for KMP common code.
 */
expect object Sha256Digest {
    fun digest(input: ByteArray): ByteArray
}
