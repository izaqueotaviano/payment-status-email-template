package com.iptvtv.player.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.domain.repository.SourceRepository
import com.iptvtv.player.domain.usecase.SyncSourceUseCase
import com.iptvtv.player.domain.usecase.SyncProgress
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * How old a stored list may be before opening the channel list re-imports it on its own. Provider
 * line-ups change rarely; the "Atualizar" button is there for the moment they do.
 */
private const val STALE_AFTER_MILLIS = 12L * 60 * 60 * 1000

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelListViewModel(
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository,
    private val sourceRepository: SourceRepository,
    private val syncSourceUseCase: SyncSourceUseCase,
) : ViewModel() {

    private val currentSourceId = MutableStateFlow<Long?>(null)

    fun setSource(sourceId: Long) {
        currentSourceId.value = sourceId
        // Checked on every entry, not only when the source changes: this view model outlives a
        // visit, so a list could otherwise never go stale for as long as the process lives.
        // refresh() already refuses to start a second import of its own.
        viewModelScope.launch { syncIfStale(sourceId) }
    }

    /**
     * Imports only when there is a reason to: nothing stored yet, or a list old enough to have
     * changed. Opening the channel list used to re-download and re-merge the whole playlist every
     * single time, which on a provider list is minutes of waiting for a list the app already had.
     */
    private suspend fun syncIfStale(sourceId: Long) {
        val stale = try {
            val age = System.currentTimeMillis() - sourceRepository.lastImportAttempt(sourceId)
            // A negative age means the clock moved backwards (or the stamp is from the future);
            // either way the stamp tells us nothing, so treat it as stale.
            channelRepository.countForSource(sourceId) == 0 || age !in 0 until STALE_AFTER_MILLIS
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Reading the stamp is not worth failing the screen over; the manual button remains.
            false
        }
        if (stale) refresh()
    }

    private val isRefreshingFlow = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = isRefreshingFlow

    /**
     * Room re-runs every active query after each write, so observing the table through an import
     * means re-reading and re-mapping all of it once per batch - work that grows with the square of
     * the playlist, and the single largest reason importing crawled.
     *
     * So an import reads the list once and shows that, rather than observing. Simply unsubscribing
     * was worse than either: the gate closed before the first read had delivered, so the screen sat
     * on "nenhum canal" for the whole import while a perfectly usable list sat in the database.
     */
    val allChannels: StateFlow<List<Channel>> =
        combine(currentSourceId, isRefreshingFlow) { sourceId, refreshing -> sourceId to refreshing }
            .flatMapLatest { (sourceId, refreshing) ->
                when {
                    sourceId == null -> flowOf(emptyList())
                    refreshing -> flow { emit(channelRepository.channelsFor(sourceId)) }
                    else -> channelRepository.observeChannels(sourceId)
                }
            }
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
        visible
    }
        // Filtering a playlist of tens of thousands of channels runs on every keystroke; it does
        // not belong on the main thread. Sorting is not repeated here - both queries that feed
        // allChannels already order by sortOrder.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val syncErrorFlow = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = syncErrorFlow

    private val syncProgressFlow = MutableStateFlow<SyncProgress?>(null)
    val syncProgress: StateFlow<SyncProgress?> = syncProgressFlow

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
                val result = if (source != null) {
                    syncSourceUseCase(source) { progress -> syncProgressFlow.value = progress }
                } else {
                    Result.success(0)
                }
                syncErrorFlow.value = result.exceptionOrNull()?.message
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                syncErrorFlow.value = error.message ?: "Falha ao sincronizar"
            } finally {
                syncProgressFlow.value = null
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
