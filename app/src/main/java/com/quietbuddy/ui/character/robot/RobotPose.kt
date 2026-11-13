package com.quietbuddy.ui.character.robot

import com.quietbuddy.core.Mood

private fun pts(vararg v: Float): List<Float> = v.toList()

private fun rgb(r: Int, g: Int, b: Int): List<Float> = listOf(r.toFloat(), g.toFloat(), b.toFloat())

/** Geometry shared by every pose and the drawing (same counts, so shapes can blend). Ported from docs/design/robot-motion.html. */
internal object RobotShapes {
    /** The orb is a circle; the visor screen is a rounded rectangle on it. The floor is at [FLOOR]. */
    const val ORB_CX = 100f
    const val ORB_CY = 94f
    const val ORB_R = 56f
    const val SCREEN_X = 58f
    const val SCREEN_Y = 62f
    const val SCREEN_W = 84f
    const val SCREEN_H = 52f
    const val SCREEN_R = 24f
    const val FLOOR = 188f

    /** A screen eye: its centre and the direction that mirrors the closed strokes. */
    class Eye(val cx: Float, val cy: Float, val dir: Float)

    val Eyes = listOf(Eye(81f, 86f, 1f), Eye(119f, 86f, -1f))
    const val EYE_RX = 7f
    const val EYE_RY = 9f

    /** The ear discs on the sides of the shell and where the floating hands rest (left, then right). */
    val Ears = listOf(44f to 94f, 156f to 94f)
    val Hands = listOf(30f to 124f, 170f to 124f)
    const val HAND_R = 10f
    const val COVER_R = 15f

    const val MOUTH_CX = 100f
    const val MOUTH_CY = 103f
    const val MOUTH_HALF = 12f

    /** Closed-eye strokes relative to each eye: x in eye half-widths, y in units. The right eye mirrors x. */
    val ClosedHappy = pts(-1.1f, 3f, 0f, -9f, 1.1f, 3f)
    val ClosedSleep = pts(-1.1f, 0f, 0f, 6f, 1.1f, 0f)
    val ClosedSqueeze = pts(-0.85f, -7f, 1.4f, 0f, -0.85f, 7f)

    /** Brows are glowing strokes on the screen, left and right. */
    val BrowNoneL = pts(72f, 72f, 80f, 71f, 89f, 72f)
    val BrowNoneR = pts(111f, 72f, 120f, 71f, 128f, 72f)
    val BrowWorriedL = pts(72f, 74f, 80f, 71f, 89f, 67f)
    val BrowWorriedR = pts(111f, 67f, 120f, 71f, 128f, 74f)
    val BrowSadL = pts(72f, 77f, 80f, 72f, 89f, 66f)
    val BrowSadR = pts(111f, 66f, 120f, 72f, 128f, 77f)

    /** The antenna light colours as r, g, b in 0..255. */
    val BulbCalm = rgb(0xFF, 0xD4, 0x3B)
    val BulbWorried = rgb(0xFF, 0xA6, 0x2B)
    val BulbSad = rgb(0x7F, 0xA8, 0xD8)
    val BulbLoud = rgb(0xFF, 0x5B, 0x4B)
    val BulbOff = rgb(0x8B, 0x97, 0xA8)
}

/**
 * Every number that defines one Astro expression. Pure data (no Compose) so poses can be blended and tested on the
 * JVM. Point lists hold x,y pairs (or r,g,b); a field's lists always have equal lengths.
 */
