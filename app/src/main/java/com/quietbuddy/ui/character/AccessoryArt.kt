package com.quietbuddy.ui.character

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.AccessorySlot

// Accessory colours are the same in every style; only the outline follows the palette.
private val HatFelt = Color(0xFF3A3346)
private val HatBand = Color(0xFFE5484D)
private val Gold = Color(0xFFFFC93C)
private val GemRed = Color(0xFFE5484D)
private val GemBlue = Color(0xFF4C9BF3)
private val BowPink = Color(0xFFFF6FA3)
private val Lens = Color(0xFF262433)
private val TieBlue = Color(0xFF4C7BF3)
private val TieStripe = Color(0xFFA9C2FF)
private val BootRed = Color(0xFFE5484D)
private val BootCuff = Color(0xFFFFE1E2)
private val CollarRed = Color(0xFFE5484D)
private val PetalPink = Color(0xFFFFB3D1)
private val LeafGreen = Color(0xFF6CC17A)

/**
 * Where a character wears its accessories, in its 200x200 design space. Each art supplies these, per pose if
 * needed, and calls [drawAccessories] at the right moments in its own draw order.
 */
class AccessoryAnchors(
    /** Centre of a hat brim, sitting on top of the head. */
    val hatAt: Offset,
    val hatScale: Float,
    val hatTilt: Float,
    /** Centre of a hair bow's knot. */
    val bowAt: Offset,
    val bowScale: Float,
    val bowTilt: Float,
    /** Centre of a flower tucked by one ear. */
    val flowerAt: Offset,
    val flowerScale: Float,
    val flowerTilt: Float,
    /** Centre of a collar's band, under the chin. */
    val collarAt: Offset,
    val collarScale: Float,
    /** Eye centres and the half-size of a sunglasses lens. */
    val leftEye: Offset,
    val rightEye: Offset,
    val lensRx: Float,
    val lensRy: Float,
    /** Centre of a tie's knot. */
    val neckAt: Offset,
    val neckScale: Float,
    /** Foot centres and half-sizes, for boots. */
    val leftFoot: Offset,
    val rightFoot: Offset,
    val footRx: Float,
    val footRy: Float,
)

