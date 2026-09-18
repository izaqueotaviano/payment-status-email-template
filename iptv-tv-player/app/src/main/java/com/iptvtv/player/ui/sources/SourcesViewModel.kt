package com.iptvtv.player.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.domain.repository.SourceRepository
import com.iptvtv.player.domain.usecase.SyncSourceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SourcesViewModel(
    private val sourceRepository: SourceRepository,
    private val settingsRepository: SettingsRepository,
    private val syncSourceUseCase: SyncSourceUseCase,
) : ViewModel() {

    val sources: StateFlow<List<Source>> = sourceRepository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSourceId: StateFlow<Long?> = settingsRepository.observeActiveSourceId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectActive(source: Source) {
        // Syncing happens once the channel list screen opens (ChannelListViewModel.setSource),
        // which is also where sync errors are shown - doing it here too would race the two
        // syncs against each other for the same source right after this call.
        viewModelScope.launch {
            settingsRepository.setActiveSourceId(source.id)
        }
    }

    fun deleteSource(id: Long) {
        viewModelScope.launch {
            sourceRepository.deleteSource(id)
        }
    }
}