data class RobotPose(
    // body group
    val hover: Float,
    val hoverHz: Float,
    val lift: Float,
    val thrust: Float,
    val shake: Float,
    val sway: Float,
    val swayHz: Float,
    val handLift: Float,
    val handIn: Float,
    val handWiggle: Float,
    val handHz: Float,
    val cover: Float,
    val breathe: Float,
    // head group (the orb itself and its antenna)
    val tilt: Float,
    val nod: Float,
    val nodHz: Float,
    val twitch: Float,
    val antennaBend: Float,
    val antennaWobble: Float,
    val antennaHz: Float,
    // face group: what the screen shows, and the extras
    val open: Float,
    val eyeScale: Float,
    val closed: List<Float>,
    val blink: Float,
    val browA: Float,
    val browL: List<Float>,
    val browR: List<Float>,
    val smile: Float,
    val mouthOpen: Float,
    val zig: Float,
    val cheek: Float,
    val dim: Float,
    val bulb: List<Float>,
    val bulbGlow: Float,
    val flicker: Float,
    val sparkle: Float,
    val sweat: Float,
    val tear: Float,
    val battery: Float,
    val waves: Float,
    val shock: Float,
    val heart: Float,
) {
    /** The face a pat puts on, blended over this pose by [g] (0 = none, 1 = full): heart eyes and a big smile. */
    fun patted(g: Float): RobotPose = if (g <= 0f) this else copy(
        heart = l(heart, 1f, g),
        open = l(open, 1f, g),
        blink = l(blink, 0f, g),
        smile = l(smile, 1f, g),
        mouthOpen = l(mouthOpen, 0.8f, g),
        cheek = l(cheek, 1f, g),
    )

    companion object {
        /**
         * Blends [a] to [b] with a separate progress per body part, so the body, head and face can move at different
         * moments. Progress 0 gives [a], 1 gives [b].
         */
        fun blend(a: RobotPose, b: RobotPose, tBody: Float, tHead: Float, tFace: Float): RobotPose = RobotPose(
            hover = l(a.hover, b.hover, tBody),
            hoverHz = l(a.hoverHz, b.hoverHz, tBody),
            lift = l(a.lift, b.lift, tBody),
            thrust = l(a.thrust, b.thrust, tBody),
            shake = l(a.shake, b.shake, tBody),
            sway = l(a.sway, b.sway, tBody),
            swayHz = l(a.swayHz, b.swayHz, tBody),
            handLift = l(a.handLift, b.handLift, tBody),
            handIn = l(a.handIn, b.handIn, tBody),
            handWiggle = l(a.handWiggle, b.handWiggle, tBody),
            handHz = l(a.handHz, b.handHz, tBody),
            cover = l(a.cover, b.cover, tBody),
            breathe = l(a.breathe, b.breathe, tBody),
            tilt = l(a.tilt, b.tilt, tHead),
            nod = l(a.nod, b.nod, tHead),
            nodHz = l(a.nodHz, b.nodHz, tHead),
            twitch = l(a.twitch, b.twitch, tHead),
            antennaBend = l(a.antennaBend, b.antennaBend, tHead),
            antennaWobble = l(a.antennaWobble, b.antennaWobble, tHead),
            antennaHz = l(a.antennaHz, b.antennaHz, tHead),
            open = l(a.open, b.open, tFace),
            eyeScale = l(a.eyeScale, b.eyeScale, tFace),
            closed = ls(a.closed, b.closed, tFace),
            blink = l(a.blink, b.blink, tFace),
            browA = l(a.browA, b.browA, tFace),
            browL = ls(a.browL, b.browL, tFace),
            browR = ls(a.browR, b.browR, tFace),
            smile = l(a.smile, b.smile, tFace),
            mouthOpen = l(a.mouthOpen, b.mouthOpen, tFace),
            zig = l(a.zig, b.zig, tFace),
            cheek = l(a.cheek, b.cheek, tFace),
            dim = l(a.dim, b.dim, tFace),
            bulb = ls(a.bulb, b.bulb, tFace),
            bulbGlow = l(a.bulbGlow, b.bulbGlow, tFace),
            flicker = l(a.flicker, b.flicker, tFace),
            sparkle = l(a.sparkle, b.sparkle, tFace),
            sweat = l(a.sweat, b.sweat, tFace),
            tear = l(a.tear, b.tear, tFace),
            battery = l(a.battery, b.battery, tFace),
            waves = l(a.waves, b.waves, tFace),
            shock = l(a.shock, b.shock, tFace),
            heart = l(a.heart, b.heart, tFace),
        )

        /** Uniform blend of every field. */
        fun lerp(a: RobotPose, b: RobotPose, t: Float): RobotPose = blend(a, b, t, t, t)
    }
}

// a*(1-t) + b*t is exact at both ends, unlike a + (b-a)*t.
private fun l(a: Float, b: Float, t: Float) = a * (1f - t) + b * t
private fun ls(a: List<Float>, b: List<Float>, t: Float) = List(b.size) { l(a[it], b[it], t) }

