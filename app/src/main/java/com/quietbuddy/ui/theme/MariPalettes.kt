package com.quietbuddy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Mari Domingi's own drawing colours for one style, beyond the shared ones in [Palette] (outline, cheek, tear,
 * mouth): her [hair] and its [hairShade], and her outfit. A laced outfit ([laces]) is a coloured [bodice] over the white
 * blouse with white puffed sleeves; otherwise the dress colour covers the blouse and [sleeve]s too. [hem] is a dark
 * band with white whirls at the bottom of the skirt and [stripe] the apron's stripes (null when the outfit has none),
 * and [shawl] crosses a white shawl over the chest. Values come from the approved prototype,
 * docs/design/mari-domingi-motion.html (its HAIR and OUTFITS tables).
 */
@Immutable
data class MariColors(
    val hair: Color,
    val hairShade: Color,
    val skirt: Color,
    val skirtDark: Color,
    val bodice: Color,
    val sleeve: Color,
    val hem: Color?,
    val stripe: Color?,
    val shawl: Boolean,
    val laces: Boolean,
) {
    companion object {
        private val Linen = Color(0xFFFDFAF3)
        private val Dress = Color(0xFF2F5FA8)

        /** Grey hair and the `gorria` outfit: a burgundy laced bodice and a red skirt. */
        val Amona = MariColors(
            hair = Color(0xFFCFCAC6), hairShade = Color(0xFFA9A29C),
            skirt = Color(0xFFB23A3F), skirtDark = Color(0xFF86262C), bodice = Color(0xFF8C2632), sleeve = Linen,
            hem = null, stripe = null, shawl = false, laces = true,
        )

        /** Purple hair and the `berdea` outfit: a green laced bodice and skirt. */
        val Morea = MariColors(
            hair = Color(0xFF9C72D6), hairShade = Color(0xFF7451AE),
            skirt = Color(0xFF3F8A62), skirtDark = Color(0xFF2B6547), bodice = Color(0xFF245740), sleeve = Linen,
            hem = null, stripe = null, shawl = false, laces = true,
        )

        /** Brown hair and the `urdina` outfit: a blue dress, a crossed white shawl, a dark hem and a striped apron. */
        val Urdina = MariColors(
            hair = Color(0xFF6B4630), hairShade = Color(0xFF4E3222),
            skirt = Dress, skirtDark = Color(0xFF1F3E73), bodice = Dress, sleeve = Dress,
            hem = Color(0xFF1C2D55), stripe = Color(0xFFC8463B), shawl = true, laces = false,
        )
    }
}

/** Amona: grey-haired Mari Domingi in red; warm linen paper with a deep-red chrome. */
val MariPalette = Palette(
    ink = Color(0xFF2E1F1E),
    inkSoft = Color(0xFF6E5755),
    background = Color(0xFFFBF6F2),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFF0E2DC),
    accent = Color(0xFFA8323C),
    accentSoft = Color(0xFFF9E5E3),
    outline = Color(0xFF3B2A20),
    body = Color(0xFFB23A3F),
    cheek = Color(0xFFF29A8C),
    tear = Color(0xFF8FC8FF),
    mouth = Color(0xFF7A2E2A),
    // The home screen's pat hearts use this; the prototype's hearts are this pink.
    tongue = Color(0xFFFF6B8B),
    mari = MariColors.Amona,
)

/** Morea: purple-haired Mari Domingi in green; lavender paper with a violet chrome that matches her hair. */
val MariMoreaPalette = MariPalette.copy(
    ink = Color(0xFF2A2340),
    inkSoft = Color(0xFF665E80),
    background = Color(0xFFF7F4FB),
    line = Color(0xFFE7E0F2),
    accent = Color(0xFF7451AE),
    accentSoft = Color(0xFFEEE7F8),
    body = Color(0xFF3F8A62),
    mari = MariColors.Morea,
)

/** Urdina: brown-haired Mari Domingi in her blue dress; cool paper with a cornflower chrome. */
val MariUrdinaPalette = MariPalette.copy(
    ink = Color(0xFF1B2638),
    inkSoft = Color(0xFF56657A),
    background = Color(0xFFF3F7FC),
    line = Color(0xFFDEE7F2),
    accent = Color(0xFF2F5FA8),
    accentSoft = Color(0xFFE3ECF8),
    body = Color(0xFF2F5FA8),
    mari = MariColors.Urdina,
)
