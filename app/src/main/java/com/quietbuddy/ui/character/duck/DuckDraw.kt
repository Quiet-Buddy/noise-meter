package com.quietbuddy.ui.character.duck

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import com.quietbuddy.ui.character.AccessoryAnchors
import com.quietbuddy.ui.character.drawAccessories
import com.quietbuddy.ui.character.drawSwimRing
import com.quietbuddy.ui.theme.Palette
import com.quietbuddy.ui.theme.QB
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// Duck-only colours that never change with the style; the rest (outline, body, bill, ...) come from the palette.
private val Eye = Color(0xFF2B1E10)
private val EyeLow = Color(0xFF6B4A2C)
private val Blush = Color(0xFFF07A8C)
private val Nostril = Color(0xFF9A4A0A)
private val Spark = Color(0xFFFFE36B)
private val Bubble = Color(0xFFDFF1FF)
private val Water = Color(0xFF5E9BD1)
private val SpeculumLight = Color(0xFF9DB8F5)

private fun clamp01(x: Float) = x.coerceIn(0f, 1f)
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun smooth(a: Float, b: Float, x: Float): Float {
    val t = clamp01((x - a) / (b - a))
    return t * t * (3f - 2f * t)
}

private fun rad(deg: Float) = deg * PI.toFloat() / 180f

private fun rot(x: Float, y: Float, deg: Float, cx: Float, cy: Float): Offset {
    val r = rad(deg)
    val c = cos(r)
    val s = sin(r)
    return Offset(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c)
}

/**
 * Builds a Path from a small SVG subset (absolute M, L, Q, C, Z and relative q, t) so static shapes stay as in the
 * prototype.
 */
private fun svgPath(d: String): Path {
    val path = Path()
    val t = d.trim().split(Regex("\\s+"))
    var i = 0
    fun n() = t[i++].toFloat()
    var x = 0f
    var y = 0f
    // The last quadratic control point, which `t` reflects.
    var qx = 0f
    var qy = 0f
    while (i < t.size) {
        var quad = false
        when (t[i++]) {
            "M" -> { x = n(); y = n(); path.moveTo(x, y) }
            "L" -> { x = n(); y = n(); path.lineTo(x, y) }
            "Q" -> {
                qx = n(); qy = n(); x = n(); y = n()
                path.quadraticTo(qx, qy, x, y)
                quad = true
            }
            "C" -> {
                val x1 = n(); val y1 = n(); val x2 = n(); val y2 = n()
                x = n(); y = n()
                path.cubicTo(x1, y1, x2, y2, x, y)
            }
            "q" -> {
                qx = x + n(); qy = y + n()
                val ex = x + n(); val ey = y + n()
                path.quadraticTo(qx, qy, ex, ey)
                x = ex; y = ey
                quad = true
            }
            "t" -> {
                qx = 2f * x - qx; qy = 2f * y - qy
                val ex = x + n(); val ey = y + n()
                path.quadraticTo(qx, qy, ex, ey)
                x = ex; y = ey
                quad = true
            }
            "Z" -> path.close()
        }
        if (!quad) { qx = x; qy = y }
    }
    return path
}

private fun strokeOf(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.fill(p: Path, color: Color, alpha: Float = 1f) = drawPath(p, color, alpha = clamp01(alpha))

private fun DrawScope.ink(p: Path, color: Color, w: Float, alpha: Float = 1f) =
    drawPath(p, color, alpha = clamp01(alpha), style = strokeOf(w))

private fun DrawScope.outlined(pal: Palette, p: Path, color: Color, w: Float, alpha: Float = 1f) {
    drawPath(p, color, alpha = clamp01(alpha))
    drawPath(p, pal.outline, alpha = clamp01(alpha), style = strokeOf(w))
}

private fun DrawScope.oval(
    cx: Float, cy: Float, rx: Float, ry: Float, color: Color, alpha: Float = 1f,
    outline: Color? = null, w: Float = 4f,
) {
    val tl = Offset(cx - max(0f, rx), cy - max(0f, ry))
    val sz = Size(max(0f, rx) * 2f, max(0f, ry) * 2f)
    drawOval(color, tl, sz, alpha = clamp01(alpha))
    if (outline != null) drawOval(outline, tl, sz, alpha = clamp01(alpha), style = Stroke(width = w))
}

/** An ellipse rotated by [deg] about ([px], [py]). */
private fun DrawScope.tiltedOval(
    cx: Float, cy: Float, rx: Float, ry: Float, color: Color, alpha: Float, deg: Float, px: Float, py: Float,
) = withTransform({ rotate(deg, Offset(px, py)) }) { oval(cx, cy, rx, ry, color, alpha) }

/** `M p0 Q p1 p2` from six numbers. */
private fun q1(p: List<Float>) = Path().apply {
    moveTo(p[0], p[1])
    quadraticTo(p[2], p[3], p[4], p[5])
}

/** A closed shape through the first point, then (control, point) pairs. */
private fun qPath(pts: List<Offset>) = Path().apply {
    moveTo(pts[0].x, pts[0].y)
    for (k in 1 until pts.size step 2) quadraticTo(pts[k].x, pts[k].y, pts[k + 1].x, pts[k + 1].y)
    close()
}

private fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)

