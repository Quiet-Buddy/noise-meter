package com.quietbuddy.domain.character

import com.quietbuddy.R
import com.quietbuddy.core.Mood

/** Mari Domingi's voice: apples, the moon, the mountain farmhouse, sheep and the winter cold. */
object MariVoice : Voice {
    override val name = R.string.mari_name
    override val sleeping = R.string.mari_sleeping
    override val greeting = R.string.mari_greeting
    override val listeningTitle = R.string.mari_listening_title

    private val lines: Map<Mood, List<Int>> = mapOf(
        Mood.HAPPY to listOf(R.string.mari_happy_1, R.string.mari_happy_2, R.string.mari_happy_3),
        Mood.FINE to listOf(R.string.mari_fine_1, R.string.mari_fine_2, R.string.mari_fine_3),
        Mood.WORRIED to listOf(R.string.mari_worried_1, R.string.mari_worried_2, R.string.mari_worried_3),
        Mood.SAD to listOf(R.string.mari_sad_1, R.string.mari_sad_2, R.string.mari_sad_3),
        // Mari covers her ears with both hands and drops her basket.
        Mood.EARS_COVERED to listOf(R.string.mari_ears_1, R.string.mari_ears_2, R.string.mari_ears_3),
    )

    override fun lines(mood: Mood): List<Int> = lines.getValue(mood)

    private val tapLines = listOf(
        R.string.mari_tap_1, R.string.mari_tap_2, R.string.mari_tap_3, R.string.mari_tap_4,
        R.string.mari_tap_5,
    )

    override fun tapLines(): List<Int> = tapLines

    override fun description(mood: Mood?): Int = when (mood) {
        null -> R.string.mari_asleep
        Mood.HAPPY -> R.string.mari_happy
        Mood.FINE -> R.string.mari_fine
        Mood.WORRIED -> R.string.mari_worried
        Mood.SAD -> R.string.mari_sad
        Mood.EARS_COVERED -> R.string.mari_ears
    }
}
