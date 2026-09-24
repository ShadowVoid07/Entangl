package `in`.grayscales.entangl.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import `in`.grayscales.entangl.core.util.zeroize
import java.security.KeyStore
import java.security.Signature
import java.security.KeyPairGenerator as JavaKeyPairGenerator

/**
 * Android implementation of [KeyPairGenerator].
 * Identity keys are generated in Android Keystore (StrongBox preferred).
 * Ephemeral X25519 keys are generated via libsignal-client and held in NativeKeyBuffer.
 */
actual class KeyPairGenerator actual constructor() {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val IDENTITY_KEY_ALIAS = "entangl_identity_ed25519"
        @Volatile
        private var fallbackKeyPair: java.security.KeyPair? = null
    }

    private val isAndroidKeyStoreAvailable: Boolean = try {
        KeyStore.getInstance(KEYSTORE_PROVIDER) != null
    } catch (_: Exception) {
        false
    }

    private val keyStore: KeyStore? = if (isAndroidKeyStoreAvailable) {
        try {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        } catch (_: Exception) {
            null
        }
    } else null

    actual fun generateIdentityKeyPair(): ByteArray {
        val ks = keyStore
        if (ks == null) {
            val kpg = JavaKeyPairGenerator.getInstance("EC")
            kpg.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
            val kp = kpg.generateKeyPair()
            fallbackKeyPair = kp
            return kp.public.encoded
        }

        if (ks.containsAlias(IDENTITY_KEY_ALIAS)) {
            ks.deleteEntry(IDENTITY_KEY_ALIAS)
        }

        val keyPairGenerator = JavaKeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )
        val specBuilder = KeyGenParameterSpec.Builder(
            IDENTITY_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)

        val keyPair = try {
            keyPairGenerator.initialize(specBuilder.setIsStrongBoxBacked(true).build())
            keyPairGenerator.generateKeyPair()
        } catch (_: Exception) {
            // Fall back to standard TEE if StrongBox is unavailable (e.g., emulators)
            keyPairGenerator.initialize(
                KeyGenParameterSpec.Builder(
                    IDENTITY_KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setIsStrongBoxBacked(false)
                    .build()
            )
            keyPairGenerator.generateKeyPair()
        }
        return keyPair.public.encoded
    }

    actual fun generateEphemeralX25519(): Pair<ByteArray, NativeKeyBuffer> {
        // Use libsignal-client for X25519 key generation
        // For now, generate via java.security and wrap in NativeKeyBuffer
        val kpg = JavaKeyPairGenerator.getInstance("X25519")
        val keyPair = kpg.generateKeyPair()

        val publicKeyBytes = keyPair.public.encoded
        val privateKeyBytes = keyPair.private.encoded

        val buffer = NativeKeyBuffer(privateKeyBytes.size)
        buffer.put(privateKeyBytes)

        // Zeroize the JVM copy immediately
        privateKeyBytes.zeroize()

        return Pair(publicKeyBytes, buffer)
    }

    actual fun getStoredIdentityPublicKey(): ByteArray? {
        val ks = keyStore
        if (ks == null) {
            return fallbackKeyPair?.public?.encoded
        }
        if (!ks.containsAlias(IDENTITY_KEY_ALIAS)) return null
        val entry = ks.getCertificate(IDENTITY_KEY_ALIAS) ?: return null
        return entry.publicKey.encoded
    }

    actual fun sign(data: ByteArray): ByteArray {
        val privateKey = if (keyStore == null) {
            fallbackKeyPair?.private
        } else {
            keyStore.getKey(IDENTITY_KEY_ALIAS, null) as? java.security.PrivateKey
        } ?: throw IllegalStateException("Identity key not found in Keystore")

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    actual fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        val keyFactory = java.security.KeyFactory.getInstance("EC")
        val pubKeySpec = java.security.spec.X509EncodedKeySpec(publicKey)
        val pubKey = keyFactory.generatePublic(pubKeySpec)

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(pubKey)
        sig.update(data)
        return sig.verify(signature)
    }

    actual fun computeX25519KeyAgreement(privateKeyBuffer: NativeKeyBuffer, peerPublicKey: ByteArray): ByteArray {
        val privateKeyBytes = privateKeyBuffer.get()
        try {
            return try {
                val kf = java.security.KeyFactory.getInstance("X25519")
                val privKey = kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes))

                val pubKeyBytes = if (peerPublicKey.size == 32) {
                    // Standard 12-byte ASN.1 X.509 prefix for Curve25519
                    byteArrayOf(
                        0x30.toByte(), 0x2a.toByte(), 0x30.toByte(), 0x05.toByte(),
                        0x06.toByte(), 0x03.toByte(), 0x2b.toByte(), 0x65.toByte(),
                        0x6e.toByte(), 0x03.toByte(), 0x21.toByte(), 0x00.toByte()
                    ) + peerPublicKey
                } else {
                    peerPublicKey
                }

                val pubKey = kf.generatePublic(java.security.spec.X509EncodedKeySpec(pubKeyBytes))
                val ka = javax.crypto.KeyAgreement.getInstance("X25519")
                ka.init(privKey)
                ka.doPhase(pubKey, true)
                ka.generateSecret()
            } catch (_: Exception) {
                // Secondary attempt with "XDH" algorithm name (supported on some Android runtimes)
                val kf = java.security.KeyFactory.getInstance("XDH")
                val privKey = kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes))
                val pubKeyBytes = if (peerPublicKey.size == 32) {
                    byteArrayOf(
                        0x30.toByte(), 0x2a.toByte(), 0x30.toByte(), 0x05.toByte(),
                        0x06.toByte(), 0x03.toByte(), 0x2b.toByte(), 0x65.toByte(),
                        0x6e.toByte(), 0x03.toByte(), 0x21.toByte(), 0x00.toByte()
                    ) + peerPublicKey
                } else {
                    peerPublicKey
                }
                val pubKey = kf.generatePublic(java.security.spec.X509EncodedKeySpec(pubKeyBytes))
                val ka = javax.crypto.KeyAgreement.getInstance("XDH")
                ka.init(privKey)
                ka.doPhase(pubKey, true)
                ka.generateSecret()
            }
        } finally {
            privateKeyBytes.zeroize()
        }
    }
}
