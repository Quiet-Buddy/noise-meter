package com.quietbuddy.ui.onboarding

import androidx.compose.ui.res.stringResource
import com.quietbuddy.R
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.Dp
import com.quietbuddy.ui.SingleColumnMaxWidth
import com.quietbuddy.ui.TwoPaneMaxWidth
import com.quietbuddy.ui.isLargeScreen
import com.quietbuddy.ui.isTwoPane
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietbuddy.core.Mood
import com.quietbuddy.ui.buddy.Buddy
import com.quietbuddy.ui.theme.QB

private val OnboardingBackground = Color(0xFFFFF7E3)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val viewModel: OnboardingViewModel = hiltViewModel()

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Whatever the answer, onboarding is finished. Home asks again later if it is still needed.
        viewModel.finish(onDone)
    }

    val large = isLargeScreen()

    val welcome: @Composable (buddySize: Dp) -> Unit = { buddySize ->
        Buddy(mood = Mood.HAPPY, modifier = Modifier.size(buddySize))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.onb_hi),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onb_intro),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
    val infoRows: @Composable () -> Unit = {
        InfoRow(Icons.Rounded.Headphones, stringResource(R.string.onb_measure), stringResource(R.string.onb_measure_sub))
        Spacer(Modifier.height(12.dp))
        InfoRow(Icons.Rounded.MicOff, stringResource(R.string.onb_never_record), stringResource(R.string.onb_never_record_sub))
        Spacer(Modifier.height(12.dp))
        InfoRow(Icons.Rounded.PhoneAndroid, stringResource(R.string.onb_stay), stringResource(R.string.onb_stay_sub))
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        if (isTwoPane(maxWidth, maxHeight)) {
            // Landscape: the welcome on the left, the promises and the buttons on the right.
            Row(
                Modifier
                    .align(Alignment.Center)
                    .widthIn(max = TwoPaneMaxWidth)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    // Leave about 120dp for the greeting and intro under Buddy.
                    val buddySize = (maxHeight - 120.dp).coerceIn(96.dp, if (large) 300.dp else 220.dp)
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        welcome(buddySize)
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        infoRows()
                        Spacer(Modifier.height(16.dp))
                    }
                    Actions(onListen = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }, onLater = { viewModel.finish(onDone) })
                }
            }
        } else {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = SingleColumnMaxWidth)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(8.dp))
                    welcome(if (large) 280.dp else 220.dp)
                    Spacer(Modifier.height(20.dp))
                    infoRows()
                    Spacer(Modifier.height(16.dp))
                }
                Actions(onListen = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }, onLater = { viewModel.finish(onDone) })
            }
        }
    }
}

/** The "start listening" button and the "maybe later" link, stacked. */
@Composable
private fun Actions(onListen: () -> Unit, onLater: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onListen,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QB.Ink, contentColor = Color.White),
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.onb_listen), style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        TextButton(
            onClick = onLater,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.onb_later), style = MaterialTheme.typography.bodyMedium, color = QB.InkSoft)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = QB.Card),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(QB.AccentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = QB.Accent)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = QB.InkSoft)
            }
        }
    }
}
