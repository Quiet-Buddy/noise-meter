package com.quietbuddy.ui.characters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietbuddy.R
import com.quietbuddy.core.Character
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.Unlocks
import com.quietbuddy.domain.character.Voices
import com.quietbuddy.ui.SingleColumnMaxWidth
import com.quietbuddy.ui.character.nameRes
import com.quietbuddy.ui.theme.LocalPalette

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun CharactersScreen() {
    val palette = LocalPalette.current
    val viewModel: CharactersViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(palette.background).statusBarsPadding()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = SingleColumnMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(settings.roomName, style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft)
                Text(stringResource(R.string.characters_title), style = MaterialTheme.typography.headlineSmall)
            }

            if (!settings.unlockAllStyles) {
                ProgressCard(calmMinutes = progress.calmMinutes)
            }

            Character.entries.forEach { c ->
                val unlocked = progress.isUnlocked(c, settings)
                val friend = CharacterStyle.defaultFor(c)
                Column(
                    modifier = Modifier.fillMaxWidth().background(palette.card, CardShape).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(Voices.of(c).name),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (!unlocked) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.inkSoft, modifier = Modifier.size(16.dp))
                            Text(
                                stringResource(
                                    R.string.style_progress,
                                    progress.calmMinutes.coerceAtMost(friend.unlockAtCalmMinutes.toLong()),
                                    friend.unlockAtCalmMinutes,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.inkSoft,
                            )
                        } else if (settings.character == c) {
                            Text(
                                stringResource(R.string.characters_in_use),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accent,
                                modifier = Modifier
                                    .background(palette.accentSoft, RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    StyleTiles(
                        character = c,
                        selectedStyle = if (settings.character == c) settings.styleFor(c) else null,
                        settings = settings,
                        progress = progress,
                        onSelect = viewModel::select,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(calmMinutes: Long) {
    val palette = LocalPalette.current
    val next = Unlocks.next(calmMinutes)
    val previous = CharacterStyle.entries.filter { it.unlockAtCalmMinutes <= calmMinutes }.maxOfOrNull { it.unlockAtCalmMinutes } ?: 0
    val fraction = if (next == null) 1f else {
        ((calmMinutes - previous).toFloat() / (next.unlockAtCalmMinutes - previous)).coerceIn(0f, 1f)
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(palette.card, CardShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.characters_calm_collected), style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft)
        Text(calmMinutes.toString(), style = MaterialTheme.typography.displaySmall)
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
            color = palette.accent,
            trackColor = palette.line,
        )
        Text(
            if (next == null) {
                stringResource(R.string.style_all_unlocked)
            } else {
                val remaining = (next.unlockAtCalmMinutes - calmMinutes).toInt().coerceAtLeast(1)
                if (next.isCharacterDefault) {
                    pluralStringResource(R.plurals.style_next_friend_hint, remaining, remaining, stringResource(Voices.of(next.character).name))
                } else {
                    pluralStringResource(R.plurals.style_next_hint, remaining, remaining, stringResource(next.nameRes))
                }
            },
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
