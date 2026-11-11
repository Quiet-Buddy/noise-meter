package com.quietbuddy.domain

import com.quietbuddy.core.Mood
import com.quietbuddy.core.Thresholds

/**
 * Turns a stream of LAeq 2 s values into a stable Buddy mood.
 *
 * - Hysteresis: moving to a calmer mood needs the level to fall [hysteresisDb]
 *   below that mood's lower edge. Moving louder is accepted immediately.
 * - Dwell: a changed candidate must persist continuously for [dwellMs] before
 *   [current] changes.
 */
class MoodEngine(
    initial: Mood = Mood.HAPPY,
    private val dwellMs: Long = 3_000,
    private val hysteresisDb: Float = 2f,
) {
    private var _current: Mood = initial
    private var pendingMood: Mood? = null
    private var pendingSinceMs: Long = 0L

    val current: Mood
        get() = _current

    /** Feed one LAeq 2 s value. Returns the (possibly unchanged) current mood. */
    fun update(levelDb: Float, thresholds: Thresholds, nowMs: Long): Mood {
        val raw = thresholds.rawMood(levelDb)
        val candidate = when {
            // Calmer than current: only accept once the level is well below the current band's edge.
            raw.ordinal < _current.ordinal &&
                levelDb >= thresholds.lowerEdge(_current) - hysteresisDb -> _current
            else -> raw
        }

        if (candidate == _current) {
            pendingMood = null
            return _current
        }

        if (candidate != pendingMood) {
            pendingMood = candidate
            pendingSinceMs = nowMs
        }

        if (nowMs - pendingSinceMs >= dwellMs) {
            _current = candidate
            pendingMood = null
        }
        return _current
    }

    fun reset(mood: Mood = Mood.HAPPY) {
        _current = mood
        pendingMood = null
        pendingSinceMs = 0L
    }
}
