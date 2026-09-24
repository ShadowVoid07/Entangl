package `in`.grayscales.entangl.core.crypto

import `in`.grayscales.entangl.core.util.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object AeadCipher {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128
    private const val MIN_PAYLOAD_BYTES = IV_LENGTH_BYTES + (TAG_LENGTH_BITS / 8) // 28 bytes

    actual fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        require(key.size >= 32) { "AES-256 requires at least a 32-byte key (got ${key.size})" }
        val effectiveKey = if (key.size == 32) key else key.copyOf(32)
        val iv = SecureRandom.nextBytes(IV_LENGTH_BYTES)

        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(effectiveKey, "AES")
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        if (aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }

        val ciphertextWithTag = cipher.doFinal(plaintext)
        return iv + ciphertextWithTag
    }

    actual fun decrypt(key: ByteArray, payload: ByteArray, aad: ByteArray): ByteArray {
        require(payload.size >= MIN_PAYLOAD_BYTES) {
            "Payload too short for AES-GCM (${payload.size} < $MIN_PAYLOAD_BYTES)"
        }
        require(key.size >= 32) { "AES-256 requires at least a 32-byte key (got ${key.size})" }
        val effectiveKey = if (key.size == 32) key else key.copyOf(32)

        val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertextWithTag = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(effectiveKey, "AES")
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        if (aad.isNotEmpty()) {
            cipher.updateAAD(aad)
        }

        return cipher.doFinal(ciphertextWithTag)
    }
}
