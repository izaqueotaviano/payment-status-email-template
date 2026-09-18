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

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories",
    ): List<XtreamCategory>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams",
    ): List<XtreamLiveStream>
}

/**
 * Builds the direct playback URL for a live stream on an Xtream Codes server.
 *
 * [baseUrl] must already contain the scheme, host and optional port (no trailing slash).
 */
fun buildXtreamStreamUrl(baseUrl: String, username: String, password: String, streamId: Int): String {
    return "$baseUrl/live/$username/$password/$streamId.ts"
}
