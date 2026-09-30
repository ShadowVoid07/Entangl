package `in`.grayscales.entangl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import `in`.grayscales.entangl.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    // IGNORE + UPDATE (never REPLACE): same DELETE+INSERT hazard class as contacts.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    @Update
    suspend fun update(message: MessageEntity)

    @Transaction
    suspend fun insertOrUpdate(message: MessageEntity) {
        if (insert(message) == -1L) {
            update(message)
        }
    }

    @Query("SELECT * FROM messages WHERE contactUid = :contactUid ORDER BY timestamp ASC")
    fun observeForContact(contactUid: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE contactUid = :contactUid ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestForContact(contactUid: String): Flow<MessageEntity?>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<MessageEntity>

    @Query("UPDATE messages SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: Int)

    @Query("DELETE FROM messages WHERE selfDestructAt IS NOT NULL AND selfDestructAt <= :now")
    suspend fun deleteExpired(now: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE status = 0")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM messages WHERE contactUid = :contactUid AND (direction = 0 OR direction = 2) AND status = 0")
    suspend fun getPendingIncomingForContact(contactUid: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE contactUid = :contactUid ORDER BY timestamp ASC")
    suspend fun getMessagesForContact(contactUid: String): List<MessageEntity>

    @Query("SELECT COUNT(*) > 0 FROM messages WHERE contactUid = :contactUid AND id LIKE 'accept-%'")
    suspend fun hasAcceptanceNotice(contactUid: String): Boolean

    @Query("SELECT * FROM messages WHERE direction = 1 AND status = 0")
    suspend fun getPendingOutgoingMessages(): List<MessageEntity>

    @Query("SELECT COUNT(*) > 0 FROM messages WHERE id = :id")
    suspend fun existsById(id: String): Boolean

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages WHERE contactUid = :contactUid")
    suspend fun deleteForContact(contactUid: String)
}
