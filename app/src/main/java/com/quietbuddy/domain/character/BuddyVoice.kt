package com.quietbuddy.domain.character

import com.quietbuddy.R
import com.quietbuddy.core.Mood

/** Buddy's kid-friendly voice. */
object BuddyVoice : Voice {
    override val name = R.string.buddy_name
    override val sleeping = R.string.speech_sleeping
    override val greeting = R.string.onb_hi
    override val challengeWon = R.string.challenge_won
    override val listeningTitle = R.string.svc_title

    private val lines: Map<Mood, List<Int>> = mapOf(
        Mood.HAPPY to listOf(R.string.speech_happy_1, R.string.speech_happy_2, R.string.speech_happy_3),
        Mood.FINE to listOf(R.string.speech_fine_1, R.string.speech_fine_2, R.string.speech_fine_3),
        Mood.WORRIED to listOf(R.string.speech_worried_1, R.string.speech_worried_2, R.string.speech_worried_3),
        Mood.SAD to listOf(R.string.speech_sad_1, R.string.speech_sad_2, R.string.speech_sad_3),
        Mood.EARS_COVERED to listOf(R.string.speech_ears_1, R.string.speech_ears_2, R.string.speech_ears_3),
    )

    override fun lines(mood: Mood): List<Int> = lines.getValue(mood)

    override fun description(mood: Mood?): Int = when (mood) {
        null -> R.string.buddy_asleep
        Mood.HAPPY -> R.string.buddy_happy
        Mood.FINE -> R.string.buddy_fine
        Mood.WORRIED -> R.string.buddy_worried
        Mood.SAD -> R.string.buddy_sad
        Mood.EARS_COVERED -> R.string.buddy_ears
    }
}
