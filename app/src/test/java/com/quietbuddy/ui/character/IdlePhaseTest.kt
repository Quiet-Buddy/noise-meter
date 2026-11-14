package com.quietbuddy.ui.character

import kotlin.math.PI
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdlePhaseTest {
    @Test
    fun advancesByTheSpeedOfEachFrame() {
        val phase = IdlePhase()
        phase.advance(hz = 0.5f, dtSec = 0.25f)
        assertEquals((PI / 4).toFloat(), phase.radians, 1e-5f)
        phase.advance(hz = 2f, dtSec = 0.125f)
        assertEquals((3 * PI / 4).toFloat(), phase.radians, 1e-5f)
    }

    @Test
    fun aSpeedChangeNeverSkipsAfterAWhile() {
        // The old sin(time * 2π * hz) skipped by 2π * Δhz * time when hz changed; after 100 s that is a big jump.
        val phase = IdlePhase()
        repeat(6000) { phase.advance(0.9f, 1f / 60f) }
        val before = phase.radians
        phase.advance(0.2f, 1f / 60f)
        val step = (phase.radians - before + 2 * PI.toFloat()) % (2 * PI.toFloat())
        assertEquals(2 * PI.toFloat() * 0.2f / 60f, step, 1e-4f)
    }

    @Test
    fun staysWithinOneTurn() {
        val phase = IdlePhase()
        repeat(100_000) { phase.advance(3f, 1f / 60f) }
        assertTrue(abs(phase.radians) < 2 * PI.toFloat())
    }
}

/**
 * Runs a character's animator for a while, then through every mood change, and checks that each idle angle only
 * ever moves on by its own speed: never more than [maxHz] cycles per second in one frame.
 */
internal fun assertIdleRhythmsNeverSkip(
    setMood: (com.quietbuddy.core.Mood?) -> Unit,
    advance: (Float) -> Unit,
    angles: () -> List<Float>,
    maxHz: Float,
) {
    val moods = listOf(
        com.quietbuddy.core.Mood.HAPPY, com.quietbuddy.core.Mood.FINE, com.quietbuddy.core.Mood.WORRIED,
        com.quietbuddy.core.Mood.SAD, com.quietbuddy.core.Mood.EARS_COVERED, null,
    )
    val dt = 1f / 60f
    val turn = (2 * PI).toFloat()
    fun moved(before: Float, after: Float) = ((after - before) % turn + turn) % turn
    repeat(6000) { advance(dt) }   // 100 s in: the old clock-based sine skipped further the longer it ran
    for (from in moods) for (to in moods) {
        setMood(from)
        repeat(120) { advance(dt) }
        setMood(to)
        repeat(120) {
            val before = angles()
            advance(dt)
            val after = angles()
            for (i in before.indices) {
                assertTrue("$from -> $to, rhythm $i", moved(before[i], after[i]) <= turn * maxHz * dt + 1e-4f)
            }
        }
    }
}
