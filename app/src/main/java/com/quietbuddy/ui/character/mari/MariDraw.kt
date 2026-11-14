package com.quietbuddy.ui.character.mari

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.quietbuddy.core.Accessory
import com.quietbuddy.ui.character.drawEguzkilore
import com.quietbuddy.ui.character.drawLantern
import com.quietbuddy.ui.character.drawRobin
import com.quietbuddy.ui.character.drawZapia
import com.quietbuddy.ui.theme.Palette
import com.quietbuddy.ui.theme.QB
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import com.quietbuddy.ui.character.mari.MariShapes as G

// Mari Domingi's colours that never change with the style; the style's own come from palette.mari.
private val Skin = Color(0xFFF7D3B5)
private val Linen = Color(0xFFFDFAF3)
private val LinenShade = Color(0xFFE6DDCC)
private val AppleRed = Color(0xFFE2463A)
private val AppleHi = Color(0xFFF7A69B)
private val Leaf = Color(0xFF5FA54B)
private val Stem = Color(0xFF6B4428)
private val Shoe = Color(0xFF8A5A3A)
private val AmberLine = Color(0xFFC98512)
private val MoonGold = Color(0xFFF3C64B)
private val Spark = Color(0xFF2E9F6E)
private val Apron = Color(0xFFFBF4E6)
private val Wicker = Color(0xFFC99A5E)
private val Weave = Color(0xFF8A6235)
private val Strap = Color(0xFF7A4E2E)
private val Sock = Color(0xFFFFFFFF)
private val Petal = Color(0xFFF7B7C8)
private val Tongue = Color(0xFFF08A8A)
private val Lace = Color(0xFFF3E6D0)
private val Shadow = Color(0xFF281E14)

/**
 * The prototype draws her in a taller space than the app's 200x200 square: her feet stand at y 285 and the tontorra's
 * hook reaches up to about y 20 (and some 50 higher at the top of the jump). Everything is drawn in the prototype's
 * coordinates and scaled by [FIT] about her feet, which land at [FLOOR] in the design space; so she stands about as
 * tall as the snowman and the robot, and even the jump stays inside the square.
 */
private const val FIT = 0.62f
private const val FLOOR = 192f

private fun clamp01(x: Float) = x.coerceIn(0f, 1f)
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun rad(deg: Float) = deg * PI.toFloat() / 180f

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

private fun strokeOf(w: Float) = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** A filled shape with the outline round it. */
private fun DrawScope.shape(pal: Palette, p: Path, fill: Color, w: Float = 4f, alpha: Float = 1f) {
    drawPath(p, fill, alpha = clamp01(alpha))
    drawPath(p, pal.outline, alpha = clamp01(alpha), style = strokeOf(w))
}

private fun DrawScope.line(p: Path, w: Float, color: Color, alpha: Float = 1f) =
    drawPath(p, color, alpha = clamp01(alpha), style = strokeOf(w))

private fun DrawScope.circ(pal: Palette, cx: Float, cy: Float, r: Float, fill: Color, w: Float = 4f) {
    drawCircle(fill, r, Offset(cx, cy))
    drawCircle(pal.outline, r, Offset(cx, cy), style = Stroke(w))
}

private fun DrawScope.ell(
    cx: Float, cy: Float, rx: Float, ry: Float, fill: Color, alpha: Float = 1f, outline: Color? = null, w: Float = 4f,
) {
    val tl = Offset(cx - max(0f, rx), cy - max(0f, ry))
    val sz = Size(max(0f, rx) * 2f, max(0f, ry) * 2f)
    drawOval(fill, tl, sz, alpha = clamp01(alpha))
    if (outline != null) drawOval(outline, tl, sz, alpha = clamp01(alpha), style = Stroke(w))
}

private fun polyline(vararg v: Float) = Path().apply {
    moveTo(v[0], v[1])
    for (i in 2 until v.size step 2) lineTo(v[i], v[i + 1])
}

/** A smooth curve through points using midpoint quadratics, like the prototype's smoothThrough. */
private fun Path.smoothThrough(ps: List<Offset>, move: Boolean) {
    if (move) moveTo(ps[0].x, ps[0].y) else lineTo(ps[0].x, ps[0].y)
    for (i in 1 until ps.size - 1) {
        val m = (ps[i] + ps[i + 1]) / 2f
        quadraticTo(ps[i].x, ps[i].y, m.x, m.y)
    }
    lineTo(ps.last().x, ps.last().y)
}

private fun starPath(x: Float, y: Float, s: Float) = Path().apply {
    val k = s * 0.2f
    moveTo(x, y - s)
    quadraticTo(x + k, y - k, x + s, y)
    quadraticTo(x + k, y + k, x, y + s)
    quadraticTo(x - k, y + k, x - s, y)
    quadraticTo(x - k, y - k, x, y - s)
    close()
}

private fun DrawScope.drop(pal: Palette, x: Float, y: Float, s: Float, alpha: Float) {
    val p = Path().apply {
        moveTo(x, y - 7f * s)
        cubicTo(x + 5f * s, y - s, x + 5f * s, y + 5f * s, x, y + 5f * s)
        cubicTo(x - 5f * s, y + 5f * s, x - 5f * s, y - s, x, y - 7f * s)
        close()
    }
    shape(pal, p, pal.tear, 2.5f, alpha)
}

