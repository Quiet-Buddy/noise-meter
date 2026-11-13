package com.quietbuddy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quietbuddy.R
import com.quietbuddy.core.Accessory
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.Character
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.Preset
import com.quietbuddy.core.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private object Keys {
    val ROOM_NAME = stringPreferencesKey("room_name")
    /** Preset name; empty string means null (custom thresholds). Absent means the AppSettings default. */
    val PRESET = stringPreferencesKey("preset")
    val WORRIED_FROM_DB = floatPreferencesKey("worried_from_db")
    val SAD_FROM_DB = floatPreferencesKey("sad_from_db")
    val CHIME_ON_SAD = booleanPreferencesKey("chime_on_sad")
    val KEEP_LISTENING = booleanPreferencesKey("keep_listening")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val CALIBRATION_OFFSET_DB = floatPreferencesKey("calibration_offset_db")
    /** Absent means null. */
    val CALIBRATED_AT_MS = longPreferencesKey("calibrated_at_ms")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    /** Character.key; absent or unknown means Character.DEFAULT. */
    val CHARACTER = stringPreferencesKey("character")
    /** CharacterStyle.key of the selected style for [c]; absent or mismatched means the character's default. */
    fun style(c: Character) = stringPreferencesKey("style_${c.key}")
    /** Accessory.keys worn by [c]; unknown keys and other characters' accessories are dropped. */
    fun accessories(c: Character) = stringSetPreferencesKey("accessories_${c.key}")
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val store: DataStore<Preferences> = context.settingsDataStore

    override val settings: Flow<AppSettings> = store.data
        .onStart { persistDefaultRoomName() }
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toAppSettings() }

    /** The room is created once, named in the language at that moment, and never re-translated. */
    private suspend fun persistDefaultRoomName() {
        runCatching {
            store.edit { prefs ->
                if (prefs[Keys.ROOM_NAME] == null) {
                    prefs[Keys.ROOM_NAME] = context.getString(R.string.default_room_name)
                }
            }
        }
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            prefs.writeAll(transform(prefs.toAppSettings()))
        }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val d = AppSettings(roomName = context.getString(R.string.default_room_name))
        val presetRaw = this[Keys.PRESET]
        val preset = when (presetRaw) {
            null -> d.preset
            "" -> null
            else -> runCatching { Preset.valueOf(presetRaw) }.getOrNull()
        }
        return AppSettings(
            roomName = this[Keys.ROOM_NAME] ?: d.roomName,
            preset = preset,
            worriedFromDb = this[Keys.WORRIED_FROM_DB] ?: d.worriedFromDb,
            sadFromDb = this[Keys.SAD_FROM_DB] ?: d.sadFromDb,
            chimeOnSad = this[Keys.CHIME_ON_SAD] ?: d.chimeOnSad,
            keepListening = this[Keys.KEEP_LISTENING] ?: d.keepListening,
            keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: d.keepScreenOn,
            calibrationOffsetDb = this[Keys.CALIBRATION_OFFSET_DB] ?: d.calibrationOffsetDb,
            calibratedAtMs = this[Keys.CALIBRATED_AT_MS],
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: d.onboardingDone,
            character = Character.fromKey(this[Keys.CHARACTER]),
            styles = Character.entries.mapNotNull { c ->
                CharacterStyle.fromKey(this[Keys.style(c)])?.takeIf { it.character == c }?.let { c to it }
            }.toMap(),
            accessories = Character.entries.mapNotNull { c ->
                this[Keys.accessories(c)]
                    ?.mapNotNull { Accessory.fromKey(it)?.takeIf { a -> a.character == c } }
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { c to it.toSet() }
            }.toMap(),
        )
    }

    private fun MutablePreferences.writeAll(s: AppSettings) {
        this[Keys.ROOM_NAME] = s.roomName
        this[Keys.PRESET] = s.preset?.name ?: ""
        this[Keys.WORRIED_FROM_DB] = s.worriedFromDb
        this[Keys.SAD_FROM_DB] = s.sadFromDb
        this[Keys.CHIME_ON_SAD] = s.chimeOnSad
        this[Keys.KEEP_LISTENING] = s.keepListening
        this[Keys.KEEP_SCREEN_ON] = s.keepScreenOn
        this[Keys.CALIBRATION_OFFSET_DB] = s.calibrationOffsetDb
        val calibrated = s.calibratedAtMs
        if (calibrated != null) this[Keys.CALIBRATED_AT_MS] = calibrated
        else remove(Keys.CALIBRATED_AT_MS)
        this[Keys.ONBOARDING_DONE] = s.onboardingDone
        this[Keys.CHARACTER] = s.character.key
        for (c in Character.entries) {
            val style = s.styles[c]
            if (style != null) this[Keys.style(c)] = style.key
            else remove(Keys.style(c))
            val worn = s.accessories[c].orEmpty()
            if (worn.isNotEmpty()) this[Keys.accessories(c)] = worn.map { it.key }.toSet()
            else remove(Keys.accessories(c))
        }
        remove(booleanPreferencesKey("unlock_all_styles")) // option removed; drop it from old installs
    }
}
