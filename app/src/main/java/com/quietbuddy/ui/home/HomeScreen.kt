package com.quietbuddy.ui.home

import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import android.Manifest
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.quietbuddy.ui.character.nameRes
import com.quietbuddy.core.CharacterStyle
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import com.quietbuddy.ui.theme.LocalPalette
import com.quietbuddy.ui.theme.QB
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen() {
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
    val listening = state.listening == ListeningState.LISTENING

    // On tablets the device usually sits on a shelf and is read from across the room, so Buddy and the
    // number get much bigger there. The cards stay at their normal size.
    val large = isLargeScreen()
    val canStartChallenge = listening && (state.mood == Mood.SAD || state.mood == Mood.EARS_COVERED)

    val header: @Composable () -> Unit = {
        Header(
            roomName = state.roomName,
            listening = listening,
            onToggle = { if (listening) viewModel.pause() else wake() },
        )
    }
    // Everything below the live reading: speech, wake prompt, band bar, stats and challenge.
    val details: @Composable () -> Unit = {
        SpeechBubble(text = stringResource(state.speechLine))
        Spacer(Modifier.height(12.dp))

        if (!listening) {
            NotListeningMessage(state.listening)
            Spacer(Modifier.height(12.dp))
            DarkButton(text = stringResource(R.string.home_wake_buddy), onClick = ::wake)
            Spacer(Modifier.height(16.dp))
        }

        BandBar(mood = state.mood)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = stringResource(R.string.home_calm_time_today),
                value = formatMinutes(state.calmMinutes),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.home_loudest_moment),
                value = state.loudestDb?.let { "${it.roundToInt()} dB" } ?: "–",
                sub = state.loudestAtMs?.let { formatTime(it) },
                modifier = Modifier.weight(1f),
            )
        }
        if (state.challenge != ChallengeState.Idle || canStartChallenge) {
            Spacer(Modifier.height(12.dp))
        }
        ChallengeSection(
            challenge = state.challenge,
            canStart = canStartChallenge,
            onStart = viewModel::startChallenge,
        )
    }

    UnlockDialog(
        unlocks = state.pendingUnlocks,
        onTry = { viewModel.dismissUnlocks(it) },
        onLater = { viewModel.dismissUnlocks(null) },
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(background).statusBarsPadding()) {
        if (isTwoPane(maxWidth, maxHeight)) {
            // Landscape: Buddy takes the full height on the left, readings and cards sit on the right.
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
                    val ringSize = minOf(maxWidth, maxHeight).coerceAtMost(if (large) 560.dp else 360.dp)
                    BuddyRing(mood = state.mood, db = state.db, size = ringSize)
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
                        Spacer(Modifier.height(if (large) 24.dp else 8.dp))
                        // Number and pill side by side to save height on phones in landscape.
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = spacedBy(16.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DbReadout(db = state.db, large = large)
                            BandPill(mood = state.mood, listening = state.listening)
                        }
                        Spacer(Modifier.height(if (large) 24.dp else 12.dp))
                        details()
                    }
                }
            }
        } else {
            // Portrait: one column. The ring takes the height left after the rest of the content, so the
            // listening screen fits without scrolling on typical phones; taller states (challenge, wake button)
            // scroll. On tablets the column is capped in width and centred.
            val contentWidth = minOf(maxWidth, SingleColumnMaxWidth)
            val reserved = if (large) 480.dp else 430.dp
            val ringSize = minOf(maxHeight - reserved, contentWidth - 48.dp)
                .coerceIn(160.dp, if (large) 520.dp else 300.dp)
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = SingleColumnMaxWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                header()
                Spacer(Modifier.height(8.dp))
                BuddyRing(mood = state.mood, db = state.db, size = ringSize)
                Spacer(Modifier.height(4.dp))
                DbReadout(db = state.db, large = large)
                Spacer(Modifier.height(4.dp))
                BandPill(mood = state.mood, listening = state.listening)
                Spacer(Modifier.height(12.dp))
                details()
            }
        }
    }
}

