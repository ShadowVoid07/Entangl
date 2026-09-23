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

    /** Store a received (already-decrypted) message. */
    suspend fun receiveAndStore(message: Message)

    /** Mark a message as delivered. */
    suspend fun markDelivered(messageId: String)

    /** Mark a message as read. */
    suspend fun markRead(messageId: String)

    /** Observe messages for a specific contact as a reactive stream. */
    fun observeForContact(contactUid: String): Flow<List<Message>>

    /** Delete messages that have passed their self-destruct time. */
    suspend fun deleteExpired()

    /** Get count of pending (unsent) messages. */
    suspend fun getPendingCount(): Int

    /** Decrypt and deliver all pending messages for a newly connected contact. */
    suspend fun unlockPendingMessages(contactUid: String)
}
