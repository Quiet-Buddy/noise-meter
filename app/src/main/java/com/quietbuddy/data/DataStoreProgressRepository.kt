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
import com.quietbuddy.core.LedgerState
import com.quietbuddy.core.Progress
import com.quietbuddy.core.ProgressRepository
import com.quietbuddy.core.Reward
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
    /** Reward keys (styles and accessories); unknown keys are dropped on read. */
    val UNLOCKED = stringSetPreferencesKey("unlocked")
    val CELEBRATED = stringSetPreferencesKey("celebrated")
    val BACKFILLED = booleanPreferencesKey("backfilled")
    val DEBT_MS = longPreferencesKey("ledger_debt_ms")
    val CALM_MS = longPreferencesKey("ledger_calm_ms")
    /** Episode start times as decimal strings (a set, which is fine: they are distinct timestamps). */
    val EPISODES = stringSetPreferencesKey("ledger_episodes")
    val LEDGER_AT = longPreferencesKey("ledger_updated_at_ms")
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

    override suspend fun ledger(): LedgerState {
        val prefs = store.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()
        return LedgerState(
            debtMs = prefs[ProgressKeys.DEBT_MS] ?: 0L,
            calmMs = prefs[ProgressKeys.CALM_MS] ?: 0L,
            episodeStartsMs = prefs[ProgressKeys.EPISODES].orEmpty().mapNotNull { it.toLongOrNull() }.sorted(),
            updatedAtMs = prefs[ProgressKeys.LEDGER_AT] ?: 0L,
        )
    }

    override suspend fun commitLedger(earnedMinutes: Int, state: LedgerState) {
        store.edit { prefs ->
            if (earnedMinutes > 0) prefs.credit(earnedMinutes.toLong())
            if (state.updatedAtMs < (prefs[ProgressKeys.LEDGER_AT] ?: 0L)) return@edit
            prefs[ProgressKeys.DEBT_MS] = state.debtMs
            prefs[ProgressKeys.CALM_MS] = state.calmMs
            prefs[ProgressKeys.EPISODES] = state.episodeStartsMs.map { it.toString() }.toSet()
            prefs[ProgressKeys.LEDGER_AT] = state.updatedAtMs
        }
    }

    override suspend fun backfillIfNeeded() {
        val worriedFromDb = settingsRepository.settings.first().worriedFromDb
        val calm = dao.countCalmMinutes(worriedFromDb, Unlocks.MIN_LISTENING_SECONDS)
        val seed = calm.toLong()
        store.edit { prefs ->
            if (prefs[ProgressKeys.BACKFILLED] == true) return@edit
            // Not marked celebrated: existing users should get the celebration.
            prefs.credit(seed)
            prefs[ProgressKeys.BACKFILLED] = true
        }
    }

    override suspend fun markCelebrated(rewards: Collection<Reward>) {
        if (rewards.isEmpty()) return
        store.edit { prefs ->
            prefs[ProgressKeys.CELEBRATED] = prefs.rewardSet(ProgressKeys.CELEBRATED).map { it.key }.toSet() +
                rewards.map { it.key }
        }
    }

    /** Adds [minutes] to the total and unions in whatever the new total earns. */
    private fun androidx.datastore.preferences.core.MutablePreferences.credit(minutes: Long) {
        val total = (this[ProgressKeys.CALM_MINUTES] ?: 0L) + minutes
        this[ProgressKeys.CALM_MINUTES] = total
        this[ProgressKeys.UNLOCKED] = (rewardSet(ProgressKeys.UNLOCKED) + Unlocks.earnedBy(total)).map { it.key }.toSet()
    }

    private fun Preferences.rewardSet(key: Preferences.Key<Set<String>>): Set<Reward> =
        (this[key].orEmpty().mapNotNull { Reward.fromKey(it) } + Progress.FREE).toSet()

    private fun Preferences.toProgress() = Progress(
        calmMinutes = this[ProgressKeys.CALM_MINUTES] ?: 0L,
        unlocked = rewardSet(ProgressKeys.UNLOCKED),
        celebrated = rewardSet(ProgressKeys.CELEBRATED),
    )
}
