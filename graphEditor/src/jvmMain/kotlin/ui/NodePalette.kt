/*
 * NodePalette - Categorized Node Type Selection Panel
 * Provides drag-and-drop interface for adding nodes to the canvas
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codenode.fbpdsl.model.NodeTypeDefinition

/**
 * Node palette component for browsing and selecting node types
 *
 * @param nodeTypes List of available node type definitions
 * @param onNodeSelected Callback when a node type is selected for placement
 * @param modifier Modifier for the palette
 */
@Composable
fun NodePalette(
    nodeTypes: List<NodeTypeDefinition>,
    onNodeSelected: (NodeTypeDefinition) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedCategories by remember { mutableStateOf(setOf<NodeTypeDefinition.NodeCategory>()) }

    // Group node types by category
    val groupedNodes = nodeTypes
        .filter { nodeType ->
            if (searchQuery.isBlank()) return@filter true
            nodeType.name.contains(searchQuery, ignoreCase = true) ||
            nodeType.description?.contains(searchQuery, ignoreCase = true) == true
        }
        .groupBy { it.category }
        .toSortedMap()

    Column(
        modifier = modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(Color(0xFFFAFAFA))
            .border(1.dp, Color(0xFFE0E0E0))
    ) {
        // Header
        Text(
            text = "Node Palette",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )

        // Search box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search nodes...") },
            singleLine = true,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        )

        Divider(color = Color(0xFFE0E0E0))

        // Node list by category
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
        ) {
            groupedNodes.forEach { (category, nodes) ->
                item {
                    CategoryHeader(
                        category = category,
                        isExpanded = category in expandedCategories,
                        onToggle = {
                            expandedCategories = if (category in expandedCategories) {
                                expandedCategories - category
                            } else {
                                expandedCategories + category
                            }
                        }
                    )
                }

                if (category in expandedCategories) {
                    items(nodes) { nodeType ->
                        NodeTypeItem(
                            nodeType = nodeType,
                            onClick = { onNodeSelected(nodeType) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Category header with expand/collapse functionality
 */
@Composable
private fun CategoryHeader(
    category: NodeTypeDefinition.NodeCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isExpanded) "▼" else "▶",
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = category.displayName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF424242)
        )
    }
}

/**
 * Individual node type item in the palette
 */
@Composable
private fun NodeTypeItem(
    nodeType: NodeTypeDefinition,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = onClick)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false },
        shape = RoundedCornerShape(4.dp),
        backgroundColor = if (isHovered) Color(0xFFE3F2FD) else Color.White,
        elevation = if (isHovered) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = nodeType.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            nodeType.description?.let { desc ->
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = Color(0xFF757575),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Show port count summary
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (nodeType.defaultInputPorts.isNotEmpty()) {
                    PortBadge(
                        count = nodeType.defaultInputPorts.size,
                        label = "in",
                        color = Color(0xFF4CAF50)
                    )
                }

                if (nodeType.defaultOutputPorts.isNotEmpty()) {
                    PortBadge(
                        count = nodeType.defaultOutputPorts.size,
                        label = "out",
                        color = Color(0xFF2196F3)
                    )
                }
            }
        }
    }

    // Show tooltip on hover
    if (isHovered) {
        NodeTypeTooltip(nodeType)
    }
}

/**
 * Small badge showing port count
 */
@Composable
private fun PortBadge(
    count: Int,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = "$count $label",
            fontSize = 10.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Tooltip showing detailed node type information
 */
@Composable
private fun NodeTypeTooltip(nodeType: NodeTypeDefinition) {
    // Placeholder - full tooltip implementation would use Popup or custom positioning
    // For now, this is a visual marker that tooltips should appear
}

/**
 * Extension function for pointer hover events
 */
private fun Modifier.onPointerEvent(
    eventType: PointerEventType,
    onEvent: () -> Unit
): Modifier = this.pointerInput(eventType) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == eventType) {
                onEvent()
            }
        }
    }
}
