package com.quietbuddy.ui.character.kitten

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
import com.quietbuddy.ui.character.AccessoryAnchors
import com.quietbuddy.ui.character.drawAccessories
import com.quietbuddy.ui.theme.Palette
import com.quietbuddy.ui.theme.QB
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

// Kitten-only colours that never change with the style; the rest (outline, body, stripes, ...) come from the palette.
private val InnerEar = Color(0xFFFFB3BE)
private val Nose = Color(0xFFF47C93)
private val Eye = Color(0xFF3B2418)
private val EyeLow = Color(0xFF7A5340)
private val Spark = Color(0xFFFFD45C)
private val Bubble = Color(0xFFDFF1FF)

// Final eye settings from the prototype's sliders (eyeFor(135, 8)): bigger, set slightly higher.
private const val EYE_S = 1.35f
private const val EYE_DX = 1.995f
private const val EYE_DY = -6.005f

private fun clamp01(x: Float) = x.coerceIn(0f, 1f)
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun smooth(a: Float, b: Float, x: Float): Float {
    val t = clamp01((x - a) / (b - a))
    return t * t * (3f - 2f * t)
}

private fun mirrorPts(p: List<Float>) = List(p.size) { if (it % 2 == 0) 200f - p[it] else p[it] }

/** Builds a Path from a small SVG subset (M, L, Q, Z with absolute coordinates) so static shapes stay as in the prototype. */
private fun svgPath(d: String): Path {
    val path = Path()
    val t = d.trim().split(Regex("\\s+"))
    var i = 0
    fun n() = t[i++].toFloat()
    while (i < t.size) {
        when (t[i++]) {
            "M" -> path.moveTo(n(), n())
            "L" -> path.lineTo(n(), n())
            "Q" -> path.quadraticTo(n(), n(), n(), n())
            "Z" -> path.close()
        }
    }
    return path
}

private val Belly = svgPath(
    "M 76 132 Q 100 142 124 132 Q 128 146 120 156 Q 117 152 114 160 Q 110 154 106 164 Q 102 157 100 166 " +
        "Q 98 157 94 164 Q 90 154 86 160 Q 83 152 80 156 Q 72 146 76 132 Z",
)
private val Crest = svgPath("M 90 40 Q 91 32 94 30 Q 96 34 99 34 Q 100 26 105 24 Q 105 30 107 33 Q 109 30 112 30 Q 110 35 111 40 Z")
private val ForeheadStripes = svgPath("M 95 35 Q 100 56 105 35 Z M 80 38 Q 87 54 89 36 Z M 120 38 Q 113 54 111 36 Z")
private val CheekStripes = svgPath(
    "M 37 82 Q 50 85 37 89 Z M 163 82 Q 150 85 163 89 Z M 38 96 Q 48 98 39 102 Z M 162 96 Q 152 98 161 102 Z",
)
private val WhiskersL = svgPath("M 60 108 Q 42 103 24 102 M 60 114 Q 42 115 24 120 M 62 119 Q 48 124 34 132")
private val WhiskersR = svgPath("M 140 108 Q 158 103 176 102 M 140 114 Q 158 115 176 120 M 138 119 Q 152 124 166 132")
private val TuftsL = svgPath("M 52 54 Q 50 42 47 34 M 57 52 Q 57 42 55 36 M 62 50 Q 63 43 63 38")
private val TuftsR = svgPath("M 148 54 Q 150 42 153 34 M 143 52 Q 143 42 145 36 M 138 50 Q 137 43 137 38")
private val NosePath = svgPath("M 93 102 Q 100 98 107 102 Q 105 108 100 110 Q 95 108 93 102 Z")
private val Fang = svgPath("M 90 115.5 L 92.5 121 L 95 117 Z")
private val Lashes = Pair(svgPath("M 60 94 L 55 98 M 64 97 L 61 102"), svgPath("M 140 94 L 145 98 M 136 97 L 139 102"))
private val ZzzBig = svgPath("M 172 50 L 186 50 L 172 64 L 186 64")
private val ZzzSmall = svgPath("M 188 28 L 197 28 L 188 37 L 197 37")
private val ShockMarks = svgPath("M 40 22 L 30 10 M 58 12 L 54 -2 M 160 22 L 170 10 M 142 12 L 146 -2")
private val WellL = svgPath("M 59 100 Q 70 112 81 100 Q 70 106 59 100 Z")
private val WellR = svgPath("M 141 100 Q 130 112 119 100 Q 130 106 141 100 Z")

