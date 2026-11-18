package com.quietbuddy.ui.character

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.kitten.KittenAnimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MoodAnimatorTest {
    @Test
    fun kindFollowsMoodLevel() {
        assertEquals(TransitionKind.LOUDER, TransitionKind.between(Mood.HAPPY, Mood.WORRIED))
        assertEquals(TransitionKind.LOUDER, TransitionKind.between(Mood.SAD, Mood.EARS_COVERED))
        assertEquals(TransitionKind.CALMER, TransitionKind.between(Mood.EARS_COVERED, Mood.SAD))
        assertEquals(TransitionKind.CALMER, TransitionKind.between(Mood.FINE, Mood.HAPPY))
        assertEquals(TransitionKind.SLEEP, TransitionKind.between(Mood.HAPPY, null))
        assertEquals(TransitionKind.WAKE, TransitionKind.between(null, Mood.EARS_COVERED))
    }

    @Test
    fun clockStaysSmallAndTimestampsFollowIt() {
        val clock = AnimationClock()
        val blinker = clock.track(Blinker(Random(1)))
        var blinks = 0
        var wasOpen = true
        // Five hours at 60 fps.
        repeat(5 * 3600 * 60) {
            clock.advance(1f / 60f)
            val shut = blinker.at(clock.seconds) > 0f
            if (shut && wasOpen) blinks++
            wasOpen = !shut
            assertTrue(clock.seconds < AnimationClock.REBASE_SEC)
        }
        // A blink every 2.2-5.4 s keeps going across every rebase: about 4,800 in five hours.
        assertTrue("blinks: $blinks", blinks in 3_300..8_200)
    }

    @Test
    fun animationKeepsMovingAfterDaysOnScreen() {
        // The old Float clock stopped advancing at 120 fps after about 73 hours; blinks and transitions froze.
        val animator = KittenAnimator(Mood.HAPPY, Random(7))
        val dt = 1f / 120f
        repeat(80 * 3600 * 120) { animator.advance(dt) }
        val before = animator.fx.time
        animator.advance(dt)
        assertEquals(dt, animator.fx.time - before, 1e-4f)

        animator.setMood(Mood.SAD)
        repeat(240) { animator.advance(dt) } // 2 s: longer than any transition
        assertEquals(animator.pose, com.quietbuddy.ui.character.kitten.KittenPoses.of(Mood.SAD))
    }

    @Test
    fun patJustBeforeTheClockMovesBackStillFades() {
        val robot = com.quietbuddy.ui.character.robot.RobotAnimator(Mood.HAPPY, Random(3))
        val mari = com.quietbuddy.ui.character.mari.MariAnimator(Mood.HAPPY, Random(3))
        val dt = 1f / 60f
        // Whole seconds add up exactly, so the pat lands a quarter of a second before the hourly rebase.
        repeat(AnimationClock.REBASE_SEC.toInt() - 1) { robot.advance(1f); mari.advance(1f) }
        robot.advance(0.75f)
        mari.advance(0.75f)
        robot.poke()
        mari.poke()
        repeat(3 * 60) { robot.advance(dt); mari.advance(dt) }
        // Without moving the pat's timestamp back too, the heart eyes and delighted face would hold for an hour.
        assertEquals(0f, robot.fx.patFace, 0.01f)
        assertEquals(0f, mari.fx.patFace, 0.01f)
        // And a pat three seconds later starts a new combo instead of continuing the old one.
        robot.poke()
        mari.poke()
        assertEquals(1, robot.comboCount)
        assertEquals(1, mari.comboCount)
    }
}
