/*
 * SelectionState - Multi-Selection State Management for Graph Editor
 * Manages selection of nodes and connections, including rectangular selection
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Represents the selection state in the graph editor.
 * Manages multi-selection of nodes and connections, as well as rectangular selection.
 *
 * @property selectedNodeIds Set of currently selected node IDs
 * @property selectedConnectionIds Set of currently selected connection IDs
 * @property selectionBoxStart Start position of rectangular selection (null if not active)
 * @property selectionBoxEnd End position of rectangular selection (null if not active)
 * @property isRectangularSelectionActive Whether rectangular selection is currently in progress
 */
data class SelectionState(
    val selectedNodeIds: Set<String> = emptySet(),
    val selectedConnectionIds: Set<String> = emptySet(),
    val selectionBoxStart: Offset? = null,
    val selectionBoxEnd: Offset? = null,
    val isRectangularSelectionActive: Boolean = false
) {
    /**
     * Whether any nodes or connections are selected
     */
    val hasSelection: Boolean
        get() = selectedNodeIds.isNotEmpty() || selectedConnectionIds.isNotEmpty()

    /**
     * Whether any nodes are selected
     */
    val hasNodeSelection: Boolean
        get() = selectedNodeIds.isNotEmpty()

    /**
     * Whether any connections are selected
     */
    val hasConnectionSelection: Boolean
        get() = selectedConnectionIds.isNotEmpty()

    /**
     * Number of selected nodes
     */
    val nodeSelectionCount: Int
        get() = selectedNodeIds.size

    /**
     * Number of selected connections
     */
    val connectionSelectionCount: Int
        get() = selectedConnectionIds.size

    /**
     * Total number of selected items (nodes + connections)
     */
    val totalSelectionCount: Int
        get() = nodeSelectionCount + connectionSelectionCount

    /**
     * Whether the current selection can be grouped into a GraphNode.
     * Requires at least 2 nodes selected.
     */
    val canGroup: Boolean
        get() = selectedNodeIds.size >= 2

    /**
     * The bounding rectangle of the selection box.
     * Returns null if selection box is not active (both start and end must be set).
     * Normalizes coordinates so the rect is always valid (min to max).
     */
    val selectionBoxBounds: Rect?
        get() {
            val start = selectionBoxStart ?: return null
            val end = selectionBoxEnd ?: return null

            return Rect(
                left = minOf(start.x, end.x),
                top = minOf(start.y, end.y),
                right = maxOf(start.x, end.x),
                bottom = maxOf(start.y, end.y)
            )
        }
}
