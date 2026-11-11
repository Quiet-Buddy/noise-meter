package com.quietbuddy.domain

import com.quietbuddy.core.Mood
import com.quietbuddy.core.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodEngineTest {

    private val thresholds = Thresholds(65f, 75f)

    @Test
    fun steadyQuietStaysHappy() {
        val engine = MoodEngine()
        var t = 0L
        repeat(50) {
            assertEquals(Mood.HAPPY, engine.update(45f, thresholds, t))
            t += 2_000
        }
        assertEquals(Mood.HAPPY, engine.current)
    }

    @Test
    fun loudLevelChangesOnlyAfterDwell() {
        val engine = MoodEngine()
        assertEquals(Mood.HAPPY, engine.update(70f, thresholds, 0L))
        assertEquals(Mood.HAPPY, engine.update(70f, thresholds, 1_000L))
        assertEquals(Mood.HAPPY, engine.update(70f, thresholds, 2_900L))
        // 70 dB is WORRIED under Thresholds(65, 75).
        assertEquals(Mood.WORRIED, engine.update(70f, thresholds, 3_000L))
    }

    @Test
    fun dwellBoundaryIsExactlyThreeSeconds() {
        val engine = MoodEngine()
        engine.update(70f, thresholds, 0L)
        assertEquals(Mood.HAPPY, engine.update(70f, thresholds, 2_999L))
        assertEquals(Mood.WORRIED, engine.update(70f, thresholds, 3_000L))
    }

    @Test
    fun flickerAroundEdgeDoesNotChangeMood() {
        val engine = MoodEngine()
        var t = 0L
        // Alternate between WORRIED (66 dB) and FINE (60 dB) every 100 ms.
        repeat(100) { i ->
            val level = if (i % 2 == 0) 66f else 60f
            assertEquals(Mood.HAPPY, engine.update(level, thresholds, t))
            t += 100
        }
        assertEquals(Mood.HAPPY, engine.current)
    }

    @Test
    fun comingDownFromWorriedNeedsHysteresis() {
        val engine = MoodEngine(initial = Mood.WORRIED)
        // Lower edge of WORRIED is 65; calmer mood needs level < 63.
        engine.update(64f, thresholds, 0L)
        assertEquals(Mood.WORRIED, engine.update(64f, thresholds, 10_000L))
        assertEquals(Mood.WORRIED, engine.update(64f, thresholds, 20_000L))

        engine.update(62f, thresholds, 30_000L)
        assertEquals(Mood.WORRIED, engine.update(62f, thresholds, 32_999L))
        assertEquals(Mood.FINE, engine.update(62f, thresholds, 33_000L))
    }

    @Test
    fun jumpFromHappyToEarsCoveredAfterDwell() {
        val engine = MoodEngine()
        engine.update(90f, thresholds, 0L)
        assertEquals(Mood.HAPPY, engine.update(90f, thresholds, 2_999L))
        assertEquals(Mood.EARS_COVERED, engine.update(90f, thresholds, 3_000L))
    }

    @Test
    fun resetReturnsToGivenMoodAndClearsPending() {
        val engine = MoodEngine()
        engine.update(90f, thresholds, 0L)
        engine.reset()
        assertEquals(Mood.HAPPY, engine.current)

        // Pending timer from before reset must not carry over.
        engine.update(90f, thresholds, 1_000L)
        assertEquals(Mood.HAPPY, engine.update(90f, thresholds, 3_999L))
        assertEquals(Mood.EARS_COVERED, engine.update(90f, thresholds, 4_000L))

        engine.reset(Mood.SAD)
        assertEquals(Mood.SAD, engine.current)
    }
}
