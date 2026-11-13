package com.quietbuddy.ui.character.kitten

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.PokeEnvelope
import com.quietbuddy.ui.character.isCalm
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** How a transition feels: the direction decides its duration, per-part windows and easing. */
enum class KittenTransitionKind(val durationMs: Int) {
    /** Towards a louder mood: a quick startle. */
    LOUDER(180),

    /** Towards a calmer mood: a slow relax. */
    CALMER(900),

    /** Any mood to asleep. */
    SLEEP(1400),

    /** Asleep to any mood. */
    WAKE(1400),
}

/** The parts of the body that move on their own schedule during a transition. */
private enum class Part { FUR, EARS, FACE }

/** When each part moves, as a fraction of the whole transition. */
private fun KittenTransitionKind.window(part: Part): ClosedFloatingPointRange<Float> = when (this) {
    KittenTransitionKind.LOUDER -> when (part) { Part.FUR -> 0f..1f; Part.EARS -> 0f..0.8f; Part.FACE -> 0f..0.6f }
    KittenTransitionKind.CALMER -> when (part) { Part.FUR -> 0f..1f; Part.EARS -> 0.3f..1f; Part.FACE -> 0f..0.55f }
    KittenTransitionKind.SLEEP -> when (part) { Part.FUR -> 0f..0.9f; Part.EARS -> 0.1f..0.7f; Part.FACE -> 0.35f..1f }
    KittenTransitionKind.WAKE -> when (part) { Part.FUR -> 0.25f..1f; Part.EARS -> 0.2f..0.75f; Part.FACE -> 0f..0.45f }
}

