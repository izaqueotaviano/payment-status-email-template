package com.iptvtv.player.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.iptvtv.player.BuildConfig
import com.iptvtv.player.ui.components.AppBackground
import com.iptvtv.player.ui.components.PillButton
import com.iptvtv.player.ui.components.SectionCard
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onManageSources: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val defaultChannelName by viewModel.defaultChannelName.collectAsState()
    val isClearingCache by viewModel.isClearingCache.collectAsState()

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ajustes",
                        color = BrandOnSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Preferências do iztv neste aparelho",
                        color = BrandMuted,
                        fontSize = 14.sp,
                    )
                }
                PillButton(text = "Voltar", onClick = onBack)
            }

            SectionCard(modifier = Modifier.width(760.dp)) {
                Column {
                    SettingsHeading(title = "Fontes", subtitle = "Listas M3U, Xtream Codes e arquivos locais")
                    PillButton(
                        text = "Gerenciar fontes",
                        onClick = onManageSources,
                        primary = true,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }

            SectionCard(modifier = Modifier.width(760.dp)) {
                Column {
                    SettingsHeading(
                        title = "Canal padrão",
                        subtitle = "Abre direto neste canal ao iniciar o app",
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    ) {
                        Text(
                            text = defaultChannelName ?: "Nenhum canal definido",
                            color = BrandOnSurface,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (defaultChannelName != null) {
                            PillButton(text = "Remover", onClick = { viewModel.clearDefaultChannel() })
                        }
                    }
                }
            }

            SectionCard(modifier = Modifier.width(760.dp)) {
                Column {
                    SettingsHeading(
                        title = "Armazenamento",
                        subtitle = "Limpa imagens e dados temporários do player",
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 14.dp),
                    ) {
                        PillButton(
                            text = if (isClearingCache) "Limpando..." else "Limpar cache",
                            onClick = { viewModel.clearCache() },
                            enabled = !isClearingCache,
                        )
                    }
                }
            }

            SectionCard(modifier = Modifier.width(760.dp)) {
                Column {
                    SettingsHeading(title = "Sobre", subtitle = "iztv · player de IPTV para Android TV")
                    Text(
                        text = "Versão ${BuildConfig.VERSION_NAME}",
                        color = BrandOnSurface,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeading(title: String, subtitle: String) {
    Column {
        Text(text = title, color = BrandOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = BrandMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
