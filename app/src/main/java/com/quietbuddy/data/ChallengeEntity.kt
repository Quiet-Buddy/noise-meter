package com.quietbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenge")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMs: Long,
    val won: Boolean,
    val peakDb: Float,
)
