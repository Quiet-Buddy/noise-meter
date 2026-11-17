package com.quietbuddy.domain

import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.Character
import com.quietbuddy.core.LedgerState
import com.quietbuddy.core.LevelSample
import com.quietbuddy.core.MinuteAggregate
import com.quietbuddy.core.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class ListeningSessionTest {
    private class Recorder : ListeningSession.Output {
        val samples = mutableListOf<LevelSample>()
        val moods = mutableListOf<Mood>()
        val alerts = mutableListOf<Pair<Mood, Long>>()
        val minutes = mutableListOf<Pair<MinuteAggregate, Mood>>()
        val ledgers = mutableListOf<Pair<Int, LedgerState>>()
        val notifications = mutableListOf<Pair<Mood, Long>>()
        var alertLengthMs = 1_500L
        var now = 0L

        override fun sample(sample: LevelSample) { samples += sample }
        override fun mood(mood: Mood) { moods += mood }
        override fun playAlert(character: Character, mood: Mood): Long { alerts += mood to now; return alertLengthMs }
        override fun minute(minute: MinuteAggregate, dominantMood: Mood) { minutes += minute to dominantMood }
        override fun ledger(earnedMinutes: Int, state: LedgerState) { ledgers += earnedMinutes to state }
        override fun notification(mood: Mood, levelDb: Float) { notifications += mood to now }
    }

    private val rate = 48_000
    private val frameSize = rate / 10
    private val quiet = ShortArray(frameSize)
    // A full-scale 1 kHz tone: far above every preset's top band with the default calibration.
    private val loud = ShortArray(frameSize) { i -> (32_000 * sin(2 * PI * 1_000 * i / rate)).toInt().toShort() }

    private val out = Recorder()
    private var ts = 60_000L * 1_000 // on a minute boundary

    private fun session(settings: AppSettings = AppSettings()) = ListeningSession(rate, settings, out)

    /** Feeds [seconds] of 100 ms frames. */
    private fun ListeningSession.feed(frame: ShortArray, seconds: Int) {
        repeat(seconds * 10) {
            out.now = ts
            onFrame(frame, frame.size, ts)
            ts += 100
        }
    }

    @Test fun quietMinuteIsRecordedAsHappyWhenTheMinuteRollsOver() {
        val s = session()
        s.ledgerLoaded(LedgerState())
        s.feed(quiet, 61)

        assertEquals(1, out.minutes.size)
        val (minute, mood) = out.minutes.single()
        assertEquals(60_000L * 1_000, minute.minuteStartMs)
        assertEquals(60, minute.listeningSeconds)
        assertEquals(Mood.HAPPY, mood)
        // The ledger is saved with every minute.
        assertEquals(1, out.ledgers.size)
    }

    @Test fun silencedFramesAreLeftOutOfLevelsAndHistory() {
        val s = session()
        s.feed(quiet, 20)
        s.silenced = true
        s.feed(quiet, 100) // past the end of the first minute and through the next
        val samplesWhileSilenced = out.samples.size
        s.silenced = false
        s.feed(quiet, 1)

        assertEquals(200, samplesWhileSilenced)
        // Only the 20 s before the silence were recorded for the first minute; the silenced minute has no row.
        assertEquals(listOf(20), out.minutes.map { it.first.listeningSeconds })
    }

    @Test fun finishRecordsThePartialMinuteAndSavesTheLedgerOnce() {
        val s = session()
        s.ledgerLoaded(LedgerState())
        s.feed(quiet, 25)
        s.finish()

        assertEquals(listOf(25), out.minutes.map { it.first.listeningSeconds })
        assertEquals(1, out.ledgers.size)
    }

    @Test fun ledgerIsOnlySavedOnceItHasLoaded() {
        val s = session()
        s.feed(quiet, 61)
        assertTrue(out.ledgers.isEmpty())
        assertEquals(1, out.minutes.size)
    }

    @Test fun loudRoomPlaysOneAlertAndHoldsTheMoodWhileItPlays() {
        val s = session(AppSettings(chimeOnSad = true))
        s.feed(loud, 4)

        assertEquals(1, out.alerts.size)
        val (alertMood, at) = out.alerts.single()
        // While the tone and the level window it fills are still in the mic, the mood doesn't move.
        val holdEnd = at + 1_500 + 2_000 + ListeningSession.ALERT_TAIL_MS
        val heldMoods = out.moods.drop(((at - 60_000L * 1_000) / 100).toInt())
        assertTrue(heldMoods.take(((holdEnd - at) / 100).toInt().coerceAtMost(heldMoods.size)).all { it == alertMood })
    }

    @Test fun noAlertsWhenSoundAlertsAreOff() {
        val s = session(AppSettings(chimeOnSad = false))
        s.feed(loud, 10)
        assertTrue(out.alerts.isEmpty())
        assertTrue(out.moods.last() > Mood.FINE)
    }

    @Test fun notificationFollowsMoodChangesAndRefreshesEveryTenSeconds() {
        val s = session(AppSettings(chimeOnSad = false))
        s.feed(quiet, 25)
        // First frame, then every 10 s.
        assertEquals(listOf(0L, 10_000L, 20_000L), out.notifications.map { it.second - 60_000L * 1_000 })

        s.feed(loud, 3)
        // Each mood change on the way up is shown straight away.
        val changes = out.moods.zipWithNext().count { (a, b) -> a != b }
        assertEquals(3 + changes, out.notifications.size)
    }

    @Test fun calibrationChangesApplyToTheNextFrame() {
        val s = session()
        s.feed(loud, 1)
        val before = out.samples.last().frameDb
        s.settings = AppSettings(calibrationOffsetDb = AppSettings.DEFAULT_CALIBRATION_OFFSET_DB - 10f)
        s.feed(loud, 1)
        assertEquals(before - 10f, out.samples.last().frameDb, 0.01f)
    }
}
