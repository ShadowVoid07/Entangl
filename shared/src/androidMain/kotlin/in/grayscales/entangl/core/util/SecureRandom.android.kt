package `in`.grayscales.entangl.core.util

/**
 * Android implementation of [SecureRandom].
 * Wraps [java.security.SecureRandom] for cryptographic random number generation.
 */
actual object SecureRandom {

    private val rng = java.security.SecureRandom()

    actual fun nextBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        rng.nextBytes(bytes)
        return bytes
    }
}