private fun KittenTransitionKind.ease(t: Float): Float = when (this) {
    KittenTransitionKind.LOUDER -> 1f - (1f - t).pow(3)
    KittenTransitionKind.CALMER -> if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
    KittenTransitionKind.SLEEP -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f
    KittenTransitionKind.WAKE -> 1f - (1f - t).pow(2)
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
fun kittenTransitionKind(from: Mood?, to: Mood?): KittenTransitionKind = when {
    to == null -> KittenTransitionKind.SLEEP
    from == null -> KittenTransitionKind.WAKE
    to.level > from.level -> KittenTransitionKind.LOUDER
    else -> KittenTransitionKind.CALMER
}

/** A blend from one pose to another that started at [startSec]. */
class KittenTransition(
    val from: KittenPose,
    val to: KittenPose,
    val kind: KittenTransitionKind,
    val startSec: Float,
) {
    private val durationSec = kind.durationMs / 1000f

    fun isFinished(nowSec: Float): Boolean = nowSec - startSec >= durationSec

    /** The blended pose at [nowSec]; exactly [to] once finished. */
    fun poseAt(nowSec: Float): KittenPose {
        val e = (nowSec - startSec) / durationSec
        if (e >= 1f) return to
        fun progress(part: Part): Float {
            val w = kind.window(part)
            return kind.ease(((e - w.start) / (w.endInclusive - w.start)).coerceIn(0f, 1f))
        }
        val tFur = progress(Part.FUR)
        val pose = KittenPose.blend(from, to, tFur, progress(Part.EARS), progress(Part.FACE))
        val fromFront = from.tailFront >= 0.5f
        val toFront = to.tailFront >= 0.5f
        if (fromFront == toFront) return pose
        // The tail wraps to the front (asleep) or back via a low side pose, so it never slides through the body.
        val down = KittenShapes.TailDown
        val first = tFur < 0.5f
        val u = if (first) tFur * 2f else (tFur - 0.5f) * 2f
        val tail = List(down.size) {
            if (first) from.tail[it] + (down[it] - from.tail[it]) * u else down[it] + (to.tail[it] - down[it]) * u
        }
        return pose.copy(tail = tail, tailFront = if (if (first) fromFront else toFront) 1f else 0f)
    }
}

/** Per-frame effects drawn on top of a pose. */
data class KittenFx(
    val time: Float = 0f,
    /** False draws the still pose: no sway, tremble, breathing or twinkle. */
    val idle: Boolean = false,
    val blink: Float = 0f,
    val twitchL: Float = 0f,
    val twitchR: Float = 0f,
    /** Giggle after a poke, 0..1: eyes squeeze into happy arcs, cheeks glow, mouth opens. */
    val giggle: Float = 0f,
    /** Both ears flicking back after a poke, 0..1. */
    val earFlick: Float = 0f,
    /** Extra tail rotation in degrees for the swish after a poke. */
    val tailSwish: Float = 0f,
) {
    companion object {
        val Still = KittenFx()
    }
}

/**
 * Holds the kitten's mood, runs pose transitions and the random idle motion (blinks, ear twitches).
 * Pure Kotlin: the caller feeds it time via [advance]. A new mood during a transition starts from the pose shown now.
 */
class KittenAnimator(initial: Mood?, private val random: Random = Random.Default) {
    private var mood: Mood? = initial
    private var transition: KittenTransition? = null
    private var clock = 0f
    private var nextBlink = 1.5f
    private var blinkStart = -10f
    private var twitchStart = -10f
    private var twitchLeft = true
    private var nextTwitch = 2f
    private val giggleEnv = PokeEnvelope(attackSec = 0.06f, holdSec = 0.2f, releaseSec = 0.2f)
    private val flickEnv = PokeEnvelope(attackSec = 0.05f, holdSec = 0.08f, releaseSec = 0.22f)
    private val swishEnv = PokeEnvelope(attackSec = 0.05f, holdSec = 0.15f, releaseSec = 0.35f)

    /** The giggle level as of the last [advance], 0..1. */
    val giggle: Float get() = giggleEnv.value

    /** The pose being shown. */
    var pose: KittenPose = KittenPoses.of(initial)
        private set

    /** The idle effects as of the last [advance]; still until the first one. */
    var fx: KittenFx = KittenFx.Still
        private set

    /** Moves towards [newMood]; with [animate] false it jumps there. */
    fun setMood(newMood: Mood?, animate: Boolean = true) {
        if (newMood == mood && transition == null) return
        val target = KittenPoses.of(newMood)
        if (animate) {
            transition = KittenTransition(pose, target, kittenTransitionKind(mood, newMood), clock)
        } else {
            transition = null
            pose = target
        }
        mood = newMood
        if (!newMood.isCalm) {
            giggleEnv.release()
            flickEnv.release()
            swishEnv.release()
        }
    }

    /** A pat: in a calm mood the kitten giggles, flicks its ears back and swishes its tail; otherwise nothing. */
    fun poke() {
        if (!mood.isCalm) return
        giggleEnv.trigger()
        flickEnv.trigger()
        swishEnv.trigger()
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
        // Ear twitches are random, and more frequent in tense poses.
        if (clock > nextTwitch) {
            if (random.nextFloat() < pose.twitch * 2f) {
                twitchStart = clock
                twitchLeft = random.nextFloat() < 0.5f
            }
            nextTwitch = clock + 0.5f
        }
        val tp = (clock - twitchStart) / 0.26f
        val tw = if (tp in 0f..1f) -14f * sin(PI.toFloat() * tp) else 0f
        giggleEnv.advance(dtSec)
        flickEnv.advance(dtSec)
        swishEnv.advance(dtSec)
        // The swish phase runs on the clock; its amplitude eases in and out, so re-pokes never jump.
        val swish = 14f * swishEnv.value * sin(clock * PI.toFloat() * 2f * 2.6f)
        fx = KittenFx(
            time = clock,
            idle = true,
            blink = blink,
            twitchL = if (twitchLeft) tw else 0f,
            twitchR = if (twitchLeft) 0f else tw,
            giggle = giggleEnv.value,
            earFlick = flickEnv.value,
            tailSwish = swish,
        )
    }
}
