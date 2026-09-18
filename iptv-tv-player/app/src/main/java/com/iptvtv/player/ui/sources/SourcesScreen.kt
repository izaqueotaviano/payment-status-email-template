package com.iptvtv.player.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.iptvtv.player.domain.model.Source
import com.iptvtv.player.ui.components.AppBackground
import com.iptvtv.player.ui.components.LayersIcon
import com.iptvtv.player.ui.components.NavRail
import com.iptvtv.player.ui.components.NavSection
import com.iptvtv.player.ui.components.PillButton
import com.iptvtv.player.ui.components.SectionCard
import com.iptvtv.player.ui.components.clickOnSelect
import com.iptvtv.player.ui.theme.BrandAccent
import com.iptvtv.player.ui.theme.BrandGradient
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandOutline
import com.iptvtv.player.ui.theme.BrandPrimary
import com.iptvtv.player.ui.theme.BrandSurface
import com.iptvtv.player.ui.theme.BrandSurfaceVariant

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

    AppBackground {
        Row(modifier = Modifier.fillMaxSize()) {
            NavRail(
                selected = NavSection.Sources,
                onSelect = { section ->
                    when (section) {
                        NavSection.Channels, NavSection.Favorites -> {
                            val target = activeSourceId ?: sources.firstOrNull()?.id
                            if (target != null) onOpenChannelList(target)
                        }
                        NavSection.Sources -> Unit
                        NavSection.Settings -> onOpenSettings()
                    }
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 32.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column {
                    Text(
                        text = "Fontes de canais",
                        color = BrandOnSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Adicione listas e alterne entre elas a qualquer momento",
                        color = BrandMuted,
                        fontSize = 14.sp,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(text = "+ Lista M3U", onClick = { onAddSource("m3u") }, primary = true)
                    PillButton(text = "+ Xtream Codes", onClick = { onAddSource("xtream") })
                    PillButton(text = "+ Arquivo local", onClick = { onAddSource("local") })
                }

                if (sources.isEmpty()) {
                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Nenhuma fonte cadastrada ainda",
                                color = BrandOnSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Adicione uma lista M3U, uma conta Xtream Codes ou importe um arquivo local para começar.",
                                color = BrandMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sources, key = { source: Source -> source.id }) { source: Source ->
                            SourceCard(
                                source = source,
                                isActive = activeSourceId == source.id,
                                onOpen = {
                                    viewModel.selectActive(source)
                                    onOpenChannelList(source.id)
                                },
                                onEdit = { onEditSource(source.id) },
                                onDelete = { viewModel.deleteSource(source.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceCard(
    source: Source,
    isActive: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)
    val badgeBrush: Brush = if (isActive) BrandGradient else SolidColor(BrandSurfaceVariant)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isFocused) BrandSurfaceVariant else BrandSurface.copy(alpha = 0.8f), shape)
            .border(
                width = if (isFocused || isActive) 2.dp else 1.dp,
                color = when {
                    isFocused -> Color.White
                    isActive -> BrandPrimary
                    else -> BrandOutline
                },
                shape = shape,
            )
            .clickOnSelect(onOpen)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(badgeBrush, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            LayersIcon(
                color = if (isActive) Color.White else BrandMuted,
                modifier = Modifier.size(17.dp),
            )
        }

        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(
                text = source.name,
                color = BrandOnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = sourceTypeLabel(source), color = BrandMuted, fontSize = 13.sp)
                if (isActive) {
                    Text(
                        text = "· EM USO",
                        color = BrandAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }

        PillButton(text = "Editar", onClick = onEdit)
        PillButton(text = "Excluir", onClick = onDelete, modifier = Modifier.padding(start = 8.dp))
    }
}
