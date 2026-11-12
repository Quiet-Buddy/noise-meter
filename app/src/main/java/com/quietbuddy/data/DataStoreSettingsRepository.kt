package com.quietbuddy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.Preset
import com.quietbuddy.core.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_START_MINUTES = intPreferencesKey("quiet_start_minutes")
    val QUIET_END_MINUTES = intPreferencesKey("quiet_end_minutes")
    val CHIME_ON_SAD = booleanPreferencesKey("chime_on_sad")
    val KEEP_LISTENING = booleanPreferencesKey("keep_listening")
    val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    val CALIBRATION_OFFSET_DB = floatPreferencesKey("calibration_offset_db")
    /** Absent means null. */
    val CALIBRATED_AT_MS = longPreferencesKey("calibrated_at_ms")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val store: DataStore<Preferences> = context.settingsDataStore

    override val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toAppSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            prefs.writeAll(transform(prefs.toAppSettings()))
        }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val d = AppSettings()
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
            quietHoursEnabled = this[Keys.QUIET_HOURS_ENABLED] ?: d.quietHoursEnabled,
            quietStartMinutes = this[Keys.QUIET_START_MINUTES] ?: d.quietStartMinutes,
            quietEndMinutes = this[Keys.QUIET_END_MINUTES] ?: d.quietEndMinutes,
            chimeOnSad = this[Keys.CHIME_ON_SAD] ?: d.chimeOnSad,
            keepListening = this[Keys.KEEP_LISTENING] ?: d.keepListening,
            keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: d.keepScreenOn,
            calibrationOffsetDb = this[Keys.CALIBRATION_OFFSET_DB] ?: d.calibrationOffsetDb,
            calibratedAtMs = this[Keys.CALIBRATED_AT_MS],
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: d.onboardingDone,
        )
    }

    private fun MutablePreferences.writeAll(s: AppSettings) {
        this[Keys.ROOM_NAME] = s.roomName
        this[Keys.PRESET] = s.preset?.name ?: ""
        this[Keys.WORRIED_FROM_DB] = s.worriedFromDb
        this[Keys.SAD_FROM_DB] = s.sadFromDb
        this[Keys.QUIET_HOURS_ENABLED] = s.quietHoursEnabled
        this[Keys.QUIET_START_MINUTES] = s.quietStartMinutes
        this[Keys.QUIET_END_MINUTES] = s.quietEndMinutes
        this[Keys.CHIME_ON_SAD] = s.chimeOnSad
        this[Keys.KEEP_LISTENING] = s.keepListening
        this[Keys.KEEP_SCREEN_ON] = s.keepScreenOn
        this[Keys.CALIBRATION_OFFSET_DB] = s.calibrationOffsetDb
        val calibrated = s.calibratedAtMs
        if (calibrated != null) this[Keys.CALIBRATED_AT_MS] = calibrated
        else remove(Keys.CALIBRATED_AT_MS)
        this[Keys.ONBOARDING_DONE] = s.onboardingDone
    }
}
