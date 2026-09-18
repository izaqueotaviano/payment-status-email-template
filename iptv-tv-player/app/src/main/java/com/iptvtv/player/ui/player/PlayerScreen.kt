package com.iptvtv.player.ui.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvtv.player.ui.components.PillButton
import com.iptvtv.player.ui.theme.BrandAccent
import com.iptvtv.player.ui.theme.BrandError
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandOutline
import com.iptvtv.player.ui.theme.BrandSurfaceVariant
import kotlinx.coroutines.delay

private const val CONTROLS_TIMEOUT_MS = 6000L
private const val CHANNEL_OVERLAY_MS = 2500L

/** Which options list is open above the control bar, if any. */
private enum class PlayerPanel { Audio, Subtitles, Aspect }

/** How the video is scaled into the screen. */
@OptIn(UnstableApi::class)
private enum class AspectMode(val label: String, val resizeMode: Int) {
    Fit("Ajustar", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    Zoom("Preencher", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    Stretch("Esticar", AspectRatioFrameLayout.RESIZE_MODE_FILL),
}

/** One row of the options panel, whatever the panel is listing. */
private data class PanelOption(
    val label: String,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

/**
 * Asks for focus more than once: a node composed on the same frame can reject the first request,
 * and on this screen a missed request leaves the remote with nothing to drive.
 */
private suspend fun FocusRequester.requestFocusInsistently() {
    repeat(5) { attempt ->
        delay(if (attempt == 0) 60L else 120L)
        if (runCatching { requestFocus() }.isSuccess) return
    }
}

/**
 * Full-screen IPTV player.
 *
 * With the controls hidden the remote drives playback directly: left/right change channel, OK
 * (or up/down) opens the controls, back exits. With the controls open the D-pad moves between
 * them and back closes them, so the same keys never mean two things at once.
 *
 * Focus is always handed over explicitly. The root Box covers the whole screen, so Compose's
 * directional search can never move focus out of it into a child - whenever what is on screen
 * changes, the code below points focus at whatever should own the remote next.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    sourceId: Long,
    initialChannelId: Long,
    onExit: () -> Unit,
) {
    val rootFocus = remember { FocusRequester() }
    val barFocus = remember { FocusRequester() }
    val panelFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }

    val currentChannel by viewModel.currentChannel.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val audioTracks by viewModel.audioTracks.collectAsState()
    val subtitleTracks by viewModel.subtitleTracks.collectAsState()

    var controlsVisible by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf<PlayerPanel?>(null) }
    var aspectMode by remember { mutableStateOf(AspectMode.Fit) }
    var showChannelOverlay by remember { mutableStateOf(false) }
    // Bumped on every key press so the auto-hide countdown restarts while the user is busy.
    var activityTick by remember { mutableIntStateOf(0) }

    val hasError = errorMessage != null

    LaunchedEffect(sourceId, initialChannelId) {
        viewModel.start(sourceId, initialChannelId)
    }

    LaunchedEffect(controlsVisible, panel, hasError) {
        val target = when {
            controlsVisible && panel != null -> panelFocus
            controlsVisible -> barFocus
            hasError -> retryFocus
            else -> rootFocus
        }
        target.requestFocusInsistently()
    }

    LaunchedEffect(controlsVisible, panel, activityTick) {
        if (controlsVisible && panel == null) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }

    LaunchedEffect(currentChannel) {
        if (currentChannel != null) {
            showChannelOverlay = true
            delay(CHANNEL_OVERLAY_MS)
            showChannelOverlay = false
        }
    }

    // Back is handled here rather than as a key: newer Android versions route it through the
    // predictive-back dispatcher instead of dispatching KEYCODE_BACK, and this also works when
    // nothing on screen holds focus.
    BackHandler {
        when {
            panel != null -> panel = null
            controlsVisible -> controlsVisible = false
            else -> onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            // Preview runs before the focused control consumes the key, so OK presses on a
            // button also count as activity.
            .onPreviewKeyEvent { event ->
                if (controlsVisible && event.type == KeyEventType.KeyDown) {
                    activityTick++
                }
                false
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) {
                    return@onKeyEvent false
                }
                when (event.key) {
                    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                        viewModel.togglePlayPause()
                        true
                    }
                    // While the controls are open the arrows belong to them, so these only act
                    // when nothing is on screen.
                    Key.DirectionLeft -> if (!controlsVisible) {
                        viewModel.previous()
                        true
                    } else {
                        false
                    }
                    Key.DirectionRight -> if (!controlsVisible) {
                        viewModel.next()
                        true
                    } else {
                        false
                    }
                    Key.DirectionCenter, Key.Enter, Key.DirectionUp, Key.DirectionDown ->
                        if (!controlsVisible) {
                            controlsVisible = true
                            true
                        } else {
                            false
                        }
                    else -> false
                }
            }
            .focusable(),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    // The app draws its own TV-friendly controls; Media3's default controller
                    // would fight this screen for focus and for the D-pad keys.
                    useController = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                }
            },
            update = { view -> view.resizeMode = aspectMode.resizeMode },
            modifier = Modifier.fillMaxSize(),
        )

        if (isBuffering) {
            Text(
                text = "Carregando...",
                color = BrandOnSurface,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xCC0E0C16), RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }

        val channel = currentChannel
        if ((showChannelOverlay || controlsVisible) && channel != null) {
            ChannelBadge(
                name = channel.displayName,
                group = channel.displayGroup,
                logoUrl = channel.logoUrl,
                modifier = Modifier.align(Alignment.TopStart).padding(32.dp),
            )
        }

        // Everything that can appear at the bottom lives in one stack so an error card and an
        // options panel can never end up drawn on top of each other.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    if (controlsVisible) {
                        Modifier.background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xF2080610))),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 32.dp, vertical = 26.dp),
        ) {
            val error = errorMessage
            if (error != null) {
                ErrorCard(
                    message = error,
                    onRetry = { viewModel.retry() },
                    retryModifier = Modifier.focusRequester(retryFocus),
                )
            }

            if (controlsVisible) {
                panel?.let { openPanel ->
                    OptionsPanel(
                        panel = openPanel,
                        audioTracks = audioTracks,
                        subtitleTracks = subtitleTracks,
                        aspectMode = aspectMode,
                        focusRequester = panelFocus,
                        onSelectAudio = { id ->
                            viewModel.selectAudioTrack(id)
                            panel = null
                        },
                        onSelectSubtitle = { id ->
                            viewModel.selectSubtitleTrack(id)
                            panel = null
                        },
                        onSelectAspect = { mode ->
                            aspectMode = mode
                            panel = null
                        },
                        modifier = Modifier.align(Alignment.Start).padding(top = 14.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                ) {
                    PillButton(
                        text = if (isPlaying) "Pausar" else "Reproduzir",
                        onClick = { viewModel.togglePlayPause() },
                        primary = true,
                        modifier = Modifier.focusRequester(barFocus),
                    )
                    PillButton(text = "Canal anterior", onClick = { viewModel.previous() })
                    PillButton(text = "Próximo canal", onClick = { viewModel.next() })
                    // These stay mounted even with nothing to choose: a button that disappears
                    // under the focus takes the remote down with it.
                    PillButton(
                        text = "Áudio",
                        onClick = { panel = if (panel == PlayerPanel.Audio) null else PlayerPanel.Audio },
                    )
                    PillButton(
                        text = "Legendas",
                        onClick = { panel = if (panel == PlayerPanel.Subtitles) null else PlayerPanel.Subtitles },
                    )
                    PillButton(
                        text = "Proporção",
                        onClick = { panel = if (panel == PlayerPanel.Aspect) null else PlayerPanel.Aspect },
                    )
                    PillButton(text = "Lista de canais", onClick = onExit)
                }

                Text(
                    text = "OK abre os controles · Esquerda/direita troca de canal · Voltar sai",
                    color = BrandMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    retryModifier: Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .wrapContentSize()
            .background(Color(0xE61A0E14), shape)
            .border(1.dp, BrandError.copy(alpha = 0.5f), shape)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = "Não foi possível reproduzir: $message",
            color = BrandError,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(420.dp),
        )
        PillButton(
            text = "Tentar novamente",
            onClick = onRetry,
            modifier = retryModifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun ChannelBadge(
    name: String,
    group: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .wrapContentSize()
            .background(Color(0xE60E0C16), shape)
            .border(1.dp, BrandOutline, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        if (logoUrl != null) {
            Box(
                modifier = Modifier
                    .size(width = 58.dp, height = 40.dp)
                    .background(BrandSurfaceVariant, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(5.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(start = if (logoUrl != null) 14.dp else 0.dp)) {
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
                text = name,
                color = BrandOnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(320.dp).padding(top = 2.dp),
            )
            Text(
                text = group,
                color = BrandMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(320.dp),
            )
        }
    }
}

@Composable
private fun OptionsPanel(
    panel: PlayerPanel,
    audioTracks: List<TrackOption>,
    subtitleTracks: List<TrackOption>,
    aspectMode: AspectMode,
    focusRequester: FocusRequester,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String) -> Unit,
    onSelectAspect: (AspectMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val title = when (panel) {
        PlayerPanel.Audio -> "ÁUDIO"
        PlayerPanel.Subtitles -> "LEGENDAS"
        PlayerPanel.Aspect -> "PROPORÇÃO DA IMAGEM"
    }
    val options = when (panel) {
        PlayerPanel.Audio -> audioTracks.map { track ->
            PanelOption(track.label, track.isSelected) { onSelectAudio(track.id) }
        }
        PlayerPanel.Subtitles -> subtitleTracks.map { track ->
            PanelOption(track.label, track.isSelected) { onSelectSubtitle(track.id) }
        }
        PlayerPanel.Aspect -> AspectMode.entries.map { mode ->
            PanelOption(mode.label, mode == aspectMode) { onSelectAspect(mode) }
        }
    }
    // Focus lands on what is currently active, so the list opens where the user left it.
    val focusIndex = options.indexOfFirst { it.selected }.coerceAtLeast(0)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .width(360.dp)
            .heightIn(max = 300.dp)
            .background(Color(0xF216141F), shape)
            .border(1.dp, BrandOutline, shape)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        Text(text = title, color = BrandMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

        if (options.isEmpty()) {
            Text(
                text = "Este canal não oferece opções aqui.",
                color = BrandOnSurface,
                fontSize = 14.sp,
            )
        } else {
            options.forEachIndexed { index, option ->
                PillButton(
                    text = option.label,
                    onClick = option.onSelect,
                    primary = option.selected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (index == focusIndex) Modifier.focusRequester(focusRequester) else Modifier,
                        ),
                )
            }
        }
    }
}
