package com.quietbuddy.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LevelMinuteEntity::class, ChallengeEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class QuietBuddyDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