private fun DrawScope.petal(x: Float, y: Float, angle: Float, alpha: Float) =
    withTransform({ rotate(angle, Offset(x, y)) }) { ell(x, y, 4.5f, 2.6f, Petal, alpha) }

// ---------- static shapes, in the prototype's coordinates ----------

private val AppleBody = svg("M0 -8 C6 -14 16 -12 16 0 C16 12 7 16 3 15 C1 14.5 -1 14.5 -3 15 C-7 16 -16 12 -16 0 C-16 -12 -6 -14 0 -8 Z")
private val AppleStem = svg("M0 -8 L1 -15")
private val AppleLeaf = svg("M2 -13 C6 -19 13 -18 14 -15 C10 -11 5 -11 2 -13 Z")

private val BasketHandle = svg("M-15 -8 C-14 -30 14 -30 15 -8")
private val BasketBody = svg("M-19 -9 L19 -9 L15 12 Q0 17 -15 12 Z")
private val BasketWeave = svg("M-17 -2 L17 -2 M-16 5 L16 5 M-8 -9 L-7 14 M0 -9 L0 15 M8 -9 L7 14")

/** Each leg's stocking and its laced abarka straps, by hip x. */
private val Socks = listOf(90f, 110f).associateWith { lx -> polyline(lx - 6f, 236f, lx + 6f, 236f, lx + 6f, 278f, lx - 6f, 278f).apply { close() } }
private val Straps = listOf(90f, 110f).associateWith { lx ->
    svg("M${lx - 6f} 263 L${lx + 6f} 270 M${lx + 6f} 263 L${lx - 6f} 270 M${lx - 6f} 271 L${lx + 6f} 277")
}

private val Skirt = svg("M63 262 C66 240 72 220 77 212 L123 212 C128 220 134 240 137 262 Q100 271 63 262 Z")
private val Hem = svg("M65 247 Q100 256 135 247 L136.5 258 Q100 267 63.5 258 Z")
private val HemLine = svg("M66 252 Q100 260 134 252")
private val ApronShape = svg("M83 212 L117 212 L122 250 Q100 256 78 250 Z")
private val ApronStripes = svg("M80.5 242 Q100 246 119.5 242 M80 246 Q100 250 120 246")
private val ApronLine = svg("M80 244 Q100 248 120 244")
private val Blouse = svg("M74 214 C74 201 84 192 100 192 C116 192 126 201 126 214 Z")
private val BodiceSides = svg("M78 214 C78 204 82 198 88 196 L92 214 Z M122 214 C122 204 118 198 112 196 L108 214 Z")
private val BodiceFront = svg("M90 200 L110 200 L111 214 L89 214 Z")
private val Laces = svg("M95 202 L105 207 M105 202 L95 207 M95 208 L105 213 M105 208 L95 213")
private val ShawlL = svg("M77 199 L90 196 L106 220 L97 224 Z")
private val ShawlR = svg("M123 199 L110 196 L94 220 L103 224 Z")

/** The white whirls on the dark hem: a ring and four little strokes each. */
private val WhirlRings = Path().apply {
    for ((x, y) in Whirls) addOval(androidx.compose.ui.geometry.Rect(Offset(x, y), 3.2f))
}
private val WhirlArms = Path().apply {
    for ((x, y) in Whirls) {
        for ((dx1, dy1, dx2, dy2) in listOf(
            floatArrayOf(2f, -2.4f, 0f, -3.2f), floatArrayOf(2.4f, 2f, 3.2f, 0f),
            floatArrayOf(-2f, 2.4f, 0f, 3.2f), floatArrayOf(-2.4f, -2f, -3.2f, 0f),
        )) {
            moveTo(x, y)
            quadraticTo(x + dx1, y + dy1, x + dx2, y + dy2)
        }
    }
}
private val Whirls get() = listOf(78f to 254f, 93f to 257f, 108f to 257f, 123f to 254f)

private val Collar: Path = run {
    val cx = G.CX
    val y = G.CY + G.R
    svg(
        "M$cx ${y - 2} C${cx - 8} ${y + 2} ${cx - 20} ${y + 2} ${cx - 22} ${y + 8} C${cx - 14} ${y + 14} ${cx - 4} ${y + 12} $cx ${y + 6} " +
            "C${cx + 4} ${y + 12} ${cx + 14} ${y + 14} ${cx + 22} ${y + 8} C${cx + 20} ${y + 2} ${cx + 8} ${y + 2} $cx ${y - 2} Z",
    )
}

