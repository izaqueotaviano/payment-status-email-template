package com.iptvtv.player.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeActiveSourceId(): Flow<Long?>
    suspend fun setActiveSourceId(id: Long?)

    /** The channel that, when set, makes the app skip the channel list and start playing directly. */
    fun observeDefaultChannelId(): Flow<Long?>
    suspend fun setDefaultChannelId(id: Long?)
}
