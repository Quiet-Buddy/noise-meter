package com.quietbuddy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMinute(row: LevelMinuteEntity)

    /** Rows with fromMs <= minuteStartMs < toMs, ascending. */
    @Query("SELECT * FROM level_minute WHERE minuteStartMs >= :fromMs AND minuteStartMs < :toMs ORDER BY minuteStartMs ASC")
    fun minutesBetween(fromMs: Long, toMs: Long): Flow<List<LevelMinuteEntity>>

    /** Minutes that count as calm under [Unlocks.isCalmMinute]'s rules; used once to backfill rewards. */
    @Query("SELECT COUNT(*) FROM level_minute WHERE laeqDb < :belowDb AND listeningSeconds >= :minSeconds")
    suspend fun countCalmMinutes(belowDb: Float, minSeconds: Int): Int

    @Query("DELETE FROM level_minute WHERE minuteStartMs < :beforeMs")
    suspend fun deleteMinutesBefore(beforeMs: Long): Int
}
