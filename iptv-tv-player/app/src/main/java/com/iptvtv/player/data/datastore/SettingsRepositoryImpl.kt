package com.iptvtv.player.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iptvtv.player.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

private val ACTIVE_SOURCE_ID = longPreferencesKey("active_source_id")
private val DEFAULT_CHANNEL_ID = longPreferencesKey("default_channel_id")
private val LIVE_ONLY_IMPORT = booleanPreferencesKey("live_only_import")

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    override fun observeActiveSourceId(): Flow<Long?> =
        context.dataStore.data.map { it[ACTIVE_SOURCE_ID] }

    override suspend fun setActiveSourceId(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id == null) {
                prefs.remove(ACTIVE_SOURCE_ID)
            } else {
                prefs[ACTIVE_SOURCE_ID] = id
            }
        }
    }

    override fun observeDefaultChannelId(): Flow<Long?> =
        context.dataStore.data.map { it[DEFAULT_CHANNEL_ID] }

    override suspend fun setDefaultChannelId(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id == null) {
                prefs.remove(DEFAULT_CHANNEL_ID)
            } else {
                prefs[DEFAULT_CHANNEL_ID] = id
            }
        }
    }

    override fun observeLiveOnlyImport(): Flow<Boolean> =
        context.dataStore.data.map { it[LIVE_ONLY_IMPORT] ?: true }

    override suspend fun setLiveOnlyImport(liveOnly: Boolean) {
        context.dataStore.edit { prefs -> prefs[LIVE_ONLY_IMPORT] = liveOnly }
    }
}
