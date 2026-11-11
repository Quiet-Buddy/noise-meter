package com.quietbuddy.domain

import com.quietbuddy.core.Mood

/** Buddy's kid-friendly speech bubbles, one list per mood. */
object SpeechLines {

    const val SLEEPING = "Zzz… tap the microphone to wake me up."

    private val lines: Map<Mood, List<String>> = mapOf(
        Mood.HAPPY to listOf(
            "Ahh, I can hear the birds outside.",
            "Everything is calm and cozy.",
            "Shh, it's nice and peaceful today.",
        ),
        Mood.FINE to listOf(
            "Nice. Everyone's chatting.",
            "Just the right amount of noise.",
            "Good talking, good listening.",
        ),
        Mood.WORRIED to listOf(
            "Hmm, it's getting a bit loud in here.",
            "Let's bring the volume down a little.",
            "Could we play a little more quietly?",
        ),
        Mood.SAD to listOf(
            "Ouch. Can we use our indoor voices?",
            "That's a lot of noise. My tummy feels fuzzy.",
            "Please help me find some quiet.",
        ),
        Mood.EARS_COVERED to listOf(
            "Too loud! My ears!",
            "Whoa! I'm covering my ears!",
            "Big noise! Let's take a quiet break!",
        ),
    )

    fun linesFor(mood: Mood): List<String> = lines.getValue(mood)

    /** Deterministic rotation: index picks line (index mod size). */
    fun line(mood: Mood, index: Int): String {
        val all = linesFor(mood)
        val i = ((index % all.size) + all.size) % all.size
        return all[i]
    }
}