private val HairFront: Path = run {
    val cx = G.CX
    val cy = G.CY
    val r = G.R
    svg(
        "M${cx - r + 1} ${cy - 2} C${cx - r + 2} ${cy - r + 2} ${cx - 10} ${cy - r - 2} $cx ${cy - r + 4} " +
            "C${cx + 10} ${cy - r - 2} ${cx + r - 2} ${cy - r + 2} ${cx + r - 1} ${cy - 2} " +
            "C${cx + r - 8} ${cy - r * 0.45f} ${cx + 10} ${cy - r * 0.62f} $cx ${cy - r * 0.5f} " +
            "C${cx - 10} ${cy - r * 0.62f} ${cx - r + 8} ${cy - r * 0.45f} ${cx - r + 1} ${cy - 2} Z",
    )
}
private val HairShade: Path = run {
    val cx = G.CX
    val cy = G.CY
    val r = G.R
    svg(
        "M${cx - 6} ${cy - r * 0.62f} C${cx - 14} ${cy - r * 0.7f} ${cx - 22} ${cy - r * 0.55f} ${cx - r + 6} ${cy - 8} " +
            "M${cx + 6} ${cy - r * 0.62f} C${cx + 14} ${cy - r * 0.7f} ${cx + 22} ${cy - r * 0.55f} ${cx + r - 6} ${cy - 8}",
    )
}

/** The hair that shows at each side of the face, below the tontorra. */
private val HairLocks: List<Path> = listOf(-1f, 1f).map { sd ->
    val cx = G.CX
    val cy = G.CY
    val r = G.R
    svg(
        "M${cx + sd * (r - 3)} ${cy - 10} C${cx + sd * (r + 9)} ${cy - 2} ${cx + sd * (r + 8)} ${cy + 16} ${cx + sd * (r - 1)} ${cy + 22} " +
            "C${cx + sd * (r + 1)} ${cy + 12} ${cx + sd * (r - 1)} ${cy + 2} ${cx + sd * (r - 6)} ${cy - 4} Z",
    )
}

/** The rolled end of the hook, around its centre: the cloth curls into a spiral rather than ending in a disc. */
private val HookSpiral = svg("M-13 2 A10 10 0 1 1 3 10 A6 6 0 0 1 -4 1 A3 3 0 0 1 1 -2")

/** The crescent moon of sleep, beside her head. */
private const val MOON_X = G.CX - G.R - 32f
private const val MOON_Y = G.CY - G.R - 24f
private val Crescent = svg("M$MOON_X ${MOON_Y - 13} A13 13 0 1 0 $MOON_X ${MOON_Y + 13} A16 16 0 0 1 $MOON_X ${MOON_Y - 13} Z")

// ---------- drawing ----------

/**
 * Draws Mari Domingi in the 200x200 design space, ported from the approved prototype
 * docs/design/mari-domingi-motion.html. See [FIT] for how her taller drawing is placed in the square.
 *
 * The pat's squash and hearts are drawn outside the art (BuddyTapEffects), so only her delighted face, the head lean,
 * the hook's boing and the combo tricks (apple toss, jump, dance, petal burst) are here. No text anywhere.
 */
fun DrawScope.drawMari(pose: MariPose, fx: MariFx, pal: Palette, accessories: Set<Accessory> = emptySet()) {
    val p = pose.patted(fx.patFace).watching(fx.watchFace)
    withTransform({
        translate(100f, FLOOR)
        scale(FIT, FIT, Offset.Zero)
        translate(-G.FEET_X, -G.FEET_Y)
    }) { drawScene(p, fx, pal, accessories) }
}

private fun DrawScope.apple(pal: Palette, x: Float, y: Float, s: Float, alpha: Float = 1f) {
    withTransform({
        translate(x, y)
        scale(s, s, Offset.Zero)
    }) {
        shape(pal, AppleBody, AppleRed, 3.5f, alpha)
        ell(-8f, -2f, 3f, 4.5f, AppleHi, 0.9f * alpha)
        line(AppleStem, 3.5f, Stem, alpha)
        shape(pal, AppleLeaf, Leaf, 2.5f, alpha)
    }
}

private fun DrawScope.basket(pal: Palette, x: Float, y: Float) {
    withTransform({ translate(x, y) }) {
        line(BasketHandle, 8f, pal.outline)
        line(BasketHandle, 3.5f, Wicker)
        apple(pal, -7f, -11f, 0.6f)
        apple(pal, 7f, -12f, 0.6f)
        shape(pal, BasketBody, Wicker, 3.5f)
        line(BasketWeave, 2.5f, Weave)
    }
}

/** A bent arm from the shoulder to the hand: the elbow is pushed outward, never a long straight arm. */
private fun DrawScope.arm(pal: Palette, sx: Float, sy: Float, hx: Float, hy: Float, color: Color, sideX: Float) {
    val dx = hx - sx
    val dy = hy - sy
    val len = hypot(dx, dy).takeIf { it > 0f } ?: 1f
    var nx = -dy / len
    var ny = dx / len
    if (nx * sideX < 0f) {
        nx = -nx
        ny = -ny
    }
    val bend = min(12f, len * 0.24f)
    val path = Path().apply {
        moveTo(sx, sy)
        quadraticTo((sx + hx) / 2f + nx * bend * 2f, (sy + hy) / 2f + ny * bend * 2f, hx, hy)
    }
    line(path, 16f, pal.outline)
    line(path, 9f, color)
}

