package com.iptvtv.player.data.remote.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class XtreamCategory(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
)

@Serializable
data class XtreamLiveStream(
    @SerialName("stream_id") val streamId: Int,
    val name: String,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)

/** Xtream Codes `player_api.php` endpoints used to list live categories and streams. */
interface XtreamApi {

    // Returned as raw JSON text (see NetworkModule.provideXtreamApi) and decoded by the caller
    // with kotlinx.serialization's reified decodeFromString - avoids depending on a
    // java.lang.reflect.Type-based converter for generic list types.
    @GET("player_api.php")
    suspend fun getLiveCategoriesJson(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories",
    ): String

    @GET("player_api.php")
    suspend fun getLiveStreamsJson(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
    ): String
}

/**
 * Builds the direct playback URL for a live stream on an Xtream Codes server.
 *
 * [baseUrl] must already contain the scheme, host and optional port (no trailing slash).
 */
fun buildXtreamStreamUrl(baseUrl: String, username: String, password: String, streamId: Int): String {
    return "$baseUrl/live/$username/$password/$streamId.ts"
}
