package com.iptvtv.player.ui.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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

    var dialogChannel by remember { mutableStateOf<Channel?>(null) }
    var previewChannelId by remember { mutableStateOf<Long?>(null) }

    val previewChannel = channels.firstOrNull { it.id == previewChannelId } ?: channels.firstOrNull()
    val liveDialogChannel = dialogChannel?.let { stored ->
        allChannels.firstOrNull { it.id == stored.id } ?: stored
    }

    val firstRowFocus = remember { FocusRequester() }
    LaunchedEffect(channels.isNotEmpty()) {
        if (channels.isNotEmpty()) {
            delay(150)
            runCatching { firstRowFocus.requestFocus() }
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
                    .padding(horizontal = 32.dp, vertical = 26.dp),
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
                        )
                    }

                    SearchField(
                        value = query,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = "Buscar canal",
                        modifier = Modifier.width(280.dp),
                    )

                    PillButton(
                        text = if (isRefreshing) "Atualizando" else "Atualizar",
                        onClick = { viewModel.refresh() },
                        enabled = !isRefreshing,
                        modifier = Modifier.padding(start = 12.dp),
                    )

                    Text(
                        text = clock,
                        color = BrandOnSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 20.dp),
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
                            EmptyState(isRefreshing = isRefreshing)
                        } else {
                            LazyColumn(
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
                                        modifier = if (index == 0) {
                                            Modifier.focusRequester(firstRowFocus)
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
                            modifier = Modifier.padding(start = 28.dp).width(390.dp).fillMaxHeight(),
                        )
                    }
                }
            }
        }
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
private fun EmptyState(isRefreshing: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isRefreshing) "Sincronizando canais..." else "Nenhum canal por aqui",
                color = BrandOnSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (isRefreshing) {
                    "Isso pode levar alguns segundos em listas grandes."
                } else {
                    "Importe uma fonte em Fontes ou remova os filtros aplicados."
                },
                color = BrandMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp),
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
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF2A1C4A), Color(0xFF14121F))),
                    RoundedCornerShape(16.dp),
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
            modifier = Modifier.padding(top = 18.dp),
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
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )

        Text(
            text = channel.displayGroup,
            color = BrandMuted,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        PillButton(
            text = "Assistir agora",
            onClick = onPlay,
            primary = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            PillButton(
                text = if (channel.isFavorite) "Remover" else "Favoritar",
                onClick = onToggleFavorite,
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "Editar",
                onClick = onEdit,
                modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
        }

        Text(
            text = "Segure OK em um canal para editar",
            color = BrandMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}
