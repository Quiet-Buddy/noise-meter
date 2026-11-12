package com.quietbuddy.ui.character.kitten

import com.quietbuddy.core.Mood

private fun pts(vararg v: Int): List<Float> = v.map { it.toFloat() }

/** Control points shared by every pose (same counts, so shapes can blend). Ported from docs/design/kitten-motion.html. */
internal object KittenShapes {
    val EarUp = pts(50, 84, 30, 40, 38, 18, 42, 10, 51, 14, 74, 26, 98, 52)
    val EarFlat = pts(58, 80, 36, 74, 13, 65, 7, 62, 12, 58, 38, 44, 62, 50)
    val InUp = pts(50, 58, 42, 36, 45, 26, 47, 21, 52, 24, 66, 32, 78, 44)
    val InFlat = pts(53, 72, 37, 68, 21, 62, 19, 60, 22, 58, 38, 52, 56, 56)

    val TailUp = pts(140, 172, 176, 172, 180, 140, 183, 118, 172, 106, 164, 98, 166, 92)
    val TailDown = pts(140, 176, 164, 186, 182, 180, 194, 175, 194, 164, 194, 156, 188, 152)
    val TailBrush = pts(140, 168, 168, 164, 176, 136, 180, 110, 182, 86, 183, 66, 182, 52)
    val TailWrap = pts(142, 172, 168, 178, 160, 192, 150, 200, 100, 198, 64, 197, 56, 188)

    val ClosedHappy = pts(58, 94, 70, 78, 82, 94)
    val ClosedSleep = pts(58, 92, 70, 102, 82, 92)
    val ClosedSqueeze = pts(60, 82, 92, 92, 60, 102)
}

/**
 * Every number that defines one kitten expression. Pure data (no Compose) so poses can be blended and tested on the JVM.
 * Point lists hold x,y pairs; a field's lists always have equal lengths.
 */
data class KittenPose(
    // fur group: moves with the whole body
    val poof: Float,
    val tail: List<Float>,
    val tailFront: Float,
    val sway: Float,
    val swayHz: Float,
    val shake: Float,
    // ears group
    val ear: Float,
    val earFold: Float,
    val twitch: Float,
    // face group: everything else
    val open: Float,
    val eyeRx: Float,
    val eyeRy: Float,
    val hi: Float,
    val eyeDy: Float,
    val closed: List<Float>,
    val closedW: Float,
    val lashes: Float,
    val blink: Float,
    val browA: Float,
    val brow: List<Float>,
    val mouth: List<Float>,
    val mouthOpen: Float,
    val fang: Float,
    val cheek: Float,
    val sparkle: Float,
    val sweat: Float,
    val sweatX: Float,
    val sweatY: Float,
    val well: Float,
    val tearDrop: Float,
    val zzz: Float,
    val bubble: Float,
    val shock: Float,
    val whisker: Float,
    val headDy: Float,
    val breathe: Float,
) {
    companion object {
        /**
         * Blends [a] to [b] with a separate progress per body part, so the fur, ears and face can move at different
         * moments. Progress 0 gives [a], 1 gives [b].
         */
        fun blend(a: KittenPose, b: KittenPose, tFur: Float, tEars: Float, tFace: Float): KittenPose = KittenPose(
            poof = l(a.poof, b.poof, tFur),
            tail = ls(a.tail, b.tail, tFur),
            tailFront = l(a.tailFront, b.tailFront, tFur),
            sway = l(a.sway, b.sway, tFur),
            swayHz = l(a.swayHz, b.swayHz, tFur),
            shake = l(a.shake, b.shake, tFur),
            ear = l(a.ear, b.ear, tEars),
            earFold = l(a.earFold, b.earFold, tEars),
            twitch = l(a.twitch, b.twitch, tEars),
            open = l(a.open, b.open, tFace),
            eyeRx = l(a.eyeRx, b.eyeRx, tFace),
            eyeRy = l(a.eyeRy, b.eyeRy, tFace),
            hi = l(a.hi, b.hi, tFace),
            eyeDy = l(a.eyeDy, b.eyeDy, tFace),
            closed = ls(a.closed, b.closed, tFace),
            closedW = l(a.closedW, b.closedW, tFace),
            lashes = l(a.lashes, b.lashes, tFace),
            blink = l(a.blink, b.blink, tFace),
            browA = l(a.browA, b.browA, tFace),
            brow = ls(a.brow, b.brow, tFace),
            mouth = ls(a.mouth, b.mouth, tFace),
            mouthOpen = l(a.mouthOpen, b.mouthOpen, tFace),
            fang = l(a.fang, b.fang, tFace),
            cheek = l(a.cheek, b.cheek, tFace),
            sparkle = l(a.sparkle, b.sparkle, tFace),
            sweat = l(a.sweat, b.sweat, tFace),
            sweatX = l(a.sweatX, b.sweatX, tFace),
            sweatY = l(a.sweatY, b.sweatY, tFace),
            well = l(a.well, b.well, tFace),
            tearDrop = l(a.tearDrop, b.tearDrop, tFace),
            zzz = l(a.zzz, b.zzz, tFace),
            bubble = l(a.bubble, b.bubble, tFace),
            shock = l(a.shock, b.shock, tFace),
            whisker = l(a.whisker, b.whisker, tFace),
            headDy = l(a.headDy, b.headDy, tFace),
            breathe = l(a.breathe, b.breathe, tFace),
        )

        /** Uniform blend of every field. */
        fun lerp(a: KittenPose, b: KittenPose, t: Float): KittenPose = blend(a, b, t, t, t)

        // a*(1-t) + b*t is exact at both ends, unlike a + (b-a)*t.
        private fun l(a: Float, b: Float, t: Float) = a * (1f - t) + b * t
        private fun ls(a: List<Float>, b: List<Float>, t: Float) = List(b.size) { l(a[it], b[it], t) }
    }
}