private fun DrawScope.drawScene(p: MariPose, fx: MariFx, pal: Palette, accessories: Set<Accessory>) {
    val c = pal.mari
    val t = fx.time
    val idle = fx.idle
    val bobY = if (idle) -p.bob * (0.5f - 0.5f * cos(fx.bobPhase)) else 0f
    val sway = if (idle) p.sway * sin(fx.swayPhase) else 0f
    val tremble = if (idle) p.shake * 1.6f * sin(t * 47f) else 0f
    val nod = if (idle) p.nod * sin(fx.nodPhase) else 0f
    val wig = if (idle) p.handWiggle * sin(2f * PI.toFloat() * 1.6f * t) else 0f
    val lift = p.sink + fx.hop

    // Hands, with any trick (dance, jump, apple toss) blended over the mood.
    var hLx = p.hLx
    var hLy = p.hLy
    var hRx = p.hRx + wig * 0.6f
    var hRy = p.hRy - abs(wig)
    if (fx.handKL > 0.001f) {
        hLx = lerp(hLx, fx.handLX, fx.handKL)
        hLy = lerp(hLy, fx.handLY, fx.handKL)
    }
    if (fx.handKR > 0.001f) {
        hRx = lerp(hRx, fx.handRX, fx.handKR)
        hRy = lerp(hRy, fx.handRY, fx.handKR)
    }

    // Ground: a floor shadow that shrinks when she hops, and the apples that rolled out of the dropped basket.
    val hopK = 1f - clamp01(-fx.hop / 40f) * 0.4f
    ell(G.FEET_X, G.FEET_Y + 1f, 44f * hopK, 6f * hopK, Shadow, 0.13f)
    if (p.roll > 0.02f) {
        val r = p.roll
        val x1 = 44f - 30f * r
        val x2 = 58f + 96f * r
        withTransform({ rotate(-200f * r, Offset(x1, 278f)) }) { apple(pal, x1, 278f, 0.7f, r) }
        withTransform({ rotate(260f * r, Offset(x2, 278f)) }) { apple(pal, x2, 278f, 0.6f, r) }
    }

    // Whole-body motion: bob, sway about the feet, the dance, tremble, breathing and the jump's squash and stretch.
    val breath = if (idle) 1f + 0.02f * p.breathe * (0.5f - 0.5f * cos(t * PI.toFloat())) else 1f
    val st = fx.stretch
    val sx = breath * (1f + 0.07f * fx.squash) / sqrt(st)
    val sy = breath * (1f - 0.1f * fx.squash) * st
    val feet = Offset(G.FEET_X, G.FEET_Y)
    withTransform({
        translate(tremble, bobY + lift)
        rotate(sway + fx.dance, feet)
        scale(sx, sy, feet)
    }) {
        // Legs: white stockings laced with abarka straps. A kick swings the leg out sideways from a hip pivot hidden
        // under the skirt, so only the foot comes out below the hem; the legs are always behind the skirt.
        for ((lx, sd, kick) in listOf(Triple(90f, -1f, fx.legL), Triple(110f, 1f, fx.legR))) {
            withTransform({ rotate(-sd * 58f * kick, Offset(lx, 236f)) }) {
                shape(pal, Socks.getValue(lx), Sock, 3f)
                line(Straps.getValue(lx), 2.2f, Strap)
                ell(lx + 1f, 281f, 10f, 5.5f, Shoe, 1f, pal.outline, 3.5f)
            }
        }
        // The skirt swings a little against the sway; the apron on it.
        withTransform({ rotate(-sway * 0.8f, Offset(100f, 212f)) }) {
            shape(pal, Skirt, c.skirt)
            val hem = c.hem
            if (hem != null) {
                shape(pal, Hem, hem, 3f)
                line(WhirlRings, 1.3f, Color.White)
                line(WhirlArms, 1.1f, Color.White)
            } else {
                line(HemLine, 3f, c.skirtDark)
            }
            shape(pal, ApronShape, Apron, 3.5f)
            val stripe = c.stripe
            if (stripe != null) line(ApronStripes, 1.8f, stripe) else line(ApronLine, 2f, LinenShade)
        }
        // The white blouse, the laced bodice (or the dress) over it, and the crossed shawl.
        shape(pal, Blouse, Linen)
        if (c.laces) {
            shape(pal, BodiceSides, c.bodice, 3.5f)
            shape(pal, BodiceFront, c.bodice, 3.5f)
            line(Laces, 1.8f, Lace)
        } else {
            shape(pal, Blouse, c.bodice)
        }
        if (c.shawl) {
            shape(pal, ShawlL, Linen, 3f)
            shape(pal, ShawlR, Linen, 3f)
        }
        arm(pal, G.SHOULDER_LX, G.SHOULDER_Y, hLx, hLy, c.sleeve, -1f)
        arm(pal, G.SHOULDER_RX, G.SHOULDER_Y, hRx, hRy, c.sleeve, 1f)
        circ(pal, G.SHOULDER_LX - 1f, G.SHOULDER_Y - 2f, 8f, c.sleeve, 3.5f)
        circ(pal, G.SHOULDER_RX + 1f, G.SHOULDER_Y - 2f, 8f, c.sleeve, 3.5f)
        if (Accessory.ZAPIA in accessories) {
            drawZapia(pal.outline, sway * 2f - fx.hop * 0.7f + fx.dance * 2.5f + tremble * 2f)
        }

        // The basket hangs from her left forearm; when she drops it, it falls to the floor in a small arc.
        val d = p.drop
        val bx = lerp(hLx - 23f, 44f - tremble, d)
        val by = lerp(hLy + 12f, 270f - (bobY + lift), d) - 10f * sin(PI.toFloat() * d)
        basket(pal, bx, by)
        if (Accessory.LANTERN in accessories) {
            val flick = if (idle && p.lampFlicker > 0.02f) {
                1f - p.lampFlicker * 0.55f * (if (sin(t * 23f) > 0.25f) 1f else 0f) * (0.6f + 0.4f * sin(t * 7f))
            } else {
                1f
            }
            val swing = (1f - d) * (10f * sin(fx.swayPhase + 0.9f) - 2f * fx.dance) + tremble * 4f
            drawLantern(pal.outline, bx - 19f - 6f * d, lerp(by - 9f, 257f - (bobY + lift), d), p.lamp * flick, swing)
        }

        // The tossed apple flies from the basket in a high arc into her raised right hand; her eyes follow it.
        var lookX = fx.lookX
        var lookY = fx.lookY
        var caught = 0f
        var flying: Triple<Float, Float, Float>? = null
        if (fx.toss >= 0f) {
            val e = fx.toss
            if (e < 0.75f) {
                val u = e / 0.75f
                val ax = lerp(bx, 134f, u)
                val ay = lerp(by - 14f, 173f, u) - 130f * sin(PI.toFloat() * u)
                flying = Triple(ax, ay, u * 600f)
                // Eased in over the first moment, so the eyes glide to the basket instead of jumping there.
                val w = smooth(0f, 0.08f, e)
                lookX = lerp(lookX, ((ax - G.CX) / 30f).coerceIn(-1f, 1f) * 3f, w)
                lookY = lerp(lookY, ((ay - 160f) / 50f).coerceIn(-1f, 1f) * 3.5f, w)
            } else {
                val back = 1f - smooth(0.75f, 0.95f, e)
                lookX = 3f * back
                lookY = 0.9f * back
                caught = 1f - smooth(0.9f, 1f, e)
            }
        }

        // The collar, then the head, which nods, tilts and leans into a pat about the neck.
        shape(pal, Collar, Linen, 3.5f)
        withTransform({
            translate(0f, p.headY)
            rotate(p.tilt + nod + fx.headLean, Offset(G.NECK_X, G.NECK_Y))
        }) { drawHead(p, fx, pal, accessories, lookX, lookY) }

        // The apple she holds up when happy (or has just caught), then the hands on top.
        val held = max(p.appleUp * (1f - fx.hideHandApple), caught)
        if (held > 0.02f) apple(pal, hRx, hRy - 9f, 0.95f * (0.6f + 0.4f * held), held)
        circ(pal, hLx, hLy, 6.5f, Skin, 3f)
        circ(pal, hRx, hRy, 6.5f, Skin, 3f)
        flying?.let { (ax, ay, spin) -> withTransform({ rotate(spin, Offset(ax, ay)) }) { apple(pal, ax, ay, 0.9f) } }

        // Extras that ride with her: sparkles, a sweat drop, worried amber lines.
        fun tw(q: Float) = if (idle) 0.7f + 0.3f * sin(t * 5f + q) else 1f
        if (p.sparkle > 0.02f) {
            drawPath(starPath(G.CX + G.R + 26f, G.CY - G.R - 2f, 7f * tw(0f)), Spark, alpha = clamp01(p.sparkle))
            drawPath(starPath(G.CX - G.R - 26f, G.CY - 4f, 5f * tw(2f)), Spark, alpha = clamp01(p.sparkle))
        }
        if (p.sweat > 0.02f) {
            val k = if (idle) (t * 0.9f) % 1f else 0.3f
            drop(pal, G.CX + G.R + 14f, G.CY - G.R + 4f + 16f * k, 1f, p.sweat * (1f - smooth(0.7f, 1f, k)))
        }
        if (p.amber > 0.02f) {
            for (ph0 in listOf(0f, 0.5f)) {
                val ph = if (idle) (t * 1.2f + ph0) % 1f else 0.25f + ph0
                val a = p.amber * (1f - smooth(0.6f, 1f, ph))
                val x = G.CX - G.R - 18f - 14f * ph
                val hh = 10f + 8f * ph
                val wave = Path().apply {
                    moveTo(x, G.CY + 6f - hh)
                    quadraticTo(x - 8f, G.CY + 6f, x, G.CY + 6f + hh)
                }
                line(wave, 3.5f, AmberLine, a)
            }
        }
    }

    // Extras that stay put: sound waves, the moon, falling petals and the trick's petal burst.
    if (p.waves > 0.02f) {
        for (ph0 in listOf(0f, 0.5f)) {
            val ph = if (idle) (t * 1.6f + ph0) % 1f else 0.25f + ph0
            val a = p.waves * (1f - smooth(0.6f, 1f, ph))
            val dx = 28f + 14f * ph
            val hh = 14f + 12f * ph
            for (side in listOf(-1f, 1f)) {
                val x = G.CX + side * (G.R + dx)
                val wave = Path().apply {
                    moveTo(x, G.CY + 4f - hh)
                    quadraticTo(x + side * 9f, G.CY + 4f, x, G.CY + 4f + hh)
                }
                line(wave, 3.5f, QB.TooLoud, a)
            }
        }
    }
    if (p.moon > 0.02f) {
        fun tw(q: Float) = if (idle) 0.7f + 0.3f * sin(t * 5f + q) else 1f
        val a = clamp01(p.moon)
        drawPath(Crescent, MoonGold, alpha = a)
        drawPath(Crescent, pal.outline, alpha = a, style = strokeOf(3f))
        drawPath(starPath(MOON_X + 22f, MOON_Y - 18f, 5f * tw(1f)), MoonGold, alpha = a)
        drawPath(starPath(MOON_X - 6f, MOON_Y + 26f, 4f * tw(3f)), MoonGold, alpha = a)
    }
    if (p.petals > 0.02f) {
        val xs = floatArrayOf(32f, 168f, 54f, 150f)
        for (i in 0 until 4) {
            val k = if (idle) (t * 0.22f + i * 0.25f) % 1f else 0.2f + i * 0.2f
            val x = xs[i] + 10f * sin(k * 6f + i)
            val y = 40f + 210f * k
            petal(x, y, k * 360f + i * 70f, p.petals * (1f - smooth(0.8f, 1f, k)) * smooth(0f, 0.1f, k))
        }
    }
    // A ring of apple-blossom petals when she lands the jump or finishes the dance.
    if (fx.burst >= 0f) {
        val bk = fx.burst
        for (i in 0 until 12) {
            val a = i * PI.toFloat() / 6f + 0.2f
            val r = 40f + 80f * bk
            petal(100f + r * cos(a), 200f + r * sin(a) * 0.6f - 30f * bk, a * 57f + bk * 300f, 1f - smooth(0.5f, 1f, bk))
        }
    }
}

