package com.quietbuddy.data

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

/** Version of the stored settings; absent means 1. Bump it and add a migration when stored values need fixing up. */
internal val SETTINGS_VERSION = intPreferencesKey("settings_version")

internal const val CURRENT_SETTINGS_VERSION = 2

/**
 * v1 -> v2: sound alerts became on by default, but builds up to 1.0.0 saved every setting on any change
 * (finishing onboarding was enough), so nearly every install has the old default stored as an explicit "off".
 * Those stored values can't be told apart from a real choice, so the key is dropped once and everyone gets
 * the new default; anyone who wants silence turns it off again, and that choice is kept from now on.
 */
internal object SettingsV2Migration : DataMigration<Preferences> {
    private val CHIME_ON_SAD = booleanPreferencesKey("chime_on_sad")

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        (currentData[SETTINGS_VERSION] ?: 1) < 2

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            remove(CHIME_ON_SAD)
            this[SETTINGS_VERSION] = 2
        }.toPreferences()

    override suspend fun cleanUp() = Unit
}
