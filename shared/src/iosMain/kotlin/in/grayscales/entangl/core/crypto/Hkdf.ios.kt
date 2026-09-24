package `in`.grayscales.entangl.core.crypto

actual object Hkdf {
    actual fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        throw UnsupportedOperationException("iOS HKDF implementation pending")
    }
}