private fun stroke(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.outlined(p: Path, fill: Color, outline: Color, w: Float = 4f) {
    drawPath(p, fill)
    drawPath(p, outline, style = stroke(w))
}

private inline fun DrawScope.placed(at: Offset, scale: Float, tilt: Float, block: DrawScope.() -> Unit) =
    withTransform({
        translate(at.x, at.y)
        rotate(tilt, Offset.Zero)
        scale(scale, scale, Offset.Zero)
    }) { block() }

/** Draws the worn accessories whose slot is in [slots], in a fixed back-to-front order. */
fun DrawScope.drawAccessories(
    worn: Set<Accessory>,
    anchors: AccessoryAnchors,
    outline: Color,
    slots: Set<AccessorySlot> = AccessorySlot.entries.toSet(),
) {
    if (worn.isEmpty()) return
    for (accessory in DrawOrder) {
        if (accessory !in worn || accessory.slot !in slots) continue
        when (accessory) {
            Accessory.BOOTS -> boots(anchors, outline)
            Accessory.BELL_COLLAR -> placed(anchors.collarAt, anchors.collarScale, 0f) { bellCollar(outline) }
            Accessory.FLOWER -> placed(anchors.flowerAt, anchors.flowerScale, anchors.flowerTilt) { flower(outline) }
            Accessory.TIE -> placed(anchors.neckAt, anchors.neckScale, 0f) { tie(outline) }
            Accessory.SUNGLASSES -> sunglasses(anchors, outline)
            Accessory.BOW -> placed(anchors.bowAt, anchors.bowScale, anchors.bowTilt) { bow(outline) }
            Accessory.TOP_HAT -> placed(anchors.hatAt, anchors.hatScale, anchors.hatTilt) { topHat(outline) }
            Accessory.CROWN -> placed(anchors.hatAt, anchors.hatScale, anchors.hatTilt) { crown(outline) }
        }
    }
}

private val DrawOrder = listOf(
    Accessory.BOOTS, Accessory.BELL_COLLAR, Accessory.TIE, Accessory.SUNGLASSES, Accessory.BOW, Accessory.FLOWER, Accessory.TOP_HAT, Accessory.CROWN,
)

// Each shape below is drawn around its anchor at (0, 0); brims and bases sit on y = 0 and rise into negative y.

private fun DrawScope.topHat(outline: Color) {
    val crown = Path().apply {
        moveTo(-21f, -2f)
        lineTo(-23f, -40f)
        quadraticTo(0f, -44f, 23f, -40f)
        lineTo(21f, -2f)
        close()
    }
    outlined(crown, HatFelt, outline)
    val band = Path().apply {
        moveTo(-21.5f, -12f)
        lineTo(21.5f, -12f)
        lineTo(21f, -3f)
        lineTo(-21f, -3f)
        close()
    }
    drawPath(band, HatBand)
    drawPath(band, outline, style = stroke(3f))
    drawRoundRect(HatFelt, Offset(-34f, -5f), Size(68f, 10f), CornerRadius(5f))
    drawRoundRect(outline, Offset(-34f, -5f), Size(68f, 10f), CornerRadius(5f), style = stroke(4f))
    drawLine(Color.White.copy(alpha = 0.35f), Offset(-15f, -36f), Offset(-15f, -18f), 3f, StrokeCap.Round)
}

private fun DrawScope.crown(outline: Color) {
    val body = Path().apply {
        moveTo(-26f, 2f)
        lineTo(-29f, -28f)
        lineTo(-14f, -14f)
        lineTo(0f, -34f)
        lineTo(14f, -14f)
        lineTo(29f, -28f)
        lineTo(26f, 2f)
        close()
    }
    outlined(body, Gold, outline)
    for (tip in listOf(Offset(-29f, -30f), Offset(0f, -37f), Offset(29f, -30f))) {
        drawCircle(Gold, 4.5f, tip)
        drawCircle(outline, 4.5f, tip, style = stroke(3f))
    }
    drawCircle(GemRed, 5f, Offset(0f, -9f))
    drawCircle(outline, 5f, Offset(0f, -9f), style = stroke(2.5f))
    for (x in listOf(-15f, 15f)) {
        drawCircle(GemBlue, 3.5f, Offset(x, -6f))
        drawCircle(outline, 3.5f, Offset(x, -6f), style = stroke(2.5f))
    }
}

private fun DrawScope.bow(outline: Color) {
    for (side in listOf(-1f, 1f)) {
        val loop = Path().apply {
            moveTo(0f, 0f)
            quadraticTo(side * 10f, -20f, side * 24f, -13f)
            quadraticTo(side * 30f, 0f, side * 24f, 13f)
            quadraticTo(side * 10f, 20f, 0f, 0f)
            close()
        }
        outlined(loop, BowPink, outline, 3.5f)
        drawPath(
            Path().apply { moveTo(side * 7f, -3f); quadraticTo(side * 14f, -6f, side * 18f, -5f) },
            outline,
            style = stroke(2.5f),
        )
    }
    drawCircle(BowPink, 6.5f, Offset.Zero)
    drawCircle(outline, 6.5f, Offset.Zero, style = stroke(3.5f))
}

private fun DrawScope.bellCollar(outline: Color) {
    // A band that smiles across the neck; the outline is a wider stroke underneath the coloured one.
    val band = Path().apply {
        moveTo(-28f, -2f)
        quadraticTo(0f, 10f, 28f, -2f)
    }
    drawPath(band, outline, style = stroke(12f))
    drawPath(band, CollarRed, style = stroke(6f))
    drawLine(Color.White.copy(alpha = 0.4f), Offset(-18f, 0f), Offset(-8f, 2.5f), 2f, StrokeCap.Round)
    // Bell hangs from the middle of the band.
    val bell = Offset(0f, 13f)
    drawCircle(Gold, 7.5f, bell)
    drawCircle(outline, 7.5f, bell, style = stroke(3f))
    drawLine(outline, Offset(-5f, 14.5f), Offset(5f, 14.5f), 2.5f, StrokeCap.Round)
    drawCircle(outline, 1.6f, Offset(0f, 17.5f))
    drawCircle(Color.White.copy(alpha = 0.8f), 1.8f, Offset(-2.6f, 10f))
}

private fun DrawScope.flower(outline: Color) {
    // One leaf peeking out behind the petals.
    val leaf = Path().apply {
        moveTo(0f, 0f)
        quadraticTo(-4f, -20f, -22f, -22f)
        quadraticTo(-24f, -4f, 0f, 0f)
        close()
    }
    outlined(leaf, LeafGreen, outline, 3f)
    for (i in 0 until 5) {
        withTransform({ rotate(i * 72f - 18f, Offset.Zero) }) {
            val petal = Path().apply {
                moveTo(0f, 0f)
                quadraticTo(-11f, -9f, -7f, -17f)
                quadraticTo(0f, -22f, 7f, -17f)
                quadraticTo(11f, -9f, 0f, 0f)
                close()
            }
            outlined(petal, PetalPink, outline, 3f)
        }
    }
    drawCircle(Gold, 6f, Offset.Zero)
    drawCircle(outline, 6f, Offset.Zero, style = stroke(3f))
    drawCircle(Color.White.copy(alpha = 0.7f), 1.5f, Offset(-2f, -2f))
}

private fun DrawScope.sunglasses(a: AccessoryAnchors, outline: Color) {
    val l = a.leftEye
    val r = a.rightEye
    // Arms back towards the sides of the head, then the bridge, then the lenses on top.
    drawLine(outline, Offset(l.x - a.lensRx, l.y - a.lensRy * 0.4f), Offset(l.x - a.lensRx - 10f, l.y - a.lensRy * 0.6f), 4f, StrokeCap.Round)
    drawLine(outline, Offset(r.x + a.lensRx, r.y - a.lensRy * 0.4f), Offset(r.x + a.lensRx + 10f, r.y - a.lensRy * 0.6f), 4f, StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(l.x + a.lensRx - 2f, l.y - a.lensRy * 0.35f)
            quadraticTo((l.x + r.x) / 2f, l.y - a.lensRy * 0.9f, r.x - a.lensRx + 2f, r.y - a.lensRy * 0.35f)
        },
        outline,
        style = stroke(4f),
    )
    for (c in listOf(l, r)) {
        val tl = Offset(c.x - a.lensRx, c.y - a.lensRy)
        val size = Size(a.lensRx * 2f, a.lensRy * 2f)
        val corner = CornerRadius(a.lensRy * 0.8f)
        drawRoundRect(Lens, tl, size, corner)
        drawRoundRect(outline, tl, size, corner, style = stroke(4f))
        drawLine(
            Color.White.copy(alpha = 0.7f),
            Offset(c.x - a.lensRx * 0.55f, c.y + a.lensRy * 0.1f),
            Offset(c.x - a.lensRx * 0.1f, c.y - a.lensRy * 0.5f),
            3f,
            StrokeCap.Round,
        )
    }
}

