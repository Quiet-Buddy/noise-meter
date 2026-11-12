package com.quietbuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.quietbuddy.core.Mood

/**
 * The colours a character brings: the UI chrome and the character's own drawing colours.
 * Read the current one with [LocalPalette].
 */
@Immutable
data class Palette(
    val ink: Color,
    val inkSoft: Color,
    val background: Color,
    val card: Color,
    val line: Color,
    val accent: Color,
    val accentSoft: Color,
    val outline: Color,
    val body: Color,
    val cheek: Color,
    val tear: Color,
    val mouth: Color,
    val tongue: Color,
) {
    /** Screen background for [m]: the alarm tint for the loud moods, the palette's own otherwise. */
    fun screenBackground(m: Mood?) = when (m) { Mood.SAD, Mood.EARS_COVERED -> QB.TooLoudBackground; else -> background }
}

/** Colours from the mockups. Use these, not hard-coded hex values. */
val BuddyPalette = Palette(
    ink = Color(0xFF1F2A37),
    inkSoft = Color(0xFF5B6B7A),
    background = Color(0xFFF4F8FB),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFE3EBF2),
    accent = Color(0xFF2E6FBF),
    accentSoft = Color(0xFFE7F0FA),
    outline = Color(0xFF2B2B2B),
    body = Color(0xFFFFC94D),
    cheek = Color(0xFFFF9AA2),
    tear = Color(0xFF8FC8FF),
    mouth = Color(0xFF5B2A2A),
    tongue = Color(0xFFFF7A85),
)

/** Kitten: warm paper and ginger. */
val KittenPalette = Palette(
    ink = Color(0xFF2E211B),
    inkSoft = Color(0xFF6E5A4E),
    background = Color(0xFFFBF6F1),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFEFE3D8),
    accent = Color(0xFFB85A22),
    accentSoft = Color(0xFFFBEADC),
    outline = Color(0xFF4A2E22),
    body = Color(0xFFF7B267),
    cheek = Color(0xFFFF9AA8),
    tear = Color(0xFF8FC8FF),
    mouth = Color(0xFF7A3240),
    tongue = Color(0xFFFF7A8A),
)

val LocalPalette = staticCompositionLocalOf { BuddyPalette }

/** Band colours. Fixed for every character so the colours always mean the same thing. */
object QB {

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
}

private fun typography(p: Palette) = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = p.ink),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = p.ink),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = p.ink),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = p.ink),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = p.ink),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.inkSoft),
)

@Composable
fun QuietBuddyTheme(palette: Palette, content: @Composable () -> Unit) {
    val typography = remember(palette) { typography(palette) }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = lightColorScheme(primary = palette.accent, background = palette.background, surface = palette.card, onSurface = palette.ink),
            typography = typography,
            content = content,
        )
    }
}
