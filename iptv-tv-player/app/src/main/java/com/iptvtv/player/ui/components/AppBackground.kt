package com.iptvtv.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.iptvtv.player.ui.theme.BrandBackground

private val HazeGradient = Brush.verticalGradient(
    listOf(Color(0xFF1C1230), BrandBackground, Color(0xFF0D0A16)),
)

private val GlowGradient = Brush.linearGradient(
    listOf(Color(0x4D7C3AED), Color(0x00000000), Color(0x33D946EF)),
)

/** Dark violet backdrop shared by every screen: a vertical haze plus a diagonal color glow. */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(BrandBackground)) {
        Box(modifier = Modifier.fillMaxSize().background(HazeGradient))
        Box(modifier = Modifier.fillMaxSize().background(GlowGradient))
        content()
    }
}
