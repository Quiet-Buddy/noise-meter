package com.quietbuddy.core

import kotlinx.coroutines.flow.Flow

/**
 * Something earned with calm minutes: a [CharacterStyle] or an [Accessory].
 * [key] is persisted (unlocked and celebrated sets); keys are unique across both kinds.
 */
sealed interface Reward {
    val key: String
    val unlockAtCalmMinutes: Int

    companion object {
        val all: List<Reward> get() = CharacterStyle.entries + Accessory.entries

        fun fromKey(key: String?): Reward? = CharacterStyle.fromKey(key) ?: Accessory.fromKey(key)
    }
}

/**
 * A colour style for a character. Buddy's default is free; Mochi's and Pip's defaults are earned, and the others unlock
 * as the room collects calm minutes (see [Unlocks]).
 *
 * [key] is persisted (selected style and unlocked set); never change an existing key.
 */
enum class CharacterStyle(
    override val key: String,
    val character: Character,
    override val unlockAtCalmMinutes: Int,
) : Reward {
    BUDDY_SUNNY("buddy_sunny", Character.BUDDY, 0),
    BUDDY_MINT("buddy_mint", Character.BUDDY, 30),
    KITTEN_GINGER("kitten_ginger", Character.KITTEN, 120),
    BUDDY_BERRY("buddy_berry", Character.BUDDY, 240),
    KITTEN_GREY("kitten_grey", Character.KITTEN, 400),
    KITTEN_SNOW("kitten_snow", Character.KITTEN, 600),
    DUCK_LEMON("duck_lemon", Character.DUCK, 800),
    DUCK_CLOUD("duck_cloud", Character.DUCK, 1050),
    DUCK_HAZEL("duck_hazel", Character.DUCK, 1500);

    val isFree: Boolean get() = unlockAtCalmMinutes == 0

    /** True for a character's default style; unlocking it is what unlocks the character itself. */
    val isCharacterDefault: Boolean get() = this == defaultFor(character)

    companion object {
        fun defaultFor(character: Character): CharacterStyle = when (character) {
            Character.BUDDY -> BUDDY_SUNNY
            Character.KITTEN -> KITTEN_GINGER
            Character.DUCK -> DUCK_LEMON
        }

        fun of(character: Character): List<CharacterStyle> = entries.filter { it.character == character }

        fun fromKey(key: String?): CharacterStyle? = entries.firstOrNull { it.key == key }
    }
}

/** Where on the body an accessory sits; arts use it to layer accessories (e.g. a tie goes under the chin). BODY wraps around the body. */
enum class AccessorySlot { HEAD, FACE, NECK, FEET, BODY }

/**
 * Something one character can wear, earned with calm minutes like the styles. Each character has its own set,
 * worn per character (see [AppSettings.accessories]).
 *
 * [key] is persisted (unlocked set and worn sets); never change an existing key.
 */
enum class Accessory(
    override val key: String,
    val character: Character,
    val slot: AccessorySlot,
    override val unlockAtCalmMinutes: Int,
) : Reward {
    SUNGLASSES("acc_sunglasses", Character.BUDDY, AccessorySlot.FACE, 60),
    BOW("acc_bow", Character.KITTEN, AccessorySlot.HEAD, 180),
    TIE("acc_tie", Character.BUDDY, AccessorySlot.NECK, 300),
    BELL_COLLAR("acc_bell_collar", Character.KITTEN, AccessorySlot.NECK, 360),
    TOP_HAT("acc_top_hat", Character.BUDDY, AccessorySlot.HEAD, 480),
    FLOWER("acc_flower", Character.KITTEN, AccessorySlot.HEAD, 540),
    BOOTS("acc_boots", Character.BUDDY, AccessorySlot.FEET, 750),
    CROWN("acc_crown", Character.KITTEN, AccessorySlot.HEAD, 1000),
    CAP("acc_cap", Character.DUCK, AccessorySlot.HEAD, 900),
    GOGGLES("acc_goggles", Character.DUCK, AccessorySlot.FACE, 1200),
    BANDANA("acc_bandana", Character.DUCK, AccessorySlot.NECK, 1350),
    SWIM_RING("acc_swim_ring", Character.DUCK, AccessorySlot.BODY, 1400);

    companion object {
        fun fromKey(key: String?): Accessory? = entries.firstOrNull { it.key == key }

        fun of(character: Character): List<Accessory> = entries.filter { it.character == character }
    }
}

