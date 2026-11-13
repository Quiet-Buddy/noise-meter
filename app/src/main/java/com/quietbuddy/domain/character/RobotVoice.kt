package com.quietbuddy.domain.character

import com.quietbuddy.R
import com.quietbuddy.core.Mood

/** Astro the robot's voice: beeps, circuits, antenna, sensors and batteries. */
object RobotVoice : Voice {
    override val name = R.string.robot_name
    override val sleeping = R.string.robot_sleeping
    override val greeting = R.string.robot_greeting
    override val listeningTitle = R.string.robot_listening_title

    private val lines: Map<Mood, List<Int>> = mapOf(
        Mood.HAPPY to listOf(R.string.robot_happy_1, R.string.robot_happy_2, R.string.robot_happy_3),
        Mood.FINE to listOf(R.string.robot_fine_1, R.string.robot_fine_2, R.string.robot_fine_3),
        Mood.WORRIED to listOf(R.string.robot_worried_1, R.string.robot_worried_2, R.string.robot_worried_3),
        Mood.SAD to listOf(R.string.robot_sad_1, R.string.robot_sad_2, R.string.robot_sad_3),
        // Astro clamps his hands over his ear discs.
        Mood.EARS_COVERED to listOf(R.string.robot_ears_1, R.string.robot_ears_2, R.string.robot_ears_3),
    )

    override fun lines(mood: Mood): List<Int> = lines.getValue(mood)

    private val tapLines = listOf(
        R.string.robot_tap_1, R.string.robot_tap_2, R.string.robot_tap_3, R.string.robot_tap_4, R.string.robot_tap_5,
    )

    override fun tapLines(): List<Int> = tapLines

    override fun description(mood: Mood?): Int = when (mood) {
        null -> R.string.robot_asleep
        Mood.HAPPY -> R.string.robot_happy
        Mood.FINE -> R.string.robot_fine
        Mood.WORRIED -> R.string.robot_worried
        Mood.SAD -> R.string.robot_sad
        Mood.EARS_COVERED -> R.string.robot_ears
    }
}
