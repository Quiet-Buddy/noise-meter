package com.quietbuddy.ui.home

import com.quietbuddy.ui.formatDuration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietbuddy.core.Mood
import com.quietbuddy.domain.character.Voices
import com.quietbuddy.ui.character.CharacterView
import com.quietbuddy.ui.character.LocalCharacter
import com.quietbuddy.ui.character.LocalStyle
import com.quietbuddy.ui.character.LocalWardrobe
import com.quietbuddy.ui.character.nameRes
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.Character
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.Reward
import androidx.compose.runtime.CompositionLocalProvider
import com.quietbuddy.ui.theme.LocalPalette
import com.quietbuddy.ui.theme.QB

// The two tiles under the noise meter: calm time today and the next reward.
/**
 * Today's calm time and the next reward, as two separate tiles side by side: one is what the room has done, the
 * other what it is working towards. The reward tile opens the Characters tab. With everything unlocked the calm
 * tile takes the whole row.
 */
@Composable
internal fun ProgressTiles(
    calmMinutes: Int,
    reward: Reward?,
    styles: Map<Character, CharacterStyle>,
    minutesToNext: Int,
    progress: Float,
    onClick: () -> Unit,
) {
    // Equal heights, so the two tiles read as a pair even when one label wraps.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = spacedBy(12.dp)) {
        CalmTile(calmMinutes, Modifier.weight(1f).fillMaxHeight())
        if (reward != null) {
            RewardTile(reward, styles, minutesToNext, progress, onClick, Modifier.weight(1f).fillMaxHeight())
        }
    }
}
internal val TileShape = RoundedCornerShape(20.dp)
@Composable
private fun CalmTile(calmMinutes: Int, modifier: Modifier) {
    val palette = LocalPalette.current
    Column(modifier.clip(TileShape).background(palette.card).padding(12.dp)) {
        Text(stringResource(R.string.home_calm_time_today), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft)
        Spacer(Modifier.weight(1f).heightIn(min = 4.dp))
        // May wrap between hours and minutes at large font sizes rather than be cut off.
        Text(
            formatDuration(calmMinutes),
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp),
            color = QB.CalmText,
        )
    }
}
@Composable
private fun RewardTile(
    reward: Reward,
    styles: Map<Character, CharacterStyle>,
    minutesToNext: Int,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val palette = LocalPalette.current
    val animatedProgress by animateFloatAsState(progress, tween(600), label = "rewardProgress")
    // The full "N min to unlock X" sentence is too long for a tile in some languages, so it is only the
    // screen-reader label; on screen the reward's name stands alone and the minutes sit at the end of the bar.
    val sentence = nextRewardText(reward, minutesToNext)
    Column(
        modifier
            .clip(TileShape)
            .background(palette.card)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = sentence }
            .padding(12.dp),
    ) {
        // The label sits on top across the whole tile, like the calm tile's, so it has room in long languages
        // and large font sizes; the preview and the reward's name share the row below it.
        Text(stringResource(R.string.home_next_reward_label), style = MaterialTheme.typography.bodySmall, color = palette.inkSoft)
        Spacer(Modifier.weight(1f).heightIn(min = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = spacedBy(8.dp)) {
            RewardPreview(reward, styles, Modifier.size(32.dp), animated = false)
            Text(
                rewardName(reward),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = palette.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(palette.line)) {
                Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(QB.Calm))
            }
            Text(
                formatDuration(minutesToNext),
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkSoft,
                softWrap = false,
            )
        }
    }
}
/** What the next reward is called: a new friend by the character's name, anything else by its own name. */
@Composable
private fun rewardName(reward: Reward): String =
    if (reward is CharacterStyle && reward.isCharacterDefault) stringResource(Voices.of(reward.character).name)
    else stringResource(reward.nameRes)
@Composable
private fun nextRewardText(reward: Reward, minutes: Int): String = when {
    reward is CharacterStyle && reward.isCharacterDefault ->
        stringResource(R.string.home_next_reward_friend, minutes, stringResource(Voices.of(reward.character).name))
    reward is Accessory -> stringResource(R.string.home_next_reward_accessory, minutes, stringResource(reward.nameRes))
    else -> stringResource(R.string.home_next_reward, minutes, stringResource(reward.nameRes))
}
/** A happy character showing off [reward]: a style on its own character, an accessory on its own character in the selected style. */
@Composable
internal fun RewardPreview(reward: Reward, styles: Map<Character, CharacterStyle>, modifier: Modifier, animated: Boolean) {
    when (reward) {
        is CharacterStyle -> CompositionLocalProvider(LocalCharacter provides reward.character, LocalStyle provides reward) {
            CharacterView(mood = Mood.HAPPY, animated = animated, modifier = modifier)
        }
        is Accessory -> {
            val worn = LocalWardrobe.current[reward.character].orEmpty()
            val style = styles[reward.character] ?: CharacterStyle.defaultFor(reward.character)
            CompositionLocalProvider(LocalCharacter provides reward.character, LocalStyle provides style) {
                CharacterView(mood = Mood.HAPPY, animated = animated, modifier = modifier, accessories = worn + reward)
            }
        }
    }
}
