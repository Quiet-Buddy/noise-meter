package com.quietbuddy.ui.character

import androidx.annotation.StringRes
import com.quietbuddy.R
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.Reward

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

/** The display name of an accessory, e.g. "Top hat". */
@get:StringRes
val Accessory.nameRes: Int
    get() = when (this) {
        Accessory.SUNGLASSES -> R.string.acc_sunglasses
        Accessory.BOW -> R.string.acc_bow
        Accessory.TIE -> R.string.acc_tie
        Accessory.BELL_COLLAR -> R.string.acc_bell_collar
        Accessory.TOP_HAT -> R.string.acc_top_hat
        Accessory.FLOWER -> R.string.acc_flower
        Accessory.BOOTS -> R.string.acc_boots
        Accessory.CROWN -> R.string.acc_crown
    }

/** The display name of any reward. */
@get:StringRes
val Reward.nameRes: Int
    get() = when (this) {
        is CharacterStyle -> nameRes
        is Accessory -> nameRes
    }
