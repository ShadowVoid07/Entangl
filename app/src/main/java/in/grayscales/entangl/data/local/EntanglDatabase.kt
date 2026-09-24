package `in`.grayscales.entangl.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import `in`.grayscales.entangl.data.local.converter.CryptoTypeConverters
import `in`.grayscales.entangl.data.local.dao.ContactDao
import `in`.grayscales.entangl.data.local.dao.MessageDao
import `in`.grayscales.entangl.data.local.entity.ContactEntity
import `in`.grayscales.entangl.data.local.entity.MessageEntity
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Database(
    entities = [ContactEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(CryptoTypeConverters::class)
abstract class EntanglDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao

    companion object {
        private const val DB_NAME = "entangl_vault.db"
        private const val KEYSTORE_ALIAS = "entangl_sqlcipher_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        @Volatile
        private var INSTANCE: EntanglDatabase? = null

        fun getInstance(context: Context): EntanglDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): EntanglDatabase {
            val passphrase = getOrCreateDatabasePassphrase(context)
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                EntanglDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(true)
                .build()
        }

        private fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val prefs = context.getSharedPreferences("entangl_db_meta", Context.MODE_PRIVATE)

            val existingKey = prefs.getString("encrypted_passphrase", null)
            val existingIv = prefs.getString("passphrase_iv", null)
            if (existingKey != null && existingIv != null && keyStore.containsAlias(KEYSTORE_ALIAS)) {
                try {
                    val encryptedBytes = android.util.Base64.decode(existingKey, android.util.Base64.NO_WRAP)
                    val iv = android.util.Base64.decode(existingIv, android.util.Base64.NO_WRAP)
                    val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                    return cipher.doFinal(encryptedBytes)
                } catch (_: Exception) {
                    // Fall through to regenerate if decryption fails
                }
            }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val specBuilder = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)

            val secretKey = try {
                keyGenerator.init(specBuilder.setIsStrongBoxBacked(true).build())
                keyGenerator.generateKey()
            } catch (_: Exception) {
                keyGenerator.init(specBuilder.setIsStrongBoxBacked(false).build())
                keyGenerator.generateKey()
            }

            val rawPassphrase = ByteArray(32)
            java.security.SecureRandom().nextBytes(rawPassphrase)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedPassphrase = cipher.doFinal(rawPassphrase)
            val iv = cipher.iv

            prefs.edit {
                putString("encrypted_passphrase", android.util.Base64.encodeToString(encryptedPassphrase, android.util.Base64.NO_WRAP))
                putString("passphrase_iv", android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
            }

            return rawPassphrase
        }
    }
}