/** The six resting poses. Numbers come straight from the approved prototype. */
object KittenPoses {
    /** Fine is the base the other moods override. */
    private val Fine = KittenPose(
        poof = 0f, tail = KittenShapes.TailUp, tailFront = 0f, sway = 4f, swayHz = 0.35f, shake = 0f,
        ear = 0f, earFold = 0f, twitch = 0.12f,
        open = 1f, eyeRx = 12f, eyeRy = 14f, hi = 1f, eyeDy = 0f, closed = KittenShapes.ClosedSleep, closedW = 4.5f,
        lashes = 0f, blink = 1f,
        browA = 0f, brow = pts(60, 70, 70, 66, 80, 68),
        mouth = pts(87, 113, 93, 120, 100, 113, 107, 120, 113, 113), mouthOpen = 0f, fang = 0f,
        cheek = 0.6f, sparkle = 0f, sweat = 0f, sweatX = 160f, sweatY = 54f, well = 0f, tearDrop = 0f,
        zzz = 0f, bubble = 0f, shock = 0f, whisker = 0f, headDy = 0f, breathe = 0f,
    )

    private val Happy = Fine.copy(
        sway = 8f, swayHz = 0.6f, open = 0f, closed = KittenShapes.ClosedHappy, closedW = 5f, blink = 0f,
        mouth = pts(86, 112, 93, 120, 100, 113, 107, 120, 114, 112), mouthOpen = 1f, cheek = 0.9f, sparkle = 1f,
        headDy = -2f,
    )

    private val Worried = Fine.copy(
        ear = -14f, twitch = 0.6f, sway = 3f, swayHz = 1.6f, eyeRx = 11f, eyeRy = 13f, hi = 0.75f, eyeDy = 2f,
        browA = 1f, brow = pts(58, 72, 68, 70, 80, 64), mouth = pts(86, 118, 93, 113, 100, 118, 107, 123, 114, 117),
        cheek = 0.35f, sweat = 1f,
    )

    private val Sad = Fine.copy(
        ear = -48f, twitch = 0.04f, tail = KittenShapes.TailDown, sway = 1.2f, swayHz = 0.2f, eyeDy = 4f,
        browA = 1f, brow = pts(58, 76, 70, 72, 80, 64), mouth = pts(90, 122, 95, 114, 100, 113, 105, 114, 110, 122),
        cheek = 0.35f, well = 1f, tearDrop = 1f, whisker = -10f, headDy = 3f,
    )

    private val Covered = Fine.copy(
        poof = 1f, tail = KittenShapes.TailBrush, sway = 0f, ear = 0f, earFold = 1f, twitch = 0f,
        open = 0f, closed = KittenShapes.ClosedSqueeze, closedW = 5f, blink = 0f,
        mouth = pts(86, 116, 93, 111, 100, 116, 107, 121, 114, 115), fang = 1f,
        cheek = 0.5f, sweat = 1f, sweatX = 150f, sweatY = 98f, shock = 1f, whisker = 12f, shake = 1f,
    )

    private val Asleep = Fine.copy(
        ear = -10f, twitch = 0.03f, tail = KittenShapes.TailWrap, tailFront = 1f, sway = 1f, swayHz = 0.15f,
        open = 0f, closed = KittenShapes.ClosedSleep, closedW = 4.5f, lashes = 1f, blink = 0f,
        mouth = pts(92, 114, 96, 118, 100, 114, 104, 118, 108, 114),
        cheek = 0.5f, bubble = 1f, zzz = 1f, whisker = -4f, headDy = 4f, breathe = 1f,
    )

    /** The resting pose for [mood]; null means asleep. */
    fun of(mood: Mood?): KittenPose = when (mood) {
        Mood.HAPPY -> Happy
        Mood.FINE -> Fine
        Mood.WORRIED -> Worried
        Mood.SAD -> Sad
        Mood.EARS_COVERED -> Covered
        null -> Asleep
    }
}