private fun DrawScope.tie(outline: Color) {
    val blade = Path().apply {
        moveTo(-5f, 4f)
        lineTo(5f, 4f)
        lineTo(10f, 26f)
        lineTo(0f, 34f)
        lineTo(-10f, 26f)
        close()
    }
    outlined(blade, TieBlue, outline, 3.5f)
    drawLine(TieStripe, Offset(-5f, 13f), Offset(5f, 9f), 3f, StrokeCap.Round)
    drawLine(TieStripe, Offset(-7f, 22f), Offset(7f, 17f), 3f, StrokeCap.Round)
    val knot = Path().apply {
        moveTo(-8f, -6f)
        lineTo(8f, -6f)
        lineTo(5f, 5f)
        lineTo(-5f, 5f)
        close()
    }
    outlined(knot, TieBlue, outline, 3.5f)
}

private fun DrawScope.boots(a: AccessoryAnchors, outline: Color) {
    for (foot in listOf(a.leftFoot, a.rightFoot)) {
        val shaftW = a.footRx * 1.2f
        val shaftTop = foot.y - a.footRy * 2.4f
        val shaftTl = Offset(foot.x - shaftW / 2f, shaftTop)
        val shaftSize = Size(shaftW, foot.y - shaftTop)
        drawRoundRect(BootRed, shaftTl, shaftSize, CornerRadius(4f))
        drawRoundRect(outline, shaftTl, shaftSize, CornerRadius(4f), style = stroke(4f))
        val cuffTl = Offset(foot.x - shaftW / 2f - 2f, shaftTop - 2f)
        val cuffSize = Size(shaftW + 4f, 7f)
        drawRoundRect(BootCuff, cuffTl, cuffSize, CornerRadius(3.5f))
        drawRoundRect(outline, cuffTl, cuffSize, CornerRadius(3.5f), style = stroke(3f))
        val soleTl = Offset(foot.x - a.footRx - 2f, foot.y - a.footRy - 1f)
        val soleSize = Size((a.footRx + 2f) * 2f, (a.footRy + 1f) * 2f)
        drawOval(BootRed, soleTl, soleSize)
        drawOval(outline, soleTl, soleSize, style = stroke(4f))
        drawLine(Color.White.copy(alpha = 0.5f), Offset(foot.x - a.footRx * 0.5f, foot.y - a.footRy * 0.3f), Offset(foot.x - a.footRx * 0.1f, foot.y - a.footRy * 0.5f), 3f, StrokeCap.Round)
    }
}
