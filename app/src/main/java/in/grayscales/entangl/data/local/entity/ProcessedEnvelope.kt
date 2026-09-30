package `in`.grayscales.entangl.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Exactly-once processing record for inbound transport envelopes.
 *
 * Relays retain 24h of history and in-memory dedup dies with the process, so
 * every app start replays backlog. Without this table, replays re-fire
 * notifications, duplicate scan notices, and repeat ACK storms. Rows prune
 * past the relay retention window; reinstalls start empty alongside the
 * fresh identity (new inbox topic), so no stale suppression carries over.
 */
@Entity(tableName = "processed_envelopes")
data class ProcessedEnvelope(
    @PrimaryKey
    val dedupKey: String,
    val timestamp: Long
) {
    companion object {
        fun keyOf(type: String, id: String): String = "$type:$id"
    }
}
