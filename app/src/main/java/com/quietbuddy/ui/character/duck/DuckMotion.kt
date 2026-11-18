package com.quietbuddy.ui.character.duck

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.IdlePhase
import com.quietbuddy.ui.character.MoodAnimator
import com.quietbuddy.ui.character.PoseTransition
import com.quietbuddy.ui.character.TransitionKind
import com.quietbuddy.ui.character.Twitcher
import com.quietbuddy.ui.character.PokeEnvelope
import com.quietbuddy.ui.character.isCalm
import kotlin.random.Random

/** The parts of the duck that move on their own schedule during a transition. */
private enum class Part { BODY, HEAD, FACE }

/** When each part moves, as a fraction of the whole transition. */
private fun TransitionKind.window(part: Part): ClosedFloatingPointRange<Float> = when (this) {
    TransitionKind.LOUDER -> when (part) { Part.BODY -> 0f..1f; Part.HEAD -> 0f..0.7f; Part.FACE -> 0f..0.6f }
    TransitionKind.CALMER -> when (part) { Part.BODY -> 0f..1f; Part.HEAD -> 0.2f..1f; Part.FACE -> 0f..0.55f }
    TransitionKind.SLEEP -> when (part) { Part.BODY -> 0f..0.9f; Part.HEAD -> 0.1f..0.8f; Part.FACE -> 0.35f..1f }
    TransitionKind.WAKE -> when (part) { Part.BODY -> 0.25f..1f; Part.HEAD -> 0.15f..0.8f; Part.FACE -> 0f..0.45f }
}

/** A blend from one pose to another. */
class DuckTransition(from: DuckPose, to: DuckPose, kind: TransitionKind) :
    PoseTransition<DuckPose>(from, to, kind, kind.defaultDurationMs) {

    override fun blend(e: Float): DuckPose {
        fun progress(part: Part) = partProgress(e, kind.window(part), kind::ease)
        return DuckPose.blend(from, to, progress(Part.BODY), progress(Part.HEAD), progress(Part.FACE))
    }
}

/** Per-frame effects drawn on top of a pose. */
data class DuckFx(
    val time: Double = 0.0,
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
class DuckAnimator(initial: Mood?, random: Random = Random.Default) :
    MoodAnimator<DuckPose>(initial, DuckPoses.of(initial), random) {
    // Small glances or twitches to the side: random, more often in tense poses.
    private val twitcher = track(Twitcher(random, durationSec = 0.35f))
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

    /** The idle effects as of the last [advance]; still until the first one. */
    var fx: DuckFx = DuckFx.Still
        private set

    override fun poseOf(mood: Mood?) = DuckPoses.of(mood)

    override fun transition(from: DuckPose, to: DuckPose, kind: TransitionKind) = DuckTransition(from, to, kind)

    override fun onMoodChanged(newMood: Mood?) {
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

    override fun update(dtSec: Float, blink: Float) {
        val twitch = twitcher.direction * 8f * twitcher.at(clock, pose.twitch)
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
            time = waveTime,
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
