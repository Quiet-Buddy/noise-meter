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
        assertEquals(CharacterStyle.entries.toSet() - CharacterStyle.KITTEN_SNOW, Unlocks.earnedBy(599))
        assertEquals(CharacterStyle.entries.toSet(), Unlocks.earnedBy(600))
    }

    @Test
    fun nextIsCheapestUnearned() {
        assertEquals(CharacterStyle.BUDDY_MINT, Unlocks.next(0))
        assertEquals(CharacterStyle.KITTEN_GREY, Unlocks.next(30))
        assertEquals(CharacterStyle.KITTEN_SNOW, Unlocks.next(300))
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
            listOf(CharacterStyle.BUDDY_MINT, CharacterStyle.KITTEN_GREY, CharacterStyle.BUDDY_BERRY, CharacterStyle.KITTEN_SNOW),
            p.pendingCelebration,
        )
        assertTrue(Progress().pendingCelebration.isEmpty())
    }

    @Test
    fun isUnlockedHonoursUnlockAll() {
        val p = Progress()
        assertFalse(p.isUnlocked(CharacterStyle.KITTEN_SNOW, AppSettings()))
        assertTrue(p.isUnlocked(CharacterStyle.KITTEN_SNOW, AppSettings(unlockAllStyles = true)))
        assertTrue(p.isUnlocked(CharacterStyle.BUDDY_SUNNY, AppSettings()))
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
