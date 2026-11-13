package com.quietbuddy.service

import android.os.Build
import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.quietbuddy.MainActivity
import com.quietbuddy.QuietBuddyApp
import com.quietbuddy.R
import com.quietbuddy.audio.AudioCapture
import com.quietbuddy.audio.LevelBus
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.Character
import com.quietbuddy.core.HistoryRepository
import com.quietbuddy.core.ListeningState
import com.quietbuddy.core.MinuteAggregate
import com.quietbuddy.core.Mood
import com.quietbuddy.core.ProgressRepository
import com.quietbuddy.core.SettingsRepository
import com.quietbuddy.domain.AlertPolicy
import com.quietbuddy.domain.CalmLedger
import com.quietbuddy.domain.MoodEngine
import com.quietbuddy.domain.character.Voices
import com.quietbuddy.audio.LevelProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ListeningService : LifecycleService() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.quietbuddy.core.AppLanguage.wrap(newBase))
    }

    @Inject lateinit var bus: LevelBus
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var history: HistoryRepository
    @Inject lateinit var progress: ProgressRepository

    @Volatile private var settings = AppSettings()
    private var capture: AudioCapture? = null
    private var processor: LevelProcessor? = null
    private val engine = MoodEngine()
    private var running = false

    // Per-minute mood counts; touched only from the capture thread (and after it has stopped).
    private val moodCounts = IntArray(Mood.values().size)
    private var lastNotifMs = 0L
    private var lastNotifMood: Mood? = null
    private val alerts = AlertPolicy()
    private var sounder: AlertSounder? = null
    // While our own alert tone is in the mic's rolling window, hold the mood so Buddy doesn't react to it.
    private var holdMoodUntilMs = 0L
    // Earns calm minutes; null until the saved debt has loaded. Fed from the capture thread.
    @Volatile private var ledger: CalmLedger? = null
    @Volatile private var silenced = false
    // Minutes the ledger has earned since the last commit; capture thread only (and after it has stopped).
    private var pendingCalmMinutes = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PAUSE -> pauseAndStop()
            else -> startListening()
        }
        return START_NOT_STICKY
    }

    private fun hasMic() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun startListening() {
        if (running) return
        if (!hasMic()) {
            bus.setState(ListeningState.NO_PERMISSION)
            stopSelf()
            return
        }
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, buildNotification(Mood.HAPPY, null),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } catch (e: Exception) {
            // e.g. ForegroundServiceStartNotAllowedException
            bus.setState(ListeningState.PAUSED)
            stopSelf()
            return
        }
        running = true
        engine.reset()
        alerts.reset()
        holdMoodUntilMs = 0L
        moodCounts.fill(0)
        bus.setMood(Mood.HAPPY)
        silenced = false
        ledger = null
        pendingCalmMinutes = 0

        lifecycleScope.launch {
            launch(Dispatchers.IO) {
                val saved = progress.ledger()
                if (running) ledger = CalmLedger(saved)
            }
            launch { settingsRepo.settings.collect { s ->
                settings = s
                processor?.calibrationOffsetDb = s.calibrationOffsetDb
            } }
            launch(Dispatchers.IO) { history.prune(System.currentTimeMillis()) }
        }

        val cap = AudioCapture(
            context = this,
            onFrame = ::onFrame,
            onFailure = { onCaptureFailure() },
            onSilenced = { isSilenced ->
                silenced = isSilenced
                bus.setState(if (isSilenced) ListeningState.MIC_BUSY else ListeningState.LISTENING)
            },
        )
        val rate = cap.prepare()
        if (rate == null) {
            onCaptureFailure()
            return
        }
        capture = cap
        processor = LevelProcessor(rate, settings.calibrationOffsetDb)
        bus.setState(ListeningState.LISTENING)
        cap.start()
    }

    private fun onFrame(frame: ShortArray, count: Int, ts: Long) {
        val p = processor ?: return
        val s = settings
        val r = p.process(frame, count, ts)
        bus.emitSample(r.sample)
        val mood = if (ts < holdMoodUntilMs) engine.current
        else engine.update(r.sample.laeq2sDb, s.baseThresholds, ts)
        bus.setMood(mood)
        moodCounts[mood.ordinal]++
        ledger?.let { l ->
            // A silenced mic reads as silence and our own tone as noise; neither should count.
            if (silenced || ts < holdMoodUntilMs) l.skip(ts)
            else pendingCalmMinutes += l.update(r.sample.laeq2sDb, s.baseThresholds, ts)
        }

        if (s.chimeOnSad) {
            alerts.onMood(mood, ts)?.let { alertMood ->
                val sounder = sounder ?: AlertSounder().also { sounder = it }
                val lengthMs = sounder.play(s.character, alertMood)
                if (lengthMs > 0) holdMoodUntilMs = ts + lengthMs + LevelProcessor.DEFAULT_WINDOW_MS + 200
            }
        } else {
            alerts.reset()
        }

        r.completedMinute?.let { recordMinute(it) }

        if (mood != lastNotifMood || ts - lastNotifMs >= 10_000) {
            lastNotifMood = mood
            lastNotifMs = ts
            val canNotify = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (canNotify) {
                runCatching {
                    NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification(mood, r.sample.laeq2sDb))
                }
            }
        }
    }

    private fun recordMinute(m: MinuteAggregate) {
        var best = 0
        for (i in moodCounts.indices) if (moodCounts[i] > moodCounts[best]) best = i
        val dominant = Mood.values()[best]
        moodCounts.fill(0)
        lifecycleScope.launch(Dispatchers.IO + NonCancellable) { history.recordMinute(m, dominant) }
        commitLedger()
    }

    /** Saves the ledger and credits what it earned; once a minute and on shutdown. */
    private fun commitLedger() {
        val state = ledger?.state ?: return
        val earned = pendingCalmMinutes
        pendingCalmMinutes = 0
        lifecycleScope.launch(Dispatchers.IO + NonCancellable) { progress.commitLedger(earned, state) }
    }

    private fun onCaptureFailure() {
        capture?.stop()
        capture = null
        bus.setState(ListeningState.MIC_BUSY)
        finishMinuteAndShutdown(ListeningState.MIC_BUSY)
    }

    private fun pauseAndStop() {
        if (!running) {
            bus.setState(ListeningState.PAUSED)
            stopSelf()
            return
        }
        capture?.stop()
        capture = null
        finishMinuteAndShutdown(ListeningState.PAUSED)
    }

    private fun finishMinuteAndShutdown(finalState: ListeningState) {
        running = false
        processor?.flushMinute()?.let { recordMinute(it) }
        processor = null
        commitLedger()
        ledger = null
        bus.clearLatest()
        bus.setState(finalState)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun label(m: Mood) = getString(
        when (m) {
            Mood.HAPPY -> R.string.svc_mood_happy
            Mood.FINE -> R.string.svc_mood_fine
            Mood.WORRIED -> R.string.svc_mood_worried
            Mood.SAD -> R.string.svc_mood_sad
            Mood.EARS_COVERED -> R.string.svc_mood_ears
        }
    )

    private fun buildNotification(mood: Mood, db: Float?): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val pause = PendingIntent.getService(
            this, 1, Intent(this, ListeningService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = if (db == null) label(mood)
        else getString(R.string.svc_text, label(mood), Math.round(db))
        return NotificationCompat.Builder(this, QuietBuddyApp.CHANNEL_LISTENING)
            .setSmallIcon(
                when (settings.character) {
                    Character.BUDDY -> R.drawable.ic_buddy_notification
                    Character.KITTEN -> R.drawable.ic_kitten_notification
                    Character.DUCK -> R.drawable.ic_duck_notification
                    Character.ROBOT -> R.drawable.ic_robot_notification
                }
            )
            .setContentTitle(getString(Voices.of(settings.character).listeningTitle, settings.roomName))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open)
            .addAction(0, getString(R.string.svc_pause), pause)
            .build()
    }

    override fun onDestroy() {
        capture?.stop()
        capture = null
        sounder?.release()
        sounder = null
        if (running) {
            running = false
            bus.setState(ListeningState.PAUSED)
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.quietbuddy.action.START"
        const val ACTION_PAUSE = "com.quietbuddy.action.PAUSE"
        private const val NOTIF_ID = 1
    }
}
