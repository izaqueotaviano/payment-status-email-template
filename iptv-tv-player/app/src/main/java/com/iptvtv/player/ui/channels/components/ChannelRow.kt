package com.iptvtv.player.ui.channels.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvtv.player.domain.model.Channel
import com.iptvtv.player.ui.components.StarIcon
import com.iptvtv.player.ui.theme.BrandAccent
import com.iptvtv.player.ui.theme.BrandGradient
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandPrimary
import com.iptvtv.player.ui.theme.BrandSurfaceVariant
import com.iptvtv.player.ui.theme.FocusRowGradient

private const val LONG_PRESS_THRESHOLD_MS = 500L

/**
 * One line of the channel guide: position, logo, name and category.
 *
 * A short press plays the channel, a long press opens the edit dialog, and focus drives the
 * preview panel next to the list.
 */
@Composable
fun ChannelRow(
    channel: Channel,
    position: Int,
    isDefault: Boolean,
    isPreviewed: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)

    val rowBackground = when {
        isFocused -> Modifier.background(FocusRowGradient, shape)
        isPreviewed -> Modifier.background(Color(0x14FFFFFF), shape)
        else -> Modifier
    }
    val accentBrush: Brush = if (isFocused) BrandGradient else SolidColor(Color.Transparent)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .then(rowBackground)
            .onFocusChanged { state -> if (state.isFocused) onFocused() }
            .onKeyEvent { event ->
                if (event.key != Key.DirectionCenter && event.key != Key.Enter) {
                    return@onKeyEvent false
                }
                if (event.type == KeyEventType.KeyUp) {
                    val pressDurationMs = event.nativeKeyEvent.eventTime - event.nativeKeyEvent.downTime
                    if (pressDurationMs >= LONG_PRESS_THRESHOLD_MS) onLongPress() else onClick()
                }
                true
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(30.dp)
                .background(accentBrush, RoundedCornerShape(2.dp)),
        )

        Text(
            text = (position + 1).toString().padStart(3, '0'),
            color = if (isFocused) Color.White else BrandMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 10.dp).width(28.dp),
        )

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(width = 46.dp, height = 32.dp)
                .background(BrandSurfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(5.dp),
                )
            } else {
                Text(
                    text = channel.displayName.take(2).uppercase(),
                    color = BrandMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                text = channel.displayName,
                color = if (isFocused) Color.White else BrandOnSurface,
                fontSize = 15.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = channel.displayGroup,
                color = BrandMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (channel.isHidden) {
            Text(
                text = "OCULTO",
                color = BrandMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (isDefault) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(6.dp)
                    .background(BrandPrimary, RoundedCornerShape(3.dp)),
            )
        }

        if (channel.isFavorite) {
            StarIcon(color = BrandAccent, modifier = Modifier.padding(start = 8.dp).size(13.dp))
        }
    }
}