/** Hair, face, the tontorra, and what sits on it: the robin on the hook's curl and the eguzkilore on its lowest band. */
private fun DrawScope.drawHead(
    p: MariPose, fx: MariFx, pal: Palette, accessories: Set<Accessory>, lookX: Float, lookY: Float,
) {
    val c = pal.mari
    ell(G.CX, G.CY - 4f, G.R + 7f, G.R + 2f, c.hair, 1f, pal.outline, 4f)
    circ(pal, G.CX, G.CY + 2f, G.R, Skin)
    shape(pal, HairFront, c.hair, 3.5f)
    line(HairShade, 2.5f, c.hairShade)
    for (lock in HairLocks) shape(pal, lock, c.hair, 3.5f)
    drawFace(p, fx, pal, G.CX, G.CY + 4f, G.R, lookX, lookY)

    val wobble = if (fx.idle) p.hookWobble * sin(fx.hookPhase) else 0f
    val baseY = G.CY - G.R * 0.45f
    val hookEnd = drawTontorra(pal, G.CX, baseY, p.lean, p.bend + wobble + fx.boing + fx.bendAdd, max(0.05f, p.curl))
    if (Accessory.ROBIN in accessories) drawRobinOnHook(p, fx, pal, hookEnd)
    if (Accessory.EGUZKILORE in accessories) {
        drawEguzkilore(pal.outline, G.CX - 30f, baseY - 2f, p.bloom, if (fx.idle) 0.12f * sin(fx.hookPhase) else 0f)
    }
}

