package com.iptvtv.player.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Owns a single [ExoPlayer] instance configured for IPTV playback (HLS, MPEG-TS, MP4, DASH)
 * over HTTP(S), including sources that redirect across http/https.
 *
 * Track selection is left at Media3's defaults so video resolution adapts to the stream and
 * to the device's decoding capability without any artificial cap.
 *
 * [DefaultMediaSourceFactory]'s constructor is `@UnstableApi`; the opt-in is contained here
 * with [OptIn] so callers of this class do not need to opt in themselves.
 */
@OptIn(UnstableApi::class)
class PlayerManager(context: Context) {

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setUserAgent("IPTVTvPlayer/1.0")

    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpDataSourceFactory)

    /**
     * Live IPTV gains nothing from a deep buffer - you cannot seek forward in it - while the
     * default one holds up to 50 seconds of video in memory. On a TV box sharing a small heap
     * with a channel list of tens of thousands of rows, that is worth trimming.
     */
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 5_000,
            /* maxBufferMs = */ 20_000,
            /* bufferForPlaybackMs = */ 1_500,
            /* bufferForPlaybackAfterRebufferMs = */ 3_000,
        )
        .build()

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .build()

    fun play(streamUrl: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun release() {
        exoPlayer.release()
    }
}
