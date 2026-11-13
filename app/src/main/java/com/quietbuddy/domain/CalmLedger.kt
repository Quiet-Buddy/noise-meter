package com.quietbuddy.domain

import com.quietbuddy.core.LedgerState
import com.quietbuddy.core.Thresholds

/**
 * Turns the live level into earned calm minutes, charging noise as a debt that calm time pays back first.
 *
 * - Calm time (below "worried") earns; every 60 s of it, once debt is paid, is one calm minute.
 * - Loud time earns nothing and adds debt: [WORRIED_RATE] s per second in the worried band (loud chatter),
 *   [SAD_RATE] in the sad band, [EARS_RATE] from "ears covered" up (yelling).
 * - Yelling (sad and up) comes in episodes, which end after [EPISODE_GAP_MS] below "sad". An episode's
 *   yelling costs double for every earlier episode that started in the last [ESCALATION_WINDOW_MS], so a
 *   one-off yell is cheap but a habit of yelling is not. The multiplier is fixed when the episode starts.
 * - Debt never exceeds [MAX_DEBT_MS], so one bad stretch can't cost the rest of the day.
 *
 * Nothing is ever taken back from the counter. Pure; unit-tested. Not thread-safe: feed it from one thread.
 */
class CalmLedger(initial: LedgerState = LedgerState()) {
    private var debtMs = initial.debtMs.coerceIn(0, MAX_DEBT_MS)
    private var calmMs = initial.calmMs.coerceIn(0, MINUTE_MS - 1)
    private val episodeStarts = ArrayDeque(initial.episodeStartsMs.sorted())
    private var updatedAtMs = initial.updatedAtMs

    private var lastMs: Long? = null
    private var inEpisode = false
    private var episodeMultiplier = 1L
    private var belowSadMs = EPISODE_GAP_MS

    val state: LedgerState
        get() = LedgerState(debtMs, calmMs, episodeStarts.toList(), updatedAtMs)

    /** Feed one LAeq 2 s value. Returns the calm minutes earned by the time since the previous call. */
    fun update(levelDb: Float, thresholds: Thresholds, nowMs: Long): Int {
        val dt = elapsed(nowMs)
        updatedAtMs = nowMs
        val yelling = levelDb >= thresholds.sadFromDb
        if (yelling) {
            if (!inEpisode) startEpisode(nowMs)
            belowSadMs = 0
        } else {
            belowSadMs += dt
            if (belowSadMs >= EPISODE_GAP_MS) inEpisode = false
        }

        if (levelDb < thresholds.worriedFromDb) {
            val paid = minOf(debtMs, dt)
            debtMs -= paid
            calmMs += dt - paid
            val minutes = (calmMs / MINUTE_MS).toInt()
            calmMs %= MINUTE_MS
            return minutes
        }
        val rate = when {
            levelDb >= thresholds.earsFromDb -> EARS_RATE * episodeMultiplier
            yelling -> SAD_RATE * episodeMultiplier
            else -> WORRIED_RATE
        }
        debtMs = minOf(MAX_DEBT_MS, debtMs + dt * rate)
        return 0
    }

    /** Lets time pass without counting it either way (mic silenced, or our own alert tone in the mic). */
    fun skip(nowMs: Long) {
        lastMs = nowMs
    }

    private fun elapsed(nowMs: Long): Long {
        val prev = lastMs
        lastMs = nowMs
        return if (prev == null) 0 else (nowMs - prev).coerceIn(0, MAX_FRAME_GAP_MS)
    }

    private fun startEpisode(nowMs: Long) {
        while (episodeStarts.isNotEmpty() && nowMs - episodeStarts.first() >= ESCALATION_WINDOW_MS) {
            episodeStarts.removeFirst()
        }
        episodeMultiplier = 1L shl minOf(episodeStarts.size, MAX_DOUBLINGS)
        episodeStarts.addLast(nowMs)
        inEpisode = true
    }

    companion object {
        const val MINUTE_MS = 60_000L
        const val WORRIED_RATE = 1L
        const val SAD_RATE = 15L
        const val EARS_RATE = 25L
        const val EPISODE_GAP_MS = 30_000L
        const val ESCALATION_WINDOW_MS = 30 * MINUTE_MS
        const val MAX_DEBT_MS = 20 * MINUTE_MS
        private const val MAX_DOUBLINGS = 10
        /** Longer gaps between samples (a stalled capture thread) count as this much, not as minutes. */
        private const val MAX_FRAME_GAP_MS = 1_000L
    }
}
