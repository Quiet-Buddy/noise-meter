package com.quietbuddy.ui.character.robot

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.quietbuddy.core.Accessory
import com.quietbuddy.ui.character.drawBowTie
import com.quietbuddy.ui.character.drawPlanetRing
import com.quietbuddy.ui.character.drawPropellerCap
import com.quietbuddy.ui.character.drawStarGlasses
import com.quietbuddy.ui.theme.Palette
import com.quietbuddy.ui.theme.QB
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import com.quietbuddy.ui.character.robot.RobotShapes as G

// Astro-only colours that never change with the style; the style's own come from palette.robot.
private val ScreenOff = Color(0xFF141B26)
private val HeartRed = Color(0xFFFF6B8B)
private val Spark = Color(0xFFFFE36B)
private val Charge = Color(0xFF4CC38A)
private val BulbHousing = Color(0xFFC4CCD8)

private fun clamp01(x: Float) = x.coerceIn(0f, 1f)
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun smooth(a: Float, b: Float, x: Float): Float {
    val t = clamp01((x - a) / (b - a))
    return t * t * (3f - 2f * t)
}

/** Linear mix in sRGB, like the prototype (Compose's own lerp works in another colour space). */
private fun mix(a: Color, b: Color, t: Float) =
    Color(lerp(a.red, b.red, t), lerp(a.green, b.green, t), lerp(a.blue, b.blue, t), lerp(a.alpha, b.alpha, t))

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

/** `M p0 Q p1 p2` from six numbers. */
private fun q1(p: List<Float>) = Path().apply {
    moveTo(p[0], p[1])
    quadraticTo(p[2], p[3], p[4], p[5])
}

private fun line(vararg v: Float) = Path().apply {
    moveTo(v[0], v[1])
    for (i in 2 until v.size step 2) lineTo(v[i], v[i + 1])
}

private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float) = Path().apply {
    addRoundRect(RoundRect(x, y, x + w, y + h, CornerRadius(r)))
}

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
    if (w > 0f) outlined(pal, p, pal.tear, w, a) else fill(p, pal.tear, a)
}

private fun heartPath(x: Float, y: Float, s: Float) = Path().apply {
    moveTo(x, y + s * 0.9f)
    cubicTo(x - s * 1.3f, y + s * 0.1f, x - s * 0.6f, y - s * 0.8f, x, y - s * 0.25f)
    cubicTo(x + s * 0.6f, y - s * 0.8f, x + s * 1.3f, y + s * 0.1f, x, y + s * 0.9f)
    close()
}

private val Screen = rrect(G.SCREEN_X, G.SCREEN_Y, G.SCREEN_W, G.SCREEN_H, G.SCREEN_R)
private val OrbClip = Path().apply {
    addOval(Rect(Offset(G.ORB_CX, G.ORB_CY), G.ORB_R - 1.5f))
}
private val Nozzle = rrect(88f, 140f, 24f, 11f, 5f)
private val Seam = Path().apply {
    moveTo(52f, 124f)
    quadraticTo(100f, 146f, 148f, 124f)
}
private val Gloss = Path().apply {
    moveTo(66f, 70f)
    quadraticTo(72f, 64f, 84f, 64f)
    lineTo(74f, 80f)
    quadraticTo(68f, 78f, 66f, 70f)
    close()
}
private val BatteryBody = rrect(140f, 24f, 24f, 13f, 3f)
private val BatteryNub = rrect(164.5f, 28f, 3f, 5f, 1f)
private val BatteryBolt = line(153f, 26f, 149f, 31f, 154f, 31f, 150f, 36f)
private val Shock = Path().apply {
    moveTo(82f, 28f); lineTo(74f, 20f)
    moveTo(118f, 28f); lineTo(126f, 20f)
    moveTo(78f, 40f); lineTo(68f, 38f)
    moveTo(122f, 40f); lineTo(132f, 38f)
}

