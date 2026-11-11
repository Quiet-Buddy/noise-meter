package com.quietbuddy.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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

    Column(
        Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
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
            Buddy(mood = Mood.HAPPY, modifier = Modifier.size(220.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "Hi! I'm Buddy.",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "I keep an eye on how loud things get at home, so we can all stay calm together.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            InfoRow(Icons.Rounded.Headphones, "I only measure how loud it is", "Just a number, many times a second")
            Spacer(Modifier.height(12.dp))
            InfoRow(Icons.Rounded.MicOff, "I never record anything", "No audio is saved, ever")
            Spacer(Modifier.height(12.dp))
            InfoRow(Icons.Rounded.PhoneAndroid, "I stay on this phone", "No internet, no account, no cloud")
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = QB.Ink, contentColor = Color.White),
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Let Buddy listen", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        TextButton(
            onClick = { viewModel.finish(onDone) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Maybe later", style = MaterialTheme.typography.bodyMedium, color = QB.InkSoft)
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
