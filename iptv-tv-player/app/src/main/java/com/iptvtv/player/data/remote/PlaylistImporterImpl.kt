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

private const val DEFAULT_GROUP = "Geral"
private val xtreamJson = Json { ignoreUnknownKeys = true }

/**
 * Fetches and parses channels for a saved [Source]: an M3U playlist by URL, a local M3U file
 * picked through Storage Access Framework, or the Xtream Codes live-stream API.
 */
class PlaylistImporterImpl(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) : PlaylistImporter {

    override suspend fun fetchChannels(source: Source): Result<List<Channel>> = withContext(Dispatchers.IO) {
        runCatching {
            when (source) {
                is Source.M3uUrlSource -> fetchFromM3uUrl(source)
                is Source.LocalFileSource -> fetchFromLocalFile(source)
                is Source.XtreamSource -> fetchFromXtream(source)
            }
        }
    }

    private fun fetchFromM3uUrl(source: Source.M3uUrlSource): List<Channel> {
        val request = Request.Builder().url(source.url).build()
        val body = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch playlist: HTTP ${response.code}")
            }
            response.body?.string() ?: throw IOException("Empty playlist response")
        }
        return M3uParser.parse(body).toChannels(source.id)
    }

    private fun fetchFromLocalFile(source: Source.LocalFileSource): List<Channel> {
        val uri = Uri.parse(source.fileUri)
        val content = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: throw IOException("Unable to open local playlist file: ${source.fileUri}")
        return M3uParser.parse(content).toChannels(source.id)
    }

    private suspend fun fetchFromXtream(source: Source.XtreamSource): List<Channel> {
        val baseUrl = source.baseUrl()
        val api = NetworkModule.provideXtreamApi(baseUrl, okHttpClient)

        val categoriesJson = api.getLiveCategoriesJson(source.username, source.password)
        val categories = xtreamJson.decodeFromString<List<XtreamCategory>>(categoriesJson)
        val categoryNamesById = categories.associate { it.categoryId to it.categoryName }

        val streamsJson = api.getLiveStreamsJson(source.username, source.password)
        val streams = xtreamJson.decodeFromString<List<XtreamLiveStream>>(streamsJson)

        return streams.mapIndexed { index, stream ->
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
                sortOrder = index,
            )
        }
    }

    private fun List<ParsedM3uEntry>.toChannels(sourceId: Long): List<Channel> {
        return mapIndexed { index, entry ->
            val group = entry.groupTitle?.takeIf { it.isNotBlank() } ?: DEFAULT_GROUP
            Channel(
                sourceId = sourceId,
                originalName = entry.name,
                displayName = entry.name,
                logoUrl = entry.logoUrl,
                streamUrl = entry.streamUrl,
                streamKey = entry.streamUrl,
                originalGroup = group,
                displayGroup = group,
                sortOrder = index,
            )
        }
    }
}
