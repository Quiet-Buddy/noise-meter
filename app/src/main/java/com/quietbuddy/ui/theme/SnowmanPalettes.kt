package com.quietbuddy.ui.theme

import androidx.compose.ui.graphics.Color

/** Frosty Snowy: a white snowman on cool paper with an icy-blue chrome. */
val SnowmanPalette = Palette(
    ink = Color(0xFF1C2B3A),
    inkSoft = Color(0xFF58697B),
    background = Color(0xFFF1F8FD),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFDDEAF4),
    accent = Color(0xFF3A8FD0),
    accentSoft = Color(0xFFE0F0FB),
    outline = Color(0xFF2B3A4C),
    body = Color(0xFFFAFDFF),
    cheek = Color(0xFFFFA3AE),
    tear = Color(0xFF8FC8FF),
    mouth = Color(0xFF5B2A3A),
    tongue = Color(0xFFFF7A8A),
)

/** Dusk Snowy: a lavender snowman in the evening light, with a violet chrome. */
val SnowmanDuskPalette = SnowmanPalette.copy(
    ink = Color(0xFF2A2340),
    inkSoft = Color(0xFF665E80),
    background = Color(0xFFF6F3FC),
    line = Color(0xFFE6E0F3),
    accent = Color(0xFF7A5FC0),
    accentSoft = Color(0xFFECE6F9),
    outline = Color(0xFF332B4D),
    body = Color(0xFFE6DDF7),
)
