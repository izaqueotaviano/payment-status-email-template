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

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun selectActive(source: Source) {
        viewModelScope.launch {
            settingsRepository.setActiveSourceId(source.id)
            _isSyncing.value = true
            val result = syncSourceUseCase(source)
            _syncError.value = result.exceptionOrNull()?.message
            _isSyncing.value = false
        }
    }

    fun deleteSource(id: Long) {
        viewModelScope.launch {
            sourceRepository.deleteSource(id)
        }
    }
}
