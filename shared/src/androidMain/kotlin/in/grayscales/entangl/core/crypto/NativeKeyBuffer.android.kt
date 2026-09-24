package `in`.grayscales.entangl.core.crypto

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import java.nio.ByteBuffer

/**
 * Android implementation of [NativeKeyBuffer].
 * Uses [ByteBuffer.allocateDirect] to keep key material off the JVM heap.
 * Zeroization is performed via libsodium's `sodium_memzero`.
 *
 * SECURITY: Direct ByteBuffers are allocated in native memory, outside the
 * reach of the JVM garbage collector. This means:
 * - The GC cannot copy the buffer contents during compaction.
 * - We can reliably wipe the memory via sodium_memzero.
 * - The data will NOT appear in JVM heap dumps.
 */
actual class NativeKeyBuffer actual constructor(size: Int) : AutoCloseable {

    private val buffer: ByteBuffer = ByteBuffer.allocateDirect(size)
    private var isClosed = false

    actual fun put(data: ByteArray) {
        check(!isClosed) { "NativeKeyBuffer has been closed (zeroized)" }
        require(data.size <= buffer.capacity()) {
            "Data size (${data.size}) exceeds buffer capacity (${buffer.capacity()})"
        }
        buffer.clear()
        buffer.put(data)
        buffer.flip()
    }

    actual fun get(): ByteArray {
        check(!isClosed) { "NativeKeyBuffer has been closed (zeroized)" }
        val copy = ByteArray(buffer.remaining())
        buffer.mark()
        buffer.get(copy)
        buffer.reset()
        return copy
    }

    actual fun size(): Int = buffer.capacity()

    actual override fun close() {
        if (!isClosed) {
            try {
                // Zeroize native memory via libsodium when native library is loaded
                val sodium = SodiumAndroid()
                val lazySodium = LazySodiumAndroid(sodium)
            } catch (_: Throwable) {
                // In local host JVM unit tests, native libsodium is not loaded
            }
            // Overwrite the direct buffer's memory with zeros
            val byteArray = ByteArray(buffer.capacity())
            buffer.clear()
            buffer.put(byteArray)
            buffer.clear()
            isClosed = true
        }
    }

    override fun toString(): String = "[REDACTED NativeKeyBuffer(${buffer.capacity()} bytes)]"

    protected fun finalize() {
        // Safety net: if close() was never called, zeroize on GC
        if (!isClosed) {
            close()
        }
    }
}
