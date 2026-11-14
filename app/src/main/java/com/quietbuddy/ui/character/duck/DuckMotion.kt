package com.quietbuddy.ui.character.duck

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.IdlePhase
import com.quietbuddy.ui.character.PokeEnvelope
import com.quietbuddy.ui.character.isCalm
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** How a transition feels: the direction decides its duration, per-part windows and easing. */
enum class DuckTransitionKind(val durationMs: Int) {
    /** Towards a louder mood: a quick startle, the wings snapping up over the head. */
    LOUDER(180),

    /** Towards a calmer mood: a slow relax, the face recovering first and the head last. */
    CALMER(900),

    /** Any mood to asleep: the eyes close last. */
    SLEEP(1400),

    /** Asleep to any mood: the eyes open first. */
    WAKE(1400),
}

/** The parts of the duck that move on their own schedule during a transition. */
private enum class Part { BODY, HEAD, FACE }

/** When each part moves, as a fraction of the whole transition. */
private fun DuckTransitionKind.window(part: Part): ClosedFloatingPointRange<Float> = when (this) {
    DuckTransitionKind.LOUDER -> when (part) { Part.BODY -> 0f..1f; Part.HEAD -> 0f..0.7f; Part.FACE -> 0f..0.6f }
    DuckTransitionKind.CALMER -> when (part) { Part.BODY -> 0f..1f; Part.HEAD -> 0.2f..1f; Part.FACE -> 0f..0.55f }
    DuckTransitionKind.SLEEP -> when (part) { Part.BODY -> 0f..0.9f; Part.HEAD -> 0.1f..0.8f; Part.FACE -> 0.35f..1f }
    DuckTransitionKind.WAKE -> when (part) { Part.BODY -> 0.25f..1f; Part.HEAD -> 0.15f..0.8f; Part.FACE -> 0f..0.45f }
}

