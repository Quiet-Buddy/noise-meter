package com.quietbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMinute(row: LevelMinuteEntity)

    @Query("SELECT * FROM level_minute WHERE minuteStartMs = :minuteStartMs")
    suspend fun minuteAt(minuteStartMs: Long): LevelMinuteEntity?

    /**
     * Stores [row], merged into the row already there for the same minute. Pausing and resuming within a
     * minute records it in two parts, and the second used to replace the first.
     */
    @Transaction
    suspend fun addMinute(row: LevelMinuteEntity) {
        insertMinute(minuteAt(row.minuteStartMs)?.let { mergeMinutes(it, row) } ?: row)
    }

    /** Rows with fromMs <= minuteStartMs < toMs, ascending. */
    @Query("SELECT * FROM level_minute WHERE minuteStartMs >= :fromMs AND minuteStartMs < :toMs ORDER BY minuteStartMs ASC")
    fun minutesBetween(fromMs: Long, toMs: Long): Flow<List<LevelMinuteEntity>>

    /** Minutes that count as calm under [Unlocks.isCalmMinute]'s rules; used once to backfill rewards. */
    @Query("SELECT COUNT(*) FROM level_minute WHERE laeqDb < :belowDb AND listeningSeconds >= :minSeconds")
    suspend fun countCalmMinutes(belowDb: Float, minSeconds: Int): Int

    /** Calm minutes (same rules as [countCalmMinutes]) with fromMs <= minuteStartMs < toMs; used to backfill a limited event. */
    @Query(
        "SELECT COUNT(*) FROM level_minute WHERE laeqDb < :belowDb AND listeningSeconds >= :minSeconds " +
            "AND minuteStartMs >= :fromMs AND minuteStartMs < :toMs"
    )
    suspend fun countCalmMinutesBetween(belowDb: Float, minSeconds: Int, fromMs: Long, toMs: Long): Int

    @Query("DELETE FROM level_minute WHERE minuteStartMs < :beforeMs")
    suspend fun deleteMinutesBefore(beforeMs: Long): Int
}
