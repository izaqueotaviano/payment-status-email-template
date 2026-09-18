package com.iptvtv.player.di

import android.content.Context
import com.iptvtv.player.data.datastore.SettingsRepositoryImpl
import com.iptvtv.player.data.local.db.AppDatabase
import com.iptvtv.player.data.remote.NetworkModule
import com.iptvtv.player.data.remote.PlaylistImporterImpl
import com.iptvtv.player.data.repository.ChannelRepositoryImpl
import com.iptvtv.player.data.repository.SourceRepositoryImpl
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.domain.repository.SourceRepository
import com.iptvtv.player.domain.usecase.PlaylistImporter
import com.iptvtv.player.domain.usecase.SyncSourceUseCase
import com.iptvtv.player.util.CacheManager
import com.iptvtv.player.util.CrashReporter
import okhttp3.OkHttpClient

/**
 * Simple hand-rolled dependency container (no DI framework): builds every singleton the app
 * needs, wired against application [Context] so nothing here outlives the process.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    private val okHttpClient: OkHttpClient by lazy { NetworkModule.provideOkHttpClient(appContext) }

    val sourceRepository: SourceRepository by lazy { SourceRepositoryImpl(database.sourceDao()) }
    val channelRepository: ChannelRepository by lazy {
        ChannelRepositoryImpl(database, database.channelDao())
    }
    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(appContext) }
    private val playlistImporter: PlaylistImporter by lazy { PlaylistImporterImpl(appContext, okHttpClient) }
    val syncSourceUseCase: SyncSourceUseCase by lazy {
        SyncSourceUseCase(playlistImporter, channelRepository, settingsRepository)
    }
    val cacheManager: CacheManager by lazy { CacheManager(appContext, okHttpClient) }
    val crashReporter: CrashReporter by lazy { CrashReporter(appContext) }
}
