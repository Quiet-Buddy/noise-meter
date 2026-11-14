package com.quietbuddy.ui.character.mari

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.IdlePhase
import com.quietbuddy.ui.character.isCalm
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** How a transition feels: the direction decides its duration, per-part windows and easing. */
enum class MariTransitionKind(val durationMs: Int) {
    /** Towards a louder mood: the hands fly up first (to her ears, dropping the basket) and the tontorra follows. */
    LOUDER(220),

    /** Towards a calmer mood: her face relaxes first, then the tontorra settles. */
    CALMER(900),

    /** Any mood to asleep: she sets the basket down, the tontorra nods, then her eyes close. */
    SLEEP(1400),

    /** Asleep to any mood: the eyes open first and the body follows. */
    WAKE(1400),
}

/** The parts of Mari Domingi that move on their own schedule during a transition. */
private enum class Part { BODY, HEAD, FACE }

/** When each part moves, as a fraction of the whole transition. */
private fun MariTransitionKind.window(part: Part): ClosedFloatingPointRange<Float> = when (this) {
    MariTransitionKind.LOUDER -> when (part) { Part.BODY -> 0f..0.95f; Part.HEAD -> 0.05f..1f; Part.FACE -> 0f..0.7f }
    MariTransitionKind.CALMER -> when (part) { Part.BODY -> 0.15f..1f; Part.HEAD -> 0.1f..1f; Part.FACE -> 0f..0.5f }
    MariTransitionKind.SLEEP -> when (part) { Part.BODY -> 0f..0.7f; Part.HEAD -> 0.2f..0.9f; Part.FACE -> 0.45f..1f }
    MariTransitionKind.WAKE -> when (part) { Part.BODY -> 0.3f..1f; Part.HEAD -> 0.1f..0.8f; Part.FACE -> 0f..0.4f }
}

private fun MariTransitionKind.ease(t: Float): Float = when (this) {
    MariTransitionKind.LOUDER -> 1f - (1f - t).pow(2)
    MariTransitionKind.CALMER -> if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
    MariTransitionKind.SLEEP -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f
    MariTransitionKind.WAKE -> 1f - (1f - t).pow(2)
}

