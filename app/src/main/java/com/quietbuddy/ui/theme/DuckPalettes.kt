package com.quietbuddy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The duck's own drawing colours, beyond the shared ones in [Palette] (outline, body, cheek, ...).
 * The optional markings are null for a plain duck; the mallard sets them all.
 * Values come from the approved prototype, docs/design/duck-motion.html.
 */
@Immutable
data class DuckColors(
    val wing: Color,
    val shade: Color,
    val belly: Color,
    val bill: Color = Color(0xFFFF9A1F),
    val billLow: Color = Color(0xFFF07A12),
    val billTip: Color = Color(0xFFE8780E),
    /** Webbed feet; the bill colour unless set. */
    val feet: Color = bill,
    /** How strongly the cheeks blush, 0..1 (pink reads grey on a green head). */
    val cheekStrength: Float = 1f,
    // Mallard markings
    val head: Color? = null,
    val headShade: Color? = null,
    val headShine: Color? = null,
    val headSheen: Color? = null,
    val neckRing: Color? = null,
    val chest: Color? = null,
    val chestShade: Color? = null,
    val back: Color? = null,
    val vermiculation: Boolean = false,
    val tail: Color? = null,
    val tailLine: Color? = null,
    val rump: Color? = null,
    val curl: Color? = null,
    val speculum: Color? = null,
) {
    companion object {
        val Lemon = DuckColors(wing = Color(0xFFF9C21F), shade = Color(0xFFE2A400), belly = Color(0xFFFFF1A6))

        val Cloud = DuckColors(wing = Color(0xFFECE5D3), shade = Color(0xFFCDC3AA), belly = Color(0xFFFFFFFF))

        /** A mallard drake: green head, white neck ring, chestnut chest, grey flanks, white tail with a black curl. */
        val Mallard = DuckColors(
            wing = Color(0xFFB3A794), shade = Color(0xFF8F877A), belly = Color(0xFFF2F2EE),
            bill = Color(0xFFE8C53A), billLow = Color(0xFFCFA82A), billTip = Color(0xFF3D2B10), feet = Color(0xFFFF8A1F),
            cheekStrength = 0.3f,
            head = Color(0xFF2E8E58), headShade = Color(0xFF1C6640), headShine = Color(0xFF9BE3BA), headSheen = Color(0xFF3B5FC0),
            neckRing = Color(0xFFFFFFFF), chest = Color(0xFF93452A), chestShade = Color(0xFF6E2F1A), back = Color(0xFF9C9384),
            vermiculation = true, tail = Color(0xFFF6F6F2), tailLine = Color(0xFFC9C9C2), rump = Color(0xFF2A2622),
            curl = Color(0xFF2A2622), speculum = Color(0xFF3F6FD8),
        )
    }
}

/** Lemon Pip: a yellow rubber duck on a pond; warm paper with pond-blue chrome. */
val DuckPalette = Palette(
    ink = Color(0xFF2B2414),
    inkSoft = Color(0xFF6B6048),
    background = Color(0xFFFBF9F1),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFEFE9D6),
    accent = Color(0xFF1E7FA8),
    accentSoft = Color(0xFFE2F2F8),
    outline = Color(0xFF3D2B10),
    body = Color(0xFFFFD43B),
    cheek = Color(0xFFFF9AA8),
    tear = Color(0xFF8FC8FF),
    mouth = Color(0xFF7A3240),
    tongue = Color(0xFFFF7A8A),
    duck = DuckColors.Lemon,
)

/** Cloud Pip: a white duck with cool slate-blue chrome. */
val DuckCloudPalette = DuckPalette.copy(
    ink = Color(0xFF1F2933),
    inkSoft = Color(0xFF586573),
    background = Color(0xFFF5F8FA),
    line = Color(0xFFE2E9EF),
    accent = Color(0xFF3D6E8F),
    accentSoft = Color(0xFFE5EEF4),
    body = Color(0xFFFBF8EF),
    duck = DuckColors.Cloud,
)

/** Mallard Pip: a mallard drake with reed-green chrome. */
val DuckMallardPalette = DuckPalette.copy(
    ink = Color(0xFF1C2A22),
    inkSoft = Color(0xFF55675C),
    background = Color(0xFFF3F7F3),
    line = Color(0xFFDFE9E1),
    accent = Color(0xFF2E7D4F),
    accentSoft = Color(0xFFE1F1E6),
    body = Color(0xFFDCDCD5),
    duck = DuckColors.Mallard,
)
