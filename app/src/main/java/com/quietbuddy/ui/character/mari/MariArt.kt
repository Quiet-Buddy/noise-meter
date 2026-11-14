package com.quietbuddy.ui.character.mari

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
 * Mari Domingi: a girl in Basque dress with a wrapped white tontorra whose curled hook acts out the moods, and a
 * basket of apples on her arm. Her expression is a set of numbers blended between moods. She does her own
 * pose-weighted tremble and breathing, so CharacterView's shared ones are off; only the mood-change bounce is shared.
 * The pat's squash and hearts come from the home screen's tap effects, so she adds her delighted face, a head lean
 * and the hook's boing, plus the combo tricks for quick pats in calm moods: 3 pats toss an apple, 6 a jump with a
 * burst of petals, 9 an aurresku dance.
 */
object MariArt : AnimatedCharacterArt {
    override val motion = Motion(
        bounceOnMoodChange = true,
        shakeWhenEarsCovered = false,
        breatheWhenAsleep = false,
    )

    /** The resting pose, no idle motion. */
    override fun DrawScope.draw(mood: Mood?, palette: Palette, accessories: Set<Accessory>) =
        drawMari(MariPoses.of(mood), MariFx.Still, palette, accessories)

    override fun createAnimator(mood: Mood?): CharacterAnimator = object : CharacterAnimator {
        private val animator = MariAnimator(mood)

        override fun setMood(mood: Mood?, animate: Boolean) = animator.setMood(mood, animate)

        override fun advance(dtSec: Float) = animator.advance(dtSec)

        override fun poke() = animator.poke()

        override fun DrawScope.drawFrame(palette: Palette, accessories: Set<Accessory>) =
            drawMari(animator.pose, animator.fx, palette, accessories)
    }
}

@Preview(showBackground = true)
@Composable
private fun MariMoodsPreview() {
    CompositionLocalProvider(LocalCharacter provides Character.MARI_DOMINGI) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            listOf<Mood?>(Mood.HAPPY, Mood.FINE, Mood.WORRIED, Mood.SAD, Mood.EARS_COVERED, null)
                .forEach { CharacterView(it, Modifier.size(56.dp), animated = false) }
        }
    }
}
