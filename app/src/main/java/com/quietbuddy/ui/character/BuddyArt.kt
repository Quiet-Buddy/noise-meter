package com.quietbuddy.ui.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.theme.Palette
import com.quietbuddy.ui.theme.QB

/** Buddy: a yellow blob with hair tufts, hands for covering its ears, and a "z z" when asleep. */
object BuddyArt : CharacterArt {
    override val motion = Motion()

    override fun DrawScope.draw(mood: Mood?, palette: Palette, accessories: Set<Accessory>) {
        drawBuddy(mood, palette)
        drawAccessories(accessories, BuddyAnchors, palette.outline)
    }
}

/** Buddy holds still apart from the shared motions, so one set of anchors fits every mood. */
private val BuddyAnchors = AccessoryAnchors(
    hatAt = Offset(102f, 33f), hatScale = 0.9f, hatTilt = 6f,
    bowAt = Offset(140f, 42f), bowScale = 0.9f, bowTilt = 24f,
    flowerAt = Offset(62f, 42f), flowerScale = 0.8f, flowerTilt = -20f,
    collarAt = Offset(100f, 158f), collarScale = 0.9f,
    leftEye = Offset(72f, 97f), rightEye = Offset(128f, 97f), lensRx = 20f, lensRy = 15f,
    neckAt = Offset(100f, 158f), neckScale = 0.85f,
    leftFoot = Offset(74f, 186f), rightFoot = Offset(126f, 186f), footRx = 18f, footRy = 8f,
)

private val Round = StrokeCap.Round

private fun path(block: Path.() -> Unit): Path = Path().apply(block)

private fun DrawScope.ink(pal: Palette, p: Path, width: Float = 4f) =
    drawPath(p, pal.outline, style = Stroke(width = width, cap = Round, join = StrokeJoin.Round))

private fun DrawScope.fillPath(p: Path, color: Color) = drawPath(p, color)

private fun DrawScope.outlinedOval(pal: Palette, cx: Float, cy: Float, rx: Float, ry: Float, color: Color) {
    val tl = Offset(cx - rx, cy - ry)
    val sz = Size(rx * 2f, ry * 2f)
    drawOval(color, topLeft = tl, size = sz)
    drawOval(pal.outline, topLeft = tl, size = sz, style = Stroke(width = 4f))
}

private fun DrawScope.eye(pal: Palette, cx: Float, cy: Float, hx: Float, hy: Float) {
    drawCircle(pal.outline, radius = 9f, center = Offset(cx, cy))
    drawCircle(Color.White, radius = 3f, center = Offset(hx, hy))
}

