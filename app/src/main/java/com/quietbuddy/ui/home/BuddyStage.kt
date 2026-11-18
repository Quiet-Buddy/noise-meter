package com.quietbuddy.ui.home

import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.quietbuddy.ui.character.rememberAnimationsOff
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.CharacterView
import com.quietbuddy.ui.theme.LocalPalette
import com.quietbuddy.ui.theme.QB

// The character on Home and the speech bubble above it.
/**
 * Buddy standing on a soft glow in the mood colour. The glow is the loudness meter: it widens and brightens
 * as the room gets louder, so the mood reads from across the room without a ring or a big number.
 */
@Composable
internal fun BuddyStage(mood: Mood?, db: Int?, size: Dp, tapCount: Int, onTap: () -> Unit) {
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
internal fun SpeechBubble(text: String) {
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
