/*
 * NavigationBreadcrumb - Breadcrumb navigation for GraphNode hierarchy
 * Shows the current navigation path and allows clicking to navigate back
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.grapheditor.state.NavigationContext

/**
 * Represents a segment in the breadcrumb trail
 */
data class BreadcrumbSegment(
    val id: String?,      // null for root
    val name: String,
    val depth: Int        // 0 for root, 1 for first level, etc.
)

/**
 * Navigation breadcrumb that displays the current path through nested GraphNodes.
 *
 * Shows a trail like: Root > GroupA > SubGroup > ...
 * Each segment (except the last) is clickable to navigate back to that level.
 *
 * @param navigationContext The current navigation context containing the path
 * @param graphNodeNames Map of GraphNode IDs to their display names
 * @param onNavigateToRoot Callback when "Root" is clicked
 * @param onNavigateToLevel Callback when a breadcrumb segment is clicked, with the target depth
 * @param textColor Color for the breadcrumb text
 * @param separatorColor Color for the ">" separators
 * @param modifier Modifier for the breadcrumb container
 */
@Composable
fun NavigationBreadcrumb(
    navigationContext: NavigationContext,
    graphNodeNames: Map<String, String> = emptyMap(),
    onNavigateToRoot: () -> Unit = {},
    onNavigateToLevel: (depth: Int) -> Unit = {},
    textColor: Color = Color.White,
    separatorColor: Color = Color.White.copy(alpha = 0.7f),
    modifier: Modifier = Modifier
) {
    // Build breadcrumb segments from navigation path
    val segments = buildBreadcrumbSegments(navigationContext, graphNodeNames)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.forEachIndexed { index, segment ->
            val isLast = index == segments.lastIndex
            val isClickable = !isLast && segments.size > 1

            // Breadcrumb segment text
            Text(
                text = segment.name,
                fontSize = 14.sp,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                color = if (isClickable) textColor else textColor.copy(alpha = 0.9f),
                modifier = if (isClickable) {
                    Modifier.clickable {
                        if (segment.id == null) {
                            onNavigateToRoot()
                        } else {
                            onNavigateToLevel(segment.depth)
                        }
                    }
                } else {
                    Modifier
                }
            )

            // Separator (except after last segment)
            if (!isLast) {
                Text(
                    text = " > ",
                    fontSize = 14.sp,
                    color = separatorColor
                )
            }
        }
    }
}

/**
 * Compact version of the breadcrumb that shows only the current level with a back arrow.
 * Used in space-constrained areas like the toolbar.
 *
 * @param isInsideGraphNode Whether we're inside a GraphNode (not at root)
 * @param currentGraphNodeName Name of the current GraphNode (if inside one)
 * @param onNavigateBack Callback when the back button is clicked
 * @param textColor Color for the text
 * @param modifier Modifier for the component
 */
@Composable
fun CompactNavigationBreadcrumb(
    isInsideGraphNode: Boolean,
    currentGraphNodeName: String?,
    onNavigateBack: () -> Unit = {},
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isInsideGraphNode) {
            // Back button
            TextButton(
                onClick = onNavigateBack,
                colors = ButtonDefaults.textButtonColors(contentColor = textColor),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "\u2190",  // Left arrow
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Current location indicator
            Text(
                text = "Inside: ${currentGraphNodeName ?: "GraphNode"}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

/**
 * Full navigation breadcrumb bar that shows the complete path.
 * Can be placed below the main toolbar for detailed navigation.
 *
 * @param navigationContext The current navigation context
 * @param graphNodeNames Map of GraphNode IDs to their display names
 * @param onNavigateToRoot Callback when "Root" is clicked
 * @param onNavigateToLevel Callback when a level is clicked (depth 0 = after first GraphNode, etc.)
 * @param modifier Modifier for the bar
 */
@Composable
fun NavigationBreadcrumbBar(
    navigationContext: NavigationContext,
    graphNodeNames: Map<String, String> = emptyMap(),
    onNavigateToRoot: () -> Unit = {},
    onNavigateToLevel: (depth: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Only show if not at root
    if (!navigationContext.isAtRoot) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = Color(0xFF1976D2),  // Slightly darker blue than toolbar
            elevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Path: ",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                NavigationBreadcrumb(
                    navigationContext = navigationContext,
                    graphNodeNames = graphNodeNames,
                    onNavigateToRoot = onNavigateToRoot,
                    onNavigateToLevel = onNavigateToLevel,
                    textColor = Color.White
                )
            }
        }
    }
}

/**
 * Builds a list of breadcrumb segments from the navigation context.
 */
private fun buildBreadcrumbSegments(
    navigationContext: NavigationContext,
    graphNodeNames: Map<String, String>
): List<BreadcrumbSegment> {
    val segments = mutableListOf<BreadcrumbSegment>()

    // Always start with Root
    segments.add(BreadcrumbSegment(id = null, name = "Root", depth = 0))

    // Add each level in the path
    navigationContext.path.forEachIndexed { index, nodeId ->
        val name = graphNodeNames[nodeId] ?: nodeId
        segments.add(BreadcrumbSegment(id = nodeId, name = name, depth = index + 1))
    }

    return segments
}
