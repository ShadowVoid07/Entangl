package `in`.grayscales.entangl.core.identity

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/**
 * Manages persistent local node identity (UID and .onion address)
 * stored locally so a device maintains its cryptographic identity across app launches.
 */
class NodeIdentityManager(context: Context) {

    private val prefs = context.getSharedPreferences("entangl_node_identity", Context.MODE_PRIVATE)

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
        private const val KEY_UID = "local_node_uid"
        private const val KEY_ONION = "local_node_onion"
        private const val KEY_USERNAME = "local_node_username"
        private const val KEY_PROFILE_COLOR = "local_node_profile_color"
        private val HEX_REGEX = Regex("^[0-9A-Fa-f]{6}$")

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
