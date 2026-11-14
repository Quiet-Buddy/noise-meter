package com.quietbuddy.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlocksTest {

    private val free = Progress.FREE
    private val lifetimeRewards = Reward.all.filterNot { it.isLimited }.toSet()

    @Test
    fun earnedByThresholds() {
        assertEquals(free, Unlocks.earnedBy(0))
        assertEquals(free, Unlocks.earnedBy(29))
        assertEquals(free + CharacterStyle.BUDDY_MINT, Unlocks.earnedBy(30))
        assertFalse(CharacterStyle.KITTEN_GINGER in Unlocks.earnedBy(119))
        assertTrue(CharacterStyle.KITTEN_GINGER in Unlocks.earnedBy(120))
        assertFalse(Accessory.SUNGLASSES in Unlocks.earnedBy(59))
        assertTrue(Accessory.SUNGLASSES in Unlocks.earnedBy(60))
        assertFalse(Accessory.CROWN in Unlocks.earnedBy(999))
        assertTrue(Accessory.CROWN in Unlocks.earnedBy(1000))
        assertEquals(lifetimeRewards - CharacterStyle.ROBOT_SKY, Unlocks.earnedBy(2399))
        assertEquals(lifetimeRewards, Unlocks.earnedBy(2400))
    }

    @Test
    fun nextIsCheapestUnearned() {
        val order = listOf(
            0L to CharacterStyle.BUDDY_MINT,
            30L to Accessory.SUNGLASSES,
            60L to CharacterStyle.KITTEN_GINGER,
            120L to Accessory.BOW,
            180L to CharacterStyle.BUDDY_BERRY,
            240L to Accessory.TIE,
            300L to Accessory.BELL_COLLAR,
            360L to CharacterStyle.KITTEN_GREY,
            400L to Accessory.TOP_HAT,
            480L to Accessory.FLOWER,
            540L to CharacterStyle.KITTEN_SNOW,
            600L to Accessory.BOOTS,
            750L to CharacterStyle.DUCK_LEMON,
            800L to Accessory.CAP,
            900L to Accessory.CROWN,
            1000L to CharacterStyle.DUCK_CLOUD,
            1050L to Accessory.GOGGLES,
            1200L to Accessory.BANDANA,
            1350L to Accessory.SWIM_RING,
            1400L to CharacterStyle.DUCK_HAZEL,
            1500L to CharacterStyle.ROBOT_PEARL,
            1700L to Accessory.PROPELLER_CAP,
            1800L to CharacterStyle.ROBOT_LILAC,
            2000L to Accessory.STAR_GLASSES,
            2100L to Accessory.BOW_TIE,
            2250L to Accessory.PLANET_RING,
            2300L to CharacterStyle.ROBOT_SKY,
        )
        for ((minutes, expected) in order) assertEquals("at $minutes", expected, Unlocks.next(minutes))
        assertNull(Unlocks.next(2400))
    }

    @Test
    fun previousThresholdIsLastEarned() {
        assertEquals(0, Unlocks.previousThreshold(0))
        assertEquals(30, Unlocks.previousThreshold(59))
        assertEquals(60, Unlocks.previousThreshold(60))
        assertEquals(2400, Unlocks.previousThreshold(5000))
    }

    @Test
    fun rewardKeysAreUniqueAndRoundTrip() {
        val keys = Reward.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        Reward.all.forEach { assertEquals(it, Reward.fromKey(it.key)) }
        assertNull(Reward.fromKey("nope"))
    }

    @Test
    fun accessoriesCombine() {
        val all = Accessory.of(Character.KITTEN).fold(AppSettings()) { s, a -> s.toggleAccessory(Character.KITTEN, a) }
        assertEquals(Accessory.of(Character.KITTEN).toSet(), all.accessoriesFor(Character.KITTEN))
        assertTrue(all.accessoriesFor(Character.BUDDY).isEmpty())
        val noBow = all.toggleAccessory(Character.KITTEN, Accessory.BOW)
        assertEquals(Accessory.of(Character.KITTEN).toSet() - Accessory.BOW, noBow.accessoriesFor(Character.KITTEN))
    }

    @Test
    fun toggleIgnoresAnotherCharactersAccessory() {
        val s = AppSettings()
        assertEquals(s, s.toggleAccessory(Character.KITTEN, Accessory.TOP_HAT))
        assertEquals(s, s.toggleAccessory(Character.BUDDY, Accessory.CROWN))
        val stray = AppSettings(accessories = mapOf(Character.BUDDY to setOf(Accessory.CROWN, Accessory.TIE)))
        assertEquals(setOf(Accessory.TIE), stray.accessoriesFor(Character.BUDDY))
    }

    @Test
    fun everyCharacterHasAccessoriesAndNoneBeforeItsOwner() {
        Character.entries.forEach { c ->
            val own = Accessory.of(c)
            assertTrue("$c has accessories", own.isNotEmpty())
            val owner = CharacterStyle.defaultFor(c).unlockAtCalmMinutes
            own.forEach { assertTrue("${it.name} unlocks after $c", it.unlockAtCalmMinutes > owner) }
        }
        assertEquals(Accessory.entries.toSet(), Character.entries.flatMap { Accessory.of(it) }.toSet())
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
            calmMinutes = 200,
            unlocked = Unlocks.earnedBy(200),
            celebrated = free,
        )
        assertEquals(
            listOf(CharacterStyle.BUDDY_MINT, Accessory.SUNGLASSES, CharacterStyle.KITTEN_GINGER, Accessory.BOW),
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
    fun pipUnlocksAtEightHundredMinutes() {
        val at799 = Progress(calmMinutes = 799, unlocked = Unlocks.earnedBy(799))
        val at800 = Progress(calmMinutes = 800, unlocked = Unlocks.earnedBy(800))
        assertFalse(at799.isUnlocked(Character.DUCK))
        assertTrue(at800.isUnlocked(Character.DUCK))
    }

    @Test
    fun astroUnlocksAtSeventeenHundredMinutes() {
        val at1699 = Progress(calmMinutes = 1699, unlocked = Unlocks.earnedBy(1699))
        val at1700 = Progress(calmMinutes = 1700, unlocked = Unlocks.earnedBy(1700))
        assertFalse(at1699.isUnlocked(Character.ROBOT))
        assertTrue(at1700.isUnlocked(Character.ROBOT))
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
        assertTrue(CharacterStyle.DUCK_LEMON.isCharacterDefault)
        assertFalse(CharacterStyle.DUCK_HAZEL.isCharacterDefault)
        assertTrue(CharacterStyle.ROBOT_PEARL.isCharacterDefault)
        assertFalse(CharacterStyle.ROBOT_SKY.isCharacterDefault)
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
