package com.quietbuddy.data

import com.quietbuddy.core.DaySummary
import com.quietbuddy.core.HistoryRepository
import com.quietbuddy.core.HourBar
import com.quietbuddy.core.MinuteAggregate
import com.quietbuddy.core.Mood
import com.quietbuddy.core.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.pow

private const val MS_PER_DAY = 86_400_000L
private const val MINUTE_RETENTION_DAYS = 35L

@Singleton
class RoomHistoryRepository @Inject constructor(
    private val dao: HistoryDao,
    private val settings: SettingsRepository,
) : HistoryRepository {

    override suspend fun recordMinute(minute: MinuteAggregate, dominantMood: Mood) {
        dao.insertMinute(
            LevelMinuteEntity(
                minuteStartMs = minute.minuteStartMs,
                laeqDb = minute.laeqDb,
                minDb = minute.minDb,
                maxDb = minute.maxDb,
                dominantMood = dominantMood.name,
                listeningSeconds = minute.listeningSeconds,
            )
        )
    }

    override fun hourly(epochDay: Long): Flow<List<HourBar>> {
        val zone = ZoneId.systemDefault()
        val from = startOfDayMs(LocalDate.ofEpochDay(epochDay), zone)
        val to = startOfDayMs(LocalDate.ofEpochDay(epochDay + 1), zone)
        return dao.minutesBetween(from, to).map { rows ->
            rows.groupBy { Instant.ofEpochMilli(it.minuteStartMs).atZone(zone).hour }
                .toSortedMap()
                .map { (hour, list) -> HourBar(hour, energyAverage(list.map { it.laeqDb })) }
        }
    }

    override fun lastDays(days: Int): Flow<List<DaySummary>> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val firstDay = today.minusDays((days - 1).toLong())
        val from = startOfDayMs(firstDay, zone)
        val to = startOfDayMs(today.plusDays(1), zone)
        return combine(
            dao.minutesBetween(from, to),
            settings.settings,
        ) { minutes, appSettings ->
            val minutesByDay = minutes.groupBy { epochDayOf(it.minuteStartMs, zone) }
            val calmThresholdDb = appSettings.worriedFromDb

            (0 until days).map { offset ->
                val epochDay = firstDay.toEpochDay() + offset
                val rows = minutesByDay[epochDay].orEmpty()
                val loudest = rows.maxByOrNull { it.maxDb }
                val dominant = rows
                    .groupingBy { it.dominantMood }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key
                    ?.let { Mood.valueOf(it) }
                DaySummary(
                    dateEpochDay = epochDay,
                    calmMinutes = rows.count { it.laeqDb < calmThresholdDb },
                    loudestDb = loudest?.maxDb,
                    loudestAtMs = loudest?.minuteStartMs,
                    dominantMood = dominant,
                )
            }
        }
    }

    override suspend fun prune(nowMs: Long) {
        dao.deleteMinutesBefore(nowMs - MINUTE_RETENTION_DAYS * MS_PER_DAY)
    }

    /** Energy average: 10 * log10(mean(10^(dB/10))). Input must be non-empty. */
    private fun energyAverage(levelsDb: List<Float>): Float {
        val meanEnergy = levelsDb.sumOf { 10.0.pow(it / 10.0) } / levelsDb.size
        return (10.0 * log10(meanEnergy)).toFloat()
    }

    private fun startOfDayMs(date: LocalDate, zone: ZoneId): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun epochDayOf(ms: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()
}
