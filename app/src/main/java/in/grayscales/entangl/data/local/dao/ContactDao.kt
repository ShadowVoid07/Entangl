package `in`.grayscales.entangl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import `in`.grayscales.entangl.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    // NEVER use REPLACE here: SQLite implements REPLACE as DELETE + INSERT, which
    // fires the messages FK CASCADE and silently wipes the peer's entire history
    // on every contact save (scan, ping, unlock). IGNORE + UPDATE preserves children.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: ContactEntity): Long

    @Update
    suspend fun update(contact: ContactEntity)

    @Transaction
    suspend fun insertOrUpdate(contact: ContactEntity) {
        if (insert(contact) == -1L) {
            update(contact)
        }
    }

    @Query("SELECT * FROM contacts WHERE uid = :uid")
    suspend fun getByUid(uid: String): ContactEntity?

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    suspend fun getAll(): List<ContactEntity>

    @Query("DELETE FROM contacts WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)

    @Query("UPDATE contacts SET lastSeenAt = :timestamp WHERE uid = :uid")
    suspend fun updateLastSeen(uid: String, timestamp: Long)

    @Query("UPDATE contacts SET isAccepted = :isAccepted WHERE uid = :uid")
    suspend fun updateAcceptance(uid: String, isAccepted: Boolean)
}
