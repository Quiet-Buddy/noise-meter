package com.quietbuddy.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietbuddy.core.HistoryRepository
import com.quietbuddy.core.LevelSource
import com.quietbuddy.core.ListeningController
import com.quietbuddy.core.ListeningState
import com.quietbuddy.core.Mood
import com.quietbuddy.core.ProgressRepository
import com.quietbuddy.core.SettingsRepository
import com.quietbuddy.core.Unlocks
import com.quietbuddy.core.Character
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.domain.character.BuddyVoice
import com.quietbuddy.domain.character.Voices
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ChallengeState {
    object Idle : ChallengeState
    data class Running(val secondsLeft: Int) : ChallengeState
    data class Finished(val won: Boolean) : ChallengeState
}

data class HomeUiState(
    val roomName: String = "",
    val listening: ListeningState = ListeningState.PAUSED,
    /** Null whenever Buddy is asleep (not LISTENING). */
    val mood: Mood? = null,
    val db: Int? = null,
    @StringRes val speechLine: Int = BuddyVoice.sleeping,
    val calmMinutes: Int = 0,
    val loudestDb: Float? = null,
    val loudestAtMs: Long? = null,
    val challenge: ChallengeState = ChallengeState.Idle,
    /** Newly unlocked styles to celebrate, cheapest first; empty when none. */
    val pendingUnlocks: List<CharacterStyle> = emptyList(),
    /** The next style to earn; null when everything is unlocked. */
    val nextReward: CharacterStyle? = null,
    val minutesToNext: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val levelSource: LevelSource,
    private val controller: ListeningController,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val speechIndex = MutableStateFlow(0)
    private val challenge = MutableStateFlow<ChallengeState>(ChallengeState.Idle)
    private var challengeJob: Job? = null

    init {
        // Rotate the speech line every time the mood changes (the first value is not a change).
        viewModelScope.launch {
            levelSource.mood.drop(1).collect {
                speechIndex.update { it + 1 }
            }
        }
    }

    private val snapshot = combine(
        combine(settingsRepository.settings, progressRepository.progress) { settings, progress -> settings to progress },
        levelSource.latest,
        levelSource.mood,
        levelSource.state,
        historyRepository.lastDays(1),
    ) { (settings, progress), latest, mood, listening, days ->
        val today = days.lastOrNull()
        val next = Unlocks.next(progress.calmMinutes)
        Snapshot(
            roomName = settings.roomName,
            character = settings.character,
            listening = listening,
            mood = mood,
            db = latest?.laeq2sDb?.roundToInt(),
            calmMinutes = today?.calmMinutes ?: 0,
            loudestDb = today?.loudestDb,
            loudestAtMs = today?.loudestAtMs,
            pendingUnlocks = progress.pendingCelebration,
            nextReward = next,
            minutesToNext = next?.let { (it.unlockAtCalmMinutes - progress.calmMinutes).toInt().coerceAtLeast(1) } ?: 0,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(snapshot, speechIndex, challenge) { s, index, ch ->
        val active = s.listening == ListeningState.LISTENING
        val mood = if (active) s.mood else null
        HomeUiState(
            roomName = s.roomName,
            listening = s.listening,
            mood = mood,
            db = if (active) s.db else null,
            speechLine = Voices.of(s.character).let { voice ->
                if (mood != null) voice.line(mood, index) else voice.sleeping
            },
            calmMinutes = s.calmMinutes,
            loudestDb = s.loudestDb,
            loudestAtMs = s.loudestAtMs,
            challenge = ch,
            pendingUnlocks = s.pendingUnlocks,
            nextReward = s.nextReward,
            minutesToNext = s.minutesToNext,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Closes the unlock celebration; with [tryStyle] also switches to that character and style. */
    fun dismissUnlocks(tryStyle: CharacterStyle?) {
        viewModelScope.launch {
            val pending = uiState.value.pendingUnlocks
            if (tryStyle != null) {
                settingsRepository.update {
                    it.copy(character = tryStyle.character, styles = it.styles + (tryStyle.character to tryStyle))
                }
            }
            progressRepository.markCelebrated(pending)
        }
    }

    fun start() = controller.start()

    fun pause() = controller.pause()

    fun startChallenge() {
        if (challengeJob?.isActive == true) return
        challengeJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val threshold = settingsRepository.settings.first().worriedFromDb
            var peak = Float.NEGATIVE_INFINITY
            var lost = false
            coroutineScope {
                val watcher = launch {
                    levelSource.samples.collect { sample ->
                        peak = maxOf(peak, sample.laeq2sDb)
                        if (sample.laeq2sDb >= threshold) lost = true
                    }
                }
                for (left in CHALLENGE_SECONDS downTo 1) {
                    if (lost) break
                    challenge.value = ChallengeState.Running(left)
                    delay(1_000)
                }
                watcher.cancel()
            }
            val won = !lost
            historyRepository.recordChallenge(startedAt, won, if (peak.isInfinite()) 0f else peak)
            if (won) progressRepository.addCalmMinutes(Unlocks.CHALLENGE_BONUS_MINUTES)
            challenge.value = ChallengeState.Finished(won)
            delay(RESULT_MS)
            challenge.value = ChallengeState.Idle
        }
    }

    private data class Snapshot(
        val roomName: String,
        val character: Character,
        val listening: ListeningState,
        val mood: Mood,
        val db: Int?,
        val calmMinutes: Int,
        val loudestDb: Float?,
        val loudestAtMs: Long?,
        val pendingUnlocks: List<CharacterStyle>,
        val nextReward: CharacterStyle?,
        val minutesToNext: Int,
    )

    private companion object {
        const val CHALLENGE_SECONDS = 30
        const val RESULT_MS = 5_000L
    }
}
