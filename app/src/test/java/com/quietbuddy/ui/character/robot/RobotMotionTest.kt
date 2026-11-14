package com.quietbuddy.ui.character.robot

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.assertIdleRhythmsNeverSkip
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RobotMotionTest {
    private val moods = listOf<Mood?>(Mood.HAPPY, Mood.FINE, Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)

    private fun animator(mood: Mood? = Mood.FINE) = RobotAnimator(mood, Random(1))

    private fun RobotAnimator.run(seconds: Float, step: Float = 1f / 60f) {
        var left = seconds
        while (left > 0f) {
            advance(minOf(step, left))
            left -= step
        }
    }

    @Test
    fun everyPoseHasTheSameListSizes() {
        val base = RobotPoses.of(Mood.FINE)
        for (m in moods) {
            val p = RobotPoses.of(m)
            assertEquals("$m closed", base.closed.size, p.closed.size)
            assertEquals("$m browL", base.browL.size, p.browL.size)
            assertEquals("$m browR", base.browR.size, p.browR.size)
            assertEquals("$m bulb", 3, p.bulb.size)
        }
        assertEquals(6, base.closed.size)
    }

    @Test
    fun posesMatchThePrototype() {
        val happy = RobotPoses.of(Mood.HAPPY)
        assertEquals(4.5f, happy.hover, 0f)
        assertEquals(-14f, happy.handLift, 0f)
        assertEquals(RobotShapes.ClosedHappy, happy.closed)
        assertEquals(1f, happy.sparkle, 0f)
        val sad = RobotPoses.of(Mood.SAD)
        assertEquals(38f, sad.antennaBend, 0f)
        assertEquals(RobotShapes.BrowSadL, sad.browL)
        assertEquals(1f, sad.tear, 0f)
        val covered = RobotPoses.of(Mood.EARS_COVERED)
        assertEquals(1f, covered.cover, 0f)
        assertEquals(1f, covered.waves, 0f)
        assertEquals(RobotShapes.BulbLoud, covered.bulb)
        val asleep = RobotPoses.of(null)
        assertEquals(34f, asleep.lift, 0f)
        assertEquals(0f, asleep.thrust, 0f)
        assertEquals(1f, asleep.battery, 0f)
        assertEquals(1f, asleep.dim, 0f)
        // Fine is the base: it shows nothing extra.
        val fine = RobotPoses.of(Mood.FINE)
        assertEquals(0f, fine.sweat + fine.tear + fine.battery + fine.waves + fine.shock + fine.heart, 0f)
    }

    @Test
    fun lerpEndpointsAreThePoses() {
        for (a in moods) for (b in moods) {
            val pa = RobotPoses.of(a)
            val pb = RobotPoses.of(b)
            assertEquals(pa, RobotPose.lerp(pa, pb, 0f))
            assertEquals(pb, RobotPose.lerp(pa, pb, 1f))
        }
    }

    @Test
    fun lerpMidpointIsBetween() {
        val mid = RobotPose.lerp(RobotPoses.of(Mood.FINE), RobotPoses.of(Mood.SAD), 0.5f)
        assertEquals(4f, mid.tilt, 1e-4f)
        assertEquals(19f, mid.antennaBend, 1e-4f)
    }

    @Test
    fun blendMovesEachPartOnItsOwnProgress() {
        val a = RobotPoses.of(Mood.FINE)
        val b = RobotPoses.of(Mood.SAD)
        val p = RobotPose.blend(a, b, 1f, 0f, 0.5f)
        assertEquals(b.lift, p.lift, 0f)
        assertEquals(a.tilt, p.tilt, 0f)
        assertEquals(0.5f * (a.cheek + b.cheek), p.cheek, 1e-4f)
    }

    @Test
    fun pattedPutsOnHeartEyes() {
        val fine = RobotPoses.of(Mood.FINE)
        assertEquals(fine, fine.patted(0f))
        val full = fine.patted(1f)
        assertEquals(1f, full.heart, 0f)
        assertEquals(0.8f, full.mouthOpen, 0f)
        assertEquals(1f, full.cheek, 0f)
        assertEquals(fine.tilt, full.tilt, 0f)
        assertEquals(0.5f, fine.patted(0.5f).heart, 1e-6f)
    }

    @Test
    fun kindFollowsMoodLevel() {
        assertEquals(RobotTransitionKind.LOUDER, robotTransitionKind(Mood.HAPPY, Mood.WORRIED))
        assertEquals(RobotTransitionKind.LOUDER, robotTransitionKind(Mood.SAD, Mood.EARS_COVERED))
        assertEquals(RobotTransitionKind.CALMER, robotTransitionKind(Mood.EARS_COVERED, Mood.SAD))
        assertEquals(RobotTransitionKind.CALMER, robotTransitionKind(Mood.FINE, Mood.HAPPY))
        assertEquals(RobotTransitionKind.SLEEP, robotTransitionKind(Mood.HAPPY, null))
        assertEquals(RobotTransitionKind.WAKE, robotTransitionKind(null, Mood.EARS_COVERED))
    }

    @Test
    fun durationsMatchTheDesign() {
        assertEquals(180, RobotTransitionKind.LOUDER.durationMs)
        assertEquals(900, RobotTransitionKind.CALMER.durationMs)
        assertEquals(1400, RobotTransitionKind.SLEEP.durationMs)
        assertEquals(1400, RobotTransitionKind.WAKE.durationMs)
    }

    @Test
    fun finishedTransitionEqualsTarget() {
        for (from in moods) for (to in moods) {
            if (from == to) continue
            val a = animator(from)
            a.setMood(to)
            repeat(100) { a.advance(0.05f) }
            assertEquals("$from -> $to", RobotPoses.of(to), a.pose)
        }
    }

    @Test
    fun headLagsTheFaceWhenCalmingDown() {
        val t = RobotTransition(RobotPoses.of(Mood.SAD), RobotPoses.of(Mood.FINE), RobotTransitionKind.CALMER, 0f)
        // At 10% the face window (0..0.55) has begun but the head window (0.2..1) has not.
        val early = t.poseAt(0.09f)
        assertEquals(RobotPoses.of(Mood.SAD).tilt, early.tilt, 0f)
        assertNotEquals(RobotPoses.of(Mood.SAD).smile, early.smile)
    }

    @Test
    fun bodyMovesFirstWhenWakingAndLast() {
        val t = RobotTransition(RobotPoses.of(null), RobotPoses.of(Mood.FINE), RobotTransitionKind.WAKE, 0f)
        // At 20% the face window (0..0.4) is under way, the body window (0.25..1) has not begun.
        val early = t.poseAt(0.28f)
        assertEquals(RobotPoses.of(null).lift, early.lift, 0f)
        assertNotEquals(RobotPoses.of(null).dim, early.dim)
    }

    @Test
    fun jumpWithoutAnimationShowsTargetAtOnce() {
        val a = animator(Mood.HAPPY)
        a.setMood(Mood.EARS_COVERED, animate = false)
        assertEquals(RobotPoses.of(Mood.EARS_COVERED), a.pose)
    }

    @Test
    fun interruptingStartsFromTheBlendedPose() {
        val a = animator(Mood.HAPPY)
        a.setMood(Mood.SAD) // louder
        a.advance(0.09f)
        val mid = a.pose
        assertNotEquals(RobotPoses.of(Mood.HAPPY), mid)
        assertNotEquals(RobotPoses.of(Mood.SAD), mid)

        a.setMood(Mood.FINE) // calmer, from where Astro is now
        a.advance(0f)
        assertEquals(mid, a.pose)

        a.advance(0.05f)
        assertNotEquals(mid, a.pose)
        repeat(100) { a.advance(0.05f) }
        assertEquals(RobotPoses.of(Mood.FINE), a.pose)
    }

    @Test
    fun pokeIgnoredWhenNotCalm() {
        for (m in listOf<Mood?>(Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)) {
            val a = animator(m)
            repeat(9) { a.poke(); a.run(0.1f) }
            assertEquals("$m", 0, a.comboCount)
            assertFalse("$m", a.isPerforming)
            assertEquals("$m", 0f, a.fx.patFace, 0f)
            assertEquals("$m", 0f, a.fx.boing, 0f)
            assertEquals("$m", 0f, a.fx.yaw, 0f)
            assertNull("$m", a.fx.orbit)
        }
    }

    @Test
    fun onePatGivesHeartEyesAndABoingThenFades() {
        val a = animator()
        a.run(1f)
        a.poke()
        a.run(0.1f)
        assertEquals(1f, a.fx.patFace, 0f)
        assertNotEquals(0f, a.fx.boing)
        assertFalse(a.isPerforming)
        a.run(1.5f)
        assertEquals(0f, a.fx.patFace, 0f)
        assertEquals(0f, a.fx.boing, 0f)
    }

    @Test
    fun threePatsStartASpinThatFinishesAndReturnsYawToZero() {
        val a = animator()
        a.run(1f)
        repeat(3) { a.poke(); if (it < 2) a.run(0.2f) }
        assertEquals(3, a.comboCount)
        assertTrue(a.isPerforming)
        a.run(0.3f)
        assertTrue(a.fx.yaw > 0f)
        assertEquals(0f, a.fx.hop, 1e-4f)
        assertTrue(a.fx.boost > 0f)
        // The heart eyes stay on while it spins.
        a.run(0.3f)
        assertEquals(1f, a.fx.patFace, 0f)
        var maxYaw = 0f
        repeat(120) { a.advance(1f / 60f); maxYaw = maxOf(maxYaw, a.fx.yaw) }
        assertTrue(maxYaw <= 360f)
        assertFalse(a.isPerforming)
        assertEquals(0f, a.fx.yaw, 0f)
        assertEquals(0f, a.fx.boost, 0f)
    }

    @Test
    fun sixPatsMakeATrickWithAHopADoubleSpinAndASparkleBurst() {
        val a = animator()
        a.run(1f)
        repeat(6) { a.poke(); if (it < 5) a.run(0.2f) }
        assertEquals(6, a.comboCount)
        var maxYaw = 0f
        var minHop = 0f
        var burst = false
        var t = 0f
        while (t < 1.6f) {
            a.advance(1f / 60f)
            t += 1f / 60f
            maxYaw = maxOf(maxYaw, a.fx.yaw)
            minHop = minOf(minHop, a.fx.hop)
            if (a.fx.burst >= 0f) burst = true
        }
        assertTrue("double spin, was $maxYaw", maxYaw > 360f)
        assertTrue("hop, was $minHop", minHop < -20f)
        assertTrue(burst)
        assertEquals(0f, a.fx.yaw, 0f)
        assertEquals(0f, a.fx.hop, 1e-4f)
    }

    @Test
    fun nineQuickPatsStartALapWhoseBankAndTrailAreZeroAtStartAndEnd() {
        val a = animator()
        a.run(1f)
        repeat(9) { a.poke(); if (it < 8) a.run(0.15f) }
        a.advance(0f)
        // At the very start there is no speed yet, so nothing leans and nothing trails.
        assertNotNull(a.fx.orbit)
        assertEquals(0f, a.fx.lapSpeed, 1e-3f)
        assertEquals(0f, a.fx.bank, 1e-2f)
        assertTrue(a.fx.trail.all { it.alpha < 1e-2f })

        var maxBank = 0f
        var maxTrail = 0
        var farthest = 0f
        var steps = 0
        while (a.fx.orbit != null && steps < 600) {
            a.advance(1f / 60f)
            steps++
            maxBank = maxOf(maxBank, kotlin.math.abs(a.fx.bank))
            maxTrail = maxOf(maxTrail, a.fx.trail.size)
            farthest = minOf(farthest, a.fx.lapY)
            assertTrue(a.fx.lapScale in 0.6f..1f)
        }
        assertTrue("it leans while flying, was $maxBank", maxBank > 5f)
        assertTrue(maxTrail > 0)
        assertTrue("it flies up and away, was $farthest", farthest < -20f)
        // Back home: no lap, no lean, no trail.
        assertNull(a.fx.orbit)
        assertEquals(0f, a.fx.bank, 0f)
        assertTrue(a.fx.trail.isEmpty())
        assertEquals(1f, a.fx.lapScale, 0f)
        // And it settles with a small damped bob.
        var bob = 0f
        repeat(60) { a.advance(1f / 60f); bob = maxOf(bob, kotlin.math.abs(a.fx.hop)) }
        assertTrue(bob > 0.5f)
        a.run(1.5f)
        assertEquals(0f, a.fx.hop, 0f)
    }

    @Test
    fun combosResetAfterAGap() {
        val a = animator()
        a.run(1f)
        a.poke(); a.run(0.3f)
        a.poke(); a.run(0.3f)
        assertEquals(2, a.comboCount)
        // A pause longer than the gap starts over, so this is pat 1, not pat 3.
        a.run(0.9f)
        a.poke()
        assertEquals(1, a.comboCount)
        a.run(0.3f)
        assertFalse(a.isPerforming)
        assertEquals(0f, a.fx.yaw, 0f)
    }

    @Test
    fun slowPatsNeverSpin() {
        val a = animator()
        a.run(1f)
        repeat(6) { a.poke(); a.run(0.9f) }
        assertEquals(1, a.comboCount)
        assertFalse(a.isPerforming)
    }

    @Test
    fun comboKeepsCountingPastNine() {
        val a = animator()
        a.run(1f)
        repeat(10) { a.poke(); a.run(0.1f) }
        assertEquals(10, a.comboCount)
    }

    @Test
    fun movingToALoudMoodEndsTheCombo() {
        val a = animator(Mood.FINE)
        a.run(1f)
        a.poke(); a.run(0.1f)
        a.poke(); a.run(0.1f)
        a.setMood(Mood.WORRIED)
        assertEquals(0, a.comboCount)
        a.run(0.5f)
        assertEquals(0f, a.fx.patFace, 0f)
    }

    @Test
    fun stillFxHasNoMotion() {
        val fx = RobotFx.Still
        assertFalse(fx.idle)
        assertEquals(0f, fx.bank, 0f)
        assertTrue(fx.trail.isEmpty())
        assertEquals(0f, fx.yaw, 0f)
    }

    @Test
    fun idleRhythmsNeverSkipOnAMoodChange() {
        val a = RobotAnimator(Mood.FINE, Random(1))
        // the fastest rhythm is the antenna rattle when too loud, at 9 Hz
        assertIdleRhythmsNeverSkip(
            setMood = { a.setMood(it) },
            advance = { a.advance(it) },
            angles = { with(a) { listOf(fx.hoverPhase, fx.swayPhase, fx.handPhase, fx.nodPhase, fx.antennaPhase) } },
            maxHz = 9f,
        )
    }
}
