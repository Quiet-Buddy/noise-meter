package com.quietbuddy.ui.character.mari

import com.quietbuddy.core.Mood

private fun pts(vararg v: Float): List<Float> = v.toList()

/**
 * Geometry shared by every pose and the drawing, in the prototype's own coordinates (docs/design/mari-domingi-motion.html):
 * front view, head circle at ([CX], [CY]) radius [R], neck pivot at [NECK_X], [NECK_Y], feet on the floor at [FEET_Y].
 * MariDraw scales the whole drawing into the app's 200x200 design space.
 */
internal object MariShapes {
    const val CX = 100f
    const val CY = 150f
    const val R = 36f
    const val NECK_X = 100f
    const val NECK_Y = 190f
    const val FEET_X = 100f
    const val FEET_Y = 285f

    /** Shoulders, where the arms start (left, then right). */
    const val SHOULDER_LX = 77f
    const val SHOULDER_RX = 123f
    const val SHOULDER_Y = 204f

    /** Closed-eye strokes relative to each eye: x1,y1, x2,y2, x3,y3; the right eye mirrors x. */
    val ClosedHappy = pts(-7f, 3f, 0f, -6f, 7f, 3f)
    val ClosedSleep = pts(-8f, 0f, 0f, 6f, 8f, 0f)
    val ClosedSqueeze = pts(-6f, -6f, 5f, 0f, -6f, 6f)
}

/**
 * Every number that defines one Mari Domingi expression. Pure data (no Compose) so poses can be blended and tested
 * on the JVM. Fields are grouped like the prototype's BODY_KEYS and HEAD_KEYS; the rest is the face group, which also
 * carries the extras (tear, sweat, sound waves, moon...) and the robin's mood.
 */
