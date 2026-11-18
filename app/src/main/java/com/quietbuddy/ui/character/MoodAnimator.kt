package com.quietbuddy.ui.character

import com.quietbuddy.core.Mood
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** How a mood change feels; each character times its body parts and easing per kind. */
enum class TransitionKind(val defaultDurationMs: Int) {
    /** Towards a louder mood: a quick startle. */
    LOUDER(180),

    /** Towards a calmer mood: a slow relax. */
    CALMER(900),

    /** Any mood to asleep. */
    SLEEP(1400),

    /** Asleep to any mood. */
    WAKE(1400);

    /** The usual easing for this kind; a character may use its own. */
    fun ease(t: Float): Float = when (this) {
        LOUDER -> 1f - (1f - t).pow(3)
        CALMER -> if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
        SLEEP -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f
        WAKE -> 1f - (1f - t).pow(2)
    }

    companion object {
        /** The kind of change between two moods (null = asleep). */
        fun between(from: Mood?, to: Mood?): TransitionKind = when {
            to == null -> SLEEP
            from == null -> WAKE
            to.ordinal > from.ordinal -> LOUDER
            else -> CALMER
        }
    }
}

/**
 * A blend from one pose to another, timed by the seconds since it started (so it never deals with large clock
 * values). Subclasses blend their body parts on their own windows within the whole transition.
 */
abstract class PoseTransition<P>(val from: P, val to: P, val kind: TransitionKind, durationMs: Int) {
    val durationSec = durationMs / 1000f

    fun isFinished(elapsedSec: Float): Boolean = elapsedSec >= durationSec

    /** The blended pose [elapsedSec] after the start; exactly [to] once finished. */
    fun poseAt(elapsedSec: Float): P {
        val e = elapsedSec / durationSec
        return if (e >= 1f) to else blend(e)
    }

    /** The pose at progress [e], 0 until 1. */
    protected abstract fun blend(e: Float): P

    /** How far a part moving during [window] (fractions of the whole) has got at progress [e], eased. */
    protected fun partProgress(e: Float, window: ClosedFloatingPointRange<Float>, ease: (Float) -> Float): Float =
        ease(((e - window.start) / (window.endInclusive - window.start)).coerceIn(0f, 1f))
}

/** Something holding clock timestamps, which must move back when the clock is rebased. */
interface ClockStamped {
    fun shiftBy(sec: Float)
}

/** Random blinks: one every [gapSec] plus up to [jitterSec], each lasting 0.16 s. */
class Blinker(
    private val random: Random,
    private val gapSec: Float = 2.2f,
    private val jitterSec: Float = 3.2f,
    firstSec: Float = 1.5f,
) : ClockStamped {
    private var next = firstSec
    private var start = -10f

    /** The blink at [clock], 0 open .. 1 shut. */
    fun at(clock: Float): Float {
        if (clock > next) {
            start = clock
            next = clock + gapSec + random.nextFloat() * jitterSec
        }
        val p = (clock - start) / 0.16f
        return if (p in 0f..1f) sin(PI.toFloat() * p) else 0f
    }

    override fun shiftBy(sec: Float) {
        next -= sec
        start -= sec
    }
}

/**
 * Small random twitches (an ear, a head, a glance) lasting [durationSec]. Every half second one starts with a
 * chance of twice the pose's twitchiness, to a random side.
 */
class Twitcher(private val random: Random, private val durationSec: Float) : ClockStamped {
    private var next = 2f
    private var start = -10f

    /** -1 or 1: the side of the current or last twitch. */
    var direction = 1f
        private set

    /** The twitch at [clock] for a pose with [twitchiness], 0..1 (a half sine). */
    fun at(clock: Float, twitchiness: Float): Float {
        if (clock > next) {
            if (random.nextFloat() < twitchiness * 2f) {
                start = clock
                direction = if (random.nextFloat() < 0.5f) -1f else 1f
            }
            next = clock + 0.5f
        }
        val p = (clock - start) / durationSec
        return if (p in 0f..1f) sin(PI.toFloat() * p) else 0f
    }

    override fun shiftBy(sec: Float) {
        next -= sec
        start -= sec
    }
}

/**
 * Seconds since the animation began, kept small: every [REBASE_SEC] it moves back by that much and the
 * timestamps registered with [track] move with it. A plain Float clock lost precision after about a day and a
 * half on screen, and around three days in, a frame's time step rounded to nothing and the animation froze.
 */
class AnimationClock {
    var seconds = 0f
        private set

    private val stamped = mutableListOf<ClockStamped>()

    fun <T : ClockStamped> track(item: T): T = item.also { stamped += it }

    /** Moves on by [dtSec]; returns how far the clock was moved back (0 almost always). */
    fun advance(dtSec: Float): Float {
        seconds += dtSec
        if (seconds < REBASE_SEC) return 0f
        seconds -= REBASE_SEC
        stamped.forEach { it.shiftBy(REBASE_SEC) }
        return REBASE_SEC
    }

    companion object {
        /** An hour: the clock never passes this, so a Float keeps well under a millisecond of precision. */
        const val REBASE_SEC = 3600f
    }
}

/**
 * The common part of an animated character: its mood, the pose transitions between moods, the clock and blinks.
 * Pure Kotlin; the caller feeds it time via [advance]. A new mood during a transition starts from the pose shown now.
 * Subclasses add their idle motion and pat reactions in [update].
 */
abstract class MoodAnimator<P>(
    initial: Mood?,
    initialPose: P,
    protected val random: Random,
    blinkGapSec: Float = 2.2f,
) {
    protected var mood: Mood? = initial
        private set

    /** The pose being shown. */
    var pose: P = initialPose
        private set

    private var transition: PoseTransition<P>? = null
    private var transitionSec = 0f

    private val time = AnimationClock()
    private val blinker = time.track(Blinker(random, gapSec = blinkGapSec))

    /** Seconds since the start, moved back an hour at a time; timestamps compared with it need [onClockRebased]. */
    protected val clock: Float get() = time.seconds

    /** Registers a helper whose timestamps must follow the clock when it is rebased. */
    protected fun <T : ClockStamped> track(item: T): T = time.track(item)

    protected abstract fun poseOf(mood: Mood?): P

    protected abstract fun transition(from: P, to: P, kind: TransitionKind): PoseTransition<P>

    /** Called after the mood has changed; calm-only reactions stop here. */
    protected open fun onMoodChanged(newMood: Mood?) {}

    /** Called when the clock moved back by [sec]: move any stored timestamps back by the same amount. */
    protected open fun onClockRebased(sec: Float) {}

    /** Moves the idle motion on by [dtSec] once the pose and [blink] are up to date. */
    protected abstract fun update(dtSec: Float, blink: Float)

    /** Moves towards [newMood]; with [animate] false it jumps there. */
    fun setMood(newMood: Mood?, animate: Boolean = true) {
        if (newMood == mood && transition == null) return
        val target = poseOf(newMood)
        if (animate) {
            transition = transition(pose, target, TransitionKind.between(mood, newMood))
            transitionSec = 0f
        } else {
            transition = null
            pose = target
        }
        mood = newMood
        onMoodChanged(newMood)
    }

    /** Moves time on by [dtSec] and refreshes [pose] and the character's effects. */
    fun advance(dtSec: Float) {
        val rebased = time.advance(dtSec)
        if (rebased > 0f) onClockRebased(rebased)
        transition?.let {
            transitionSec += dtSec
            pose = it.poseAt(transitionSec)
            if (it.isFinished(transitionSec)) transition = null
        }
        update(dtSec, blinker.at(clock))
    }
}
