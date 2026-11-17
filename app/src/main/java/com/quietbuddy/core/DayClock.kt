package com.quietbuddy.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Today's local date as a flow, so "today" screens move on at midnight instead of keeping the day they were
 * opened on (a classroom tablet is often left on overnight). Checked once a minute rather than timed to
 * midnight, so a clock or time zone change is picked up too.
 */
@Singleton
class DayClock(
    private val nowMs: () -> Long,
    private val zone: () -> ZoneId,
    private val checkEveryMs: Long = 60_000L,
) {
    @Inject constructor() : this(System::currentTimeMillis, ZoneId::systemDefault)

    fun today(): LocalDate = Instant.ofEpochMilli(nowMs()).atZone(zone()).toLocalDate()

    /** Emits today's date now, then again whenever it changes. */
    val days: Flow<LocalDate> = flow {
        while (true) {
            emit(today())
            delay(checkEveryMs)
        }
    }.distinctUntilChanged()
}
