package com.quietbuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.Preset
import com.quietbuddy.core.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun selectPreset(preset: Preset) = update {
        it.copy(preset = preset, worriedFromDb = preset.worriedFromDb, sadFromDb = preset.sadFromDb)
    }

    /** Custom thresholds from the slider; clears the preset. */
    fun setThresholds(worriedFromDb: Float, sadFromDb: Float) = update {
        it.copy(preset = null, worriedFromDb = worriedFromDb, sadFromDb = sadFromDb)
    }


    fun setChimeOnSad(enabled: Boolean) = update { it.copy(chimeOnSad = enabled) }

    fun setKeepListening(enabled: Boolean) = update { it.copy(keepListening = enabled) }

    fun setKeepScreenOn(enabled: Boolean) = update { it.copy(keepScreenOn = enabled) }

    fun setRoomName(name: String) = update {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) it else it.copy(roomName = trimmed)
    }
}
