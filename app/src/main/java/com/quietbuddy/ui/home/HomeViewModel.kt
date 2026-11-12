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
import com.quietbuddy.core.Reward
import com.quietbuddy.core.SettingsRepository
import com.quietbuddy.core.Unlocks
import com.quietbuddy.core.Character
import com.quietbuddy.core.Accessory
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
    val challenge: ChallengeState = ChallengeState.Idle,
    /** Newly unlocked rewards to celebrate, cheapest first; empty when none. */
    val pendingUnlocks: List<Reward> = emptyList(),
    /** The next reward to earn; null when everything is unlocked. */
    val nextReward: Reward? = null,
    val minutesToNext: Int = 0,
    /** 0..1 progress from the previous reward's threshold to the next one's. */
    val nextRewardProgress: Float = 0f,
    /** The selected style of each character, for previewing rewards. */
    val styles: Map<Character, CharacterStyle> = emptyMap(),
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
            styles = Character.entries.associateWith { settings.styleFor(it) },
            listening = listening,
            mood = mood,
            db = latest?.laeq2sDb?.roundToInt(),
            calmMinutes = today?.calmMinutes ?: 0,
            pendingUnlocks = progress.pendingCelebration,
            nextReward = next,
            minutesToNext = next?.let { (it.unlockAtCalmMinutes - progress.calmMinutes).toInt().coerceAtLeast(1) } ?: 0,
            nextRewardProgress = next?.let { n ->
                val previous = Reward.all.map { it.unlockAtCalmMinutes }.filter { it < n.unlockAtCalmMinutes }.maxOrNull() ?: 0
                ((progress.calmMinutes - previous).toFloat() / (n.unlockAtCalmMinutes - previous)).coerceIn(0f, 1f)
            } ?: 0f,
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
            challenge = ch,
            pendingUnlocks = s.pendingUnlocks,
            nextReward = s.nextReward,
            minutesToNext = s.minutesToNext,
            nextRewardProgress = s.nextRewardProgress,
            styles = s.styles,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /**
     * Closes the unlock celebration. With [tryReward] also tries it: a style switches to its character and style,
     * an accessory is put on the character it belongs to, which is switched to.
     */
    fun dismissUnlocks(tryReward: Reward?) {
        viewModelScope.launch {
            val pending = uiState.value.pendingUnlocks
            when (tryReward) {
                is CharacterStyle -> settingsRepository.update {
                    it.copy(character = tryReward.character, styles = it.styles + (tryReward.character to tryReward))
                }
                is Accessory -> settingsRepository.update {
                    val worn = it.accessoriesFor(tryReward.character) + tryReward
                    it.copy(character = tryReward.character, accessories = it.accessories + (tryReward.character to worn))
                }
                null -> Unit
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
        val styles: Map<Character, CharacterStyle>,
        val listening: ListeningState,
        val mood: Mood,
        val db: Int?,
        val calmMinutes: Int,
        val pendingUnlocks: List<Reward>,
        val nextReward: Reward?,
        val minutesToNext: Int,
        val nextRewardProgress: Float,
    )

    private companion object {
        const val CHALLENGE_SECONDS = 30
        const val RESULT_MS = 5_000L
    }
}
