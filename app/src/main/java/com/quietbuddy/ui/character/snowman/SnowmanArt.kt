package com.quietbuddy.ui.character.snowman

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.Character
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.CharacterArt
import com.quietbuddy.ui.character.CharacterView
import com.quietbuddy.ui.character.LocalCharacter
import com.quietbuddy.ui.character.Motion
import com.quietbuddy.ui.theme.Palette

/**
 * Snowy the snowman: two stacked snowballs with coal eyes, a carrot nose and stick arms. Static; the mood shows in
 * the face alone, and the shared bounce, shake and breathing of [CharacterView] do the moving. Snowy draws its own
 * scarf and beanie, so the shared accessory art skips them.
 */
object SnowmanArt : CharacterArt {
    override val motion = Motion()

    override fun DrawScope.draw(mood: Mood?, palette: Palette, accessories: Set<Accessory>) =
        drawSnowman(mood, palette, accessories)
}

private val Carrot = Color(0xFFFF8A2B)
private val Twig = Color(0xFF7A5230)
private val ScarfRed = Color(0xFFE5484D)
private val ScarfDark = Color(0xFFB92E37)
private val BeanieBlue = Color(0xFF4A7FD1)
private val BeanieCuff = Color(0xFF35609F)
private val Pom = Color(0xFFFFFFFF)

private const val Line = 4f

private val HeadAt = Offset(100f, 66f)
private const val HeadR = 34f
private val BodyAt = Offset(100f, 142f)
private const val BodyR = 46f

private fun DrawScope.drawSnowman(mood: Mood?, palette: Palette, accessories: Set<Accessory>) {
    val outline = palette.outline
    val armsUp = mood == Mood.EARS_COVERED
    if (!armsUp) arms(raised = false)
    ball(BodyAt, BodyR, palette)
    ball(HeadAt, HeadR, palette)
    if (armsUp) arms(raised = true)
    for (y in listOf(128f, 146f, 164f)) drawCircle(outline, 4f, Offset(100f, y))
    if (Accessory.SCARF in accessories) scarf(outline)
    face(mood, palette)
    if (Accessory.BEANIE in accessories) beanie(outline)
}

private fun DrawScope.ball(center: Offset, radius: Float, palette: Palette) {
    drawCircle(palette.body, radius, center)
    drawCircle(palette.outline, radius, center, style = Stroke(Line))
}

private fun DrawScope.stroke(path: Path, color: Color, width: Float = Line) =
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))

private fun DrawScope.arms(raised: Boolean) {
    for (side in listOf(-1f, 1f)) {
        fun x(dx: Float) = 100f + side * dx
        val path = Path().apply {
            if (raised) {
                // Up the side of the head, the "hand" tucked over the ear.
                moveTo(x(52f), 124f)
                quadraticTo(x(74f), 96f, x(60f), 70f)
            } else {
                moveTo(x(50f), 126f)
                lineTo(x(88f), 104f)
            }
        }
        stroke(path, Twig, 5f)
        val twig = Path().apply {
            if (raised) {
                moveTo(x(67f), 92f)
                lineTo(x(80f), 88f)
            } else {
                moveTo(x(72f), 113f)
                lineTo(x(80f), 124f)
            }
        }
        stroke(twig, Twig, 4f)
    }
}

