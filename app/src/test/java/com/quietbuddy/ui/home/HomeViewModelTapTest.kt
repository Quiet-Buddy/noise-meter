package com.quietbuddy.ui.home

import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.DaySummary
import com.quietbuddy.core.HistoryRepository
import com.quietbuddy.core.HourBar
import com.quietbuddy.core.LevelSample
import com.quietbuddy.core.LevelSource
import com.quietbuddy.core.ListeningController
import com.quietbuddy.core.ListeningState
import com.quietbuddy.core.MinuteAggregate
import com.quietbuddy.core.Mood
import com.quietbuddy.core.Progress
import com.quietbuddy.core.LedgerState
import com.quietbuddy.core.ProgressRepository
import com.quietbuddy.core.Reward
import com.quietbuddy.core.SettingsRepository
import com.quietbuddy.domain.character.BuddyVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTapTest {
    private class FakeLevels : LevelSource {
        override val samples: SharedFlow<LevelSample> = MutableSharedFlow()
        override val latest = MutableStateFlow<LevelSample?>(null)
        override val mood = MutableStateFlow(Mood.HAPPY)
        override val state = MutableStateFlow(ListeningState.LISTENING)
    }

    private val levels = FakeLevels()
    private var now = 10_000L

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = HomeViewModel(
        levels,
        object : ListeningController { override fun start() = Unit; override fun pause() = Unit },
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
    ).also {
        it.clockMs = { now }
        backgroundScope.launch { it.uiState.collect {} }
    }

    @Test fun rapidCalmTapsAllCount() = runTest {
        val vm = viewModel()
        runCurrent()
        repeat(5) { vm.onCharacterTapped(); now += 100 }
        runCurrent()
        assertEquals(5, vm.uiState.value.tapCount)
        assertTrue(vm.uiState.value.speechLine in BuddyVoice.tapLines())
    }

    @Test fun lineHoldsForRapidTapsThenAdvances() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onCharacterTapped()
        runCurrent()
        val first = vm.uiState.value.speechLine
        repeat(3) { now += 300; vm.onCharacterTapped() }
        runCurrent()
        assertEquals(first, vm.uiState.value.speechLine)

        now += 400 // 1.3 s since the first line appeared
        vm.onCharacterTapped()
        runCurrent()
        val second = vm.uiState.value.speechLine
        assertTrue(second in BuddyVoice.tapLines())
        assertTrue(first != second)
    }

    @Test fun replyFadesFourSecondsAfterTheLastTap() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onCharacterTapped()
        advanceTimeBy(3_000)
        now += 3_000
        vm.onCharacterTapped()
        advanceTimeBy(3_000)
        runCurrent()
        assertTrue(vm.uiState.value.speechLine in BuddyVoice.tapLines())
        advanceTimeBy(1_100)
        runCurrent()
        assertFalse(vm.uiState.value.speechLine in BuddyVoice.tapLines())
    }

    @Test fun replyFadesBackToTheMoodLineAfterAFewSeconds() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onCharacterTapped()
        runCurrent()
        assertTrue(vm.uiState.value.speechLine in BuddyVoice.tapLines())
        advanceTimeBy(4_100)
        runCurrent()
        assertFalse(vm.uiState.value.speechLine in BuddyVoice.tapLines())
    }

    @Test fun nothingHappensWhenNotCalmOrNotListening() = runTest {
        val vm = viewModel()
        runCurrent()
        levels.mood.value = Mood.WORRIED
        runCurrent()
        vm.onCharacterTapped()
        levels.mood.value = Mood.HAPPY
        levels.state.value = ListeningState.PAUSED
        runCurrent()
        now += 5_000
        vm.onCharacterTapped()
        runCurrent()
        assertEquals(0, vm.uiState.value.tapCount)
    }

    @Test fun replyIsDroppedWhenTheRoomGetsLoud() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onCharacterTapped()
        runCurrent()
        levels.mood.value = Mood.SAD
        runCurrent()
        assertFalse(vm.uiState.value.speechLine in BuddyVoice.tapLines())
        levels.mood.value = Mood.HAPPY
        runCurrent()
        assertFalse(vm.uiState.value.speechLine in BuddyVoice.tapLines())
    }
}
