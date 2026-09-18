package com.iptvtv.player.util

import android.content.Context
import coil.Coil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/** Clears all caches the app maintains: HTTP cache and the image loader's caches. */
class CacheManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        okHttpClient.cache?.evictAll()

        try {
            val imageLoader = Coil.imageLoader(context)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (_: Exception) {
            // Best-effort: image cache clearing must never fail the overall operation.
        }
    }
}
