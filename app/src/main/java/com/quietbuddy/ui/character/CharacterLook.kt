package com.quietbuddy.ui.character

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.Character
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.duck.DuckArt
import com.quietbuddy.ui.character.kitten.KittenArt
import com.quietbuddy.ui.theme.BuddyBerryPalette
import com.quietbuddy.ui.theme.BuddyMintPalette
import com.quietbuddy.ui.theme.BuddyPalette
import com.quietbuddy.ui.theme.DuckCloudPalette
import com.quietbuddy.ui.theme.DuckHazelPalette
import com.quietbuddy.ui.theme.DuckPalette
import com.quietbuddy.ui.theme.KittenGreyPalette
import com.quietbuddy.ui.theme.KittenSnowPalette
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

    /** Draws the character in a 200x200 design space wearing [accessories]; [mood] null means asleep. */
    fun DrawScope.draw(mood: Mood?, palette: Palette, accessories: Set<Accessory>)
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

    /** Draws the current frame in the 200x200 design space, wearing [accessories]. */
    fun DrawScope.drawFrame(palette: Palette, accessories: Set<Accessory>)

    /**
     * The child patted the character. Calm moods react (a giggle); others ignore it. Repeated pokes extend the
     * reaction smoothly rather than restarting it.
     */
    fun poke() {}
}

/** Moods in which a poke makes the character giggle. */
internal val Mood?.isCalm: Boolean get() = this == Mood.HAPPY || this == Mood.FINE

/**
 * A 0..1 reaction level for pokes: rises over [attackSec], holds for [holdSec], then falls over [releaseSec].
 * A new [trigger] rises again from the current level (never drops first), so rapid pokes just keep it up.
 * Pure Kotlin; the caller feeds it time via [advance].
 */
class PokeEnvelope(
    private val attackSec: Float,
    private val holdSec: Float,
    private val releaseSec: Float,
) {
    private var level = 0f
    private var sustainLeft = 0f

    /** The eased level (smoothstep of the linear one), 0 at rest. */
    val value: Float get() = level * level * (3f - 2f * level)

    /** True while rising, holding or falling. */
    val isActive: Boolean get() = level > 0f || sustainLeft > 0f

    fun trigger() {
        sustainLeft = (1f - level) * attackSec + holdSec
    }

    /** Skips the hold and starts falling now, e.g. when the mood stops being calm. */
    fun release() {
        sustainLeft = 0f
    }

    fun advance(dtSec: Float) {
        if (sustainLeft > 0f) {
            level = minOf(1f, level + dtSec / attackSec)
            sustainLeft -= dtSec
        } else {
            level = maxOf(0f, level - dtSec / releaseSec)
        }
    }
}

/** A character's art and palette. */
@Immutable
class CharacterLook(val art: CharacterArt, val palette: Palette)

object CharacterLooks {
    private val looks: Map<CharacterStyle, CharacterLook> = CharacterStyle.entries.associateWith { style ->
        val palette = when (style) {
            CharacterStyle.BUDDY_SUNNY -> BuddyPalette
            CharacterStyle.BUDDY_MINT -> BuddyMintPalette
            CharacterStyle.BUDDY_BERRY -> BuddyBerryPalette
            CharacterStyle.KITTEN_GINGER -> KittenPalette
            CharacterStyle.KITTEN_GREY -> KittenGreyPalette
            CharacterStyle.KITTEN_SNOW -> KittenSnowPalette
            CharacterStyle.DUCK_LEMON -> DuckPalette
            CharacterStyle.DUCK_CLOUD -> DuckCloudPalette
            CharacterStyle.DUCK_HAZEL -> DuckHazelPalette
            // TODO(robot): placeholder until Astro's own palettes exist
            CharacterStyle.ROBOT_PEARL -> DuckCloudPalette
            CharacterStyle.ROBOT_LILAC -> DuckCloudPalette
            CharacterStyle.ROBOT_SKY -> DuckCloudPalette
        }
        CharacterLook(
            art = when (style.character) {
                Character.BUDDY -> BuddyArt
                Character.KITTEN -> KittenArt
                Character.DUCK -> DuckArt
                Character.ROBOT -> DuckArt // TODO(robot): placeholder until Astro's own art exists
            },
            palette = palette,
        )
    }

    fun of(style: CharacterStyle): CharacterLook = looks.getValue(style)

    /** The character's free default style. */
    fun of(character: Character): CharacterLook = of(CharacterStyle.defaultFor(character))
}

/** The character the UI is currently themed with. */
val LocalCharacter = staticCompositionLocalOf { Character.DEFAULT }

/** The current character's style. Ignored (the default style is used) when it belongs to another character. */
val LocalStyle = staticCompositionLocalOf { CharacterStyle.defaultFor(Character.DEFAULT) }

/** What each character is wearing (see AppSettings.accessories); characters missing from the map wear nothing. */
val LocalWardrobe = staticCompositionLocalOf<Map<Character, Set<Accessory>>> { emptyMap() }

/** The current character's look in the current style. */
val LocalLook: CharacterLook
    @Composable @ReadOnlyComposable get() {
        val character = LocalCharacter.current
        val style = LocalStyle.current.takeIf { it.character == character } ?: CharacterStyle.defaultFor(character)
        return CharacterLooks.of(style)
    }
