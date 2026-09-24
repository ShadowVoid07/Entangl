package `in`.grayscales.entangl.core.crypto

import `in`.grayscales.entangl.core.util.currentTimeMillis
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ContactMigrationItem(
    val uid: String,
    val publicKey: ByteArray,
    val onionAddress: String,
    val safetyNumber: String,
    val displayName: String? = null,
    val createdAt: Long,
    val lastSeenAt: Long? = null,
    val isAccepted: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContactMigrationItem) return false
        return uid == other.uid &&
            publicKey.contentEquals(other.publicKey) &&
            onionAddress == other.onionAddress &&
            safetyNumber == other.safetyNumber &&
            displayName == other.displayName &&
            createdAt == other.createdAt &&
            lastSeenAt == other.lastSeenAt &&
            isAccepted == other.isAccepted
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + onionAddress.hashCode()
        result = 31 * result + safetyNumber.hashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (lastSeenAt?.hashCode() ?: 0)
        result = 31 * result + isAccepted.hashCode()
        return result
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MessageMigrationItem(
    val id: String,
    val contactUid: String,
    val ciphertext: ByteArray,
    val direction: Int,
    val status: Int,
    val timestamp: Long,
    val selfDestructAt: Long?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageMigrationItem) return false
        return id == other.id &&
            contactUid == other.contactUid &&
            ciphertext.contentEquals(other.ciphertext) &&
            direction == other.direction &&
            status == other.status &&
            timestamp == other.timestamp &&
            selfDestructAt == other.selfDestructAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + contactUid.hashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + direction
        result = 31 * result + status
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (selfDestructAt?.hashCode() ?: 0)
        return result
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SessionKeyMigrationItem(
    val contactUid: String,
    val sessionKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionKeyMigrationItem) return false
        return contactUid == other.contactUid && sessionKey.contentEquals(other.sessionKey)
    }

    override fun hashCode(): Int {
        var result = contactUid.hashCode()
        result = 31 * result + sessionKey.contentHashCode()
        return result
    }
}

/**
 * Complete, authenticated bundle transferred from Old Device to New Device.
 * Transmitted exclusively over an encrypted P2P tunnel (AES-256-GCM).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeviceMigrationPayload(
    val version: Int = 1,
    val senderUid: String,
    val contacts: List<ContactMigrationItem>,
    val messages: List<MessageMigrationItem>,
    val sessionKeys: List<SessionKeyMigrationItem>,
    val certificate: SuccessionCertificate?,
    val timestamp: Long = currentTimeMillis()
) {
    fun toCbor(): ByteArray = Cbor.encodeToByteArray(serializer(), this)

    companion object {
        fun fromCbor(bytes: ByteArray): DeviceMigrationPayload? = try {
            Cbor.decodeFromByteArray(serializer(), bytes)
        } catch (_: Exception) {
            null
        }
    }
}
