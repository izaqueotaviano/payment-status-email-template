package com.iptvtv.player.ui.channels

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.ui.channels.components.ChannelCard

@Composable
fun ChannelListScreen(
    viewModel: ChannelListViewModel,
    sourceId: Long,
    onChannelClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    LaunchedEffect(sourceId) {
        viewModel.setSource(sourceId)
    }

    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        viewModel.searchQuery.value = query
    }

    val favoritesOnly by viewModel.favoritesOnly.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val groupedChannels by viewModel.groupedChannels.collectAsState()
    val defaultChannelId by viewModel.defaultChannelId.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()

    var dialogChannel by remember { mutableStateOf<Channel?>(null) }
    val liveDialogChannel = dialogChannel?.let { stored ->
        allChannels.firstOrNull { it.id == stored.id } ?: stored
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(8.dp),
            )

            Button(
                onClick = { viewModel.favoritesOnly.value = !favoritesOnly },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(text = if (favoritesOnly) "Favoritos: ON" else "Favoritos")
            }

            Button(
                onClick = { viewModel.refresh() },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                if (isRefreshing) {
                    Text(text = "Atualizando...")
                } else {
                    Text(text = "Atualizar")
                }
            }

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(text = "Configuracoes")
            }
        }

        if (groupedChannels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Nenhum canal visivel. Importe uma fonte ou remova os filtros.")
            }
        } else {
            TvLazyColumn(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {
                groupedChannels.forEach { (group, channelsInGroup) ->
                    item(key = "header_$group") {
                        Text(text = group, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    item(key = "row_$group") {
                        TvLazyRow {
                            items(channelsInGroup, key = { ch: Channel -> ch.id }) { ch: Channel ->
                                ChannelCard(
                                    channel = ch,
                                    isDefault = ch.id == defaultChannelId,
                                    onClick = { onChannelClick(ch.id) },
                                    onLongPress = { dialogChannel = ch },
                                    modifier = Modifier.padding(end = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    liveDialogChannel?.let { ch ->
        EditChannelDialog(
            channel = ch,
            isDefault = ch.id == defaultChannelId,
            onDismiss = { dialogChannel = null },
            onPlay = {
                dialogChannel = null
                onChannelClick(ch.id)
            },
            onToggleFavorite = { viewModel.toggleFavorite(ch) },
            onToggleHidden = { viewModel.toggleHidden(ch) },
            onRename = { newName ->
                viewModel.rename(ch, newName)
                dialogChannel = null
            },
            onRegroup = { newGroup ->
                viewModel.regroup(ch, newGroup)
                dialogChannel = null
            },
            onMoveUp = { viewModel.moveUp(ch) },
            onMoveDown = { viewModel.moveDown(ch) },
            onSetDefault = { viewModel.setAsDefault(ch) },
            onClearDefault = { viewModel.clearDefault() },
        )
    }
}
