package com.quietbuddy.data

import android.content.Context
import androidx.room.Room
import com.quietbuddy.core.HistoryRepository
import com.quietbuddy.core.ProgressRepository
import com.quietbuddy.core.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuietBuddyDatabase =
        Room.databaseBuilder(context, QuietBuddyDatabase::class.java, "quietbuddy.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideHistoryDao(db: QuietBuddyDatabase): HistoryDao = db.historyDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindHistoryRepository(impl: RoomHistoryRepository): HistoryRepository

    @Binds
    abstract fun bindProgressRepository(impl: DataStoreProgressRepository): ProgressRepository
}
