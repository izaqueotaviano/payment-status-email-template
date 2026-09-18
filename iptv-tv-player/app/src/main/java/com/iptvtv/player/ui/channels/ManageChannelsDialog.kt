package com.iptvtv.player.ui.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import com.iptvtv.player.ui.components.PillButton
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandOutline
import com.iptvtv.player.ui.theme.BrandSurface
import kotlinx.coroutines.delay

/**
 * Bulk actions for trimming a playlist down. Providers hand out thousands of channels and people
 * watch a handful, so hiding them one by one is not a real option - and hiding has to be
 * reversible, which is what the hidden-channel toggle is for.
 */
@Composable
fun ManageChannelsDialog(
    favoriteCount: Int,
    hiddenCount: Int,
    showHidden: Boolean,
    onToggleShowHidden: () -> Unit,
    onKeepOnlyFavorites: () -> Unit,
    onShowAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val firstAction = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstAction.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .width(470.dp)
                .background(BrandSurface, shape)
                .border(1.dp, BrandOutline, shape)
                .padding(26.dp),
        ) {
            Text(
                text = "Gerenciar canais",
                color = BrandOnSurface,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$favoriteCount favoritos · $hiddenCount ocultos",
                color = BrandMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            Text(
                text = "Favorite os canais que você usa (segure OK na lista) e depois esconda o resto.",
                color = BrandMuted,
                fontSize = 13.sp,
            )
            PillButton(
                text = "Manter só os favoritos",
                onClick = onKeepOnlyFavorites,
                primary = true,
                enabled = favoriteCount > 0,
                modifier = Modifier.fillMaxWidth().focusRequester(firstAction),
            )

            PillButton(
                text = if (showHidden) "Parar de mostrar ocultos" else "Mostrar canais ocultos",
                onClick = onToggleShowHidden,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
            PillButton(
                text = "Reexibir todos os canais",
                onClick = onShowAll,
                enabled = hiddenCount > 0,
                modifier = Modifier.fillMaxWidth(),
            )

            PillButton(
                text = "Fechar",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}
