package com.quietbuddy.domain.character

import androidx.annotation.StringRes
import com.quietbuddy.core.Character
import com.quietbuddy.core.Mood

/**
 * Everything a character says, as string resources so each character is written natively in every
 * language (no "%s is happy" name substitution: Basque and Spanish inflect names and adjectives).
 */
interface Voice {
    /** The character's name, e.g. "Buddy". */
    @get:StringRes val name: Int

    /** Speech bubble while asleep (not listening). */
    @get:StringRes val sleeping: Int

    /** Speech bubbles for [mood]; at least one. */
    @StringRes fun lines(mood: Mood): List<Int>

    /** Speech when a child taps the character while the room is calm; at least one. */
    @StringRes fun tapLines(): List<Int>

    /** Accessibility description of the character in [mood]; null means asleep. */
    @StringRes fun description(mood: Mood?): Int

    /** Onboarding introduction, e.g. "Hi! I'm Buddy." */
    @get:StringRes val greeting: Int

    /** Notification title while listening; takes the room name as %1$s. */
    @get:StringRes val listeningTitle: Int

    /** Deterministic rotation: [index] picks a line (index mod size). */
    @StringRes
    fun line(mood: Mood, index: Int): Int {
        val all = lines(mood)
        return all[((index % all.size) + all.size) % all.size]
    }
}

object Voices {
    fun of(character: Character): Voice = when (character) {
        Character.BUDDY -> BuddyVoice
        Character.KITTEN -> KittenVoice
        Character.DUCK -> DuckVoice
        Character.ROBOT -> DuckVoice // TODO(robot): placeholder until Astro's own voice exists
    }
}
