package `in`.grayscales.entangl.core.util

import java.security.MessageDigest

actual object Sha256Digest {
    actual fun digest(input: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input)
    }
}
