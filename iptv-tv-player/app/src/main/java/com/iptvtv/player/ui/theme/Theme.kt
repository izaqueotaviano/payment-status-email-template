package com.iptvtv.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val AppColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    secondary = BrandAccent,
    background = BrandBackground,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandMuted,
    error = BrandError,
)

/** App-wide theme built on Compose for TV's material3, tuned for a dark full-screen TV UI. */
@Composable
fun IptvTvPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content,
    )
}
