package com.quietbuddy.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MergeMinutesTest {
    private fun row(laeq: Float, min: Float, max: Float, mood: String, seconds: Int) =
        LevelMinuteEntity(60_000L, laeq, min, max, mood, seconds)

    @Test fun pauseAndResumeInsideAMinuteKeepsBothParts() {
        // 40 s before a pause, 10 s after resuming: the second part used to replace the first.
        val merged = mergeMinutes(row(50f, 40f, 62f, "HAPPY", 40), row(50f, 45f, 58f, "FINE", 10))
        assertEquals(50, merged.listeningSeconds)
        assertEquals(50f, merged.laeqDb, 0.001f)
        assertEquals(40f, merged.minDb)
        assertEquals(62f, merged.maxDb)
        assertEquals("HAPPY", merged.dominantMood)
        assertEquals(60_000L, merged.minuteStartMs)
    }

    @Test fun levelIsTheEnergyAverageWeightedByTime() {
        // Equal halves at 60 and 70 dB: mean energy (10^6 + 10^7) / 2, about 67.4 dB, not the plain 65.
        val merged = mergeMinutes(row(60f, 60f, 60f, "FINE", 30), row(70f, 70f, 70f, "WORRIED", 30))
        assertEquals(67.40f, merged.laeqDb, 0.01f)
    }

    @Test fun moodComesFromTheLongerPart() {
        val merged = mergeMinutes(row(50f, 50f, 50f, "HAPPY", 10), row(70f, 70f, 70f, "WORRIED", 40))
        assertEquals("WORRIED", merged.dominantMood)
    }

    @Test fun listeningTimeNeverPassesAMinute() {
        val merged = mergeMinutes(row(50f, 50f, 50f, "HAPPY", 60), row(50f, 50f, 50f, "HAPPY", 60))
        assertEquals(60, merged.listeningSeconds)
    }

    @Test fun emptyPartsDontDivideByZero() {
        val merged = mergeMinutes(row(50f, 50f, 50f, "HAPPY", 0), row(55f, 55f, 55f, "FINE", 0))
        assertEquals(55f, merged.laeqDb)
    }
}
