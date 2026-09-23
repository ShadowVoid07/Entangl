package `in`.grayscales.entangl.core.util

/**
 * Platform-specific cryptographically secure random number generator.
 */
expect object SecureRandom {

    /** Generate [size] cryptographically random bytes. */
    fun nextBytes(size: Int): ByteArray
}
