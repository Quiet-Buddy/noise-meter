package com.quietbuddy.ui.character

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.quietbuddy.core.Character
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.theme.BuddyPalette
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

/** A character's art and palette. */
@Immutable
class CharacterLook(val art: CharacterArt, val palette: Palette)

object CharacterLooks {
    private val Buddy = CharacterLook(BuddyArt, BuddyPalette)

    fun of(character: Character): CharacterLook = when (character) {
        Character.BUDDY -> Buddy
        Character.KITTEN -> Buddy // TODO(kitten): KittenArt + KittenPalette
    }
}

/** The character the UI is currently themed with. */
val LocalCharacter = staticCompositionLocalOf { Character.DEFAULT }

/** The current character's look. */
val LocalLook: CharacterLook
    @Composable @ReadOnlyComposable get() = CharacterLooks.of(LocalCharacter.current)
