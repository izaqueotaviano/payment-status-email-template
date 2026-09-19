package com.iptvtv.player.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.domain.repository.SourceRepository
import com.iptvtv.player.util.CacheManager
import com.iptvtv.player.util.CrashReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val sourceRepository: SourceRepository,
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository,
    private val cacheManager: CacheManager,
    private val crashReporter: CrashReporter,
) : ViewModel() {

    private val _lastCrash = MutableStateFlow(crashReporter.lastCrash())
    val lastCrash: StateFlow<String?> = _lastCrash

    fun clearLastCrash() {
        crashReporter.clear()
        _lastCrash.value = null
    }

    val sources: StateFlow<List<Source>> = sourceRepository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultChannelName: StateFlow<String?> = settingsRepository.observeDefaultChannelId()
        .map { id -> id?.let { channelRepository.getChannel(it)?.displayName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache

    val liveOnlyImport: StateFlow<Boolean> = settingsRepository.observeLiveOnlyImport()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setLiveOnlyImport(liveOnly: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLiveOnlyImport(liveOnly)
            // Otherwise the switch appears to do nothing: every source would still count as freshly
            // imported, so no list would be re-read with the new setting for up to twelve hours.
            sourceRepository.clearImportAttempts()
        }
    }

    fun clearDefaultChannel() {
        viewModelScope.launch {
            settingsRepository.setDefaultChannelId(null)
        }
    }

    fun deleteSource(id: Long) {
        viewModelScope.launch {
            channelRepository.deleteChannelsForSource(id)
            sourceRepository.deleteSource(id)
            if (settingsRepository.observeActiveSourceId().first() == id) {
                settingsRepository.setActiveSourceId(null)
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _isClearingCache.value = true
            cacheManager.clearAll()
            _isClearingCache.value = false
        }
    }
}
