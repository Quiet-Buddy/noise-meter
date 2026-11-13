package com.quietbuddy.ui.character.robot

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.isCalm
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** How a transition feels: the direction decides its duration, per-part windows and easing. */
enum class RobotTransitionKind(val durationMs: Int) {
    /** Towards a louder mood: a quick startle, the hands snapping onto the ear discs. */
    LOUDER(180),

    /** Towards a calmer mood: a slow relax, the screen face recovering first and the body last. */
    CALMER(900),

    /** Any mood to asleep (landing): the screen dims last. */
    SLEEP(1400),

    /** Asleep to any mood (taking off): the screen lights up first. */
    WAKE(1400),
}

/** The parts of Astro that move on their own schedule during a transition. */
private enum class Part { BODY, HEAD, FACE }

/** When each part moves, as a fraction of the whole transition. */
private fun RobotTransitionKind.window(part: Part): ClosedFloatingPointRange<Float> = when (this) {
    RobotTransitionKind.LOUDER -> when (part) { Part.BODY -> 0f..1f; Part.HEAD -> 0f..0.7f; Part.FACE -> 0f..0.6f }
    RobotTransitionKind.CALMER -> when (part) { Part.BODY -> 0.1f..1f; Part.HEAD -> 0.2f..1f; Part.FACE -> 0f..0.55f }
    RobotTransitionKind.SLEEP -> when (part) { Part.BODY -> 0f..0.9f; Part.HEAD -> 0.1f..0.8f; Part.FACE -> 0.4f..1f }
    RobotTransitionKind.WAKE -> when (part) { Part.BODY -> 0.25f..1f; Part.HEAD -> 0.15f..0.8f; Part.FACE -> 0f..0.4f }
}

private fun RobotTransitionKind.ease(t: Float): Float = when (this) {
    RobotTransitionKind.LOUDER -> 1f - (1f - t).pow(3)
    RobotTransitionKind.CALMER -> easeInOutCubic(t)
    RobotTransitionKind.SLEEP -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f
    RobotTransitionKind.WAKE -> 1f - (1f - t).pow(2)
}

private fun easeInOutCubic(t: Float) = if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f

