package com.iptvtv.player.ui.player

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

/**
 * Full-screen IPTV player. D-pad left/right switch channel, back exits to the channel list.
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    sourceId: Long,
    initialChannelId: Long,
    onExit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val currentChannel by viewModel.currentChannel.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(sourceId, initialChannelId) {
        viewModel.start(sourceId, initialChannelId)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var showChannelOverlay by remember { mutableStateOf(false) }
    LaunchedEffect(currentChannel) {
        if (currentChannel != null) {
            showChannelOverlay = true
            delay(2500)
            showChannelOverlay = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            viewModel.previous()
                            true
                        }
                        Key.DirectionRight -> {
                            viewModel.next()
                            true
                        }
                        Key.Back -> {
                            onExit()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .focusable(),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        val channel = currentChannel
        if (showChannelOverlay && channel != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .wrapContentSize(),
            ) {
                Text(
                    text = channel.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        val error = errorMessage
        if (error != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .wrapContentSize(),
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
