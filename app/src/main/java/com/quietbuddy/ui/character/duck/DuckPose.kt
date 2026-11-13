package com.quietbuddy.ui.character.duck

import com.quietbuddy.core.Mood

private fun pts(vararg v: Float): List<Float> = v.toList()

/** Geometry shared by every pose and the drawing (same counts, so shapes can blend). Ported from docs/design/duck-motion.html. */
internal object DuckShapes {
    /** Head radius in design units, and the scale it gives the head drawing (drawn around radius 38). */
    const val HEAD_R = 43f
    const val HEAD_SCALE = HEAD_R / 38f
    const val HEAD_CX = 114f
    const val HEAD_CY = 68f

    /** Closed-eye strokes relative to each eye: x in eye half-widths, y in units. The far eye mirrors x. */
    val ClosedHappy = pts(-1f, 3f, 0f, -9f, 1f, 3f)
    val ClosedSleep = pts(-1f, 0f, 0f, 8f, 1f, 0f)
    val ClosedSqueeze = pts(-0.85f, -7f, 1.4f, 0f, -0.85f, 7f)

    val BrowNoneNear = pts(88f, 38f, 98f, 34f, 108f, 36f)
    val BrowNoneFar = pts(128f, 35f, 136f, 33f, 144f, 35f)
    val BrowWorriedNear = pts(88f, 40f, 98f, 36f, 108f, 30f)
    val BrowWorriedFar = pts(128f, 30f, 136f, 34f, 144f, 38f)
    val BrowSadNear = pts(88f, 44f, 98f, 37f, 108f, 29f)
    val BrowSadFar = pts(128f, 29f, 136f, 35f, 144f, 42f)

    /** The near eye (left on screen) and the smaller far eye; [dir] mirrors the closed strokes. */
    class Eye(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val dir: Float)

    val Eyes = listOf(Eye(98f, 58f, 9.5f, 12.5f, 1f), Eye(136f, 55f, 7.4f, 12f, -1f))

    /** The folded wing (body units) and the same wing raised over the near side of the head (head units). */
    val WingRest = pts(
        128f, 132f, 108f, 112f, 78f, 122f, 60f, 128f, 46f, 134f, 54f, 138f, 58f, 139f, 52f, 144f, 49f, 149f,
        58f, 150f, 63f, 149f, 59f, 154f, 58f, 160f, 70f, 162f, 80f, 162f, 110f, 166f, 128f, 136f,
    )
    val WingCover = pts(
        126f, 132f, 68f, 112f, 60f, 80f, 56f, 60f, 56f, 42f, 62f, 44f, 64f, 42f, 62f, 34f, 66f, 30f,
        72f, 34f, 74f, 32f, 74f, 26f, 78f, 22f, 86f, 26f, 86f, 36f, 88f, 80f, 130f, 136f,
    )

    /** Point indices of [WingCover] in body units; the rest are in head units. */
    val WingCoverBodyPoints = setOf(0, 16)

    /** The far wing is hidden behind the body until it rises behind the head. */
    val FarRest = pts(140f, 126f, 130f, 118f, 110f, 122f, 90f, 128f, 100f, 140f, 120f, 146f, 140f, 134f)
    val FarCover = pts(150f, 124f, 162f, 100f, 168f, 70f, 170f, 40f, 152f, 22f, 140f, 36f, 146f, 96f)

    /** Point index of [FarCover] in body units. */
    const val FAR_COVER_BODY_POINT = 0
}

/**
 * Every number that defines one duck expression. Pure data (no Compose) so poses can be blended and tested on the JVM.
 * Point lists hold x,y pairs; a field's lists always have equal lengths.
 */
