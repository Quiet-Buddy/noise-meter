package com.quietbuddy.ui.character

import com.quietbuddy.R
import com.quietbuddy.core.Mood

/** Short, translated name of a mood (e.g. for accessibility descriptions). */
@androidx.annotation.StringRes
fun moodLabelRes(mood: Mood): Int = when (mood) {
    Mood.HAPPY -> R.string.svc_mood_happy
    Mood.FINE -> R.string.svc_mood_fine
    Mood.WORRIED -> R.string.svc_mood_worried
    Mood.SAD -> R.string.svc_mood_sad
    Mood.EARS_COVERED -> R.string.svc_mood_ears
}
