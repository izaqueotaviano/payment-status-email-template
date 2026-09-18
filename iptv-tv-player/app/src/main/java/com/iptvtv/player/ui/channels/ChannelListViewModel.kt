package com.iptvtv.player.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.domain.repository.SourceRepository
import com.iptvtv.player.domain.usecase.SyncSourceUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

    /** The category chip currently applied, or null for "all categories". */
    val selectedGroup = MutableStateFlow<String?>(null)

    /** Reveals hidden channels in the list so they can be restored. */
    val showHidden = MutableStateFlow(false)

    val hiddenCount: StateFlow<Int> = currentSourceId
        .filterNotNull()
        .flatMapLatest { sourceId -> channelRepository.observeHiddenCount(sourceId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val groups: StateFlow<List<String>> = allChannels
        .map { channels -> channels.filterNot { it.isHidden }.map { it.displayGroup }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleChannels: StateFlow<List<Channel>> = combine(
        allChannels,
        searchQuery,
        favoritesOnly,
        selectedGroup,
        showHidden,
    ) { channels, query, favOnly, group, withHidden ->
        var visible = if (withHidden) channels else channels.filterNot { it.isHidden }
        if (favOnly) {
            visible = visible.filter { it.isFavorite }
        }
        if (group != null) {
            visible = visible.filter { it.displayGroup == group }
        }
        if (query.isNotBlank()) {
            visible = visible.filter { it.displayName.contains(query, ignoreCase = true) }
        }
        visible.sortedBy { it.sortOrder }
    }
        // Filtering and sorting a playlist of tens of thousands of channels runs on every
        // keystroke; it does not belong on the main thread.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val isRefreshingFlow = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = isRefreshingFlow

    private val syncErrorFlow = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = syncErrorFlow

    private var refreshJob: Job? = null

    fun refresh() {
        val sourceId = currentSourceId.value ?: return
        // Two imports of the same source race each other: both read the same "before" snapshot
        // and both insert the whole playlist, duplicating every channel for good.
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            isRefreshingFlow.value = true
            syncErrorFlow.value = null
            try {
                val source = sourceRepository.getSource(sourceId)
                val result = if (source != null) syncSourceUseCase(source) else Result.success(Unit)
                syncErrorFlow.value = result.exceptionOrNull()?.message
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                syncErrorFlow.value = error.message ?: "Falha ao sincronizar"
            } finally {
                isRefreshingFlow.value = false
            }
        }
    }

    fun toggleFavorite(c: Channel) {
        viewModelScope.launch { channelRepository.setFavorite(c.id, !c.isFavorite) }
    }

    /** Hides everything that is not a favorite - one write, however long the playlist is. */
    fun keepOnlyFavorites() {
        val sourceId = currentSourceId.value ?: return
        viewModelScope.launch { channelRepository.hideNonFavorites(sourceId) }
    }

    fun showAllChannels() {
        val sourceId = currentSourceId.value ?: return
        viewModelScope.launch { channelRepository.showAllChannels(sourceId) }
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

    /**
     * Moves [channel] past its neighbour in the list the user is actually looking at - reordering
     * against the unfiltered list would look like nothing happened while silently shuffling
     * channels off screen.
     */
    private suspend fun move(channel: Channel, deltaIndex: Int) {
        val ordered = visibleChannels.value
        val index = ordered.indexOfFirst { it.id == channel.id }
        if (index == -1) return
        val neighborIndex = index + deltaIndex
        if (neighborIndex !in ordered.indices) return
        channelRepository.swapOrder(channel.id, ordered[neighborIndex].id)
    }
}
