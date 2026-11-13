package com.quietbuddy.core

/**
 * The characters the app can be themed with. A character supplies its look (art + palette, ui.character),
 * its voice (domain.character.Voices) and its alert sounds (audio.AlertSounds).
 *
 * Every layer maps a character with an exhaustive `when`, so adding an entry here fails the build
 * until each layer handles it.
 *
 * [key] is persisted in settings; never change an existing key.
 */
enum class Character(val key: String) {
    BUDDY("buddy"),
    KITTEN("kitten"),
    DUCK("duck");

    companion object {
        val DEFAULT = BUDDY

        /** Unknown or missing keys fall back to [DEFAULT], e.g. after a character is removed. */
        fun fromKey(key: String?): Character = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
