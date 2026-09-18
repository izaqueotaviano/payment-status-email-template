package com.iptvtv.player.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.iptvtv.player.ui.theme.BrandGradient
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandOutline
import com.iptvtv.player.ui.theme.BrandPrimary

/** The sections reachable from the side rail. */
enum class NavSection { Channels, Favorites, Sources, Settings }

/**
 * Persistent left navigation rail. [selected] marks the current screen, while [favoritesActive]
 * also lights up the favorites entry while that filter is applied.
 */
@Composable
fun NavRail(
    selected: NavSection,
    onSelect: (NavSection) -> Unit,
    modifier: Modifier = Modifier,
    favoritesActive: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(196.dp)
            .background(Color(0xCC121020))
            .padding(horizontal = 16.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, bottom = 26.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(BrandGradient, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                PlayIcon(color = Color.White, modifier = Modifier.size(11.dp))
            }
            Text(
                text = "iztv",
                color = BrandOnSurface,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        NavRailItem(
            label = "Canais",
            selected = selected == NavSection.Channels,
            onClick = { onSelect(NavSection.Channels) },
            icon = { color, iconModifier -> GridIcon(color = color, modifier = iconModifier) },
        )
        NavRailItem(
            label = "Favoritos",
            selected = selected == NavSection.Favorites || favoritesActive,
            onClick = { onSelect(NavSection.Favorites) },
            icon = { color, iconModifier -> StarIcon(color = color, modifier = iconModifier) },
        )
        NavRailItem(
            label = "Fontes",
            selected = selected == NavSection.Sources,
            onClick = { onSelect(NavSection.Sources) },
            icon = { color, iconModifier -> LayersIcon(color = color, modifier = iconModifier) },
        )
        NavRailItem(
            label = "Ajustes",
            selected = selected == NavSection.Settings,
            onClick = { onSelect(NavSection.Settings) },
            icon = { color, iconModifier -> SlidersIcon(color = color, modifier = iconModifier) },
        )
    }
}

@Composable
private fun NavRailItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color, Modifier) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val contentColor = when {
        isFocused -> Color.White
        selected -> BrandOnSurface
        else -> BrandMuted
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = when {
                    isFocused -> BrandPrimary
                    selected -> Color(0x335B21B6)
                    else -> Color.Transparent
                },
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = if (isFocused) Color.White else if (selected) BrandOutline else Color.Transparent,
                shape = shape,
            )
            .clickOnSelect(onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        icon(contentColor, Modifier.size(17.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = if (selected || isFocused) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
