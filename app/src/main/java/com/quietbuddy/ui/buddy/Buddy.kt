package com.quietbuddy.ui.buddy

import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.theme.QB

/** mood == null means sleeping (closed eyes as two downward arcs, small "z z" text or paths, no mouth open). */
@Composable
fun Buddy(mood: Mood?, modifier: Modifier = Modifier, bodyColor: Color = QB.BuddyYellow) {
    // Bounce whenever the mood changes: 1 -> 1.06 -> 1 over ~400 ms.
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(mood) {
        bounce.animateTo(1.06f, tween(200))
        bounce.animateTo(1f, tween(200))
    }

    val transition = rememberInfiniteTransition(label = "buddy")
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

    val dx = if (mood == Mood.EARS_COVERED) shake else 0f
    val scale = bounce.value * (if (mood == null) breath else 1f)
    val description = when (mood) {
        null -> stringResource(R.string.buddy_asleep)
        Mood.HAPPY -> stringResource(R.string.buddy_happy)
        Mood.FINE -> stringResource(R.string.buddy_fine)
        Mood.WORRIED -> stringResource(R.string.buddy_worried)
        Mood.SAD -> stringResource(R.string.buddy_sad)
        Mood.EARS_COVERED -> stringResource(R.string.buddy_ears)
    }

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
            drawBuddy(mood, bodyColor)
        }
    }
}

private val Round = StrokeCap.Round

private fun path(block: Path.() -> Unit): Path = Path().apply(block)

private fun DrawScope.ink(p: Path, width: Float = 4f) =
    drawPath(p, QB.Outline, style = Stroke(width = width, cap = Round, join = StrokeJoin.Round))

private fun DrawScope.fillPath(p: Path, color: Color) = drawPath(p, color)

private fun DrawScope.outlinedOval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
    val tl = Offset(cx - rx, cy - ry)
    val sz = Size(rx * 2f, ry * 2f)
    drawOval(color, topLeft = tl, size = sz)
    drawOval(QB.Outline, topLeft = tl, size = sz, style = Stroke(width = 4f))
}

private fun DrawScope.eye(cx: Float, cy: Float, hx: Float, hy: Float) {
    drawCircle(QB.Outline, radius = 9f, center = Offset(cx, cy))
    drawCircle(Color.White, radius = 3f, center = Offset(hx, hy))
}

