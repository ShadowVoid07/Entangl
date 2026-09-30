package `in`.grayscales.entangl.core.identity

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Manages persistent local node identity (UID and .onion address)
 * securely persisted using EncryptedSharedPreferences backed by Android Keystore.
 * Automatically migrates from legacy unencrypted storage if present.
 */
class NodeIdentityManager(context: Context) {

    private val prefs: SharedPreferences = run {
        val encryptedPrefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w("NodeIdentityManager", "Failed to initialize EncryptedSharedPreferences, falling back to private SharedPreferences: ${e.message}")
            context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        }

        migrateLegacyPrefsIfPresent(context, encryptedPrefs)
        encryptedPrefs
    }

    val localUid: String
        get() {
            var uid = prefs.getString(KEY_UID, null)
            if (uid == null) {
                uid = "node-" + UUID.randomUUID().toString().replace("-", "").take(12)
                prefs.edit { putString(KEY_UID, uid) }
            }
            return uid
        }

    val localOnion: String
        get() {
            var onion = prefs.getString(KEY_ONION, null)
            if (onion == null) {
                onion = "mesh-" + UUID.randomUUID().toString().replace("-", "").take(16) + ".entangl.net"
                prefs.edit { putString(KEY_ONION, onion) }
            }
            return onion
        }

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) {
            val sanitized = value?.trim()?.take(MAX_USERNAME_LENGTH)
            if (sanitized.isNullOrEmpty()) {
                prefs.edit { remove(KEY_USERNAME) }
            } else {
                prefs.edit { putString(KEY_USERNAME, sanitized) }
            }
        }

    var profileColor: String
        get() = prefs.getString(KEY_PROFILE_COLOR, DEFAULT_PROFILE_COLOR) ?: DEFAULT_PROFILE_COLOR
        set(value) {
            val sanitized = sanitizeHexColor(value)
            prefs.edit { putString(KEY_PROFILE_COLOR, sanitized) }
        }

    val isUsernameSet: Boolean
        get() = !username.isNullOrBlank()

    companion object {
        const val MAX_USERNAME_LENGTH = 25
        const val DEFAULT_PROFILE_COLOR = "#00F0FF"
        const val LEGACY_PREFS_NAME = "entangl_node_identity"
        const val ENCRYPTED_PREFS_NAME = "entangl_node_identity_encrypted"
        private const val KEY_UID = "local_node_uid"
        private const val KEY_ONION = "local_node_onion"
        private const val KEY_USERNAME = "local_node_username"
        private const val KEY_PROFILE_COLOR = "local_node_profile_color"
        private val HEX_REGEX = Regex("^[0-9A-Fa-f]{6}$")

        private fun migrateLegacyPrefsIfPresent(context: Context, targetPrefs: SharedPreferences) {
            try {
                val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                val allLegacy = legacy.all
                if (allLegacy.isNotEmpty()) {
                    targetPrefs.edit {
                        allLegacy.forEach { (key, value) ->
                            when (value) {
                                is String -> putString(key, value)
                                is Boolean -> putBoolean(key, value)
                                is Int -> putInt(key, value)
                                is Long -> putLong(key, value)
                                is Float -> putFloat(key, value)
                            }
                        }
                    }
                    legacy.edit { clear() }
                    Log.d("NodeIdentityManager", "Migrated legacy unencrypted identity prefs to encrypted storage")
                }
            } catch (e: Exception) {
                Log.w("NodeIdentityManager", "Legacy prefs migration check failed: ${e.message}")
            }
        }

        fun sanitizeHexColor(hex: String?): String {
            if (hex.isNullOrBlank()) return DEFAULT_PROFILE_COLOR
            val cleaned = hex.trim().removePrefix("#")
            return if (HEX_REGEX.matches(cleaned)) {
                "#${cleaned.uppercase()}"
            } else {
                DEFAULT_PROFILE_COLOR
            }
        }
    }
}
