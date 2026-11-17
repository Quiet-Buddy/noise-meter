package com.quietbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.log10
import kotlin.math.pow

@Entity(tableName = "level_minute")
data class LevelMinuteEntity(
    @PrimaryKey val minuteStartMs: Long,
    val laeqDb: Float,
    val minDb: Float,
    val maxDb: Float,
    /** Mood.name */
    val dominantMood: String,
    val listeningSeconds: Int,
)

/**
 * Two recordings of the same minute (a pause and resume inside it) as one row: listening time adds up,
 * the level is the energy average weighted by listening time, and the mood is the one of the longer part.
 */
internal fun mergeMinutes(a: LevelMinuteEntity, b: LevelMinuteEntity): LevelMinuteEntity {
    val seconds = a.listeningSeconds + b.listeningSeconds
    val laeq = if (seconds <= 0) b.laeqDb else {
        val energy = a.listeningSeconds * 10.0.pow(a.laeqDb / 10.0) + b.listeningSeconds * 10.0.pow(b.laeqDb / 10.0)
        (10.0 * log10(energy / seconds)).toFloat()
    }
    return LevelMinuteEntity(
        minuteStartMs = a.minuteStartMs,
        laeqDb = laeq,
        minDb = minOf(a.minDb, b.minDb),
        maxDb = maxOf(a.maxDb, b.maxDb),
        dominantMood = if (b.listeningSeconds > a.listeningSeconds) b.dominantMood else a.dominantMood,
        // A clock step back can record the same minute twice; it still lasted at most 60 s.
        listeningSeconds = seconds.coerceAtMost(60),
    )
}
