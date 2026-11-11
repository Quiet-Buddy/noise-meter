package com.quietbuddy.audio

import com.quietbuddy.core.LevelSample
import com.quietbuddy.core.LevelSource
import com.quietbuddy.core.ListeningState
import com.quietbuddy.core.Mood
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LevelBus @Inject constructor() : LevelSource {
    private val _samples = MutableSharedFlow<LevelSample>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _latest = MutableStateFlow<LevelSample?>(null)
    private val _mood = MutableStateFlow(Mood.HAPPY)
    private val _state = MutableStateFlow(ListeningState.PAUSED)

    override val samples: SharedFlow<LevelSample> = _samples
    override val latest: StateFlow<LevelSample?> = _latest
    override val mood: StateFlow<Mood> = _mood
    override val state: StateFlow<ListeningState> = _state

    fun emitSample(sample: LevelSample) {
        _latest.value = sample
        _samples.tryEmit(sample)
    }

    fun setMood(mood: Mood) { _mood.value = mood }
    fun setState(state: ListeningState) { _state.value = state }
    fun clearLatest() { _latest.value = null }
}
