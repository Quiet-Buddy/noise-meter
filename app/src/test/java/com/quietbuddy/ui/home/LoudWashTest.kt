package com.quietbuddy.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.quietbuddy.core.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoudWashTest {
    private val calm = Color(0xFFFBF6F1)
    private val t = Thresholds(worriedFromDb = 60f, sadFromDb = 70f) // fine from 50, ears from 80

    @Test
    fun `calm band and not listening keep the palette colour`() {
        assertEquals(LoudWash(calm, calm), loudWash(null, t, calm))
        assertEquals(LoudWash(calm, calm), loudWash(40f, t, calm))
        assertEquals(LoudWash(calm, calm), loudWash(50f, t, calm))
    }

    @Test
    fun `deepens steadily with every decibel`() {
        val tops = (50..85).map { loudWash(it.toFloat(), t, calm).top.luminance() }
        tops.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", b <= a + 1e-4f) }
        assertTrue(tops.last() < tops.first() - 0.4f)
    }

    @Test
    fun `no jump at a band edge`() {
        for (edge in listOf(60f, 70f, 80f)) {
            val below = loudWash(edge - 0.1f, t, calm).top
            val above = loudWash(edge + 0.1f, t, calm).top
            assertTrue("jump at $edge", kotlin.math.abs(below.luminance() - above.luminance()) < 0.01f)
        }
    }

    @Test
    fun `follows the room's thresholds`() {
        val shifted = t.shiftedBy(10f)
        assertEquals(loudWash(65f, t, calm), loudWash(75f, shifted, calm))
    }

    @Test
    fun `every palette's ink stays readable on the deepest top`() {
        val deepest = loudWash(120f, t, calm).top.luminance()
        // The darkest-to-lightest range of the palettes' ink colours.
        for (ink in listOf(Color(0xFF17293D), Color(0xFF2B2414), Color(0xFF2E211B), Color(0xFF2A2340), Color(0xFF1B2B26))) {
            val contrast = (deepest + 0.05f) / (ink.luminance() + 0.05f)
            assertTrue("contrast $contrast for $ink", contrast >= 3f)
        }
    }
}
