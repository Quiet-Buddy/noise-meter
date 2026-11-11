package com.quietbuddy.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
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
import com.quietbuddy.core.HistoryRepository
import com.quietbuddy.core.ListeningState
import com.quietbuddy.core.MinuteAggregate
import com.quietbuddy.core.Mood
import com.quietbuddy.core.SettingsRepository
import com.quietbuddy.domain.MoodEngine
import com.quietbuddy.audio.LevelProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ListeningService : LifecycleService() {
    @Inject lateinit var bus: LevelBus
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var history: HistoryRepository

    @Volatile private var settings = AppSettings()
    private var capture: AudioCapture? = null
    private var processor: LevelProcessor? = null
    private val engine = MoodEngine()
    private var running = false

    // Per-minute mood counts; touched only from the capture thread (and after it has stopped).
    private val moodCounts = IntArray(Mood.values().size)
    private var lastNotifMs = 0L
    private var lastNotifMood: Mood? = null
    private var lastChimeMs = 0L
    private var tone: ToneGenerator? = null

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
        moodCounts.fill(0)
        bus.setMood(Mood.HAPPY)

        lifecycleScope.launch {
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
            onSilenced = { silenced ->
                bus.setState(if (silenced) ListeningState.MIC_BUSY else ListeningState.LISTENING)
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
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val before = engine.current
        val mood = engine.update(r.sample.laeq2sDb, s.effectiveThresholds(minuteOfDay), ts)
        bus.setMood(mood)
        moodCounts[mood.ordinal]++

        if (s.chimeOnSad && mood != before && mood >= Mood.SAD && before < Mood.SAD) chime(ts)

        r.completedMinute?.let { recordMinute(it) }

        if (mood != lastNotifMood || ts - lastNotifMs >= 10_000) {
            lastNotifMood = mood
            lastNotifMs = ts
            runCatching {
                NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification(mood, r.sample.laeq2sDb))
            }
        }
    }

    private fun recordMinute(m: MinuteAggregate) {
        var best = 0
        for (i in moodCounts.indices) if (moodCounts[i] > moodCounts[best]) best = i
        val dominant = Mood.values()[best]
        moodCounts.fill(0)
        lifecycleScope.launch(Dispatchers.IO + NonCancellable) { history.recordMinute(m, dominant) }
    }

    private fun chime(now: Long) {
        if (now - lastChimeMs < 2 * 60_000L) return
        lastChimeMs = now
        runCatching {
            val t = tone ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 40).also { tone = it }
            t.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
        }
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
            .setSmallIcon(R.drawable.ic_buddy_notification)
            .setContentTitle(getString(R.string.svc_title, settings.roomName))
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
        tone?.release()
        tone = null
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
