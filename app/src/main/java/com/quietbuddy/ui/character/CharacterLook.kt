package com.quietbuddy.ui.character

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.quietbuddy.core.Character
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.kitten.KittenArt
import com.quietbuddy.ui.theme.BuddyPalette
import com.quietbuddy.ui.theme.KittenPalette
import com.quietbuddy.ui.theme.Palette

/** Which shared motions [CharacterView] applies to a character. */
data class Motion(
    val bounceOnMoodChange: Boolean = true,
    val shakeWhenEarsCovered: Boolean = true,
    val breatheWhenAsleep: Boolean = true,
)

/** How one character is drawn. */
interface CharacterArt {
    val motion: Motion

    /** Draws the character in a 200x200 design space; [mood] null means asleep. */
    fun DrawScope.draw(mood: Mood?, palette: Palette)
}

/** Art that blends between poses and runs idle motion; [CharacterView] drives it frame by frame. */
interface AnimatedCharacterArt : CharacterArt {
    /** A fresh animator showing [mood] at rest. */
    fun createAnimator(mood: Mood?): CharacterAnimator
}

/** The per-view state of an [AnimatedCharacterArt]. */
interface CharacterAnimator {
    /** Moves towards [mood]; with [animate] false it jumps there. */
    fun setMood(mood: Mood?, animate: Boolean)

    /** Moves time on by [dtSec] seconds. */
    fun advance(dtSec: Float)

    /** Draws the current frame in the 200x200 design space. */
    fun DrawScope.drawFrame(palette: Palette)
}

/** A character's art and palette. */
@Immutable
class CharacterLook(val art: CharacterArt, val palette: Palette)

object CharacterLooks {
    private val Buddy = CharacterLook(BuddyArt, BuddyPalette)
    private val Kitten = CharacterLook(KittenArt, KittenPalette)

    fun of(character: Character): CharacterLook = when (character) {
        Character.BUDDY -> Buddy
        Character.KITTEN -> Kitten
    }
}

/** The character the UI is currently themed with. */
val LocalCharacter = staticCompositionLocalOf { Character.DEFAULT }

/** The current character's look. */
val LocalLook: CharacterLook
    @Composable @ReadOnlyComposable get() = CharacterLooks.of(LocalCharacter.current)
