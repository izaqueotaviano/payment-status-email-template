package com.iptvtv.player.domain.repository

import com.iptvtv.player.domain.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    /** All channels for [sourceId] (including hidden ones), ordered by sortOrder ascending. */
    fun observeChannels(sourceId: Long): Flow<List<Channel>>

    suspend fun getChannel(id: Long): Channel?

    /** Ids of the visible channels of [sourceId], in display order. */
    fun observeVisibleChannelIds(sourceId: Long): Flow<List<Long>>

    /**
     * Persists the result of a (re-)import for [sourceId].
     *
     * Matches [freshChannels] against the existing rows by [Channel.streamKey]: for a match,
     * originalName/originalGroup/logoUrl/streamUrl are refreshed but isFavorite, isHidden,
     * displayName, displayGroup and sortOrder are preserved from the existing row. Channels
     * with no match are appended (sortOrder continues after the current max). Existing rows
     * whose streamKey is absent from [freshChannels] are deleted.
     *
     * Throws if [freshChannels] is empty rather than deleting everything, and applies the whole
     * merge in a single transaction.
     */
    suspend fun replaceChannelsForSource(sourceId: Long, freshChannels: List<Channel>)

    suspend fun setHidden(channelId: Long, hidden: Boolean)
    suspend fun setFavorite(channelId: Long, favorite: Boolean)

    /** Hides every non-favorite channel of [sourceId] - the way to trim a huge playlist. */
    suspend fun hideNonFavorites(sourceId: Long)

    /** Un-hides every channel of [sourceId]. */
    suspend fun showAllChannels(sourceId: Long)

    /** How many channels of [sourceId] are currently hidden. */
    fun observeHiddenCount(sourceId: Long): Flow<Int>
    suspend fun rename(channelId: Long, newName: String)
    suspend fun regroup(channelId: Long, newGroup: String)

    /**
     * Exchanges the sortOrder of two channels - the whole of what moving a channel up or down
     * needs, instead of renumbering every row of the source.
     */
    suspend fun swapOrder(firstChannelId: Long, secondChannelId: Long)

    suspend fun deleteChannelsForSource(sourceId: Long)
}
