package com.quietbuddy

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.quietbuddy.core.ProgressRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class QuietBuddyApp : Application() {
    @Inject lateinit var progress: ProgressRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Credit existing history toward style unlocks, once per install.
        appScope.launch { progress.backfillIfNeeded() }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_LISTENING, getString(R.string.svc_channel_listening), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
        )
    }

    companion object {
        const val CHANNEL_LISTENING = "listening"
    }
}
