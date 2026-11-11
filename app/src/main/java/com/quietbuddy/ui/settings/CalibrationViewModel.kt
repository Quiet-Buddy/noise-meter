package com.quietbuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.LevelSample
import com.quietbuddy.core.LevelSource
import com.quietbuddy.core.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val levelSource: LevelSource,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val latest: StateFlow<LevelSample?> = levelSource.latest

    /** newOffset = current offset + (reference - current reading). No-op if there is no live reading. */
    fun save(referenceDb: Float, onSaved: () -> Unit) {
        val current = levelSource.latest.value?.laeq2sDb ?: return
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(
                    calibrationOffsetDb = it.calibrationOffsetDb + (referenceDb - current),
                    calibratedAtMs = System.currentTimeMillis(),
                )
            }
            onSaved()
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(calibrationOffsetDb = AppSettings.DEFAULT_CALIBRATION_OFFSET_DB, calibratedAtMs = null)
            }
        }
    }
}
