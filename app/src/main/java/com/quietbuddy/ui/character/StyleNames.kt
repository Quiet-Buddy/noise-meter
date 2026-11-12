package com.quietbuddy.ui.character

import androidx.annotation.StringRes
import com.quietbuddy.R
import com.quietbuddy.core.CharacterStyle

/** The display name of a style, e.g. "Mint Buddy". */
@get:StringRes
val CharacterStyle.nameRes: Int
    get() = when (this) {
        CharacterStyle.BUDDY_SUNNY -> R.string.style_buddy_sunny
        CharacterStyle.BUDDY_MINT -> R.string.style_buddy_mint
        CharacterStyle.BUDDY_BERRY -> R.string.style_buddy_berry
        CharacterStyle.KITTEN_GINGER -> R.string.style_kitten_ginger
        CharacterStyle.KITTEN_GREY -> R.string.style_kitten_grey
        CharacterStyle.KITTEN_SNOW -> R.string.style_kitten_snow
    }
