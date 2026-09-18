package com.iptvtv.player.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.domain.repository.SourceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SourcesViewModel(
    private val sourceRepository: SourceRepository,
    private val settingsRepository: SettingsRepository,
    private val channelRepository: ChannelRepository,
) : ViewModel() {

    val sources: StateFlow<List<Source>> = sourceRepository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSourceId: StateFlow<Long?> = settingsRepository.observeActiveSourceId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val channelCounts: StateFlow<Map<Long, Int>> = channelRepository.observeChannelCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectActive(source: Source) {
        // Syncing happens once the channel list screen opens (ChannelListViewModel.setSource),
        // which is also where sync errors are shown - doing it here too would race the two
        // syncs against each other for the same source right after this call.
        viewModelScope.launch {
            settingsRepository.setActiveSourceId(source.id)
        }
    }

    /**
     * Deletes a source along with everything that points at it. There is no foreign key on the
     * channels table, so its rows would otherwise be orphaned, and the stored active source and
     * default channel would keep sending the app back to a source that no longer exists.
     */
    fun deleteSource(id: Long) {
        viewModelScope.launch {
            val defaultChannelId = settingsRepository.observeDefaultChannelId().first()
            if (defaultChannelId != null && channelRepository.getChannel(defaultChannelId)?.sourceId == id) {
                settingsRepository.setDefaultChannelId(null)
            }
            if (settingsRepository.observeActiveSourceId().first() == id) {
                settingsRepository.setActiveSourceId(null)
            }
            channelRepository.deleteChannelsForSource(id)
            sourceRepository.deleteSource(id)
        }
    }
}
