package com.quietbuddy.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LimitedEventsTest {

    private val zone = ZoneId.of("Europe/Madrid")
    private val event = LimitedEvent("snow_a", Character.SNOWMAN, LocalDate.of(2026, 11, 14), LocalDate.of(2026, 12, 5))
    private val comeback = LimitedEvent("snow_b", Character.SNOWMAN, LocalDate.of(2027, 11, 14), LocalDate.of(2027, 12, 5))
    private val events = listOf(event, comeback)

    private fun ms(date: LocalDate, hour: Int = 0, minute: Int = 0): Long =
        date.atStartOfDay(zone).plusHours(hour.toLong()).plusMinutes(minute.toLong()).toInstant().toEpochMilli()

    private val during = ms(LocalDate.of(2026, 11, 20), 12)
    private val before = ms(LocalDate.of(2026, 11, 13), 23, 59)
    private val after = ms(LocalDate.of(2026, 12, 6))

    private fun progress(
        calm: Long = 0,
        eventMinutes: Map<String, Long> = emptyMap(),
        extra: Set<Reward> = emptySet(),
    ) = Progress(
        calmMinutes = calm,
        unlocked = Unlocks.earnedBy(calm) + extra,
        eventMinutes = eventMinutes,
    )

    @Test
    fun activeEventCharacterListedFirstThenBackInPlace() {
        val usual = listOf(Character.BUDDY, Character.KITTEN, Character.DUCK, Character.ROBOT)
        assertEquals(usual, LimitedEvents.charactersInOrder(progress(), events, before, zone))
        assertEquals(listOf(Character.SNOWMAN) + usual, LimitedEvents.charactersInOrder(progress(), events, during, zone))
        // Missed: gone. Kept: back at the end, in its usual place.
        assertEquals(usual, LimitedEvents.charactersInOrder(progress(), events, after, zone))
        val kept = progress(extra = setOf(CharacterStyle.SNOWMAN_FROST))
        assertEquals(usual + Character.SNOWMAN, LimitedEvents.charactersInOrder(kept, events, after, zone))
    }

    @Test
    fun windowBoundaries() {
        val start = event.startMs(zone)
        assertFalse(event.isActive(start - 1, zone))
        assertTrue(event.isActive(start, zone))
        val lastMs = ms(LocalDate.of(2026, 12, 6)) - 1
        assertTrue(event.isActive(lastMs, zone))
        assertFalse(event.isActive(lastMs + 1, zone))
        assertEquals(lastMs + 1, event.endMs(zone))
    }

    @Test
    fun windowFollowsTheZone() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        assertTrue(event.startMs(tokyo) < event.startMs(zone))
        assertTrue(event.isActive(event.startMs(tokyo), tokyo))
        assertFalse(event.isActive(event.startMs(tokyo), zone))
    }

    @Test
    fun daysLeftCountsTheLastDay() {
        assertEquals(22, event.daysLeft(LocalDate.of(2026, 11, 14)))
        assertEquals(2, event.daysLeft(LocalDate.of(2026, 12, 4)))
        assertEquals(1, event.daysLeft(LocalDate.of(2026, 12, 5)))
    }

    @Test
    fun activeAndActiveFor() {
        assertEquals(listOf(event), LimitedEvents.active(events, during, zone))
        assertTrue(LimitedEvents.active(events, before, zone).isEmpty())
        assertEquals(event, LimitedEvents.activeFor(Character.SNOWMAN, events, during, zone))
        assertNull(LimitedEvents.activeFor(Character.BUDDY, events, during, zone))
        assertNull(LimitedEvents.activeFor(Character.SNOWMAN, events, after, zone))
    }

    @Test
    fun earnedInPaysForTheCharactersLadderOnly() {
        assertTrue(LimitedEvents.earnedIn(event, 59).isEmpty())
        assertEquals(setOf(CharacterStyle.SNOWMAN_FROST), LimitedEvents.earnedIn(event, 60))
        assertEquals(setOf(CharacterStyle.SNOWMAN_FROST, Accessory.SCARF), LimitedEvents.earnedIn(event, 169))
        assertEquals(
            setOf(CharacterStyle.SNOWMAN_FROST, Accessory.SCARF, CharacterStyle.SNOWMAN_DUSK, Accessory.BEANIE),
            LimitedEvents.earnedIn(event, 225),
        )
    }

    @Test
    fun limitedRewardsNeverUnlockFromTheLifetimeTotal() {
        val p = progress(calm = 100_000)
        assertFalse(p.isUnlocked(CharacterStyle.SNOWMAN_FROST))
        assertFalse(p.isUnlocked(Character.SNOWMAN))
        assertTrue(Reward.all.filter { it.isLimited }.none { it in Progress.FREE })
    }

    @Test
    fun beforeStartIsHidden() {
        val p = progress()
        assertFalse(LimitedEvents.isVisible(Character.SNOWMAN, p, events, before, zone))
        assertFalse(LimitedEvents.isVisible(Accessory.SCARF, p, events, before, zone))
    }

    @Test
    fun activeUnearnedIsVisible() {
        val p = progress()
        assertTrue(LimitedEvents.isVisible(Character.SNOWMAN, p, events, during, zone))
        assertTrue(LimitedEvents.isVisible(Accessory.SCARF, p, events, during, zone))
    }

    @Test
    fun activeEarnedIsVisible() {
        val p = progress(extra = setOf(CharacterStyle.SNOWMAN_FROST, Accessory.SCARF))
        assertTrue(LimitedEvents.isVisible(Character.SNOWMAN, p, events, during, zone))
        assertTrue(LimitedEvents.isVisible(Accessory.SCARF, p, events, during, zone))
    }

    @Test
    fun endedEarnedStaysAndUnearnedDisappears() {
        val p = progress(extra = setOf(CharacterStyle.SNOWMAN_FROST, Accessory.SCARF))
        assertTrue(LimitedEvents.isVisible(Character.SNOWMAN, p, events, after, zone))
        assertTrue(LimitedEvents.isVisible(Accessory.SCARF, p, events, after, zone))
        assertFalse(LimitedEvents.isVisible(Accessory.BEANIE, p, events, after, zone))
        assertFalse(LimitedEvents.isVisible(CharacterStyle.SNOWMAN_DUSK, p, events, after, zone))
    }

    @Test
    fun endedWithoutTheDefaultStyleHidesTheCharacter() {
        assertFalse(LimitedEvents.isVisible(Character.SNOWMAN, progress(), events, after, zone))
    }

    @Test
    fun unlimitedRewardsAreAlwaysVisible() {
        val p = progress()
        assertTrue(LimitedEvents.isVisible(Character.BUDDY, p, emptyList(), during, zone))
        assertTrue(LimitedEvents.isVisible(CharacterStyle.KITTEN_GREY, p, emptyList(), during, zone))
        assertTrue(LimitedEvents.isVisible(Accessory.CROWN, p, emptyList(), during, zone))
    }

    @Test
    fun comebackCountsFromZeroAndKeepsOwnedItems() {
        val comebackTime = ms(LocalDate.of(2027, 11, 20), 12)
        // Lifetime ladder finished, so only the event can be next.
        val p = progress(
            calm = 5000,
            eventMinutes = mapOf(event.key to 130L, comeback.key to 0L),
            extra = LimitedEvents.earnedIn(event, 130),
        )
        assertEquals(0, LimitedEvents.minutesTowards(Accessory.BEANIE, p, events, comebackTime, zone))
        assertTrue(p.isUnlocked(CharacterStyle.SNOWMAN_FROST))
        assertTrue(p.isUnlocked(Accessory.SCARF))
        // The missing items are earnable again from 0; the owned ones are not offered.
        val next = LimitedEvents.next(p, events, comebackTime, zone)!!
        assertEquals(CharacterStyle.SNOWMAN_DUSK, next.reward)
        assertEquals(170, next.minutesToGo)
        assertEquals(0f, next.fraction, 0f)
    }

    @Test
    fun minutesTowardsSplitsLifetimeAndEvent() {
        val p = progress(calm = 500, eventMinutes = mapOf(event.key to 42L))
        assertEquals(500, LimitedEvents.minutesTowards(CharacterStyle.KITTEN_GREY, p, events, during, zone))
        assertEquals(42, LimitedEvents.minutesTowards(Accessory.SCARF, p, events, during, zone))
        assertEquals(0, LimitedEvents.minutesTowards(Accessory.SCARF, p, events, after, zone))
        assertEquals(0, LimitedEvents.minutesTowards(Accessory.SCARF, progress(), events, during, zone))
    }

    @Test
    fun nextWithoutEventsIsTheLifetimeReward() {
        val n = LimitedEvents.next(progress(calm = 45), events, before, zone)!!
        assertEquals(Accessory.SUNGLASSES, n.reward)
        assertEquals(15, n.minutesToGo)
        assertEquals(0.5f, n.fraction, 0.001f)
    }

    @Test
    fun nextPicksTheFewestMinutesToGo() {
        // Lifetime: 45 calm -> SUNGLASSES at 60 (15 to go). Event: 50 minutes -> FROST at 60 (10 to go).
        val eventWins = progress(calm = 45, eventMinutes = mapOf(event.key to 50L))
        val n = LimitedEvents.next(eventWins, events, during, zone)!!
        assertEquals(CharacterStyle.SNOWMAN_FROST, n.reward)
        assertEquals(10, n.minutesToGo)
        assertEquals(50f / 60f, n.fraction, 0.001f)

        // Event: 10 minutes -> 50 to go, lifetime has 15 to go.
        val lifetimeWins = progress(calm = 45, eventMinutes = mapOf(event.key to 10L))
        assertEquals(Accessory.SUNGLASSES, LimitedEvents.next(lifetimeWins, events, during, zone)!!.reward)
    }

    @Test
    fun nextTieGoesToTheLifetimeReward() {
        val p = progress(calm = 45, eventMinutes = mapOf(event.key to 45L))
        assertEquals(Accessory.SUNGLASSES, LimitedEvents.next(p, events, during, zone)!!.reward)
    }

    @Test
    fun nextFractionRunsFromThePreviousStepOnTheEventLadder() {
        val p = progress(
            calm = 5000,
            eventMinutes = mapOf(event.key to 100L),
            extra = LimitedEvents.earnedIn(event, 100),
        )
        val n = LimitedEvents.next(p, events, during, zone)!!
        assertEquals(Accessory.SCARF, n.reward)
        assertEquals(20, n.minutesToGo)
        assertEquals(40f / 60f, n.fraction, 0.001f)
    }

    @Test
    fun nextSkipsAFinishedEventAndIsNullWhenEverythingIsEarned() {
        val all = Reward.all.toSet()
        val done = Progress(calmMinutes = 5000, unlocked = all, eventMinutes = mapOf(event.key to 500L))
        assertNull(LimitedEvents.next(done, events, during, zone))
        val p = progress(calm = 5000, eventMinutes = mapOf(event.key to 50L))
        assertNull(LimitedEvents.next(p, events, after, zone))
    }

    @Test
    fun nextIsAtLeastOneMinuteToGo() {
        val p = progress(calm = 0, eventMinutes = mapOf(event.key to 59L))
        assertEquals(1, LimitedEvents.next(p, events, during, zone)!!.minutesToGo)
    }
    @Test
    fun mariDomingiVisitsFromDecemberFirstToTheTwentySixthEveryYear() {
        val christmas = LimitedEvents.scheduled.filter { it.key.matches(Regex("mari_domingi_\\d{4}")) }
        for (year in listOf(2026, 2027, 2035)) {
            val opens = ms(LocalDate.of(year, 12, 1))
            val closes = ms(LocalDate.of(year, 12, 27))
            assertNull(LimitedEvents.activeFor(Character.MARI_DOMINGI, christmas, opens - 1, zone))
            assertEquals("mari_domingi_$year", LimitedEvents.activeFor(Character.MARI_DOMINGI, christmas, opens, zone)?.key)
            assertEquals("mari_domingi_$year", LimitedEvents.activeFor(Character.MARI_DOMINGI, christmas, closes - 1, zone)?.key)
            assertNull(LimitedEvents.activeFor(Character.MARI_DOMINGI, christmas, closes, zone))
        }
    }

    @Test
    fun scheduledEventsOfOneCharacterNeverOverlapAndHaveUniqueKeys() {
        val scheduled = LimitedEvents.scheduled
        assertEquals(scheduled.size, scheduled.map { it.key }.toSet().size)
        scheduled.groupBy { it.character }.values.forEach { same ->
            same.sortedBy { it.from }.zipWithNext().forEach { (a, b) -> assertTrue("${a.key} overlaps ${b.key}", a.until < b.from) }
        }
    }

    @Test
    fun mariDomingiRewardsUnlockOnHerEventMinutes() {
        val mari = LimitedEvent("m", Character.MARI_DOMINGI, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 26))
        assertEquals(emptySet<Reward>(), LimitedEvents.earnedIn(mari, 59))
        assertEquals(setOf(CharacterStyle.MARI_AMONA), LimitedEvents.earnedIn(mari, 60))
        assertEquals(
            setOf(
                CharacterStyle.MARI_AMONA, CharacterStyle.MARI_MOREA, CharacterStyle.MARI_URDINA,
                Accessory.EGUZKILORE, Accessory.ZAPIA, Accessory.LANTERN, Accessory.ROBIN,
            ),
            LimitedEvents.earnedIn(mari, 300),
        )
    }
}
