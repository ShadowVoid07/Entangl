package `in`.grayscales.entangl.domain.model

/**
 * Represents a single message in a conversation.
 * Messages are stored double-encrypted: ratchet-encrypted blobs inside SQLCipher.
 * In memory, plaintext is only available after decryption.
 */
data class Message(
    val id: String,
    val contactUid: String,
    val plaintext: String,
    val direction: Direction,
    val status: MessageStatus,
    val timestamp: Long,
    val selfDestructAt: Long?
)

enum class Direction {
    INCOMING,
    OUTGOING
}

enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    READ
}
