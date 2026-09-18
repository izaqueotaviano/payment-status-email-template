package com.iptvtv.player.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.iptvtv.player.BuildConfig

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onManageSources: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val defaultChannelName by viewModel.defaultChannelName.collectAsState()
    val isClearingCache by viewModel.isClearingCache.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(text = "Configurações", style = MaterialTheme.typography.headlineMedium)

            Button(onClick = onManageSources) {
                Text(text = "Gerenciar fontes")
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Canal padrão", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = defaultChannelName ?: "Nenhum")
                    if (defaultChannelName != null) {
                        Button(onClick = { viewModel.clearDefaultChannel() }) {
                            Text(text = "Remover canal padrão")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Armazenamento", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(
                        onClick = { viewModel.clearCache() },
                    ) {
                        Text(text = "Limpar cache")
                    }
                    if (isClearingCache) {
                        Text(text = "Limpando...")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Sobre", style = MaterialTheme.typography.titleMedium)
                Text(text = "iztv")
                Text(text = "Versão ${BuildConfig.VERSION_NAME}")
            }
        }
    }
}
