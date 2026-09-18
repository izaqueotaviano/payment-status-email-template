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
     * Opens an import for [sourceId] and returns its stamp, to be handed to [writeSyncBatch] and
     * [finishSync].
     *
     * An import is applied in batches rather than all at once: a provider playlist can carry
     * hundreds of thousands of channels, and holding one - let alone the several copies a
     * whole-list merge needs - is what exhausts the heap.
     */
    suspend fun beginSync(sourceId: Long): Long

    /**
     * Merges one batch of freshly imported channels.
     *
     * Matches them against the existing rows by [Channel.streamKey]: for a match,
     * originalName/originalGroup/logoUrl/streamUrl are refreshed but isFavorite, isHidden,
     * displayName, displayGroup and sortOrder are preserved. Channels with no match are
     * appended. Everything written is stamped with [stamp].
     */
    suspend fun writeSyncBatch(sourceId: Long, stamp: Long, batch: List<Channel>)

    /**
     * Closes the import: deletes the rows no batch stamped, i.e. the channels the provider
     * dropped, and returns how many the source now has.
     *
     * Throws when [importedCount] is zero rather than deleting everything, since a provider
     * answering with an error page parses to no channels.
     */
    suspend fun finishSync(sourceId: Long, stamp: Long, importedCount: Int): Int

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