private fun sparklePath(x: Float, y: Float, s: Float) = Path().apply {
    val k = s * 0.18f
    moveTo(x, y - s)
    quadraticTo(x + k, y - k, x + s, y)
    quadraticTo(x + k, y + k, x, y + s)
    quadraticTo(x - k, y + k, x - s, y)
    quadraticTo(x - k, y - k, x, y - s)
    close()
}

private fun DrawScope.sparkle(pal: Palette, x: Float, y: Float, s: Float, a: Float) =
    outlined(pal, sparklePath(x, y, s), Spark, 2f, a)

private fun DrawScope.drop(pal: Palette, x: Float, y: Float, h: Float, a: Float, w: Float = 2.2f) {
    val p = Path().apply {
        moveTo(x, y)
        quadraticTo(x + h * 0.35f, y + h * 0.7f, x, y + h)
        quadraticTo(x - h * 0.35f, y + h * 0.7f, x, y)
        close()
    }
    outlined(pal, p, pal.tear, w, a)
}

/** A webbed foot under the water; [flip] mirrors it and [angle] kicks it about its ankle. */
private fun DrawScope.foot(pal: Palette, cx: Float, cy: Float, flip: Boolean, angle: Float) {
    val s = if (flip) -1f else 1f
    val p = Path().apply {
        moveTo(cx, cy - 6f)
        quadraticTo(cx - 4f * s, cy + 2f, cx - 16f * s, cy + 6f)
        quadraticTo(cx - 12f * s, cy + 10f, cx - 8f * s, cy + 8f)
        quadraticTo(cx - 6f * s, cy + 12f, cx - s, cy + 9f)
        quadraticTo(cx + 3f * s, cy + 12f, cx + 5f * s, cy + 7f)
        quadraticTo(cx + 4f * s, cy, cx, cy - 6f)
        close()
    }
    withTransform({ rotate(angle, Offset(cx, cy - 6f)) }) { outlined(pal, p, pal.duck.feet, 2.6f, 0.9f) }
}

