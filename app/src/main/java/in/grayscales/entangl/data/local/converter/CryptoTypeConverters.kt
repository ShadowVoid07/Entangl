package `in`.grayscales.entangl.data.local.converter

import androidx.room.TypeConverter

/**
 * Type converters for Room to handle ByteArrays if stored as hex.
 * Note: SQLite natively supports BLOB for ByteArray, but converters provide
 * fallback and serialization compatibility.
 */
class CryptoTypeConverters {

    @TypeConverter
    fun fromByteArray(bytes: ByteArray?): String? {
        return bytes?.joinToString("") { "%02x".format(it) }
    }

    @TypeConverter
    fun toByteArray(hex: String?): ByteArray? {
        if (hex == null || hex.isEmpty()) return null
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
