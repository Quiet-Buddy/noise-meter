package com.quietbuddy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quietbuddy.core.CharacterStyle
import com.quietbuddy.core.Progress
import com.quietbuddy.core.ProgressRepository
import com.quietbuddy.core.SettingsRepository
import com.quietbuddy.core.Unlocks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.progressDataStore: DataStore<Preferences> by preferencesDataStore(name = "progress")

private object ProgressKeys {
    val CALM_MINUTES = longPreferencesKey("calm_minutes")
    /** CharacterStyle.keys; unknown keys are dropped on read. */
    val UNLOCKED = stringSetPreferencesKey("unlocked")
    val CELEBRATED = stringSetPreferencesKey("celebrated")
    val BACKFILLED = booleanPreferencesKey("backfilled")
}

@Singleton
class DataStoreProgressRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val dao: HistoryDao,
    private val settingsRepository: SettingsRepository,
) : ProgressRepository {

    private val store: DataStore<Preferences> = context.progressDataStore

    override val progress: Flow<Progress> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toProgress() }

    override suspend fun addCalmMinutes(minutes: Int) {
        if (minutes <= 0) return
        store.edit { prefs -> prefs.credit(minutes.toLong()) }
    }

    override suspend fun backfillIfNeeded() {
        val worriedFromDb = settingsRepository.settings.first().worriedFromDb
        val calm = dao.countCalmMinutes(worriedFromDb, Unlocks.MIN_LISTENING_SECONDS)
        val won = dao.countWonChallenges()
        val seed = calm.toLong() + Unlocks.CHALLENGE_BONUS_MINUTES.toLong() * won
        store.edit { prefs ->
            if (prefs[ProgressKeys.BACKFILLED] == true) return@edit
            // Not marked celebrated: existing users should get the celebration.
            prefs.credit(seed)
            prefs[ProgressKeys.BACKFILLED] = true
        }
    }

    override suspend fun markCelebrated(styles: Collection<CharacterStyle>) {
        if (styles.isEmpty()) return
        store.edit { prefs ->
            prefs[ProgressKeys.CELEBRATED] = prefs.styleSet(ProgressKeys.CELEBRATED).map { it.key }.toSet() +
                styles.map { it.key }
        }
    }

    /** Adds [minutes] to the total and unions in whatever the new total earns. */
    private fun androidx.datastore.preferences.core.MutablePreferences.credit(minutes: Long) {
        val total = (this[ProgressKeys.CALM_MINUTES] ?: 0L) + minutes
        this[ProgressKeys.CALM_MINUTES] = total
        this[ProgressKeys.UNLOCKED] = (styleSet(ProgressKeys.UNLOCKED) + Unlocks.earnedBy(total)).map { it.key }.toSet()
    }

    private fun Preferences.styleSet(key: Preferences.Key<Set<String>>): Set<CharacterStyle> =
        (this[key].orEmpty().mapNotNull { CharacterStyle.fromKey(it) } + CharacterStyle.entries.filter { it.isFree }).toSet()

    private fun Preferences.toProgress() = Progress(
        calmMinutes = this[ProgressKeys.CALM_MINUTES] ?: 0L,
        unlocked = styleSet(ProgressKeys.UNLOCKED),
        celebrated = styleSet(ProgressKeys.CELEBRATED),
    )
}
