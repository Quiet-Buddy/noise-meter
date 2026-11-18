package com.quietbuddy.ui.home

import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import androidx.compose.ui.semantics.onClick
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietbuddy.domain.character.Voices
import com.quietbuddy.ui.character.nameRes
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.Character
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.Reward
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.TextButton

/** Tells the user about newly earned rewards and offers to try the most exciting one: a new friend, else the newest. */
@Composable
internal fun UnlockDialog(unlocks: List<Reward>, styles: Map<Character, CharacterStyle>, onTry: (Reward) -> Unit, onLater: () -> Unit) {
    val friend = unlocks.firstOrNull { it is CharacterStyle && it.isCharacterDefault } as CharacterStyle?
    val newest = friend ?: unlocks.maxByOrNull { it.unlockAtCalmMinutes } ?: return
    val name = if (friend != null) stringResource(Voices.of(friend.character).name) else stringResource(newest.nameRes)
    val more = unlocks.size - 1
    val (title, body) = when {
        friend != null -> R.string.unlock_friend_title to
            if (more > 0) pluralStringResource(R.plurals.unlock_friend_body_more, more, name, more)
            else stringResource(R.string.unlock_friend_body, name)
        newest is Accessory -> R.string.unlock_accessory_title to
            if (more > 0) pluralStringResource(R.plurals.unlock_accessory_body_more, more, name, more)
            else stringResource(R.string.unlock_accessory_body, name)
        else -> R.string.unlock_title to
            if (more > 0) pluralStringResource(R.plurals.unlock_body_more, more, name, more)
            else stringResource(R.string.unlock_body, name)
    }
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(title), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RewardPreview(newest, styles, Modifier.size(120.dp), animated = true)
                Spacer(Modifier.height(12.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            TextButton(onClick = { onTry(newest) }) {
                Text(stringResource(if (newest is Accessory) R.string.unlock_wear else R.string.unlock_try))
            }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.unlock_later)) } },
    )
}
