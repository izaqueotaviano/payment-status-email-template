package com.iptvtv.player.domain.usecase

import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.ChannelRepository
import kotlinx.coroutines.CancellationException

/** Where a sync currently is, so the screens can say more than "wait". */
enum class SyncStage(val label: String) {
    Downloading("Baixando a lista..."),
    Saving("Salvando os canais..."),
}

/** Fetches the channel list for [source] and persists it, preserving the user's customizations. */
class SyncSourceUseCase(
    private val importer: PlaylistImporter,
    private val channelRepository: ChannelRepository,
) {
    /**
     * Reports progress through [onStage] and returns how many channels the source now has.
     *
     * Importing a large playlist takes long enough on a TV's connection that a screen with no
     * feedback looks broken, so callers get told which step is running.
     */
    suspend operator fun invoke(
        source: Source,
        onStage: (SyncStage) -> Unit = {},
    ): Result<Int> {
        onStage(SyncStage.Downloading)
        val channels = importer.fetchChannels(source).getOrElse { error ->
            return Result.failure(error)
        }

        onStage(SyncStage.Saving)
        // Result.map does not catch, so anything the merge throws (a SQLite error, an empty
        // playlist) would escape all the way out of the caller's coroutine and kill the app.
        return try {
            channelRepository.replaceChannelsForSource(source.id, channels)
            Result.success(channels.size)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }
}