/** The rules for earning styles and accessories. Pure; unit-tested. */
object Unlocks {
    // Live earning is done by CalmLedger; these per-minute rules only remain for the one-off backfill from history.

    /** A recorded minute counts as calm when at least this much of it was listened to... */
    const val MIN_LISTENING_SECONDS = 30
    /** ...and its level stayed under the user's "worried" threshold. */
    fun isCalmMinute(minute: MinuteAggregate, settings: AppSettings): Boolean =
        minute.listeningSeconds >= MIN_LISTENING_SECONDS && minute.laeqDb < settings.worriedFromDb

    /** Calm minutes a won challenge is worth. */
    const val CHALLENGE_BONUS_MINUTES = 5

    /** Rewards earned by [calmMinutes], free ones included. */
    fun earnedBy(calmMinutes: Long): Set<Reward> =
        Reward.all.filter { calmMinutes >= it.unlockAtCalmMinutes }.toSet()

    /** The cheapest reward not yet earned, or null when everything is earned. */
    fun next(calmMinutes: Long): Reward? =
        Reward.all.filter { calmMinutes < it.unlockAtCalmMinutes }.minByOrNull { it.unlockAtCalmMinutes }

    /** The threshold of the most expensive reward already earned (0 when none), for progress bars. */
    fun previousThreshold(calmMinutes: Long): Int =
        Reward.all.filter { it.unlockAtCalmMinutes <= calmMinutes }.maxOfOrNull { it.unlockAtCalmMinutes } ?: 0
}

/**
 * Lifetime reward progress. Kept apart from history, which is pruned.
 * [unlocked] only grows: a reward stays unlocked even if the thresholds change later.
 * [celebrated] are the unlocked rewards the user has already been told about.
 */
data class Progress(
    val calmMinutes: Long = 0,
    val unlocked: Set<Reward> = FREE,
    val celebrated: Set<Reward> = FREE,
) {
    /** Newly unlocked rewards waiting for a celebration, cheapest first. */
    val pendingCelebration: List<Reward>
        get() = (unlocked - celebrated).sortedBy { it.unlockAtCalmMinutes }

    fun isUnlocked(reward: Reward): Boolean = reward in unlocked

    /** A character is unlocked once its default style is. */
    fun isUnlocked(character: Character): Boolean =
        isUnlocked(CharacterStyle.defaultFor(character))

    companion object {
        /** Rewards everyone starts with. */
        val FREE: Set<Reward> = Reward.all.filter { it.unlockAtCalmMinutes == 0 }.toSet()
    }
}

/** Implemented in the data package on its own Preferences DataStore ("progress"). */
interface ProgressRepository {
    val progress: Flow<Progress>

    /** Adds calm minutes and unlocks whatever the new total earns, atomically. */
    suspend fun addCalmMinutes(minutes: Int)

    /** The saved noise debt, for the listening service to pick up where it left off. */
    suspend fun ledger(): LedgerState

    /**
     * Credits [earnedMinutes] (see [addCalmMinutes]) and saves [state], atomically. A state older than the
     * saved one is ignored, so commits that land out of order can't bring back paid-off debt.
     */
    suspend fun commitLedger(earnedMinutes: Int, state: LedgerState)

    /**
     * Once per install: seeds the total from existing history (calm minutes still in the database plus
     * [Unlocks.CHALLENGE_BONUS_MINUTES] per won challenge) so early users get credit. No-op afterwards.
     */
    suspend fun backfillIfNeeded()

    /** Marks rewards as celebrated so they are not announced again. */
    suspend fun markCelebrated(rewards: Collection<Reward>)
}

/**
 * What CalmLedger carries between listening sessions, so pausing doesn't wipe noise debt.
 * [calmMs] is calm time not yet worth a minute; [episodeStartsMs] are recent yelling episodes (wall clock).
 */
data class LedgerState(
    val debtMs: Long = 0,
    val calmMs: Long = 0,
    val episodeStartsMs: List<Long> = emptyList(),
    val updatedAtMs: Long = 0,
)
