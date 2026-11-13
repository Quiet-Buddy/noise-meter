package com.quietbuddy.ui.home

import com.quietbuddy.ui.formatDuration
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.quietbuddy.core.Thresholds
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import android.Manifest
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.quietbuddy.ui.character.rememberAnimationsOff
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietbuddy.core.ListeningState
import com.quietbuddy.core.Mood
import com.quietbuddy.domain.character.Voices
import com.quietbuddy.ui.character.CharacterView
import com.quietbuddy.ui.character.LocalCharacter
import com.quietbuddy.ui.character.LocalStyle
import com.quietbuddy.ui.character.LocalWardrobe
import com.quietbuddy.ui.character.nameRes
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.Character
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.Reward
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import com.quietbuddy.ui.theme.LocalPalette
import com.quietbuddy.ui.theme.QB

@Composable
fun HomeScreen(onOpenCharacters: () -> Unit, onFullScreenChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.start()
        } else if (context.findActivity()?.let { !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO) } == true) {
            // Permanently denied: the system will no longer show the dialog, so send the user to settings.
            openAppSettings(context)
        }
    }

    fun wake() {
        if (hasMicPermission(context)) viewModel.start() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) {
        if (hasMicPermission(context)) viewModel.start()
    }

    val background by animateColorAsState(palette.screenBackground(state.mood), tween(600), label = "background")
    // Kept here (not in MainActivity) so it survives rotation; MainActivity only needs it to hide the tabs.
    var fullScreen by rememberSaveable { mutableStateOf(false) }
    FullScreenEffect(fullScreen, onFullScreenChange)
    val listening = state.listening == ListeningState.LISTENING

    // On tablets the device usually sits on a shelf and is read from across the room, so Buddy and the
    // status chip get much bigger there. The cards stay at their normal size.
    val large = isLargeScreen()
    val canStartChallenge = listening && (state.mood == Mood.SAD || state.mood == Mood.EARS_COVERED)

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
    // Everything below the status chip: wake prompt, challenge and the reward strip.
    val details: @Composable () -> Unit = {
        if (!listening) {
            NotListeningMessage(state.listening)
            Spacer(Modifier.height(12.dp))
            DarkButton(text = stringResource(R.string.home_wake_buddy), onClick = ::wake)
            Spacer(Modifier.height(16.dp))
        }
        if (state.challenge != ChallengeState.Idle || canStartChallenge) {
            ChallengeSection(
                challenge = state.challenge,
                canStart = canStartChallenge,
                onStart = viewModel::startChallenge,
            )
            Spacer(Modifier.height(12.dp))
        }
        ProgressTiles(
            calmMinutes = state.calmMinutes,
            reward = state.nextReward,
            styles = state.styles,
            minutesToNext = state.minutesToNext,
            progress = state.nextRewardProgress,
            onClick = onOpenCharacters,
        )
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
            // so the listening screen fits without scrolling on typical phones; taller states (challenge,
            // wake button) scroll. On tablets the column is capped in width and centred.
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

/**
 * Today's calm time and the next reward, as two separate tiles side by side: one is what the room has done, the
 * other what it is working towards. The reward tile opens the Characters tab. With everything unlocked the calm
 * tile takes the whole row.
 */
@Composable
private fun ProgressTiles(
    calmMinutes: Int,
    reward: Reward?,
    styles: Map<Character, CharacterStyle>,
    minutesToNext: Int,
    progress: Float,
    onClick: () -> Unit,
) {
    // Equal heights, so the two tiles read as a pair even when one label wraps.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = spacedBy(12.dp)) {
        CalmTile(calmMinutes, Modifier.weight(1f).fillMaxHeight())
        if (reward != null) {
            RewardTile(reward, styles, minutesToNext, progress, onClick, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

private val TileShape = RoundedCornerShape(20.dp)

@Composable
private fun CalmTile(calmMinutes: Int, modifier: Modifier) {
    val palette = LocalPalette.current
    Column(modifier.clip(TileShape).background(palette.card).padding(12.dp)) {
        Text(stringResource(R.string.home_calm_time_today), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft)
        Spacer(Modifier.weight(1f).heightIn(min = 4.dp))
        // May wrap between hours and minutes at large font sizes rather than be cut off.
        Text(
            formatDuration(calmMinutes),
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp),
            color = QB.CalmText,
        )
    }
}

@Composable
private fun RewardTile(
    reward: Reward,
    styles: Map<Character, CharacterStyle>,
    minutesToNext: Int,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val palette = LocalPalette.current
    val animatedProgress by animateFloatAsState(progress, tween(600), label = "rewardProgress")
    // The full "N min to unlock X" sentence is too long for a tile in some languages, so it is only the
    // screen-reader label; on screen the reward's name stands alone and the minutes sit at the end of the bar.
    val sentence = nextRewardText(reward, minutesToNext)
    Column(
        modifier
            .clip(TileShape)
            .background(palette.card)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = sentence }
            .padding(12.dp),
    ) {
        // The label sits on top across the whole tile, like the calm tile's, so it has room in long languages
        // and large font sizes; the preview and the reward's name share the row below it.
        Text(stringResource(R.string.home_next_reward_label), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft)
        Spacer(Modifier.weight(1f).heightIn(min = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = spacedBy(8.dp)) {
            RewardPreview(reward, styles, Modifier.size(32.dp), animated = false)
            Text(
                rewardName(reward),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = palette.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(palette.line)) {
                Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(QB.Calm))
            }
            Text(
                formatDuration(minutesToNext),
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkSoft,
                softWrap = false,
            )
        }
    }
}

/** What the next reward is called: a new friend by the character's name, anything else by its own name. */
@Composable
private fun rewardName(reward: Reward): String =
    if (reward is CharacterStyle && reward.isCharacterDefault) stringResource(Voices.of(reward.character).name)
    else stringResource(reward.nameRes)

@Composable
private fun nextRewardText(reward: Reward, minutes: Int): String = when {
    reward is CharacterStyle && reward.isCharacterDefault ->
        stringResource(R.string.home_next_reward_friend, minutes, stringResource(Voices.of(reward.character).name))
    reward is Accessory -> stringResource(R.string.home_next_reward_accessory, minutes, stringResource(reward.nameRes))
    else -> stringResource(R.string.home_next_reward, minutes, stringResource(reward.nameRes))
}

/** A happy character showing off [reward]: a style on its own character, an accessory on its own character in the selected style. */
@Composable
private fun RewardPreview(reward: Reward, styles: Map<Character, CharacterStyle>, modifier: Modifier, animated: Boolean) {
    when (reward) {
        is CharacterStyle -> CompositionLocalProvider(LocalCharacter provides reward.character, LocalStyle provides reward) {
            CharacterView(mood = Mood.HAPPY, animated = animated, modifier = modifier)
        }
        is Accessory -> {
            val worn = LocalWardrobe.current[reward.character].orEmpty()
            val style = styles[reward.character] ?: CharacterStyle.defaultFor(reward.character)
            CompositionLocalProvider(LocalCharacter provides reward.character, LocalStyle provides style) {
                CharacterView(mood = Mood.HAPPY, animated = animated, modifier = modifier, accessories = worn + reward)
            }
        }
    }
}

/**
 * Tells MainActivity about the full-screen flag (so it can hide the tabs) and hides the system bars while it is on.
 * Everything is undone when the flag turns off or Home leaves composition, so no other tab ever inherits the mode.
 */
@Composable
private fun FullScreenEffect(fullScreen: Boolean, onFullScreenChange: (Boolean) -> Unit) {
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
private fun FullScreenStage(
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

/**
 * Buddy standing on a soft glow in the mood colour. The glow is the loudness meter: it widens and brightens
 * as the room gets louder, so the mood reads from across the room without a ring or a big number.
 */
@Composable
private fun BuddyStage(mood: Mood?, db: Int?, size: Dp, tapCount: Int, onTap: () -> Unit) {
    val palette = LocalPalette.current
    val animationsOff = rememberAnimationsOff()
    val calm = mood == Mood.HAPPY || mood == Mood.FINE
    val motion = rememberBuddyTapMotion()
    val burst = rememberTapBurst()
    val haptics = LocalHapticFeedback.current
    val sizePx = with(LocalDensity.current) { size.toPx() }
    // A calm pat is rewarded: a springy squash leaning away from the finger, hearts where it landed and a tick,
    // building to hops on a streak. A pat in a loud room gets no reward, just a tiny sway.
    val pat by rememberUpdatedState { at: Offset ->
        val x = (at.x / sizePx * 2f - 1f).coerceIn(-1f, 1f)
        if (!calm) {
            if (!animationsOff) motion.wobble(x)
        } else {
            onTap()
            val big = !animationsOff && motion.pat(x)
            haptics.performHapticFeedback(if (big) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick)
            if (!animationsOff) burst.burst(at, big)
        }
    }
    val target = if (mood == null || db == null) 0f else ((db - 30f) / 70f).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, tween(600), label = "glowProgress")
    val glowColor by animateColorAsState(mood?.let { QB.bandColor(it) } ?: palette.line, tween(600), label = "glowColor")

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = this.size.width * (0.7f + 0.4f * progress)
            val alpha = 0.18f + 0.62f * progress
            // Centred at Buddy's feet, in the lower third; a circle gradient squashed into a floor ellipse.
            val center = Offset(this.size.width / 2f, this.size.height * 0.85f)
            withTransform({ scale(1f, 0.28f, pivot = center) }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = alpha), glowColor.copy(alpha = 0f)),
                        center = center,
                        radius = width / 2f,
                    ),
                    radius = width / 2f,
                    center = center,
                )
            }
        }
        val patLabel = stringResource(R.string.home_pat_character)
        CharacterView(
            mood = mood,
            // Taps are read before the motion layers, so the hit area stays put while the character bounces.
            // detectTapGestures consumes the tap, so a parent's own click (full-screen exit) never sees it.
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) { detectTapGestures(onTap = { pat(it) }) }
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick(label = patLabel) {
                        pat(Offset(sizePx / 2f, sizePx / 2f))
                        true
                    }
                }
                .tapMotion(motion),
            pokes = tapCount,
        )
        TapBurstLayer(burst, Modifier.matchParentSize())
    }
}

