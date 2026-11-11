package com.quietbuddy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Widest a single column of cards gets before it is centred, so cards don't stretch across a tablet. */
val SingleColumnMaxWidth = 640.dp

/** Widest a two-pane layout gets on very large screens. */
val TwoPaneMaxWidth = 1100.dp

/**
 * True when there is room to place content side by side: a landscape-shaped area that is wide enough
 * for two columns of cards. Covers phones in landscape and tablets in landscape.
 */
fun isTwoPane(width: Dp, height: Dp): Boolean = width > height && width >= 480.dp

/** True on tablets and unfolded foldables (smallest width 600dp or more), whatever the orientation. */
@Composable
fun isLargeScreen(): Boolean = LocalConfiguration.current.smallestScreenWidthDp >= 600

/**
 * Use a side navigation rail instead of the bottom bar when the window is wide (tablets) or landscape
 * (phones on their side), where a bottom bar would take too much of the short height.
 */
@Composable
fun useNavigationRail(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp >= 600 || config.screenWidthDp > config.screenHeightDp
}
