package `in`.grayscales.entangl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.grayscales.entangl.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE uid = :uid")
    suspend fun getByUid(uid: String): ContactEntity?

    @Query("SELECT * FROM contacts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("DELETE FROM contacts WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)

    @Query("UPDATE contacts SET lastSeenAt = :timestamp WHERE uid = :uid")
    suspend fun updateLastSeen(uid: String, timestamp: Long)

    @Query("UPDATE contacts SET isAccepted = :isAccepted WHERE uid = :uid")
    suspend fun updateAcceptance(uid: String, isAccepted: Boolean)
}
