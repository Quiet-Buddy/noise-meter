package com.quietbuddy.ui.characters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietbuddy.R
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.Character
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.LimitedEvent
import com.quietbuddy.core.LimitedEvents
import com.quietbuddy.core.Mood
import com.quietbuddy.core.Progress
import com.quietbuddy.core.Reward
import com.quietbuddy.ui.character.CharacterView
import com.quietbuddy.ui.character.LocalCharacter
import com.quietbuddy.ui.character.LocalStyle
import com.quietbuddy.ui.character.nameRes
import com.quietbuddy.ui.theme.LocalPalette
import java.time.ZoneId

/** Wraps a character preview; when locked, overlays a lock icon (the preview itself is dimmed by the caller). */
@Composable
internal fun LockedPreview(unlocked: Boolean, size: Dp, content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        content()
        if (!unlocked) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = LocalPalette.current.ink, modifier = Modifier.size(if (size >= 64.dp) 28.dp else 24.dp))
        }
    }
}

/** One tile per style of [character]; locked ones show the calm-minute progress and cannot be picked. */
@Composable
internal fun StyleTiles(
    character: Character,
    selectedStyle: CharacterStyle?,
    progress: Progress,
    events: List<LimitedEvent>,
    nowMs: Long,
    zone: ZoneId,
    onSelect: (CharacterStyle) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().selectableGroup(),
    ) {
        CharacterStyle.of(character).filter { LimitedEvents.isVisible(it, progress, events, nowMs, zone) }.forEach { style ->
            val unlocked = progress.isUnlocked(style)
            val isSelected = style == selectedStyle
            RewardTile(
                reward = style,
                name = stringResource(style.nameRes),
                unlocked = unlocked,
                highlighted = isSelected,
                calmMinutes = LimitedEvents.minutesTowards(style, progress, events, nowMs, zone),
                interaction = Modifier.selectable(
                    selected = isSelected,
                    enabled = unlocked,
                    role = Role.RadioButton,
                    onClick = { onSelect(style) },
                ),
            ) {
                CompositionLocalProvider(LocalCharacter provides character, LocalStyle provides style) {
                    CharacterView(mood = Mood.HAPPY, animated = false, modifier = it)
                }
            }
        }
    }
}

/**
 * One tile per accessory of [character], four to a row, each showing it wearing just that accessory in [style].
 * Tapping an unlocked one puts it on or takes it off; worn ones are highlighted.
 */
@Composable
internal fun AccessoryTiles(
    character: Character,
    style: CharacterStyle,
    worn: Set<Accessory>,
    progress: Progress,
    events: List<LimitedEvent>,
    nowMs: Long,
    zone: ZoneId,
    onToggle: (Accessory) -> Unit,
) {
    val palette = LocalPalette.current
    val wornLabel = stringResource(R.string.accessory_worn)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.characters_accessories), style = MaterialTheme.typography.labelMedium, color = palette.inkSoft)
        Accessory.of(character).filter { LimitedEvents.isVisible(it, progress, events, nowMs, zone) }.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { accessory ->
                    val unlocked = progress.isUnlocked(accessory)
                    val isWorn = accessory in worn
                    RewardTile(
                        reward = accessory,
                        name = stringResource(accessory.nameRes),
                        unlocked = unlocked,
                        highlighted = isWorn,
                        calmMinutes = LimitedEvents.minutesTowards(accessory, progress, events, nowMs, zone),
                        interaction = Modifier
                            .toggleable(value = isWorn, enabled = unlocked, role = Role.Checkbox, onValueChange = { onToggle(accessory) })
                            .semantics { if (isWorn) stateDescription = wornLabel },
                    ) {
                        CompositionLocalProvider(LocalCharacter provides character, LocalStyle provides style) {
                            CharacterView(mood = Mood.HAPPY, animated = false, modifier = it, accessories = setOf(accessory))
                        }
                    }
                }
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** A tile with a 56dp preview and a name; locked ones are dimmed, show a lock and the calm-minute progress. */
@Composable
private fun RowScope.RewardTile(
    reward: Reward,
    name: String,
    unlocked: Boolean,
    highlighted: Boolean,
    calmMinutes: Long,
    interaction: Modifier,
    preview: @Composable (Modifier) -> Unit,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(14.dp)
    val lockedDescription = if (unlocked) name else stringResource(
        R.string.style_locked_desc,
        name,
        pluralStringResource(R.plurals.style_needs_minutes, reward.unlockAtCalmMinutes, reward.unlockAtCalmMinutes),
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(if (highlighted) palette.accentSoft else palette.background, shape)
            .border(2.dp, if (highlighted) palette.accent else palette.line, shape)
            .then(if (unlocked) interaction else Modifier)
            .semantics(mergeDescendants = true) { if (!unlocked) contentDescription = lockedDescription }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LockedPreview(unlocked = unlocked, size = 56.dp) {
            preview(Modifier.size(56.dp).alpha(if (unlocked) 1f else 0.35f))
        }
        Text(
            name,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlighted) palette.accent else palette.ink,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!unlocked) {
            Text(
                stringResource(R.string.style_progress, calmMinutes.coerceAtMost(reward.unlockAtCalmMinutes.toLong()), reward.unlockAtCalmMinutes),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
