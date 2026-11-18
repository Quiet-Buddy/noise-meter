package com.quietbuddy.ui.home

import androidx.compose.ui.res.stringResource
import com.quietbuddy.ui.findActivity
import com.quietbuddy.R
import androidx.compose.ui.semantics.onClick
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.theme.LocalPalette

// Home's full-screen "stage" mode: the character and meter only, with the system bars hidden.
/**
 * Tells MainActivity about the full-screen flag (so it can hide the tabs) and hides the system bars while it is on.
 * Everything is undone when the flag turns off or Home leaves composition, so no other tab ever inherits the mode.
 */
@Composable
internal fun FullScreenEffect(fullScreen: Boolean, onFullScreenChange: (Boolean) -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current
    DisposableEffect(fullScreen) {
        onFullScreenChange(fullScreen)
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (fullScreen && controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (fullScreen) onFullScreenChange(false)
        }
    }
}
/**
 * Full-screen "stage" mode for a device left on a shelf: just the speech bubble and the biggest character that fits.
 * Pats still work on the character; a tap anywhere else, or Back, leaves. A faint hint explains this for a few seconds.
 */
@Composable
internal fun FullScreenStage(
    speech: String,
    mood: Mood?,
    db: Int?,
    tapCount: Int,
    onTap: () -> Unit,
    onExit: () -> Unit,
    chip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    BackHandler(onBack = onExit)
    var hintShown by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(3000)
        hintShown = false
    }
    val hintAlpha by animateFloatAsState(if (hintShown) 1f else 0f, tween(600), label = "exitHint")
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onExit)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        // The bubble (two lines plus tail) sits above the character and the dB chip below it, in both orientations;
        // 240dp leaves room for both plus the exit hint at the bottom.
        val size = minOf(maxWidth * 0.9f, maxHeight - 240.dp).coerceAtLeast(120.dp)
        Column(Modifier.widthIn(max = size.coerceAtLeast(320.dp)), horizontalAlignment = Alignment.CenterHorizontally) {
            SpeechBubble(text = speech)
            Spacer(Modifier.height(4.dp))
            BuddyStage(mood = mood, db = db, size = size, tapCount = tapCount, onTap = onTap)
            Spacer(Modifier.height(16.dp))
            chip()
        }
        Text(
            stringResource(R.string.home_full_screen_exit_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkSoft,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).alpha(hintAlpha),
        )
    }
}
