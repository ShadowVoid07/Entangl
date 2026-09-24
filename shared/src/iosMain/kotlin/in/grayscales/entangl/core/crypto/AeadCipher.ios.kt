package `in`.grayscales.entangl.core.crypto

actual object AeadCipher {
    actual fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        throw UnsupportedOperationException("iOS AEAD implementation pending")
    }

    actual fun decrypt(key: ByteArray, payload: ByteArray, aad: ByteArray): ByteArray {
        throw UnsupportedOperationException("iOS AEAD implementation pending")
    }
}
