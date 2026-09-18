package com.iptvtv.player

import android.app.Application
import com.iptvtv.player.di.AppContainer

class IptvApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.crashReporter.install()
    }
}