data class DuckPose(
    // body group
    val poof: Float,
    val sway: Float,
    val swayHz: Float,
    val shake: Float,
    val tailDroop: Float,
    val tailWag: Float,
    val tailHz: Float,
    val wing: Float,
    val cover: Float,
    val flap: Float,
    val flapHz: Float,
    val float: Float,
    val floatHz: Float,
    val paddle: Float,
    val breathe: Float,
    // head group
    val headDx: Float,
    val headDy: Float,
    val tilt: Float,
    val twitch: Float,
    val nod: Float,
    val nodHz: Float,
    // face group: everything else
    val open: Float,
    val eyeScale: Float,
    val closed: List<Float>,
    val closedW: Float,
    val lashes: Float,
    val blink: Float,
    val browA: Float,
    val browN: List<Float>,
    val browF: List<Float>,
    val smile: Float,
    val mouthOpen: Float,
    val cheek: Float,
    val sparkle: Float,
    val sweat: Float,
    val well: Float,
    val tearDrop: Float,
    val zzz: Float,
    val bubble: Float,
    val shock: Float,
) {
    /** The face a pat puts on, blended over this pose by [g] (0 = none, 1 = full giggle). */
    fun giggled(g: Float): DuckPose = if (g <= 0f) this else copy(
        open = l(open, 0f, g),
        closed = ls(closed, DuckShapes.ClosedHappy, g),
        closedW = l(closedW, 3.6f, g),
        blink = l(blink, 0f, g),
        smile = l(smile, 1f, g),
        mouthOpen = l(mouthOpen, 1f, g),
        cheek = l(cheek, 1f, g),
    )

    companion object {
        /**
         * Blends [a] to [b] with a separate progress per body part, so the body, head and face can move at different
         * moments. Progress 0 gives [a], 1 gives [b].
         */
        fun blend(a: DuckPose, b: DuckPose, tBody: Float, tHead: Float, tFace: Float): DuckPose = DuckPose(
            poof = l(a.poof, b.poof, tBody),
            sway = l(a.sway, b.sway, tBody),
            swayHz = l(a.swayHz, b.swayHz, tBody),
            shake = l(a.shake, b.shake, tBody),
            tailDroop = l(a.tailDroop, b.tailDroop, tBody),
            tailWag = l(a.tailWag, b.tailWag, tBody),
            tailHz = l(a.tailHz, b.tailHz, tBody),
            wing = l(a.wing, b.wing, tBody),
            cover = l(a.cover, b.cover, tBody),
            flap = l(a.flap, b.flap, tBody),
            flapHz = l(a.flapHz, b.flapHz, tBody),
            float = l(a.float, b.float, tBody),
            floatHz = l(a.floatHz, b.floatHz, tBody),
            paddle = l(a.paddle, b.paddle, tBody),
            breathe = l(a.breathe, b.breathe, tBody),
            headDx = l(a.headDx, b.headDx, tHead),
            headDy = l(a.headDy, b.headDy, tHead),
            tilt = l(a.tilt, b.tilt, tHead),
            twitch = l(a.twitch, b.twitch, tHead),
            nod = l(a.nod, b.nod, tHead),
            nodHz = l(a.nodHz, b.nodHz, tHead),
            open = l(a.open, b.open, tFace),
            eyeScale = l(a.eyeScale, b.eyeScale, tFace),
            closed = ls(a.closed, b.closed, tFace),
            closedW = l(a.closedW, b.closedW, tFace),
            lashes = l(a.lashes, b.lashes, tFace),
            blink = l(a.blink, b.blink, tFace),
            browA = l(a.browA, b.browA, tFace),
            browN = ls(a.browN, b.browN, tFace),
            browF = ls(a.browF, b.browF, tFace),
            smile = l(a.smile, b.smile, tFace),
            mouthOpen = l(a.mouthOpen, b.mouthOpen, tFace),
            cheek = l(a.cheek, b.cheek, tFace),
            sparkle = l(a.sparkle, b.sparkle, tFace),
            sweat = l(a.sweat, b.sweat, tFace),
            well = l(a.well, b.well, tFace),
            tearDrop = l(a.tearDrop, b.tearDrop, tFace),
            zzz = l(a.zzz, b.zzz, tFace),
            bubble = l(a.bubble, b.bubble, tFace),
            shock = l(a.shock, b.shock, tFace),
        )

        /** Uniform blend of every field. */
        fun lerp(a: DuckPose, b: DuckPose, t: Float): DuckPose = blend(a, b, t, t, t)
    }
}

// a*(1-t) + b*t is exact at both ends, unlike a + (b-a)*t.
private fun l(a: Float, b: Float, t: Float) = a * (1f - t) + b * t
private fun ls(a: List<Float>, b: List<Float>, t: Float) = List(b.size) { l(a[it], b[it], t) }