private val BodyShade = svgPath("M 40 162 Q 100 184 162 160 Q 100 172 40 162 Z")
private val Mottle = svgPath(
    "M 60 140 L 64 144 L 68 140 M 76 148 L 80 152 L 84 148 M 96 154 L 100 158 L 104 154 M 66 160 L 70 164 L 74 160 " +
        "M 86 164 L 90 168 L 94 164 M 112 162 L 116 166 L 120 162 M 126 150 L 130 154 L 134 150 " +
        "M 50 128 L 53 131 L 56 128 M 138 160 L 141 163 L 144 160",
)
private val Crown = svgPath("M 80 44 Q 92 26 114 26 Q 138 27 148 46 Q 118 34 80 44 Z")
private val EyeStripe = svgPath("M 75 60 Q 84 58 89 58 M 107 60 Q 111 66 113 74")
private val BillSaddle = svgPath("M 128 76 Q 146 73 160 77 Q 146 84 130 81 Z")
private val Scallops = svgPath("M 140 136 q 4 4 8 0 M 150 146 q 4 4 8 0 M 138 150 q 4 4 8 0 M 150 160 q 4 3 8 -1")
private val PoofBody = svgPath("M 52 116 L 46 106 M 66 120 L 63 109 M 40 112 L 32 104")
private val PoofHead = svgPath("M 92 32 L 86 22 M 108 26 L 107 15 M 124 27 L 128 16")
private val Nape = svgPath("M 78 72 Q 70 72 67 77 Q 72 79 76 80 Q 71 84 71 89 Q 77 87 80 84 Z")
private val CheekShade = svgPath("M 75.5 70 Q 78 97 106 101.5 Q 86 94 81 70 Z")
private val BlushStrokes = svgPath("M 86 78.5 L 84 82 M 90.5 78.5 L 88.5 82 M 95 78.5 L 93 82")
private val Well = svgPath("M 89 70 Q 98 79 107 70 Q 98 74 89 70 Z")
private val Speculum = svgPath("M 92 143.5 Q 76 138.4 58 139 Q 54 142 52 145 Q 60 149 63 149 Q 78 149.6 92 143.5 Z")
private val SpeculumGlint = svgPath("M 66 140 Q 60 140 57 141.5 Q 60 143 66 142.4 Z")
private val WingBelly = svgPath("M 124 132 Q 106 116 80 124 Q 96 136 122 140 Z")
private val WingFeathers = svgPath("M 82 126 Q 88 132 94 130 Q 100 136 106 134 Q 112 140 120 138")
private val WingLines = svgPath("M 98 139 Q 80 138 58 139 M 100 148 Q 82 150 63 149 M 102 156 Q 86 160 70 160")
private val WingCoverLines = svgPath("M 70 100 Q 66 70 64 44 M 80 96 Q 80 60 72 34")
private val ZzzBig = svgPath("M 158 22 L 172 22 L 158 36 L 172 36")
private val ZzzSmall = svgPath("M 178 2 L 187 2 L 178 11 L 187 11")
private val WaterLine = svgPath("M 26 174 Q 60 179 94 176 M 106 176 Q 142 179 176 173")
private val Glints = svgPath("M 36 183 L 44 183 M 160 182 L 165 182 M 52 189 L 56 189")

/**
 * Draws Pip the duck at 200x200 design-space scale, in the prototype's coordinates. The prototype's viewBox adds a
 * margin around that square; like the kitten, the duck is drawn full size and the few things that reach past the
 * edge (quack waves, shock lines) overflow, since the canvas doesn't clip.
 *
 * The pat's squash and hearts are drawn outside the art (BuddyTapEffects), so only the giggle, tail wag and quack
 * waves are here.
 */
fun DrawScope.drawDuck(pose: DuckPose, fx: DuckFx, pal: Palette, accessories: Set<Accessory> = emptySet()) {
    val p = pose.giggled(fx.giggle)
    val rig = Rig(p, fx)
    val t = fx.time
    val idle = fx.idle
    val tau = PI.toFloat() * 2f

    val floatY = if (idle) p.float * sin(t * tau * p.floatHz) else 0f
    val rock = if (idle) p.sway * sin(t * tau * p.swayHz) else 0f
    // Whole-body motion: tremble and breathing are weighted by the pose, so they ease in and out with it.
    val dx = if (idle) p.shake * 2.5f * sin(t * tau / 0.12f) else 0f
    val breath = if (idle) 1f + 0.02f * p.breathe * (0.5f - 0.5f * cos(t * PI.toFloat())) else 1f

    drawWater(p, fx, pal)
    withTransform({
        translate(dx, floatY)
        rotate(rock, Offset(100f, 174f))
        scale(breath, breath, Offset(100f, 174f))
    }) {
        val anchors = anchors(p, rig)
        drawSwimRing(accessories, anchors, pal.outline, front = false)
        drawFarWing(p, pal, rig)
        drawBody(p, fx, pal)
        drawHead(p, pal, rig)
        drawAccessories(accessories, anchors, pal.outline, NeckSlots)
        drawFace(p, fx, pal, rig)
        drawAccessories(accessories, anchors, pal.outline, HeadSlots)
        drawNearWing(p, fx, pal, rig)
        drawSwimRing(accessories, anchors, pal.outline, front = true)
        drawExtras(p, fx, pal, rig)
    }
    // The waterline sits in front of her bottom so she floats, with a few glints.
    ink(WaterLine, Water, 3f, 0.85f)
    ink(Glints, Color.White, 2.2f, 0.9f)
}

private val NeckSlots = setOf(AccessorySlot.NECK)
private val HeadSlots = setOf(AccessorySlot.HEAD, AccessorySlot.FACE)

/** Where the head sits this frame: its centre, tilt and the map from head units to design space. */
private class Rig(p: DuckPose, fx: DuckFx) {
    val nod = if (fx.idle) p.nod * sin(fx.time * PI.toFloat() * 2f * p.nodHz) else 0f
    val cx = DuckShapes.HEAD_CX + p.headDx
    val cy = DuckShapes.HEAD_CY + p.headDy + nod * 0.8f
    val tilt = p.tilt + fx.twitch