/** The back of the orb, seen only while spinning: a hatch with four screws, vent slots and a star sticker. */
private fun DrawScope.backHatch(pal: Palette) {
    val c = pal.robot
    outlined(pal, rrect(70f, 64f, 60f, 46f, 12f), c.shade, 3.5f)
    for ((x, y) in listOf(77f to 71f, 123f to 71f, 77f to 103f, 123f to 103f)) oval(x, y, 2.4f, 2.4f, c.dark)
    val vents = Path().apply {
        moveTo(84f, 78f); lineTo(116f, 78f)
        moveTo(84f, 86f); lineTo(116f, 86f)
        moveTo(84f, 94f); lineTo(116f, 94f)
    }
    ink(vents, c.dark, 3f)
    outlined(pal, starPath(100f, 124f, 7f, 3.4f), Spark, 2.4f)
}

private fun starPath(cx: Float, cy: Float, bigR: Float, smallR: Float) = Path().apply {
    for (i in 0 until 10) {
        val a = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
        val rad = if (i % 2 == 1) smallR else bigR
        val x = cx + rad * cos(a)
        val y = cy + rad * sin(a)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** A point turned around the orb's vertical axis: x across and how far it is towards the viewer. */
private class Turn(yawDeg: Float) {
    private val yaw = yawDeg * PI.toFloat() / 180f
    private val c = cos(yaw)
    private val s = sin(yaw)
    val spinning = abs(yawDeg) > 0.01f

    fun x(dx: Float, dz: Float) = G.ORB_CX + dx * c + dz * s
    fun z(dx: Float, dz: Float) = -dx * s + dz * c
}

/** Where the propeller cap sits on the sphere: an offset from the orb centre, towards the viewer, and its lean. */
private const val CAP_DX = -24.9f
private const val CAP_Y = 47.2f
private const val CAP_DZ = 18.1f
private const val CAP_ANGLE = -28f

private fun bulbColor(rgb: List<Float>) = Color(rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f)

/**
 * Draws Astro at 200x200 design-space scale, in the prototype's coordinates. The prototype's viewBox adds a margin
 * around that square; like the duck, Astro is drawn full size and the few things that reach past the edge (the
 * antenna light during a hop, sound waves, a lap's far swing) overflow, since the canvas doesn't clip.
 *
 * The pat's squash and hearts are drawn outside the art (BuddyTapEffects), so only the heart eyes, the antenna
 * boing and the combo moves (spin, hop, sparkle burst, lap) are here.
 */
fun DrawScope.drawRobot(pose: RobotPose, fx: RobotFx, pal: Palette, accessories: Set<Accessory> = emptySet()) {
    val p = pose.patted(fx.patFace)
    if (fx.orbit != null) {
        // The trail dots stay behind in room space; Astro itself is placed on the lap's circle.
        for (d in fx.trail) oval(d.x, d.y, d.radius, d.radius, pal.robot.glow, d.alpha)
        val k = fx.lapScale
        withTransform({
            translate(fx.lapX, fx.lapY)
            translate(100f, G.FLOOR)
            scale(k, k, Offset.Zero)
            translate(-100f, -G.FLOOR)
        }) { drawAstro(p, fx, pal, accessories) }
    } else {
        drawAstro(p, fx, pal, accessories)
    }
    // The trick ends in a ring of sparkles flying out from the orb.
    if (fx.burst >= 0f) {
        val bk = fx.burst
        for (i in 0 until 8) {
            val a = i * PI.toFloat() / 4f + 0.3f
            val r = 50f + 46f * bk
            sparkle(pal, 100f + r * cos(a), 80f + r * sin(a) * 0.8f, 7f * (1f - bk * 0.5f), 1f - smooth(0.5f, 1f, bk))
        }
    }
}

/** The floor shadow, the thruster's glow on it, and Astro hovering above. */
private fun DrawScope.drawAstro(p: RobotPose, fx: RobotFx, pal: Palette, accessories: Set<Accessory>) {
    val t = fx.time
    val idle = fx.idle
    val tau = PI.toFloat() * 2f
    val hoverY = if (idle) p.hover * sin(t * tau * p.hoverHz) else 0f
    val rock = if (idle) p.sway * sin(t * tau * p.swayHz) else 0f
    val tremble = if (idle) p.shake * 2.5f * sin(t * tau / 0.12f) else 0f
    val th = p.thrust + fx.boost
    // Breathing is weighted by the pose, so it eases in and out with it.
    val breath = if (idle) 1f + 0.02f * p.breathe * (0.5f - 0.5f * cos(t * PI.toFloat())) else 1f

    // Floor: a shadow that grows as Astro sinks, and the thruster's glow on the floor.
    val near = clamp01((p.lift + hoverY + 6f) / 40f)
    oval(100f, G.FLOOR + 2f, 30f + 16f * near, 5f + 1.5f * near, pal.outline, 0.08f + 0.08f * near)
    if (th > 0.02f) {
        oval(100f, G.FLOOR + 2f, 24f + 8f * th, 4f, pal.robot.glow, 0.45f * th * (1f - near * 0.5f))
    }

    withTransform({
        translate(tremble, hoverY + p.lift + fx.hop)
        rotate(fx.bank, Offset(100f, 150f))
        rotate(rock, Offset(100f, 150f))
        scale(breath, breath, Offset(100f, 150f))
    }) { drawBody(p, fx, pal, accessories, th) }
}

private class HandDot(val x: Float, val y: Float, val r: Float)

private fun DrawScope.drawBody(p: RobotPose, fx: RobotFx, pal: Palette, accessories: Set<Accessory>, th: Float) {
    val c = pal.robot
    val t = fx.time
    val idle = fx.idle
    val tau = PI.toFloat() * 2f
    val turn = Turn(fx.yaw)
    val spinning = turn.spinning

    // Thruster flame under the nozzle; it flickers.
    if (th > 0.02f) {
        val fl = if (idle) 0.85f + 0.15f * sin(t * 31f) else 1f
        val len = 26f * th * fl
        val flame = Path().apply {
            moveTo(90f, 149f); quadraticTo(100f, 149f + len * 1.25f, 110f, 149f); close()
        }
        val core = Path().apply {
            moveTo(95f, 149f); quadraticTo(100f, 149f + len * 0.7f, 105f, 149f); close()
        }
        fill(flame, c.glow, 0.8f * clamp01(th * 1.4f))
        fill(core, Color.White, 0.85f * clamp01(th * 1.4f))
    }
    outlined(pal, Nozzle, c.dark, 3.5f)

    // Floating hands: they hover beside the orb, and in "too loud" clamp over the ear discs. While spinning they
    // swing around with the orb, passing behind it.
    val cover = smooth(0f, 1f, p.cover)
    val handsBack = ArrayList<HandDot>()
    val handsFront = ArrayList<HandDot>()
    G.Hands.forEachIndexed { i, (hx, hy) ->
        val side = if (i == 0) 1f else -1f
        val wig = if (idle) p.handWiggle * sin(t * tau * p.handHz + i * PI.toFloat() * 0.8f) else 0f
        val restX = hx + side * p.handIn
        val restY = hy + p.handLift + wig
        val ear = G.Ears[i]
        var x = lerp(restX, ear.first - side * 2f, cover)
        val y = lerp(restY, ear.second, cover)
        var r = lerp(G.HAND_R, G.COVER_R, cover)
        var z = 0f
        if (spinning) {
            val dx = x - G.ORB_CX
            x = turn.x(dx, 0f)
            z = turn.z(dx, 0f)
            r *= 1f + 0.15f * z / 70f
        }
        (if (z < -1f) handsBack else handsFront).add(HandDot(x, y, r))
    }
    fun DrawScope.hand(h: HandDot) {
        oval(h.x, h.y, h.r, h.r, c.shade, 1f, pal.outline, 3.6f)
        oval(h.x - h.r * 0.3f, h.y - h.r * 0.35f, h.r * 0.32f, h.r * 0.22f, Color.White, 0.6f)
    }
    handsBack.forEach { hand(it) }

    // The orb and everything on it tilts as one.
    val tilt = p.tilt + (if (idle) p.nod * sin(t * tau * p.nodHz) else 0f) + fx.twitch * 0.4f
    withTransform({ rotate(tilt, Offset(G.ORB_CX, G.ORB_CY)) }) {
        drawOrb(p, fx, pal, accessories, th, turn)
    }
    handsFront.forEach { hand(it) }

    // Extras.
    if (p.sweat > 0.02f) drop(pal, 52f, 34f, 13f - 2f * p.shake, p.sweat)
    if (p.sparkle > 0.02f) {
        fun tw(k: Float) = if (idle) 0.75f + 0.25f * sin(t * 5f + k) else 1f
        sparkle(pal, 20f, 50f, 8f * tw(0f), p.sparkle)
        sparkle(pal, 178f, 30f, 6f * tw(2f), p.sparkle)
        sparkle(pal, 186f, 96f, 4f * tw(4f), p.sparkle)
    }
    // Sound waves spreading out on both sides while the hands are over the ears.
    if (p.waves > 0.02f) {
        for (ph0 in listOf(0f, 0.5f)) {
            val ph = if (idle) (t * 1.6f + ph0) % 1f else 0.25f + ph0
            val a = p.waves * (1f - smooth(0.6f, 1f, ph))
            val dx = 22f + 14f * ph
            val hh = 14f + 12f * ph
            for (side in listOf(-1f, 1f)) {
                val x = 100f + side * (56f + dx)
                val wave = Path().apply {
                    moveTo(x, 94f - hh)
                    quadraticTo(x + side * 9f, 94f, x, 94f + hh)
                }
                ink(wave, QB.TooLoud, 3.5f, a)
            }
        }
    }
    if (p.shock > 0.02f) {
        val fl = if (idle) 0.7f + 0.3f * sin(t * 22f) else 1f
        ink(Shock, QB.TooLoud, 4f, p.shock * fl)
    }
}

/** The shell with antenna, ears, screen face, accessories and battery, in the orb's own (untilted) space. */
private fun DrawScope.drawOrb(
    p: RobotPose, fx: RobotFx, pal: Palette, accessories: Set<Accessory>, th: Float, turn: Turn,
) {
    val c = pal.robot
    val t = fx.time
    val idle = fx.idle
    val tau = PI.toFloat() * 2f
    val spinning = turn.spinning
    val faceA = 1f - 0.55f * p.dim
    val ring = Accessory.PLANET_RING in accessories
    val ringAt = Offset(G.ORB_CX, G.ORB_CY + 38f)

    if (ring) drawPlanetRing(pal.outline, front = false, at = ringAt)

    // Antenna: rooted at the top of the shell, it bends and wobbles, with the mood light on top.
    val aDeg = p.antennaBend + (if (idle) p.antennaWobble * sin(t * tau * p.antennaHz) else 0f) + fx.boing
    val aRad = aDeg * PI.toFloat() / 180f
    val baseX = 100f
    val baseY = 40f
    val len = 18f
    val tipX = baseX + len * sin(aRad)
    val tipY = baseY - len * cos(aRad)
    ink(
        Path().apply {
            moveTo(baseX, baseY)
            quadraticTo(baseX + len * 0.25f * sin(aRad), baseY - len * 0.6f, tipX, tipY)
        },
        pal.outline, 4f,
    )
    val flick = if (idle && p.flicker > 0.02f) 1f - p.flicker * 0.6f * (if (sin(t * 17f) > 0.3f) 1f else 0f) else 1f
    val bulb = bulbColor(p.bulb)
    if (p.bulbGlow > 0.02f) oval(tipX, tipY, 12f, 12f, bulb, 0.35f * p.bulbGlow * flick)
    oval(tipX, tipY, 6.5f, 6.5f, mix(bulb, BulbHousing, 1f - flick), 1f, pal.outline, 3.5f)
    oval(tipX - 2f, tipY - 2f, 1.8f, 1.8f, Color.White, 0.4f + 0.5f * p.bulbGlow)

    // Ear discs sit on the sides of the shell; turned towards the viewer they widen into full discs.
    val earsFront = ArrayList<FloatArray>()
    for ((ex0, ey) in G.Ears) {
        val x = if (spinning) turn.x(ex0 - G.ORB_CX, 0f) else ex0
        val z = if (spinning) turn.z(ex0 - G.ORB_CX, 0f) else 0f
        val k = abs(z) / G.ORB_R
        if (z > 2f) earsFront.add(floatArrayOf(x, ey, k)) else ear(pal, x, ey, k)
    }
    var capX = 0f
    var capFront = true
    val cap = Accessory.PROPELLER_CAP in accessories
    val capSpin = if (idle) t * (4f + 14f * th) else 0.6f
    if (cap) {
        val cx = if (spinning) turn.x(CAP_DX, CAP_DZ) else G.ORB_CX + CAP_DX
        val cz = if (spinning) turn.z(CAP_DX, CAP_DZ) else CAP_DZ
        capX = cx
        capFront = cz > -10f
        if (!capFront) drawCap(pal, capX, capSpin)
    }

    // The shell: a sphere shaded towards the bottom right, a seam below the screen, a highlight top left.
    val shell = Brush.radialGradient(
        0.55f to c.shell, 1f to c.shade,
        center = Offset(44f + 0.38f * 112f, 38f + 0.32f * 112f), radius = 0.75f * 112f,
    )
    drawCircle(shell, G.ORB_R, Offset(G.ORB_CX, G.ORB_CY))
    drawCircle(pal.outline, G.ORB_R, Offset(G.ORB_CX, G.ORB_CY), style = strokeOf(4f))
    ink(Seam, c.shade, 2.4f)
    for (side in listOf(-1f, 1f)) {
        val x = if (spinning) turn.x(side * 44f, 34.6f) else G.ORB_CX + side * 44f
        val z = if (spinning) turn.z(side * 44f, 34.6f) else 1f
        if (z > 0f) oval(x, 126f, 2f, 2f, c.dark)
    }
    // The light comes from the top left whichever way the orb faces, so the highlight stays put.
    withTransform({ rotate(-28f, Offset(74f, 54f)) }) { oval(74f, 54f, 14f, 6.5f, Color.White, 0.75f) }
    oval(60f, 70f, 2.6f, 2.6f, Color.White, 0.7f)

    // The visor screen and the glowing face on it.
    fun DrawScope.front() {
        drawScreen(p, fx, pal, accessories, faceA)
        if (Accessory.BOW_TIE in accessories) drawBowTie(pal.outline)
    }
    if (!spinning) {
        front()
    } else {
        // The front slides around the sphere, narrowing towards the edge; the back hatch comes round behind it.
        val r = G.ORB_R - 4f
        val fx0 = turn.x(0f, r)
        val fz = turn.z(0f, r)
        val bx0 = turn.x(0f, -r)
        val bz = turn.z(0f, -r)
        fun DrawScope.squeezed(x: Float, k: Float, block: DrawScope.() -> Unit) = clipPath(OrbClip) {
            withTransform({
                translate(x, 0f)
                scale(k, 1f, Offset.Zero)
                translate(-G.ORB_CX, 0f)
            }) { block() }
        }
        if (fz > 0.5f) squeezed(fx0, fz / r) { front() }
        if (bz > 0.5f) squeezed(bx0, bz / r) { backHatch(pal) }
    }
    for (e in earsFront) ear(pal, e[0], e[1], e[2])
    if (cap && capFront) drawCap(pal, capX, capSpin)
    if (ring) drawPlanetRing(pal.outline, front = true, at = ringAt)

    // The battery charges while asleep, in place of Z's.
    if (p.battery > 0.02f) {
        val a = p.battery
        val charge = if (idle) floor((t * 1.2f) % 4f) / 3f else 1f
        outlined(pal, BatteryBody, Color.White, 2.8f, a)
        fill(BatteryNub, pal.outline, a)
        if (charge > 0f) fill(rrect(143f, 27f, 18f * charge, 7f, 1.5f), Charge, a)
        ink(BatteryBolt, pal.outline, 1.8f, 0.8f * a)
    }
}

private fun DrawScope.ear(pal: Palette, x: Float, y: Float, k: Float) {
    oval(x, y, 8f + 7f * k, 15f, pal.robot.accent, 1f, pal.outline, 4f)
    oval(x, y, 3f + 4f * k, 8f, Color.White, 0.35f)
}

private fun DrawScope.drawCap(pal: Palette, x: Float, spin: Float) {
    val angle = CAP_ANGLE * (x - G.ORB_CX) / CAP_DX
    drawPropellerCap(pal.outline, Offset(x, CAP_Y), angle, spin)
}

/** The screen itself with the face on it: cheeks, brows, eyes, tears and mouth, clipped to the glass. */
private fun DrawScope.drawScreen(p: RobotPose, fx: RobotFx, pal: Palette, accessories: Set<Accessory>, faceA: Float) {
    val c = pal.robot
    val t = fx.time
    val idle = fx.idle
    outlined(pal, Screen, mix(c.screen, ScreenOff, p.dim), 4f)
    clipPath(Screen) {
        fill(Gloss, Color.White, 0.12f * (1f - p.dim * 0.6f))
        // Cheek lights.
        for (x in listOf(67f, 133f)) oval(x, 101f, 6.5f, 3.8f, pal.cheek, 0.85f * p.cheek * faceA)
        // Brows.
        if (p.browA > 0.02f) {
            ink(q1(p.browL), c.glow, 3.2f, p.browA * faceA)
            ink(q1(p.browR), c.glow, 3.2f, p.browA * faceA)
        }
        // Eyes: an open glowing oval, crossfading to the closed stroke for blinks and closed-eye moods, or hearts
        // after a pat.
        val effOpen = p.open * (1f - p.blink * fx.blink)
        val closedA = 1f - smooth(0.08f, 0.4f, effOpen)
        val shape = if (p.open > 0.5f) G.ClosedSleep else p.closed
        val eyeA = 1f - smooth(0.2f, 0.7f, p.heart)
        val glance = fx.twitch * 0.5f
        for (e in G.Eyes) {
            val ex = e.cx + glance
            val ey = e.cy
            val rx = G.EYE_RX * p.eyeScale
            val ry = G.EYE_RY * p.eyeScale
            if (effOpen > 0.03f) {
                val oy = ry * effOpen
                val ha = smooth(0.45f, 0.8f, effOpen)
                oval(ex, ey, rx + 3f, oy + 3f, c.glow, 0.22f * faceA * eyeA)
                oval(ex, ey, rx, oy, c.glow, faceA * eyeA)
                oval(ex - rx * 0.32f, ey - oy * 0.38f, rx * 0.3f, oy * 0.24f, Color.White, ha * 0.9f * eyeA)
            }
            if (closedA > 0.02f) {
                val s = List(6) { j -> if (j % 2 == 0) ex + shape[j] * (rx + 1f) * e.dir else ey + shape[j] }
                ink(q1(s), c.glow, 4.5f, closedA * faceA * eyeA)
            }
            if (p.heart > 0.02f) {
                fill(heartPath(ex, ey - 1f, 8.5f + 1.5f * fx.heartPulse), HeartRed, smooth(0.2f, 0.7f, p.heart))
            }
        }
        // Tears run down the screen from under the left eye.
        if (p.tear > 0.02f) {
            val ph = if (idle) (t * 0.55f) % 1f else 0.3f
            drop(pal, 75f, 92f + ph * 16f, 9f, p.tear * (1f - smooth(0.75f, 1f, ph)), 0f)
        }
        drawMouth(p, fx, c.glow, faceA)
        if (Accessory.STAR_GLASSES in accessories) {
            drawStarGlasses(pal.outline, G.Eyes.map { Offset(it.cx, it.cy) })
        }
    }
}

/** A glowing line sampled across its width: smile bends it, zig turns it into static, mouthOpen fills it. */
private fun DrawScope.drawMouth(p: RobotPose, fx: RobotFx, glow: Color, faceA: Float) {
    val n = 9
    val top = ArrayList<Offset>()
    val bot = ArrayList<Offset>()
    for (i in 0 until n) {
        val u = i / (n - 1f) * 2f - 1f
        val x = G.MOUTH_CX + u * G.MOUTH_HALF * (1f - 0.2f * max(0f, -p.smile))
        val bend = 6f * p.smile * (1f - u * u)
        val z = p.zig * (if (i % 2 == 1) -3f else 3f) * (if (fx.idle) (if (sin(fx.time * 40f + i) > 0f) 1f else 0.6f) else 1f)
        val y = G.MOUTH_CY + bend - 2f * p.smile + z
        top.add(Offset(x, y))
        bot.add(Offset(x, y + p.mouthOpen * 9f * (1f - u * u)))
    }
    val topLine = Path().apply {
        moveTo(top[0].x, top[0].y)
        for (i in 1 until n) lineTo(top[i].x, top[i].y)
    }
    if (p.mouthOpen > 0.05f) {
        val shape = Path().apply {
            moveTo(top[0].x, top[0].y)
            for (i in 1 until n) lineTo(top[i].x, top[i].y)
            for (i in n - 1 downTo 0) lineTo(bot[i].x, bot[i].y)
            close()
        }
        fill(shape, glow, faceA)
    }
    ink(topLine, glow, 3.5f, faceA)
}
