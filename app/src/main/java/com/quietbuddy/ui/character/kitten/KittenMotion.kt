package com.quietbuddy.ui.character.kitten

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.IdlePhase
import com.quietbuddy.ui.character.MoodAnimator
import com.quietbuddy.ui.character.PokeEnvelope
import com.quietbuddy.ui.character.PoseTransition
import com.quietbuddy.ui.character.TransitionKind
import com.quietbuddy.ui.character.Twitcher
import com.quietbuddy.ui.character.isCalm
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** The parts of the body that move on their own schedule during a transition. */
private enum class Part { FUR, EARS, FACE }

/** When each part moves, as a fraction of the whole transition. */
private fun TransitionKind.window(part: Part): ClosedFloatingPointRange<Float> = when (this) {
    TransitionKind.LOUDER -> when (part) { Part.FUR -> 0f..1f; Part.EARS -> 0f..0.8f; Part.FACE -> 0f..0.6f }
    TransitionKind.CALMER -> when (part) { Part.FUR -> 0f..1f; Part.EARS -> 0.3f..1f; Part.FACE -> 0f..0.55f }
    TransitionKind.SLEEP -> when (part) { Part.FUR -> 0f..0.9f; Part.EARS -> 0.1f..0.7f; Part.FACE -> 0.35f..1f }
    TransitionKind.WAKE -> when (part) { Part.FUR -> 0.25f..1f; Part.EARS -> 0.2f..0.75f; Part.FACE -> 0f..0.45f }
}

/** A blend from one pose to another. */
class KittenTransition(from: KittenPose, to: KittenPose, kind: TransitionKind) :
    PoseTransition<KittenPose>(from, to, kind, kind.defaultDurationMs) {

    override fun blend(e: Float): KittenPose {
        fun progress(part: Part) = partProgress(e, kind.window(part), kind::ease)
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
    /** Angle in radians of the idle sway; see [IdlePhase]. */
    val swayPhase: Float = 0f,
) {
    companion object {
        val Still = KittenFx()
    }
}

/**
 * The kitten's idle motion (ear twitches, sway) and its reaction to a pat, on top of [MoodAnimator]'s mood,
 * transitions and blinks.
 */
class KittenAnimator(initial: Mood?, random: Random = Random.Default) :
    MoodAnimator<KittenPose>(initial, KittenPoses.of(initial), random) {
    // Ear twitches are random, and more frequent in tense poses.
    private val twitcher = track(Twitcher(random, durationSec = 0.26f))
    private val giggleEnv = PokeEnvelope(attackSec = 0.06f, holdSec = 0.2f, releaseSec = 0.2f)
    private val flickEnv = PokeEnvelope(attackSec = 0.05f, holdSec = 0.08f, releaseSec = 0.22f)
    private val swishEnv = PokeEnvelope(attackSec = 0.05f, holdSec = 0.15f, releaseSec = 0.35f)
    private val swayPhase = IdlePhase()

    /** The giggle level as of the last [advance], 0..1. */
    val giggle: Float get() = giggleEnv.value

    /** The idle effects as of the last [advance]; still until the first one. */
    var fx: KittenFx = KittenFx.Still
        private set

    override fun poseOf(mood: Mood?) = KittenPoses.of(mood)

    override fun transition(from: KittenPose, to: KittenPose, kind: TransitionKind) = KittenTransition(from, to, kind)

    override fun onMoodChanged(newMood: Mood?) {
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

    override fun update(dtSec: Float, blink: Float) {
        val tw = -14f * twitcher.at(clock, pose.twitch)
        val left = twitcher.direction < 0f
        giggleEnv.advance(dtSec)
        flickEnv.advance(dtSec)
        swishEnv.advance(dtSec)
        swayPhase.advance(pose.swayHz, dtSec)
        // The swish phase runs on the clock; its amplitude eases in and out, so re-pokes never jump.
        val swish = 14f * swishEnv.value * sin(clock * PI.toFloat() * 2f * 2.6f)
        fx = KittenFx(
            time = clock,
            idle = true,
            blink = blink,
            twitchL = if (left) tw else 0f,
            twitchR = if (left) 0f else tw,
            giggle = giggleEnv.value,
            earFlick = flickEnv.value,
            tailSwish = swish,
            swayPhase = swayPhase.radians,
        )
    }
}
