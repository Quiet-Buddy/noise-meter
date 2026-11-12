package com.quietbuddy.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlocksTest {

    private val free = CharacterStyle.entries.filter { it.isFree }.toSet()

    @Test
    fun earnedByThresholds() {
        assertEquals(free, Unlocks.earnedBy(0))
        assertEquals(free, Unlocks.earnedBy(29))
        assertEquals(free + CharacterStyle.BUDDY_MINT, Unlocks.earnedBy(30))
        assertFalse(CharacterStyle.KITTEN_GINGER in Unlocks.earnedBy(119))
        assertTrue(CharacterStyle.KITTEN_GINGER in Unlocks.earnedBy(120))
        assertEquals(CharacterStyle.entries.toSet() - CharacterStyle.KITTEN_SNOW, Unlocks.earnedBy(599))
        assertEquals(CharacterStyle.entries.toSet(), Unlocks.earnedBy(600))
    }

    @Test
    fun nextIsCheapestUnearned() {
        assertEquals(CharacterStyle.BUDDY_MINT, Unlocks.next(0))
        assertEquals(CharacterStyle.KITTEN_GINGER, Unlocks.next(30))
        assertEquals(CharacterStyle.BUDDY_BERRY, Unlocks.next(120))
        assertEquals(CharacterStyle.KITTEN_GREY, Unlocks.next(240))
        assertEquals(CharacterStyle.KITTEN_SNOW, Unlocks.next(400))
        assertNull(Unlocks.next(600))
    }

    private fun minute(laeq: Float, seconds: Int) = MinuteAggregate(0L, laeq, laeq, laeq, seconds)

    @Test
    fun calmMinuteBoundaries() {
        val s = AppSettings()
        val t = s.worriedFromDb
        assertTrue(Unlocks.isCalmMinute(minute(t - 0.1f, 30), s))
        assertFalse(Unlocks.isCalmMinute(minute(t - 0.1f, 29), s))
        assertFalse(Unlocks.isCalmMinute(minute(t, 60), s))
    }

    @Test
    fun pendingCelebrationIsCheapestFirst() {
        val p = Progress(
            calmMinutes = 600,
            unlocked = CharacterStyle.entries.toSet(),
            celebrated = free,
        )
        assertEquals(
            listOf(CharacterStyle.BUDDY_MINT, CharacterStyle.KITTEN_GINGER, CharacterStyle.BUDDY_BERRY, CharacterStyle.KITTEN_GREY, CharacterStyle.KITTEN_SNOW),
            p.pendingCelebration,
        )
        assertTrue(Progress().pendingCelebration.isEmpty())
    }

    @Test
    fun mochiUnlocksAtOneHundredTwentyMinutes() {
        val at119 = Progress(calmMinutes = 119, unlocked = Unlocks.earnedBy(119))
        val at120 = Progress(calmMinutes = 120, unlocked = Unlocks.earnedBy(120))
        assertFalse(at119.isUnlocked(Character.KITTEN))
        assertTrue(at120.isUnlocked(Character.KITTEN))
    }

    @Test
    fun buddyIsAlwaysUnlocked() {
        assertTrue(Progress().isUnlocked(Character.BUDDY))
        assertTrue(Progress().isUnlocked(Character.DEFAULT))
    }

    @Test
    fun characterDefaultStyles() {
        assertTrue(CharacterStyle.BUDDY_SUNNY.isCharacterDefault)
        assertTrue(CharacterStyle.KITTEN_GINGER.isCharacterDefault)
        assertFalse(CharacterStyle.BUDDY_MINT.isCharacterDefault)
        assertFalse(CharacterStyle.KITTEN_SNOW.isCharacterDefault)
    }

    @Test
    fun styleForFallsBack() {
        assertEquals(CharacterStyle.BUDDY_SUNNY, AppSettings().styleFor(Character.BUDDY))
        val chosen = AppSettings(styles = mapOf(Character.KITTEN to CharacterStyle.KITTEN_GREY))
        assertEquals(CharacterStyle.KITTEN_GREY, chosen.styleFor(Character.KITTEN))
        val mismatched = AppSettings(styles = mapOf(Character.KITTEN to CharacterStyle.BUDDY_MINT))
        assertEquals(CharacterStyle.KITTEN_GINGER, mismatched.styleFor(Character.KITTEN))
    }
}