/** One pill with the reading and the band: "34 dB · Nice and calm", or just the paused/sleeping label. */
/**
 * The live noise level: the dB number and band name over a gauge whose track is split into the room's calm, loud
 * and too-loud zones. A bar follows the level and a tick holds the recent peak for a moment before sinking back,
 * so the room can see the noise rise and fall, not just which band it is in.
 */
@Composable
private fun NoiseMeter(mood: Mood?, db: Int?, thresholds: Thresholds, listening: ListeningState, large: Boolean) {
    val palette = LocalPalette.current
    val label = when (mood) {
        Mood.HAPPY, Mood.FINE -> stringResource(R.string.band_nice_calm)
        Mood.WORRIED -> stringResource(R.string.band_getting_loud)
        Mood.SAD -> stringResource(R.string.band_too_loud)
        Mood.EARS_COVERED -> stringResource(R.string.band_way_too_loud)
        null -> stringResource(if (listening == ListeningState.PAUSED) R.string.band_paused else R.string.band_sleeping)
    }
    val active = mood != null && db != null
    val textColor = mood?.let { QB.bandText(it) } ?: palette.inkSoft
    // The scale: a stretch of quiet below the loud zone, and some room above the too-loud edge.
    val low = thresholds.worriedFromDb - 25f
    val high = thresholds.sadFromDb + 15f
    fun fraction(level: Float) = ((level - low) / (high - low)).coerceIn(0f, 1f)

    val target = if (active) fraction(db!!.toFloat()) else 0f
    val animationsOff = rememberAnimationsOff()
    val level by animateFloatAsState(target, if (animationsOff) snap() else tween(250), label = "meterLevel")
    val currentTarget by rememberUpdatedState(target)
    var peak by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var heldSinceMs = 0L
        // Restarts on every new level; once the tick has sunk to the level it waits, drawing no frames.
        snapshotFlow { currentTarget }.collectLatest { t ->
            if (t >= peak) {
                peak = t
                heldSinceMs = System.currentTimeMillis()
            }
            delay(heldSinceMs + PEAK_HOLD_MS - System.currentTimeMillis())
            var lastMs = withFrameMillis { it }
            while (peak > t) {
                withFrameMillis { now ->
                    peak = maxOf(t, peak - (now - lastMs).coerceAtMost(100) * PEAK_FALL_PER_MS)
                    lastMs = now
                }
            }
        }
    }

    val zones = listOf(
        fraction(thresholds.worriedFromDb) to QB.CalmSoft,
        fraction(thresholds.sadFromDb) to QB.LoudSoft,
        1f to QB.TooLoudSoft,
    )
    val barColor = mood?.let { QB.bandColor(it) } ?: palette.line
    val trackHeight = if (large) 20.dp else 14.dp
    val text = buildAnnotatedString {
        if (active) {
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("$db dB") }
            append(" · ")
        }
        append(label)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(palette.card)
            .padding(horizontal = 16.dp, vertical = if (large) 16.dp else 12.dp),
    ) {
        Text(
            text,
            style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(if (large) 12.dp else 8.dp))
        Canvas(Modifier.fillMaxWidth().height(trackHeight).clip(RoundedCornerShape(50))) {
            var from = 0f
            zones.forEach { (to, color) ->
                drawRect(color, topLeft = Offset(from * size.width, 0f), size = Size((to - from) * size.width, size.height))
                from = to
            }
            if (active) {
                val r = size.height / 2f
                drawRoundRect(barColor, size = Size(maxOf(level * size.width, size.height), size.height), cornerRadius = CornerRadius(r, r))
                val tick = 3.dp.toPx()
                val x = (peak * size.width).coerceIn(tick, size.width - tick)
                drawRect(palette.ink, topLeft = Offset(x - tick / 2f, 0f), size = Size(tick, size.height))
            }
        }
    }
}

