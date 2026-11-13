package com.quietbuddy.domain.character

import com.quietbuddy.R
import com.quietbuddy.core.Mood

/** Pip the duck's voice: quacks, ripples, feathers and wings. */
object DuckVoice : Voice {
    override val name = R.string.duck_name
    override val sleeping = R.string.duck_sleeping
    override val greeting = R.string.duck_greeting
    override val listeningTitle = R.string.duck_listening_title

    private val lines: Map<Mood, List<Int>> = mapOf(
        Mood.HAPPY to listOf(R.string.duck_happy_1, R.string.duck_happy_2, R.string.duck_happy_3),
        Mood.FINE to listOf(R.string.duck_fine_1, R.string.duck_fine_2, R.string.duck_fine_3),
        Mood.WORRIED to listOf(R.string.duck_worried_1, R.string.duck_worried_2, R.string.duck_worried_3),
        Mood.SAD to listOf(R.string.duck_sad_1, R.string.duck_sad_2, R.string.duck_sad_3),
        // The duck clamps its wings over the sides of its head.
        Mood.EARS_COVERED to listOf(R.string.duck_ears_1, R.string.duck_ears_2, R.string.duck_ears_3),
    )

    override fun lines(mood: Mood): List<Int> = lines.getValue(mood)

    private val tapLines = listOf(
        R.string.duck_tap_1, R.string.duck_tap_2, R.string.duck_tap_3, R.string.duck_tap_4, R.string.duck_tap_5,
    )

    override fun tapLines(): List<Int> = tapLines

    override fun description(mood: Mood?): Int = when (mood) {
        null -> R.string.duck_asleep
        Mood.HAPPY -> R.string.duck_happy
        Mood.FINE -> R.string.duck_fine
        Mood.WORRIED -> R.string.duck_worried
        Mood.SAD -> R.string.duck_sad
        Mood.EARS_COVERED -> R.string.duck_ears
    }
}
