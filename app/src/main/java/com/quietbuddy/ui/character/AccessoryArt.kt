package com.quietbuddy.ui.character

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.AccessorySlot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

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
    /** Centre and radii of a swim ring floating around the body; only characters that can wear one set these. */
    val waistAt: Offset = Offset.Zero,
    val waistRx: Float = 0f,
    val waistRy: Float = 0f,
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
            Accessory.BANDANA -> placed(anchors.collarAt, anchors.collarScale, 0f) { bandana(outline) }
            Accessory.GOGGLES -> goggles(anchors, outline)
            Accessory.CAP -> placed(anchors.hatAt, anchors.hatScale, anchors.hatTilt) { backwardsCap(outline) }
            // Drawn in two halves around the body by the art itself, see drawSwimRing.
            Accessory.SWIM_RING -> Unit
            // Astro's four turn with it when it spins, so RobotArt draws them itself, see drawPropellerCap,
            // drawStarGlasses, drawBowTie and drawPlanetRing.
            Accessory.PROPELLER_CAP, Accessory.STAR_GLASSES, Accessory.BOW_TIE, Accessory.PLANET_RING -> Unit
            // Snowy draws its own scarf and beanie, see SnowmanArt.
            Accessory.SCARF, Accessory.BEANIE -> Unit
            // Mari Domingi draws her own, in her own front/back order, see drawEguzkilore, drawZapia, drawLantern
            // and drawRobin.
            Accessory.EGUZKILORE, Accessory.ZAPIA, Accessory.LANTERN, Accessory.ROBIN -> Unit
        }
    }
}