    /** A point drawn in head units, as it ends up in design space. */
    fun head(x: Float, y: Float): Offset {
        val r = rot((x - 112f) * DuckShapes.HEAD_SCALE, (y - 64f) * DuckShapes.HEAD_SCALE, tilt, 0f, 0f)
        return Offset(cx + r.x, cy + r.y)
    }
}

private inline fun DrawScope.inHead(rig: Rig, block: DrawScope.() -> Unit) = withTransform({
    translate(rig.cx, rig.cy)
    rotate(rig.tilt, Offset.Zero)
    scale(DuckShapes.HEAD_SCALE, DuckShapes.HEAD_SCALE, Offset.Zero)
    translate(-112f, -64f)
}) { block() }

/** Stroke width that looks like [w] inside the scaled head. */
private const val HEAD_STROKE = 1f / DuckShapes.HEAD_SCALE

private val NeckStart = Offset(88f, 118f)
private val NeckEnd = Offset(144f, 115f)
private val NeckMid = Offset(118f, 109f)

/**
 * Where the duck wears things for this frame. Head anchors ride the head's transform; the neck ones sit on the
 * neck join along the body's top edge.
 */
private fun anchors(p: DuckPose, rig: Rig): AccessoryAnchors {
    val k = DuckShapes.HEAD_SCALE
    val ring = Offset(
        (NeckStart.x + 2f * NeckMid.x + NeckEnd.x) / 4f,
        (NeckStart.y + 2f * NeckMid.y + NeckEnd.y) / 4f,
    )
    val near = DuckShapes.Eyes[0]
    val far = DuckShapes.Eyes[1]
    return AccessoryAnchors(
        hatAt = rig.head(112f, 27f), hatScale = k, hatTilt = rig.tilt,
        bowAt = rig.head(84f, 36f), bowScale = 0.6f, bowTilt = rig.tilt - 30f,
        flowerAt = rig.head(136f, 32f), flowerScale = 0.6f, flowerTilt = rig.tilt + 30f,
        collarAt = ring, collarScale = 1f,
        leftEye = rig.head(near.cx, near.cy), rightEye = rig.head(far.cx, far.cy),
        lensRx = near.rx * p.eyeScale * k, lensRy = near.ry * p.eyeScale * k,
        neckAt = ring, neckScale = 1f,
        leftFoot = Offset(70f, 190f), rightFoot = Offset(132f, 192f), footRx = 14f, footRy = 8f,
        waistAt = Offset(99f, 166f), waistRx = 70f, waistRy = 14f,
    )
}

/** Ripples spreading out and webbed feet paddling below the surface. */
private fun DrawScope.drawWater(p: DuckPose, fx: DuckFx, pal: Palette) {
    for (ph0 in listOf(0f, 0.5f)) {
        val ph = if (fx.idle) (fx.time * 0.3f + ph0) % 1f else 0.3f + ph0
        val rx = 76f + 22f * ph
        val ry = 6f + 3.5f * ph
        drawOval(
            Water, Offset(100f - rx, 176f - ry), Size(rx * 2f, ry * 2f),
            alpha = clamp01((1f - ph) * 0.7f), style = Stroke(width = 2.5f),
        )
    }
    val kick = if (fx.idle) 16f * sin(fx.time * PI.toFloat() * 2f * p.paddle) * clamp01(p.paddle) else 0f
    foot(pal, 76f, 186f, false, kick)
    foot(pal, 126f, 188f, true, -kick)
    oval(100f, 184f, 76f, 12f, Water, 0.16f)
}

/** Tucked behind the body, rising behind the head when it gets too loud. */
private fun DrawScope.drawFarWing(p: DuckPose, pal: Palette, rig: Rig) {
    val cover = smooth(0f, 1f, p.cover)
    if (cover <= 0.02f) return
    val rest = DuckShapes.FarRest
    val up = DuckShapes.FarCover
    val pts = List(rest.size / 2) {
        val target = if (it == DuckShapes.FAR_COVER_BODY_POINT) Offset(up[0], up[1]) else rig.head(up[it * 2], up[it * 2 + 1])
        Offset(lerp(rest[it * 2], target.x, cover), lerp(rest[it * 2 + 1], target.y, cover))
    }
    outlined(pal, qPath(pts), pal.duck.wing, 3.5f)
}