@Composable
private fun Header(roomName: String, listening: Boolean, onToggle: () -> Unit) {
    val palette = LocalPalette.current
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..16 -> stringResource(R.string.greeting_afternoon)
        else -> stringResource(R.string.greeting_evening)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft)
            Text(roomName, style = MaterialTheme.typography.headlineSmall)
        }
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
private fun BuddyRing(mood: Mood?, db: Int?, size: Dp) {
    val palette = LocalPalette.current
    val target = if (mood == null || db == null) 0f else ((db - 30f) / 70f).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, tween(600), label = "ringProgress")
    val arcColor by animateColorAsState(mood?.let { QB.bandColor(it) } ?: palette.line, tween(600), label = "ringColor")

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = (size * 0.053f).coerceAtLeast(10.dp).toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
            drawArc(
                color = palette.line,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            if (progress > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        CharacterView(mood = mood, modifier = Modifier.size(size * 2 / 3))
    }
}

@Composable
private fun DbReadout(db: Int?, large: Boolean = false) {
    val number = MaterialTheme.typography.displayLarge
    Row(verticalAlignment = Alignment.Bottom) {
        Text(db?.toString() ?: "–", style = if (large) number.copy(fontSize = 96.sp) else number)
        Spacer(Modifier.width(6.dp))
        Text(
            "dB",
            style = MaterialTheme.typography.titleMedium.let { if (large) it.copy(fontSize = 24.sp) else it },
            modifier = Modifier.padding(bottom = if (large) 22.dp else 12.dp),
        )
    }
}

@Composable
private fun BandPill(mood: Mood?, listening: ListeningState) {
    val palette = LocalPalette.current
    val label = when (mood) {
        Mood.HAPPY, Mood.FINE -> stringResource(R.string.band_nice_calm)
        Mood.WORRIED -> stringResource(R.string.band_getting_loud)
        Mood.SAD -> stringResource(R.string.band_too_loud)
        Mood.EARS_COVERED -> stringResource(R.string.band_way_too_loud)
        null -> stringResource(if (listening == ListeningState.PAUSED) R.string.band_paused else R.string.band_sleeping)
    }
    val soft = mood?.let { QB.bandSoft(it) } ?: palette.accentSoft
    val text = mood?.let { QB.bandText(it) } ?: palette.inkSoft
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(soft)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = text)
    }
}

@Composable
private fun SpeechBubble(text: String) {
    val palette = LocalPalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.card),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(20.dp),
        )
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
private fun BandBar(mood: Mood?) {
    val palette = LocalPalette.current
    val active = when (mood) {
        null -> -1
        Mood.HAPPY, Mood.FINE -> 0
        Mood.WORRIED -> 1
        Mood.SAD, Mood.EARS_COVERED -> 2
    }
    val segments = listOf(
        Triple(stringResource(R.string.legend_calm), QB.Calm, 0),
        Triple(stringResource(R.string.legend_loud), QB.Loud, 1),
        Triple(stringResource(R.string.legend_too_loud), QB.TooLoud, 2),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = spacedBy(6.dp)) {
        segments.forEach { (_, color, index) ->
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (index == active) color else palette.line),
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = spacedBy(6.dp)) {
        segments.forEach { (label, _, _) ->
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    val palette = LocalPalette.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.card),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = palette.ink))
            if (sub != null) {
                Text(sub, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
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
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "$h h $m min" else "$m min"
}

private fun formatTime(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

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

/** Tells the user about newly earned styles and offers to switch to the newest one. */
@Composable
private fun UnlockDialog(unlocks: List<CharacterStyle>, onTry: (CharacterStyle) -> Unit, onLater: () -> Unit) {
    val newest = unlocks.maxByOrNull { it.unlockAtCalmMinutes } ?: return
    val name = stringResource(newest.nameRes)
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.unlock_title), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                CompositionLocalProvider(LocalCharacter provides newest.character, LocalStyle provides newest) {
                    CharacterView(mood = Mood.HAPPY, modifier = Modifier.size(120.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (unlocks.size > 1) pluralStringResource(R.plurals.unlock_body_more, unlocks.size - 1, name, unlocks.size - 1)
                    else stringResource(R.string.unlock_body, name),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onTry(newest) }) { Text(stringResource(R.string.unlock_try)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.unlock_later)) } },
    )
}
