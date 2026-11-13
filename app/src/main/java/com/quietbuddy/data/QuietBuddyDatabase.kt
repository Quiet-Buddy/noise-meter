package com.quietbuddy.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LevelMinuteEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class QuietBuddyDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

/** v1 -> v2: the Quiet Challenge was removed, so its table goes; minute history is untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS challenge")
    }
}
