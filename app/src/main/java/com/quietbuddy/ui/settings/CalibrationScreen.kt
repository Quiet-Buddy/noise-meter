package com.quietbuddy.ui.settings

import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import com.quietbuddy.ui.SingleColumnMaxWidth
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

    Box(
        Modifier
            .fillMaxSize()
            .background(QB.Background)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // A form, so it stays one column; on wide screens it is capped in width and centred.
        Column(
            modifier = Modifier
                .widthIn(max = SingleColumnMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cal_back), tint = QB.Ink)
                }
                Text(stringResource(R.string.cal_title), style = MaterialTheme.typography.headlineSmall)
            }

            Column(
                modifier = Modifier.fillMaxWidth().background(QB.Card, RoundedCornerShape(20.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.cal_hears_now), style = MaterialTheme.typography.labelSmall)
                Text(
                    current?.let { String.format(Locale.US, "%.0f dB", it) } ?: "-- dB",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(R.string.cal_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = referenceText,
                    onValueChange = { referenceText = it },
                    label = { Text(stringResource(R.string.cal_reading_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { reference?.let { viewModel.save(it, onBack) } },
                    enabled = reference != null && current != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.common_save))
                }
                TextButton(
                    onClick = { viewModel.resetToDefault() },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.cal_reset))
                }
            }

            Text(
                stringResource(R.string.cal_silent_hint),
                style = MaterialTheme.typography.labelSmall,
                color = QB.InkSoft,
            )
        }
    }
}
