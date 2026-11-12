package com.quietbuddy.ui.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.Progress
import com.quietbuddy.core.ProgressRepository
import com.quietbuddy.core.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CharactersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val progress: StateFlow<Progress> = progressRepository.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Progress())

    /** Switches to [style]'s character and style, if it is unlocked. */
    fun select(style: CharacterStyle) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            if (!progressRepository.progress.first().isUnlocked(style, current)) return@launch
            settingsRepository.update {
                it.copy(character = style.character, styles = it.styles + (style.character to style))
            }
        }
    }
}
