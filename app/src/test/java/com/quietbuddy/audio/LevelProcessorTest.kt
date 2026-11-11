package com.quietbuddy.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin

class LevelProcessorTest {
    private val offset = 90f

    private fun sineFrame(rate: Int, freq: Double, amp: Double, startSample: Long, n: Int): ShortArray =
        ShortArray(n) { i ->
            (amp * 32767.0 * sin(2.0 * PI * freq * (startSample + i) / rate)).roundToInt().toShort()
        }

    /** Feeds [frames] 100 ms frames of a sine and returns the last frameDb. */
    private fun settledDb(rate: Int, freq: Double, amp: Double = 0.5, frames: Int = 10): Float {
        val p = LevelProcessor(rate, offset)
        val n = rate / 10
        var last = 0f
        for (k in 0 until frames) {
            last = p.process(sineFrame(rate, freq, amp, k.toLong() * n, n), n, 1_000L + k * 100L).sample.frameDb
        }
        return last
    }

    private val expected1k = (20 * log10(0.5 / Math.sqrt(2.0)) + offset).toFloat()

    @Test fun oneKhz48k() = assertEquals(expected1k, settledDb(48_000, 1000.0), 0.5f)

    @Test fun oneKhz44k() = assertEquals(expected1k, settledDb(44_100, 1000.0), 0.5f)

    @Test fun hundredHzAttenuated() {
        for (rate in listOf(48_000, 44_100)) {
            val diff = settledDb(rate, 1000.0) - settledDb(rate, 100.0)
            assertEquals("rate $rate", 19.1f, diff, 1.5f)
        }
    }

    @Test fun silenceIsLow() {
        val p = LevelProcessor(48_000, offset)
        val r = p.process(ShortArray(4800), 4800, 0L)
        assertEquals(0f, r.sample.frameDb, 0.001f)
        assertTrue(r.sample.laeq2sDb < 1f)
    }

    @Test fun laeqIsEnergyAverage() {
        // Constant-amplitude frames at ~60 and ~80 dB; DC-free sine so the filter settles.
        val rate = 48_000
        val n = rate / 10
        val p = LevelProcessor(rate, offset)
        // Settle first with 1 kHz at 60 dB level, then alternate 60/80 dB for 20 frames (2 s).
        val amp60 = Math.sqrt(2.0) * Math.pow(10.0, (60 - offset) / 20.0)
        val amp80 = Math.sqrt(2.0) * Math.pow(10.0, (80 - offset) / 20.0)
        var pos = 0L
        var t = 0L
        var last = 0f
        for (k in 0 until 40) {
            val amp = if (k % 2 == 0) amp60 else amp80
            t += 100
            last = p.process(sineFrame(rate, 1000.0, amp, pos, n), n, t).sample.laeq2sDb
            pos += n
        }
        assertEquals(77f, last, 0.5f)
        assertTrue(last > 72f)
    }

    @Test fun minuteRollover() {
        val rate = 48_000
        val n = rate / 10
        val p = LevelProcessor(rate, offset)
        val minute0 = 10 * 60_000L
        var completed = 0
        var agg: com.quietbuddy.core.MinuteAggregate? = null
        var pos = 0L
        // 600 frames = 60 s ending exactly at minute0 + 60 s - 100 ms .. use end times inside the minute.
        for (k in 0 until 600) {
            val r = p.process(sineFrame(rate, 1000.0, 0.1, pos, n), n, minute0 + 100L * (k + 1) - 1)
            pos += n
            if (r.completedMinute != null) { completed++; agg = r.completedMinute }
        }
        assertEquals(0, completed)
        val r = p.process(sineFrame(rate, 1000.0, 0.1, pos, n), n, minute0 + 60_000L + 50)
        assertNotNull(r.completedMinute)
        agg = r.completedMinute
        assertEquals(minute0, agg!!.minuteStartMs)
        assertEquals(60, agg.listeningSeconds)
        assertTrue(agg.minDb <= agg.laeqDb && agg.laeqDb <= agg.maxDb)
        assertNull(p.process(sineFrame(rate, 1000.0, 0.1, pos, n), n, minute0 + 60_200L).completedMinute)
        assertNotNull(p.flushMinute())
        assertNull(p.flushMinute())
    }
}
