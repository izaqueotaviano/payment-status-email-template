package com.iptvtv.player.ui.sources

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Button
import androidx.tv.material3.CircularProgressIndicator
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.iptvtv.player.domain.model.Source

private fun sourceTypeLabel(source: Source): String = when (source) {
    is Source.M3uUrlSource -> "Lista M3U"
    is Source.XtreamSource -> "Xtream Codes"
    is Source.LocalFileSource -> "Arquivo local"
}

@Composable
fun SourcesScreen(
    viewModel: SourcesViewModel,
    onAddSource: (sourceType: String) -> Unit,
    onEditSource: (sourceId: Long) -> Unit,
    onOpenChannelList: (sourceId: Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sources by viewModel.sources.collectAsState()
    val activeSourceId by viewModel.activeSourceId.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Fontes de canais", style = MaterialTheme.typography.headlineMedium)
                if (isSyncing) {
                    CircularProgressIndicator()
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onAddSource("m3u") }) { Text(text = "+ Lista M3U") }
                Button(onClick = { onAddSource("xtream") }) { Text(text = "+ Xtream Codes") }
                Button(onClick = { onAddSource("local") }) { Text(text = "+ Arquivo local") }
                Button(onClick = onOpenSettings) { Text(text = "Configurações") }
            }

            if (syncError != null) {
                Text(
                    text = "Erro ao sincronizar: $syncError",
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (sources.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Nenhuma fonte cadastrada ainda.")
                    Text(text = "Adicione uma lista M3U, uma conta Xtream Codes ou importe um arquivo local para começar.")
                }
            } else {
                TvLazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sources, key = { it.id }) { source ->
                        val isActive = activeSourceId == source.id
                        Surface(
                            onClick = {
                                viewModel.selectActive(source)
                                onOpenChannelList(source.id)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isActive) {
                                        Modifier.border(
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                            RoundedCornerShape(8.dp),
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                    Text(
                                        text = (if (isActive) "▶ " else "") + source.name,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(text = sourceTypeLabel(source), style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = { onEditSource(source.id) }) {
                                    Text(text = "Editar")
                                }
                                Button(onClick = { viewModel.deleteSource(source.id) }) {
                                    Text(text = "Excluir")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
