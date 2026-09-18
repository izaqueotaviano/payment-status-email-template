package com.iptvtv.player.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives full-screen playback for a source: resolves the channel to start on, keeps the
 * ordered/visible channel list used for D-pad previous/next, and surfaces player errors.
 */
class PlayerViewModel(
    context: Context,
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val manager = PlayerManager(context)
    val player: ExoPlayer get() = manager.exoPlayer

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Visible, ordered channel list used to resolve prev/next. Kept in sync with the repository. */
    private var visibleChannels: List<Channel> = emptyList()
    private var currentIndex: Int = -1
    private var hasResolvedInitialChannel = false

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                _errorMessage.value = error.message
            }
        })
    }

    fun start(sourceId: Long, initialChannelId: Long) {
        viewModelScope.launch {
            channelRepository.observeChannels(sourceId).collect { channels ->
                val visible = channels.filter { !it.isHidden }.sortedBy { it.sortOrder }
                visibleChannels = visible

                if (!hasResolvedInitialChannel) {
                    hasResolvedInitialChannel = true
                    val index = visible.indexOfFirst { it.id == initialChannelId }
                    if (index >= 0) {
                        currentIndex = index
                        playChannel(visible[index])
                    } else {
                        val fallback = channelRepository.getChannel(initialChannelId)
                        if (fallback != null) {
                            playChannel(fallback)
                        }
                    }
                } else {
                    // The list changed after we started (a resync, a hide/show, a reorder...) -
                    // keep currentIndex pointing at whatever channel is actually on screen so
                    // next()/previous() don't silently desync from it.
                    val playingId = _currentChannel.value?.id
                    if (playingId != null) {
                        val newIndex = visible.indexOfFirst { it.id == playingId }
                        if (newIndex >= 0) currentIndex = newIndex
                    }
                }
            }
        }
    }

    fun next() {
        if (visibleChannels.isEmpty() || currentIndex < 0) return
        currentIndex = (currentIndex + 1).mod(visibleChannels.size)
        playChannel(visibleChannels[currentIndex])
    }

    fun previous() {
        if (visibleChannels.isEmpty() || currentIndex < 0) return
        currentIndex = (currentIndex - 1).mod(visibleChannels.size)
        playChannel(visibleChannels[currentIndex])
    }

    private fun playChannel(channel: Channel) {
        _currentChannel.value = channel
        manager.play(channel.streamUrl)
    }

    override fun onCleared() {
        manager.release()
    }
}