/** The body with its pointed rubber-duck tail and, when the style has one, a tail in its own colour. */
private fun DrawScope.drawBody(p: DuckPose, fx: DuckFx, pal: Palette) {
    val d = pal.duck
    val idle = fx.idle
    val wagHz = p.tailHz + fx.patWag * 3f
    val wag = if (idle) (p.tailWag + fx.patWag * 6f) * sin(fx.time * PI.toFloat() * 2f * wagHz) else 0f
    val td = p.tailDroop + wag
    val bodyD = Path().apply {
        moveTo(84f, 118f)
        quadraticTo(64f, 124f, 48f, 116f)
        quadraticTo(34f, 108f + td * 0.5f, 24f - td * 0.25f, 96f + td)
        quadraticTo(22f, 118f + td * 0.5f, 32f, 140f)
        cubicTo(36f, 166f, 56f, 174f, 76f, 174f)
        lineTo(126f, 174f)
        cubicTo(168f, 174f, 178f, 134f, 148f, 116f)
        quadraticTo(118f, 108f, 84f, 118f)
        close()
    }
    outlined(pal, bodyD, pal.body, 3.5f)
    fill(BodyShade, d.shade, 0.45f)
    if (d.tail != null) {
        // The tail region in its own colour; the body outline is drawn again on top below.
        val tail = Path().apply {
            moveTo(52f, 117f)
            quadraticTo(34f, 108f + td * 0.5f, 24f - td * 0.25f, 96f + td)
            quadraticTo(22f, 118f + td * 0.5f, 32f, 140f)
            quadraticTo(38f, 126f, 52f, 117f)
            close()
        }
        fill(tail, d.tail)
    }
    tiltedOval(70f, 128f, 18f, 4.5f, d.belly, 0.6f, -12f, 70f, 128f)
    oval(142f, 148f, 17f, 13f, d.belly, 0.8f)
    if (d.mottle != null) ink(Mottle, d.mottle, 2f, 0.8f)
    val tailLines = Path().apply {
        moveTo(46f, 122f)
        quadraticTo(36f, 112f + td * 0.6f, 30f - td * 0.15f, 102f + td * 0.9f)
        moveTo(40f, 132f)
        quadraticTo(32f, 122f + td * 0.4f, 28f - td * 0.1f, 114f + td * 0.6f)
    }
    ink(tailLines, d.tailLine ?: d.shade, 2.2f)
    ink(Scallops, d.shade, 2f, 0.8f)
    if (d.tail != null) ink(bodyD, pal.outline, 3.5f)
    oval(118f, 122f, 30f, 6f, d.shade, 0.35f)
    if (p.poof > 0.02f) ink(PoofBody, pal.outline, 3f, p.poof)
}

private fun headColour(pal: Palette) = pal.duck.head ?: pal.body

/** Nape feathers, the round head and the short neck that joins it to the body. */
private fun DrawScope.drawHead(p: DuckPose, pal: Palette, rig: Rig) {
    val head = headColour(pal)
    inHead(rig) {
        outlined(pal, Nape, head, 3f * HEAD_STROKE)
        oval(112f, 64f, 38f, 38f, head, outline = pal.outline, w = 3.5f * HEAD_STROKE)
    }
    val nb = NeckStart
    val nf = NeckEnd
    fun onHead(angle: Float, r: Float) = rig.head(112f + r * cos(angle), 64f + r * sin(angle))
    val hb = onHead(2.09f, 38f)
    val hf = onHead(1.05f, 38f)
    val hbi = onHead(2.09f, 26f)
    val hfi = onHead(1.05f, 26f)
    if (hb.y < nb.y - 4f) {
        val cb = Offset((nb.x + hb.x) / 2f - 2f, (nb.y + hb.y) / 2f)
        val cf = Offset((nf.x + hf.x) / 2f + 2f, (nf.y + hf.y) / 2f + 2f)
        // The fill reaches a little into the body to hide the body's outline under the neck: that part takes the
        // body colour, and the neck itself takes the head colour.
        val reach = Path().apply {
            moveTo(nb.x, nb.y + 8f)
            lineTo(nb.x, nb.y)
            lineTo(nf.x, nf.y)
            lineTo(nf.x, nf.y + 8f)
            close()
        }
        fill(reach, pal.body)
        val neck = Path().apply {
            moveTo(nb.x, nb.y)
            quadraticTo(cb.x, cb.y, hb.x, hb.y)
            lineTo(hbi.x, hbi.y)
            lineTo(hfi.x, hfi.y)
            lineTo(hf.x, hf.y)
            quadraticTo(cf.x, cf.y, nf.x, nf.y)
            quadraticTo(NeckMid.x, NeckMid.y, nb.x, nb.y)
            close()
        }
        fill(neck, head)
        val edges = Path().apply {
            moveTo(nb.x, nb.y)
            quadraticTo(cb.x, cb.y, hb.x, hb.y)
            moveTo(hf.x, hf.y)
            quadraticTo(cf.x, cf.y, nf.x, nf.y)
        }
        ink(edges, pal.outline, 3.5f)
    }
}