private fun DrawScope.face(mood: Mood?, palette: Palette) {
    val outline = palette.outline
    // Eyes.
    for (side in listOf(-1f, 1f)) {
        val x = 100f + side * 12f
        when (mood) {
            null -> {
                val lid = Path().apply { moveTo(x - 5f, 59f); quadraticTo(x, 66f, x + 5f, 59f) }
                stroke(lid, outline, 3f)
            }
            Mood.EARS_COVERED -> {
                val squeeze = Path().apply {
                    moveTo(x - side * 6f, 55f); lineTo(x + side * 5f, 60f); lineTo(x - side * 6f, 65f)
                }
                stroke(squeeze, outline, 3f)
            }
            else -> drawCircle(outline, 4f, Offset(x, 60f))
        }
    }
    // Brows.
    if (mood == Mood.WORRIED) {
        for (side in listOf(-1f, 1f)) {
            val brow = Path().apply { moveTo(100f + side * 21f, 52f); lineTo(100f + side * 7f, 47f) }
            stroke(brow, outline, 3f)
        }
    }
    // Cheeks.
    if (mood == null || mood == Mood.HAPPY || mood == Mood.FINE) {
        for (side in listOf(-1f, 1f)) {
            drawCircle(palette.cheek.copy(alpha = 0.75f), 6f, Offset(100f + side * 26f, 76f))
        }
    }
    // Carrot nose.
    val nose = Path().apply {
        moveTo(95f, 68f); lineTo(117f, 73f); lineTo(95f, 78f); close()
    }
    drawPath(nose, Carrot)
    drawPath(nose, outline, style = Stroke(2.5f, join = StrokeJoin.Round))
    // Mouth.
    when (mood) {
        Mood.HAPPY -> {
            val mouth = Path().apply { moveTo(84f, 83f); quadraticTo(100f, 103f, 116f, 83f); close() }
            drawPath(mouth, palette.mouth)
            drawPath(mouth, outline, style = Stroke(3f, join = StrokeJoin.Round))
        }
        Mood.FINE -> stroke(Path().apply { moveTo(90f, 85f); quadraticTo(100f, 93f, 110f, 85f) }, outline, 3f)
        Mood.WORRIED -> stroke(Path().apply { moveTo(91f, 88f); lineTo(109f, 88f) }, outline, 3f)
        Mood.SAD -> {
            stroke(Path().apply { moveTo(90f, 91f); quadraticTo(100f, 82f, 110f, 91f) }, outline, 3f)
            val tear = Path().apply {
                moveTo(81f, 66f); quadraticTo(75f, 76f, 81f, 79f); quadraticTo(87f, 76f, 81f, 66f)
            }
            drawPath(tear, palette.tear)
        }
        Mood.EARS_COVERED -> {
            drawOval(palette.mouth, Offset(94f, 82f), Size(12f, 14f))
            drawOval(outline, Offset(94f, 82f), Size(12f, 14f), style = Stroke(3f))
        }
        null -> stroke(Path().apply { moveTo(94f, 86f); quadraticTo(100f, 90f, 106f, 86f) }, outline, 3f)
    }
}

private fun DrawScope.scarf(outline: Color) {
    val tail = Path().apply {
        moveTo(108f, 98f); lineTo(130f, 104f); lineTo(126f, 134f); lineTo(110f, 130f); close()
    }
    drawPath(tail, ScarfDark)
    drawPath(tail, outline, style = Stroke(3f, join = StrokeJoin.Round))
    val band = Path().apply {
        moveTo(66f, 92f); quadraticTo(100f, 108f, 134f, 92f)
        lineTo(133f, 105f); quadraticTo(100f, 121f, 67f, 105f); close()
    }
    drawPath(band, ScarfRed)
    drawPath(band, outline, style = Stroke(3f, join = StrokeJoin.Round))
}

private fun DrawScope.beanie(outline: Color) {
    val dome = Path().apply {
        moveTo(68f, 40f); cubicTo(66f, 6f, 134f, 6f, 132f, 40f); close()
    }
    drawPath(dome, BeanieBlue)
    drawPath(dome, outline, style = Stroke(3.5f, join = StrokeJoin.Round))
    drawCircle(Pom, 8f, Offset(100f, 13f))
    drawCircle(outline, 8f, Offset(100f, 13f), style = Stroke(3f))
    val cuff = Path().apply {
        moveTo(65f, 34f); lineTo(135f, 34f); lineTo(135f, 45f); lineTo(65f, 45f); close()
    }
    drawPath(cuff, BeanieCuff)
    drawPath(cuff, outline, style = Stroke(3.5f, join = StrokeJoin.Round))
}

@Preview(showBackground = true)
@Composable
private fun SnowmanMoodsPreview() {
    CompositionLocalProvider(LocalCharacter provides Character.SNOWMAN) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            listOf<Mood?>(Mood.HAPPY, Mood.FINE, Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)
                .forEach { CharacterView(it, Modifier.size(56.dp), animated = false) }
        }
    }
}
