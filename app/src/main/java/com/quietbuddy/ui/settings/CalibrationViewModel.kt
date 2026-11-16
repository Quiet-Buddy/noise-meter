package com.quietbuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.Calibration
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

    private var saving = false

    /**
     * newOffset = current offset + (reference - current reading), then calls [onSaved]. No-op if there is
     * no live reading, if a save is already running (a double tap would apply the correction twice), or if
     * the result is outside [Calibration.MAX_OFFSET_DEVIATION_DB] of the default.
     */
    fun save(referenceDb: Float, onSaved: () -> Unit) {
        if (saving) return
        val current = levelSource.latest.value?.laeq2sDb ?: return
        saving = true
        viewModelScope.launch {
            var saved = false
            settingsRepository.update {
                val offset = Calibration.correctedOffset(it.calibrationOffsetDb, referenceDb, current)
                    ?: return@update it
                saved = true
                it.copy(calibrationOffsetDb = offset, calibratedAtMs = System.currentTimeMillis())
            }
            if (saved) onSaved() else saving = false
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
