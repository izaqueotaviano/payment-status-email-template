package com.iptvtv.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.iptvtv.player.ui.theme.BrandGradient
import com.iptvtv.player.ui.theme.BrandMuted
import com.iptvtv.player.ui.theme.BrandOnSurface
import com.iptvtv.player.ui.theme.BrandOutline
import com.iptvtv.player.ui.theme.BrandPrimary
import com.iptvtv.player.ui.theme.BrandSurface
import com.iptvtv.player.ui.theme.BrandSurfaceVariant

/**
 * Runs [onClick] when the D-pad center or Enter key is released on the focused element.
 *
 * Must sit above `focusable()` in a modifier chain so the bubbling key event reaches it.
 */
fun Modifier.clickOnSelect(onClick: () -> Unit): Modifier = onKeyEvent { event ->
    if (event.key != Key.DirectionCenter && event.key != Key.Enter) {
        return@onKeyEvent false
    }
    if (event.type == KeyEventType.KeyUp) {
        onClick()
    }
    true
}

/** Rounded action button: a gradient fill when [primary], an outlined pill otherwise. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(percent = 50)

    val background = when {
        primary -> Modifier.background(BrandGradient, shape)
        isFocused -> Modifier.background(BrandPrimary, shape)
        else -> Modifier.background(BrandSurfaceVariant, shape)
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .scale(if (isFocused && enabled) 1.04f else 1f)
            .then(background)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else BrandOutline,
                shape = shape,
            )
            .clickOnSelect { if (enabled) onClick() }
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary || isFocused) Color.White else BrandOnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Compact filter chip used for categories and quick toggles. */
@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(percent = 50)

    val background = if (selected) {
        Modifier.background(BrandGradient, shape)
    } else {
        Modifier.background(if (isFocused) BrandSurfaceVariant else BrandSurface, shape)
    }

    Box(
        modifier = modifier
            .scale(if (isFocused) 1.04f else 1f)
            .then(background)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else BrandOutline,
                shape = shape,
            )
            .clickOnSelect(onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected || isFocused) Color.White else BrandMuted,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Pill-shaped search input with a placeholder and a violet focus ring. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(percent = 50)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = BrandOnSurface, fontSize = 15.sp),
        cursorBrush = SolidColor(BrandPrimary),
        interactionSource = interactionSource,
        modifier = modifier
            .background(BrandSurface, shape)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) BrandPrimary else BrandOutline,
                shape = shape,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchIcon(color = BrandMuted, modifier = Modifier.size(16.dp))
                Box(modifier = Modifier.padding(start = 10.dp).fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = BrandMuted, fontSize = 15.sp)
                    }
                    innerTextField()
                }
            }
        },
    )
}

/** Rounded translucent panel that groups related content. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(BrandSurface.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
            .border(1.dp, BrandOutline, RoundedCornerShape(20.dp))
            .padding(24.dp),
    ) {
        content()
    }
}
