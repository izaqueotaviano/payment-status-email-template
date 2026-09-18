package com.iptvtv.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.iptvtv.player.di.AppContainer

class IptvApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.crashReporter.install()
    }

    /**
     * Coil defaults to a quarter of the heap for cached bitmaps. A channel guide scrolls past
     * thousands of logos, so that ceiling competes for memory with the playlist and the video
     * buffer on a device that has little to spare.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.10).build() }
            .build()
}
