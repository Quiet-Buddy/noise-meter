package com.quietbuddy.audio

import com.quietbuddy.core.LevelSample
import com.quietbuddy.core.MinuteAggregate
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Turns PCM frames into calibrated A-weighted levels, a rolling LAeq and per-minute aggregates. */
class LevelProcessor(
    private val sampleRate: Int,
    var calibrationOffsetDb: Float,
    private val windowMs: Long = DEFAULT_WINDOW_MS,
) {
    data class Result(val sample: LevelSample, val completedMinute: MinuteAggregate?)

    private val filter = AWeightingFilter(sampleRate)
    private var scratch = DoubleArray(0)

    // Rolling window: frame energy (linear) and duration (ms), oldest first.
    private val winEnergy = ArrayDeque<Double>()
    private val winDurMs = ArrayDeque<Double>()
    private var winDurTotalMs = 0.0

    // Current minute accumulation.
    private var minuteStart = Long.MIN_VALUE
    private var minEnergySum = 0.0
    private var minFrames = 0
    private var minMin = Float.MAX_VALUE
    private var minMax = -Float.MAX_VALUE
    private var minSeconds = 0.0

    fun process(frame: ShortArray, count: Int, timestampMs: Long): Result {
        if (scratch.size < count) scratch = DoubleArray(count)
        filter.process(frame, count, scratch)
        var sumSq = 0.0
        for (i in 0 until count) sumSq += scratch[i] * scratch[i]
        val rms = if (count > 0) sqrt(sumSq / count) else 0.0
        val frameDb = max(0.0, 20.0 * log10(max(rms, 1e-9)) + calibrationOffsetDb).toFloat()
        val durationS = count.toDouble() / sampleRate

        // Rolling LAeq over the window.
        val energy = 10.0.pow(frameDb / 10.0)
        val durMs = durationS * 1000.0
        winEnergy.addLast(energy)
        winDurMs.addLast(durMs)
        winDurTotalMs += durMs
        while (winEnergy.size > 1 && winDurTotalMs - winDurMs.first() >= windowMs) {
            winDurTotalMs -= winDurMs.removeFirst()
            winEnergy.removeFirst()
        }
        var eSum = 0.0
        for (e in winEnergy) eSum += e
        val laeq2s = (10.0 * log10(eSum / winEnergy.size)).toFloat()

        // Minute rollover.
        val key = Math.floorDiv(timestampMs, 60_000L) * 60_000L
        var completed: MinuteAggregate? = null
        if (minuteStart != Long.MIN_VALUE && key != minuteStart) {
            completed = flushMinute()
        }
        if (minuteStart == Long.MIN_VALUE) minuteStart = key
        minEnergySum += energy
        minFrames++
        if (frameDb < minMin) minMin = frameDb
        if (frameDb > minMax) minMax = frameDb
        minSeconds += durationS

        return Result(LevelSample(timestampMs, frameDb, laeq2s), completed)
    }

    /** Flush the partial current minute (e.g. on pause). Null if nothing accumulated. */
    fun flushMinute(): MinuteAggregate? {
        if (minFrames == 0 || minuteStart == Long.MIN_VALUE) return null
        val agg = MinuteAggregate(
            minuteStartMs = minuteStart,
            laeqDb = (10.0 * log10(minEnergySum / minFrames)).toFloat(),
            minDb = minMin,
            maxDb = minMax,
            listeningSeconds = minSeconds.roundToInt(),
        )
        resetMinute()
        return agg
    }

    fun reset() {
        filter.reset()
        winEnergy.clear()
        winDurMs.clear()
        winDurTotalMs = 0.0
        resetMinute()
    }

    private fun resetMinute() {
        minuteStart = Long.MIN_VALUE
        minEnergySum = 0.0
        minFrames = 0
        minMin = Float.MAX_VALUE
        minMax = -Float.MAX_VALUE
        minSeconds = 0.0
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 2_000L
    }
}