/** Shine, cheek, brows, eyes, tears, bill, sleep bubble and the quack's sound waves, all in head units. */
private fun DrawScope.drawFace(p: DuckPose, fx: DuckFx, pal: Palette, rig: Rig) {
    val d = pal.duck
    val idle = fx.idle
    val t = fx.time
    val sw = HEAD_STROKE
    val headShade = d.headShade ?: d.shade
    val headShine = d.headShine ?: d.belly
    inHead(rig) {
        fill(CheekShade, headShade, 0.3f)
        if (d.crown != null) fill(Crown, d.crown, 0.85f)
        tiltedOval(92f, 36f, 9f, 4.5f, headShine, 0.85f, -35f, 92f, 36f)
        oval(81f, 46f, 2.4f, 2.4f, headShine, 0.8f)
        val cheekA = p.cheek * d.cheekStrength
        oval(90f, 80f, 8.5f, 5.5f, pal.cheek, cheekA)
        ink(BlushStrokes, Blush, 1.5f * sw, cheekA)
        if (d.eyeStripe != null) ink(EyeStripe, d.eyeStripe, 4f * sw, 0.9f)
        if (p.browA > 0.02f) {
            ink(q1(p.browN), pal.outline, 3.2f * sw, p.browA)
            ink(q1(p.browF), pal.outline, 3f * sw, p.browA)
        }

        // Eyes: the open eye, then the closed stroke for blinks and closed-eye moods.
        val effOpen = p.open * (1f - p.blink * fx.blink)
        val ca = 1f - smooth(0.08f, 0.4f, effOpen)
        val shape = if (p.open > 0.5f) DuckShapes.ClosedSleep else p.closed
        for (e in DuckShapes.Eyes) {
            val rx = e.rx * p.eyeScale
            val ry = e.ry * p.eyeScale
            val w = rx + 1f
            if (effOpen > 0.03f) {
                val oy = ry * effOpen
                val ha = smooth(0.45f, 0.8f, effOpen)
                oval(e.cx, e.cy, rx, oy, Eye)
                val lower = Path().apply {
                    moveTo(e.cx - rx + 1.2f, e.cy + 2f * effOpen)
                    quadraticTo(e.cx, e.cy + oy + 2.5f, e.cx + rx - 1.2f, e.cy + 2f * effOpen)
                    quadraticTo(e.cx, e.cy + oy - 3f * effOpen, e.cx - rx + 1.2f, e.cy + 2f * effOpen)
                    close()
                }
                fill(lower, EyeLow)
                oval(e.cx + rx * 0.3f, e.cy - oy * 0.35f, rx * 0.42f, oy * 0.32f, Color.White, ha)
                oval(e.cx - rx * 0.35f, e.cy + oy * 0.4f, rx * 0.2f, oy * 0.15f, Color.White, ha)
                oval(e.cx - rx * 0.45f, e.cy - oy * 0.45f, rx * 0.12f, rx * 0.12f * effOpen, Color.White, ha * 0.8f)
            }
            if (ca > 0.02f) {
                val c = List(6) { j -> if (j % 2 == 0) e.cx + shape[j] * w * e.dir else e.cy + shape[j] }
                ink(q1(c), pal.outline, p.closedW * sw, ca)
            }
            if (p.lashes > 0.02f) {
                val x0 = e.cx - w * e.dir
                val lash = Path().apply {
                    moveTo(x0 + 2f * e.dir, e.cy + 3f)
                    lineTo(x0 - 2f * e.dir, e.cy + 7f)
                }
                ink(lash, pal.outline, 2.2f * sw, p.lashes)
            }
        }
        if (p.well > 0.02f) fill(Well, pal.tear, 0.9f * p.well * smooth(0.3f, 0.8f, effOpen))
        if (p.tearDrop > 0.02f) {
            val ph = if (idle) (t * 0.55f) % 1f else 0.3f
            val a = p.tearDrop * (1f - smooth(0.75f, 1f, ph)) * smooth(0.3f, 0.8f, effOpen)
            drop(pal, 103f, 74f + ph * 14f, 12f, a, 2.2f * sw)
        }
        drawBill(p, pal, sw)
        // No line is ever drawn on the face at the corner of the bill: it reads as a second mouth. Smiles come from
        // the eyes, cheeks and open bill; unhappiness from the drooping bill.
        if (p.bubble > 0.02f) {
            val br = 7f * (0.7f + 0.3f * (if (idle) 0.5f - 0.5f * cos(t * PI.toFloat()) else 1f))
            oval(181f, 93f, br, br, Bubble, 0.9f * p.bubble, outline = pal.outline, w = 2f * sw)
            oval(179f, 91f, 2f, 1.5f, Color.White, p.bubble)
        }
        if (p.poof > 0.02f) ink(PoofHead, pal.outline, 3f * sw, p.poof)
        if (fx.quack >= 0f) drawQuack(fx.quack, pal, sw)
    }
}

