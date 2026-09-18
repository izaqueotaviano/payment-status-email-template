package com.iptvtv.player.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.domain.repository.SourceRepository
import com.iptvtv.player.domain.usecase.SyncSourceUseCase
import com.iptvtv.player.domain.usecase.SyncProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** State of an in-progress save operation for the add/edit source form. */
sealed class SaveState {
    data object Idle : SaveState()

    /** Saving is a streaming import; [progress] is what it has actually done so far. */
    data class Saving(val progress: SyncProgress) : SaveState()

    data class Success(val channelCount: Int) : SaveState()
    data class Error(val message: String) : SaveState()
}

class AddEditSourceViewModel(
    private val sourceRepository: SourceRepository,
    private val syncSourceUseCase: SyncSourceUseCase,
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    private val _loadedSource = MutableStateFlow<Source?>(null)
    val loadedSource: StateFlow<Source?> = _loadedSource

    fun loadExisting(sourceId: Long) {
        viewModelScope.launch {
            _loadedSource.value = sourceRepository.getSource(sourceId)
        }
    }

    fun save(source: Source) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving(SyncProgress())
            runCatching {
                val resolved: Source = if (source.id == 0L) {
                    val newId = sourceRepository.addSource(source)
                    withId(source, newId)
                } else {
                    sourceRepository.updateSource(source)
                    source
                }
                syncSourceUseCase(resolved) { progress ->
                    _saveState.value = SaveState.Saving(progress)
                }.getOrThrow()
            }.onSuccess { channelCount ->
                _saveState.value = SaveState.Success(channelCount)
            }.onFailure { error ->
                _saveState.value = SaveState.Error(error.message ?: "Erro desconhecido")
            }
        }
    }

    private fun withId(source: Source, id: Long): Source = when (source) {
        is Source.M3uUrlSource -> source.copy(id = id)
        is Source.XtreamSource -> source.copy(id = id)
        is Source.LocalFileSource -> source.copy(id = id)
    }
}
