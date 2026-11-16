package com.quietbuddy.domain

import com.quietbuddy.core.Mood
import com.quietbuddy.core.Thresholds

/**
 * Turns a stream of LAeq 2 s values into a stable Buddy mood.
 *
 * Asymmetric on purpose: reacting to noise should feel immediate, calming down should be steady.
 *
 * - Louder: once the level has been above the current mood's band continuously for [riseDwellMs],
 *   the mood rises to the calmest louder mood seen during that time (so a brief peak can't jump
 *   straight to the top, but a level wobbling between two loud bands still escalates).
 * - Calmer: once the level has been at least [hysteresisDb] below the current band's lower edge
 *   continuously for [fallDwellMs], the mood falls to the loudest calmer mood seen during that time
 *   (so a level wobbling between two calmer bands still calms down, but only as far as the room
 *   has consistently been). Any reading back within the hysteresis margin restarts the wait.
 */
class MoodEngine(
    initial: Mood = Mood.HAPPY,
    private val riseDwellMs: Long = 800,
    private val fallDwellMs: Long = 3_000,
    private val hysteresisDb: Float = 2f,
) {
    private var _current: Mood = initial

    // Pending escalation: louder than current since [riseSinceMs]; [riseMin] is the calmest louder mood seen.
    private var riseMin: Mood? = null
    private var riseSinceMs = 0L

    // Pending calm-down: calmer than current since [fallSinceMs]; [fallMax] is the loudest calmer mood seen.
    private var fallMax: Mood? = null
    private var fallSinceMs = 0L

    val current: Mood
        get() = _current

    /** Feed one LAeq 2 s value. Returns the (possibly unchanged) current mood. */
    fun update(levelDb: Float, thresholds: Thresholds, nowMs: Long): Mood {
        val raw = thresholds.rawMood(levelDb)

        when {
            raw > _current -> {
                fallMax = null
                val min = riseMin
                if (min == null) {
                    riseMin = raw
                    riseSinceMs = nowMs
                } else if (raw < min) {
                    riseMin = raw
                }
                if (nowMs - riseSinceMs >= riseDwellMs) {
                    _current = riseMin!!
                    riseMin = null
                }
            }
            raw < _current && levelDb < thresholds.lowerEdge(_current) - hysteresisDb -> {
                riseMin = null
                val max = fallMax
                if (max == null) {
                    fallMax = raw
                    fallSinceMs = nowMs
                } else if (raw > max) {
                    fallMax = raw
                }
                if (nowMs - fallSinceMs >= fallDwellMs) {
                    _current = fallMax!!
                    fallMax = null
                }
            }
            // Same band, or calmer but still within the hysteresis margin: nothing pending.
            else -> {
                riseMin = null
                fallMax = null
            }
        }
        return _current
    }

    fun reset(mood: Mood = Mood.HAPPY) {
        _current = mood
        riseMin = null
        riseSinceMs = 0L
        fallMax = null
        fallSinceMs = 0L
    }
}
