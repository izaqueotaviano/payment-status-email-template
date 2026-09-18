package com.iptvtv.player.domain.repository

import com.iptvtv.player.domain.model.Channel
import kotlinx.coroutines.flow.Flow

/**
 * One import in flight. Created by [ChannelRepository.beginSync] and handed back to the calls that
 * apply and close it, so nothing about an import lives in state another import could reach.
 */
interface SyncSession {
    val sourceId: Long

    /** How many channels the source held when this import started; 0 for a brand-new source. */
    val previousCount: Int
}

interface ChannelRepository {
    /** All channels for [sourceId] (including hidden ones), ordered by sortOrder ascending. */
    fun observeChannels(sourceId: Long): Flow<List<Channel>>

    suspend fun getChannel(id: Long): Channel?

    /** Ids of the visible channels of [sourceId], in display order. */
    fun observeVisibleChannelIds(sourceId: Long): Flow<List<Long>>

    /** How many channels each source holds, keyed by source id. */
    fun observeChannelCounts(): Flow<Map<Long, Int>>

    /** How many channels [sourceId] currently holds. */
    suspend fun countForSource(sourceId: Long): Int

    /**
     * Opens an import for [sourceId], or returns null when one is already running for it.
     *
     * An import is applied in batches rather than all at once: a provider playlist can carry
     * hundreds of thousands of channels, and holding one - let alone the several copies a
     * whole-list merge needs - is what exhausts the heap. The returned [SyncSession] carries that
     * import's own stamp and ordering state, so two imports of one source can never write over
     * each other's rows.
     */
    suspend fun beginSync(sourceId: Long): SyncSession?

    /**
     * Merges one batch of freshly imported channels into [session]'s import.
     *
     * Matches them against the existing rows by [Channel.streamKey]: for a match,
     * originalName/originalGroup/logoUrl/streamUrl are refreshed but isFavorite, isHidden,
     * displayName, displayGroup and sortOrder are preserved. Channels with no match are
     * appended. Everything written is stamped with the session's stamp.
     */
    suspend fun writeSyncBatch(session: SyncSession, batch: List<Channel>)

    /**
     * Closes [session]: deletes the rows no batch stamped, i.e. the channels the provider dropped,
     * and returns how many the source now has. Releases the session either way.
     *
     * Throws when [importedCount] is zero rather than deleting everything, since a provider
     * answering with an error page parses to no channels.
     */
    suspend fun finishSync(session: SyncSession, importedCount: Int): Int

    /**
     * Abandons [session] without deleting anything, releasing the source for a later import.
     * Whatever batches already landed stay: a half-imported list is still better than none, and
     * the next import stamps them again. Calling it twice is harmless.
     */
    suspend fun cancelSync(session: SyncSession)

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
