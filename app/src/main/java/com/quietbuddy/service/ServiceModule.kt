package com.quietbuddy.service

import com.quietbuddy.audio.LevelBus
import com.quietbuddy.core.LevelSource
import com.quietbuddy.core.ListeningController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds @Singleton abstract fun levelSource(impl: LevelBus): LevelSource
    @Binds @Singleton abstract fun controller(impl: ServiceListeningController): ListeningController
}
