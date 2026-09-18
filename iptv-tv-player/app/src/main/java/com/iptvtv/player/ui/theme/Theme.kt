package com.iptvtv.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val AppColorScheme = darkColorScheme(
    primary = BrandPrimary,
    background = BrandBackground,
    surface = BrandSurface,
    onBackground = BrandOnSurface,
    onSurface = BrandOnSurface,
)

/** App-wide theme built on Compose for TV's material3, tuned for a dark full-screen TV UI. */
@Composable
fun IptvTvPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content,
    )
}