private fun DrawScope.drawBuddy(mood: Mood?, p: Palette) {
    // Hair tufts
    ink(p, path {
        moveTo(78f, 30f)
        cubicTo(72f, 14f, 80f, 8f, 92f, 18f)
    })
    ink(p, path {
        moveTo(100f, 26f)
        cubicTo(100f, 10f, 108f, 6f, 116f, 14f)
    })

    // Body
    outlinedOval(p, 100f, 108f, 78f, 80f, p.body)

    when (mood) {
        null -> {
            // Closed eyes: two downward arcs
            ink(p, path {
                moveTo(58f, 100f)
                quadraticTo(70f, 112f, 82f, 100f)
            }, 5f)
            ink(p, path {
                moveTo(118f, 100f)
                quadraticTo(130f, 112f, 142f, 100f)
            }, 5f)
            // Small closed mouth
            ink(p, path {
                moveTo(92f, 136f)
                quadraticTo(100f, 140f, 108f, 136f)
            }, 4f)
            // "z z" as paths
            ink(p, path {
                moveTo(146f, 40f); lineTo(162f, 40f); lineTo(146f, 56f); lineTo(162f, 56f)
            }, 3.5f)
            ink(p, path {
                moveTo(170f, 20f); lineTo(180f, 20f); lineTo(170f, 30f); lineTo(180f, 30f)
            }, 3f)
        }

        Mood.HAPPY -> {
            drawOval(p.cheek, topLeft = Offset(39f, 116f), size = Size(26f, 16f), alpha = 0.8f)
            drawOval(p.cheek, topLeft = Offset(135f, 116f), size = Size(26f, 16f), alpha = 0.8f)
            ink(p, path {
                moveTo(58f, 96f)
                quadraticTo(70f, 82f, 82f, 96f)
            }, 5f)
            ink(p, path {
                moveTo(118f, 96f)
                quadraticTo(130f, 82f, 142f, 96f)
            }, 5f)
            fillPath(path {
                moveTo(68f, 126f)
                quadraticTo(100f, 160f, 132f, 126f)
                close()
            }, p.mouth)
            ink(p, path {
                moveTo(68f, 126f)
                quadraticTo(100f, 160f, 132f, 126f)
                close()
            })
            fillPath(path {
                moveTo(84f, 140f)
                quadraticTo(100f, 152f, 116f, 140f)
                quadraticTo(100f, 158f, 84f, 140f)
                close()
            }, p.tongue)
        }

        Mood.FINE -> {
            drawOval(p.cheek, topLeft = Offset(39f, 116f), size = Size(26f, 16f), alpha = 0.6f)
            drawOval(p.cheek, topLeft = Offset(135f, 116f), size = Size(26f, 16f), alpha = 0.6f)
            eye(p, 72f, 96f, 75f, 92f)
            eye(p, 128f, 96f, 131f, 92f)
            ink(p, path {
                moveTo(78f, 134f)
                quadraticTo(100f, 150f, 122f, 134f)
            }, 5f)
        }

        Mood.WORRIED -> {
            ink(p, path { moveTo(56f, 70f); lineTo(84f, 78f) }, 5f)
            ink(p, path { moveTo(144f, 70f); lineTo(116f, 78f) }, 5f)
            eye(p, 72f, 100f, 75f, 96f)
            eye(p, 128f, 100f, 131f, 96f)
            ink(p, path {
                moveTo(76f, 140f)
                quadraticTo(88f, 128f, 100f, 140f)
                quadraticTo(112f, 152f, 124f, 140f)
            }, 5f)
            fillPath(path {
                moveTo(160f, 70f)
                quadraticTo(166f, 84f, 160f, 92f)
                quadraticTo(154f, 84f, 160f, 70f)
                close()
            }, p.tear)
            ink(p, path {
                moveTo(160f, 70f)
                quadraticTo(166f, 84f, 160f, 92f)
                quadraticTo(154f, 84f, 160f, 70f)
                close()
            }, 3f)
        }

        Mood.SAD -> {
            ink(p, path { moveTo(58f, 76f); lineTo(84f, 68f) }, 5f)
            ink(p, path { moveTo(142f, 76f); lineTo(116f, 68f) }, 5f)
            eye(p, 72f, 100f, 75f, 96f)
            eye(p, 128f, 100f, 131f, 96f)
            ink(p, path {
                moveTo(74f, 148f)
                quadraticTo(100f, 126f, 126f, 148f)
            }, 5f)
            val tear = path {
                moveTo(140f, 112f)
                quadraticTo(148f, 128f, 140f, 136f)
                quadraticTo(132f, 128f, 140f, 112f)
                close()
            }
            fillPath(tear, p.tear)
            ink(p, tear, 3f)
        }

        Mood.EARS_COVERED -> {
            ink(p, path { moveTo(58f, 84f); lineTo(82f, 94f) }, 5f)
            ink(p, path { moveTo(142f, 84f); lineTo(118f, 94f) }, 5f)
            ink(p, path { moveTo(66f, 100f); lineTo(80f, 106f) }, 5f)
            ink(p, path { moveTo(134f, 100f); lineTo(120f, 106f) }, 5f)
            ink(p, path {
                moveTo(74f, 140f)
                quadraticTo(84f, 128f, 94f, 140f)
                quadraticTo(104f, 152f, 114f, 140f)
                quadraticTo(122f, 130f, 128f, 140f)
            }, 5f)
            // Hands over ears
            outlinedOval(p, 34f, 112f, 22f, 28f, p.body)
            ink(p, path {
                moveTo(22f, 92f); lineTo(22f, 80f)
                moveTo(34f, 88f); lineTo(34f, 76f)
                moveTo(46f, 92f); lineTo(46f, 82f)
            }, 4f)
            outlinedOval(p, 166f, 112f, 22f, 28f, p.body)
            ink(p, path {
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
            fillPath(tear, p.tear)
            ink(p, tear, 3f)
        }
    }

    // Feet
    outlinedOval(p, 74f, 186f, 18f, 8f, p.body)
    outlinedOval(p, 126f, 186f, 18f, 8f, p.body)
}
