package com.iptvtv.player.domain.usecase

import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.ChannelRepository
import kotlinx.coroutines.CancellationException

/** Where a sync currently is, so the screens can say more than "wait". */
sealed class SyncStage {
    /** Connecting and reading the playlist. */
    data object Downloading : SyncStage()

    /** Channels already written to the device. */
    data class Saving(val channelsSoFar: Int) : SyncStage()

    val label: String
        get() = when (this) {
            is Downloading -> "Baixando a lista..."
            is Saving -> "Salvando os canais... ($channelsSoFar)"
        }
}

/** Fetches the channel list for [source] and persists it, preserving the user's customizations. */
class SyncSourceUseCase(
    private val importer: PlaylistImporter,
    private val channelRepository: ChannelRepository,
) {
    /**
     * Reports progress through [onStage] and returns how many channels the source now has.
     *
     * The playlist is streamed into the database in batches, so memory stays flat however long
     * the list is, and the count reported back climbs as it goes.
     */
    suspend operator fun invoke(
        source: Source,
        onStage: (SyncStage) -> Unit = {},
    ): Result<Int> {
        onStage(SyncStage.Downloading)

        return try {
            val stamp = channelRepository.beginSync(source.id)
            var saved = 0

            val importResult = importer.fetchChannels(source) { batch ->
                channelRepository.writeSyncBatch(source.id, stamp, batch)
                saved += batch.size
                onStage(SyncStage.Saving(saved))
            }

            importResult.fold(
                onSuccess = { imported ->
                    Result.success(channelRepository.finishSync(source.id, stamp, imported))
                },
                onFailure = { error -> Result.failure(error) },
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // Nothing may escape: the callers run this in coroutines with no supervisor, where a
            // throw would take the process down instead of showing a message.
            Result.failure(error)
        }
    }
}