private fun DrawScope.drawBuddy(mood: Mood?, body: Color) {
    // Hair tufts
    ink(path {
        moveTo(78f, 30f)
        cubicTo(72f, 14f, 80f, 8f, 92f, 18f)
    })
    ink(path {
        moveTo(100f, 26f)
        cubicTo(100f, 10f, 108f, 6f, 116f, 14f)
    })

    // Body
    outlinedOval(100f, 108f, 78f, 80f, body)

    when (mood) {
        null -> {
            // Closed eyes: two downward arcs
            ink(path {
                moveTo(58f, 100f)
                quadraticTo(70f, 112f, 82f, 100f)
            }, 5f)
            ink(path {
                moveTo(118f, 100f)
                quadraticTo(130f, 112f, 142f, 100f)
            }, 5f)
            // Small closed mouth
            ink(path {
                moveTo(92f, 136f)
                quadraticTo(100f, 140f, 108f, 136f)
            }, 4f)
            // "z z" as paths
            ink(path {
                moveTo(146f, 40f); lineTo(162f, 40f); lineTo(146f, 56f); lineTo(162f, 56f)
            }, 3.5f)
            ink(path {
                moveTo(170f, 20f); lineTo(180f, 20f); lineTo(170f, 30f); lineTo(180f, 30f)
            }, 3f)
        }

        Mood.HAPPY -> {
            drawOval(QB.Cheek, topLeft = Offset(39f, 116f), size = Size(26f, 16f), alpha = 0.8f)
            drawOval(QB.Cheek, topLeft = Offset(135f, 116f), size = Size(26f, 16f), alpha = 0.8f)
            ink(path {
                moveTo(58f, 96f)
                quadraticTo(70f, 82f, 82f, 96f)
            }, 5f)
            ink(path {
                moveTo(118f, 96f)
                quadraticTo(130f, 82f, 142f, 96f)
            }, 5f)
            fillPath(path {
                moveTo(68f, 126f)
                quadraticTo(100f, 160f, 132f, 126f)
                close()
            }, QB.Mouth)
            ink(path {
                moveTo(68f, 126f)
                quadraticTo(100f, 160f, 132f, 126f)
                close()
            })
            fillPath(path {
                moveTo(84f, 140f)
                quadraticTo(100f, 152f, 116f, 140f)
                quadraticTo(100f, 158f, 84f, 140f)
                close()
            }, QB.Tongue)
        }

        Mood.FINE -> {
            drawOval(QB.Cheek, topLeft = Offset(39f, 116f), size = Size(26f, 16f), alpha = 0.6f)
            drawOval(QB.Cheek, topLeft = Offset(135f, 116f), size = Size(26f, 16f), alpha = 0.6f)
            eye(72f, 96f, 75f, 92f)
            eye(128f, 96f, 131f, 92f)
            ink(path {
                moveTo(78f, 134f)
                quadraticTo(100f, 150f, 122f, 134f)
            }, 5f)
        }

        Mood.WORRIED -> {
            ink(path { moveTo(56f, 70f); lineTo(84f, 78f) }, 5f)
            ink(path { moveTo(144f, 70f); lineTo(116f, 78f) }, 5f)
            eye(72f, 100f, 75f, 96f)
            eye(128f, 100f, 131f, 96f)
            ink(path {
                moveTo(76f, 140f)
                quadraticTo(88f, 128f, 100f, 140f)
                quadraticTo(112f, 152f, 124f, 140f)
            }, 5f)
            fillPath(path {
                moveTo(160f, 70f)
                quadraticTo(166f, 84f, 160f, 92f)
                quadraticTo(154f, 84f, 160f, 70f)
                close()
            }, QB.Tear)
            ink(path {
                moveTo(160f, 70f)
                quadraticTo(166f, 84f, 160f, 92f)
                quadraticTo(154f, 84f, 160f, 70f)
                close()
            }, 3f)
        }

        Mood.SAD -> {
            ink(path { moveTo(58f, 76f); lineTo(84f, 68f) }, 5f)
            ink(path { moveTo(142f, 76f); lineTo(116f, 68f) }, 5f)
            eye(72f, 100f, 75f, 96f)
            eye(128f, 100f, 131f, 96f)
            ink(path {
                moveTo(74f, 148f)
                quadraticTo(100f, 126f, 126f, 148f)
            }, 5f)
            val tear = path {
                moveTo(140f, 112f)
                quadraticTo(148f, 128f, 140f, 136f)
                quadraticTo(132f, 128f, 140f, 112f)
                close()
            }
            fillPath(tear, QB.Tear)
            ink(tear, 3f)
        }

        Mood.EARS_COVERED -> {
            ink(path { moveTo(58f, 84f); lineTo(82f, 94f) }, 5f)
            ink(path { moveTo(142f, 84f); lineTo(118f, 94f) }, 5f)
            ink(path { moveTo(66f, 100f); lineTo(80f, 106f) }, 5f)
            ink(path { moveTo(134f, 100f); lineTo(120f, 106f) }, 5f)
            ink(path {
                moveTo(74f, 140f)
                quadraticTo(84f, 128f, 94f, 140f)
                quadraticTo(104f, 152f, 114f, 140f)
                quadraticTo(122f, 130f, 128f, 140f)
            }, 5f)
            // Hands over ears
            outlinedOval(34f, 112f, 22f, 28f, body)
            ink(path {
                moveTo(22f, 92f); lineTo(22f, 80f)
                moveTo(34f, 88f); lineTo(34f, 76f)
                moveTo(46f, 92f); lineTo(46f, 82f)
            }, 4f)
            outlinedOval(166f, 112f, 22f, 28f, body)
            ink(path {
                moveTo(154f, 92f); lineTo(154f, 82f)
                moveTo(166f, 88f); lineTo(166f, 76f)
                moveTo(178f, 92f); lineTo(178f, 80f)
            }, 4f)
            // Sound lines
            drawPath(
                path {
                    moveTo(6f, 60f); lineTo(14f, 56f)
                    moveTo(6f, 76f); lineTo(14f, 76f)
                    moveTo(6f, 92f); lineTo(14f, 96f)
                    moveTo(194f, 60f); lineTo(186f, 56f)
                    moveTo(194f, 76f); lineTo(186f, 76f)
                    moveTo(194f, 92f); lineTo(186f, 96f)
                },
                QB.TooLoud,
                style = Stroke(width = 4f, cap = Round),
            )
            val tear = path {
                moveTo(150f, 54f)
                quadraticTo(154f, 66f, 150f, 72f)
                quadraticTo(146f, 66f, 150f, 54f)
                close()
            }
            fillPath(tear, QB.Tear)
            ink(tear, 3f)
        }
    }

    // Feet
    outlinedOval(74f, 186f, 18f, 8f, body)
    outlinedOval(126f, 186f, 18f, 8f, body)
}

@Preview(showBackground = true)
@Composable
private fun BuddyMoodsPreview() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        listOf<Mood?>(Mood.HAPPY, Mood.FINE, Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)
            .forEach { Buddy(it, Modifier.size(56.dp)) }
    }
}

/** Short, translated name of a mood (e.g. for accessibility descriptions). */
@androidx.annotation.StringRes
fun moodLabelRes(mood: Mood): Int = when (mood) {
    Mood.HAPPY -> R.string.svc_mood_happy
    Mood.FINE -> R.string.svc_mood_fine
    Mood.WORRIED -> R.string.svc_mood_worried
    Mood.SAD -> R.string.svc_mood_sad
    Mood.EARS_COVERED -> R.string.svc_mood_ears
}
