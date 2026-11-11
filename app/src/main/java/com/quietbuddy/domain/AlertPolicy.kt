package com.quietbuddy.domain

import com.quietbuddy.core.Mood

/**
 * Decides when to play an audible alert, and which one, as Buddy's mood changes.
 *
 * - Getting louder (reaching a louder mood than the last one alerted) sounds right away.
 * - Staying loud repeats a reminder, more often the louder it is.
 * - Calming down re-arms the alert, so going loud again sounds again.
 * - After calming down, a new alert waits until [minGapMs] since the last one, so a mood bouncing
 *   on an edge doesn't nag. Escalating while still loud only waits [escalationGapMs], so the
 *   previous tone can finish.
 */
class AlertPolicy(
    private val minGapMs: Long = 5_000,
    private val escalationGapMs: Long = 1_500,
    private val repeatMs: (Mood) -> Long? = ::defaultRepeatMs,
) {
    private var lastMood: Mood? = null
    private var lastAlertMs = Long.MIN_VALUE / 2

    /** Feed the current mood. Returns the mood to sound an alert for, or null for silence. */
    fun onMood(mood: Mood, nowMs: Long): Mood? {
        if (mood < Mood.WORRIED) {
            lastMood = null
            return null
        }
        val last = lastMood
        val sinceLast = nowMs - lastAlertMs
        val escalating = last == null || mood > last
        val sound = when {
            escalating && last != null -> sinceLast >= escalationGapMs
            sinceLast < minGapMs -> false
            escalating -> true
            else -> repeatMs(mood)?.let { sinceLast >= it } ?: false
        }
        if (sound) lastAlertMs = nowMs
        // Track the mood even when silent, so dropping a band and coming back counts as escalation.
        if (sound || (last != null && mood < last)) lastMood = mood
        return if (sound) mood else null
    }

    fun reset() {
        lastMood = null
        lastAlertMs = Long.MIN_VALUE / 2
    }

    companion object {
        fun defaultRepeatMs(mood: Mood): Long? = when (mood) {
            Mood.WORRIED -> 60_000
            Mood.SAD -> 30_000
            Mood.EARS_COVERED -> 15_000
            else -> null
        }
    }
}
