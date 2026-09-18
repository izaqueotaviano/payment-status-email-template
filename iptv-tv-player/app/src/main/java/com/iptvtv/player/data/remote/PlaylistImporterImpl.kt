package com.iptvtv.player.data.remote

import android.content.Context
import android.net.Uri
import com.iptvtv.player.data.remote.m3u.M3uParser
import com.iptvtv.player.data.remote.m3u.ParsedM3uEntry
import com.iptvtv.player.data.remote.xtream.XtreamCategory
import com.iptvtv.player.data.remote.xtream.XtreamLiveStream
import com.iptvtv.player.data.remote.xtream.buildXtreamStreamUrl
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.usecase.PlaylistImporter
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.ForwardingSource
import okio.Source as OkioSource
import okio.buffer
import okio.source

private const val DEFAULT_GROUP = "Geral"

/**
 * How many channels are handed over at a time: small enough that memory stays flat, large enough
 * that a big playlist does not pay for hundreds of separate database transactions.
 */
private const val BATCH_SIZE = 2000

/** Report download progress at most every 256 KB. */
private const val REPORT_EVERY_BYTES = 256L * 1024

private val xtreamJson = Json { ignoreUnknownKeys = true }

/**
 * Fetches and parses channels for a saved [Source]: an M3U playlist by URL, a local M3U file
 * picked through Storage Access Framework, or the Xtream Codes live-stream API.
 *
 * Channels are delivered in batches as they are parsed. A provider playlist can carry hundreds
 * of thousands of entries, and building the whole list before returning it is what ran the app
 * out of memory.
 */
class PlaylistImporterImpl(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) : PlaylistImporter {

    override suspend fun fetchChannels(
        source: Source,
        onBytes: (bytesRead: Long, totalBytes: Long) -> Unit,
        onBatch: suspend (List<Channel>) -> Unit,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            when (source) {
                is Source.M3uUrlSource -> importFromM3uUrl(source, onBytes, onBatch)
                is Source.LocalFileSource -> importFromLocalFile(source, onBytes, onBatch)
                is Source.XtreamSource -> importFromXtream(source, onBatch)
            }
        }
    }

    private suspend fun importFromM3uUrl(
        source: Source.M3uUrlSource,
        onBytes: (Long, Long) -> Unit,
        onBatch: suspend (List<Channel>) -> Unit,
    ): Int {
        val request = Request.Builder().url(source.url).build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Falha ao baixar a lista: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("A fonte respondeu sem conteúdo.")
            val totalBytes = body.contentLength().coerceAtLeast(0L)
            val charset = body.contentType()?.charset() ?: Charsets.UTF_8
            // Stream the response instead of body.string(): some IPTV providers serve playlists
            // tens of MB long, and buffering the whole thing into one String would exhaust the
            // heap. Counting as it goes is also what makes the progress bar real.
            CountingSource(body.source()) { read -> onBytes(read, totalBytes) }
                .buffer()
                .inputStream()
                .reader(charset)
                .useLines { lines -> importEntries(source.id, lines, onBatch) }
        }
    }

    private suspend fun importFromLocalFile(
        source: Source.LocalFileSource,
        onBytes: (Long, Long) -> Unit,
        onBatch: suspend (List<Channel>) -> Unit,
    ): Int {
        val uri = Uri.parse(source.fileUri)
        val totalBytes = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull()?.coerceAtLeast(0L) ?: 0L

        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Não foi possível abrir o arquivo: ${source.fileUri}")
        return input.use { stream ->
            CountingSource(stream.source()) { read -> onBytes(read, totalBytes) }
                .buffer()
                .inputStream()
                .reader()
                .useLines { lines -> importEntries(source.id, lines, onBatch) }
        }
    }

    /** Parses [lines] and flushes a batch every [BATCH_SIZE] channels. Returns the total. */
    private suspend fun importEntries(
        sourceId: Long,
        lines: Sequence<String>,
        onBatch: suspend (List<Channel>) -> Unit,
    ): Int {
        val batch = ArrayList<Channel>(BATCH_SIZE)
        var total = 0

        for (entry in M3uParser.entries(lines)) {
            batch += entry.toChannel(sourceId, total + batch.size)
            if (batch.size >= BATCH_SIZE) {
                onBatch(ArrayList(batch))
                total += batch.size
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) {
            onBatch(ArrayList(batch))
            total += batch.size
        }

        return total
    }

    private suspend fun importFromXtream(
        source: Source.XtreamSource,
        onBatch: suspend (List<Channel>) -> Unit,
    ): Int {
        val baseUrl = source.baseUrl()
        val api = NetworkModule.provideXtreamApi(baseUrl, okHttpClient)

        val categoriesJson = api.getLiveCategoriesJson(source.username, source.password)
        val categoryNamesById = xtreamJson.decodeFromString<List<XtreamCategory>>(categoriesJson)
            .associate { it.categoryId to it.categoryName }

        val streamsJson = api.getLiveStreamsJson(source.username, source.password)
        val streams = xtreamJson.decodeFromString<List<XtreamLiveStream>>(streamsJson)

        var total = 0
        for (window in streams.chunked(BATCH_SIZE)) {
            val batch = window.mapIndexed { index, stream ->
                val groupName = stream.categoryId
                    ?.let { categoryNamesById[it] }
                    ?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_GROUP
                Channel(
                    sourceId = source.id,
                    originalName = stream.name,
                    displayName = stream.name,
                    logoUrl = stream.streamIcon,
                    streamUrl = buildXtreamStreamUrl(baseUrl, source.username, source.password, stream.streamId),
                    streamKey = "xtream-${stream.streamId}",
                    originalGroup = groupName,
                    displayGroup = groupName,
                    sortOrder = total + index,
                )
            }
            onBatch(batch)
            total += batch.size
        }
        return total
    }

    /**
     * Counts bytes as they are read, reporting at most every [REPORT_EVERY_BYTES] so a progress
     * callback cannot become the bottleneck of the download itself.
     */
    private class CountingSource(
        delegate: OkioSource,
        private val onProgress: (Long) -> Unit,
    ) : ForwardingSource(delegate) {
        private var totalRead = 0L
        private var lastReported = 0L

        override fun read(sink: Buffer, byteCount: Long): Long {
            val read = super.read(sink, byteCount)
            if (read > 0) {
                totalRead += read
                if (totalRead - lastReported >= REPORT_EVERY_BYTES) {
                    lastReported = totalRead
                    onProgress(totalRead)
                }
            }
            return read
        }
    }

    private fun ParsedM3uEntry.toChannel(sourceId: Long, position: Int): Channel {
        val group = groupTitle?.takeIf { it.isNotBlank() } ?: DEFAULT_GROUP
        return Channel(
            sourceId = sourceId,
            originalName = name,
            displayName = name,
            logoUrl = logoUrl,
            streamUrl = streamUrl,
            streamKey = streamUrl,
            originalGroup = group,
            displayGroup = group,
            sortOrder = position,
        )
    }
}