/**
 * The wrapped tontorra: a low, wide cone of four puffy cloth bands that spiral upwards (each one bulges and pinches
 * where the next starts), ending in a curled hook that bends forward. [lean] tilts the cone, [bend] (degrees, spread
 * over the bands) curls the top forward and [curl] rolls the hook. Returns the centre of the hook's rolled end.
 */
private fun DrawScope.drawTontorra(pal: Palette, cx: Float, baseY: Float, lean: Float, bend: Float, curl: Float): Offset {
    val n = 4
    val seg = 17f
    val w0 = 98f
    val wt = 48f
    val sx = FloatArray(n + 1)
    val sy = FloatArray(n + 1)
    val sa = FloatArray(n + 1)
    val sw = FloatArray(n + 1)
    sx[0] = cx
    sy[0] = baseY
    sa[0] = rad(-90f + lean)
    sw[0] = w0
    for (i in 0 until n) {
        val f = (i + 1f) / n
        val a = rad(-90f + lean + bend * f.pow(2))
        sx[i + 1] = sx[i] + seg * cos(a)
        sy[i + 1] = sy[i] + seg * sin(a)
        sa[i + 1] = a
        sw[i + 1] = w0 - (w0 - wt) * f * f
    }
    // A point along the spine: position, direction and width.
    fun spAt(t: Float): FloatArray {
        val i = min(floor(t).toInt(), n - 1)
        val k = t - i
        return floatArrayOf(
            lerp(sx[i], sx[i + 1], k), lerp(sy[i], sy[i + 1], k), lerp(sa[i], sa[i + 1], k), lerp(sw[i], sw[i + 1], k),
        )
    }
    fun side(t: Float, off: Float, dir: Float): Offset {
        val s = spAt(t)
        val h = s[3] / 2f + off
        return Offset(s[0] + dir * cos(s[2] + PI.toFloat() / 2f) * h, s[1] + dir * sin(s[2] + PI.toFloat() / 2f) * h)
    }
    // Each side bulges out mid-band and pinches in where the next band starts.
    fun edge(dir: Float): List<Offset> = listOf(side(0f, 0f, dir)) +
        (1..2 * n).map { k -> side(min(n.toFloat(), k / 2f), if (k % 2 == 1) 6f else -4f, dir) }
    val lp = edge(-1f)
    val rp = edge(1f)
    val tipA = sa[n]
    val ux = cos(tipA)
    val uy = sin(tipA)
    val vx = cos(tipA + PI.toFloat() / 2f)
    val vy = sin(tipA + PI.toFloat() / 2f)
    fun at(du: Float, dv: Float) = Offset(sx[n] + ux * du + vx * dv, sy[n] + uy * du + vy * dv)
    val hook = listOf(at(-10f, -4f), at(14f, -6f), at(28f, 10f * curl), at(24f - 12f * curl, 28f * curl + 2f))
    val end = hook[3]
    val hookPath = Path().apply { smoothThrough(hook, true) }

    line(hookPath, 36f, pal.outline)
    circ(pal, end.x, end.y, 14f, Linen, 4f)
    line(hookPath, 28f, Linen)
    val top = at(14f, 0f)
    val body = Path().apply {
        smoothThrough(lp, true)
        quadraticTo(top.x, top.y, rp.last().x, rp.last().y)
        smoothThrough(rp.reversed(), false)
        quadraticTo(cx, baseY + 10f, lp[0].x, lp[0].y)
        close()
    }
    drawPath(body, Linen)
    // Each band: a soft shade along its lower edge, then the wrap line (pinch to pinch, rising left to right).
    for (i in 1..n) {
        val a = side(i - 1f, -4f, -1f)
        val b = side(min(n, i).toFloat(), -4f, 1f)
        val m = spAt(i - 0.5f)
        val mu = Offset(cos(m[2]), sin(m[2]))
        val ctl = Offset(m[0], m[1]) - mu * 2f
        if (i > 1) {
            val sh = ctl + mu * 4f
            val a4 = a + mu * 4f
            val b4 = b + mu * 4f
            line(Path().apply { moveTo(a4.x, a4.y); quadraticTo(sh.x, sh.y, b4.x, b4.y) }, 5f, LinenShade, 0.8f)
        }
        if (i in 2 until n) line(Path().apply { moveTo(a.x, a.y); quadraticTo(ctl.x, ctl.y, b.x, b.y) }, 3f, pal.outline)
    }
    drawPath(body, pal.outline, style = Stroke(4f, join = StrokeJoin.Round))
    // The hook's root, filled over the outline so the hook grows out of the top band.
    line(Path().apply { smoothThrough(hook.subList(0, 3), true) }, 27f, Linen)
    drawCircle(Linen, 13f, at(4f, -3f))
    // The curled end rolls into a spiral.
    val spiral = Path().apply { addPath(HookSpiral, end) }
    line(spiral, 3f, pal.outline, 0.7f)
    return end
}

