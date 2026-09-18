package com.iptvtv.player.domain.usecase

import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.ChannelRepository
import kotlinx.coroutines.CancellationException

/**
 * Real progress of an import.
 *
 * [bytesRead] and [channels] are counted as they happen; [totalBytes] is what the server declared
 * and is 0 when it declared nothing, in which case there is no percentage to show honestly.
 */
data class SyncProgress(
    val bytesRead: Long = 0,
    val totalBytes: Long = 0,
    val channels: Int = 0,
    val finishing: Boolean = false,
) {
    /** 0f..1f when the total is known, null otherwise. */
    val fraction: Float?
        get() = if (totalBytes > 0) (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f) else null

    val label: String
        get() = when {
            finishing -> "Concluindo..."
            channels == 0 -> "Baixando a lista..."
            else -> "$channels canais"
        }
}

/** Fetches the channel list for [source] and persists it, preserving the user's customizations. */
class SyncSourceUseCase(
    private val importer: PlaylistImporter,
    private val channelRepository: ChannelRepository,
) {
    /**
     * Reports progress through [onProgress] and returns how many channels the source now has.
     *
     * The playlist is streamed into the database in batches, so memory stays flat however long
     * the list is, and the progress reported back is measured, not guessed.
     */
    suspend operator fun invoke(
        source: Source,
        onProgress: (SyncProgress) -> Unit = {},
    ): Result<Int> {
        var progress = SyncProgress()
        onProgress(progress)

        return try {
            val stamp = channelRepository.beginSync(source.id)

            val importResult = importer.fetchChannels(
                source = source,
                onBytes = { bytesRead, totalBytes ->
                    progress = progress.copy(bytesRead = bytesRead, totalBytes = totalBytes)
                    onProgress(progress)
                },
                onBatch = { batch ->
                    channelRepository.writeSyncBatch(source.id, stamp, batch)
                    progress = progress.copy(channels = progress.channels + batch.size)
                    onProgress(progress)
                },
            )

            importResult.fold(
                onSuccess = { imported ->
                    onProgress(progress.copy(finishing = true))
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
