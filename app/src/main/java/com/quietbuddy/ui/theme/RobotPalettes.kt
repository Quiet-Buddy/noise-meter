package com.quietbuddy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Astro's own drawing colours, beyond the shared ones in [Palette] (outline, cheek, tear). [shell] and [shade]
 * colour the orb, [dark] the nozzle and screws, [accent] the ear discs, [screen] the visor and [glow] the eyes, mouth
 * and thruster flame on it. Values come from the approved prototype, docs/design/robot-motion.html.
 */
@Immutable
data class RobotColors(
    val shell: Color,
    val shade: Color,
    val dark: Color,
    val accent: Color,
    val screen: Color,
    val glow: Color,
) {
    companion object {
        val Pearl = RobotColors(
            shell = Color(0xFFF2F5F9), shade = Color(0xFFC9D4E1), dark = Color(0xFF8FA3B8),
            accent = Color(0xFF7FB8E8), screen = Color(0xFF1E2B44), glow = Color(0xFF7FE9FF),
        )
        val Lilac = RobotColors(
            shell = Color(0xFFE0D8F6), shade = Color(0xFFB8ABE2), dark = Color(0xFF8C7FC0),
            accent = Color(0xFFF59BC8), screen = Color(0xFF251F3D), glow = Color(0xFFFFC7F0),
        )
        val Sky = RobotColors(
            shell = Color(0xFFC4E6FF), shade = Color(0xFF92C3EE), dark = Color(0xFF5E92C2),
            accent = Color(0xFFFFB36B), screen = Color(0xFF1B2A40), glow = Color(0xFFFFE27A),
        )
    }
}

/** Pearl Astro: a white-and-silver robot with a cyan screen; cool paper with steel-blue chrome. */
val RobotPalette = Palette(
    ink = Color(0xFF1B2533),
    inkSoft = Color(0xFF586577),
    background = Color(0xFFF3F6FA),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFE1E8F0),
    accent = Color(0xFF3B6FB6),
    accentSoft = Color(0xFFE4EDF8),
    outline = Color(0xFF2A3442),
    body = Color(0xFFF2F5F9),
    cheek = Color(0xFFFF9AA8),
    tear = Color(0xFF8FC8FF),
    mouth = Color(0xFF5B2A3A),
    tongue = Color(0xFFFF7A8A),
    robot = RobotColors.Pearl,
)

/** Lilac Astro: a lavender robot with pink ear discs and a soft violet chrome. */
val RobotLilacPalette = RobotPalette.copy(
    ink = Color(0xFF2A2340),
    inkSoft = Color(0xFF665E80),
    background = Color(0xFFF7F4FC),
    line = Color(0xFFE8E2F4),
    accent = Color(0xFF7A5FC0),
    accentSoft = Color(0xFFEEE8FA),
    body = Color(0xFFE0D8F6),
    robot = RobotColors.Lilac,
)

/** Sky Astro: a light-blue robot with amber ear discs and a clear-sky chrome. */
val RobotSkyPalette = RobotPalette.copy(
    ink = Color(0xFF17293D),
    inkSoft = Color(0xFF51667D),
    background = Color(0xFFF2F8FD),
    line = Color(0xFFDDEAF5),
    accent = Color(0xFF2A74B8),
    accentSoft = Color(0xFFE0EFFB),
    body = Color(0xFFC4E6FF),
    robot = RobotColors.Sky,
)
