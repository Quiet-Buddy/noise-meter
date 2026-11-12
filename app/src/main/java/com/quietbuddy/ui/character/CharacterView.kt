package com.quietbuddy.ui.character

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.Mood
import com.quietbuddy.domain.character.Voices

/**
 * Draws the current character in [mood] (null = asleep) with the shared motions; scales to fit and centres.
 * Animated arts also blend between moods and idle (blink, sway, ...) while composed; pass [animated] false for a
 * still resting pose, e.g. small icons in a list. Idle motion is skipped when the system animation scale is 0.
 * The character wears what [LocalWardrobe] says unless [accessories] is given, e.g. to preview one.
 */
@Composable
fun CharacterView(
    mood: Mood?,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    accessories: Set<Accessory>? = null,
) {
    val character = LocalCharacter.current
    val worn = accessories ?: LocalWardrobe.current[character].orEmpty()
    val look = LocalLook
    val art = look.art
    val palette = look.palette
    val motion = art.motion

    // Bounce whenever the mood changes: 1 -> 1.06 -> 1 over ~400 ms.
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(mood) {
        if (!motion.bounceOnMoodChange) return@LaunchedEffect
        bounce.animateTo(1.06f, tween(200))
        bounce.animateTo(1f, tween(200))
    }

    val animationsOff = rememberAnimationsOff()
    val animator = if (animated && art is AnimatedCharacterArt) remember(art) { art.createAnimator(mood) } else null
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(animator, mood, animationsOff) {
        animator?.setMood(mood, animate = !animationsOff)
        frame++
    }
    if (animator != null && !animationsOff) {
        LaunchedEffect(animator) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    // Clamp so a long pause (backgrounded, debugger) does not skip a transition.
                    val dt = if (last == 0L) 0f else minOf(0.05f, (now - last) / 1_000_000_000f)
                    last = now
                    animator.advance(dt)
                    frame++
                }
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "character")
    // Horizontal shake for EARS_COVERED: +/-3 design units, 120 ms period (60 ms each way).
    val shake by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(60, easing = LinearEasing), RepeatMode.Reverse),
        label = "shake",
    )
    // Slow breathing while asleep: scale 1.0 -> 1.02 -> 1.0 over 2 s.
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "breath",
    )

    val dx = if (motion.shakeWhenEarsCovered && mood == Mood.EARS_COVERED) shake else 0f
    val scale = bounce.value * (if (motion.breatheWhenAsleep && mood == null) breath else 1f)
    val description = stringResource(Voices.of(character).description(mood))

    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        // Design space is 200x200; scale uniformly to fit and centre.
        val s = minOf(size.width, size.height) / 200f
        val ox = (size.width - 200f * s) / 2f
        val oy = (size.height - 200f * s) / 2f
        withTransform({
            translate(ox, oy)
            scale(s, s, Offset.Zero)
            translate(dx, 0f)
            scale(scale, scale, Offset(100f, 100f))
        }) {
            if (animator != null) {
                frame // read here so each new frame redraws without recomposing
                with(animator) { drawFrame(palette, worn) }
            } else {
                with(art) { draw(mood, palette, worn) }
            }
        }
    }
}

/** True when the system animator duration scale is 0 (the "remove animations" setting). */
@Composable
private fun rememberAnimationsOff(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterMoodsPreview() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        listOf<Mood?>(Mood.HAPPY, Mood.FINE, Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)
            .forEach { CharacterView(it, Modifier.size(56.dp)) }
    }
}
