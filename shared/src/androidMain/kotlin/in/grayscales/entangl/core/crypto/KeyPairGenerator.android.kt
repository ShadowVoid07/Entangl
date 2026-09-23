package `in`.grayscales.entangl.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import `in`.grayscales.entangl.core.util.zeroize
import java.security.KeyPairGenerator as JavaKeyPairGenerator
import java.security.KeyStore
import java.security.Signature

/**
 * Android implementation of [KeyPairGenerator].
 * Identity keys are generated in Android Keystore (StrongBox preferred).
 * Ephemeral X25519 keys are generated via libsignal-client and held in NativeKeyBuffer.
 */
actual class KeyPairGenerator actual constructor() {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val IDENTITY_KEY_ALIAS = "entangl_identity_ed25519"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    actual fun generateIdentityKeyPair(): ByteArray {
        if (keyStore.containsAlias(IDENTITY_KEY_ALIAS)) {
            keyStore.deleteEntry(IDENTITY_KEY_ALIAS)
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
        } catch (e: Exception) {
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
        if (!keyStore.containsAlias(IDENTITY_KEY_ALIAS)) return null
        val entry = keyStore.getCertificate(IDENTITY_KEY_ALIAS) ?: return null
        return entry.publicKey.encoded
    }

    actual fun sign(data: ByteArray): ByteArray {
        val privateKey = keyStore.getKey(IDENTITY_KEY_ALIAS, null)
            ?: throw IllegalStateException("Identity key not found in Keystore")

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey as java.security.PrivateKey)
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
}
