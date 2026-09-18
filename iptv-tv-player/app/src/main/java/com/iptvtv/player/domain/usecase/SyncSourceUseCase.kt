package com.iptvtv.player.domain.usecase

import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.ChannelRepository

/** Fetches the channel list for [source] and persists it, preserving the user's customizations. */
class SyncSourceUseCase(
    private val importer: PlaylistImporter,
    private val channelRepository: ChannelRepository,
) {
    suspend operator fun invoke(source: Source): Result<Unit> =
        importer.fetchChannels(source).map { channels ->
            channelRepository.replaceChannelsForSource(source.id, channels)
        }
}