/** The lower half swings open under the wide flat upper half; the whole bill droops when unhappy. */
private fun DrawScope.drawBill(p: DuckPose, pal: Palette, sw: Float) {
    val d = pal.duck
    val ang = p.mouthOpen * 20f
    val droop = max(0f, -p.smile) * 7f
    val tip = rot(164f, 92f, ang, 116f, 86f)
    withTransform({ rotate(droop, Offset(113f, 82f)) }) {
        if (p.mouthOpen > 0.02f) {
            val mouth = Path().apply {
                moveTo(116f, 86f)
                lineTo(166f, 88f)
                lineTo(tip.x, tip.y)
                close()
            }
            fill(mouth, pal.mouth)
            withTransform({ rotate(ang * 0.5f, Offset(116f, 86f)) }) {
                oval(132f, 90f + 3f * p.mouthOpen, 9f, 3f * p.mouthOpen, pal.tongue)
            }
        }
        withTransform({ rotate(ang, Offset(116f, 86f)) }) {
            val lower = Path().apply {
                moveTo(116f, 86f)
                quadraticTo(140f, 91f, 164f, 88f)
                quadraticTo(165f, 94f, 146f, 95.5f)
                quadraticTo(126f, 96f, 116f, 90f)
                close()
            }
            outlined(pal, lower, d.billLow, 3f * sw)
        }
        val upper = Path().apply {
            moveTo(112f, 78f)
            quadraticTo(124f, 72f, 140f, 74.5f)
            quadraticTo(158f, 77f, 170f, 74f)
            quadraticTo(181f, 75f, 176f, 84f)
            quadraticTo(170f, 90f, 150f, 90f)
            quadraticTo(128f, 90f, 114f, 85f)
            close()
        }
        outlined(pal, upper, d.bill, 3f * sw)
        val nail = Path().apply {
            moveTo(168f, 75f)
            quadraticTo(178f, 75f, 176f, 83f)
            quadraticTo(171f, 82f, 168f, 75f)
            close()
        }
        fill(nail, d.billTip)
        val underside = Path().apply {
            moveTo(114f, 85f)
            quadraticTo(128f, 90f, 150f, 90f)
            quadraticTo(166f, 90f, 172f, 86f)
            quadraticTo(150f, 87f, 114f, 82f)
            close()
        }
        fill(underside, d.billLow, 0.55f)
        if (d.billSaddle != null) fill(BillSaddle, d.billSaddle, 0.85f)
        val ridge = Path().apply {
            moveTo(120f, 77f)
            quadraticTo(140f, 77f, 162f, 78f)
        }
        ink(ridge, Color.White, 2.2f * sw, 0.55f)
        val nostrils = Path().apply {
            moveTo(139f, 79.5f); lineTo(142f, 79.5f)
            moveTo(150f, 80f); lineTo(153f, 80f)
        }
        ink(nostrils, Nostril, 2.4f * sw)
    }
}

