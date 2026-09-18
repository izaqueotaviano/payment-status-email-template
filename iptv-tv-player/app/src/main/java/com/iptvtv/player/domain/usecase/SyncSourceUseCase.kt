package com.iptvtv.player.domain.usecase

import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.ChannelRepository
import kotlinx.coroutines.CancellationException

/** Fetches the channel list for [source] and persists it, preserving the user's customizations. */
class SyncSourceUseCase(
    private val importer: PlaylistImporter,
    private val channelRepository: ChannelRepository,
) {
    suspend operator fun invoke(source: Source): Result<Unit> {
        val channels = importer.fetchChannels(source).getOrElse { error ->
            return Result.failure(error)
        }
        // Result.map does not catch, so anything the merge throws (a SQLite error, an empty
        // playlist) would escape all the way out of the caller's coroutine and kill the app.
        return try {
            Result.success(channelRepository.replaceChannelsForSource(source.id, channels))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }
}
