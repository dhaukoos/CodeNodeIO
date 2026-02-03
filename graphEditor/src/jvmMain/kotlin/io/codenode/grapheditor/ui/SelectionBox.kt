/*
 * SelectionBox - Rectangular Selection Visual Component
 * Renders a dotted rectangle during Shift-drag selection
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Renders a dotted rectangular selection box during Shift-drag selection.
 *
 * @param bounds The bounding rectangle to draw (already normalized with min/max)
 * @param modifier Modifier for the canvas
 */
@Composable
fun SelectionBox(
    bounds: Rect,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw semi-transparent fill
        drawRect(
            color = Color(0xFF2196F3).copy(alpha = 0.1f),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height)
        )

        // Draw dotted border
        drawRect(
            color = Color(0xFF2196F3),
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height),
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
            )
        )
    }
}
