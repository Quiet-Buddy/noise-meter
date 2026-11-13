package com.quietbuddy.ui.character.duck

import com.quietbuddy.core.Mood
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DuckMotionTest {
    private val moods = listOf<Mood?>(Mood.HAPPY, Mood.FINE, Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)

    @Test
    fun everyPoseHasTheSameListSizes() {
        val base = DuckPoses.of(Mood.FINE)
        for (m in moods) {
            val p = DuckPoses.of(m)
            assertEquals("$m closed", base.closed.size, p.closed.size)
            assertEquals("$m browN", base.browN.size, p.browN.size)
            assertEquals("$m browF", base.browF.size, p.browF.size)
        }
        assertEquals(6, base.closed.size)
        assertEquals(DuckShapes.WingRest.size, DuckShapes.WingCover.size)
        assertEquals(DuckShapes.FarRest.size, DuckShapes.FarCover.size)
    }

    @Test
    fun lerpEndpointsAreThePoses() {
        for (a in moods) for (b in moods) {
            val pa = DuckPoses.of(a)
            val pb = DuckPoses.of(b)
            assertEquals(pa, DuckPose.lerp(pa, pb, 0f))
            assertEquals(pb, DuckPose.lerp(pa, pb, 1f))
        }
    }

    @Test
    fun lerpMidpointIsBetween() {
        val mid = DuckPose.lerp(DuckPoses.of(Mood.FINE), DuckPoses.of(Mood.SAD), 0.5f)
        assertEquals(5f, mid.tilt, 1e-4f)
        assertEquals(6.5f, mid.tailDroop, 1e-4f)
    }

    @Test
    fun blendMovesEachPartOnItsOwnProgress() {
        val a = DuckPoses.of(Mood.FINE)
        val b = DuckPoses.of(Mood.SAD)
        val p = DuckPose.blend(a, b, 1f, 0f, 0.5f)
        assertEquals(b.tailDroop, p.tailDroop, 0f)
        assertEquals(a.tilt, p.tilt, 0f)
        assertEquals(0.5f * (a.cheek + b.cheek), p.cheek, 1e-4f)
    }

    @Test
    fun gigglePutsOnThePatFace() {
        val fine = DuckPoses.of(Mood.FINE)
        assertEquals(fine, fine.giggled(0f))
        val full = fine.giggled(1f)
        assertEquals(0f, full.open, 0f)
        assertEquals(1f, full.mouthOpen, 0f)
        assertEquals(DuckShapes.ClosedHappy, full.closed)
        assertEquals(fine.tilt, full.tilt, 0f)
    }

    @Test
    fun kindFollowsMoodLevel() {
        assertEquals(DuckTransitionKind.LOUDER, duckTransitionKind(Mood.HAPPY, Mood.WORRIED))
        assertEquals(DuckTransitionKind.LOUDER, duckTransitionKind(Mood.SAD, Mood.EARS_COVERED))
        assertEquals(DuckTransitionKind.CALMER, duckTransitionKind(Mood.EARS_COVERED, Mood.SAD))
        assertEquals(DuckTransitionKind.CALMER, duckTransitionKind(Mood.FINE, Mood.HAPPY))
        assertEquals(DuckTransitionKind.SLEEP, duckTransitionKind(Mood.HAPPY, null))
        assertEquals(DuckTransitionKind.WAKE, duckTransitionKind(null, Mood.EARS_COVERED))
    }

    @Test
    fun durationsMatchTheDesign() {
        assertEquals(180, DuckTransitionKind.LOUDER.durationMs)
        assertEquals(900, DuckTransitionKind.CALMER.durationMs)
        assertEquals(1400, DuckTransitionKind.SLEEP.durationMs)
        assertEquals(1400, DuckTransitionKind.WAKE.durationMs)
    }

    @Test
    fun finishedTransitionEqualsTarget() {
        for (from in moods) for (to in moods) {
            if (from == to) continue
            val animator = DuckAnimator(from, Random(1))
            animator.setMood(to)
            repeat(100) { animator.advance(0.05f) }
            assertEquals("$from -> $to", DuckPoses.of(to), animator.pose)
        }
    }

    @Test
    fun headLagsTheFaceWhenCalmingDown() {
        val t = DuckTransition(DuckPoses.of(Mood.SAD), DuckPoses.of(Mood.FINE), DuckTransitionKind.CALMER, 0f)
        // At 10% the face window (0..0.55) has begun but the head window (0.2..1) has not.
        val early = t.poseAt(0.09f)
        assertEquals(DuckPoses.of(Mood.SAD).tilt, early.tilt, 0f)
        assertNotEquals(DuckPoses.of(Mood.SAD).smile, early.smile)
    }

    @Test
    fun jumpWithoutAnimationShowsTargetAtOnce() {
        val animator = DuckAnimator(Mood.HAPPY, Random(1))
        animator.setMood(Mood.EARS_COVERED, animate = false)
        assertEquals(DuckPoses.of(Mood.EARS_COVERED), animator.pose)
    }

    @Test
    fun interruptingStartsFromTheBlendedPose() {
        val animator = DuckAnimator(Mood.HAPPY, Random(1))
        animator.setMood(Mood.SAD) // louder
        animator.advance(0.09f)
        val mid = animator.pose
        assertNotEquals(DuckPoses.of(Mood.HAPPY), mid)
        assertNotEquals(DuckPoses.of(Mood.SAD), mid)

        animator.setMood(Mood.FINE) // calmer, from where the duck is now
        // No time has passed, so the transition is still at its start: the pose must not jump.
        animator.advance(0f)
        assertEquals(mid, animator.pose)

        animator.advance(0.05f)
        assertNotEquals(mid, animator.pose)
        repeat(100) { animator.advance(0.05f) }
        assertEquals(DuckPoses.of(Mood.FINE), animator.pose)
    }
}
