package `in`.grayscales.entangl.core.util

import kotlin.random.Random

actual object SecureRandom {
    actual fun nextBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        Random.Default.nextBytes(bytes)
        return bytes
    }
}
