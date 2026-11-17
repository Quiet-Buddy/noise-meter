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
import com.quietbuddy.core.LedgerState
import com.quietbuddy.core.LevelSample
import com.quietbuddy.core.ListeningState
import com.quietbuddy.core.MinuteAggregate
import com.quietbuddy.core.Mood
import com.quietbuddy.core.ProgressRepository
import com.quietbuddy.core.SettingsRepository
import com.quietbuddy.domain.ListeningSession
import com.quietbuddy.domain.character.Voices
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

    // Latest settings, for the notification; the session gets its own copy.
    @Volatile private var settings = AppSettings()
    private var capture: AudioCapture? = null
    private var session: ListeningSession? = null
    private var sounder: AlertSounder? = null
    private var running = false

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
        bus.setMood(Mood.HAPPY)

        val cap = AudioCapture(
            context = this,
            onFrame = { frame, count, ts -> session?.onFrame(frame, count, ts) },
            onFailure = { onCaptureFailure() },
            onSilenced = { isSilenced ->
                session?.silenced = isSilenced
                bus.setState(if (isSilenced) ListeningState.MIC_BUSY else ListeningState.LISTENING)
            },
        )
        val rate = cap.prepare()
        if (rate == null) {
            onCaptureFailure()
            return
        }
        val newSession = ListeningSession(rate, settings, output)
        session = newSession

        lifecycleScope.launch {
            launch(Dispatchers.IO) {
                val saved = progress.ledger()
                if (session === newSession) newSession.ledgerLoaded(saved)
            }
            launch { settingsRepo.settings.collect { s ->
                settings = s
                newSession.settings = s
            } }
            launch(Dispatchers.IO) { history.prune(System.currentTimeMillis()) }
        }

        capture = cap
        bus.setState(ListeningState.LISTENING)
        cap.start()
    }

    /** Carries out what the session asks for; called on the capture thread. */
    private val output = object : ListeningSession.Output {
        override fun sample(sample: LevelSample) = bus.emitSample(sample)

        override fun mood(mood: Mood) = bus.setMood(mood)

        override fun playAlert(character: Character, mood: Mood): Long =
            (sounder ?: AlertSounder().also { sounder = it }).play(character, mood)

        override fun minute(minute: MinuteAggregate, dominantMood: Mood) {
            lifecycleScope.launch(Dispatchers.IO + NonCancellable) { history.recordMinute(minute, dominantMood) }
        }

        override fun ledger(earnedMinutes: Int, state: LedgerState) {
            lifecycleScope.launch(Dispatchers.IO + NonCancellable) { progress.commitLedger(earnedMinutes, state) }
        }

        override fun notification(mood: Mood, levelDb: Float) {
            val canNotify = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(this@ListeningService, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (canNotify) {
                runCatching {
                    NotificationManagerCompat.from(this@ListeningService).notify(NOTIF_ID, buildNotification(mood, levelDb))
                }
            }
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
        session?.finish()
        session = null
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
                    Character.SNOWMAN -> R.drawable.ic_snowman_notification
                    Character.MARI_DOMINGI -> R.drawable.ic_mari_notification
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
