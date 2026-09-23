package `in`.grayscales.entangl.core.crypto

/**
 * Platform-specific wrapper for cryptographic key material held in native memory.
 *
 * Android implementation uses [java.nio.ByteBuffer.allocateDirect] to keep
 * key material off the JVM heap. On [close], the buffer is zeroed via
 * `sodium_memzero` to prevent memory scraping.
 *
 * CRITICAL SECURITY RULES:
 * - NEVER convert the contents to a [String] (immutable, cannot be wiped).
 * - ALWAYS use try-with-resources or .use {} to ensure zeroization.
 * - The [get] method returns a COPY; the caller is responsible for zeroing it.
 */
expect class NativeKeyBuffer(size: Int) : AutoCloseable {

    /** Write key material into the native buffer. */
    fun put(data: ByteArray)

    /**
     * Read a COPY of the key material from the native buffer.
     * The internal buffer remains untouched.
     * Caller MUST zeroize the returned ByteArray after use.
     */
    fun get(): ByteArray

    /** Returns the size of the buffer in bytes. */
    fun size(): Int

    /**
     * Zeroize the native memory buffer.
     * After this call, the buffer contents are irrecoverable.
     */
    override fun close()
}
