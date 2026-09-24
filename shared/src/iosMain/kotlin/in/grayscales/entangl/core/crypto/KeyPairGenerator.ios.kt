package `in`.grayscales.entangl.core.crypto

actual class KeyPairGenerator actual constructor() {

    actual fun generateIdentityKeyPair(): ByteArray {
        throw UnsupportedOperationException("iOS KeyPairGenerator implementation pending")
    }

    actual fun generateEphemeralX25519(): Pair<ByteArray, NativeKeyBuffer> {
        throw UnsupportedOperationException("iOS KeyPairGenerator implementation pending")
    }

    actual fun getStoredIdentityPublicKey(): ByteArray? = null

    actual fun sign(data: ByteArray): ByteArray {
        throw UnsupportedOperationException("iOS KeyPairGenerator implementation pending")
    }

    actual fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        throw UnsupportedOperationException("iOS KeyPairGenerator implementation pending")
    }

    actual fun computeX25519KeyAgreement(privateKeyBuffer: NativeKeyBuffer, peerPublicKey: ByteArray): ByteArray {
        throw UnsupportedOperationException("iOS KeyPairGenerator implementation pending")
    }
}
