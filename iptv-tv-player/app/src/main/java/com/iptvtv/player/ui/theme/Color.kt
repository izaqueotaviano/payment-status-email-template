package com.iptvtv.player.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BrandBackground = Color(0xFF0A0910)
val BrandSurface = Color(0xFF16141F)
val BrandSurfaceVariant = Color(0xFF211D2E)
val BrandOutline = Color(0xFF322C46)

val BrandPrimary = Color(0xFF9B5CF6)
val BrandPrimaryDeep = Color(0xFF6D28D9)
val BrandAccent = Color(0xFFE879C8)

val BrandOnSurface = Color(0xFFF4F1FB)
val BrandMuted = Color(0xFF9A93AE)
val BrandError = Color(0xFFFF6B81)

/** Violet to magenta sweep used on primary actions and the active navigation marker. */
val BrandGradient = Brush.horizontalGradient(listOf(BrandPrimaryDeep, BrandPrimary, BrandAccent))

/** Soft highlight that trails off to the right, used behind the focused row of a list. */
val FocusRowGradient = Brush.horizontalGradient(
    listOf(BrandPrimary.copy(alpha = 0.45f), BrandPrimary.copy(alpha = 0.10f), Color.Transparent),
)
