package com.quietbuddy.ui.character.mari

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.assertIdleRhythmsNeverSkip
import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MariMotionTest {
    private val moods = listOf<Mood?>(Mood.HAPPY, Mood.FINE, Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)

    private fun animator(mood: Mood? = Mood.FINE) = MariAnimator(mood, Random(1))

    private fun MariAnimator.run(seconds: Float, step: Float = 1f / 60f) {
        var left = seconds
        while (left > 0f) {
            advance(minOf(step, left))
            left -= step
        }
    }

    /** [n] quick pats, [gap] seconds apart (no wait after the last). */
    private fun MariAnimator.pats(n: Int, gap: Float = 0.2f) = repeat(n) {
        poke()
        if (it < n - 1) run(gap)
    }

    @Test
    fun everyPoseHasTheSameListSizes() {
        for (m in moods) assertEquals("$m closed", 6, MariPoses.of(m).closed.size)
    }

    @Test
    fun posesMatchThePrototype() {
        val happy = MariPoses.of(Mood.HAPPY)
        assertEquals(139f, happy.hRx, 0f)
        assertEquals(1f, happy.appleUp, 0f)
        assertEquals(22f, happy.bend, 0f)
        assertEquals(MariShapes.ClosedHappy, happy.closed)
        assertEquals(1.25f, happy.bloom, 0f)
        val worried = MariPoses.of(Mood.WORRIED)
        assertEquals(0.35f, worried.robinAway, 0f)
        assertEquals(1f, worried.sweat, 0f)
        val sad = MariPoses.of(Mood.SAD)
        assertEquals(70f, sad.bend, 0f)
        assertEquals(1f, sad.tear, 0f)
        val covered = MariPoses.of(Mood.EARS_COVERED)
        assertEquals(57f, covered.hLx, 0f)
        assertEquals(143f, covered.hRx, 0f)
        assertEquals(1f, covered.drop, 0f)
        assertEquals(1f, covered.grit, 0f)
        assertEquals(1f, covered.eyeSharp, 0f)
        assertEquals(MariShapes.ClosedSqueeze, covered.closed)
        assertEquals(1f, covered.robinAway, 0f)
        val asleep = MariPoses.of(null)
        assertEquals(1f, asleep.moon, 0f)
        assertEquals(1f, asleep.drop, 0f)
        assertEquals(1f, asleep.lamp, 0f)
        assertEquals(0.2f, asleep.bloom, 0f)
        assertEquals(1f, asleep.robinSleep, 0f)
        // Fine is the base: it shows nothing extra.
        val fine = MariPoses.of(Mood.FINE)
        assertEquals(0f, fine.sweat + fine.tear + fine.waves + fine.moon + fine.petals + fine.grit + fine.drop, 0f)
    }

    @Test
    fun lerpEndpointsAreThePoses() {
        for (a in moods) for (b in moods) {
            val pa = MariPoses.of(a)
            val pb = MariPoses.of(b)
            assertEquals(pa, MariPose.lerp(pa, pb, 0f))
            assertEquals(pb, MariPose.lerp(pa, pb, 1f))
        }
    }

    @Test
    fun blendMovesEachPartOnItsOwnProgress() {
        val a = MariPoses.of(Mood.FINE)
        val b = MariPoses.of(Mood.EARS_COVERED)
        val p = MariPose.blend(a, b, 1f, 0f, 0.5f)
        assertEquals(b.hLx, p.hLx, 0f)
        assertEquals(b.drop, p.drop, 0f)
        assertEquals(a.bend, p.bend, 0f)
        assertEquals(0.5f * (a.grit + b.grit), p.grit, 1e-4f)
        // The robin belongs to the face group.
        assertEquals(0.5f, p.robinAway, 1e-4f)
    }

    @Test
    fun pattedShutsTheEyesWithDelight() {
        val fine = MariPoses.of(Mood.FINE)
        assertEquals(fine, fine.patted(0f))
        val full = fine.patted(1f)
        assertEquals(0f, full.open, 0f)
        assertEquals(MariShapes.ClosedHappy, full.closed)
        assertEquals(1.35f, full.cheek, 1e-6f)
        assertEquals(1f, full.smile, 0f)
        assertEquals(fine.bend, full.bend, 0f)
        assertEquals(0.5f, fine.patted(0.5f).open, 1e-6f)
    }

    @Test
    fun kindFollowsMoodLevel() {
        assertEquals(MariTransitionKind.LOUDER, mariTransitionKind(Mood.HAPPY, Mood.WORRIED))
        assertEquals(MariTransitionKind.LOUDER, mariTransitionKind(Mood.SAD, Mood.EARS_COVERED))
        assertEquals(MariTransitionKind.CALMER, mariTransitionKind(Mood.EARS_COVERED, Mood.SAD))
        assertEquals(MariTransitionKind.CALMER, mariTransitionKind(Mood.FINE, Mood.HAPPY))
        assertEquals(MariTransitionKind.SLEEP, mariTransitionKind(Mood.HAPPY, null))
        assertEquals(MariTransitionKind.WAKE, mariTransitionKind(null, Mood.EARS_COVERED))
    }

    @Test
    fun durationsMatchThePrototype() {
        assertEquals(220, MariTransitionKind.LOUDER.durationMs)
        assertEquals(900, MariTransitionKind.CALMER.durationMs)
        assertEquals(1400, MariTransitionKind.SLEEP.durationMs)
        assertEquals(1400, MariTransitionKind.WAKE.durationMs)
    }

    @Test
    fun settlesIntoEveryMoodFromEveryMood() {
        for (from in moods) for (to in moods) {
            if (from == to) continue
            val a = animator(from)
            a.setMood(to)
            repeat(100) { a.advance(0.05f) }
            assertEquals("$from -> $to", MariPoses.of(to), a.pose)
        }
    }

    @Test
    fun faceRelaxesBeforeTheTontorraWhenCalmingDown() {
        val t = MariTransition(MariPoses.of(Mood.SAD), MariPoses.of(Mood.FINE), MariTransitionKind.CALMER, 0f)
        // At 8% the face window (0..0.5) has begun but the head window (0.1..1) has not.
        val early = t.poseAt(0.072f)
        assertEquals(MariPoses.of(Mood.SAD).bend, early.bend, 0f)
        assertNotEquals(MariPoses.of(Mood.SAD).smile, early.smile)
    }

    @Test
    fun fallingAsleepSetsTheBasketDownBeforeTheEyesClose() {
        val t = MariTransition(MariPoses.of(Mood.FINE), MariPoses.of(null), MariTransitionKind.SLEEP, 0f)
        // At 40% the body window (0..0.7) is well under way, the face window (0.45..1) has not begun.
        val early = t.poseAt(0.56f)
        assertTrue(early.drop > 0.3f)
        assertEquals(MariPoses.of(Mood.FINE).open, early.open, 0f)
    }

    @Test
    fun jumpWithoutAnimationShowsTargetAtOnce() {
        val a = animator(Mood.HAPPY)
        a.setMood(Mood.EARS_COVERED, animate = false)
        assertEquals(MariPoses.of(Mood.EARS_COVERED), a.pose)
    }

    @Test
    fun interruptingStartsFromTheBlendedPose() {
        val a = animator(Mood.HAPPY)
        a.setMood(Mood.SAD) // louder
        a.advance(0.1f)
        val mid = a.pose
        assertNotEquals(MariPoses.of(Mood.HAPPY), mid)
        assertNotEquals(MariPoses.of(Mood.SAD), mid)

        a.setMood(Mood.FINE) // calmer, from where she is now
        a.advance(0f)
        assertEquals(mid, a.pose)

        a.advance(0.05f)
        assertNotEquals(mid, a.pose)
        repeat(100) { a.advance(0.05f) }
        assertEquals(MariPoses.of(Mood.FINE), a.pose)
    }

    @Test
    fun pokeIgnoredWhenNotCalm() {
        for (m in listOf<Mood?>(Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)) {
            val a = animator(m)
            a.run(1f)
            repeat(9) { a.poke(); a.run(0.1f) }
            assertEquals("$m", 0, a.comboCount)
            assertFalse("$m", a.isPerforming)
            assertEquals("$m", 0f, a.fx.patFace, 0f)
            assertEquals("$m", 0f, a.fx.boing, 0f)
            assertEquals("$m", 0f, a.fx.headLean, 0f)
            assertEquals("$m", -1f, a.fx.toss, 0f)
        }
    }

    @Test
    fun onePatLeansTheHeadAndBoingsTheHookThenSettles() {
        val a = animator()
        a.run(1f)
        a.poke()
        var boing = 0f
        repeat(12) { a.advance(1f / 60f); boing = maxOf(boing, abs(a.fx.boing)) }
        assertTrue("boing, was $boing", boing > 10f)
        assertTrue(a.fx.patFace > 0.9f)
        assertTrue(a.fx.headLean > 3f)
        assertFalse(a.isPerforming)
        a.run(2.5f)
        assertEquals(0f, a.fx.patFace, 1e-3f)
        assertEquals(0f, a.fx.headLean, 1e-2f)
        assertEquals(0f, a.fx.boing, 0.1f)
    }

    @Test
    fun quickPatsKickTheSpringRatherThanRestartingIt() {
        // A second pat mid-swing adds to the hook's swing instead of snapping it back to the start.
        val a = animator()
        a.run(1f)
        a.poke()
        a.run(0.05f)
        val before = a.fx.boing
        assertTrue(before > 10f)
        a.poke()
        a.advance(1f / 60f)
        assertTrue("no snap, was $before then ${a.fx.boing}", a.fx.boing > before)
    }

    @Test
    fun threePatsTossAnAppleSheWatchesAndCatches() {
        val a = animator(Mood.HAPPY)
        a.run(1f)
        a.pats(3)
        assertEquals(3, a.comboCount)
        assertEquals(MariTrick.TOSS, a.trick)
        var minLookY = 0f
        var reach = 0f
        var hidden = 0f
        var t = 0f
        while (t < 1.4f) {
            a.advance(1f / 60f)
            t += 1f / 60f
            minLookY = minOf(minLookY, a.fx.lookY)
            reach = maxOf(reach, a.fx.handKR)
            hidden = maxOf(hidden, a.fx.hideHandApple)
            assertTrue(a.fx.toss in 0f..1f)
        }
        // Her eyes open to watch it (no pat face), and the right hand reaches up for it.
        assertTrue(reach > 0.9f)
        assertTrue(hidden > 0.9f)
        a.run(0.5f)
        assertFalse(a.isPerforming)
        assertEquals(-1f, a.fx.toss, 0f)
        assertEquals(0f, a.fx.handKR, 1e-3f)
        assertEquals(0f, a.fx.hideHandApple, 1e-3f)
    }

    @Test
    fun sixPatsJumpWithAPetalBurst() {
        val a = animator()
        a.run(1f)
        a.pats(3)
        a.run(1.6f) // let the toss finish
        a.pats(3, gap = 0.2f)
        // The gap after the toss broke the combo, so this is a fresh 3 again: another toss.
        assertEquals(MariTrick.TOSS, a.trick)

        val b = animator()
        b.run(1f)
        b.pats(6, gap = 0.1f)
        assertEquals(6, b.comboCount)
        var minHop = 0f
        var maxStretch = 1f
        var burst = false
        var t = 0f
        while (t < 3f) {
            b.advance(1f / 60f)
            t += 1f / 60f
            if (b.trick == MariTrick.HOP) {
                minHop = minOf(minHop, b.fx.hop)
                maxStretch = maxOf(maxStretch, b.fx.stretch)
            }
            if (b.fx.burst >= 0f) burst = true
        }
        assertTrue("jumps, was $minHop", minHop < -40f)
        assertTrue("stretches, was $maxStretch", maxStretch > 1.05f)
        assertTrue(burst)
        assertFalse(b.isPerforming)
        assertEquals(0f, b.fx.hop, 1e-3f)
        assertEquals(1f, b.fx.stretch, 1e-3f)
    }

    @Test
    fun ninePatsDanceWithTheLegsKickingInTurn() {
        val a = animator()
        a.run(1f)
        a.pats(9, gap = 0.1f)
        assertEquals(9, a.comboCount)
        // The toss started at pat 3 is still running: the bigger tricks wait for it.
        assertEquals(MariTrick.TOSS, a.trick)
        assertEquals(MariTrick.DANCE, a.queuedTrick)
        var sawDance = false
        var maxL = 0f
        var maxR = 0f
        var maxDance = 0f
        var burst = false
        var t = 0f
        while (t < 6f) {
            a.advance(1f / 60f)
            t += 1f / 60f
            if (a.trick == MariTrick.DANCE) sawDance = true
            maxL = maxOf(maxL, a.fx.legL)
            maxR = maxOf(maxR, a.fx.legR)
            maxDance = maxOf(maxDance, abs(a.fx.dance))
            // Only one leg kicks at a time.
            assertTrue(minOf(a.fx.legL, a.fx.legR) < 0.15f)
            if (a.fx.burst >= 0f) burst = true
        }
        assertTrue(sawDance)
        assertTrue(maxL > 0.8f && maxR > 0.8f)
        assertTrue(maxDance > 4f)
        assertTrue(burst)
        assertFalse(a.isPerforming)
        assertEquals(0f, a.fx.legL, 1e-3f)
        assertEquals(0f, a.fx.legR, 1e-3f)
        assertEquals(0f, a.fx.dance, 1e-3f)
    }

    @Test
    fun aBiggerTrickQueuesInsteadOfCuttingTheRunningOne() {
        val a = animator()
        a.run(1f)
        a.pats(6, gap = 0.15f)
        // Pat 6 came while the toss (1.5 s) was still in the air.
        assertEquals(MariTrick.TOSS, a.trick)
        assertEquals(MariTrick.HOP, a.queuedTrick)
        var steps = 0
        while (a.trick == MariTrick.TOSS && steps < 600) {
            a.advance(1f / 60f)
            steps++
        }
        // The jump starts right when the toss ends.
        assertEquals(MariTrick.HOP, a.trick)
        assertNull(a.queuedTrick)
    }

    @Test
    fun aMoodChangeNeverCancelsARunningTrick() {
        val a = animator(Mood.HAPPY)
        a.run(1f)
        a.pats(9, gap = 0.1f)
        a.run(1.6f)
        assertEquals(MariTrick.DANCE, a.trick)
        a.setMood(Mood.EARS_COVERED)
        assertEquals(0, a.comboCount)
        a.run(1f)
        assertEquals(MariTrick.DANCE, a.trick)
        // It finishes in its own time, and no new trick starts in the loud room.
        a.run(3f)
        assertFalse(a.isPerforming)
        repeat(9) { a.poke(); a.run(0.1f) }
        assertFalse(a.isPerforming)
    }

    @Test
    fun aQueuedTrickIsDroppedIfTheRoomIsNoLongerCalm() {
        val a = animator()
        a.run(1f)
        a.pats(6, gap = 0.15f)
        assertEquals(MariTrick.HOP, a.queuedTrick)
        a.setMood(Mood.SAD)
        a.run(2f)
        assertFalse(a.isPerforming)
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
        assertFalse(a.isPerforming)
    }

    @Test
    fun slowPatsNeverStartATrick() {
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
    fun movingToALoudMoodEndsTheComboAndThePatFace() {
        val a = animator(Mood.FINE)
        a.run(1f)
        a.poke(); a.run(0.1f)
        a.poke(); a.run(0.1f)
        a.setMood(Mood.WORRIED)
        assertEquals(0, a.comboCount)
        // It eases away rather than snapping off.
        a.advance(1f / 60f)
        assertTrue(a.fx.patFace > 0.5f)
        a.run(1f)
        assertEquals(0f, a.fx.patFace, 1e-2f)
    }

    @Test
    fun trickOutputsNeverJumpBetweenFrames() {
        val a = animator(Mood.HAPPY)
        a.run(1f)
        val dt = 1f / 60f
        var prev = a.fx
        fun check() {
            val f = a.fx
            assertTrue("hop ${prev.hop} -> ${f.hop}", abs(f.hop - prev.hop) < 6f)
            assertTrue("dance", abs(f.dance - prev.dance) < 1.5f)
            assertTrue("bend", abs(f.bendAdd - prev.bendAdd) < 8f)
            assertTrue("stretch", abs(f.stretch - prev.stretch) < 0.06f)
            assertTrue("legs", abs(f.legL - prev.legL) < 0.2f && abs(f.legR - prev.legR) < 0.2f)
            assertTrue("hands", abs(f.handKL - prev.handKL) < 0.2f && abs(f.handKR - prev.handKR) < 0.2f)
            assertTrue("look", abs(f.lookX - prev.lookX) < 1f && abs(f.lookY - prev.lookY) < 1f)
            assertTrue("lean", abs(f.headLean - prev.headLean) < 1.5f)
            assertTrue("face", abs(f.patFace - prev.patFace) < 0.3f)
            assertTrue("apple", abs(f.hideHandApple - prev.hideHandApple) < 0.3f)
            prev = f
        }
        // Every trick, queued back to back, with a mood change in the middle of the dance.
        repeat(9) {
            a.poke()
            repeat(6) { a.advance(dt); check() }
        }
        repeat(60 * 4) { a.advance(dt); check() }
        a.setMood(Mood.FINE)
        repeat(60 * 3) { a.advance(dt); check() }
        a.setMood(Mood.EARS_COVERED)
        repeat(60 * 2) { a.advance(dt); check() }
    }

    @Test
    fun stillFxHasNoMotion() {
        val fx = MariFx.Still
        assertFalse(fx.idle)
        assertEquals(0f, fx.hop, 0f)
        assertEquals(1f, fx.stretch, 0f)
        assertEquals(-1f, fx.toss, 0f)
        assertEquals(-1f, fx.burst, 0f)
    }

    @Test
    fun idleRhythmsNeverSkipOnAMoodChange() {
        val a = MariAnimator(Mood.FINE, Random(1))
        // the fastest rhythm is the hook's rattle when too loud, at 9 Hz
        assertIdleRhythmsNeverSkip(
            setMood = { a.setMood(it) },
            advance = { a.advance(it) },
            angles = { with(a) { listOf(fx.bobPhase, fx.swayPhase, fx.nodPhase, fx.hookPhase) } },
            maxHz = 9f,
        )
    }
}
