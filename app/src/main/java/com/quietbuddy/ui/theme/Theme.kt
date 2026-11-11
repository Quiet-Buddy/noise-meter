package com.quietbuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.quietbuddy.core.Mood

/** Colours from the mockups. Use these, not hard-coded hex values. */
object QB {
    val Ink = Color(0xFF1F2A37)
    val InkSoft = Color(0xFF5B6B7A)
    val Background = Color(0xFFF4F8FB)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFE3EBF2)
    val Accent = Color(0xFF2E6FBF)
    val AccentSoft = Color(0xFFE7F0FA)
    val Outline = Color(0xFF2B2B2B)
    val BuddyYellow = Color(0xFFFFC94D)
    val Cheek = Color(0xFFFF9AA2)
    val Tear = Color(0xFF8FC8FF)
    val Mouth = Color(0xFF5B2A2A)
    val Tongue = Color(0xFFFF7A85)

    val Calm = Color(0xFF2E9F6E)
    val CalmSoft = Color(0xFFDDF3E8)
    val CalmText = Color(0xFF1E6B49)
    val Loud = Color(0xFFE39A1C)
    val LoudSoft = Color(0xFFFDE8C4)
    val LoudText = Color(0xFF7A4F0A)
    val TooLoud = Color(0xFFD9513F)
    val TooLoudSoft = Color(0xFFFAD6CF)
    val TooLoudText = Color(0xFF8A2E21)
    val TooLoudBackground = Color(0xFFFFF1EE)

    fun bandColor(m: Mood) = when (m) { Mood.HAPPY, Mood.FINE -> Calm; Mood.WORRIED -> Loud; else -> TooLoud }
    fun bandSoft(m: Mood) = when (m) { Mood.HAPPY, Mood.FINE -> CalmSoft; Mood.WORRIED -> LoudSoft; else -> TooLoudSoft }
    fun bandText(m: Mood) = when (m) { Mood.HAPPY, Mood.FINE -> CalmText; Mood.WORRIED -> LoudText; else -> TooLoudText }
    fun screenBackground(m: Mood?) = when (m) { Mood.SAD, Mood.EARS_COVERED -> TooLoudBackground; else -> Background }
}

private val Typo = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = QB.Ink),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = QB.Ink),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = QB.Ink),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = QB.Ink),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = QB.Ink),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = QB.InkSoft),
)

@Composable
fun QuietBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = QB.Accent, background = QB.Background, surface = QB.Card, onSurface = QB.Ink),
        typography = Typo,
        content = content,
    )
}
