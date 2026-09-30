package `in`.grayscales.entangl.domain.repository

import `in`.grayscales.entangl.domain.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for encrypted message persistence.
 * Messages are double-encrypted: ratchet-encrypted ciphertext stored in SQLCipher.
 * Defined in domain (commonMain), implemented in data layer (androidMain).
 */
interface MessageRepository {

    /** Encrypt and persist an outgoing message. */
    suspend fun send(contactUid: String, plaintext: String)

    /** Encrypt and persist an outgoing message with ephemeral self-destruct. */
    suspend fun send(contactUid: String, plaintext: String, selfDestructAt: Long?)

    /** Store a received (already-decrypted) message. */
    suspend fun receiveAndStore(message: Message)

    /** Mark a message as delivered. */
    suspend fun markDelivered(messageId: String)

    /** Mark a message as read. */
    suspend fun markRead(messageId: String)

    /** Observe messages for a specific contact as a reactive stream. */
    fun observeForContact(contactUid: String): Flow<List<Message>>

    /** Observe only the latest message for roster previews (single decrypt). */
    fun observeLastMessage(contactUid: String): Flow<Message?>

    /** Delete messages that have passed their self-destruct time. */
    suspend fun deleteExpired()

    /** Delete a single message by id (TTL vaporization). */
    suspend fun deleteMessage(messageId: String)

    /** Get count of pending (unsent) messages. */
    suspend fun getPendingCount(): Int

    /** Decrypt and deliver all pending messages for a newly connected contact. */
    suspend fun unlockPendingMessages(contactUid: String)

    /** Delete all stored messages for a contact (clear chat history). */
    suspend fun clearChat(contactUid: String)
}
