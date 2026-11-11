package com.quietbuddy.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.quietbuddy.audio.LevelBus
import com.quietbuddy.core.ListeningController
import com.quietbuddy.core.ListeningState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceListeningController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bus: LevelBus,
) : ListeningController {
    override fun start() {
        try {
            ContextCompat.startForegroundService(
                context, Intent(context, ListeningService::class.java).setAction(ListeningService.ACTION_START),
            )
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException and friends: stay paused.
            bus.setState(ListeningState.PAUSED)
        }
    }

    override fun pause() {
        // Only meaningful if the service is running; a plain startService from the background may be refused.
        if (bus.state.value == ListeningState.PAUSED) return
        try {
            context.startService(Intent(context, ListeningService::class.java).setAction(ListeningService.ACTION_PAUSE))
        } catch (e: Exception) {
            context.stopService(Intent(context, ListeningService::class.java))
            bus.setState(ListeningState.PAUSED)
        }
    }
}