/** Sound waves pop out of the bill tip when patted (no words, so nothing needs translating). */
private fun DrawScope.drawQuack(quack: Float, pal: Palette, sw: Float) {
    val cx = 180f
    val cy = 82f
    for (i in 0 until 3) {
        val q = (quack - i * 0.07f) / 0.6f
        if (q <= 0f || q >= 1f) continue
        val r = 7f + 6f * i + 8f * q
        val arc = Path().apply {
            arcTo(Rect(cx - r, cy - r, cx + r, cy + r), -ARC_DEG, ARC_DEG * 2f, true)
        }
        ink(arc, pal.outline, 3f * sw, 1f - smooth(0.5f, 1f, q))
    }
}

/** Half the angle of a sound wave's arc: 0.62 rad. */
private const val ARC_DEG = 0.62f * 180f / PI.toFloat()

/** Layered feathers folded on the body, blending into the wing clamped over the side of the head. */
private fun DrawScope.drawNearWing(p: DuckPose, fx: DuckFx, pal: Palette, rig: Rig) {
    val d = pal.duck
    val cover = smooth(0f, 1f, p.cover)
    val flap = if (fx.idle) p.flap * (0.5f + 0.5f * sin(fx.time * PI.toFloat() * 2f * p.flapHz)) else 0f
    val restAngle = (p.wing + flap) * (1f - cover)
    val rest = DuckShapes.WingRest
    val up = DuckShapes.WingCover
    val pts = List(rest.size / 2) {
        val r = rot(rest[it * 2], rest[it * 2 + 1], restAngle, 122f, 130f)
        val c = if (it in DuckShapes.WingCoverBodyPoints) Offset(up[it * 2], up[it * 2 + 1]) else rig.head(up[it * 2], up[it * 2 + 1])
        Offset(lerp(r.x, c.x, cover), lerp(r.y, c.y, cover))
    }
    outlined(pal, qPath(pts), d.wing, 3.5f)
    val restA = 1f - smooth(0f, 0.5f, cover)
    val coverA = smooth(0.5f, 1f, cover)
    if (restA > 0.02f) {
        withTransform({ rotate(restAngle, Offset(122f, 130f)) }) {
            fill(WingBelly, d.belly, 0.45f * restA)
            ink(WingFeathers, d.shade, 2f, restA)
            // The blue patch is a flight feather at the back of the folded wing, tapering to a point toward the front.
            if (d.speculum != null) {
                fill(Speculum, d.speculum, restA)
                fill(SpeculumGlint, SpeculumLight, 0.8f * restA)
            }
            ink(WingLines, d.shade, 2.2f, restA)
        }
    }
    if (coverA > 0.02f) inHead(rig) { ink(WingCoverLines, d.shade, 2.2f * HEAD_STROKE, coverA) }
}

/** Sweat, sparkles, Z's and shock lines. */
private fun DrawScope.drawExtras(p: DuckPose, fx: DuckFx, pal: Palette, rig: Rig) {
    val t = fx.time
    val idle = fx.idle
    if (p.sweat > 0.02f) {
        val sp = rig.head(62f, 30f)
        drop(pal, sp.x, sp.y, 13f - 2f * p.shake, p.sweat)
    }
    if (p.sparkle > 0.02f) {
        fun tw(k: Float) = if (idle) 0.75f + 0.25f * sin(t * 5f + k) else 1f
        sparkle(pal, 22f, 52f, 8f * tw(0f), p.sparkle)
        sparkle(pal, 182f, 26f, 6f * tw(2f), p.sparkle)
        sparkle(pal, 190f, 104f, 4f * tw(4f), p.sparkle)
    }
    if (p.zzz > 0.02f) {
        val bob = if (idle) sin(t * 1.6f) * 2.5f else 0f
        withTransform({ translate(0f, bob) }) {
            ink(ZzzBig, pal.outline, 3.5f, p.zzz)
            ink(ZzzSmall, pal.outline, 3f, p.zzz)
        }
    }
    if (p.shock > 0.02f) {
        val fl = if (idle) 0.7f + 0.3f * sin(t * 22f) else 1f
        val r = DuckShapes.HEAD_R
        val x = rig.cx
        val y = rig.cy
        val lines = Path().apply {
            moveTo(x - r - 22f, y - 18f); lineTo(x - r - 36f, y - 26f)
            moveTo(x - r - 6f, y - r - 6f); lineTo(x - r - 16f, y - r - 18f)
            moveTo(x + r + 26f, y - 18f); lineTo(x + r + 40f, y - 26f)
            moveTo(x + r + 8f, y - r - 6f); lineTo(x + r + 16f, y - r - 18f)
        }
        ink(lines, QB.TooLoud, 4f, p.shock * fl)
    }
}
