package `in`.grayscales.entangl.core.crypto

actual class NativeKeyBuffer actual constructor(private val size: Int) : AutoCloseable {
    private val buffer = ByteArray(size)

    actual fun put(data: ByteArray) {
        val len = minOf(data.size, size)
        data.copyInto(buffer, 0, 0, len)
    }

    actual fun get(): ByteArray = buffer.copyOf()

    actual fun size(): Int = size

    actual override fun close() {
        buffer.fill(0)
    }
}
