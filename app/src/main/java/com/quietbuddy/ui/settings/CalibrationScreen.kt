package com.quietbuddy.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietbuddy.ui.theme.QB
import java.util.Locale

@Composable
fun CalibrationScreen(onBack: () -> Unit) {
    val viewModel: CalibrationViewModel = hiltViewModel()
    val latest by viewModel.latest.collectAsStateWithLifecycle()
    var referenceText by rememberSaveable { mutableStateOf("") }
    val reference = referenceText.toFloatOrNull()
    val current = latest?.laeq2sDb

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QB.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = QB.Ink)
            }
            Text("Calibrate microphone", style = MaterialTheme.typography.headlineSmall)
        }

        Column(
            modifier = Modifier.fillMaxWidth().background(QB.Card, RoundedCornerShape(20.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Buddy hears right now", style = MaterialTheme.typography.labelSmall)
            Text(
                current?.let { String.format(Locale.US, "%.0f dB", it) } ?: "-- dB",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "Hold a sound meter next to the phone in a steady sound and type what it shows.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = referenceText,
                onValueChange = { referenceText = it },
                label = { Text("Sound meter reading (dB)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { reference?.let { viewModel.save(it, onBack) } },
                enabled = reference != null && current != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Save")
            }
            TextButton(
                onClick = { viewModel.resetToDefault() },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text("Reset to default")
            }
        }

        Text(
            "In a silent room Buddy should read below 45 dB.",
            style = MaterialTheme.typography.labelSmall,
            color = QB.InkSoft,
        )
    }
}
