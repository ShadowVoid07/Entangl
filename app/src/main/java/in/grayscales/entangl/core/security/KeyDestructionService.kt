package `in`.grayscales.entangl.core.security

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import `in`.grayscales.entangl.data.network.NetworkTransport
import java.security.KeyStore

/**
 * Executes a permanent, one-way cryptographic self-destruct and local data wipe.
 * Invoked immediately after a successful, verified device migration handoff.
 */
class KeyDestructionService(
    private val context: Context,
    private val networkTransport: NetworkTransport
) {

    fun decommissionDevice(): Boolean {
        Log.w("KeyDestructionService", "INITIATING ATOMIC DEVICE DECOMMISSION AND CRYPTOGRAPHIC SELF-DESTRUCT")

        var success = true

        // 1. Stop active network listeners and transports
        try {
            networkTransport.stopListening()
        } catch (e: Exception) {
            Log.e("KeyDestructionService", "Error stopping transport: ${e.message}")
        }

        // 2. Wipe Android Keystore aliases
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val aliases = keyStore.aliases().toList()
            for (alias in aliases) {
                if (alias.startsWith("entangl_")) {
                    keyStore.deleteEntry(alias)
                    Log.i("KeyDestructionService", "Zeroized Keystore alias: $alias")
                }
            }
        } catch (e: Exception) {
            Log.e("KeyDestructionService", "Error wiping Keystore: ${e.message}")
            success = false
        }

        // 3. Close and wipe SQLCipher database files
        try {
            context.deleteDatabase("entangl.db")
            context.deleteDatabase("entangl.db-wal")
            context.deleteDatabase("entangl.db-shm")
            Log.i("KeyDestructionService", "Permanently deleted SQLCipher database files")
        } catch (e: Exception) {
            Log.e("KeyDestructionService", "Error deleting databases: ${e.message}")
            success = false
        }

        // 4. Wipe SharedPreferences
        val prefsToClear = listOf(
            "entangl_node_identity",
            "entangl_db_meta",
            "entangl_session_keys_encrypted",
            "entangl_ratchet_hmacs"
        )
        for (prefName in prefsToClear) {
            try {
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit { clear() }
                Log.i("KeyDestructionService", "Wiped SharedPreferences: $prefName")
            } catch (e: Exception) {
                Log.e("KeyDestructionService", "Error clearing pref $prefName: ${e.message}")
            }
        }

        // 5. Mark device as permanently decommissioned
        try {
            context.getSharedPreferences(LIFECYCLE_PREFS, Context.MODE_PRIVATE).edit {
                putBoolean(KEY_DECOMMISSIONED, true)
            }
        } catch (e: Exception) {
            Log.e("KeyDestructionService", "Error writing decommissioned flag: ${e.message}")
        }

        Log.w("KeyDestructionService", "DEVICE PERMANENTLY DECOMMISSIONED. ZERO RECOVERY POSSIBLE.")
        return success
    }

    fun isDeviceDecommissioned(): Boolean {
        return context.getSharedPreferences(LIFECYCLE_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DECOMMISSIONED, false)
    }

    companion object {
        const val LIFECYCLE_PREFS = "entangl_lifecycle"
        const val KEY_DECOMMISSIONED = "is_decommissioned"
    }
}