data class MariPose(
    // body group: the whole-body rhythms, the hands, the basket and the lantern
    val lamp: Float,
    val lampFlicker: Float,
    val bob: Float,
    val bobHz: Float,
    val sink: Float,
    val sway: Float,
    val swayHz: Float,
    val shake: Float,
    val breathe: Float,
    val hLx: Float,
    val hLy: Float,
    val hRx: Float,
    val hRy: Float,
    val handWiggle: Float,
    val drop: Float,
    val roll: Float,
    val appleUp: Float,
    // head group: the head and the tontorra on it (lean tilts the cone, bend curls the top forward, curl rolls the hook)
    val bloom: Float,
    val headY: Float,
    val tilt: Float,
    val nod: Float,
    val nodHz: Float,
    val twitch: Float,
    val lean: Float,
    val bend: Float,
    val curl: Float,
    val hookWobble: Float,
    val hookHz: Float,
    // face group and extras
    val robinAway: Float,
    val robinHop: Float,
    val robinSleep: Float,
    val open: Float,
    val closed: List<Float>,
    val eyeSharp: Float,
    val blink: Float,
    val look: Float,
    val browA: Float,
    val browLift: Float,
    val smile: Float,
    val mouthOpen: Float,
    val zig: Float,
    val grit: Float,
    val cheek: Float,
    val tear: Float,
    val sweat: Float,
    val sparkle: Float,
    val petals: Float,
    val amber: Float,
    val waves: Float,
    val moon: Float,
) {
    /** The face a pat puts on, blended over this pose by [g] (0 = none, 1 = full): eyes shut with delight, a deep blush. */
    fun patted(g: Float): MariPose = if (g <= 0f) this else copy(
        open = l(open, 0f, g),
        closed = ls(closed, MariShapes.ClosedHappy, g),
        eyeSharp = l(eyeSharp, 0f, g),
        blink = l(blink, 0f, g),
        smile = l(smile, 1f, g),
        mouthOpen = l(mouthOpen, 0.45f, g),
        cheek = l(cheek, 1.35f, g),
    )

    /** The small open smile she wears while watching a tossed apple, blended by [g]. */
    fun watching(g: Float): MariPose = if (g <= 0f) this else copy(
        smile = l(smile, 0.5f, g),
        mouthOpen = l(mouthOpen, 0.35f, g),
    )

    companion object {
        /**
         * Blends [a] to [b] with a separate progress per body part, so the body, head and face can move at different
         * moments. Progress 0 gives [a], 1 gives [b].
         */
        fun blend(a: MariPose, b: MariPose, tBody: Float, tHead: Float, tFace: Float): MariPose = MariPose(
            lamp = l(a.lamp, b.lamp, tBody),
            lampFlicker = l(a.lampFlicker, b.lampFlicker, tBody),
            bob = l(a.bob, b.bob, tBody),
            bobHz = l(a.bobHz, b.bobHz, tBody),
            sink = l(a.sink, b.sink, tBody),
            sway = l(a.sway, b.sway, tBody),
            swayHz = l(a.swayHz, b.swayHz, tBody),
            shake = l(a.shake, b.shake, tBody),
            breathe = l(a.breathe, b.breathe, tBody),
            hLx = l(a.hLx, b.hLx, tBody),
            hLy = l(a.hLy, b.hLy, tBody),
            hRx = l(a.hRx, b.hRx, tBody),
            hRy = l(a.hRy, b.hRy, tBody),
            handWiggle = l(a.handWiggle, b.handWiggle, tBody),
            drop = l(a.drop, b.drop, tBody),
            roll = l(a.roll, b.roll, tBody),
            appleUp = l(a.appleUp, b.appleUp, tBody),
            bloom = l(a.bloom, b.bloom, tHead),
            headY = l(a.headY, b.headY, tHead),
            tilt = l(a.tilt, b.tilt, tHead),
            nod = l(a.nod, b.nod, tHead),
            nodHz = l(a.nodHz, b.nodHz, tHead),
            twitch = l(a.twitch, b.twitch, tHead),
            lean = l(a.lean, b.lean, tHead),
            bend = l(a.bend, b.bend, tHead),
            curl = l(a.curl, b.curl, tHead),
            hookWobble = l(a.hookWobble, b.hookWobble, tHead),
            hookHz = l(a.hookHz, b.hookHz, tHead),
            robinAway = l(a.robinAway, b.robinAway, tFace),
            robinHop = l(a.robinHop, b.robinHop, tFace),
            robinSleep = l(a.robinSleep, b.robinSleep, tFace),
            open = l(a.open, b.open, tFace),
            closed = ls(a.closed, b.closed, tFace),
            eyeSharp = l(a.eyeSharp, b.eyeSharp, tFace),
            blink = l(a.blink, b.blink, tFace),
            look = l(a.look, b.look, tFace),
            browA = l(a.browA, b.browA, tFace),
            browLift = l(a.browLift, b.browLift, tFace),
            smile = l(a.smile, b.smile, tFace),
            mouthOpen = l(a.mouthOpen, b.mouthOpen, tFace),
            zig = l(a.zig, b.zig, tFace),
            grit = l(a.grit, b.grit, tFace),
            cheek = l(a.cheek, b.cheek, tFace),
            tear = l(a.tear, b.tear, tFace),
            sweat = l(a.sweat, b.sweat, tFace),
            sparkle = l(a.sparkle, b.sparkle, tFace),
            petals = l(a.petals, b.petals, tFace),
            amber = l(a.amber, b.amber, tFace),
            waves = l(a.waves, b.waves, tFace),
            moon = l(a.moon, b.moon, tFace),
        )

        /** Uniform blend of every field. */
        fun lerp(a: MariPose, b: MariPose, t: Float): MariPose = blend(a, b, t, t, t)
    }
}

// a*(1-t) + b*t is exact at both ends, unlike a + (b-a)*t.
private fun l(a: Float, b: Float, t: Float) = a * (1f - t) + b * t
private fun ls(a: List<Float>, b: List<Float>, t: Float) = List(b.size) { l(a[it], b[it], t) }

/** The six resting poses. Numbers come straight from the approved prototype. */
object MariPoses {
    /** Fine is the base the other moods override. */
    private val Fine = MariPose(
        lamp = 0.5f, lampFlicker = 0f, bob = 1.5f, bobHz = 0.5f, sink = 0f, sway = 1.2f, swayHz = 0.35f, shake = 0f,
        breathe = 0f, hLx = 95f, hLy = 220f, hRx = 105f, hRy = 220f, handWiggle = 0f, drop = 0f, roll = 0f, appleUp = 0f,
        bloom = 1f, headY = 0f, tilt = 0f, nod = 1.2f, nodHz = 0.4f, twitch = 0.1f,
        lean = 2f, bend = 14f, curl = 1f, hookWobble = 3f, hookHz = 0.8f,
        robinAway = 0f, robinHop = 0f, robinSleep = 0f,
        open = 1f, closed = MariShapes.ClosedSleep, eyeSharp = 0f, blink = 1f, look = 0f, browA = 0f, browLift = 0f,
        smile = 0.6f, mouthOpen = 0f, zig = 0f, grit = 0f, cheek = 0.7f,
        tear = 0f, sweat = 0f, sparkle = 0.4f, petals = 0f, amber = 0f, waves = 0f, moon = 0f,
    )

