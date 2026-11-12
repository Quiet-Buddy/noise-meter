package com.quietbuddy.ui.character.kitten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.Character
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.character.AnimatedCharacterArt
import com.quietbuddy.ui.character.CharacterAnimator
import com.quietbuddy.ui.character.CharacterView
import com.quietbuddy.ui.character.LocalCharacter
import com.quietbuddy.ui.character.Motion
import com.quietbuddy.ui.theme.Palette

/**
 * Kitten: an orange tabby whose expression is a set of numbers blended between moods. It does its own pose-weighted
 * shake and breathing, so CharacterView's shared ones are off; only the mood-change bounce is shared.
 */
object KittenArt : AnimatedCharacterArt {
    override val motion = Motion(
        bounceOnMoodChange = true,
        shakeWhenEarsCovered = false,
        breatheWhenAsleep = false,
    )

    /** The resting pose, no idle motion. */
    override fun DrawScope.draw(mood: Mood?, palette: Palette, accessories: Set<Accessory>) =
        drawKitten(KittenPoses.of(mood), KittenFx.Still, palette, accessories)

    override fun createAnimator(mood: Mood?): CharacterAnimator = object : CharacterAnimator {
        private val animator = KittenAnimator(mood)

        override fun setMood(mood: Mood?, animate: Boolean) = animator.setMood(mood, animate)

        override fun advance(dtSec: Float) = animator.advance(dtSec)

        override fun DrawScope.drawFrame(palette: Palette, accessories: Set<Accessory>) =
            drawKitten(animator.pose, animator.fx, palette, accessories)
    }
}

@Preview(showBackground = true)
@Composable
private fun KittenMoodsPreview() {
    CompositionLocalProvider(LocalCharacter provides Character.KITTEN) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            listOf<Mood?>(Mood.HAPPY, Mood.FINE, Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)
                .forEach { CharacterView(it, Modifier.size(56.dp), animated = false) }
        }
    }
}
