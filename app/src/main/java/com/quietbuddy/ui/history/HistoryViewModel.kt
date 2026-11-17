package com.quietbuddy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.DayClock
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
    dayClock: DayClock,
) : ViewModel() {

    private val today: StateFlow<Long> = dayClock.days.map { it.toEpochDay() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), dayClock.today().toEpochDay())

    // Null means "today", so the selection moves on at midnight unless the user picked another day.
    private val pickedDay = MutableStateFlow<Long?>(null)

    val selectedDay: StateFlow<Long> = combine(today, pickedDay) { t, picked -> picked ?: t }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), today.value)

    /** Last seven days, oldest first. */
    val days: StateFlow<List<DaySummary>> = historyRepository.lastDays(7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Hourly bars for the selected day. */
    val hours: StateFlow<List<HourBar>> = selectedDay
        .flatMapLatest { historyRepository.hourly(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun selectDay(epochDay: Long) {
        pickedDay.value = epochDay.takeIf { it != today.value }
    }
}
