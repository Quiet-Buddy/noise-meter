package com.quietbuddy.ui.home

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Home starts listening by itself once per process: when the app is opened, not every time Home comes back
 * (tab switches, rotation, theme or language changes). Lives in the singleton graph because HomeScreen, its
 * ViewModel and even the activity come and go; a session the user paused (on Home or from the notification)
 * then stays paused until they resume it.
 */
@Singleton
class LaunchAutoStart @Inject constructor() {
    private var done = false

    /** True the first time it is called in this process, false ever after. */
    @Synchronized
    fun consume(): Boolean {
        if (done) return false
        done = true
        return true
    }
}
