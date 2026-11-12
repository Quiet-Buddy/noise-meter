package com.quietbuddy.audio

import com.quietbuddy.core.Character
import com.quietbuddy.core.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class AlertSoundsTest {
    private val loud = listOf(Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED)

    @Test
    fun calmMoodsHaveNoSound() {
        for (c in Character.entries) {
            assertEquals(0, AlertSounds.variantCount(c, Mood.HAPPY))
            assertEquals(0, AlertSounds.render(c, Mood.FINE, 0).size)
        }
    }

    @Test
    fun loudMoodsHaveSeveralDistinctVariants() {
        for (c in Character.entries) {
            for (mood in loud) {
                val n = AlertSounds.variantCount(c, mood)
                assertTrue("$mood variants", n >= 3)
                val renders = (0 until n).map { AlertSounds.render(c, mood, it).toList() }
                assertEquals("$mood variants differ", n, renders.toSet().size)
            }
        }
    }

    @Test
    fun soundsAreShortEnoughNotToBlockTheMeterLong() {
        for (c in Character.entries) {
            for (mood in loud) for (v in 0 until AlertSounds.variantCount(c, mood)) {
                val ms = AlertSounds.render(c, mood, v).size * 1000L / AlertSounds.RATE
                assertTrue("$mood/$v is $ms ms", ms in 300..2_000)
            }
        }
    }

    @Test
    fun soundsAreLoudAndLouderMoodsAreLouder() {
        for (c in Character.entries) {
            fun peak(m: Mood, v: Int) = AlertSounds.render(c, m, v).maxOf { abs(it.toInt()) }
            fun rms(m: Mood, v: Int): Double {
                val pcm = AlertSounds.render(c, m, v)
                return sqrt(pcm.sumOf { it.toDouble() * it } / pcm.size)
            }
            for (mood in loud) for (v in 0 until AlertSounds.variantCount(c, mood)) {
                assertTrue("$mood/$v peak", peak(mood, v) > Short.MAX_VALUE * 0.7)
                assertTrue("$mood/$v rms", rms(mood, v) > Short.MAX_VALUE * 0.12)
            }
            fun maxPeak(m: Mood) = (0 until AlertSounds.variantCount(c, m)).maxOf { peak(m, it) }
            assertTrue(maxPeak(Mood.WORRIED) < maxPeak(Mood.SAD))
            assertTrue(maxPeak(Mood.SAD) <= maxPeak(Mood.EARS_COVERED))
        }
    }

    @Test
    fun soundsStartAndEndSoftlyWithoutClicks() {
        for (c in Character.entries) {
            for (mood in loud) for (v in 0 until AlertSounds.variantCount(c, mood)) {
                val pcm = AlertSounds.render(c, mood, v)
                assertFalse("$mood/$v starts with a click", abs(pcm.first().toInt()) > 1_000)
                assertFalse("$mood/$v ends with a click", abs(pcm.last().toInt()) > 1_000)
            }
        }
    }

    @Test
    fun variantIndexWraps() {
        for (c in Character.entries) {
            val n = AlertSounds.variantCount(c, Mood.SAD)
            assertEquals(AlertSounds.render(c, Mood.SAD, 1).toList(), AlertSounds.render(c, Mood.SAD, n + 1).toList())
        }
    }

    @Test
    fun everyCharacterSoundsOffForLoudMoodsOnly() {
        for (ch in Character.entries) {
            for (mood in loud) assertTrue("$ch/$mood", AlertSounds.variantCount(ch, mood) >= 1)
            for (mood in listOf(Mood.HAPPY, Mood.FINE)) assertEquals("$ch/$mood", 0, AlertSounds.variantCount(ch, mood))
        }
    }
}
