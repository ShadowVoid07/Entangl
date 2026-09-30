package `in`.grayscales.entangl.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.grayscales.entangl.data.local.entity.ProcessedEnvelope

@Dao
interface ProcessedEnvelopeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(envelope: ProcessedEnvelope)

    @Query("SELECT COUNT(*) > 0 FROM processed_envelopes WHERE dedupKey = :dedupKey")
    suspend fun exists(dedupKey: String): Boolean

    @Query("DELETE FROM processed_envelopes WHERE timestamp < :olderThan")
    suspend fun pruneOlderThan(olderThan: Long)
}