/**
 * The robin perched on the hook's curl. It hops when she's happy, flutters up when it gets loud, flies off when it's
 * too loud (coming back as the room calms) and sleeps with its head tucked in.
 */
private fun DrawScope.drawRobinOnHook(p: MariPose, fx: MariFx, pal: Palette, hookEnd: Offset) {
    val away = smooth(0f, 1f, p.robinAway)
    val t = fx.time
    val flap = if (fx.idle) sin(t * 34f) else 0.4f
    val perchX = hookEnd.x - 1f
    val perchY = hookEnd.y - 25f
    val hop = if (fx.idle) p.robinHop * abs(sin(t * 4.2f)) * 4f else 0f
    val k = away
    val x = lerp(perchX, perchX + 110f, smooth(0.45f, 1f, k)) + 8f * sin(t * 2.3f) * smooth(0.05f, 0.4f, k)
    val y = lerp(perchY - hop, perchY - 34f, smooth(0f, 0.35f, k)) - 90f * smooth(0.45f, 1f, k)
    val flying = k > 0.05f
    val a = 1f - smooth(0.85f, 1f, away)
    if (a < 0.02f) return
    drawRobin(
        pal.outline, x, y,
        dir = if (flying && k > 0.45f) 1f else -1f,
        wing = if (flying) 40f * flap else null,
        sleep = p.robinSleep,
        alpha = a,
    )
}