private val DrawOrder = listOf(
    Accessory.BOOTS, Accessory.BELL_COLLAR, Accessory.TIE, Accessory.BANDANA, Accessory.SUNGLASSES, Accessory.GOGGLES,
    Accessory.BOW, Accessory.FLOWER, Accessory.TOP_HAT, Accessory.CROWN, Accessory.CAP,
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

// The duck's accessories, from the prototype in docs/design/duck-motion.html.
private val CapWhite = Color(0xFFFFFFFF)
private val CapPink = Color(0xFFF28B8B)
private val CapBlue = Color(0xFF7FB8E8)
private val GoggleTeal = Color(0xFF22A6B3)
private val GoggleLens = Color(0xFFBFE8F2)
private val BandanaRed = Color(0xFFE2453C)
private val RingRed = Color(0xFFE8473F)
private val RingWhite = Color(0xFFFFFFFF)

private fun quad(x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float) = Path().apply {
    moveTo(x0, y0)
    quadraticTo(cx, cy, x1, y1)
}

/** A backwards cap: the crown hugs the top of the head and the peak points back over the nape (negative x). */
private fun DrawScope.backwardsCap(outline: Color) {
    val crownShape = Path().apply {
        moveTo(-36f, 17f)
        quadraticTo(-32f, -13f, 0f, -15f)
        quadraticTo(32f, -15f, 36f, 13f)
        quadraticTo(0f, 3f, -36f, 17f)
        close()
    }
    outlined(crownShape, CapWhite, outline, 3.4f)
    drawPath(
        Path().apply {
            moveTo(-28f, 3f); quadraticTo(-22f, -9f, -8f, -12f); lineTo(-4f, 7f); quadraticTo(-18f, 9f, -28f, 11f); close()
        },
        CapPink,
    )
    drawPath(Path().apply { moveTo(8f, -13f); quadraticTo(26f, -9f, 32f, 5f); lineTo(14f, 4f); close() }, CapBlue)
    drawPath(quad(-36f, 17f, 0f, 3f, 36f, 13f), outline, style = stroke(3.4f))
    val peak = Path().apply {
        moveTo(-32f, 15f); quadraticTo(-52f, 17f, -62f, 27f); quadraticTo(-46f, 29f, -28f, 23f); close()
    }
    outlined(peak, CapBlue, outline, 3.2f)
    drawOval(CapPink, Offset(-3.5f, -16.5f), Size(7f, 5f))
    drawOval(outline, Offset(-3.5f, -16.5f), Size(7f, 5f), style = stroke(2f))
    drawPath(quad(-20f, -4f, -14f, -10f, -6f, -11f), Color.White.copy(alpha = 0.8f), style = stroke(2.2f))
}

/**
 * Swim goggles in three-quarter view: a strap back towards the nape, a bridge between the lenses, and the far lens
 * narrower than the near one. The lenses stay mostly clear so the eyes, which carry the moods, still read.
 */
private fun DrawScope.goggles(a: AccessoryAnchors, outline: Color) {
    val near = a.leftEye
    val far = a.rightEye
    val r = a.lensRx
    val rf = r * 0.72f
    val sc = a.hatScale
    fun band(p: Path, w: Float) {
        drawPath(p, outline, style = stroke(w + 4f))
        drawPath(p, GoggleTeal, style = stroke(w))
    }
    band(quad(near.x - 27f * sc, near.y + 3f * sc, near.x - r - 6f, near.y - 2f, near.x - r, near.y), 5f)
    band(Path().apply { moveTo(far.x + rf, far.y); lineTo(far.x + 15f * sc, far.y - sc) }, 5f)
    band(quad(near.x + r, near.y - 1f, (near.x + far.x) / 2f, near.y - 5f, far.x - rf, far.y - 1f), 3.5f)
    for ((c, rx) in listOf(far to rf, near to r)) {
        // the rim is a ring, so the eyes show through the tinted lens
        val rim = Size((rx + 1.5f) * 2f, (r + 1.5f) * 2f)
        val rimTl = Offset(c.x - rx - 1.5f, c.y - r - 1.5f)
        drawOval(GoggleLens.copy(alpha = 0.3f), rimTl, rim)
        drawOval(outline, rimTl, rim, style = stroke(7.5f))
        drawOval(GoggleTeal, rimTl, rim, style = stroke(4f))
        drawPath(
            quad(c.x - rx * 0.45f, c.y + r * 0.1f, c.x - rx * 0.4f, c.y - r * 0.45f, c.x, c.y - r * 0.55f),
            Color.White.copy(alpha = 0.85f),
            style = stroke(2.6f),
        )
    }
}

/**
 * A striped swim ring floating around the body at the waterline, if worn. Arts draw it twice: the back half
 * ([front] false) before the body and the front half after it, so the body sits inside the ring.
 */
fun DrawScope.drawSwimRing(worn: Set<Accessory>, anchors: AccessoryAnchors, outline: Color, front: Boolean) {
    if (Accessory.SWIM_RING !in worn) return
    val c = anchors.waistAt
    val rx = anchors.waistRx
    val ry = anchors.waistRy
    val oval = Rect(c.x - rx, c.y - ry, c.x + rx, c.y + ry)
    // Front half runs through the bottom of the oval (0..180 degrees), back half through the top.
    val half = Path().apply { arcTo(oval, if (front) 0f else 180f, 180f, forceMoveTo = true) }
    drawPath(half, outline, style = stroke(25f))
    // round ends like the outline's, so the two halves join without dark notches at the sides
    drawPath(half, RingWhite, style = stroke(19.5f))
    drawPath(half, RingRed, style = Stroke(width = 19.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 22f))))
    if (front) {
        val shine = Rect(c.x - rx, c.y - ry * 0.55f, c.x + rx, c.y + ry * 0.55f)
        drawPath(
            Path().apply { arcTo(shine, 97f, 39f, forceMoveTo = true) },
            Color.White.copy(alpha = 0.75f),
            style = stroke(3f),
        )
    }
}