private fun strokeOf(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.fill(p: Path, color: Color, alpha: Float = 1f) = drawPath(p, color, alpha = alpha)

private fun DrawScope.ink(p: Path, color: Color, w: Float, alpha: Float = 1f) =
    drawPath(p, color, alpha = alpha, style = strokeOf(w))

private fun DrawScope.outlined(pal: Palette, p: Path, color: Color, w: Float = 4f, alpha: Float = 1f) {
    drawPath(p, color, alpha = alpha)
    drawPath(p, pal.outline, alpha = alpha, style = strokeOf(w))
}

private fun DrawScope.oval(
    cx: Float, cy: Float, rx: Float, ry: Float, color: Color, alpha: Float = 1f,
    outline: Color? = null, w: Float = 4f,
) {
    val tl = Offset(cx - max(0f, rx), cy - max(0f, ry))
    val sz = Size(max(0f, rx) * 2f, max(0f, ry) * 2f)
    drawOval(color, tl, sz, alpha = alpha)
    if (outline != null) drawOval(outline, tl, sz, alpha = alpha, style = Stroke(width = w))
}

/** Polyline of x,y pairs as a quad path: [pairs] 3 points per segment chain (matches q1/q2/q3 in the prototype). */
private fun quadChain(p: List<Float>, close: Boolean = false): Path = Path().apply {
    moveTo(p[0], p[1])
    var i = 2
    while (i + 3 < p.size) {
        quadraticTo(p[i], p[i + 1], p[i + 2], p[i + 3])
        i += 4
    }
    if (close) close()
}

/** An ellipse outline made of fur tufts; [depthAt] sets how far each tuft sticks out. */
private fun fluffy(cx: Float, cy: Float, rx: Float, ry: Float, n: Int, sweep: Float, depthAt: (Float, Int) -> Float): Path {
    fun pt(a: Float, k: Float): Offset {
        val r = a * PI.toFloat() / 180f
        return Offset(cx + (rx + k) * cos(r), cy + (ry + k) * sin(r))
    }
    val span = 360f / n
    val path = Path()
    val start = pt(-90f, 0f)
    path.moveTo(start.x, start.y)
    for (i in 0 until n) {
        val a = -90f + i * span
        val dd = depthAt(a + span * 0.5f, i)
        val c1 = pt(a + span * 0.3f, dd * 0.8f)
        val tip = pt(a + span * sweep, dd)
        val c2 = pt(a + span * 0.9f, dd * 0.2f)
        val v = pt(a + span, 0f)
        path.quadraticTo(c1.x, c1.y, tip.x, tip.y)
        path.quadraticTo(c2.x, c2.y, v.x, v.y)
    }
    path.close()
    return path
}

private fun zone(a: Float, centers: List<Float>, half: Float, soft: Float): Float {
    var w = 0f
    for (c in centers) w = max(w, clamp01((half - kotlin.math.abs(a - c)) / soft))
    return w
}

private fun vary(i: Int) = 1f + 0.35f * sin(i * 2.4f + 0.7f)

private val HeadZones = listOf(12f, 168f)
private val BodyZones = listOf(10f, 170f)

/** The tail as an outlined tube along three quadratic segments; poof thickens it and adds bristles. */
private fun DrawScope.tail(pal: Palette, pts: List<Float>, poof: Float, swayDeg: Float) {
    val bx = pts[0]
    val by = pts[1]
    val rad = swayDeg * PI.toFloat() / 180f
    val cs = cos(rad)
    val sn = sin(rad)
    val px = FloatArray(7)
    val py = FloatArray(7)
    for (k in 0 until 7) {
        val dx = pts[k * 2] - bx
        val dy = pts[k * 2 + 1] - by
        px[k] = bx + dx * cs - dy * sn
        py[k] = by + dx * sn + dy * cs
    }
    val n = 14
    val sx = ArrayList<Float>()
    val sy = ArrayList<Float>()
    val stx = ArrayList<Float>()
    val sty = ArrayList<Float>()
    for (si in 0 until 3) {
        val a = si * 2
        for (j in (if (si == 0) 0 else 1)..n) {
            val t = j / n.toFloat()
            val u = 1f - t
            sx += u * u * px[a] + 2 * u * t * px[a + 1] + t * t * px[a + 2]
            sy += u * u * py[a] + 2 * u * t * py[a + 1] + t * t * py[a + 2]
            var tx = 2 * u * (px[a + 1] - px[a]) + 2 * t * (px[a + 2] - px[a + 1])
            var ty = 2 * u * (py[a + 1] - py[a]) + 2 * t * (py[a + 2] - py[a + 1])
            val len = hypot(tx, ty).let { if (it == 0f) 1f else it }
            tx /= len
            ty /= len
            stx += tx
            sty += ty
        }
    }
    val count = sx.size
    val r = 9f + poof * 4f
    val spike = poof * 7f
    fun tri(i: Int) = floatArrayOf(0f, 0.5f, 1f, 0.5f)[i % 4]
    val leftX = FloatArray(count)
    val leftY = FloatArray(count)
    val rightX = FloatArray(count)
    val rightY = FloatArray(count)
    for (i in 0 until count) {
        val nx = -sty[i]
        val ny = stx[i]
        val fade = smooth(0f, 4f, i.toFloat())
        val rl = r + spike * tri(i) * fade
        val rr = r + spike * tri(i + 2) * fade
        leftX[i] = sx[i] + nx * rl
        leftY[i] = sy[i] + ny * rl
        rightX[i] = sx[i] - nx * rr
        rightY[i] = sy[i] - ny * rr
    }
    val path = Path()
    path.moveTo(leftX[0], leftY[0])
    for (i in 1 until count) path.lineTo(leftX[i], leftY[i])
    // Rounded end cap
    val ex = sx[count - 1]
    val ey = sy[count - 1]
    val etx = stx[count - 1]
    val ety = sty[count - 1]
    val capR = r + spike * 0.6f
    for (k in 1 until 8) {
        val a = PI.toFloat() * k / 8f
        val nx = -ety
        val ny = etx
        path.lineTo(
            ex + (nx * cos(a) + etx * sin(a)) * capR,
            ey + (ny * cos(a) + ety * sin(a)) * capR,
        )
    }
    for (i in count - 1 downTo 0) path.lineTo(rightX[i], rightY[i])
    path.close()
    outlined(pal, path, pal.body)

    val stripes = Path()
    for (fr in listOf(0.5f, 0.68f, 0.85f)) {
        val i = Math.round(fr * (count - 1))
        val nx = -sty[i]
        val ny = stx[i]
        val rs = r * 0.55f
        stripes.moveTo(sx[i] + nx * rs, sy[i] + ny * rs)
        stripes.lineTo(sx[i] - nx * rs, sy[i] - ny * rs)
    }
    ink(stripes, pal.stripe, 3.5f)
}

private fun DrawScope.ear(pal: Palette, p: KittenPose, left: Boolean, twitchDeg: Float) {
    val fold = p.earFold
    var outer = List(14) { lerp(KittenShapes.EarUp[it], KittenShapes.EarFlat[it], fold) }
    var inner = List(14) { lerp(KittenShapes.InUp[it], KittenShapes.InFlat[it], fold) }
    var angle = p.ear + twitchDeg
    var pivotX = 66f
    if (!left) {
        outer = mirrorPts(outer)
        inner = mirrorPts(inner)
        angle = -angle
        pivotX = 134f
    }
    withTransform({ rotate(angle, Offset(pivotX, 60f)) }) {
        outlined(pal, quadChain(outer, close = true), pal.body)
        fill(quadChain(inner, close = true), InnerEar)
        ink(if (left) TuftsL else TuftsR, pal.cream, 2f, clamp01(1f - fold * 1.4f))
    }
}

private fun DrawScope.sparkle(pal: Palette, x: Float, y: Float, s: Float, a: Float) {
    val k = s * 0.18f
    val p = Path().apply {
        moveTo(x, y - s)
        quadraticTo(x + k, y - k, x + s, y)
        quadraticTo(x + k, y + k, x, y + s)
        quadraticTo(x - k, y + k, x - s, y)
        quadraticTo(x - k, y - k, x, y - s)
        close()
    }
    outlined(pal, p, Spark, 2f, clamp01(a))
}

private fun DrawScope.drop(pal: Palette, x: Float, y: Float, h: Float, a: Float) {
    val p = Path().apply {
        moveTo(x, y)
        quadraticTo(x + h * 0.35f, y + h * 0.7f, x, y + h)
        quadraticTo(x - h * 0.35f, y + h * 0.7f, x, y)
        close()
    }
    outlined(pal, p, pal.tear, 2.5f, clamp01(a))
}

/** The open eye at the base size; the caller scales it about its centre. */
private fun DrawScope.eye(cx: Float, cy: Float, p: KittenPose, open: Float) {
    if (open <= 0.03f) return
    val ry = p.eyeRy * open
    val rx = p.eyeRx
    oval(cx, cy, rx, ry, Eye)
    val lower = Path().apply {
        moveTo(cx - rx + 2f, cy + 3f * open)
        quadraticTo(cx, cy + (ry + 3f), cx + rx - 2f, cy + 3f * open)
        quadraticTo(cx, cy + ry - 5f * open, cx - rx + 2f, cy + 3f * open)
        close()
    }
    fill(lower, EyeLow)
    val ha = smooth(0.45f, 0.8f, open)
    val big = lerp(4f, 5f, p.hi)
    oval(cx + 4f, cy - 5f * open, big, big * 1.1f * open, Color.White, ha)
    oval(cx - 4f, cy + 6f * open, 2.4f, 2.4f * open, Color.White, ha)
}

/**
 * Draws the kitten at 200x200 design-space scale. It's drawn full size and nudged up 4 units so it fills the space
 * like Buddy does; only the wrapped sleeping tail and the shock marks reach a few units past the edges, and the
 * canvas doesn't clip.
 */
fun DrawScope.drawKitten(pose: KittenPose, fx: KittenFx, pal: Palette, accessories: Set<Accessory> = emptySet()) {
    withTransform({
        translate(0f, -4f)
    }) {
        drawRig(pose, fx, pal, accessories)
    }
}

private val BodySlots = setOf(AccessorySlot.NECK, AccessorySlot.FEET)
private val HeadSlots = setOf(AccessorySlot.HEAD, AccessorySlot.FACE)

/**
 * Where the kitten wears things for pose [p]. Head anchors are in head space (drawHead's translate), so they ride
 * with headDy; the hat lifts as the fur poofs and the glasses follow the eyes.
 */
private fun anchors(p: KittenPose, poof: Float): AccessoryAnchors {
    // Matches the eyeSpace transform in drawHead: an eye drawn at y = 90 + eyeDy ends up here.
    val eyeY = 92f + p.eyeDy + EYE_DY - 2f * EYE_S
    return AccessoryAnchors(
        hatAt = Offset(100f, 38f - 6f * poof), hatScale = 0.8f, hatTilt = -4f,
        bowAt = Offset(141f, 42f - 6f * poof), bowScale = 0.75f, bowTilt = 30f,
        flowerAt = Offset(59f, 42f - 6f * poof), flowerScale = 0.75f, flowerTilt = -30f,
        collarAt = Offset(100f, 145f), collarScale = 0.9f,
        leftEye = Offset(70f - EYE_DX, eyeY), rightEye = Offset(130f + EYE_DX, eyeY), lensRx = 21f, lensRy = 17f,
        neckAt = Offset(100f, 140f), neckScale = 0.9f,
        leftFoot = Offset(84f, 184f), rightFoot = Offset(116f, 184f), footRx = 16f, footRy = 10f,
    )
}

private fun DrawScope.drawRig(p: KittenPose, fx: KittenFx, pal: Palette, accessories: Set<Accessory>) {
    val t = fx.time
    val idle = fx.idle
    val sway = if (idle) p.sway * sin(t * PI.toFloat() * 2f * p.swayHz) else 0f
    val tremble = if (idle) p.shake else 0f
    val poofJitter = tremble * 0.12f * sin(t * 47f)
    val poof = (p.poof + poofJitter).coerceIn(0f, 1.2f)

    // Whole-body motion: tremble and breathing are weighted by the pose, so they ease in and out with it.
    val dx = if (idle) tremble * 3f * sin(t * PI.toFloat() * 2f / 0.12f) else 0f
    val breath = if (idle) 1f + 0.02f * p.breathe * (0.5f - 0.5f * cos(t * PI.toFloat())) else 1f

    withTransform({
        translate(dx, 0f)
        scale(breath, breath, Offset(100f, 100f))
    }) {
        val tailBehind = p.tailFront < 0.5f
        if (tailBehind) tail(pal, p.tail, poof, sway)

        // body
        val body = fluffy(100f, 158f, 46f + 6f * poof, 34f + 2f * poof, 20, 0.66f) { a, i ->
            6f * zone(a, BodyZones, 20f, 8f) + poof * 8f * vary(i)
        }
        outlined(pal, body, pal.body)
        fill(Belly, pal.cream)
        val belly = Path().apply {
            moveTo(70f - 4f * poof, 152f); lineTo(76f - 2f * poof, 150f)
            moveTo(130f + 4f * poof, 152f); lineTo(124f + 2f * poof, 150f)
        }
        ink(belly, pal.stripe, 2.5f)
        if (!tailBehind) tail(pal, p.tail, poof, sway)
        for (cx in listOf(84f, 116f)) {
            oval(cx, 184f, 16f, 10f, pal.cream, outline = pal.outline)
            val toes = Path().apply {
                moveTo(cx - 5f, 178f); lineTo(cx - 5f, 184f)
                moveTo(cx + 5f, 178f); lineTo(cx + 5f, 184f)
            }
            ink(toes, pal.outline, 2.5f)
        }
        // Tie and boots go under the head, so the chin tucks over the knot.
        val anchors = anchors(p, poof)
        drawAccessories(accessories, anchors, pal.outline, BodySlots)

        withTransform({ translate(0f, p.headDy) }) {
            drawHead(p, fx, pal, poof)
            drawAccessories(accessories, anchors, pal.outline, HeadSlots)
        }

        if (p.sparkle > 0.02f) {
            fun tw(k: Float) = if (idle) 0.75f + 0.25f * sin(t * 5f + k) else 1f
            sparkle(pal, 26f, 46f, 8f * tw(0f), p.sparkle)
            sparkle(pal, 176f, 36f, 6f * tw(2f), p.sparkle)
            sparkle(pal, 184f, 66f, 4f * tw(4f), p.sparkle)
        }
        if (p.zzz > 0.02f) {
            val bob = if (idle) sin(t * 1.6f) * 2.5f else 0f
            withTransform({ translate(0f, bob) }) {
                ink(ZzzBig, pal.outline, 3.5f, clamp01(p.zzz))
                ink(ZzzSmall, pal.outline, 3f, clamp01(p.zzz))
            }
        }
        if (p.shock > 0.02f) {
            val fl = if (idle) 0.7f + 0.3f * sin(t * 22f) else 1f
            ink(ShockMarks, QB.TooLoud, 4f, clamp01(p.shock * fl))
        }
    }
}

private fun DrawScope.drawHead(p: KittenPose, fx: KittenFx, pal: Palette, poof: Float) {
    val t = fx.time
    val idle = fx.idle
    ear(pal, p, left = true, twitchDeg = fx.twitchL)
    ear(pal, p, left = false, twitchDeg = fx.twitchR)
    outlined(pal, Crest, pal.body)
    val head = fluffy(100f, 88f, 64f, 54f, 28, 0.66f) { a, i ->
        10f * zone(a, HeadZones, 26f, 8f) + poof * 9f * vary(i)
    }
    outlined(pal, head, pal.body)
    // markings
    fill(ForeheadStripes, pal.stripe)
    fill(CheekStripes, pal.stripe)
    oval(89f, 112f, 15f, 11f, pal.cream)
    oval(111f, 112f, 15f, 11f, pal.cream)
    oval(100f, 120f, 14f, 8f, pal.cream)
    // whiskers
    withTransform({ rotate(-p.whisker, Offset(60f, 112f)) }) { ink(WhiskersL, pal.outline, 2f, 0.75f) }
    withTransform({ rotate(p.whisker, Offset(140f, 112f)) }) { ink(WhiskersR, pal.outline, 2f, 0.75f) }
    // cheeks
    oval(55f, 112f, 11f, 7f, pal.cheek, clamp01(p.cheek))
    oval(145f, 112f, 11f, 7f, pal.cheek, clamp01(p.cheek))

    // Eyes are drawn at the base size, then scaled about each eye's centre. Brows ride with them.
    val lift = (EYE_S - 1f) * 15f - EYE_DY
    val eyeCy = 92f + p.eyeDy
    fun DrawScope.eyeSpace(left: Boolean, sc: Float, block: DrawScope.() -> Unit) {
        val cx = if (left) 70f else 130f
        val dx = if (left) -EYE_DX else EYE_DX
        withTransform({
            translate(cx + dx, eyeCy + EYE_DY)
            scale(sc, sc, Offset.Zero)
            translate(-cx, -eyeCy)
        }) { block() }
    }
    val closedS = 1f + (EYE_S - 1f) * 0.5f
    if (p.browA > 0.02f) {
        withTransform({ translate(-EYE_DX, -lift) }) { ink(quadChain(p.brow), pal.outline, 4f, clamp01(p.browA)) }
        withTransform({ translate(EYE_DX, -lift) }) { ink(quadChain(mirrorPts(p.brow)), pal.outline, 4f, clamp01(p.browA)) }
    }
    // open shape, then the closed stroke for blinks and closed-eye moods
    val effOpen = p.open * (1f - p.blink * fx.blink)
    val ca = 1f - smooth(0.08f, 0.4f, effOpen)
    val shape = if (p.open > 0.5f) List(6) { KittenShapes.ClosedSleep[it] + if (it % 2 == 1) p.eyeDy else 0f } else p.closed
    val wellA = 0.9f * p.well * smooth(0.3f, 0.8f, effOpen)
    for (left in listOf(true, false)) {
        eyeSpace(left, EYE_S) {
            eye(if (left) 70f else 130f, 90f + p.eyeDy, p, effOpen)
            if (p.well > 0.02f) fill(if (left) WellL else WellR, pal.tear, clamp01(wellA))
        }
    }
    if (ca > 0.02f || p.lashes > 0.02f) {
        for (left in listOf(true, false)) {
            eyeSpace(left, closedS) {
                if (ca > 0.02f) ink(quadChain(if (left) shape else mirrorPts(shape)), pal.outline, p.closedW, clamp01(ca))
                if (p.lashes > 0.02f) ink(if (left) Lashes.first else Lashes.second, pal.outline, 2.5f, clamp01(p.lashes))
            }
        }
    }

    // mouth
    if (p.mouthOpen > 0.02f) {
        val mo = p.mouthOpen
        val m = Path().apply {
            moveTo(90f, 116f)
            quadraticTo(100f, 116f + 20f * mo, 110f, 116f)
            quadraticTo(100f, 120f, 90f, 116f)
            close()
        }
        outlined(pal, m, pal.mouth, 3f)
        val tongue = Path().apply {
            moveTo(94f, 116f + 8f * mo)
            quadraticTo(100f, 116f + 4f * mo, 106f, 116f + 8f * mo)
            quadraticTo(104f, 116f + 15f * mo, 100f, 116f + 15f * mo)
            quadraticTo(96f, 116f + 15f * mo, 94f, 116f + 8f * mo)
            close()
        }
        fill(tongue, pal.tongue, clamp01(mo))
    }
    ink(quadChain(p.mouth), pal.outline, 3f)
    if (p.fang > 0.02f) outlined(pal, Fang, Color.White, 1.6f, clamp01(p.fang))
    ink(Path().apply { moveTo(100f, 110f); lineTo(p.mouth[4], p.mouth[5]) }, pal.outline, 3f)
    outlined(pal, NosePath, Nose, 2.5f)
    oval(97f, 102.5f, 2.2f, 1.2f, Color.White, 0.8f)

    // extras
    if (p.sweat > 0.02f) drop(pal, p.sweatX, p.sweatY, 16f - 3f * p.shake, p.sweat)
    if (p.tearDrop > 0.02f) {
        val ph = if (idle) (t * 0.55f) % 1f else 0.3f
        drop(
            pal,
            138f + EYE_DX + 4f * (EYE_S - 1f),
            106f + (EYE_S - 1f) * 12f + EYE_DY + ph * 16f,
            18f,
            p.tearDrop * (1f - smooth(0.75f, 1f, ph)) * smooth(0.3f, 0.8f, effOpen),
        )
    }
    if (p.bubble > 0.02f) {
        val br = 7f * (0.7f + 0.3f * (if (idle) 0.5f - 0.5f * cos(t * PI.toFloat()) else 1f))
        oval(112f, 106f, br, br, Bubble, clamp01(0.9f * p.bubble), outline = pal.outline, w = 2f)
        oval(110f, 104f, 2f, 1.5f, Color.White, clamp01(p.bubble))
    }
}
