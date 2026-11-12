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

    @Insert
    suspend fun insertChallenge(row: ChallengeEntity)

    /** Rows with fromMs <= minuteStartMs < toMs, ascending. */
    @Query("SELECT * FROM level_minute WHERE minuteStartMs >= :fromMs AND minuteStartMs < :toMs ORDER BY minuteStartMs ASC")
    fun minutesBetween(fromMs: Long, toMs: Long): Flow<List<LevelMinuteEntity>>

    /** Rows with fromMs <= startedAtMs < toMs, ascending. */
    @Query("SELECT * FROM challenge WHERE startedAtMs >= :fromMs AND startedAtMs < :toMs ORDER BY startedAtMs ASC")
    fun challengesBetween(fromMs: Long, toMs: Long): Flow<List<ChallengeEntity>>

    /** Minutes that count as calm under [Unlocks.isCalmMinute]'s rules; used once to backfill rewards. */
    @Query("SELECT COUNT(*) FROM level_minute WHERE laeqDb < :belowDb AND listeningSeconds >= :minSeconds")
    suspend fun countCalmMinutes(belowDb: Float, minSeconds: Int): Int

    @Query("SELECT COUNT(*) FROM challenge WHERE won = 1")
    suspend fun countWonChallenges(): Int

    @Query("DELETE FROM level_minute WHERE minuteStartMs < :beforeMs")
    suspend fun deleteMinutesBefore(beforeMs: Long): Int

    @Query("DELETE FROM challenge WHERE startedAtMs < :beforeMs")
    suspend fun deleteChallengesBefore(beforeMs: Long): Int
}
