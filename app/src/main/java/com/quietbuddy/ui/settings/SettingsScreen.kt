package com.quietbuddy.ui.settings

import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import com.quietbuddy.ui.SingleColumnMaxWidth
import com.quietbuddy.ui.TwoPaneMaxWidth
import com.quietbuddy.ui.isTwoPane
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.quietbuddy.core.AppLanguage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.Character
import com.quietbuddy.core.Mood
import com.quietbuddy.core.Preset
import com.quietbuddy.domain.character.Voices
import com.quietbuddy.ui.character.CharacterView
import com.quietbuddy.ui.character.LocalCharacter
import com.quietbuddy.ui.theme.LocalPalette
import com.quietbuddy.ui.theme.QB
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun SettingsScreen(onOpenCalibration: () -> Unit) {
    val palette = LocalPalette.current
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showRoomDialog by rememberSaveable { mutableStateOf(false) }
    var roomDraft by rememberSaveable { mutableStateOf("") }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    // Slider value while the user drags; committed on release.
    var dragRange by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // The setting is saved regardless of the answer.
    }

    val header: @Composable () -> Unit = {
        Column {
            Text(settings.roomName, style = MaterialTheme.typography.bodyMedium, color = palette.inkSoft)
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        }
    }

    // Character card
    val character: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().background(palette.card, CardShape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_character), style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().selectableGroup(),
            ) {
                Character.entries.forEach { c ->
                    val selected = settings.character == c
                    val shape = RoundedCornerShape(14.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (selected) palette.accentSoft else palette.background, shape)
                            .border(2.dp, if (selected) palette.accent else palette.line, shape)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { viewModel.setCharacter(c) },
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CompositionLocalProvider(LocalCharacter provides c) {
                            CharacterView(mood = Mood.HAPPY, modifier = Modifier.size(64.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected, onClick = null)
                            Text(
                                stringResource(Voices.of(c).name),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selected) palette.accent else palette.ink,
                            )
                        }
                    }
                }
            }
        }
    }

    // Sensitivity card
    val sensitivity: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().background(palette.card, CardShape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_sensitivity), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    Preset.QUIET_TIME to stringResource(R.string.preset_quiet_time),
                    Preset.EVERYDAY to stringResource(R.string.preset_everyday),
                    Preset.PLAYTIME to stringResource(R.string.preset_playtime),
                ).forEach { (preset, label) ->
                    val selected = settings.preset == preset
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(shape)
                            .background(if (selected) palette.accentSoft else palette.background, shape)
                            .border(2.dp, if (selected) palette.accent else palette.line, shape)
                            .clickable(role = Role.RadioButton, onClick = { viewModel.selectPreset(preset) })
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selected) palette.accent else palette.ink,
                        )
                    }
                }
            }
            val displayed: ClosedFloatingPointRange<Float> =
                dragRange ?: (settings.worriedFromDb..settings.sadFromDb)
            RangeSlider(
                value = displayed,
                onValueChange = { dragRange = it },
                onValueChangeFinished = {
                    dragRange?.let { viewModel.setThresholds(it.start, it.endInclusive) }
                    dragRange = null
                },
                valueRange = 30f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = palette.accent,
                    activeTrackColor = palette.accent,
                ),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("30 dB", style = MaterialTheme.typography.labelSmall)
                Text("100 dB", style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    // Switch rows
    val switches: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().background(palette.card, CardShape).padding(vertical = 4.dp),
        ) {
            SwitchRow(
                title = stringResource(R.string.settings_chime),
                subtitle = stringResource(R.string.settings_chime_sub),
                checked = settings.chimeOnSad,
                onCheckedChange = viewModel::setChimeOnSad,
            )
            SwitchRow(
                title = stringResource(R.string.settings_keep_listening),
                subtitle = stringResource(R.string.settings_keep_listening_sub),
                checked = settings.keepListening,
                onCheckedChange = { on ->
                    viewModel.setKeepListening(on)
                    if (on && Build.VERSION.SDK_INT >= 33) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            SwitchRow(
                title = stringResource(R.string.settings_keep_screen_on),
                subtitle = stringResource(R.string.settings_keep_screen_on_sub),
                checked = settings.keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOn,
            )
        }
    }

    // Navigation rows
    val navRows: @Composable () -> Unit = {
        val offset = (settings.calibrationOffsetDb - AppSettings.DEFAULT_CALIBRATION_OFFSET_DB).roundToInt()
        val calibratedText = settings.calibratedAtMs?.let {
            stringResource(R.string.settings_last_done, SimpleDateFormat("d MMM", Locale.getDefault()).format(it))
        } ?: ""
        Column(
            modifier = Modifier.fillMaxWidth().background(palette.card, CardShape).padding(vertical = 4.dp),
        ) {
            NavRow(
                title = stringResource(R.string.settings_room_name),
                subtitle = settings.roomName,
                onClick = {
                    roomDraft = settings.roomName
                    showRoomDialog = true
                },
            )
            NavRow(
                title = stringResource(R.string.cal_title),
                subtitle = stringResource(
                    R.string.settings_offset,
                    String.format(Locale.US, "%+d", offset),
                    calibratedText,
                ),
                onClick = onOpenCalibration,
            )
            NavRow(
                title = stringResource(R.string.settings_language),
                subtitle = languageLabel(AppLanguage.current(context)),
                onClick = { showLanguageDialog = true },
            )
        }
    }

    // Privacy note
    val privacy: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().background(QB.CalmSoft, CardShape).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.settings_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = QB.CalmText,
            )
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding(),
    ) {
        val twoPane = isTwoPane(maxWidth, maxHeight)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = if (twoPane) TwoPaneMaxWidth else SingleColumnMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            header()
            if (twoPane) {
                // Landscape: how Buddy reacts on the left, room, calibration and privacy on the right.
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        character()
                        sensitivity()
                        switches()
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        navRows()
                        privacy()
                    }
                }
            } else {
                character()
                sensitivity()
                switches()
                navRows()
                privacy()
            }
        }
    }

    if (showLanguageDialog) {
        val currentTag = AppLanguage.current(context)
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    AppLanguage.options.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(role = Role.RadioButton) {
                                    showLanguageDialog = false
                                    if (tag != currentTag) (context as? Activity)?.let { AppLanguage.set(it, tag) }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = tag == currentTag, onClick = null)
                            Text(languageLabel(tag), Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showRoomDialog) {
        AlertDialog(
            onDismissRequest = { showRoomDialog = false },
            title = { Text(stringResource(R.string.settings_room_name), style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = roomDraft,
                    onValueChange = { roomDraft = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_room)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setRoomName(roomDraft)
                    showRoomDialog = false
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRoomDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun languageLabel(tag: String): String = when (tag) {
    "en" -> "English"
    "es" -> "Español"
    "eu" -> "Euskara"
    else -> stringResource(R.string.settings_language_system)
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Switch, onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = palette.accent),
        )
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall)
        }
        Text(">", style = MaterialTheme.typography.titleMedium, color = palette.inkSoft)
    }
}

