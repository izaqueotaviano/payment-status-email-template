package com.iptvtv.player.ui.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import kotlinx.coroutines.delay

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
    val playFocus = remember { FocusRequester() }

    // A dialog opens with nothing focused inside it, so the remote would have no target.
    LaunchedEffect(channel.id) {
        delay(120)
        runCatching { playFocus.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(470.dp)
                // The full action list is taller than a 540dp TV screen: without a bounded,
                // scrollable body a Column measures the last children at zero height and the
                // bottom buttons silently vanish.
                .heightIn(max = 440.dp)
                .background(BrandSurface, shape)
                .border(1.dp, BrandOutline, shape)
                .verticalScroll(rememberScrollState())
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
                modifier = Modifier.fillMaxWidth().focusRequester(playFocus),
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
