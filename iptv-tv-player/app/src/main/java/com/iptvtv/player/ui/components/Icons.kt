package com.iptvtv.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The icon set is drawn with [Canvas] primitives rather than glyphs or vector assets so it
 * renders identically on every TV device regardless of the system font.
 */

/** Four rounded tiles - the channel grid. */
@Composable
fun GridIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gap = size.minDimension * 0.18f
        val cell = (size.minDimension - gap) / 2f
        val radius = CornerRadius(cell * 0.32f, cell * 0.32f)
        val origins = listOf(
            Offset(0f, 0f),
            Offset(cell + gap, 0f),
            Offset(0f, cell + gap),
            Offset(cell + gap, cell + gap),
        )
        origins.forEach { origin ->
            drawRoundRect(color = color, topLeft = origin, size = Size(cell, cell), cornerRadius = radius)
        }
    }
}

/** Five-pointed star, filled or outlined. */
@Composable
fun StarIcon(color: Color, modifier: Modifier = Modifier, filled: Boolean = true) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outer = size.minDimension / 2f
        val inner = outer * 0.45f
        val path = Path()
        for (point in 0 until 10) {
            val radius = if (point % 2 == 0) outer else inner
            val angle = ((-90.0 + point * 36.0) * PI / 180.0).toFloat()
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path = path,
            color = color,
            style = if (filled) Fill else Stroke(width = size.minDimension * 0.12f),
        )
    }
}

/** Stacked sheets - the playlist sources. */
@Composable
fun LayersIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val height = size.height * 0.2f
        val gap = size.height * 0.12f
        val widths = listOf(size.width, size.width * 0.78f, size.width * 0.56f)
        widths.forEachIndexed { index, width ->
            drawRoundRect(
                color = if (index == 0) color else color.copy(alpha = 0.7f),
                topLeft = Offset((size.width - width) / 2f, index * (height + gap)),
                size = Size(width, height),
                cornerRadius = CornerRadius(height / 2f, height / 2f),
            )
        }
    }
}

/** Three sliders - the settings entry. */
@Composable
fun SlidersIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val rows = 3
        val spacing = size.height / (rows + 1)
        val knobRadius = size.height * 0.11f
        val knobPositions = listOf(0.68f, 0.34f, 0.55f)
        for (row in 0 until rows) {
            val y = spacing * (row + 1)
            drawLine(
                color = color.copy(alpha = 0.55f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = size.height * 0.07f,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = color,
                radius = knobRadius,
                center = Offset(size.width * knobPositions[row], y),
            )
        }
    }
}

/** Solid play triangle. */
@Composable
fun PlayIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path()
        path.moveTo(size.width * 0.12f, 0f)
        path.lineTo(size.width, size.height / 2f)
        path.lineTo(size.width * 0.12f, size.height)
        path.close()
        drawPath(path = path, color = color, style = Fill)
    }
}

/** Magnifying glass for the search field. */
@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.12f
        val radius = size.minDimension * 0.32f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = stroke))
        drawLine(
            color = color,
            start = Offset(center.x + radius * 0.75f, center.y + radius * 0.75f),
            end = Offset(size.width * 0.94f, size.height * 0.94f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
