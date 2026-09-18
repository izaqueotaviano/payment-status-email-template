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
import kotlinx.coroutines.CancellationException
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

    /**
     * Ids of the visible channels, in order, used to resolve prev/next.
     *
     * Only the ids: observing whole rows here would hold a second full copy of a playlist that
     * can run to tens of thousands of channels, on top of the one the channel list screen still
     * has behind it on the back stack.
     */
    private var visibleChannelIds: List<Long> = emptyList()
    private var currentIndex: Int = -1

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
            try {
                // Start playing straight away: waiting for the channel list would delay the
                // first frame by a whole query and mapping of the playlist.
                channelRepository.getChannel(initialChannelId)?.let { playChannel(it) }

                channelRepository.observeVisibleChannelIds(sourceId).collect { ids ->
                    visibleChannelIds = ids
                    // The list can change under playback (a re-sync, a hide, a reorder), so keep
                    // the cursor on whatever is actually on screen.
                    val playingId = _currentChannel.value?.id ?: initialChannelId
                    currentIndex = ids.indexOf(playingId)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                // Nothing here may escape: this coroutine has no supervisor, so a throw would
                // take the whole process down instead of showing a message.
                _errorMessage.value = error.message ?: "Não foi possível iniciar o canal."
            }
        }
    }

    fun next() = playAt(currentIndex + 1)

    fun previous() = playAt(currentIndex - 1)

    private fun playAt(index: Int) {
        val ids = visibleChannelIds
        if (ids.isEmpty()) return
        val targetIndex = index.mod(ids.size)
        currentIndex = targetIndex
        viewModelScope.launch {
            runCatching { channelRepository.getChannel(ids[targetIndex]) }
                .getOrNull()
                ?.let { playChannel(it) }
        }
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
