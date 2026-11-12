package com.quietbuddy

import android.os.Bundle
import androidx.compose.ui.platform.LocalView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.navigation.NavHostController
import com.quietbuddy.ui.useNavigationRail
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.ListeningController
import com.quietbuddy.core.SettingsRepository
import com.quietbuddy.ui.Routes
import com.quietbuddy.ui.history.HistoryScreen
import com.quietbuddy.ui.home.HomeScreen
import com.quietbuddy.ui.onboarding.OnboardingScreen
import com.quietbuddy.ui.settings.CalibrationScreen
import com.quietbuddy.ui.settings.SettingsScreen
import com.quietbuddy.ui.theme.QuietBuddyTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    val controller: ListeningController,
) : ViewModel() {
    /** Null until the first settings emission. */
    val settings: StateFlow<AppSettings?> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.quietbuddy.core.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuietBuddyTheme { Root() }
        }
    }
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Rounded.Home),
    Tab(Routes.HISTORY, R.string.nav_history, Icons.Rounded.BarChart),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
)

@Composable
private fun Root(vm: MainViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val loaded = settings ?: return

    // Stop listening when the UI goes away, unless the user asked to keep listening.
    val keep by rememberUpdatedState(loaded.keepListening)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP && !keep) vm.controller.pause()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Keep the display on while the app is in front; the flag is dropped automatically when it leaves.
    val view = LocalView.current
    DisposableEffect(view, loaded.keepScreenOn) {
        view.keepScreenOn = loaded.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val start = remember(Unit) { if (loaded.onboardingDone) Routes.HOME else Routes.ONBOARDING }
    val nav = rememberNavController()
    val backEntry by nav.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route

    val showTabs = tabs.any { it.route == route }
    // Tablets and phones on their side get a rail at the start edge; a bottom bar would eat the short height.
    val rail = useNavigationRail()
    fun select(tab: Tab) {
        nav.navigate(tab.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        // Include the display cutout so content clears the camera notch in landscape.
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        bottomBar = {
            if (showTabs && !rail) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { select(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (showTabs && rail) {
                // Insets are already applied by the Scaffold padding above.
                NavigationRail(windowInsets = WindowInsets(0)) {
                    Spacer(Modifier.weight(1f))
                    tabs.forEach { tab ->
                        NavigationRailItem(
                            selected = route == tab.route,
                            onClick = { select(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
            AppNavHost(nav, start, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, start: String, modifier: Modifier) {
    // consumeWindowInsets in the caller stops screens that also apply statusBarsPadding() from adding the inset twice.
    NavHost(nav, startDestination = start, modifier = modifier) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                nav.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable(Routes.HOME) { HomeScreen() }
        composable(Routes.HISTORY) { HistoryScreen() }
        composable(Routes.SETTINGS) {
            SettingsScreen(onOpenCalibration = { nav.navigate(Routes.CALIBRATION) })
        }
        composable(Routes.CALIBRATION) { CalibrationScreen(onBack = { nav.popBackStack() }) }
    }
}
