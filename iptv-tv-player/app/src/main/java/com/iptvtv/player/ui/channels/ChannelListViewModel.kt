package com.iptvtv.player.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.domain.repository.SourceRepository
import com.iptvtv.player.domain.usecase.SyncSourceUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelListViewModel(
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository,
    private val sourceRepository: SourceRepository,
    private val syncSourceUseCase: SyncSourceUseCase,
) : ViewModel() {

    private val currentSourceId = MutableStateFlow<Long?>(null)

    fun setSource(sourceId: Long) {
        if (currentSourceId.value == sourceId) return
        currentSourceId.value = sourceId
        refresh()
    }

    val allChannels: StateFlow<List<Channel>> = currentSourceId
        .filterNotNull()
        .flatMapLatest { sourceId -> channelRepository.observeChannels(sourceId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultChannelId: StateFlow<Long?> = settingsRepository.observeDefaultChannelId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val searchQuery = MutableStateFlow("")
    val favoritesOnly = MutableStateFlow(false)

    val groupedChannels: StateFlow<Map<String, List<Channel>>> = combine(
        allChannels,
        searchQuery,
        favoritesOnly,
    ) { channels, query, favOnly ->
        var visible = channels.filterNot { it.isHidden }
        if (favOnly) {
            visible = visible.filter { it.isFavorite }
        }
        if (query.isNotBlank()) {
            visible = visible.filter { it.displayName.contains(query, ignoreCase = true) }
        }
        val sorted = visible.sortedBy { it.sortOrder }
        when {
            sorted.isEmpty() -> emptyMap()
            favOnly -> mapOf("Favoritos" to sorted)
            else -> sorted.groupBy { it.displayGroup }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val isRefreshingFlow = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = isRefreshingFlow

    private val syncErrorFlow = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = syncErrorFlow

    fun refresh() {
        val sourceId = currentSourceId.value ?: return
        viewModelScope.launch {
            isRefreshingFlow.value = true
            syncErrorFlow.value = null
            val source = sourceRepository.getSource(sourceId)
            val result = if (source != null) syncSourceUseCase(source) else Result.success(Unit)
            syncErrorFlow.value = result.exceptionOrNull()?.message
            isRefreshingFlow.value = false
        }
    }

    fun toggleFavorite(c: Channel) {
        viewModelScope.launch { channelRepository.setFavorite(c.id, !c.isFavorite) }
    }

    fun toggleHidden(c: Channel) {
        viewModelScope.launch { channelRepository.setHidden(c.id, !c.isHidden) }
    }

    fun rename(c: Channel, newName: String) {
        viewModelScope.launch { channelRepository.rename(c.id, newName) }
    }

    fun regroup(c: Channel, newGroup: String) {
        viewModelScope.launch { channelRepository.regroup(c.id, newGroup) }
    }

    fun setAsDefault(c: Channel) {
        viewModelScope.launch { settingsRepository.setDefaultChannelId(c.id) }
    }

    fun clearDefault() {
        viewModelScope.launch { settingsRepository.setDefaultChannelId(null) }
    }

    fun moveUp(c: Channel) {
        viewModelScope.launch { move(c, -1) }
    }

    fun moveDown(c: Channel) {
        viewModelScope.launch { move(c, 1) }
    }

    private suspend fun move(channel: Channel, deltaIndex: Int) {
        val sorted = allChannels.value.sortedBy { it.sortOrder }
        val index = sorted.indexOfFirst { it.id == channel.id }
        if (index == -1) return
        val neighborIndex = index + deltaIndex
        if (neighborIndex !in sorted.indices) return
        val reordered = sorted.toMutableList()
        val tmp = reordered[index]
        reordered[index] = reordered[neighborIndex]
        reordered[neighborIndex] = tmp
        channelRepository.reorder(reordered.map { it.id })
    }
}
