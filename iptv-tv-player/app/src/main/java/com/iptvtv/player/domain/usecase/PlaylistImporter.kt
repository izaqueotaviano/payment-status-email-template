package com.iptvtv.player.domain.usecase

import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.model.Source

/**
 * Fetches and parses the channel list for a [Source] (M3U by URL, Xtream Codes API, or a
 * local file previously picked through Storage Access Framework).
 *
 * Implementations return channels with id=0, sortOrder=index in the returned list,
 * isFavorite=false, isHidden=false, displayName=originalName and displayGroup=originalGroup;
 * [ChannelRepository.replaceChannelsForSource][com.iptvtv.player.domain.repository.ChannelRepository.replaceChannelsForSource]
 * is responsible for merging that against any existing customization.
 */
interface PlaylistImporter {
    suspend fun fetchChannels(source: Source): Result<List<Channel>>
}
