package com.iptvtv.player.data.remote

import android.content.Context
import com.iptvtv.player.data.remote.xtream.XtreamApi
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds the shared [OkHttpClient] and per-source [XtreamApi] instances used across the app. */
object NetworkModule {

    private const val CACHE_SIZE_BYTES = 50L * 1024 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    fun provideOkHttpClient(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, CACHE_SIZE_BYTES))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun provideXtreamApi(baseUrl: String, okHttpClient: OkHttpClient): XtreamApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(XtreamApi::class.java)
    }
}