/** Cheeks, eyes, brows, mouth (or gritted teeth) and the running tear. */
private fun DrawScope.drawFace(
    p: MariPose, fx: MariFx, pal: Palette, cx: Float, cy: Float, r: Float, lookX: Float, lookY: Float,
) {
    val ey = cy + r * 0.14f
    val dx = r * 0.36f
    val my = cy + r * 0.46f
    val t = fx.time
    // Cheeks: a pat deepens and widens the blush.
    if (p.cheek > 0.02f) {
        val k = 1f + 0.3f * max(0f, p.cheek - 1f)
        for (sd in listOf(-1f, 1f)) {
            ell(cx + sd * r * 0.6f, cy + r * 0.38f, r * 0.14f * k, r * 0.09f * k, pal.cheek, 0.75f * min(1.25f, p.cheek))
        }
    }
    val o = p.open * (1f - fx.blink * p.blink)
    for (sd in listOf(-1f, 1f)) {
        val ex = cx + sd * dx + fx.twitch + lookX
        val ey0 = ey + p.look + lookY
        if (o > 0.08f) {
            ell(ex, ey0, r * 0.1f, r * 0.13f * o, pal.outline)
            if (o > 0.5f) ell(ex + r * 0.03f, ey0 - r * 0.05f * o, r * 0.035f, r * 0.035f, Color.White)
        }
        val ca = clamp01(1f - o * 2.5f)
        if (ca > 0.02f) {
            val s = p.closed
            // x mirrors so a squeezed eye points in towards the nose: > <
            val a = Offset(ex - sd * s[0], ey0 + s[1])
            val m = Offset(ex - sd * s[2], ey0 + s[3])
            val b = Offset(ex - sd * s[4], ey0 + s[5])
            val stroke = if (p.eyeSharp > 0.5f) {
                polyline(a.x, a.y, m.x, m.y, b.x, b.y)
            } else {
                val ctl = m * 2f - (a + b) / 2f
                Path().apply { moveTo(a.x, a.y); quadraticTo(ctl.x, ctl.y, b.x, b.y) }
            }
            line(stroke, 4f, pal.outline, ca)
        }
        if (p.browA > 0.02f) {
            val by = ey0 - r * 0.28f
            line(polyline(ex - sd * 8f, by, ex + sd * 6f, by - p.browLift), 3.5f, pal.outline, p.browA)
        }
    }

    // Mouth: one curve for smile or frown, opened into a shape, wobbling when worried.
    val wob = if (fx.idle) sin(t * 7f) else 1f
    val top = ArrayList<Offset>(9)
    val bot = ArrayList<Offset>(9)
    for (i in 0..8) {
        val u = i / 4f - 1f
        val x = cx + 10f * u
        val y = my + 8f * p.smile * (1f - u * u) + p.zig * 2.4f * sin(u * 3f * PI.toFloat()) * (0.7f + 0.3f * wob)
        top.add(Offset(x, y))
        bot.add(Offset(x, y + p.mouthOpen * 12f * (1f - u * u)))
    }
    // Gritted teeth when it's too loud, faded in over the mouth.
    if (p.grit > 0.02f) {
        val w = 22f
        val h = 11f
        val x0 = cx - w / 2f
        val y0 = my - 3f
        val a = clamp01(p.grit)
        val corner = androidx.compose.ui.geometry.CornerRadius(5f)
        drawRoundRect(Color.White, Offset(x0, y0), Size(w, h), corner, alpha = a)
        drawRoundRect(pal.outline, Offset(x0, y0), Size(w, h), corner, alpha = a, style = Stroke(3.5f))
        val teeth = Path().apply {
            moveTo(x0 + 2f, y0 + h / 2f); lineTo(x0 + w - 2f, y0 + h / 2f)
            moveTo(cx - 4f, y0 + 1f); lineTo(cx - 4f, y0 + h - 1f)
            moveTo(cx + 4f, y0 + 1f); lineTo(cx + 4f, y0 + h - 1f)
        }
        line(teeth, 2f, pal.outline, a)
    }
    val mA = 1f - clamp01(p.grit)
    if (mA >= 0.02f) {
        val topLine = Path().apply {
            moveTo(top[0].x, top[0].y)
            for (i in 1 until top.size) lineTo(top[i].x, top[i].y)
        }
        if (p.mouthOpen > 0.05f) {
            val inside = Path().apply {
                moveTo(top[0].x, top[0].y)
                for (i in 1 until top.size) lineTo(top[i].x, top[i].y)
                for (i in bot.indices.reversed()) lineTo(bot[i].x, bot[i].y)
                close()
            }
            drawPath(inside, pal.mouth, alpha = mA)
            if (p.mouthOpen > 0.45f && p.smile > 0.3f) {
                ell(cx, my + 8f * p.smile + p.mouthOpen * 7f, 5f * p.mouthOpen, 2.6f * p.mouthOpen, Tongue)
            }
        }
        line(topLine, 3.5f, pal.outline, mA)
        if (p.mouthOpen > 0.05f) {
            val botLine = Path().apply {
                moveTo(bot[0].x, bot[0].y)
                for (i in 1 until bot.size) lineTo(bot[i].x, bot[i].y)
            }
            line(botLine, 3.5f, pal.outline, mA)
        }
    }
    // A tear runs down the cheek and drips, again and again.
    if (p.tear > 0.02f) {
        val k = if (fx.idle) (t * 0.7f) % 1f else 0.2f
        drop(pal, cx - dx - 2f, ey + 12f + 14f * k, 0.9f, p.tear * (1f - smooth(0.75f, 1f, k)))
    }
}
