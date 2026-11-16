package com.quietbuddy.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quietbuddy.core.AppSettings
import com.quietbuddy.core.Character
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStorageTest {
    private val chime = booleanPreferencesKey("chime_on_sad")
    private val room = stringPreferencesKey("room_name")

    @Test
    fun migrationDropsTheStoredSoundAlertChoiceOnce() = runTest {
        // What 1.0.0 left behind: onboarding saved every field, including the old "off" default.
        val v1 = preferencesOf(chime to false, room to "Class 3")
        assertTrue(SettingsV2Migration.shouldMigrate(v1))

        val v2 = SettingsV2Migration.migrate(v1)
        assertNull(v2[chime])
        assertEquals("Class 3", v2[room])
        assertEquals(2, v2[SETTINGS_VERSION])
        assertFalse(SettingsV2Migration.shouldMigrate(v2))
    }

    @Test
    fun migrationRunsOnFreshInstallsWithoutLosingAnything() = runTest {
        val fresh = preferencesOf()
        assertTrue(SettingsV2Migration.shouldMigrate(fresh))
        assertEquals(preferencesOf(SETTINGS_VERSION to 2), SettingsV2Migration.migrate(fresh))
    }

    @Test
    fun choiceMadeAfterTheMigrationIsKept() = runTest {
        val migrated = SettingsV2Migration.migrate(preferencesOf(chime to false)).toMutablePreferences()
        val old = AppSettings()
        migrated.writeChanged(old, old.copy(chimeOnSad = false))
        assertEquals(false, migrated[chime])
        assertFalse(SettingsV2Migration.shouldMigrate(migrated))
    }

    @Test
    fun savingOneSettingLeavesUntouchedOnesAbsent() {
        val prefs = mutablePreferencesOf()
        val old = AppSettings()
        prefs.writeChanged(old, old.copy(onboardingDone = true))

        assertEquals(true, prefs[Keys.ONBOARDING_DONE])
        // The bug behind the migration: these used to be written too and so froze at their old defaults.
        assertNull(prefs[chime])
        assertNull(prefs[Keys.KEEP_SCREEN_ON])
        assertNull(prefs[Keys.PRESET])
        assertNull(prefs[Keys.CALIBRATION_OFFSET_DB])
    }

    @Test
    fun changedSettingsAreWrittenAndClearedOnesRemoved() {
        val prefs = mutablePreferencesOf()
        val old = AppSettings(calibratedAtMs = 1_000L)
        prefs[Keys.CALIBRATED_AT_MS] = 1_000L
        prefs.writeChanged(old, old.copy(character = Character.DUCK, preset = null, calibratedAtMs = null))

        assertEquals(Character.DUCK.key, prefs[Keys.CHARACTER])
        assertEquals("", prefs[Keys.PRESET])
        assertNull(prefs[Keys.CALIBRATED_AT_MS])
    }
}
