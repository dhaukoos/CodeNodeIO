/*
 * CollapsiblePanel - Reusable collapsible wrapper for side panels
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Which side of the layout the panel is on.
 * Determines toggle strip placement and chevron direction.
 */
enum class PanelSide {
    /** Panel on the left side — toggle strip on right edge */
    LEFT,
    /** Panel on the right side — toggle strip on left edge */
    RIGHT
}

/**
 * Reusable collapsible panel wrapper.
 *
 * Renders a narrow toggle strip (divider + chevron icon) that is always visible,
 * and conditionally shows the panel content based on [isExpanded].
 *
 * @param isExpanded Whether the panel content is currently visible
 * @param onToggle Callback invoked when the toggle strip is clicked
 * @param side Which side of the layout this panel is on (determines chevron direction)
 * @param modifier Modifier for the outer container
 * @param content The panel content to show when expanded
 */
@Composable
fun CollapsiblePanel(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    side: PanelSide,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(modifier = modifier) {
        if (side == PanelSide.RIGHT) {
            // Right-side panel: toggle strip on the left edge
            ToggleStrip(
                isExpanded = isExpanded,
                onToggle = onToggle,
                expandedIcon = Icons.Default.ChevronRight,
                collapsedIcon = Icons.Default.ChevronLeft,
                expandedDescription = "Collapse panel",
                collapsedDescription = "Expand panel"
            )
        }

        if (isExpanded) {
            if (side == PanelSide.LEFT) {
                content()
                Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
            } else {
                Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
                content()
            }
        }

        if (side == PanelSide.LEFT) {
            // Left-side panel: toggle strip on the right edge
            ToggleStrip(
                isExpanded = isExpanded,
                onToggle = onToggle,
                expandedIcon = Icons.Default.ChevronLeft,
                collapsedIcon = Icons.Default.ChevronRight,
                expandedDescription = "Collapse panel",
                collapsedDescription = "Expand panel"
            )
        }
    }
}

@Composable
private fun ToggleStrip(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    expandedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    collapsedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    expandedDescription: String,
    collapsedDescription: String
) {
    Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(20.dp)
            .clickable(onClick = onToggle)
            .background(MaterialTheme.colors.surface),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isExpanded) expandedIcon else collapsedIcon,
            contentDescription = if (isExpanded) expandedDescription else collapsedDescription,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}
