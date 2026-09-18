package com.iptvtv.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
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
            // Stays focusable while disabled on purpose: a button that drops out of the focus
            // graph mid-press (e.g. "Atualizar" while syncing) leaves the screen with no focus
            // owner and the remote does nothing until the flag flips back.
            .focusable(interactionSource = interactionSource)
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

/**
 * Moves focus instead of letting a text field swallow the D-pad.
 *
 * Compose text fields bind the arrow keys to caret movement and consume them, which on a remote
 * means focus can never leave the field. On TV the caret is handled by the on-screen keyboard
 * while it is open, so the arrows are free to navigate.
 */
fun Modifier.dpadFocusEscape(focusManager: FocusManager): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) {
        return@onPreviewKeyEvent false
    }
    val direction = when (event.key) {
        Key.DirectionLeft -> FocusDirection.Left
        Key.DirectionRight -> FocusDirection.Right
        Key.DirectionUp -> FocusDirection.Up
        Key.DirectionDown -> FocusDirection.Down
        else -> null
    } ?: return@onPreviewKeyEvent false
    focusManager.moveFocus(direction)
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
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(percent = 50)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = BrandOnSurface, fontSize = 15.sp),
        cursorBrush = SolidColor(BrandPrimary),
        interactionSource = interactionSource,
        modifier = modifier
            .dpadFocusEscape(focusManager)
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

/**
 * Indeterminate progress bar: a gradient block sweeping across a track.
 *
 * Driven by [withFrameMillis] rather than the animation APIs so it costs nothing beyond the
 * frames it draws, and an import of unknown length still looks alive.
 */
@Composable
fun ProgressBar(modifier: Modifier = Modifier) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var firstFrame = 0L
        while (true) {
            withFrameMillis { frame ->
                if (firstFrame == 0L) firstFrame = frame
                phase = ((frame - firstFrame) % SWEEP_MS) / SWEEP_MS.toFloat()
            }
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(4.dp)) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = BrandSurfaceVariant, cornerRadius = radius)
        val blockWidth = size.width * 0.28f
        drawRoundRect(
            brush = BrandGradient,
            topLeft = Offset(-blockWidth + phase * (size.width + blockWidth), 0f),
            size = Size(blockWidth, size.height),
            cornerRadius = radius,
        )
    }
}

private const val SWEEP_MS = 1400L

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
