package com.iptvtv.player.domain.usecase

import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.domain.repository.SourceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Real progress of an import: every number here was counted, none is a guess.
 *
 * [bytesRead] is what has actually come down the wire and [channels] what has actually been
 * written. [totalBytes] is what the server declared, and is 0 whenever it declared nothing - which
 * is most of the time, since a playlist is generated on the fly and usually arrives gzipped or
 * chunked. [expectedChannels] is how many channels the source held before this import and is the
 * denominator that is actually available on a refresh. [skippedVod] counts the films and episodes
 * the live-only filter dropped.
 */
data class SyncProgress(
    val bytesRead: Long = 0,
    val totalBytes: Long = 0,
    val channels: Int = 0,
    val skippedVod: Int = 0,
    val expectedChannels: Int = 0,
    val finishing: Boolean = false,
) {
    /**
     * 0f..1f when there is a denominator worth trusting, null when there is not - in which case the
     * bar is honest about it rather than inventing a percentage.
     */
    val fraction: Float?
        get() = when {
            totalBytes > 0 -> (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
            // Nothing measured at all: the Xtream API path, which does not stream. The only
            // denominator left is how many channels the source held last time, and it is a fair
            // one there - that endpoint returns live channels only, so the count is comparable.
            bytesRead == 0L && expectedChannels > 0 ->
                (channels.toFloat() / expectedChannels).coerceIn(0f, 1f)
            else -> null
        }

    val label: String
        get() {
            if (finishing) return "Concluindo..."
            val parts = ArrayList<String>(3)
            if (bytesRead > 0) parts += formatMegabytes(bytesRead)
            if (channels > 0) parts += "$channels canais"
            if (skippedVod > 0) parts += "$skippedVod filmes/séries ignorados"
            return if (parts.isEmpty()) "Baixando a lista..." else parts.joinToString(" · ")
        }
}

private const val BYTES_PER_MB = 1024.0 * 1024.0

private fun formatMegabytes(bytes: Long): String {
    val megabytes = bytes / BYTES_PER_MB
    return if (megabytes < 10) {
        "%.1f MB".format(megabytes)
    } else {
        "${megabytes.toInt()} MB"
    }
}

/** Fetches the channel list for [source] and persists it, preserving the user's customizations. */
class SyncSourceUseCase(
    private val importer: PlaylistImporter,
    private val channelRepository: ChannelRepository,
    private val sourceRepository: SourceRepository,
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Reports progress through [onProgress] and returns how many channels the source now has.
     *
     * The playlist is streamed into the database in batches, so memory stays flat however long
     * the list is, and the progress reported back is measured, not guessed. When an import of the
     * same source is already running this does nothing and reports the current count: a second one
     * would read the same "before" snapshot and delete the first one's rows as stale.
     */
    suspend operator fun invoke(
        source: Source,
        onProgress: (SyncProgress) -> Unit = {},
    ): Result<Int> {
        var progress = SyncProgress()
        onProgress(progress)

        return try {
            val liveOnly = settingsRepository.observeLiveOnlyImport().first()
            val session = channelRepository.beginSync(source.id)
                // Reporting the current count here would show "Pronto! N canais" for an import that
                // never ran, and hide from the user that their edit has not been applied yet.
                ?: return Result.failure(IllegalStateException("Esta lista já está sendo atualizada."))

            // Stamped now, not on success: an import the user walks out of, or one the provider
            // fails, must still count as attempted or every later visit downloads it all again.
            runCatching { sourceRepository.markImportAttempt(source.id, System.currentTimeMillis()) }

            progress = progress.copy(expectedChannels = session.previousCount)
            onProgress(progress)

            try {
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
                        channelRepository.writeSyncBatch(session, batch)
                        progress = progress.copy(channels = progress.channels + batch.size)
                        onProgress(progress)
                    },
                )

                importResult.fold(
                    onSuccess = { imported ->
                        onProgress(progress.copy(finishing = true))
                        Result.success(channelRepository.finishSync(session, imported))
                    },
                    onFailure = { error -> Result.failure(error) },
                )
            } finally {
                // finishSync releases the session itself; this covers every other way out,
                // including a cancelled screen. Releasing twice is harmless.
                channelRepository.cancelSync(session)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // Nothing may escape: the callers run this in coroutines with no supervisor, where a
            // throw would take the process down instead of showing a message.
            Result.failure(error)
        }
    }
}
