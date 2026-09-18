package com.iptvtv.player.domain.usecase

import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Real progress of an import.
 *
 * [bytesRead] and [channels] are counted as they happen; [totalBytes] is what the server declared
 * and is 0 when it declared nothing, in which case there is no percentage to show honestly.
 * [skippedVod] counts the films and episodes the live-only filter dropped.
 */
data class SyncProgress(
    val bytesRead: Long = 0,
    val totalBytes: Long = 0,
    val channels: Int = 0,
    val skippedVod: Int = 0,
    val finishing: Boolean = false,
) {
    /** 0f..1f when the total is known, null otherwise. */
    val fraction: Float?
        get() = if (totalBytes > 0) (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f) else null

    val label: String
        get() = when {
            finishing -> "Concluindo..."
            channels == 0 && skippedVod == 0 -> "Baixando a lista..."
            skippedVod > 0 -> "$channels canais · $skippedVod filmes/séries ignorados"
            else -> "$channels canais"
        }
}

/** Fetches the channel list for [source] and persists it, preserving the user's customizations. */
class SyncSourceUseCase(
    private val importer: PlaylistImporter,
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository,
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
            val liveOnly = settingsRepository.observeLiveOnlyImport().first()
            val stamp = channelRepository.beginSync(source.id)

            val importResult = importer.fetchChannels(
                source = source,
                liveOnly = liveOnly,
                onBytes = { bytesRead, totalBytes ->
                    progress = progress.copy(bytesRead = bytesRead, totalBytes = totalBytes)
                    onProgress(progress)
                },
                onSkipped = { skipped ->
                    progress = progress.copy(skippedVod = skipped)
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
