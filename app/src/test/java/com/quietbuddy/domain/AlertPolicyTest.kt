package com.quietbuddy.domain

import com.quietbuddy.core.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertPolicyTest {

    @Test
    fun calmMoodsAreSilent() {
        val p = AlertPolicy()
        assertNull(p.onMood(Mood.HAPPY, 0L))
        assertNull(p.onMood(Mood.FINE, 1_000L))
    }

    @Test
    fun reachingWorriedSoundsImmediately() {
        val p = AlertPolicy()
        assertEquals(Mood.WORRIED, p.onMood(Mood.WORRIED, 0L))
        assertNull(p.onMood(Mood.WORRIED, 100L))
    }

    @Test
    fun escalationSoundsAfterShortGap() {
        val p = AlertPolicy()
        p.onMood(Mood.WORRIED, 0L)
        assertNull(p.onMood(Mood.SAD, 1_000L))
        assertEquals(Mood.SAD, p.onMood(Mood.SAD, 1_500L))
        assertEquals(Mood.EARS_COVERED, p.onMood(Mood.EARS_COVERED, 3_000L))
    }

    @Test
    fun stayingLoudRepeatsFasterWhenLouder() {
        val p = AlertPolicy()
        assertEquals(Mood.EARS_COVERED, p.onMood(Mood.EARS_COVERED, 0L))
        assertNull(p.onMood(Mood.EARS_COVERED, 14_900L))
        assertEquals(Mood.EARS_COVERED, p.onMood(Mood.EARS_COVERED, 15_000L))

        val q = AlertPolicy()
        q.onMood(Mood.SAD, 0L)
        assertNull(q.onMood(Mood.SAD, 15_000L))
        assertEquals(Mood.SAD, q.onMood(Mood.SAD, 30_000L))
    }

    @Test
    fun calmingDownReArmsButRespectsMinGap() {
        val p = AlertPolicy()
        p.onMood(Mood.SAD, 0L)
        assertNull(p.onMood(Mood.FINE, 2_000L))
        assertNull(p.onMood(Mood.SAD, 3_000L))
        assertEquals(Mood.SAD, p.onMood(Mood.SAD, 5_000L))
    }

    @Test
    fun droppingOneBandThenRisingAgainSounds() {
        val p = AlertPolicy()
        p.onMood(Mood.SAD, 0L)
        assertNull(p.onMood(Mood.WORRIED, 10_000L))
        assertEquals(Mood.SAD, p.onMood(Mood.SAD, 12_000L))
    }
}
