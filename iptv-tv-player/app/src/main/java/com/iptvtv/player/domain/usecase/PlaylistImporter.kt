package com.iptvtv.player.domain.usecase

import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.model.Source

/**
 * Fetches and parses the channel list for a [Source] (M3U by URL, Xtream Codes API, or a
 * local file previously picked through Storage Access Framework).
 *
 * Channels arrive through [onBatch] as they are parsed, and the result is how many there were
 * in total. Nothing accumulates the whole playlist: provider lists reach hundreds of thousands
 * of channels, which does not fit in a TV's heap several times over.
 *
 * Implementations produce channels with id=0, sortOrder=position in the playlist,
 * isFavorite=false, isHidden=false, displayName=originalName and displayGroup=originalGroup;
 * the repository's sync merges that against any existing customization.
 */
interface PlaylistImporter {
    suspend fun fetchChannels(
        source: Source,
        onBatch: suspend (List<Channel>) -> Unit,
    ): Result<Int>
}