/** A red polka-dot bandana knotted at the back of the neck (negative x), its point hanging over the chest. */
private fun DrawScope.bandana(outline: Color) {
    val band = quad(-24f, -4f, 0f, 6f, 24f, -4f)
    drawPath(band, outline, style = stroke(11f))
    drawPath(band, BandanaRed, style = stroke(6.5f))
    val knot = Path().apply {
        moveTo(-26f, -3f); lineTo(-36f, 4f); lineTo(-27f, 7f); close()
        moveTo(-25f, -2f); lineTo(-31f, 11f); lineTo(-22f, 8f); close()
    }
    outlined(knot, BandanaRed, outline, 2.8f)
    val point = Path().apply { moveTo(-4f, 1f); quadraticTo(10f, 5f, 24f, -3f); lineTo(14f, 22f); close() }
    outlined(point, BandanaRed, outline, 3.2f)
    for (dot in listOf(Offset(4f, 6f), Offset(14f, 5f), Offset(12f, 13f), Offset(-10f, 2.5f), Offset(-18f, 0f))) {
        drawCircle(Color.White, 1.7f, dot)
    }
}

// Astro's accessories. Unlike the others they are not placed from anchors: the robot spins on its vertical axis, so
// RobotArt draws them in its own order (the cap and the ring by front/back) and in the prototype's coordinates.
private val PropCap = Color(0xFFFFD43B)
private val PropCapRed = Color(0xFFE8473F)
private val PropCapBlue = Color(0xFF4E8FE0)
private val PropBlade = Color(0xFFE8473F)
private val StarPink = Color(0xFFFF5FA2)
private val StarLens = Color(0xFFFFD6EA)
private val BowTieRed = Color(0xFFE2453C)
private val PlanetRing = Color(0xFFFFB547)
private val PlanetBand = Color(0xFFFF8A3D)

private fun DrawScope.ovalOutlined(cx: Float, cy: Float, rx: Float, ry: Float, fill: Color, outline: Color, w: Float) {
    val tl = Offset(cx - rx, cy - ry)
    val sz = Size(rx * 2f, ry * 2f)
    drawOval(fill, tl, sz)
    drawOval(outline, tl, sz, style = stroke(w))
}

/**
 * A propeller beanie sitting at [at], leaning [angle] degrees, its propeller turned [spin] radians: each blade's
 * width follows the cosine of the angle, so it seems to spin.
 */
fun DrawScope.drawPropellerCap(outline: Color, at: Offset, angle: Float, spin: Float) {
    withTransform({
        translate(at.x, at.y)
        rotate(angle, Offset.Zero)
    }) {
        val dome = Path().apply {
            moveTo(-24f, 8f)
            quadraticTo(-23f, -17f, 0f, -18f)
            quadraticTo(23f, -17f, 24f, 8f)
            quadraticTo(0f, 2f, -24f, 8f)
            close()
        }
        outlined(dome, PropCap, outline, 3.4f)
        drawPath(
            Path().apply {
                moveTo(-8f, -16.5f); quadraticTo(0f, -18f, 8f, -16.5f); lineTo(6f, 4f); quadraticTo(0f, 3.6f, -6f, 4f); close()
            },
            PropCapRed,
        )
        for (side in listOf(-1f, 1f)) {
            drawPath(
                Path().apply {
                    moveTo(side * 22f, -2f); quadraticTo(side * 20f, -12f, side * 12f, -15.6f)
                    lineTo(side * 14f, 5f); quadraticTo(side * 19f, 6f, side * 22f, 6.6f); close()
                },
                PropCapBlue,
            )
        }
        drawPath(Path().apply { moveTo(-24f, 8f); quadraticTo(0f, 2f, 24f, 8f) }, outline, style = stroke(3.4f))
        drawPath(
            Path().apply { moveTo(-16f, -8f); quadraticTo(-11f, -13f, -5f, -14f) },
            Color.White, alpha = 0.8f, style = stroke(2.2f),
        )
        drawPath(Path().apply { moveTo(0f, -18f); lineTo(0f, -25f) }, outline, style = stroke(3f))
        val w = 16f * kotlin.math.abs(kotlin.math.cos(spin))
        ovalOutlined(-w / 2f, -26f, w / 2f, 3.6f, PropBlade, outline, 2.6f)
        ovalOutlined(w / 2f, -26f, w / 2f, 3.6f, PropCapBlue, outline, 2.6f)
        ovalOutlined(0f, -26f, 3f, 3f, PropCap, outline, 2.4f)
    }
}

