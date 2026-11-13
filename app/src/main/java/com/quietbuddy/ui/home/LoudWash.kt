package com.quietbuddy.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.quietbuddy.core.Thresholds

/**
 * The Home background for a live level: a vertical gradient, [top] to [bottom], that slides continuously with the
 * dB instead of jumping between moods. It stays the palette's own colour through the calm band, then deepens through
 * light amber (getting loud) and coral (too loud) to red (way too loud). Each step is darker as well as warmer, since
 * a change in lightness is what reads from across a room. Even the deepest top keeps 3:1 contrast with the palettes'
 * dark ink, so text drawn straight on the wash (the room name, in large type) stays in ink rather than turning white.
 */
data class LoudWash(val top: Color, val bottom: Color)

private val LoudTop = Color(0xFFF6C97A)
private val LoudBottom = Color(0xFFFDEBCB)
private val TooLoudTop = Color(0xFFEE9583)
private val TooLoudBottom = Color(0xFFFBDCD4)
private val WayTooLoudTop = Color(0xFFD9513F)
private val WayTooLoudBottom = Color(0xFFF4B3A6)

/**
 * The wash for [db] (null when not listening) on a palette whose background is [calm]. The colour stops sit on the
 * room's own band edges, so the background agrees with the meter whatever the thresholds are set to.
 */
fun loudWash(db: Float?, thresholds: Thresholds, calm: Color): LoudWash {
    if (db == null) return LoudWash(calm, calm)
    val stops = listOf(
        Triple(thresholds.fineFromDb, calm, calm),
        Triple(thresholds.worriedFromDb, LoudTop, LoudBottom),
        Triple(thresholds.sadFromDb, TooLoudTop, TooLoudBottom),
        Triple(thresholds.earsFromDb, WayTooLoudTop, WayTooLoudBottom),
    )
    if (db <= stops.first().first) return LoudWash(calm, calm)
    for (i in 1 until stops.size) {
        val (d1, top1, bottom1) = stops[i]
        if (db <= d1) {
            val (d0, top0, bottom0) = stops[i - 1]
            val t = (db - d0) / (d1 - d0)
            return LoudWash(lerp(top0, top1, t), lerp(bottom0, bottom1, t))
        }
    }
    return LoudWash(WayTooLoudTop, WayTooLoudBottom)
}
