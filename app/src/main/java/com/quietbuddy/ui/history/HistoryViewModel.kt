package com.quietbuddy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.DaySummary
import com.quietbuddy.core.HistoryRepository
import com.quietbuddy.core.HourBar
import com.quietbuddy.core.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val todayEpochDay: Long = LocalDate.now().toEpochDay()

    private val _selectedDay = MutableStateFlow(todayEpochDay)
    val selectedDay: StateFlow<Long> = _selectedDay.asStateFlow()

    /** Last seven days, oldest first. */
    val days: StateFlow<List<DaySummary>> = historyRepository.lastDays(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Hourly bars for the selected day. */
    val hours: StateFlow<List<HourBar>> = _selectedDay
        .flatMapLatest { historyRepository.hourly(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun selectDay(epochDay: Long) {
        _selectedDay.value = epochDay
    }
}