    private val Happy = Fine.copy(
        robinHop = 1f, lamp = 0.65f, bloom = 1.25f, bob = 3f, bobHz = 0.9f, sway = 3f, swayHz = 0.7f,
        hRx = 139f, hRy = 180f, handWiggle = 3f, appleUp = 1f,
        nod = 2.5f, nodHz = 1.1f, lean = 4f, bend = 22f, curl = 1.2f, hookWobble = 6f, hookHz = 1.6f,
        open = 0f, closed = MariShapes.ClosedHappy, blink = 0f, smile = 1f, mouthOpen = 0.75f, cheek = 1f,
        sparkle = 1f, petals = 1f,
    )

    private val Worried = Fine.copy(
        robinAway = 0.35f, lampFlicker = 0.4f, bloom = 0.9f, bob = 1f, bobHz = 1.3f, sway = 0.8f, swayHz = 1.4f,
        hRx = 125f, hRy = 172f, handWiggle = 1.2f,
        twitch = 0.6f, lean = 0f, bend = 6f, curl = 0.6f, hookWobble = 2.5f, hookHz = 3f,
        open = 0.9f, browA = 1f, browLift = 4f, smile = -0.2f, zig = 1f, cheek = 0.5f, sparkle = 0f, sweat = 1f,
        amber = 1f,
    )

    private val Sad = Fine.copy(
        lamp = 0.35f, bloom = 0.6f, bob = 0.6f, bobHz = 0.25f, sink = 3f, sway = 0.5f, swayHz = 0.2f,
        hLx = 93f, hLy = 233f, hRx = 107f, hRy = 233f,
        headY = 3f, tilt = 5f, nod = 0.6f, nodHz = 0.3f, twitch = 0.03f, lean = -6f, bend = 70f, curl = 1.4f,
        hookWobble = 1f, hookHz = 0.4f,
        open = 0.85f, look = 2f, browA = 1f, browLift = 6f, smile = -1f, cheek = 0.4f, sparkle = 0f, tear = 1f,
    )

    private val Covered = Fine.copy(
        robinAway = 1f, lamp = 0.7f, lampFlicker = 1f, bloom = 0.8f, bob = 0f, sway = 0f, shake = 1f,
        hLx = 57f, hLy = 150f, hRx = 143f, hRy = 150f, drop = 1f, roll = 1f,
        nod = 0f, twitch = 0f, lean = 0f, bend = 0f, curl = 0.1f, hookWobble = 6f, hookHz = 9f,
        open = 0f, closed = MariShapes.ClosedSqueeze, eyeSharp = 1f, blink = 0f, smile = -0.3f, mouthOpen = 0f,
        zig = 0f, grit = 1f, cheek = 0f, sparkle = 0f, sweat = 0.6f, waves = 1f,
    )

    private val Asleep = Fine.copy(
        robinSleep = 1f, lamp = 1f, bloom = 0.2f, bob = 0f, sway = 0f, breathe = 1f,
        hLx = 95f, hLy = 230f, hRx = 105f, hRy = 230f, drop = 1f,
        headY = 2f, tilt = 6f, nod = 0.4f, nodHz = 0.2f, twitch = 0f, lean = 6f, bend = 60f, curl = 1.2f,
        hookWobble = 1f, hookHz = 0.25f,
        open = 0f, closed = MariShapes.ClosedSleep, blink = 0f, smile = 0f, mouthOpen = 0.3f, cheek = 0.4f,
        sparkle = 0f, moon = 1f,
    )

    /** The resting pose for [mood]; null means asleep. */
    fun of(mood: Mood?): MariPose = when (mood) {
        Mood.HAPPY -> Happy
        Mood.FINE -> Fine
        Mood.WORRIED -> Worried
        Mood.SAD -> Sad
        Mood.EARS_COVERED -> Covered
        null -> Asleep
    }
}