private fun smooth(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Louder = higher level; asleep is handled before levels are compared. */
private val Mood.level: Int
    get() = when (this) {
        Mood.HAPPY -> 0
        Mood.FINE -> 1
        Mood.WORRIED -> 2
        Mood.SAD -> 3
        Mood.EARS_COVERED -> 4
    }

/** Picks the transition kind between two moods (null = asleep). */
fun robotTransitionKind(from: Mood?, to: Mood?): RobotTransitionKind = when {
    to == null -> RobotTransitionKind.SLEEP
    from == null -> RobotTransitionKind.WAKE
    to.level > from.level -> RobotTransitionKind.LOUDER
    else -> RobotTransitionKind.CALMER
}

/** A blend from one pose to another that started at [startSec]. */
class RobotTransition(
    val from: RobotPose,
    val to: RobotPose,
    val kind: RobotTransitionKind,
    val startSec: Float,
) {
    private val durationSec = kind.durationMs / 1000f

    fun isFinished(nowSec: Float): Boolean = nowSec - startSec >= durationSec

    /** The blended pose at [nowSec]; exactly [to] once finished. */
    fun poseAt(nowSec: Float): RobotPose {
        val e = (nowSec - startSec) / durationSec
        if (e >= 1f) return to
        fun progress(part: Part): Float {
            val w = kind.window(part)
            return kind.ease(((e - w.start) / (w.endInclusive - w.start)).coerceIn(0f, 1f))
        }
        return RobotPose.blend(from, to, progress(Part.BODY), progress(Part.HEAD), progress(Part.FACE))
    }
}

/** One dot of the fading trail behind a flying lap, in the lap's own (unscaled) space. */
class TrailDot(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/** Per-frame effects drawn on top of a pose. */
data class RobotFx(
    val time: Float = 0f,
    /** False draws the still pose: no hovering, rocking, hand waving, tremble, breathing, flame flicker or twinkle. */
    val idle: Boolean = false,
    val blink: Float = 0f,
    /** Sideways glance of the screen eyes (and a little head tilt) in design units. */
    val twitch: Float = 0f,
    /** Heart eyes and big smile after a pat, 0..1, blended over the pose. */
    val patFace: Float = 0f,
    /** Extra antenna angle in degrees: the boing after a pat. */
    val boing: Float = 0f,
    /** 0..1 pulse that makes the heart eyes beat. */
    val heartPulse: Float = 0f,
    /** Degrees turned around the vertical axis during a combo spin; 0 when not spinning. */
    val yaw: Float = 0f,
    /** Vertical offset in design units (negative = up) from the trick's hop and the settle after a lap. */
    val hop: Float = 0f,
    /** Extra thruster power during spins and the lap. */
    val boost: Float = 0f,
    /** Angle in radians along the lap's circle, or null when not flying a lap. */
    val orbit: Float? = null,
    /** Normalised lap speed: 0 at both ends, 1 midway. */
    val lapSpeed: Float = 0f,
    /** Progress 0..1 of the trick's sparkle burst, or negative when there is none. */
    val burst: Float = -1f,
) {
    /** How far the body leans into the turn; follows the speed, so it is 0 when the lap starts and ends. */
    val bank: Float get() = orbit?.let { 14f * cos(it) * lapSpeed } ?: 0f

    /** Where the lap puts Astro: the floor circle seen from the front, smaller and higher when far away. */
    val lapX: Float get() = orbit?.let { 38f * sin(it) } ?: 0f
    val lapY: Float get() = orbit?.let { -36f * (1f - cos(it)) / 2f } ?: 0f
    val lapScale: Float get() = orbit?.let { 1f - 0.4f * (1f - cos(it)) / 2f } ?: 1f

    /** The fading trail of glowing dots the lap leaves behind; empty at the start and the end. */
    val trail: List<TrailDot>
        get() {
            val a = orbit ?: return emptyList()
            return (1..5).mapNotNull { i ->
                val b = a - i * 0.22f
                if (b < 0f) return@mapNotNull null
                val depth = (1f - cos(b)) / 2f
                val k = 1f - 0.4f * depth
                TrailDot(100f + 38f * sin(b), 150f - 36f * depth, 5f * k, 0.5f * (1f - i / 6f) * lapSpeed)
            }
        }

    companion object {
        val Still = RobotFx()
    }
}

/**
 * Holds Astro's mood, runs pose transitions, the random idle motion (blinks, glances) and the pat combos.
 * Pure Kotlin: the caller feeds it time via [advance]. A new mood during a transition starts from the pose shown now.
 *
 * Pats (see [poke]) only count in calm moods. Each gives heart eyes and an antenna boing; quick pats (less than
 * [COMBO_GAP_SEC] apart) add up: 3 make one spin, 6 a hop with a double spin and a sparkle burst, 9 a flying lap,
 * and then the count carries on.
 */
class RobotAnimator(initial: Mood?, private val random: Random = Random.Default) {
    private class Spin(val start: Float, val dur: Float, val turns: Float, val hop: Float)
    private class Lap(val start: Float, val dur: Float)

    private var mood: Mood? = initial
    private var transition: RobotTransition? = null
    private var clock = 0f
    private var nextBlink = 1.5f
    private var blinkStart = -10f
    private var twitchStart = -10f
    private var twitchDir = 1f
    private var nextTwitch = 2f

    private var lastPat = -10f
    private var combo = 0
    private var spin: Spin? = null
    private var lap: Lap? = null
    private var lapEnd = -10f
    private var burstAt = -10f

    /** When the heart eyes start to fade: after the last pat, or after the move it started if that ends later. */
    private var faceHoldUntil = -10f

    /** The pose being shown, before the pat's heart eyes are blended over it (that is [RobotFx.patFace]). */
    var pose: RobotPose = RobotPoses.of(initial)
        private set

    /** The idle effects as of the last [advance]; still until the first one. */
    var fx: RobotFx = RobotFx.Still
        private set

    /** The number of quick pats in the current combo. */
    val comboCount: Int get() = combo

    /** Whether a spin or a lap is under way. */
    val isPerforming: Boolean get() = spin != null || lap != null

    /** Moves towards [newMood]; with [animate] false it jumps there. */
    fun setMood(newMood: Mood?, animate: Boolean = true) {
        if (newMood == mood && transition == null) return
        val target = RobotPoses.of(newMood)
        if (animate) {
            transition = RobotTransition(pose, target, robotTransitionKind(mood, newMood), clock)
        } else {
            transition = null
            pose = target
        }
        mood = newMood
        if (!newMood.isCalm) {
            // Heart eyes make no sense in a loud room: start fading now. The combo is over too.
            faceHoldUntil = min(faceHoldUntil, clock - FACE_HOLD_SEC)
            combo = 0
        }
    }

    /** A pat: in a calm mood Astro shows heart eyes, boings its antenna and counts the combo; otherwise nothing. */
    fun poke() {
        if (!mood.isCalm) return
        combo = if (clock - lastPat < COMBO_GAP_SEC) combo + 1 else 1
        lastPat = clock
        when (combo % LAP_AT) {
            0 -> lap = Lap(clock, LAP_SEC)
            TRICK_AT -> {
                spin = Spin(clock, 1.4f, turns = 2f, hop = 1f)
                burstAt = clock + 0.55f
            }
            SPIN_AT -> if (spin == null) spin = Spin(clock, 0.9f, turns = 1f, hop = 0f)
        }
        faceHoldUntil = max(clock, max(spin?.let { it.start + it.dur } ?: clock, lap?.let { it.start + it.dur } ?: clock))
    }

    /** Moves time on by [dtSec] and refreshes [pose] and [fx]. */
    fun advance(dtSec: Float) {
        clock += dtSec
        transition?.let {
            pose = it.poseAt(clock)
            if (it.isFinished(clock)) transition = null
        }
        if (clock > nextBlink) {
            blinkStart = clock
            nextBlink = clock + 2.2f + random.nextFloat() * 3.2f
        }
        val bp = (clock - blinkStart) / 0.16f
        val blink = if (bp in 0f..1f) sin(PI.toFloat() * bp) else 0f
        // Quick glances to the side: random, more often when worried.
        if (clock > nextTwitch) {
            if (random.nextFloat() < pose.twitch * 2f) {
                twitchStart = clock
                twitchDir = if (random.nextFloat() < 0.5f) -1f else 1f
            }
            nextTwitch = clock + 0.5f
        }
        val tp = (clock - twitchStart) / 0.5f
        val twitch = if (tp in 0f..1f) twitchDir * 8f * sin(PI.toFloat() * tp) else 0f

        // Pat: heart eyes and an antenna boing (the squash and hearts come from the home screen).
        val ps = clock - lastPat
        val boing = if (ps >= 0f && ps < 1.2f) 26f * exp(-ps * 4.5f) * sin(ps * 24f) else 0f
        val heartPulse = if (ps >= 0f && ps < 1f) abs(sin(ps * 9f)) * (1f - ps) else 0f
        val patFace = 1f - smooth(FACE_HOLD_SEC, FACE_HOLD_SEC + 0.35f, clock - faceHoldUntil)

        // A combo spin: ease in and out, with the thruster flaring and (for the trick) a hop.
        var yaw = 0f
        var hop = 0f
        var boost = 0f
        spin?.let {
            val e = (clock - it.start) / it.dur
            if (e >= 1f) {
                spin = null
            } else {
                yaw = 360f * it.turns * easeInOutCubic(e)
                hop = -30f * it.hop * sin(PI.toFloat() * e)
                boost = 0.5f * sin(PI.toFloat() * e) * (1f + it.hop)
            }
        }
        // A combo lap: one circle around the room, away from the viewer and back, leaning into the turn.
        var orbit: Float? = null
        var lapSpeed = 0f
        lap?.let {
            val e = (clock - it.start) / it.dur
            if (e >= 1f) {
                lap = null
                lapEnd = clock
            } else {
                // Sine ease: speeds up and slows down gently; lapSpeed is 0 at both ends and 1 midway.
                orbit = (1f - cos(PI.toFloat() * e)) / 2f * PI.toFloat() * 2f
                lapSpeed = sin(PI.toFloat() * e)
                boost = max(boost, 0.6f * lapSpeed)
            }
        }
        // After the lap Astro settles: a small dip and a damped bob over the landing spot.
        val ls = clock - lapEnd
        if (ls >= 0f && ls < 1.2f) hop += 5f * exp(-4f * ls) * sin(9f * ls)
        val bk = (clock - burstAt) / BURST_SEC

        fx = RobotFx(
            time = clock,
            idle = true,
            blink = blink,
            twitch = twitch,
            patFace = patFace,
            boing = boing,
            heartPulse = heartPulse,
            yaw = yaw,
            hop = hop,
            boost = boost,
            orbit = orbit,
            lapSpeed = lapSpeed,
            burst = if (bk in 0f..1f) bk else -1f,
        )
    }

    companion object {
        /** Pats closer together than this build a combo. */
        const val COMBO_GAP_SEC = 0.7f
        const val SPIN_AT = 3
        const val TRICK_AT = 6
        const val LAP_AT = 9
        const val LAP_SEC = 2.6f
        const val BURST_SEC = 0.8f

        /** How long the heart eyes stay after the pat (or the move it started) before fading. */
        private const val FACE_HOLD_SEC = 0.7f
    }
}