private const val PEAK_HOLD_MS = 1_500L
private const val PEAK_FALL_PER_MS = 0.0002f

/** A comic-style bubble with a tail pointing down at Buddy's head. */
private class BubbleShape(private val corner: Dp, private val tailWidth: Dp, private val tailHeight: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { corner.toPx() }
        val tw = with(density) { tailWidth.toPx() }
        val th = with(density) { tailHeight.toPx() }
        val bodyBottom = size.height - th
        val path = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, bodyBottom, CornerRadius(c, c)))
            // Starts a pixel inside the body so the tail joins it without an anti-aliasing seam.
            moveTo(size.width / 2f - tw / 2f, bodyBottom - 1f)
            lineTo(size.width / 2f + tw / 2f, bodyBottom - 1f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Speech bubble above Buddy. Keeps a two-line minimum height, text centred, so Buddy doesn't bounce when the line changes. */
@Composable
private fun SpeechBubble(text: String) {
    val palette = LocalPalette.current
    val tail = 12.dp
    Box(
        Modifier
            .fillMaxWidth()
            .background(palette.card, BubbleShape(corner = 20.dp, tailWidth = 22.dp, tailHeight = tail))
            .heightIn(min = 76.dp + tail)
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp + tail),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = palette.ink, textAlign = TextAlign.Center)
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
private fun ChallengeSection(challenge: ChallengeState, canStart: Boolean, onStart: () -> Unit) {
    val palette = LocalPalette.current
    val voice = Voices.of(LocalCharacter.current)
    when (challenge) {
        is ChallengeState.Running -> Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = palette.card),
        ) {
            Text(
                stringResource(R.string.challenge_running, challenge.secondsLeft),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            )
        }
        is ChallengeState.Finished -> Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (challenge.won) QB.CalmSoft else QB.TooLoudSoft),
        ) {
            Text(
                stringResource(if (challenge.won) voice.challengeWon else R.string.challenge_lost),
                style = MaterialTheme.typography.titleMedium,
                color = if (challenge.won) QB.CalmText else QB.TooLoudText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            )
        }
        ChallengeState.Idle -> if (canStart) {
            DarkButton(text = stringResource(R.string.challenge_start), onClick = onStart)
        }
    }
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


