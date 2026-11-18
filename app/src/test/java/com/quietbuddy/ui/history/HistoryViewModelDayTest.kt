package com.quietbuddy.ui.history

import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.DayClock
import com.quietbuddy.core.DaySummary
import com.quietbuddy.core.HistoryRepository
import com.quietbuddy.core.HourBar
import com.quietbuddy.core.MinuteAggregate
import com.quietbuddy.core.Mood
import com.quietbuddy.core.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelDayTest {
    private val nov30 = LocalDate.of(2026, 11, 30)
    private var now = nov30.atTime(23, 58).toInstant(ZoneOffset.UTC).toEpochMilli()
    private var zone: ZoneId = ZoneOffset.UTC
    private val clock = DayClock(nowMs = { now }, zone = { zone })

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private val hourlyRequests = mutableListOf<Long>()

    private fun TestScope.viewModel() = HistoryViewModel(
        object : HistoryRepository {
            override suspend fun recordMinute(minute: MinuteAggregate, dominantMood: Mood) = Unit
            override fun hourly(epochDay: Long): Flow<List<HourBar>> { hourlyRequests += epochDay; return flowOf(emptyList()) }
            override fun lastDays(days: Int): Flow<List<DaySummary>> = flowOf(emptyList())
            override suspend fun prune(nowMs: Long) = Unit
        },
        object : SettingsRepository {
            override val settings: Flow<AppSettings> = flowOf(AppSettings())
            override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
        },
        clock,
    ).also {
        backgroundScope.launch { it.selectedDay.collect {} }
        backgroundScope.launch { it.hours.collect {} }
    }

    private fun TestScope.passMidnight() {
        now += 3 * 60_000L
        advanceTimeBy(60_001)
        runCurrent()
    }

    @Test fun dayClockEmitsTheNewDateAfterMidnight() = runTest {
        val days = mutableListOf<LocalDate>()
        backgroundScope.launch { clock.days.take(2).toList(days) }
        runCurrent()
        passMidnight()
        assertEquals(listOf(nov30, nov30.plusDays(1)), days)
    }

    @Test fun selectedTodayMovesOnAtMidnight() = runTest {
        val vm = viewModel()
        runCurrent()
        assertEquals(nov30.toEpochDay(), vm.selectedDay.value)

        passMidnight()
        assertEquals(nov30.plusDays(1).toEpochDay(), vm.selectedDay.value)
    }

    @Test fun aDayTheUserPickedStaysPicked() = runTest {
        val vm = viewModel()
        runCurrent()
        val picked = nov30.minusDays(2).toEpochDay()
        vm.selectDay(picked)
        runCurrent()

        passMidnight()
        assertEquals(picked, vm.selectedDay.value)
    }

    @Test fun pickingTodayAgainFollowsTheDateAgain() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.selectDay(nov30.minusDays(1).toEpochDay())
        vm.selectDay(nov30.toEpochDay())
        runCurrent()

        passMidnight()
        assertEquals(nov30.plusDays(1).toEpochDay(), vm.selectedDay.value)
    }

    @Test fun hourBarsAreLoadedAgainWhenTheTimeZoneChanges() = runTest {
        viewModel()
        runCurrent()
        assertEquals(listOf(nov30.toEpochDay()), hourlyRequests)

        zone = ZoneOffset.ofHours(-1) // still Nov 30 at 23:58 UTC
        advanceTimeBy(60_001)
        runCurrent()
        assertEquals(listOf(nov30.toEpochDay(), nov30.toEpochDay()), hourlyRequests)
    }
}
