package com.quietbuddy.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** A local date together with the zone it was worked out in; either changing means day windows must be redone. */
data class DayStamp(val date: LocalDate, val zone: ZoneId)

/**
 * Today's local date as a flow, so "today" screens move on at midnight instead of keeping the day they were
 * opened on (a classroom tablet is often left on overnight). It wakes at the next local midnight, or after
 * [checkEveryMs] if that comes first, so a clock or time zone change is picked up within a minute.
 */
@Singleton
class DayClock(
    private val nowMs: () -> Long,
    private val zone: () -> ZoneId,
    private val checkEveryMs: Long = 60_000L,
) {
    @Inject constructor() : this(System::currentTimeMillis, ZoneId::systemDefault)

    fun today(): LocalDate = Instant.ofEpochMilli(nowMs()).atZone(zone()).toLocalDate()

    /** Emits today's date and zone now, then again whenever either changes. */
    val stamps: Flow<DayStamp> = flow {
        while (true) {
            val now = nowMs()
            val z = zone()
            val date = Instant.ofEpochMilli(now).atZone(z).toLocalDate()
            emit(DayStamp(date, z))
            val untilMidnightMs = date.plusDays(1).atStartOfDay(z).toInstant().toEpochMilli() - now
            delay(minOf(checkEveryMs, untilMidnightMs).coerceAtLeast(1L))
        }
    }.distinctUntilChanged()

    /** Emits today's date now, then again whenever it changes. */
    val days: Flow<LocalDate> = stamps.map { it.date }.distinctUntilChanged()

    /** Emits the time zone now, then again whenever it changes. */
    val zones: Flow<ZoneId> = stamps.map { it.zone }.distinctUntilChanged()
}
