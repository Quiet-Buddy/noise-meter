package com.quietbuddy.domain

import com.quietbuddy.audio.LevelProcessor
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.Character
import com.quietbuddy.core.LedgerState
import com.quietbuddy.core.LevelSample
import com.quietbuddy.core.MinuteAggregate
import com.quietbuddy.core.Mood

/**
 * Everything one listening session does with the microphone frames, without Android: levels, Buddy's mood,
 * alert sounds, calm minutes and per-minute history. ListeningService feeds it frames and carries out what it
 * asks for through [Output] (publish to the UI, play a sound, save, update the notification).
 *
 * Threading: [onFrame] runs on the capture thread; [finish] runs once capture has stopped. [settings], [silenced]
 * and [ledgerLoaded] may be set from the main thread at any time.
 */
class ListeningSession(
    sampleRate: Int,
    settings: AppSettings,
    private val out: Output,
) {
    interface Output {
        fun sample(sample: LevelSample)
        fun mood(mood: Mood)
        /** Plays the alert sound; returns its length in ms, or 0 if nothing played. */
        fun playAlert(character: Character, mood: Mood): Long
        fun minute(minute: MinuteAggregate, dominantMood: Mood)
        /** Saves the ledger and credits [earnedMinutes]; once a minute and when the session finishes. */
        fun ledger(earnedMinutes: Int, state: LedgerState)
        fun notification(mood: Mood, levelDb: Float)
    }

    private val processor = LevelProcessor(sampleRate, settings.calibrationOffsetDb)

    @Volatile var settings: AppSettings = settings
        set(value) {
            field = value
            processor.calibrationOffsetDb = value.calibrationOffsetDb
        }

    /** True while another app has the mic and we only get zeros. */
    @Volatile var silenced = false

    // Earns calm minutes; null until the saved debt has loaded.
    @Volatile private var ledger: CalmLedger? = null

    private val engine = MoodEngine()
    private val alerts = AlertPolicy()
    // Per-minute mood counts, for the minute's dominant mood.
    private val moodCounts = IntArray(Mood.entries.size)
    // While our own alert tone is in the mic's rolling window, hold the mood so Buddy doesn't react to it.
    private var holdMoodUntilMs = 0L
    // Minutes the ledger has earned since the last commit.
    private var pendingCalmMinutes = 0
    private var lastNotifMs = 0L
    private var lastNotifMood: Mood? = null

    fun ledgerLoaded(saved: LedgerState) {
        ledger = CalmLedger(saved)
    }

    fun onFrame(frame: ShortArray, count: Int, ts: Long) {
        // Leave silenced frames out entirely: fed in, they read as a perfectly calm room, so history filled up
        // with ~0 dB calm minutes and Buddy drifted to happy.
        if (silenced) {
            ledger?.skip(ts)
            return
        }
        val s = settings
        val r = processor.process(frame, count, ts)
        out.sample(r.sample)
        val holding = ts < holdMoodUntilMs
        val mood = if (holding) engine.current else engine.update(r.sample.laeq2sDb, s.baseThresholds, ts)
        out.mood(mood)
        moodCounts[mood.ordinal]++
        ledger?.let { l ->
            // Our own alert tone reads as noise; it shouldn't count either way.
            if (holding) l.skip(ts)
            else pendingCalmMinutes += l.update(r.sample.laeq2sDb, s.baseThresholds, ts)
        }

        if (s.chimeOnSad) {
            alerts.onMood(mood, ts)?.let { alertMood ->
                val lengthMs = out.playAlert(s.character, alertMood)
                if (lengthMs > 0) holdMoodUntilMs = ts + lengthMs + LevelProcessor.DEFAULT_WINDOW_MS + ALERT_TAIL_MS
            }
        } else {
            alerts.reset()
        }

        r.completedMinute?.let { recordMinute(it) }

        if (mood != lastNotifMood || ts - lastNotifMs >= NOTIFICATION_REFRESH_MS) {
            lastNotifMood = mood
            lastNotifMs = ts
            out.notification(mood, r.sample.laeq2sDb)
        }
    }

    /** Records the partial minute and saves the ledger. Call once, after the last [onFrame]. */
    fun finish() {
        processor.flushMinute()?.let { recordMinute(it) } ?: commitLedger()
        ledger = null
    }

    private fun recordMinute(m: MinuteAggregate) {
        var best = 0
        for (i in moodCounts.indices) if (moodCounts[i] > moodCounts[best]) best = i
        moodCounts.fill(0)
        out.minute(m, Mood.entries[best])
        commitLedger()
    }

    private fun commitLedger() {
        val state = ledger?.state ?: return
        val earned = pendingCalmMinutes
        pendingCalmMinutes = 0
        out.ledger(earned, state)
    }

    companion object {
        /** Extra hold after an alert, on top of its length and the level window. */
        const val ALERT_TAIL_MS = 200L
        /** The notification's level is refreshed at least this often, and on every mood change. */
        const val NOTIFICATION_REFRESH_MS = 10_000L
    }
}
