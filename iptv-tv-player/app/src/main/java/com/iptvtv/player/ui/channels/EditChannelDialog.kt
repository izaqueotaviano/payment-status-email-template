package com.iptvtv.player.ui.channels

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.iptvtv.player.domain.model.Channel

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(420.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = channel.displayName)

                Button(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(text = "Assistir")
                }

                Button(
                    onClick = onToggleFavorite,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(text = if (channel.isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos")
                }

                Button(
                    onClick = onToggleHidden,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(text = if (channel.isHidden) "Mostrar canal" else "Esconder canal")
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    BasicTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .padding(8.dp),
                    )
                }
                Button(
                    onClick = { onRename(nameText) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(text = "Salvar nome")
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    BasicTextField(
                        value = groupText,
                        onValueChange = { groupText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .padding(8.dp),
                    )
                }
                Button(
                    onClick = { onRegroup(groupText) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(text = "Salvar categoria")
                }

                Button(
                    onClick = onMoveUp,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(text = "Mover para cima")
                }

                Button(
                    onClick = onMoveDown,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(text = "Mover para baixo")
                }

                Button(
                    onClick = if (isDefault) onClearDefault else onSetDefault,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(text = if (isDefault) "Remover canal padrao" else "Definir como canal padrao")
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text(text = "Fechar")
                }
            }
        }
    }
}
