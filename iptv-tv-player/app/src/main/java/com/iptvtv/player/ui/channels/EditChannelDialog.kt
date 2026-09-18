package com.iptvtv.player.ui.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.ui.components.PillButton
import com.iptvtv.player.ui.sources.components.LabeledTextField
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandOutline
import com.iptvtv.player.ui.theme.BrandSurface

@Composable
fun EditChannelDialog(
    channel: Channel,
    isDefault: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onRename: (String) -> Unit,
    onRegroup: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSetDefault: () -> Unit,
    onClearDefault: () -> Unit,
) {
    var nameText by remember(channel.id) { mutableStateOf(channel.displayName) }
    var groupText by remember(channel.id) { mutableStateOf(channel.displayGroup) }
    val shape = RoundedCornerShape(24.dp)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(470.dp)
                .background(BrandSurface, shape)
                .border(1.dp, BrandOutline, shape)
                .padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = channel.displayName,
                    color = BrandOnSurface,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = channel.displayGroup, color = BrandMuted, fontSize = 13.sp)
            }

            PillButton(
                text = "Assistir",
                onClick = onPlay,
                primary = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PillButton(
                    text = if (channel.isFavorite) "Remover favorito" else "Favoritar",
                    onClick = onToggleFavorite,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = if (channel.isHidden) "Mostrar" else "Esconder",
                    onClick = onToggleHidden,
                    modifier = Modifier.weight(1f),
                )
            }

            LabeledTextField(label = "Nome", value = nameText, onValueChange = { nameText = it })
            PillButton(
                text = "Salvar nome",
                onClick = { onRename(nameText) },
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledTextField(label = "Categoria", value = groupText, onValueChange = { groupText = it })
            PillButton(
                text = "Salvar categoria",
                onClick = { onRegroup(groupText) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PillButton(text = "Subir", onClick = onMoveUp, modifier = Modifier.weight(1f))
                PillButton(text = "Descer", onClick = onMoveDown, modifier = Modifier.weight(1f))
            }

            PillButton(
                text = if (isDefault) "Remover canal padrão" else "Definir como canal padrão",
                onClick = if (isDefault) onClearDefault else onSetDefault,
                modifier = Modifier.fillMaxWidth(),
            )

            PillButton(text = "Fechar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
