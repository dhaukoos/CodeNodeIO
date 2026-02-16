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
import io.codenode.grapheditor.viewmodel.NodePaletteViewModel
import io.codenode.grapheditor.viewmodel.NodePaletteState

/**
 * Node palette component for browsing and selecting node types.
 * Uses NodePaletteViewModel for state management.
 *
 * This composable is purely for UI rendering - all business logic and state
 * management is delegated to the NodePaletteViewModel.
 *
 * @param viewModel The ViewModel managing state and business logic
 * @param nodeTypes List of available node type definitions
 * @param onNodeSelected Callback when a node type is selected for placement
 * @param modifier Modifier for the palette
 */
@Composable
fun NodePalette(
    viewModel: NodePaletteViewModel,
    nodeTypes: List<NodeTypeDefinition>,
    onNodeSelected: (NodeTypeDefinition) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    NodePaletteContent(
        state = state,
        nodeTypes = nodeTypes,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onCategoryToggle = { viewModel.toggleCategory(it) },
        onNodeSelected = onNodeSelected,
        onNodeDeleted = { viewModel.deleteCustomNode(it) },
        modifier = modifier
    )
}

/**
 * Stateless content composable for the Node Palette.
 * Pure rendering function with no business logic.
 */
@Composable
private fun NodePaletteContent(
    state: NodePaletteState,
    nodeTypes: List<NodeTypeDefinition>,
    onSearchQueryChange: (String) -> Unit,
    onCategoryToggle: (NodeTypeDefinition.NodeCategory) -> Unit,
    onNodeSelected: (NodeTypeDefinition) -> Unit,
    onNodeDeleted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group node types by category with search filter applied
    val groupedNodes = nodeTypes
        .filter { nodeType ->
            if (state.searchQuery.isBlank()) return@filter true
            nodeType.name.contains(state.searchQuery, ignoreCase = true) ||
            nodeType.description?.contains(state.searchQuery, ignoreCase = true) == true
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
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
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
                        isExpanded = category in state.expandedCategories,
                        onToggle = { onCategoryToggle(category) }
                    )
                }

                if (category in state.expandedCategories) {
                    items(nodes) { nodeType ->
                        NodeTypeItem(
                            nodeType = nodeType,
                            onClick = { onNodeSelected(nodeType) },
                            isDeletable = nodeType.name in state.deletableNodeNames,
                            onDelete = { onNodeDeleted(nodeType.name) }
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
            text = category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF424242)
        )
    }
}

/**
 * Individual node type item in the palette
 *
 * @param nodeType The node type definition to display
 * @param onClick Callback when the item is clicked
 * @param isDeletable Whether this node can be deleted (shows X button)
 * @param onDelete Callback when the delete button is clicked
 */
@Composable
private fun NodeTypeItem(
    nodeType: NodeTypeDefinition,
    onClick: () -> Unit,
    isDeletable: Boolean = false,
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        backgroundColor = Color.White,
        elevation = 1.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = if (isDeletable) 24.dp else 0.dp)
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
                    val inputPorts = nodeType.getInputPortTemplates()
                    if (inputPorts.isNotEmpty()) {
                        PortBadge(
                            count = inputPorts.size,
                            label = "in",
                            color = Color(0xFF4CAF50)
                        )
                    }

                    val outputPorts = nodeType.getOutputPortTemplates()
                    if (outputPorts.isNotEmpty()) {
                        PortBadge(
                            count = outputPorts.size,
                            label = "out",
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            }

            // Delete button in upper right corner (only for deletable/custom nodes)
            if (isDeletable) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clickable(onClick = onDelete),
                    shape = RoundedCornerShape(2.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = "×",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
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
