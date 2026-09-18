package com.iptvtv.player.domain.repository

import com.iptvtv.player.domain.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    /** All channels for [sourceId] (including hidden ones), ordered by sortOrder ascending. */
    fun observeChannels(sourceId: Long): Flow<List<Channel>>

    suspend fun getChannel(id: Long): Channel?

    /**
     * Persists the result of a (re-)import for [sourceId].
     *
     * Matches [freshChannels] against the existing rows by [Channel.streamKey]: for a match,
     * originalName/originalGroup/logoUrl/streamUrl are refreshed but isFavorite, isHidden,
     * displayName, displayGroup and sortOrder are preserved from the existing row. Channels
     * with no match are appended (sortOrder continues after the current max). Existing rows
     * whose streamKey is absent from [freshChannels] are deleted.
     */
    suspend fun replaceChannelsForSource(sourceId: Long, freshChannels: List<Channel>)

    suspend fun setHidden(channelId: Long, hidden: Boolean)
    suspend fun setFavorite(channelId: Long, favorite: Boolean)
    suspend fun rename(channelId: Long, newName: String)
    suspend fun regroup(channelId: Long, newGroup: String)

    /** Sets sortOrder = index for each id in [orderedChannelIds] (must all belong to the same source). */
    suspend fun reorder(orderedChannelIds: List<Long>)

    suspend fun deleteChannelsForSource(sourceId: Long)
}
