package com.quietbuddy.ui.home

import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.DaySummary
import com.quietbuddy.core.HistoryRepository
import com.quietbuddy.core.HourBar
import com.quietbuddy.core.LedgerState
import com.quietbuddy.core.LevelSample
import com.quietbuddy.core.LevelSource
import com.quietbuddy.core.ListeningController
import com.quietbuddy.core.ListeningState
import com.quietbuddy.core.MinuteAggregate
import com.quietbuddy.core.Mood
import com.quietbuddy.core.Progress
import com.quietbuddy.core.ProgressRepository
import com.quietbuddy.core.Reward
import com.quietbuddy.core.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelAutoStartTest {
    private class FakeLevels : LevelSource {
        override val samples: SharedFlow<LevelSample> = MutableSharedFlow()
        override val latest = MutableStateFlow<LevelSample?>(null)
        override val mood = MutableStateFlow(Mood.HAPPY)
        override val state = MutableStateFlow(ListeningState.PAUSED)
    }

    private class CountingController : ListeningController {
        var starts = 0
        override fun start() { starts++ }
        override fun pause() = Unit
    }

    private val controller = CountingController()
    /** Shared like the real singleton: one per process, while view models come and go. */
    private val launchAutoStart = LaunchAutoStart()

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = HomeViewModel(
        FakeLevels(),
        controller,
        object : SettingsRepository {
            override val settings: Flow<AppSettings> = flowOf(AppSettings())
            override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
        },
        object : HistoryRepository {
            override suspend fun recordMinute(minute: MinuteAggregate, dominantMood: Mood) = Unit
            override fun hourly(epochDay: Long): Flow<List<HourBar>> = flowOf(emptyList())
            override fun lastDays(days: Int): Flow<List<DaySummary>> = flowOf(emptyList())
            override suspend fun prune(nowMs: Long) = Unit
        },
        object : ProgressRepository {
            override val progress: Flow<Progress> = flowOf(Progress())
            override suspend fun addCalmMinutes(minutes: Int) = Unit
            override suspend fun ledger() = LedgerState()
            override suspend fun commitLedger(earnedMinutes: Int, state: LedgerState) = Unit
            override suspend fun backfillIfNeeded() = Unit
            override suspend fun markCelebrated(rewards: Collection<Reward>) = Unit
        },
        launchAutoStart,
    )

    @Test fun firstHomeStartsListening() {
        viewModel().autoStart()
        assertEquals(1, controller.starts)
    }

    @Test fun returningToHomeDoesNotRestartAPausedSession() {
        val vm = viewModel()
        vm.autoStart()
        vm.pause()
        vm.autoStart() // back from another tab
        viewModel().autoStart() // a fresh view model after the activity was recreated
        assertEquals(1, controller.starts)
    }

    @Test fun aPauseAfterAStartFromTheWakeButtonSticks() {
        // Home first opened without the mic, so autoStart wasn't called; the user then granted it via the wake button.
        val vm = viewModel()
        vm.start()
        vm.pause()
        vm.autoStart() // back from another tab, mic now granted
        assertEquals(1, controller.starts)
    }

    @Test fun anExplicitStartStillWorksAfterTheAutoStart() {
        val vm = viewModel()
        vm.autoStart()
        vm.start()
        assertEquals(2, controller.starts)
    }
}
