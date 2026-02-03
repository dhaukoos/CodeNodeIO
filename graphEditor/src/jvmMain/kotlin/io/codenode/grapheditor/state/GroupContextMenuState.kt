/*
 * GroupContextMenuState - State for Group/Ungroup Context Menu
 * Manages the state of the context menu for grouping operations
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import androidx.compose.ui.geometry.Offset

/**
 * State for the group/ungroup context menu.
 *
 * @property position Screen position where the menu should appear
 * @property selectedNodeIds Set of currently selected node IDs
 * @property isGraphNodeSelected True if a GraphNode (not CodeNode) is selected
 * @property selectedGraphNodeId The ID of the selected GraphNode (if isGraphNodeSelected is true)
 */
data class GroupContextMenuState(
    val position: Offset,
    val selectedNodeIds: Set<String>,
    val isGraphNodeSelected: Boolean,
    val selectedGraphNodeId: String? = null
) {
    /**
     * True if the "Group" option should be shown (2+ nodes selected)
     */
    val showGroupOption: Boolean get() = selectedNodeIds.size >= 2

    /**
     * True if the "Ungroup" option should be shown (single GraphNode selected)
     */
    val showUngroupOption: Boolean get() = isGraphNodeSelected && selectedNodeIds.size == 1

    /**
     * True if any menu options are available
     */
    val hasOptions: Boolean get() = showGroupOption || showUngroupOption
}
