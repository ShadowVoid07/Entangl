package `in`.grayscales.entangl.core.identity

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import `in`.grayscales.entangl.core.crypto.DefaultCryptoManager
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Securely persists Double Ratchet session keys using EncryptedSharedPreferences,
 * backed by Android Keystore's AES-256 MasterKey.
 *
 * This ensures session keys survive app restarts and process death
 * while remaining encrypted at rest — only this app can read them.
 *
 * Keys are stored as Base64-encoded strings, keyed by contact UID.
 */
@OptIn(ExperimentalEncodingApi::class)
class SessionKeyStore(context: Context) : DefaultCryptoManager.SessionKeyPersistence {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Store a session key for a contact UID.
     */
    override fun storeKey(contactUid: String, sessionKey: ByteArray) {
        val encoded = Base64.encode(sessionKey)
        prefs.edit { putString(keyForUid(contactUid), encoded) }
    }

    /**
     * Retrieve a previously stored session key for a contact UID.
     * @return The session key bytes, or null if no key is stored for this UID.
     */
    override fun loadKey(contactUid: String): ByteArray? {
        val encoded = prefs.getString(keyForUid(contactUid), null) ?: return null
        return try {
            Base64.decode(encoded)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete the session key for a contact UID.
     */
    override fun deleteKey(contactUid: String) {
        prefs.edit { remove(keyForUid(contactUid)) }
    }

    /**
     * Load all stored session keys.
     * @return Map of contactUid to session key bytes.
     */
    override fun loadAll(): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        prefs.all.forEach { (prefKey, value) ->
            if (prefKey.startsWith(KEY_PREFIX) && value is String) {
                val contactUid = prefKey.removePrefix(KEY_PREFIX)
                try {
                    result[contactUid] = Base64.decode(value)
                } catch (_: Exception) {
                    // Skip corrupted entries
                }
            }
        }
        return result
    }

    private fun keyForUid(contactUid: String): String = KEY_PREFIX + contactUid

    companion object {
        private const val PREFS_FILENAME = "entangl_session_keys_encrypted"
        private const val KEY_PREFIX = "sk_"
    }
}
