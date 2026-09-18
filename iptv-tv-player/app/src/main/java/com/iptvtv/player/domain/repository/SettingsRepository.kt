package com.iptvtv.player.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeActiveSourceId(): Flow<Long?>
    suspend fun setActiveSourceId(id: Long?)

    /** The channel that, when set, makes the app skip the channel list and start playing directly. */
    fun observeDefaultChannelId(): Flow<Long?>
    suspend fun setDefaultChannelId(id: Long?)

    /**
     * Whether imports keep only live channels, dropping the films and series episodes that an
     * Xtream "m3u_plus" export carries in the same playlist. Defaults to true: this is a live TV
     * player, and the VOD catalogue is usually the overwhelming majority of the entries.
     */
    fun observeLiveOnlyImport(): Flow<Boolean>
    suspend fun setLiveOnlyImport(liveOnly: Boolean)
}
