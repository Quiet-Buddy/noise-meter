package com.quietbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
