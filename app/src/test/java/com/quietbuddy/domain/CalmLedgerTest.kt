package com.quietbuddy.domain

import com.quietbuddy.core.Thresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLedgerTest {

    private val t = Thresholds(worriedFromDb = 65f, sadFromDb = 75f)
    private val calm = 50f
    private val chatter = 70f
    private val yell = 78f
    private val ears = 90f

    private val hourS = 3600

    /**
     * Feeds [seconds] of levels (second by second, in 100 ms frames like the service) and returns minutes earned.
     * The first frame only sets the clock, so chained calls should start where the previous one ended.
     */
    private fun CalmLedger.feed(levelAt: (Int) -> Float, seconds: Int = hourS, startMs: Long = 0): Int {
        var earned = 0
        for (frame in 0..seconds * 10) {
            val ms = startMs + frame * 100L
            earned += update(levelAt(maxOf(0, frame - 1) / 10), t, ms)
        }
        return earned
    }

    /** A [lengthS]-second burst at [level] starting at [offsetS] and every [everyS] after (0 = once). */
    private fun bursts(level: Float, lengthS: Int, everyS: Int = 0, offsetS: Int = 300): (Int) -> Float = { s ->
        val inBurst = s >= offsetS && (if (everyS == 0) s - offsetS else (s - offsetS) % everyS) < lengthS
        if (inBurst) level else calm
    }

    private fun hour(levelAt: (Int) -> Float) = CalmLedger().feed(levelAt)

    @Test fun quietHourEarnsSixty() = assertEquals(60, hour { calm })

    @Test fun oneYellCostsAboutAMinute() {
        assertEquals(59, hour(bursts(yell, 3)))
        assertEquals(58, hour(bursts(ears, 3)))
    }

    @Test fun twoMinutesOfYellingCostsTheCapAndMore() =
        assertEquals(60 - 2 - 20, hour(bursts(yell, 120)))

    @Test fun twoMinutesOfChatterIsMild() = assertEquals(56, hour(bursts(chatter, 120)))

    @Test fun repeatedYellingEscalates() {
        assertEquals(56, hour(bursts(yell, 3, everyS = 1200)))
        // Exactly 10 min apart, the yell 30 min back has just left the window (x4 at most)...
        assertEquals(45, hour(bursts(yell, 3, everyS = 600)))
        // ...a little closer together, it hasn't (x8).
        assertEquals(36, hour(bursts(yell, 3, everyS = 570)))
        assertEquals(14, hour(bursts(yell, 3, everyS = 300)))
        assertTrue(hour(bursts(yell, 3, everyS = 120)) <= 8)
    }

    private val oneYellDebtMs = 3 * 1000L * CalmLedger.SAD_RATE

    @Test fun yellsFarApartDoNotEscalate() {
        val ledger = CalmLedger()
        ledger.feed({ yell }, seconds = 3)
        assertEquals(oneYellDebtMs, ledger.state.debtMs)
        ledger.feed({ calm }, seconds = 30 * 60, startMs = 3_000)
        ledger.feed({ yell }, seconds = 3, startMs = 3_000 + 30 * 60_000L)
        assertEquals(oneYellDebtMs, ledger.state.debtMs)
    }

    @Test fun debtSurvivesARestart() {
        val before = CalmLedger()
        before.feed(bursts(yell, 120, offsetS = 0), seconds = 120)
        val resumed = CalmLedger(before.state)
        // The 20-minute cap has to be paid back before anything is earned.
        assertEquals(0, resumed.feed({ calm }, seconds = 20 * 60, startMs = 10 * 60_000L))
        assertEquals(1, resumed.feed({ calm }, seconds = 60, startMs = 30 * 60_000L))
    }

    @Test fun recentEpisodesCarryOverARestart() {
        val before = CalmLedger()
        before.feed({ yell }, seconds = 3)
        val resumed = CalmLedger(before.state.copy(debtMs = 0))
        resumed.feed({ yell }, seconds = 3, startMs = 10 * 60_000L)
        assertEquals(2 * oneYellDebtMs, resumed.state.debtMs)
    }

    @Test fun skippedTimeCountsForNothing() {
        val ledger = CalmLedger()
        ledger.update(calm, t, 0)
        ledger.skip(10 * 60_000L)
        assertEquals(0, ledger.update(calm, t, 10 * 60_000L + 100))
        assertEquals(100, ledger.state.calmMs)
    }

    @Test fun aStalledCaptureDoesNotEarnTheGap() {
        val ledger = CalmLedger()
        ledger.update(calm, t, 0)
        assertEquals(0, ledger.update(calm, t, 5 * 60_000L))
    }
}
