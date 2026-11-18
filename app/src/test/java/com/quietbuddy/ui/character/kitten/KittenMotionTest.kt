package com.quietbuddy.ui.character.kitten

import com.quietbuddy.ui.character.TransitionKind
import kotlin.math.roundToInt
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.assertIdleRhythmsNeverSkip
import kotlin.random.Random
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

class KittenMotionTest {
    private val moods = listOf<Mood?>(Mood.HAPPY, Mood.FINE, Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)

    @Test
    fun lerpEndpointsAreThePoses() {
        for (a in moods) for (b in moods) {
            val pa = KittenPoses.of(a)
            val pb = KittenPoses.of(b)
            assertEquals(pa, KittenPose.lerp(pa, pb, 0f))
            assertEquals(pb, KittenPose.lerp(pa, pb, 1f))
        }
    }

    @Test
    fun lerpMidpointIsBetween() {
        val mid = KittenPose.lerp(KittenPoses.of(Mood.FINE), KittenPoses.of(Mood.SAD), 0.5f)
        assertEquals(-24f, mid.ear, 1e-4f)
    }


    @Test
    fun durationsMatchTheDesign() {
        assertEquals(180, durationMs(TransitionKind.LOUDER))
        assertEquals(900, durationMs(TransitionKind.CALMER))
        assertEquals(1400, durationMs(TransitionKind.SLEEP))
        assertEquals(1400, durationMs(TransitionKind.WAKE))
    }

    @Test
    fun finishedTransitionEqualsTarget() {
        for (from in moods) for (to in moods) {
            if (from == to) continue
            val animator = KittenAnimator(from, Random(1))
            animator.setMood(to)
            repeat(100) { animator.advance(0.05f) }
            assertEquals("$from -> $to", KittenPoses.of(to), animator.pose)
        }
    }

    @Test
    fun jumpWithoutAnimationShowsTargetAtOnce() {
        val animator = KittenAnimator(Mood.HAPPY, Random(1))
        animator.setMood(Mood.EARS_COVERED, animate = false)
        assertEquals(KittenPoses.of(Mood.EARS_COVERED), animator.pose)
    }

    @Test
    fun interruptingStartsFromTheBlendedPose() {
        val animator = KittenAnimator(Mood.HAPPY, Random(1))
        animator.setMood(Mood.SAD) // louder
        animator.advance(0.09f)
        val mid = animator.pose
        assertNotEquals(KittenPoses.of(Mood.HAPPY), mid)
        assertNotEquals(KittenPoses.of(Mood.SAD), mid)

        animator.setMood(Mood.FINE) // calmer, from where the kitten is now
        // No time has passed, so the transition is still at its start: the pose must not jump.
        animator.advance(0f)
        assertEquals(mid, animator.pose)

        animator.advance(0.05f)
        assertNotEquals(mid, animator.pose)
        repeat(100) { animator.advance(0.05f) }
        assertEquals(KittenPoses.of(Mood.FINE), animator.pose)
    }

    @Test
    fun tailRoutesThroughDownPoseWhenGoingToSleep() {
        val from = KittenPoses.of(Mood.HAPPY)
        val to = KittenPoses.of(null)
        val t = KittenTransition(from, to, TransitionKind.SLEEP)
        // Fur window is 0..0.9 of 1.4 s, so its midpoint is 0.63 s: the tail should be the low side pose.
        val mid = t.poseAt(0.63f)
        assertEquals(KittenShapes.TailDown, mid.tail.map { Math.round(it * 10f) / 10f })
        assertEquals(0f, t.poseAt(0.3f).tailFront, 0f)
        assertEquals(1f, t.poseAt(1.0f).tailFront, 0f)
    }

    @Test
    fun idleRhythmsNeverSkipOnAMoodChange() {
        val a = KittenAnimator(Mood.FINE, Random(1))
        // the fastest sway is the worried one, at 1.6 Hz
        assertIdleRhythmsNeverSkip(
            setMood = { a.setMood(it) },
            advance = { a.advance(it) },
            angles = { with(a) { listOf(fx.swayPhase) } },
            maxHz = 1.6f,
        )
    }

    private fun durationMs(kind: TransitionKind): Int =
        (KittenTransition(KittenPoses.of(null), KittenPoses.of(null), kind).durationSec * 1000).roundToInt()

}