/** Pink star-shaped glasses over the screen eyes at [eyes]: a bridge, tinted lenses and a glint on each. */
fun DrawScope.drawStarGlasses(outline: Color, eyes: List<Offset>) {
    val bridge = Path().apply { moveTo(92f, 84f); quadraticTo(100f, 80f, 108f, 84f) }
    drawPath(bridge, outline, style = stroke(4.5f))
    drawPath(bridge, StarPink, style = stroke(2.2f))
    for (e in eyes) {
        val star = Path().apply {
            for (i in 0 until 10) {
                val a = -Math.PI.toFloat() / 2f + i * Math.PI.toFloat() / 5f
                val r = if (i % 2 == 1) 8.5f else 16f
                val x = e.x + r * kotlin.math.cos(a)
                val y = e.y + r * kotlin.math.sin(a)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(star, StarLens, alpha = 0.28f)
        drawPath(star, outline, style = stroke(6.5f))
        drawPath(star, StarPink, style = stroke(3.2f))
    }
    drawPath(Path().apply { moveTo(74f, 76f); lineTo(78f, 72f) }, Color.White, alpha = 0.85f, style = stroke(2f))
    drawPath(Path().apply { moveTo(112f, 76f); lineTo(116f, 72f) }, Color.White, alpha = 0.85f, style = stroke(2f))
}

/** A red polka-dot bow tie on the shell under the screen, where a chin would be. */
fun DrawScope.drawBowTie(outline: Color) {
    val x = 100f
    val y = 128f
    for (side in listOf(-1f, 1f)) {
        val wing = Path().apply {
            moveTo(x, y)
            lineTo(x + side * 15f, y - 8f)
            quadraticTo(x + side * 18f, y, x + side * 15f, y + 8f)
            close()
        }
        outlined(wing, BowTieRed, outline, 3f)
    }
    ovalOutlined(x, y, 4.2f, 5f, BowTieRed, outline, 3f)
    for ((dx, dy) in listOf(-10f to -2f, -8f to 4f, 10f to -2f, 8f to 4f)) drawCircle(Color.White, 1.5f, Offset(x + dx, y + dy))
}

/**
 * A planet ring around the orb's lower half, centred at [at], so it never crosses the face. Arts draw it twice:
 * the back half ([front] false) behind the orb and the front half after it.
 */
fun DrawScope.drawPlanetRing(outline: Color, front: Boolean, at: Offset) {
    val rx = 76f
    val ry = 13f
    val oval = Rect(at.x - rx, at.y - ry, at.x + rx, at.y + ry)
    withTransform({ rotate(-8f, at) }) {
        // The front half runs through the bottom of the oval (0..180 degrees), the back half through the top.
        val half = Path().apply { arcTo(oval, if (front) 0f else 180f, 180f, forceMoveTo = true) }
        drawPath(half, outline, style = stroke(13f))
        drawPath(half, PlanetRing, style = stroke(8f))
        drawPath(half, PlanetBand, style = stroke(2.4f))
        if (front) {
            val glint = Path().apply {
                moveTo(at.x - rx * 0.7f, at.y + ry * 0.62f)
                quadraticTo(at.x - rx * 0.45f, at.y + ry * 0.99f, at.x - rx * 0.2f, at.y + ry * 0.96f)
            }
            drawPath(glint, Color.White, alpha = 0.75f, style = stroke(2f))
        }
    }
}

// Mari Domingi's accessories, from docs/design/mari-domingi-motion.html. Like Astro's, they are not placed from
// anchors: her art draws each one in its own front/back order, in the prototype's coordinates (before MariDraw scales
// her into the design space), and drives them with her mood.
private val BractSilver = Color(0xFFF1ECDF)
private val ThistleHeart = Color(0xFFD9A43A)
private val ThistleDot = Color(0xFF9C6B22)
private val ZapiaGround = Color(0xFFF2E3BD)
private val PaisleyInk = Color(0xFF2E2A35)
private val PaisleyGold = Color(0xFFC8902E)
private val PaisleyRust = Color(0xFF9E2B2F)
private val LanternIron = Color(0xFF3A3A44)
private val LanternGlass = Color(0xFFFFF1C4)
private val LanternFlame = Color(0xFFFFB347)
private val LanternGlow = Color(0xFFFFD978)
private val RobinBrown = Color(0xFF8B6B4E)
private val RobinWing = Color(0xFF6E5440)
private val RobinBreast = Color(0xFFE8743B)
private val RobinLegs = Color(0xFF4A3A30)
private val RobinBeak = Color(0xFF3A2E2A)

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

/**
 * The eguzkilore (Carlina acaulis), the silvery thistle nailed to Basque doors: a ring of thin bracts round a golden
 * heart, centred at ([x], [y]). [open] 0 closes the bracts into an upright tuft, 1 opens them in a full ring (more
 * makes them longer); [spin] turns the open ring a little, in radians.
 */
fun DrawScope.drawEguzkilore(outline: Color, x: Float, y: Float, open: Float, spin: Float) {
    val n = 18
    val k = open.coerceIn(0f, 1f)
    val len = 5f + 8f * open
    val bracts = Path()
    for (i in 0 until n) {
        val aOpen = i.toFloat() / n * 2f * PI.toFloat() + spin
        val aShut = -PI.toFloat() / 2f + (i.toFloat() / (n - 1) - 0.5f) * 1.1f
        val a = aShut + (aOpen - aShut) * k
        bracts.moveTo(x, y)
        bracts.lineTo(x + cos(a) * len, y + sin(a) * len * 0.85f)
    }
    drawPath(bracts, outline, style = stroke(4.4f))
    drawPath(bracts, BractSilver, style = stroke(2.4f))
    val hr = 3.6f + 1.6f * k
    ovalOutlined(x, y, hr, hr, ThistleHeart, outline, 2.5f)
    for ((dx, dy) in listOf(-1.4f to -1f, 1.3f to -0.6f, 0f to 1.4f)) drawCircle(ThistleDot, 0.8f, Offset(x + dx * k, y + dy * k))
}

/**
 * The zapia's cream paisley print: a 13-unit tile turned -18 degrees, laid out once over the area the kerchief covers
 * (its ends swing inside their own rotated group, so the print moves with the cloth).
 */
private object PaisleyPrint {
    private const val TILE = 13f
    private val tear = svg("M4 8.5 C1.5 6 3 2.2 6.6 2.8 C9.6 3.4 9.4 7.2 6.6 8.4 C5.4 9 5.6 11 7.6 11.4 C4.8 12.4 3 10.8 4 8.5 Z")
    val ink = Path()
    val gold = Path()
    val rust = Path()

    // In the print's own (rotated) frame the kerchief spans about x -15..73 and y 196..271.
    val area = Rect(-3 * TILE, 14 * TILE, 7 * TILE, 22 * TILE)

    init {
        for (i in -3..6) for (j in 14..21) {
            val o = Offset(i * TILE, j * TILE)
            ink.addPath(tear, o)
            gold.addOval(Rect(Offset(o.x + 6.3f, o.y + 5.4f), 1.2f))
            gold.addOval(Rect(Offset(o.x + 10.6f, o.y + 10.2f), 0.8f))
            rust.addOval(Rect(Offset(o.x + 11f, o.y + 3.2f), 1f))
        }
    }
}

private fun DrawScope.printed(shape: Path, outline: Color, w: Float) {
    clipPath(shape) {
        withTransform({ rotate(-18f, Offset.Zero) }) {
            drawRect(ZapiaGround, PaisleyPrint.area.topLeft, PaisleyPrint.area.size)
            drawPath(PaisleyPrint.ink, PaisleyInk)
            drawPath(PaisleyPrint.gold, PaisleyGold)
            drawPath(PaisleyPrint.rust, PaisleyRust)
        }
    }
    drawPath(shape, outline, style = stroke(w))
}

private val ZapiaTips = listOf(svg("M98 222 L90 236 Q95 237 100 230 Z"), svg("M102 222 L111 235 Q106 237 100 230 Z"))
private val ZapiaBands = listOf(-1f, 1f).map { sd ->
    fun x(v: Float) = 100f + sd * v
    svg(
        "M${x(19f)} 187 C${x(30f)} 186 ${x(36f)} 192 ${x(37f)} 201 C${x(30f)} 210 ${x(14f)} 218 ${x(3f)} 224 " +
            "L${x(1f)} 216 C${x(9f)} 210 ${x(18f)} 203 ${x(21f)} 196 C${x(21f)} 192 ${x(20f)} 189 ${x(19f)} 187 Z",
    ) to svg("M${x(34f)} 197 C${x(28f)} 207 ${x(14f)} 215 ${x(4f)} 220")
}
private val ZapiaKnot = svg("M95 218 Q100 214 105 218 Q107 224 100 227 Q93 224 95 218 Z")

/**
 * The women's zapia, in Mari Domingi's coordinates: a cream paisley kerchief folded into a triangle and laid over the
 * shoulders, its two ends crossing down the front in a V and knotted at the bodice. The little ends below the knot
 * swing by [tailAngle] degrees, so they flutter when she moves.
 */
fun DrawScope.drawZapia(outline: Color, tailAngle: Float) {
    withTransform({ rotate(tailAngle, Offset(100f, 222f)) }) {
        for (tip in ZapiaTips) printed(tip, outline, 2.8f)
    }
    for ((band, border) in ZapiaBands) {
        printed(band, outline, 3.4f)
        drawPath(border, PaisleyRust, alpha = 0.85f, style = stroke(1.8f))
    }
    printed(ZapiaKnot, outline, 3f)
}

private val LanternHook = svg("M0 0 L0 5")
private val LanternCap = svg("M-6 5 L6 5 L8.5 9 L-8.5 9 Z")
private val LanternBar = svg("M0 9 L0 25")
private val LanternFlamePath = svg("M0 13.5 C3 17 2.5 21 0 21 C-2.5 21 -3 17 0 13.5 Z")
private val LanternBase = svg("M-8 25 L8 25 L6.5 28.5 L-6.5 28.5 Z")

/**
 * A small iron lantern hanging from its ring at ([x], [y]) and swinging by [angle] degrees. [glow] 0..1 lights the
 * glass, the flame and the warm halo around it.
 */
fun DrawScope.drawLantern(outline: Color, x: Float, y: Float, glow: Float, angle: Float) {
    val g = max(0f, glow)
    drawCircle(LanternGlow, 10f + 26f * g, Offset(x, y + 16f), alpha = (0.16f * g).coerceIn(0f, 1f))
    drawCircle(LanternGlow, 7f + 8f * g, Offset(x, y + 16f), alpha = (0.3f * g).coerceIn(0f, 1f))
    withTransform({
        translate(x, y)
        rotate(angle, Offset.Zero)
    }) {
        drawPath(LanternHook, LanternIron, style = stroke(2.4f))
        drawCircle(LanternIron, 2.2f, Offset(0f, -1.5f), style = Stroke(1.8f))
        outlined(LanternCap, LanternIron, outline, 2.4f)
        val glassTl = Offset(-7f, 9f)
        val glassSize = Size(14f, 16f)
        drawRoundRect(LanternGlass, glassTl, glassSize, CornerRadius(2f), alpha = (0.45f + 0.55f * g).coerceIn(0f, 1f))
        drawRoundRect(outline, glassTl, glassSize, CornerRadius(2f), style = Stroke(2.6f))
        drawPath(LanternBar, LanternIron, alpha = 0.7f, style = stroke(1.6f))
        drawPath(LanternFlamePath, LanternFlame, alpha = (0.35f + 0.65f * g).coerceIn(0f, 1f))
        outlined(LanternBase, LanternIron, outline, 2.4f)
    }
}

private val RobinTail = svg("M-6 1 L-14 -3 L-13 3 Z")
private val RobinLegsPath = svg("M-1 6 L-2 10 M2.5 6 L2.5 10")
private val RobinWingPath = svg("M-5 -2 Q1 -5 4 0 Q-1 3 -6 2 Z")

/**
 * The txantxangorria (robin) at ([x], [y]), facing right, or left when [dir] is -1. [wing] is the wing's angle in
 * degrees while flying, or null when perched (then its legs show). [sleep] 0..1 tucks its head in and shuts its eye.
 */
fun DrawScope.drawRobin(outline: Color, x: Float, y: Float, dir: Float, wing: Float?, sleep: Float, alpha: Float = 1f) {
    val a = alpha.coerceIn(0f, 1f)
    withTransform({
        translate(x, y)
        scale(1.4f * dir, 1.4f, Offset.Zero)
    }) {
        drawPath(RobinTail, RobinWing, alpha = a)
        drawPath(RobinTail, outline, alpha = a, style = stroke(2.4f))
        if (wing == null) drawPath(RobinLegsPath, RobinLegs, alpha = a, style = stroke(1.8f))
        drawOval(RobinBrown, Offset(-7.5f, -6.5f), Size(15f, 13f), alpha = a)
        drawOval(outline, Offset(-7.5f, -6.5f), Size(15f, 13f), alpha = a, style = Stroke(2.6f))
        drawOval(RobinBreast, Offset(3f - 4.6f, 1.6f - 4.3f), Size(9.2f, 8.6f), alpha = a)
        val hx = 4.6f - 1.8f * sleep
        val hy = -5.6f + 2.4f * sleep
        drawCircle(RobinBrown, 4.5f, Offset(hx, hy), alpha = a)
        drawCircle(outline, 4.5f, Offset(hx, hy), alpha = a, style = Stroke(2.4f))
        drawCircle(RobinBreast, 2.4f, Offset(hx + 2f, hy + 1.1f), alpha = a)
        if (sleep > 0.5f) {
            val shut = Path().apply {
                moveTo(hx + 0.4f, hy - 0.6f)
                quadraticTo(hx + 1.7f, hy + 0.4f, hx + 3f, hy - 0.6f)
            }
            drawPath(shut, outline, alpha = a, style = stroke(1.3f))
        } else {
            drawCircle(outline, 1.15f, Offset(hx + 1.5f, hy - 1f), alpha = a)
            drawCircle(Color.White, 0.4f, Offset(hx + 1.8f, hy - 1.4f), alpha = a)
        }
        val beak = Path().apply {
            moveTo(hx + 4.2f, hy - 0.2f)
            lineTo(hx + 7.4f, hy + 0.6f)
            lineTo(hx + 4.2f, hy + 1.5f)
            close()
        }
        drawPath(beak, RobinBeak, alpha = a)
        withTransform({ rotate(-(wing ?: 0f), Offset(-1f, -1f)) }) {
            drawPath(RobinWingPath, RobinWing, alpha = a)
            drawPath(RobinWingPath, outline, alpha = a, style = stroke(2f))
        }
    }
}
