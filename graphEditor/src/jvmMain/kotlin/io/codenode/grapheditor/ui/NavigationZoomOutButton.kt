/*
 * NavigationZoomOutButton - Floating button for navigating out of GraphNode
 * Displays when inside a GraphNode to allow returning to parent level
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Floating zoom-out button that appears when inside a GraphNode.
 * Clicking it navigates back to the parent level.
 *
 * @param enabled Whether the button is enabled (false when at root level)
 * @param currentGraphNodeName Name of the current GraphNode (shown as tooltip/label)
 * @param onClick Callback when the button is clicked
 * @param modifier Modifier for the button
 */
@Composable
fun NavigationZoomOutButton(
    enabled: Boolean,
    currentGraphNodeName: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only show when enabled (inside a GraphNode)
    if (!enabled) return

    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1976D2))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Back arrow icon
        Text(
            text = "\u2190",  // Left arrow Unicode
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Label
        Text(
            text = "Exit",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        // Optional: Show current location
        if (currentGraphNodeName != null) {
            Text(
                text = "($currentGraphNodeName)",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Compact circular zoom-out button for space-constrained areas.
 *
 * @param enabled Whether the button is enabled (false when at root level)
 * @param onClick Callback when the button is clicked
 * @param modifier Modifier for the button
 */
@Composable
fun CompactZoomOutButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only show when enabled (inside a GraphNode)
    if (!enabled) return

    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF1976D2))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u2190",  // Left arrow Unicode
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Navigation control bar that appears at the top of the canvas when inside a GraphNode.
 * Shows current location and provides navigation controls.
 *
 * @param isInsideGraphNode Whether we are inside a GraphNode (not at root)
 * @param currentGraphNodeName Name of the current GraphNode
 * @param navigationDepth Current depth in the navigation hierarchy
 * @param onNavigateOut Callback to navigate out one level
 * @param onNavigateToRoot Callback to navigate directly to root
 * @param modifier Modifier for the control bar
 */
@Composable
fun NavigationControlBar(
    isInsideGraphNode: Boolean,
    currentGraphNodeName: String?,
    navigationDepth: Int,
    onNavigateOut: () -> Unit,
    onNavigateToRoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Only show when inside a GraphNode
    if (!isInsideGraphNode) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1565C0),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Back button and location info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Back button
                TextButton(
                    onClick = onNavigateOut,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(
                        text = "\u2190 Back",
                        fontWeight = FontWeight.Medium
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )

                // Current location
                Column {
                    Text(
                        text = "Inside:",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = currentGraphNodeName ?: "GraphNode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Right side: Depth indicator and root button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Depth indicator
                Text(
                    text = "Depth: $navigationDepth",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // Go to root button (only show if depth > 1)
                if (navigationDepth > 1) {
                    TextButton(
                        onClick = onNavigateToRoot,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text(
                            text = "\u21B0 Root",  // Return arrow
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
