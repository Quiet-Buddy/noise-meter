package com.quietbuddy.ui

/**
 * Navigation routes and the screen composables that MainActivity's NavHost calls.
 * Every screen gets its ViewModel with hiltViewModel() itself.
 *   OnboardingScreen(onDone: () -> Unit)           ui/onboarding
 *   HomeScreen()                                   ui/home
 *   HistoryScreen()                                ui/history
 *   SettingsScreen(onOpenCalibration: () -> Unit)  ui/settings
 *   CalibrationScreen(onBack: () -> Unit)          ui/settings
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val CALIBRATION = "calibration"
}
