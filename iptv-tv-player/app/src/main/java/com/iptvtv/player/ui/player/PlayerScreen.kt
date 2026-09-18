package com.iptvtv.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvtv.player.ui.theme.BrandAccent
import com.iptvtv.player.ui.theme.BrandError
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandOutline
import com.iptvtv.player.ui.theme.BrandSurfaceVariant
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
            .background(Color.Black)
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
            val shape = RoundedCornerShape(18.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
                    .wrapContentSize()
                    .background(Color(0xE60E0C16), shape)
                    .border(1.dp, BrandOutline, shape)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                if (channel.logoUrl != null) {
                    Box(
                        modifier = Modifier
                            .size(width = 58.dp, height = 40.dp)
                            .background(BrandSurfaceVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(5.dp),
                        )
                    }
                }

                Column(modifier = Modifier.padding(start = if (channel.logoUrl != null) 14.dp else 0.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(BrandAccent, RoundedCornerShape(3.dp)),
                        )
                        Text(
                            text = "AO VIVO",
                            color = BrandAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                    Text(
                        text = channel.displayName,
                        color = BrandOnSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(320.dp).padding(top = 2.dp),
                    )
                    Text(
                        text = "${channel.displayGroup}  ·  Esquerda/direita troca de canal",
                        color = BrandMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(320.dp),
                    )
                }
            }
        }

        val error = errorMessage
        if (error != null) {
            val shape = RoundedCornerShape(14.dp)
            Text(
                text = error,
                color = BrandError,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .wrapContentSize()
                    .background(Color(0xE61A0E14), shape)
                    .border(1.dp, BrandError.copy(alpha = 0.5f), shape)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }
}
