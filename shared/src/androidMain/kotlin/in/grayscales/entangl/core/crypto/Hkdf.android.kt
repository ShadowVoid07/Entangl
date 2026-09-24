package `in`.grayscales.entangl.core.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

actual object Hkdf {
    private const val HMAC_ALGO = "HmacSHA256"
    private const val HASH_LEN = 32

    actual fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length <= 255 * HASH_LEN) { "Requested length too large for HKDF-SHA256" }

        // 1. Extract: PRK = HMAC-Hash(salt, IKM)
        val effectiveSalt = if (salt.isNotEmpty()) salt else ByteArray(HASH_LEN)
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(effectiveSalt, HMAC_ALGO))
        val prk = mac.doFinal(ikm)

        // 2. Expand: T(1) || T(2) || ... || T(N)
        val n = ceil(length.toDouble() / HASH_LEN).toInt()
        val result = ByteArray(length)
        var t = ByteArray(0)
        var bytesWritten = 0

        for (i in 1..n) {
            mac.init(SecretKeySpec(prk, HMAC_ALGO))
            mac.update(t)
            if (info.isNotEmpty()) {
                mac.update(info)
            }
            mac.update(i.toByte())
            t = mac.doFinal()

            val toCopy = minOf(HASH_LEN, length - bytesWritten)
            System.arraycopy(t, 0, result, bytesWritten, toCopy)
            bytesWritten += toCopy
        }

        return result
    }
}
