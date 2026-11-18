package com.quietbuddy.ui.home

import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow
import com.quietbuddy.core.Thresholds
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.snap
import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quietbuddy.ui.character.rememberAnimationsOff
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietbuddy.core.ListeningState
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.theme.LocalPalette
import com.quietbuddy.ui.theme.QB

// The noise meter bar under the character: level, peak tick and mood zones.
/** One pill with the reading and the band: "34 dB · Nice and calm", or just the paused/sleeping label. */
/**
 * The live noise level: the dB number and band name over a gauge whose track is split into the room's calm, loud
 * and too-loud zones. A bar follows the level and a tick holds the recent peak for a moment before sinking back,
 * so the room can see the noise rise and fall, not just which band it is in.
 */
@Composable
internal fun NoiseMeter(mood: Mood?, db: Int?, thresholds: Thresholds, listening: ListeningState, large: Boolean) {
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
