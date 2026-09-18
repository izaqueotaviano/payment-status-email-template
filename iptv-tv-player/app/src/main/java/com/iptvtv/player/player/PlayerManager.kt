package com.iptvtv.player.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
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

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
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
