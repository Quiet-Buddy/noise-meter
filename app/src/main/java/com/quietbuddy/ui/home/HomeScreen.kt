package com.quietbuddy.ui.home

import androidx.compose.ui.res.stringResource
import com.quietbuddy.ui.cannotAskAgain
import com.quietbuddy.ui.hasMicPermission
import com.quietbuddy.ui.openAppSettings
import com.quietbuddy.R
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import com.quietbuddy.ui.SingleColumnMaxWidth
import com.quietbuddy.ui.TwoPaneMaxWidth
import com.quietbuddy.ui.isLargeScreen
import com.quietbuddy.ui.isTwoPane
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietbuddy.core.ListeningState
import com.quietbuddy.ui.theme.LocalPalette

@Composable
fun HomeScreen(onOpenCharacters: () -> Unit, onFullScreenChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.start()
        } else if (context.cannotAskAgain(Manifest.permission.RECORD_AUDIO)) {
            // Permanently denied: the system will no longer show the dialog, so send the user to settings.
            context.openAppSettings()
        }
    }

    fun wake() {
        if (context.hasMicPermission()) viewModel.start() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Runs on every return to Home; autoStart only acts once per process, so a user's pause sticks.
    LaunchedEffect(Unit) {
        if (context.hasMicPermission()) viewModel.autoStart()
    }

    // The background follows the dB continuously (see LoudWash); the short tween only smooths the 100 ms updates.
    val wash = loudWash(state.db?.toFloat(), state.thresholds, palette.background)
    val washTop by animateColorAsState(wash.top, tween(400), label = "washTop")
    val washBottom by animateColorAsState(wash.bottom, tween(400), label = "washBottom")
    val background = Brush.verticalGradient(listOf(washTop, washBottom))
    // Kept here (not in MainActivity) so it survives rotation; MainActivity only needs it to hide the tabs.
    var fullScreen by rememberSaveable { mutableStateOf(false) }
    FullScreenEffect(fullScreen, onFullScreenChange)
    val listening = state.listening == ListeningState.LISTENING

    // On tablets the device usually sits on a shelf and is read from across the room, so Buddy and the
    // status chip get much bigger there. The cards stay at their normal size.
    val large = isLargeScreen()

    val header: @Composable () -> Unit = {
        Header(
            roomName = state.roomName,
            listening = listening,
            onToggle = { if (listening) viewModel.pause() else wake() },
            onFullScreen = { fullScreen = true },
        )
    }
    val chip: @Composable () -> Unit = {
        NoiseMeter(mood = state.mood, db = state.db, thresholds = state.thresholds, listening = state.listening, large = large)
    }
    // Everything below the status chip: wake prompt and the reward strip.
    val details: @Composable () -> Unit = {
        // The wake prompt takes the tiles' place instead of sitting above them, so the layout (and Buddy's
        // size) is identical whether or not we're listening. The tiles stay in the layout, invisible, to keep the height.
        Box(Modifier.fillMaxWidth()) {
            val hidden = if (listening) Modifier else Modifier.alpha(0f).clearAndSetSemantics {}
            Box(hidden) {
                ProgressTiles(
                    calmMinutes = state.calmMinutes,
                    reward = state.nextReward,
                    styles = state.styles,
                    minutesToNext = state.minutesToNext,
                    progress = state.nextRewardProgress,
                    onClick = onOpenCharacters,
                )
            }
            if (!listening) {
                Column(
                    Modifier.matchParentSize().pointerInput(Unit) {},
                    verticalArrangement = Arrangement.Center,
                ) {
                    NotListeningMessage(state.listening)
                    DarkButton(text = stringResource(R.string.home_wake_buddy), onClick = ::wake)
                }
            }
        }
    }
    val stage: @Composable (Dp) -> Unit = { size ->
        SpeechBubble(text = stringResource(state.speechLine))
        Spacer(Modifier.height(4.dp))
        BuddyStage(mood = state.mood, db = state.db, size = size, tapCount = state.tapCount, onTap = viewModel::onCharacterTapped)
    }
    val maxCharacter = if (large) 560.dp else 400.dp

    UnlockDialog(
        unlocks = state.pendingUnlocks,
        styles = state.styles,
        onTry = { viewModel.dismissUnlocks(it) },
        onLater = { viewModel.dismissUnlocks(null) },
    )

    if (fullScreen) {
        FullScreenStage(
            speech = stringResource(state.speechLine),
            mood = state.mood,
            db = state.db,
            tapCount = state.tapCount,
            onTap = viewModel::onCharacterTapped,
            onExit = { fullScreen = false },
            // Always the large chip: full screen is for reading the room from across it.
            chip = { NoiseMeter(mood = state.mood, db = state.db, thresholds = state.thresholds, listening = state.listening, large = true) },
            modifier = Modifier.background(background),
        )
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(background).statusBarsPadding()) {
        if (isTwoPane(maxWidth, maxHeight)) {
            // Landscape: Buddy and his speech bubble take the full height on the left, the rest sits on the right.
            Row(
                Modifier
                    .align(Alignment.Center)
                    .widthIn(max = TwoPaneMaxWidth)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    // The bubble (two lines plus tail) sits above the character.
                    val size = minOf(maxWidth, maxHeight - 110.dp).coerceIn(120.dp, maxCharacter)
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        stage(size)
                    }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        header()
                        Spacer(Modifier.height(if (large) 24.dp else 12.dp))
                        chip()
                        Spacer(Modifier.height(if (large) 24.dp else 12.dp))
                        details()
                    }
                }
            }
        } else {
            // Portrait: one column. Buddy takes the width, or the height left after the rest of the content,
            // so the listening screen fits without scrolling on typical phones; taller states (wake
            // button) scroll. On tablets the column is capped in width and centred.
            val contentWidth = minOf(maxWidth, SingleColumnMaxWidth) - 48.dp
            // Buddy absorbs whatever height the rest of the content leaves: the non-Buddy height is measured
            // (content height minus Buddy's current height), so the result is stable and doesn't loop.
            val density = LocalDensity.current
            var otherHeightPx by remember { mutableStateOf(with(density) { 300.dp.toPx() }) }
            val available = maxHeight - 28.dp
            val size = minOf(contentWidth, available - with(density) { otherHeightPx.toDp() })
                .coerceIn(160.dp, maxCharacter)
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = SingleColumnMaxWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 16.dp)
                    // fillMaxSize gives the content a minimum height of the whole screen; drop it before measuring,
                    // or a short layout reads as screen-tall and Buddy can never grow back after shrinking (e.g. when
                    // the system bars return after full screen).
                    .wrapContentHeight(Alignment.Top)
                    .onSizeChanged { otherHeightPx = it.height - with(density) { size.toPx() } },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                header()
                Spacer(Modifier.height(8.dp))
                stage(size)
                Spacer(Modifier.height(16.dp))
                chip()
                Spacer(Modifier.height(12.dp))
                details()
            }
        }
    }
}
/** Room name, the full-screen button and the mic toggle on one compact row. */
@Composable
private fun Header(roomName: String, listening: Boolean, onToggle: () -> Unit, onFullScreen: () -> Unit) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(roomName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        IconButton(
            onClick = onFullScreen,
            modifier = Modifier.size(48.dp).background(palette.card, CircleShape),
        ) {
            Icon(
                imageVector = Icons.Rounded.Fullscreen,
                contentDescription = stringResource(R.string.home_full_screen),
                tint = palette.ink,
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(48.dp).background(palette.card, CircleShape),
        ) {
            Icon(
                imageVector = if (listening) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                contentDescription = stringResource(if (listening) R.string.home_pause_listening else R.string.home_resume_listening),
                tint = palette.ink,
            )
        }
    }
}
@Composable
private fun NotListeningMessage(listening: ListeningState) {
    val palette = LocalPalette.current
    val message = when (listening) {
        ListeningState.NO_PERMISSION -> stringResource(R.string.home_needs_mic)
        ListeningState.MIC_BUSY -> stringResource(R.string.home_mic_busy)
        else -> null
    } ?: return
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = palette.inkSoft,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
@Composable
private fun DarkButton(text: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = Color.White),
    ) {
        // Centred so a label that wraps in longer languages (Spanish, Basque) stays balanced.
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
    }
}
