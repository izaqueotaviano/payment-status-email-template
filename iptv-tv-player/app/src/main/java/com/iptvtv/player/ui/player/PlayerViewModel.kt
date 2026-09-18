package com.iptvtv.player.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.domain.repository.ChannelRepository
import com.iptvtv.player.domain.repository.SettingsRepository
import com.iptvtv.player.player.PlayerManager
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One selectable audio or subtitle track, already labeled for display. */
data class TrackOption(
    val id: String,
    val label: String,
    val isSelected: Boolean,
)

/** Id of the synthetic "subtitles off" entry. */
const val SUBTITLES_OFF_ID = "off"

/**
 * Drives full-screen playback for a source: resolves the channel to start on, keeps the
 * ordered/visible channel list used for D-pad previous/next, exposes the playback state and
 * the stream's audio/subtitle tracks, and surfaces player errors.
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

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackOption>>(emptyList())
    val audioTracks: StateFlow<List<TrackOption>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackOption>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackOption>> = _subtitleTracks.asStateFlow()

    /** Maps a [TrackOption.id] back to the group and index needed to build a selection override. */
    private var trackRefs: Map<String, Pair<TrackGroup, Int>> = emptyMap()

    /** Visible, ordered channel list used to resolve prev/next. Kept in sync with the repository. */
    private var visibleChannels: List<Channel> = emptyList()
    private var currentIndex: Int = -1
    private var hasResolvedInitialChannel = false

    init {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // A null message would be indistinguishable from "no error" and would leave the
                // user staring at a black screen with no way to retry.
                _errorMessage.value = error.message ?: error.errorCodeName
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
            }

            override fun onTracksChanged(tracks: Tracks) {
                refreshTracks(tracks)
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

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    /** Replays the current channel - used to recover from a playback error. */
    fun retry() {
        _currentChannel.value?.let { playChannel(it) }
    }

    fun selectAudioTrack(optionId: String) {
        val (group, trackIndex) = trackRefs[optionId] ?: return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(group, trackIndex))
            .build()
    }

    /** Selects a subtitle track, or turns subtitles off for [SUBTITLES_OFF_ID]. */
    fun selectSubtitleTrack(optionId: String) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (optionId == SUBTITLES_OFF_ID) {
            builder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            val (group, trackIndex) = trackRefs[optionId] ?: return
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group, trackIndex))
        }
        player.trackSelectionParameters = builder.build()
    }

    private fun refreshTracks(tracks: Tracks) {
        val audio = mutableListOf<TrackOption>()
        val subtitles = mutableListOf<TrackOption>()
        val refs = mutableMapOf<String, Pair<TrackGroup, Int>>()

        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != C.TRACK_TYPE_AUDIO && group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
            for (trackIndex in 0 until group.length) {
                if (!group.isTrackSupported(trackIndex)) continue
                val optionId = "$groupIndex:$trackIndex"
                val target = if (group.type == C.TRACK_TYPE_AUDIO) audio else subtitles
                val option = TrackOption(
                    id = optionId,
                    label = trackLabel(group.getTrackFormat(trackIndex), target.size),
                    isSelected = group.isTrackSelected(trackIndex),
                )
                target += option
                refs[optionId] = group.mediaTrackGroup to trackIndex
            }
        }

        trackRefs = refs
        _audioTracks.value = audio
        _subtitleTracks.value = if (subtitles.isEmpty()) {
            emptyList()
        } else {
            listOf(
                TrackOption(
                    id = SUBTITLES_OFF_ID,
                    label = "Desativadas",
                    isSelected = subtitles.none { it.isSelected },
                ),
            ) + subtitles
        }
    }

    private fun trackLabel(format: Format, position: Int): String {
        format.label?.takeIf { it.isNotBlank() }?.let { return it }
        val language = format.language?.takeIf { it.isNotBlank() && it != "und" }
        if (language != null) {
            val display = Locale.forLanguageTag(language).displayLanguage
            if (display.isNotBlank()) {
                return display.replaceFirstChar { it.uppercase() }
            }
        }
        return "Faixa ${position + 1}"
    }

    private fun playChannel(channel: Channel) {
        _currentChannel.value = channel
        _errorMessage.value = null
        manager.play(channel.streamUrl)
    }

    override fun onCleared() {
        manager.release()
    }
}