private fun hasMicPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Tells the user about newly earned rewards and offers to try the most exciting one: a new friend, else the newest. */
@Composable
private fun UnlockDialog(unlocks: List<Reward>, styles: Map<Character, CharacterStyle>, onTry: (Reward) -> Unit, onLater: () -> Unit) {
    val friend = unlocks.firstOrNull { it is CharacterStyle && it.isCharacterDefault } as CharacterStyle?
    val newest = friend ?: unlocks.maxByOrNull { it.unlockAtCalmMinutes } ?: return
    val name = if (friend != null) stringResource(Voices.of(friend.character).name) else stringResource(newest.nameRes)
    val more = unlocks.size - 1
    val (title, body) = when {
        friend != null -> R.string.unlock_friend_title to
            if (more > 0) pluralStringResource(R.plurals.unlock_friend_body_more, more, name, more)
            else stringResource(R.string.unlock_friend_body, name)
        newest is Accessory -> R.string.unlock_accessory_title to
            if (more > 0) pluralStringResource(R.plurals.unlock_accessory_body_more, more, name, more)
            else stringResource(R.string.unlock_accessory_body, name)
        else -> R.string.unlock_title to
            if (more > 0) pluralStringResource(R.plurals.unlock_body_more, more, name, more)
            else stringResource(R.string.unlock_body, name)
    }
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(title), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RewardPreview(newest, styles, Modifier.size(120.dp), animated = true)
                Spacer(Modifier.height(12.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            TextButton(onClick = { onTry(newest) }) {
                Text(stringResource(if (newest is Accessory) R.string.unlock_wear else R.string.unlock_try))
            }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.unlock_later)) } },
    )
}
