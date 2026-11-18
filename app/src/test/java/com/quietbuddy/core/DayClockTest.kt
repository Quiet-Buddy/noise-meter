package com.quietbuddy.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DayClockTest {
    private val nov30 = LocalDate.of(2026, 11, 30)
    private var now = nov30.atTime(23, 59, 30).toInstant(ZoneOffset.UTC).toEpochMilli()
    private var zone: ZoneId = ZoneOffset.UTC
    private val clock = DayClock(nowMs = { now }, zone = { zone })

    @Test fun datePassesToTheNewDayRightAtMidnight() = runTest {
        val days = mutableListOf<LocalDate>()
        backgroundScope.launch { clock.days.take(2).toList(days) }
        runCurrent()

        // Half a minute on is midnight, well before the next minute-long poll.
        now += 30_000
        advanceTimeBy(30_001)
        runCurrent()
        assertEquals(listOf(nov30, nov30.plusDays(1)), days)
    }

    @Test fun zoneChangeIsEmittedEvenWhenTheDateStaysTheSame() = runTest {
        now = nov30.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val stamps = mutableListOf<DayStamp>()
        val dates = mutableListOf<LocalDate>()
        backgroundScope.launch { clock.stamps.take(2).toList(stamps) }
        backgroundScope.launch { clock.days.toList(dates) }
        runCurrent()

        zone = ZoneOffset.ofHours(2)
        advanceTimeBy(60_001)
        runCurrent()
        assertEquals(listOf(DayStamp(nov30, ZoneOffset.UTC), DayStamp(nov30, ZoneOffset.ofHours(2))), stamps)
        assertEquals(listOf(nov30), dates)
    }
}
