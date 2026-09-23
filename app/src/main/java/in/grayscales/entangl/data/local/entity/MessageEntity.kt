package `in`.grayscales.entangl.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import `in`.grayscales.entangl.domain.model.Direction
import `in`.grayscales.entangl.domain.model.Message
import `in`.grayscales.entangl.domain.model.MessageStatus

/**
 * Encrypted message stored inside SQLCipher.
 *
 * CRITICAL SECURITY ARCHITECTURE (DOUBLE ENCRYPTION):
 * 1. Inner Layer: The message plaintext is encrypted using the Double Ratchet (forward secret session key).
 * 2. Outer Layer: The resulting [ciphertext] blob is stored in SQLCipher (AES-256-GCM page encryption).
 * 3. In-memory only: [Message.plaintext] is NEVER written to disk.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["uid"],
            childColumns = ["contactUid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contactUid"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val contactUid: String,
    val ciphertext: ByteArray,
    val direction: Int, // 0 = INCOMING, 1 = OUTGOING
    val status: Int,    // 0 = PENDING, 1 = SENT, 2 = DELIVERED, 3 = READ
    val timestamp: Long,
    val selfDestructAt: Long?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    fun toDomain(decryptedPlaintext: String): Message {
        return Message(
            id = id,
            contactUid = contactUid,
            plaintext = decryptedPlaintext,
            direction = when (direction) {
                1 -> Direction.OUTGOING
                else -> Direction.INCOMING
            },
            status = when (status) {
                1 -> MessageStatus.SENT
                2 -> MessageStatus.DELIVERED
                3 -> MessageStatus.READ
                else -> MessageStatus.PENDING
            },
            timestamp = timestamp,
            selfDestructAt = selfDestructAt
        )
    }

    companion object {
        fun fromDomain(message: Message, ciphertext: ByteArray): MessageEntity {
            return MessageEntity(
                id = message.id,
                contactUid = message.contactUid,
                ciphertext = ciphertext,
                direction = if (message.direction == Direction.OUTGOING) 1 else 0,
                status = when (message.status) {
                    MessageStatus.SENT -> 1
                    MessageStatus.DELIVERED -> 2
                    MessageStatus.READ -> 3
                    MessageStatus.PENDING -> 0
                },
                timestamp = message.timestamp,
                selfDestructAt = message.selfDestructAt
            )
        }
    }
}
