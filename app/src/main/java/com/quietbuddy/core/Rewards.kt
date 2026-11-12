package com.quietbuddy.core

import kotlinx.coroutines.flow.Flow

/**
 * A colour style for a character. Each character has one free default style; the others unlock
 * as the room collects calm minutes (see [Unlocks]).
 *
 * [key] is persisted (selected style and unlocked set); never change an existing key.
 */
enum class CharacterStyle(val key: String, val character: Character, val unlockAtCalmMinutes: Int) {
    BUDDY_SUNNY("buddy_sunny", Character.BUDDY, 0),
    KITTEN_GINGER("kitten_ginger", Character.KITTEN, 0),
    BUDDY_MINT("buddy_mint", Character.BUDDY, 30),
    KITTEN_GREY("kitten_grey", Character.KITTEN, 120),
    BUDDY_BERRY("buddy_berry", Character.BUDDY, 300),
    KITTEN_SNOW("kitten_snow", Character.KITTEN, 600);

    val isFree: Boolean get() = unlockAtCalmMinutes == 0

    companion object {
        fun defaultFor(character: Character): CharacterStyle = when (character) {
            Character.BUDDY -> BUDDY_SUNNY
            Character.KITTEN -> KITTEN_GINGER
        }

        fun of(character: Character): List<CharacterStyle> = entries.filter { it.character == character }

        fun fromKey(key: String?): CharacterStyle? = entries.firstOrNull { it.key == key }
    }
}

/** The rules for earning styles. Pure; unit-tested. */
object Unlocks {
    /** A recorded minute counts as calm when at least this much of it was listened to... */
    const val MIN_LISTENING_SECONDS = 30
    /** ...and its level stayed under the user's "worried" threshold. */
    fun isCalmMinute(minute: MinuteAggregate, settings: AppSettings): Boolean =
        minute.listeningSeconds >= MIN_LISTENING_SECONDS && minute.laeqDb < settings.worriedFromDb

    /** Calm minutes a won challenge is worth. */
    const val CHALLENGE_BONUS_MINUTES = 5

    /** Styles earned by [calmMinutes], free ones included. */
    fun earnedBy(calmMinutes: Long): Set<CharacterStyle> =
        CharacterStyle.entries.filter { calmMinutes >= it.unlockAtCalmMinutes }.toSet()

    /** The cheapest style not yet earned, or null when everything is earned. */
    fun next(calmMinutes: Long): CharacterStyle? =
        CharacterStyle.entries.filter { calmMinutes < it.unlockAtCalmMinutes }.minByOrNull { it.unlockAtCalmMinutes }
}

/**
 * Lifetime reward progress. Kept apart from history, which is pruned.
 * [unlocked] only grows: a style stays unlocked even if the thresholds change later.
 * [celebrated] are the unlocked styles the user has already been told about.
 */
data class Progress(
    val calmMinutes: Long = 0,
    val unlocked: Set<CharacterStyle> = CharacterStyle.entries.filter { it.isFree }.toSet(),
    val celebrated: Set<CharacterStyle> = CharacterStyle.entries.filter { it.isFree }.toSet(),
) {
    /** Newly unlocked styles waiting for a celebration, cheapest first. */
    val pendingCelebration: List<CharacterStyle>
        get() = (unlocked - celebrated).sortedBy { it.unlockAtCalmMinutes }

    fun isUnlocked(style: CharacterStyle, settings: AppSettings): Boolean =
        settings.unlockAllStyles || style in unlocked
}

/** Implemented in the data package on its own Preferences DataStore ("progress"). */
interface ProgressRepository {
    val progress: Flow<Progress>

    /** Adds calm minutes and unlocks whatever the new total earns, atomically. */
    suspend fun addCalmMinutes(minutes: Int)

    /**
     * Once per install: seeds the total from existing history (calm minutes still in the database plus
     * [Unlocks.CHALLENGE_BONUS_MINUTES] per won challenge) so early users get credit. No-op afterwards.
     */
    suspend fun backfillIfNeeded()

    /** Marks styles as celebrated so they are not announced again. */
    suspend fun markCelebrated(styles: Collection<CharacterStyle>)
}
