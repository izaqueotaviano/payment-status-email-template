package com.iptvtv.player.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.iptvtv.player.ui.channels.ChannelListViewModel
import com.iptvtv.player.ui.player.PlayerViewModel
import com.iptvtv.player.ui.settings.SettingsViewModel
import com.iptvtv.player.ui.sources.AddEditSourceViewModel
import com.iptvtv.player.ui.sources.SourcesViewModel

/** Builds every screen ViewModel by hand from [AppContainer], since there is no DI framework. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when (modelClass) {
            SourcesViewModel::class.java -> SourcesViewModel(
                container.sourceRepository,
                container.settingsRepository,
                container.channelRepository,
            )
            AddEditSourceViewModel::class.java -> AddEditSourceViewModel(
                container.sourceRepository,
                container.syncSourceUseCase,
            )
            ChannelListViewModel::class.java -> ChannelListViewModel(
                container.channelRepository,
                container.settingsRepository,
                container.sourceRepository,
                container.syncSourceUseCase,
            )
            PlayerViewModel::class.java -> PlayerViewModel(
                container.appContext,
                container.channelRepository,
                container.settingsRepository,
            )
            SettingsViewModel::class.java -> SettingsViewModel(
                container.sourceRepository,
                container.channelRepository,
                container.settingsRepository,
                container.cacheManager,
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return viewModel as T
    }
}
