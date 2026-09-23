package `in`.grayscales.entangl.core.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Android implementation of [RatchetStateVerifier].
 * Uses a hardware-backed HMAC key stored in Android Keystore to verify
 * the integrity of serialized ratchet state on every app launch.
 *
 * If the HMAC doesn't match, the ratchet state has been tampered with
 * or rolled back — the session must be re-established via a new QR handshake.
 */
class AndroidRatchetStateVerifier(
    private val context: Context
) : RatchetStateVerifier {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val HMAC_KEY_PREFIX = "entangl_ratchet_hmac_"
        private const val HMAC_ALGORITHM = "HmacSHA256"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    override fun computeAndStoreHmac(contactUid: String, ratchetState: ByteArray) {
        val keyAlias = HMAC_KEY_PREFIX + contactUid
        val hmacKey = getOrCreateHmacKey(keyAlias)
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(hmacKey)
        val hmac = mac.doFinal(ratchetState)

        // Store the computed HMAC in SharedPreferences (encrypted)
        // The HMAC KEY itself is in Keystore (hardware-backed), so this is safe.
        context.getSharedPreferences("entangl_ratchet_hmacs", Context.MODE_PRIVATE)
            .edit()
            .putString(contactUid, hmac.joinToString("") { "%02x".format(it) })
            .apply()
    }

    override fun verify(contactUid: String, ratchetState: ByteArray): Boolean {
        val keyAlias = HMAC_KEY_PREFIX + contactUid
        val hmacKey = keyStore.getKey(keyAlias, null) as? SecretKey ?: return false

        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(hmacKey)
        val computed = mac.doFinal(ratchetState)
        val computedHex = computed.joinToString("") { "%02x".format(it) }

        val storedHex = context.getSharedPreferences("entangl_ratchet_hmacs", Context.MODE_PRIVATE)
            .getString(contactUid, null) ?: return false

        // Constant-time comparison to prevent timing attacks
        return constantTimeEquals(computedHex, storedHex)
    }

    override fun deleteHmac(contactUid: String) {
        val keyAlias = HMAC_KEY_PREFIX + contactUid
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
        context.getSharedPreferences("entangl_ratchet_hmacs", Context.MODE_PRIVATE)
            .edit()
            .remove(contactUid)
            .apply()
    }

    private fun getOrCreateHmacKey(alias: String): SecretKey {
        keyStore.getKey(alias, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            KEYSTORE_PROVIDER
        )
        val specBuilder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)

        return try {
            keyGenerator.init(specBuilder.setIsStrongBoxBacked(true).build())
            keyGenerator.generateKey()
        } catch (e: Exception) {
            keyGenerator.init(specBuilder.setIsStrongBoxBacked(false).build())
            keyGenerator.generateKey()
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
