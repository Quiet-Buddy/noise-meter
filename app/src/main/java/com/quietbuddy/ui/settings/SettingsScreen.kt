package com.quietbuddy.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.quietbuddy.core.Preset
import com.quietbuddy.ui.theme.QB
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun SettingsScreen(onOpenCalibration: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showRoomDialog by rememberSaveable { mutableStateOf(false) }
    var roomDraft by rememberSaveable { mutableStateOf("") }
    // Slider value while the user drags; committed on release.
    var dragRange by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // The setting is saved regardless of the answer.
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QB.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(settings.roomName, style = MaterialTheme.typography.bodyMedium, color = QB.InkSoft)
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

        // Sensitivity card
        Column(
            modifier = Modifier.fillMaxWidth().background(QB.Card, CardShape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("How sensitive is Buddy?", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    Preset.QUIET_TIME to "Quiet time",
                    Preset.EVERYDAY to "Everyday",
                    Preset.PLAYTIME to "Playtime",
                ).forEach { (preset, label) ->
                    val selected = settings.preset == preset
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(shape)
                            .background(if (selected) QB.AccentSoft else QB.Background, shape)
                            .border(2.dp, if (selected) QB.Accent else QB.Line, shape)
                            .clickable(role = Role.RadioButton, onClick = { viewModel.selectPreset(preset) })
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selected) QB.Accent else QB.Ink,
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
                    thumbColor = QB.Accent,
                    activeTrackColor = QB.Accent,
                ),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("30 dB", style = MaterialTheme.typography.labelSmall)
                Text("100 dB", style = MaterialTheme.typography.labelSmall)
            }
            Text(
                "Buddy gets worried at ${displayed.start.roundToInt()} dB and sad at " +
                    "${displayed.endInclusive.roundToInt()} dB. Normal talking is about 60 dB.",
                style = MaterialTheme.typography.bodyMedium,
                color = QB.InkSoft,
            )
        }

        // Switch rows
        Column(
            modifier = Modifier.fillMaxWidth().background(QB.Card, CardShape).padding(vertical = 4.dp),
        ) {
            SwitchRow(
                title = "Quiet hours",
                subtitle = "${formatMinutes(settings.quietStartMinutes)} to " +
                    "${formatMinutes(settings.quietEndMinutes)}, Buddy is " +
                    "${AppSettings.QUIET_HOURS_SHIFT_DB.roundToInt()} dB stricter",
                checked = settings.quietHoursEnabled,
                onCheckedChange = viewModel::setQuietHours,
            )
            SwitchRow(
                title = "Soft chime when Buddy gets sad",
                subtitle = "Once, then quiet for 2 minutes",
                checked = settings.chimeOnSad,
                onCheckedChange = viewModel::setChimeOnSad,
            )
            SwitchRow(
                title = "Keep listening with screen off",
                subtitle = "Shows Buddy in the notification shade",
                checked = settings.keepListening,
                onCheckedChange = { on ->
                    viewModel.setKeepListening(on)
                    if (on && Build.VERSION.SDK_INT >= 33) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }

        // Navigation rows
        val offset = (settings.calibrationOffsetDb - AppSettings.DEFAULT_CALIBRATION_OFFSET_DB).roundToInt()
        val calibratedText = settings.calibratedAtMs?.let {
            ", last done ${SimpleDateFormat("d MMM", Locale.getDefault()).format(it)}"
        } ?: ""
        Column(
            modifier = Modifier.fillMaxWidth().background(QB.Card, CardShape).padding(vertical = 4.dp),
        ) {
            NavRow(
                title = "Room name",
                subtitle = settings.roomName,
                onClick = {
                    roomDraft = settings.roomName
                    showRoomDialog = true
                },
            )
            NavRow(
                title = "Calibrate microphone",
                subtitle = "Offset ${String.format(Locale.US, "%+d", offset)} dB$calibratedText",
                onClick = onOpenCalibration,
            )
        }

        // Privacy note
        Row(
            modifier = Modifier.fillMaxWidth().background(QB.CalmSoft, CardShape).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Buddy never records or sends audio. Everything stays on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = QB.CalmText,
            )
        }
    }

    if (showRoomDialog) {
        AlertDialog(
            onDismissRequest = { showRoomDialog = false },
            title = { Text("Room name", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = roomDraft,
                    onValueChange = { roomDraft = it },
                    singleLine = true,
                    label = { Text("Room") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setRoomName(roomDraft)
                    showRoomDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRoomDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
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
            colors = SwitchDefaults.colors(checkedTrackColor = QB.Accent),
        )
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
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
        Text(">", style = MaterialTheme.typography.titleMedium, color = QB.InkSoft)
    }
}

private fun formatMinutes(minuteOfDay: Int): String =
    String.format(Locale.US, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)
