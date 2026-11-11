package com.quietbuddy.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Live readings. Implemented by audio.LevelBus (a @Singleton), fed by ListeningService. */
interface LevelSource {
    val samples: SharedFlow<LevelSample>
    val latest: StateFlow<LevelSample?>
    val mood: StateFlow<Mood>
    val state: StateFlow<ListeningState>
}

/** Starts and stops ListeningService. Implemented in the service package. */
interface ListeningController {
    /** Must be called while the app is in the foreground (Android 14 microphone FGS rule). */
    fun start()
    fun pause()
}

/** Implemented in the data package on Preferences DataStore. */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

/** Implemented in the data package on Room. All times epoch ms, days are LocalDate.toEpochDay() in the system zone. */
interface HistoryRepository {
    suspend fun recordMinute(minute: MinuteAggregate, dominantMood: Mood)
    suspend fun recordChallenge(startedAtMs: Long, won: Boolean, peakDb: Float)
    /** Hourly energy-averaged levels for one day, only hours that have data, ascending. */
    fun hourly(epochDay: Long): Flow<List<HourBar>>
    /** Summaries for the last [days] days ending today, oldest first; days without data have null fields and 0 counts. */
    fun lastDays(days: Int): Flow<List<DaySummary>>
    /** Deletes minute rows older than 35 days and challenge rows older than 365 days. */
    suspend fun prune(nowMs: Long)
}
