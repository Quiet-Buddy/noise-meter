package com.quietbuddy.core

import com.quietbuddy.BuildConfig
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * A scheduled window in which a limited [character] can be earned. [from] and [until] are both inclusive
 * local dates. [key] is persisted (per-event minutes, seen events); never change an existing key, and give a
 * comeback of the same character a new one. Two events for the same character must not overlap.
 */
data class LimitedEvent(val key: String, val character: Character, val from: LocalDate, val until: LocalDate) {
    /** Local midnight at the start of [from]. */
    fun startMs(zone: ZoneId): Long = from.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Local midnight after [until]; exclusive. */
    fun endMs(zone: ZoneId): Long = until.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun isActive(nowMs: Long, zone: ZoneId): Boolean = nowMs >= startMs(zone) && nowMs < endMs(zone)

    /** Days left including [today], so it is at least 1 while the event is active. */
    fun daysLeft(today: LocalDate): Int = (ChronoUnit.DAYS.between(today, until) + 1).toInt()
}

/** The reward Home points at: [minutesToGo] is at least 1, [fraction] (0..1) is how far along its ladder step it is. */
data class NextReward(val reward: Reward, val minutesToGo: Int, val fraction: Float)

/**
 * The rules for limited characters. Pure and unit-tested: every function takes the events, the time and the
 * zone explicitly. Inside a window a limited reward costs calm minutes earned since the window opened
 * ([Progress.eventMinutes]); rewards not earned by the time it closes disappear, earned ones are kept.
 */
object LimitedEvents {
    private val SNOWMAN_TEST = LimitedEvent(
        key = "snowman_test_2026",
        character = Character.SNOWMAN,
        from = LocalDate.of(2026, 11, 14),
        until = LocalDate.of(2026, 12, 5),
    )

    /**
     * Mari Domingi visits every Christmas from December 1 to 26. Each year is its own event, so a class that missed
     * some of her rewards can earn them the next December.
     */
    private val MARI_DOMINGI_CHRISTMAS = (2026..2040).map { year ->
        LimitedEvent(
            key = "mari_domingi_$year",
            character = Character.MARI_DOMINGI,
            from = LocalDate.of(year, 12, 1),
            until = LocalDate.of(year, 12, 26),
        )
    }

    /** Lets debug builds try Mari Domingi before her first December; it ends the day before the real event starts. */
    private val MARI_DOMINGI_TEST = LimitedEvent(
        key = "mari_domingi_test_2026",
        character = Character.MARI_DOMINGI,
        from = LocalDate.of(2026, 11, 14),
        until = LocalDate.of(2026, 11, 30),
    )

    /** The snowman and Mari's early test events only run in debug builds. */
    val scheduled: List<LimitedEvent> =
        MARI_DOMINGI_CHRISTMAS + if (BuildConfig.DEBUG) listOf(SNOWMAN_TEST, MARI_DOMINGI_TEST) else emptyList()

    fun active(events: List<LimitedEvent>, nowMs: Long, zone: ZoneId): List<LimitedEvent> =
        events.filter { it.isActive(nowMs, zone) }

    fun activeFor(character: Character, events: List<LimitedEvent>, nowMs: Long, zone: ZoneId): LimitedEvent? =
        active(events, nowMs, zone).firstOrNull { it.character == character }

    /** The event character's rewards that [minutes] earned in the window pay for. */
    fun earnedIn(event: LimitedEvent, minutes: Long): Set<Reward> =
        Reward.all.filter { it.character == event.character && it.unlockAtCalmMinutes <= minutes }.toSet()

    /** Limited rewards show while earnable (their event is active) and forever once earned. */
    fun isVisible(reward: Reward, progress: Progress, events: List<LimitedEvent>, nowMs: Long, zone: ZoneId): Boolean =
        !reward.isLimited || progress.isUnlocked(reward) || activeFor(reward.character, events, nowMs, zone) != null

    /** A limited character shows while its event is active, and afterwards only if its default style was earned. */
    fun isVisible(character: Character, progress: Progress, events: List<LimitedEvent>, nowMs: Long, zone: ZoneId): Boolean =
        !character.isLimited || progress.isUnlocked(character) || activeFor(character, events, nowMs, zone) != null

    /**
     * The characters the Characters page lists, in order: those with an active event first (they have a deadline),
     * then the rest in their usual order. Once its event ends, a kept limited character goes back to its usual place.
     */
    fun charactersInOrder(progress: Progress, events: List<LimitedEvent>, nowMs: Long, zone: ZoneId): List<Character> =
        Character.entries
            .filter { isVisible(it, progress, events, nowMs, zone) }
            .sortedBy { if (activeFor(it, events, nowMs, zone) != null) 0 else 1 }

    /** Minutes counted towards [reward]: the lifetime total, or the active event's minutes (0 if none) for limited rewards. */
    fun minutesTowards(reward: Reward, progress: Progress, events: List<LimitedEvent>, nowMs: Long, zone: ZoneId): Long {
        if (!reward.isLimited) return progress.calmMinutes
        val event = activeFor(reward.character, events, nowMs, zone) ?: return 0
        return progress.eventMinutes[event.key] ?: 0
    }

    /**
     * The single next reward for Home: the fewest minutes to go among the lifetime ladder's next reward and each
     * active event's cheapest unearned reward. Ties go to the lifetime one.
     */
    fun next(progress: Progress, events: List<LimitedEvent>, nowMs: Long, zone: ZoneId): NextReward? {
        val lifetime = Unlocks.next(progress.calmMinutes)?.let {
            candidate(it, progress.calmMinutes, Unlocks.previousThreshold(progress.calmMinutes))
        }
        val fromEvents = active(events, nowMs, zone).mapNotNull { event ->
            val minutes = progress.eventMinutes[event.key] ?: 0
            val ladder = Reward.all.filter { it.character == event.character }
            val reward = ladder.filter { it !in progress.unlocked && it.unlockAtCalmMinutes > minutes }
                .minByOrNull { it.unlockAtCalmMinutes } ?: return@mapNotNull null
            val previous = ladder.filter { it.unlockAtCalmMinutes <= minutes }.maxOfOrNull { it.unlockAtCalmMinutes } ?: 0
            candidate(reward, minutes, previous)
        }
        return (listOfNotNull(lifetime) + fromEvents).minByOrNull { it.minutesToGo }
    }

    private fun candidate(reward: Reward, minutes: Long, previous: Int): NextReward {
        val span = reward.unlockAtCalmMinutes - previous
        return NextReward(
            reward = reward,
            minutesToGo = (reward.unlockAtCalmMinutes - minutes).toInt().coerceAtLeast(1),
            fraction = ((minutes - previous).toFloat() / span).coerceIn(0f, 1f),
        )
    }
}
