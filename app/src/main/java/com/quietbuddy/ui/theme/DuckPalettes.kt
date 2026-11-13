package com.quietbuddy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The duck's own drawing colours, beyond the shared ones in [Palette] (outline, body, cheek, ...).
 * The optional markings are null for a plain duck. Values come from the approved prototype,
 * docs/design/duck-motion.html.
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
    /** How strongly the cheeks blush, 0..1. */
    val cheekStrength: Float = 1f,
    /** A head (and neck) coloured differently from the body, with its own shading and shine. */
    val head: Color? = null,
    val headShade: Color? = null,
    val headShine: Color? = null,
    /** Mallard hen markings: a darker crown, a stripe through the eye, brown chevrons on the body, a dark patch
     *  on the bill and the blue wing feather (speculum). */
    val crown: Color? = null,
    val eyeStripe: Color? = null,
    val mottle: Color? = null,
    val billSaddle: Color? = null,
    val speculum: Color? = null,
    /** A tail coloured differently from the body, and the feather lines on it. */
    val tail: Color? = null,
    val tailLine: Color? = null,
) {
    companion object {
        val Lemon = DuckColors(wing = Color(0xFFF9C21F), shade = Color(0xFFE2A400), belly = Color(0xFFFFF1A6))

        val Cloud = DuckColors(wing = Color(0xFFECE5D3), shade = Color(0xFFCDC3AA), belly = Color(0xFFFFFFFF))

        /** A mallard hen: mottled brown, darker crown, dark eye stripe, orange bill with a dark patch, blue wing feather. */
        val Hazel = DuckColors(
            wing = Color(0xFFA9845A), shade = Color(0xFF8C6A45), belly = Color(0xFFE6CDA8),
            bill = Color(0xFFEE8F30), billLow = Color(0xFFCF7621), billTip = Color(0xFF3D2B10), feet = Color(0xFFF28C28),
            cheekStrength = 0.7f,
            head = Color(0xFFC29D70), headShade = Color(0xFF9C774C), headShine = Color(0xFFEBD6B4),
            crown = Color(0xFF7A5A38), eyeStripe = Color(0xFF5A4128), mottle = Color(0xFF8A6540),
            billSaddle = Color(0xFF4A3A2A), speculum = Color(0xFF3F6FD8),
            tail = Color(0xFFB89468), tailLine = Color(0xFF8A6540),
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

/** Hazel Pip: a mallard hen with warm brown chrome. */
val DuckHazelPalette = DuckPalette.copy(
    ink = Color(0xFF2C2117),
    inkSoft = Color(0xFF6B5A48),
    background = Color(0xFFFAF6F0),
    line = Color(0xFFEDE3D6),
    accent = Color(0xFF8A5A2B),
    accentSoft = Color(0xFFF5E9DC),
    body = Color(0xFFC9A57A),
    duck = DuckColors.Hazel,
)