internal fun smooth(a: Float, b: Float, x: Float): Float {
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
fun mariTransitionKind(from: Mood?, to: Mood?): MariTransitionKind = when {
    to == null -> MariTransitionKind.SLEEP
    from == null -> MariTransitionKind.WAKE
    to.level > from.level -> MariTransitionKind.LOUDER
    else -> MariTransitionKind.CALMER
}

/** A blend from one pose to another that started at [startSec]. */
class MariTransition(
    val from: MariPose,
    val to: MariPose,
    val kind: MariTransitionKind,
    val startSec: Float,
) {
    private val durationSec = kind.durationMs / 1000f

    fun isFinished(nowSec: Float): Boolean = nowSec - startSec >= durationSec

    /** The blended pose at [nowSec]; exactly [to] once finished. */
    fun poseAt(nowSec: Float): MariPose {
        val e = (nowSec - startSec) / durationSec
        if (e >= 1f) return to
        fun progress(part: Part): Float {
            val w = kind.window(part)
            return kind.ease(((e - w.start) / (w.endInclusive - w.start)).coerceIn(0f, 1f))
        }
        return MariPose.blend(from, to, progress(Part.BODY), progress(Part.HEAD), progress(Part.FACE))
    }
}

/** The combo tricks, smallest first, and how long each one lasts. */
enum class MariTrick(val durationSec: Float) {
    /** 3 quick pats: an apple flies from the basket in a high arc into her raised hand; her eyes follow it. */
    TOSS(1.5f),

    /** 6: a crouch, a big jump with her arms up and the hook trailing, a squashy landing and a burst of petals. */
    HOP(1.1f),

    /** 9: an aurresku-style dance, four hop-kicks on alternate legs with the arms held high. */
    DANCE(3.2f),
}

/** Per-frame effects drawn on top of a pose. Hand and leg values are in the prototype's coordinates. */
data class MariFx(
    val time: Float = 0f,
    /** False draws the still pose: no bobbing, swaying, wobbling, trembling, breathing, flicker or twinkle. */
    val idle: Boolean = false,
    val blink: Float = 0f,
    /** Sideways glance of the eyes, in units. */
    val twitch: Float = 0f,
    /** The pat's delighted face (eyes shut, blush), 0..1, blended over the pose. */
    val patFace: Float = 0f,
    /** The open little smile she wears while watching a tossed apple, 0..1. */
    val watchFace: Float = 0f,
    /** Body squash from the springy catch (positive = squashed). */
    val squash: Float = 0f,
    /** Extra hook bend in degrees: the boing after a pat. */
    val boing: Float = 0f,
    /** Vertical offset of the whole body (negative = up) from a trick. */
    val hop: Float = 0f,
    /** Extra sway in degrees during the dance. */
    val dance: Float = 0f,
    /** Vertical stretch of the body (1 = none) for the jump's crouch, take-off and landing. */
    val stretch: Float = 1f,
    /** Head lean in degrees into a pat. */
    val headLean: Float = 0f,
    /** Extra hook bend in degrees from a trick. */
    val bendAdd: Float = 0f,
    /** Where a trick points her eyes, in units. */
    val lookX: Float = 0f,
    val lookY: Float = 0f,
    /** How far each leg kicks out sideways in the dance, 0..1. */
    val legL: Float = 0f,
    val legR: Float = 0f,
    /** How far a trick takes each hand (0 = the pose's own place, 1 = [handL] / [handR]). */
    val handKL: Float = 0f,
    val handKR: Float = 0f,
    val handLX: Float = 95f,
    val handLY: Float = 220f,
    val handRX: Float = 105f,
    val handRY: Float = 220f,
    /** Progress 0..1 of an apple toss, or negative when there is none. */
    val toss: Float = -1f,
    /** How much the apple she holds when happy hides while another one is tossed, 0..1. */
    val hideHandApple: Float = 0f,
    /** Progress 0..1 of the petal burst after the jump or the dance, or negative when there is none. */
    val burst: Float = -1f,
    /** Angles in radians of the idle rhythms; see [IdlePhase]. */
    val bobPhase: Float = 0f,
    val swayPhase: Float = 0f,
    val nodPhase: Float = 0f,
    val hookPhase: Float = 0f,
) {
    companion object {
        val Still = MariFx()
    }
}

/**
 * Holds Mari Domingi's mood, runs pose transitions, the random idle motion (blinks, glances) and the pat combos.
 * Pure Kotlin: the caller feeds it time via [advance]. A new mood during a transition starts from the pose shown now.
 *
 * Pats (see [poke]) only count in calm moods. Each kicks the hook's spring (so quick pats build on each other rather
 * than restarting), leans her head into the pat and gives her a delighted face; the body squash and the hearts come
 * from the home screen. Quick pats (less than [COMBO_GAP_SEC] apart) add up: 3 toss an apple, 6 jump, 9 dance, and
 * then the count carries on. A trick never cuts another off: a new one waits and plays right after, and a mood
 * change lets a running one finish. Everything a trick drives is eased, so nothing jumps between frames.
 */
class MariAnimator(initial: Mood?, private val random: Random = Random.Default) {
    private class Act(val trick: MariTrick, val start: Float) {
        var caught = false
    }

    /** A damped spring, integrated in small steps so it stays stable at any frame rate. */
    private class Spring(private val w: Float, private val damp: Float, private val limit: Float) {
        var x = 0f
        var v = 0f

        fun step(dt: Float) {
            val n = max(1, ceil(dt / 0.004f).toInt())
            val h = dt / n
            repeat(n) {
                v += (-w * w * x - damp * v) * h
                x += v * h
            }
            x = x.coerceIn(-limit, limit)
        }
    }

    private var mood: Mood? = initial
    private var transition: MariTransition? = null
    private var clock = 0f
    private var nextBlink = 1.5f
    private var blinkStart = -10f
    private var twitchStart = -10f
    private var twitchDir = 1f
    private var nextTwitch = 2f

    private var lastPat = -10f
    private var combo = 0
    private var act: Act? = null
    private var queued: MariTrick? = null
    private var burstAt = -10f

    private val squashSpring = Spring(w = 22f, damp = 12f, limit = 1.6f)
    private val boingSpring = Spring(w = 24f, damp = 9f, limit = 45f)

    // Trick outputs, low-pass filtered so starting, stopping or switching never snaps.
    private var hop = 0f
    private var dance = 0f
    private var headLean = 0f
    private var bendAdd = 0f
    private var stretch = 1f
    private var lookX = 0f
    private var lookY = 0f
    private var eyesOpen = 0f
    private var legL = 0f
    private var legR = 0f
    private var kL = 0f
    private var kR = 0f
    private var lx = 95f
    private var ly = 220f
    private var rx = 105f
    private var ry = 220f
    private var face = 0f
    private var hide = 0f

    private val bobPhase = IdlePhase()
    private val swayPhase = IdlePhase()
    private val nodPhase = IdlePhase()
    private val hookPhase = IdlePhase()

    /** The pose being shown, before the pat's face is blended over it (that is [MariFx.patFace]). */
    var pose: MariPose = MariPoses.of(initial)
        private set

    /** The idle effects as of the last [advance]; still until the first one. */
    var fx: MariFx = MariFx.Still
        private set

    /** The number of quick pats in the current combo. */
    val comboCount: Int get() = combo

    /** The trick under way, if any. */
    val trick: MariTrick? get() = act?.trick

    /** The trick waiting for the running one to finish, if any. */
    val queuedTrick: MariTrick? get() = queued

    /** Whether a trick is under way. */
    val isPerforming: Boolean get() = act != null

    /** Moves towards [newMood]; with [animate] false it jumps there. A running trick carries on either way. */
    fun setMood(newMood: Mood?, animate: Boolean = true) {
        if (newMood == mood && transition == null) return
        val target = MariPoses.of(newMood)
        if (animate) {
            transition = MariTransition(pose, target, mariTransitionKind(mood, newMood), clock)
        } else {
            transition = null
            pose = target
        }
        mood = newMood
        if (!newMood.isCalm) {
            // A delighted face makes no sense in a loud room: it eases away now. The combo is over too.
            lastPat = -10f
            combo = 0
        }
    }

    /** A pat: in a calm mood she leans into it, the hook boings and the combo counts; otherwise nothing. */
    fun poke() {
        if (!mood.isCalm) return
        combo = if (clock - lastPat < COMBO_GAP_SEC) combo + 1 else 1
        lastPat = clock
        // Kick the hook's spring rather than restarting it, so quick pats build on each other.
        boingSpring.v += 720f
        val trick = when (combo % DANCE_AT) {
            0 -> MariTrick.DANCE
            HOP_AT -> MariTrick.HOP
            TOSS_AT -> MariTrick.TOSS
            else -> return
        }
        if (act == null) {
            start(trick)
        } else {
            // Never cut a trick off mid-air: the new one (the biggest asked for) waits and plays right after.
            val q = queued
            if (q == null || trick.ordinal > q.ordinal) queued = trick
        }
    }

    private fun start(trick: MariTrick) {
        act = Act(trick, clock)
        if (trick == MariTrick.HOP) burstAt = clock + trick.durationSec * 0.72f
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
            nextBlink = clock + 2.4f + random.nextFloat() * 3.2f
        }
        val bp = (clock - blinkStart) / 0.16f
        val blink = if (bp in 0f..1f) sin(PI.toFloat() * bp) else 0f
        // Small glances to the side: random, more often when worried.
        if (clock > nextTwitch) {
            if (random.nextFloat() < pose.twitch * 2f) {
                twitchStart = clock
                twitchDir = if (random.nextFloat() < 0.5f) -1f else 1f
            }
            nextTwitch = clock + 0.5f
        }
        val tp = (clock - twitchStart) / 0.5f
        val twitch = if (tp in 0f..1f) twitchDir * 3f * sin(PI.toFloat() * tp) else 0f

        squashSpring.step(dtSec)
        boingSpring.step(dtSec)

        // The trick under way; when it ends, a queued one starts (only if the room is still calm).
        act?.let {
            if ((clock - it.start) / it.trick.durationSec >= 1f) {
                act = null
                val q = queued
                queued = null
                if (q != null && mood.isCalm) start(q)
            }
        }
        val out = Targets(lx, ly, rx, ry)
        act?.let { a -> a.drive((clock - a.start) / a.trick.durationSec, out) }

        val ps = clock - lastPat
        val leanTarget = if (ps >= 0f && ps < 0.5f) PAT_LEAN else 0f
        val faceTarget = if (ps >= 0f && ps < 0.75f) 1f else 0f
        fun eased(cur: Float, tgt: Float, tau: Float) = cur + (tgt - cur) * (1f - exp(-dtSec / tau))
        hop = eased(hop, out.hop, TAU)
        dance = eased(dance, out.dance, TAU)
        headLean = eased(headLean, leanTarget, if (abs(leanTarget) > abs(headLean)) 0.1f else 0.25f)
        bendAdd = eased(bendAdd, out.bendAdd, TAU)
        stretch = eased(stretch, out.stretch, TAU)
        lookX = eased(lookX, out.lookX, TAU)
        lookY = eased(lookY, out.lookY, TAU)
        eyesOpen = eased(eyesOpen, out.open, TAU)
        legL = eased(legL, out.legL, TAU)
        legR = eased(legR, out.legR, TAU)
        kL = eased(kL, out.kL, TAU)
        kR = eased(kR, out.kR, TAU)
        lx = eased(lx, out.lx, TAU)
        ly = eased(ly, out.ly, TAU)
        rx = eased(rx, out.rx, TAU)
        ry = eased(ry, out.ry, TAU)
        face = eased(face, faceTarget, if (faceTarget > face) 0.05f else 0.15f)
        hide = eased(hide, out.hide, TAU)

        // The idle rhythms advance by their current speed, so a mood change slows or speeds them without a skip.
        bobPhase.advance(pose.bobHz, dtSec)
        swayPhase.advance(pose.swayHz, dtSec)
        nodPhase.advance(pose.nodHz, dtSec)
        hookPhase.advance(pose.hookHz, dtSec)

        val bk = (clock - burstAt) / BURST_SEC
        fx = MariFx(
            time = clock,
            idle = true,
            blink = blink,
            twitch = twitch,
            patFace = face * (1f - eyesOpen.coerceIn(0f, 1f)),
            watchFace = out.watch,
            squash = squashSpring.x,
            boing = boingSpring.x,
            hop = hop,
            dance = dance,
            stretch = stretch,
            headLean = headLean,
            bendAdd = bendAdd,
            lookX = lookX,
            lookY = lookY,
            legL = legL,
            legR = legR,
            handKL = kL,
            handKR = kR,
            handLX = lx,
            handLY = ly,
            handRX = rx,
            handRY = ry,
            toss = out.toss,
            hideHandApple = hide,
            burst = if (bk >= 0f && bk < 1f) bk else -1f,
            bobPhase = bobPhase.radians,
            swayPhase = swayPhase.radians,
            nodPhase = nodPhase.radians,
            hookPhase = hookPhase.radians,
        )
    }

    /** What a trick asks for this frame, before easing. Hand targets hold where they are when a trick leaves them. */
    private class Targets(var lx: Float, var ly: Float, var rx: Float, var ry: Float) {
        var hop = 0f
        var dance = 0f
        var bendAdd = 0f
        var stretch = 1f
        var lookX = 0f
        var lookY = 0f
        var open = 0f
        var legL = 0f
        var legR = 0f
        var kL = 0f
        var kR = 0f
        var hide = 0f
        var watch = 0f
        var toss = -1f
    }

    /** Fills [o] with what this trick does at progress [e] (0..1). */
    private fun Act.drive(e: Float, o: Targets) {
        val pi = PI.toFloat()
        when (trick) {
            MariTrick.TOSS -> {
                // 0-0.75: the apple flies from the basket in a high arc into her raised right hand; she watches it.
                o.toss = e
                o.hide = smooth(0f, 0.06f, e) * (1f - smooth(0.9f, 1f, e))
                o.kR = smooth(0.25f, 0.55f, e) * (1f - smooth(0.92f, 1f, e))
                o.rx = 134f
                o.ry = 182f
                o.open = smooth(0f, 0.1f, e) * (1f - smooth(0.85f, 1f, e))
                o.watch = smooth(0f, 0.15f, e) * (1f - smooth(0.7f, 0.8f, e))
                if (e > 0.75f && !caught) {
                    caught = true
                    squashSpring.v -= 10f // a little bob as she catches it
                }
            }
            MariTrick.HOP -> {
                // Crouch, jump (stretched, arms up, hook trailing), land with a squash.
                fun air(u: Float) = 1f + 0.1f * cos(pi * u) * cos(pi * u)
                when {
                    e < 0.2f -> {
                        val c = sin(pi / 2f * e / 0.2f)
                        o.stretch = 1f - 0.13f * c
                        o.hop = 4f * c
                    }
                    e < 0.72f -> {
                        val u = (e - 0.2f) / 0.52f
                        val k = smooth(0f, 0.18f, u)
                        o.hop = -52f * sin(pi * u) + 4f * (1f - k)
                        o.stretch = lerp(0.87f, air(u), k)
                        o.bendAdd = 30f * sin(2f * pi * u)
                        o.kL = sin(pi * u)
                        o.kR = o.kL
                        o.lx = 66f
                        o.ly = 172f
                        o.rx = 134f
                        o.ry = 172f
                    }
                    else -> {
                        val v = (e - 0.72f) / 0.28f
                        val land = 1f - 0.16f * sin(pi * min(1f, v * 1.5f)) * (1f - v)
                        o.stretch = lerp(air(1f), land, smooth(0f, 0.2f, v))
                    }
                }
            }
            MariTrick.DANCE -> {
                // Aurresku-style: four hop-kicks on alternate legs with the arms held high.
                val env = smooth(0f, 0.08f, e) * (1f - smooth(0.92f, 1f, e))
                val beat = min(3f, floor(e * 4f)).toInt()
                val v = (e * 4f) % 1f
                val side = if (beat % 2 == 1) 1f else -1f
                val s = sin(pi * v)
                o.hop = -16f * s * env
                if (side < 0f) o.legL = s * env else o.legR = s * env
                o.dance = 6f * side * s * env
                o.bendAdd = -22f * side * s * env
                // The arm on the kicking side reaches higher.
                val lift = 8f * s
                o.kL = env
                o.kR = env
                o.lx = 62f
                o.ly = 160f - (if (side < 0f) lift else 0f)
                o.rx = 138f
                o.ry = 160f - (if (side > 0f) lift else 0f)
                if (e > 0.9f && burstAt < start) burstAt = clock
            }
        }
    }

    companion object {
        /** Pats closer together than this build a combo. */
        const val COMBO_GAP_SEC = 0.7f
        const val TOSS_AT = 3
        const val HOP_AT = 6
        const val DANCE_AT = 9
        const val BURST_SEC = 0.9f

        /** Degrees her head leans into a pat (the app doesn't say where the finger landed, so always one way). */
        private const val PAT_LEAN = 7f

        /** Time constant of the low-pass filter on trick outputs, in seconds. */
        private const val TAU = 0.05f
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