/** The six resting poses. Numbers come straight from the approved prototype. */
object RobotPoses {
    /** Fine is the base the other moods override. */
    private val Fine = RobotPose(
        hover = 3f, hoverHz = 0.45f, lift = 0f, thrust = 0.7f, shake = 0f, sway = 1.5f, swayHz = 0.3f,
        handLift = 0f, handIn = 0f, handWiggle = 1.5f, handHz = 0.7f, cover = 0f, breathe = 0f,
        tilt = 0f, nod = 1.5f, nodHz = 0.4f, twitch = 0.12f, antennaBend = 0f, antennaWobble = 3f, antennaHz = 1.2f,
        open = 1f, eyeScale = 1f, closed = RobotShapes.ClosedSleep, blink = 1f, browA = 0f,
        browL = RobotShapes.BrowNoneL, browR = RobotShapes.BrowNoneR,
        smile = 0.4f, mouthOpen = 0f, zig = 0f, cheek = 0.6f, dim = 0f,
        bulb = RobotShapes.BulbCalm, bulbGlow = 0.8f, flicker = 0f,
        sparkle = 0f, sweat = 0f, tear = 0f, battery = 0f, waves = 0f, shock = 0f, heart = 0f,
    )

    private val Happy = Fine.copy(
        hover = 4.5f, hoverHz = 0.7f, thrust = 1f, sway = 4f, swayHz = 0.6f, handLift = -14f, handWiggle = 7f,
        handHz = 2.2f, nod = 3f, nodHz = 1.2f, antennaWobble = 6f, antennaHz = 2.2f,
        open = 0f, closed = RobotShapes.ClosedHappy, blink = 0f, smile = 1f, mouthOpen = 0.6f, cheek = 1f,
        bulbGlow = 1f, sparkle = 1f,
    )

    private val Worried = Fine.copy(
        hover = 2f, hoverHz = 1.2f, thrust = 0.55f, sway = 1f, swayHz = 1.4f, handLift = 6f, handIn = 12f,
        handWiggle = 2f, handHz = 3f, twitch = 0.6f, antennaWobble = 2f, antennaHz = 4f,
        eyeScale = 0.82f, browA = 1f, browL = RobotShapes.BrowWorriedL, browR = RobotShapes.BrowWorriedR,
        smile = -0.4f, cheek = 0.3f, bulb = RobotShapes.BulbWorried, flicker = 1f, sweat = 1f,
    )

    private val Sad = Fine.copy(
        hover = 1.2f, hoverHz = 0.25f, lift = 10f, thrust = 0.35f, sway = 0.8f, swayHz = 0.2f, handLift = 16f,
        handIn = 6f, handWiggle = 0.5f, handHz = 0.4f, tilt = 8f, nod = 0.8f, nodHz = 0.3f, twitch = 0.03f,
        antennaBend = 38f, antennaWobble = 1f, antennaHz = 0.5f,
        open = 0.72f, eyeScale = 0.92f, browA = 1f, browL = RobotShapes.BrowSadL, browR = RobotShapes.BrowSadR,
        smile = -1f, cheek = 0.25f, dim = 0.25f, bulb = RobotShapes.BulbSad, bulbGlow = 0.4f, tear = 1f,
    )

    private val Covered = Fine.copy(
        hover = 0.6f, hoverHz = 2f, thrust = 0.5f, shake = 1f, sway = 0f, handWiggle = 0f, cover = 1f,
        nod = 0f, twitch = 0f, antennaWobble = 10f, antennaHz = 9f,
        open = 0f, closed = RobotShapes.ClosedSqueeze, blink = 0f, smile = -0.5f, zig = 1f, cheek = 0.4f,
        bulb = RobotShapes.BulbLoud, bulbGlow = 1f, sweat = 1f, waves = 1f, shock = 1f,
    )

    private val Asleep = Fine.copy(
        hover = 0.4f, hoverHz = 0.2f, lift = 34f, thrust = 0f, sway = 0f, handLift = 20f, handIn = -4f,
        handWiggle = 0f, breathe = 1f, tilt = 7f, nod = 0.5f, nodHz = 0.2f, twitch = 0.03f, antennaBend = 22f,
        antennaWobble = 0f, open = 0f, closed = RobotShapes.ClosedSleep, blink = 0f, smile = 0.15f, cheek = 0f,
        dim = 1f, bulb = RobotShapes.BulbOff, bulbGlow = 0f, battery = 1f,
    )

    /** The resting pose for [mood]; null means asleep. */
    fun of(mood: Mood?): RobotPose = when (mood) {
        Mood.HAPPY -> Happy
        Mood.FINE -> Fine
        Mood.WORRIED -> Worried
        Mood.SAD -> Sad
        Mood.EARS_COVERED -> Covered
        null -> Asleep
    }
}
