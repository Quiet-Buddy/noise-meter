package com.quietbuddy.domain

import androidx.annotation.StringRes
import com.quietbuddy.R
import com.quietbuddy.core.Mood

/** Buddy's kid-friendly speech bubbles, one list per mood (as string resources, so they can be translated). */
object SpeechLines {

    @StringRes
    val SLEEPING: Int = R.string.speech_sleeping

    private val lines: Map<Mood, List<Int>> = mapOf(
        Mood.HAPPY to listOf(R.string.speech_happy_1, R.string.speech_happy_2, R.string.speech_happy_3),
        Mood.FINE to listOf(R.string.speech_fine_1, R.string.speech_fine_2, R.string.speech_fine_3),
        Mood.WORRIED to listOf(R.string.speech_worried_1, R.string.speech_worried_2, R.string.speech_worried_3),
        Mood.SAD to listOf(R.string.speech_sad_1, R.string.speech_sad_2, R.string.speech_sad_3),
        Mood.EARS_COVERED to listOf(R.string.speech_ears_1, R.string.speech_ears_2, R.string.speech_ears_3),
    )

    @StringRes
    fun linesFor(mood: Mood): List<Int> = lines.getValue(mood)

    /** Deterministic rotation: index picks line (index mod size). */
    @StringRes
    fun line(mood: Mood, index: Int): Int {
        val all = linesFor(mood)
        val i = ((index % all.size) + all.size) % all.size
        return all[i]
    }
}