/** The six resting poses. Numbers come straight from the approved prototype. */
object DuckPoses {
    /** Fine is the base the other moods override. */
    private val Fine = DuckPose(
        poof = 0f, sway = 1.5f, swayHz = 0.3f, shake = 0f, tailDroop = 0f, tailWag = 2f, tailHz = 0.8f,
        wing = 0f, cover = 0f, flap = 0f, flapHz = 1f, float = 1.5f, floatHz = 0.4f, paddle = 0.6f, breathe = 0f,
        headDx = 0f, headDy = 0f, tilt = 0f, twitch = 0.12f, nod = 1.2f, nodHz = 0.5f,
        open = 1f, eyeScale = 1f, closed = DuckShapes.ClosedSleep, closedW = 3.4f, lashes = 0f, blink = 1f,
        browA = 0f, browN = DuckShapes.BrowNoneNear, browF = DuckShapes.BrowNoneFar,
        smile = 0.35f, mouthOpen = 0f, cheek = 0.6f,
        sparkle = 0f, sweat = 0f, well = 0f, tearDrop = 0f, zzz = 0f, bubble = 0f, shock = 0f,
    )

    private val Happy = Fine.copy(
        sway = 3f, swayHz = 0.6f, tailDroop = -6f, tailWag = 4f, tailHz = 2.4f, wing = 6f, flap = 14f, flapHz = 1.6f,
        float = 2.2f, floatHz = 0.8f, paddle = 1.5f, headDy = -3f, tilt = -6f, nod = 2.5f, nodHz = 1.4f,
        open = 0f, closed = DuckShapes.ClosedHappy, closedW = 3.6f, blink = 0f, smile = 1f, mouthOpen = 0.7f,
        cheek = 0.9f, sparkle = 1f,
    )

    private val Worried = Fine.copy(
        sway = 1.5f, swayHz = 1.4f, tailWag = 1f, tailHz = 3f, wing = -2f, float = 1f, floatHz = 0.9f, paddle = 1f,
        headDx = -2f, headDy = -5f, twitch = 0.6f, nod = 0.8f, nodHz = 1.2f,
        eyeScale = 0.88f, browA = 1f, browN = DuckShapes.BrowWorriedNear, browF = DuckShapes.BrowWorriedFar,
        smile = -0.45f, mouthOpen = 0.1f, cheek = 0.35f, sweat = 1f,
    )

    private val Sad = Fine.copy(
        sway = 0.8f, swayHz = 0.2f, tailDroop = 13f, tailWag = 0.3f, tailHz = 0.4f, wing = -8f, float = 0.8f,
        floatHz = 0.25f, paddle = 0.2f, headDx = 4f, headDy = 7f, tilt = 10f, twitch = 0.03f, nod = 0.5f, nodHz = 0.3f,
        browA = 1f, browN = DuckShapes.BrowSadNear, browF = DuckShapes.BrowSadFar,
        smile = -1f, cheek = 0.35f, well = 1f, tearDrop = 1f,
    )

    private val Covered = Fine.copy(
        poof = 1f, sway = 0f, shake = 1f, tailWag = 0f, cover = 1f, float = 0.5f, floatHz = 2f, paddle = 2.2f,
        headDy = 6f, tilt = 3f, twitch = 0f, nod = 0f,
        open = 0f, closed = DuckShapes.ClosedSqueeze, closedW = 3.6f, blink = 0f, smile = -0.6f, mouthOpen = 0.3f,
        cheek = 0.5f, sweat = 1f, shock = 1f,
    )

    private val Asleep = Fine.copy(
        sway = 0.8f, swayHz = 0.15f, tailDroop = 8f, tailWag = 0f, wing = -2f, float = 1f, floatHz = 0.25f,
        paddle = 0f, breathe = 1f, headDx = -2f, headDy = 10f, tilt = 14f, twitch = 0.03f, nod = 0.6f, nodHz = 0.25f,
        open = 0f, closed = DuckShapes.ClosedSleep, closedW = 3.4f, lashes = 1f, blink = 0f, smile = 0.2f,
        cheek = 0.5f, bubble = 1f, zzz = 1f,
    )

    /** The resting pose for [mood]; null means asleep. */
    fun of(mood: Mood?): DuckPose = when (mood) {
        Mood.HAPPY -> Happy
        Mood.FINE -> Fine
        Mood.WORRIED -> Worried
        Mood.SAD -> Sad
        Mood.EARS_COVERED -> Covered
        null -> Asleep
    }
}
