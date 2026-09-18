package com.iptvtv.player.ui.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.ui.channels.components.ChannelRow
import com.iptvtv.player.ui.components.AppBackground
import com.iptvtv.player.ui.components.FilterChip
import com.iptvtv.player.ui.components.NavRail
import com.iptvtv.player.ui.components.NavSection
import com.iptvtv.player.ui.components.PillButton
import com.iptvtv.player.ui.components.PlayIcon
import com.iptvtv.player.ui.components.SearchField
import com.iptvtv.player.ui.theme.BrandAccent
import com.iptvtv.player.ui.theme.BrandError
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandOutline
import com.iptvtv.player.ui.theme.BrandSurface
import com.iptvtv.player.ui.theme.BrandSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun ChannelListScreen(
    viewModel: ChannelListViewModel,
    sourceId: Long,
    onChannelClick: (Long) -> Unit,
    onOpenSources: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    LaunchedEffect(sourceId) {
        viewModel.setSource(sourceId)
    }

    val query by viewModel.searchQuery.collectAsState()
    val favoritesOnly by viewModel.favoritesOnly.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val channels by viewModel.visibleChannels.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val defaultChannelId by viewModel.defaultChannelId.collectAsState()

    val showHidden by viewModel.showHidden.collectAsState()
    val hiddenCount by viewModel.hiddenCount.collectAsState()

    var dialogChannel by remember { mutableStateOf<Channel?>(null) }
    var showManageDialog by remember { mutableStateOf(false) }
    var previewChannelId by remember { mutableStateOf<Long?>(null) }

    // The previewed channel can leave the list under the user (unfavoriting it while the
    // "Favoritos" filter is on). Re-anchoring the id keeps the panel mounted - letting it vanish
    // would take the focus owner with it - and keeps what the buttons act on equal to what is
    // drawn, instead of silently re-pointing at another channel.
    LaunchedEffect(channels, previewChannelId) {
        if (previewChannelId != null && channels.none { it.id == previewChannelId }) {
            previewChannelId = channels.firstOrNull()?.id
        }
    }
    val previewChannel = channels.firstOrNull { it.id == previewChannelId } ?: channels.firstOrNull()
    val liveDialogChannel = dialogChannel?.let { stored ->
        allChannels.firstOrNull { it.id == stored.id } ?: stored
    }

    val hasFilters = query.isNotBlank() || favoritesOnly || selectedGroup != null
    val clearFilters = {
        viewModel.searchQuery.value = ""
        viewModel.favoritesOnly.value = false
        viewModel.selectedGroup.value = null
    }

    val listState = rememberLazyListState()
    val rowFocus = remember { FocusRequester() }
    val emptyStateFocus = remember { FocusRequester() }
    // One shot, and anchored to the restored scroll position: re-running it would yank focus out
    // of the search field while typing, and targeting row 0 would find nothing composed after
    // returning from the player deep in the list.
    var focusAnchorIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(channels.isNotEmpty()) {
        if (focusAnchorIndex == -1 && channels.isNotEmpty()) {
            focusAnchorIndex = listState.firstVisibleItemIndex.coerceAtMost(channels.lastIndex)
            delay(150)
            runCatching { rowFocus.requestFocus() }
        }
    }
    // An empty list removes every focusable in the main area, so hand focus to its action - but
    // never while the user is typing, or a search that momentarily matches nothing would pull
    // the focus out of the field and close the keyboard mid-word.
    LaunchedEffect(channels.isEmpty(), query.isBlank()) {
        if (channels.isEmpty() && query.isBlank()) {
            delay(150)
            runCatching { emptyStateFocus.requestFocus() }
        }
    }

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            nowMillis = System.currentTimeMillis()
        }
    }
    val clock = remember(nowMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
    }

    AppBackground {
        Row(modifier = Modifier.fillMaxSize()) {
            NavRail(
                selected = NavSection.Channels,
                favoritesActive = favoritesOnly,
                clock = clock,
                onSelect = { section ->
                    when (section) {
                        NavSection.Channels -> viewModel.selectedGroup.value = null
                        NavSection.Favorites -> viewModel.favoritesOnly.value = !favoritesOnly
                        NavSection.Sources -> onOpenSources()
                        NavSection.Settings -> onOpenSettings()
                    }
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 26.dp, vertical = 22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ao vivo",
                            color = BrandOnSurface,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (channels.isEmpty()) {
                                "Nenhum canal para exibir"
                            } else {
                                "${channels.size} canais · ${groups.size} categorias"
                            },
                            color = BrandMuted,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    SearchField(
                        value = query,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = "Buscar canal",
                        modifier = Modifier.width(250.dp),
                    )

                    PillButton(
                        text = if (isRefreshing) "Atualizando" else "Atualizar",
                        onClick = { viewModel.refresh() },
                        enabled = !isRefreshing,
                        modifier = Modifier.padding(start = 12.dp),
                    )

                    PillButton(
                        text = "Gerenciar",
                        onClick = { showManageDialog = true },
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                ) {
                    item {
                        FilterChip(
                            text = "Todos",
                            selected = selectedGroup == null && !favoritesOnly,
                            onClick = {
                                viewModel.selectedGroup.value = null
                                viewModel.favoritesOnly.value = false
                            },
                        )
                    }
                    item {
                        FilterChip(
                            text = "Favoritos",
                            selected = favoritesOnly,
                            onClick = { viewModel.favoritesOnly.value = !favoritesOnly },
                        )
                    }
                    items(groups, key = { group: String -> group }) { group: String ->
                        FilterChip(
                            text = group,
                            selected = selectedGroup == group,
                            onClick = {
                                viewModel.selectedGroup.value = if (selectedGroup == group) null else group
                            },
                        )
                    }
                }

                val error = syncError
                if (error != null) {
                    Text(
                        text = "Erro ao sincronizar: $error",
                        color = BrandError,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(Color(0x33FF6B81), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }

                Row(modifier = Modifier.fillMaxSize().padding(top = 18.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (channels.isEmpty()) {
                            EmptyState(
                                isRefreshing = isRefreshing,
                                hasFilters = hasFilters,
                                onClearFilters = clearFilters,
                                onRefresh = { viewModel.refresh() },
                                actionModifier = Modifier.focusRequester(emptyStateFocus),
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(
                                    channels,
                                    key = { _: Int, channel: Channel -> channel.id },
                                ) { index: Int, channel: Channel ->
                                    ChannelRow(
                                        channel = channel,
                                        position = index,
                                        isDefault = channel.id == defaultChannelId,
                                        isPreviewed = channel.id == previewChannel?.id,
                                        onClick = { onChannelClick(channel.id) },
                                        onLongPress = { dialogChannel = channel },
                                        onFocused = { previewChannelId = channel.id },
                                        modifier = if (index == focusAnchorIndex) {
                                            Modifier.focusRequester(rowFocus)
                                        } else {
                                            Modifier
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (previewChannel != null) {
                        PreviewPanel(
                            channel = previewChannel,
                            isDefault = previewChannel.id == defaultChannelId,
                            onPlay = { onChannelClick(previewChannel.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(previewChannel) },
                            onEdit = { dialogChannel = previewChannel },
                            modifier = Modifier.padding(start = 22.dp).width(330.dp).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }

    if (showManageDialog) {
        ManageChannelsDialog(
            favoriteCount = allChannels.count { it.isFavorite },
            hiddenCount = hiddenCount,
            showHidden = showHidden,
            onToggleShowHidden = { viewModel.showHidden.value = !showHidden },
            onKeepOnlyFavorites = {
                viewModel.keepOnlyFavorites()
                showManageDialog = false
            },
            onShowAll = {
                viewModel.showAllChannels()
                showManageDialog = false
            },
            onDismiss = { showManageDialog = false },
        )
    }

    liveDialogChannel?.let { channel ->
        EditChannelDialog(
            channel = channel,
            isDefault = channel.id == defaultChannelId,
            onDismiss = { dialogChannel = null },
            onPlay = {
                dialogChannel = null
                onChannelClick(channel.id)
            },
            onToggleFavorite = { viewModel.toggleFavorite(channel) },
            onToggleHidden = { viewModel.toggleHidden(channel) },
            onRename = { newName ->
                viewModel.rename(channel, newName)
                dialogChannel = null
            },
            onRegroup = { newGroup ->
                viewModel.regroup(channel, newGroup)
                dialogChannel = null
            },
            onMoveUp = { viewModel.moveUp(channel) },
            onMoveDown = { viewModel.moveDown(channel) },
            onSetDefault = { viewModel.setAsDefault(channel) },
            onClearDefault = { viewModel.clearDefault() },
        )
    }
}

@Composable
private fun EmptyState(
    isRefreshing: Boolean,
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    actionModifier: Modifier,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isRefreshing) "Sincronizando canais..." else "Nenhum canal por aqui",
                color = BrandOnSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when {
                    isRefreshing -> "Isso pode levar alguns segundos em listas grandes."
                    hasFilters -> "Nenhum canal corresponde aos filtros aplicados."
                    else -> "Importe uma fonte em Fontes para começar."
                },
                color = BrandMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            PillButton(
                text = if (hasFilters) "Limpar filtros" else "Atualizar agora",
                onClick = if (hasFilters) onClearFilters else onRefresh,
                primary = true,
                enabled = hasFilters || !isRefreshing,
                modifier = actionModifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun PreviewPanel(
    channel: Channel,
    isDefault: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .background(BrandSurface.copy(alpha = 0.9f), shape)
            .border(1.dp, BrandOutline, shape)
            .padding(18.dp),
    ) {
        // The artwork takes whatever height is left so the actions below always stay on screen,
        // however short the panel gets.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF2A1C4A), Color(0xFF14121F))),
                    RoundedCornerShape(15.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(28.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(BrandSurfaceVariant, RoundedCornerShape(27.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayIcon(color = BrandOnSurface, modifier = Modifier.size(20.dp))
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(BrandAccent, RoundedCornerShape(4.dp)),
            )
            Text(
                text = "AO VIVO",
                color = BrandAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
            if (isDefault) {
                Text(
                    text = "· CANAL PADRÃO",
                    color = BrandMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        Text(
            text = channel.displayName,
            color = BrandOnSurface,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )

        Text(
            text = channel.displayGroup,
            color = BrandMuted,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )

        Spacer(modifier = Modifier.height(14.dp))

        PillButton(
            text = "Assistir agora",
            onClick = onPlay,
            primary = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            PillButton(
                text = if (channel.isFavorite) "Remover" else "Favoritar",
                onClick = onToggleFavorite,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "Editar",
                onClick = onEdit,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
        }
    }
}