private fun DuckTransitionKind.ease(t: Float): Float = when (this) {
    DuckTransitionKind.LOUDER -> 1f - (1f - t).pow(3)
    DuckTransitionKind.CALMER -> if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
    DuckTransitionKind.SLEEP -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f
    DuckTransitionKind.WAKE -> 1f - (1f - t).pow(2)
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
fun duckTransitionKind(from: Mood?, to: Mood?): DuckTransitionKind = when {
    to == null -> DuckTransitionKind.SLEEP
    from == null -> DuckTransitionKind.WAKE
    to.level > from.level -> DuckTransitionKind.LOUDER
    else -> DuckTransitionKind.CALMER
}

/** A blend from one pose to another that started at [startSec]. */
class DuckTransition(
    val from: DuckPose,
    val to: DuckPose,
    val kind: DuckTransitionKind,
    val startSec: Float,
) {
    private val durationSec = kind.durationMs / 1000f

    fun isFinished(nowSec: Float): Boolean = nowSec - startSec >= durationSec

    /** The blended pose at [nowSec]; exactly [to] once finished. */
    fun poseAt(nowSec: Float): DuckPose {
        val e = (nowSec - startSec) / durationSec
        if (e >= 1f) return to
        fun progress(part: Part): Float {
            val w = kind.window(part)
            return kind.ease(((e - w.start) / (w.endInclusive - w.start)).coerceIn(0f, 1f))
        }
        return DuckPose.blend(from, to, progress(Part.BODY), progress(Part.HEAD), progress(Part.FACE))
    }
}

/** Per-frame effects drawn on top of a pose. */
data class DuckFx(
    val time: Float = 0f,
    /** False draws the still pose: no floating, rocking, paddling, tremble, breathing or twinkle. */
    val idle: Boolean = false,
    val blink: Float = 0f,
    /** Extra head tilt in degrees for a quick bird-like twitch. */
    val twitch: Float = 0f,
    /** Giggle after a pat, 0..1: the happy-eyed open-billed face is blended over the pose. */
    val giggle: Float = 0f,
    /** Extra tail wagging after a pat, 0..1. */
    val patWag: Float = 0f,
    /** Seconds since the pat's quack began, or negative when there is none. */
    val quack: Float = -1f,
    /** Angles in radians of the idle rhythms; see [IdlePhase]. */
    val floatPhase: Float = 0f,
    val swayPhase: Float = 0f,
    val nodPhase: Float = 0f,
    val paddlePhase: Float = 0f,
    val wagPhase: Float = 0f,
    val flapPhase: Float = 0f,
) {
    companion object {
        val Still = DuckFx()
    }
}

/**
 * Holds the duck's mood, runs pose transitions and the random idle motion (blinks, head twitches).
 * Pure Kotlin: the caller feeds it time via [advance]. A new mood during a transition starts from the pose shown now.
 */
class DuckAnimator(initial: Mood?, private val random: Random = Random.Default) {
    private var mood: Mood? = initial
    private var transition: DuckTransition? = null
    private var clock = 0f
    private var nextBlink = 1.5f
    private var blinkStart = -10f
    private var twitchStart = -10f
    private var twitchDir = 1f
    private var nextTwitch = 2f
    private var quackSec = -1f
    private val giggleEnv = PokeEnvelope(attackSec = 0.05f, holdSec = 0.5f, releaseSec = 0.35f)
    private val wagEnv = PokeEnvelope(attackSec = 0.05f, holdSec = 0.35f, releaseSec = 0.6f)
    private val floatPhase = IdlePhase()
    private val swayPhase = IdlePhase()
    private val nodPhase = IdlePhase()
    private val paddlePhase = IdlePhase()
    private val wagPhase = IdlePhase()
    private val flapPhase = IdlePhase()

    /** The giggle level as of the last [advance], 0..1. */
    val giggle: Float get() = giggleEnv.value

    /** The pose being shown, before the pat's giggle is blended over it (that is [DuckFx.giggle]). */
    var pose: DuckPose = DuckPoses.of(initial)
        private set

    /** The idle effects as of the last [advance]; still until the first one. */
    var fx: DuckFx = DuckFx.Still
        private set

    /** Moves towards [newMood]; with [animate] false it jumps there. */
    fun setMood(newMood: Mood?, animate: Boolean = true) {
        if (newMood == mood && transition == null) return
        val target = DuckPoses.of(newMood)
        if (animate) {
            transition = DuckTransition(pose, target, duckTransitionKind(mood, newMood), clock)
        } else {
            transition = null
            pose = target
        }
        mood = newMood
        if (!newMood.isCalm) {
            giggleEnv.release()
            wagEnv.release()
        }
    }

    /** A pat: in a calm mood the duck giggles, quacks and wags its tail; otherwise nothing. */
    fun poke() {
        if (!mood.isCalm) return
        giggleEnv.trigger()
        wagEnv.trigger()
        quackSec = 0f
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
        // Head twitches are random, and more frequent in tense poses.
        if (clock > nextTwitch) {
            if (random.nextFloat() < pose.twitch * 2f) {
                twitchStart = clock
                twitchDir = if (random.nextFloat() < 0.5f) -1f else 1f
            }
            nextTwitch = clock + 0.5f
        }
        val tp = (clock - twitchStart) / 0.35f
        val twitch = if (tp in 0f..1f) twitchDir * 8f * sin(PI.toFloat() * tp) else 0f
        giggleEnv.advance(dtSec)
        wagEnv.advance(dtSec)
        floatPhase.advance(pose.floatHz, dtSec)
        swayPhase.advance(pose.swayHz, dtSec)
        nodPhase.advance(pose.nodHz, dtSec)
        // The paddle amount doubles as its speed: faster, harder kicks when anxious.
        paddlePhase.advance(pose.paddle, dtSec)
        // A pat speeds the wag up; the envelope eases that change in and out.
        wagPhase.advance(pose.tailHz + wagEnv.value * 3f, dtSec)
        flapPhase.advance(pose.flapHz, dtSec)
        if (quackSec >= 0f) {
            quackSec += dtSec
            if (quackSec >= QUACK_SEC) quackSec = -1f
        }
        fx = DuckFx(
            time = clock,
            idle = true,
            blink = blink,
            twitch = twitch,
            giggle = giggleEnv.value,
            patWag = wagEnv.value,
            quack = quackSec,
            floatPhase = floatPhase.radians,
            swayPhase = swayPhase.radians,
            nodPhase = nodPhase.radians,
            paddlePhase = paddlePhase.radians,
            wagPhase = wagPhase.radians,
            flapPhase = flapPhase.radians,
        )
    }

    private companion object {
        /** How long the quack's sound waves last. */
        const val QUACK_SEC = 0.8f
    }
}
