package com.quietbuddy.domain.character

import com.quietbuddy.R
import com.quietbuddy.core.Mood

/** Mochi the kitten's voice: purrs, whiskers, paws and puffed-up fur. */
object KittenVoice : Voice {
    override val name = R.string.kitten_name
    override val sleeping = R.string.kitten_sleeping
    override val greeting = R.string.kitten_greeting
    override val challengeWon = R.string.kitten_challenge_won
    override val listeningTitle = R.string.kitten_listening_title

    private val lines: Map<Mood, List<Int>> = mapOf(
        Mood.HAPPY to listOf(R.string.kitten_happy_1, R.string.kitten_happy_2, R.string.kitten_happy_3),
        Mood.FINE to listOf(R.string.kitten_fine_1, R.string.kitten_fine_2, R.string.kitten_fine_3),
        Mood.WORRIED to listOf(R.string.kitten_worried_1, R.string.kitten_worried_2, R.string.kitten_worried_3),
        Mood.SAD to listOf(R.string.kitten_sad_1, R.string.kitten_sad_2, R.string.kitten_sad_3),
        // The kitten doesn't cover its ears: it gets startled and puffs up.
        Mood.EARS_COVERED to listOf(R.string.kitten_ears_1, R.string.kitten_ears_2, R.string.kitten_ears_3),
    )

    override fun lines(mood: Mood): List<Int> = lines.getValue(mood)

    private val tapLines = listOf(
        R.string.kitten_tap_1, R.string.kitten_tap_2, R.string.kitten_tap_3, R.string.kitten_tap_4, R.string.kitten_tap_5,
    )

    override fun tapLines(): List<Int> = tapLines

    override fun description(mood: Mood?): Int = when (mood) {
        null -> R.string.kitten_asleep
        Mood.HAPPY -> R.string.kitten_happy
        Mood.FINE -> R.string.kitten_fine
        Mood.WORRIED -> R.string.kitten_worried
        Mood.SAD -> R.string.kitten_sad
        Mood.EARS_COVERED -> R.string.kitten_ears
    }
}
