package com.quietbuddy.ui.character

import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.kitten.KittenAnimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PokeReactionTest {
    private val step = 1f / 60f

    private fun run(seconds: Float, advance: (Float) -> Unit) {
        repeat((seconds / step).toInt()) { advance(step) }
    }

    @Test
    fun envelopeRisesFastHoldsAndDecaysToZero() {
        val env = PokeEnvelope(attackSec = 0.06f, holdSec = 0.2f, releaseSec = 0.2f)
        assertEquals(0f, env.value)
        env.trigger()
        run(0.1f, env::advance)
        assertEquals(1f, env.value, 1e-4f)
        run(0.4f, env::advance)
        assertEquals(0f, env.value)
    }

    @Test
    fun repokeDuringDecayRisesAgainWithoutDroppingToZero() {
        val env = PokeEnvelope(attackSec = 0.06f, holdSec = 0.2f, releaseSec = 0.2f)
        env.trigger()
        run(0.36f, env::advance) // into the release
        val mid = env.value
        assertTrue(mid > 0f && mid < 1f)
        env.trigger()
        var min = 1f
        var prev = env.value
        repeat(6) {
            env.advance(step)
            min = minOf(min, env.value)
            assertTrue("never falls right after a re-poke", env.value >= prev)
            prev = env.value
        }
        assertTrue(min >= mid)
        assertEquals(1f, env.value, 1e-4f)
    }

    @Test
    fun rapidPokesKeepItUpWithoutJumps() {
        val env = PokeEnvelope(attackSec = 0.06f, holdSec = 0.2f, releaseSec = 0.2f)
        var prev = 0f
        repeat(120) { i ->
            if (i % 6 == 0) env.trigger() // every 100 ms
            env.advance(step)
            if (i > 10) assertEquals(1f, env.value, 1e-4f)
            assertTrue(kotlin.math.abs(env.value - prev) < 0.5f)
            prev = env.value
        }
    }

    @Test
    fun kittenGigglesOnlyWhenCalm() {
        val calm = KittenAnimator(Mood.FINE, Random(1))
        calm.advance(step)
        calm.poke()
        run(0.1f, calm::advance)
        assertTrue(calm.giggle > 0.9f)
        assertTrue(calm.fx.giggle > 0.9f)
        assertTrue(calm.fx.earFlick > 0f)
        run(0.6f, calm::advance)
        assertEquals(0f, calm.fx.giggle)
        assertEquals(0f, calm.fx.earFlick)

        for (mood in listOf(Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)) {
            val tense = KittenAnimator(mood, Random(1))
            tense.poke()
            run(0.1f, tense::advance)
            assertEquals(0f, tense.giggle)
            assertEquals(0f, tense.fx.tailSwish)
        }
    }

    @Test
    fun kittenGiggleFadesWhenMoodTurnsTense() {
        val k = KittenAnimator(Mood.HAPPY, Random(1))
        k.poke()
        run(0.1f, k::advance)
        k.setMood(Mood.WORRIED)
        run(0.25f, k::advance)
        assertEquals(0f, k.giggle)
    }

    @Test
    fun buddyGigglesOnlyWhenCalm() {
        val b = BuddyAnimator(Mood.HAPPY, Random(1))
        b.poke()
        run(0.1f, b::advance)
        assertTrue(b.giggle > 0.9f)
        run(0.5f, b::advance)
        assertEquals(0f, b.giggle)

        for (mood in listOf(Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)) {
            val t = BuddyAnimator(mood, Random(1))
            t.poke()
            run(0.1f, t::advance)
            assertEquals(0f, t.giggle)
        }
    }

    @Test
    fun buddyBlinksNowAndThen() {
        val b = BuddyAnimator(Mood.FINE, Random(1))
        var maxBlink = 0f
        run(8f) { b.advance(it); maxBlink = maxOf(maxBlink, b.blink) }
        assertTrue(maxBlink > 0.5f)
    }
}
