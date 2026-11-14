package com.quietbuddy.domain.character

import com.quietbuddy.R
import com.quietbuddy.core.Mood

/** Snowy the snowman's voice: snow, frost, scarves, carrots and staying cool. */
object SnowmanVoice : Voice {
    override val name = R.string.snowman_name
    override val sleeping = R.string.snowman_sleeping
    override val greeting = R.string.snowman_greeting
    override val listeningTitle = R.string.snowman_listening_title

    private val lines: Map<Mood, List<Int>> = mapOf(
        Mood.HAPPY to listOf(R.string.snowman_happy_1, R.string.snowman_happy_2, R.string.snowman_happy_3),
        Mood.FINE to listOf(R.string.snowman_fine_1, R.string.snowman_fine_2, R.string.snowman_fine_3),
        Mood.WORRIED to listOf(R.string.snowman_worried_1, R.string.snowman_worried_2, R.string.snowman_worried_3),
        Mood.SAD to listOf(R.string.snowman_sad_1, R.string.snowman_sad_2, R.string.snowman_sad_3),
        // Snowy clamps its stick arms to the sides of its head.
        Mood.EARS_COVERED to listOf(R.string.snowman_ears_1, R.string.snowman_ears_2, R.string.snowman_ears_3),
    )

    override fun lines(mood: Mood): List<Int> = lines.getValue(mood)

    private val tapLines = listOf(
        R.string.snowman_tap_1, R.string.snowman_tap_2, R.string.snowman_tap_3, R.string.snowman_tap_4,
        R.string.snowman_tap_5,
    )

    override fun tapLines(): List<Int> = tapLines

    override fun description(mood: Mood?): Int = when (mood) {
        null -> R.string.snowman_asleep
        Mood.HAPPY -> R.string.snowman_happy
        Mood.FINE -> R.string.snowman_fine
        Mood.WORRIED -> R.string.snowman_worried
        Mood.SAD -> R.string.snowman_sad
        Mood.EARS_COVERED -> R.string.snowman_ears
    }
}
