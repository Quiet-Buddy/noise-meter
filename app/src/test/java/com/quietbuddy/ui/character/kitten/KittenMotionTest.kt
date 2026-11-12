package com.quietbuddy.ui.character.kitten

import com.quietbuddy.core.Mood
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
    fun kindFollowsMoodLevel() {
        assertEquals(KittenTransitionKind.LOUDER, kittenTransitionKind(Mood.HAPPY, Mood.WORRIED))
        assertEquals(KittenTransitionKind.LOUDER, kittenTransitionKind(Mood.SAD, Mood.EARS_COVERED))
        assertEquals(KittenTransitionKind.CALMER, kittenTransitionKind(Mood.EARS_COVERED, Mood.SAD))
        assertEquals(KittenTransitionKind.CALMER, kittenTransitionKind(Mood.FINE, Mood.HAPPY))
        assertEquals(KittenTransitionKind.SLEEP, kittenTransitionKind(Mood.HAPPY, null))
        assertEquals(KittenTransitionKind.WAKE, kittenTransitionKind(null, Mood.EARS_COVERED))
    }

    @Test
    fun durationsMatchTheDesign() {
        assertEquals(180, KittenTransitionKind.LOUDER.durationMs)
        assertEquals(900, KittenTransitionKind.CALMER.durationMs)
        assertEquals(1400, KittenTransitionKind.SLEEP.durationMs)
        assertEquals(1400, KittenTransitionKind.WAKE.durationMs)
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
        val t = KittenTransition(from, to, KittenTransitionKind.SLEEP, 0f)
        // Fur window is 0..0.9 of 1.4 s, so its midpoint is 0.63 s: the tail should be the low side pose.
        val mid = t.poseAt(0.63f)
        assertEquals(KittenShapes.TailDown, mid.tail.map { Math.round(it * 10f) / 10f })
        assertEquals(0f, t.poseAt(0.3f).tailFront, 0f)
        assertEquals(1f, t.poseAt(1.0f).tailFront, 0f)
    }
}
