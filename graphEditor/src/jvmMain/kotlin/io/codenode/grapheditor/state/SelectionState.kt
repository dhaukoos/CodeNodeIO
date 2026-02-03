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
 * Manages multi-selection of nodes and connections using a unified SelectableElement model.
 *
 * @property selectedElements Set of currently selected elements (nodes and connections)
 * @property selectionBoxStart Start position of rectangular selection (null if not active)
 * @property selectionBoxEnd End position of rectangular selection (null if not active)
 * @property isRectangularSelectionActive Whether rectangular selection is currently in progress
 */
data class SelectionState(
    val selectedElements: Set<SelectableElement> = emptySet(),
    val selectionBoxStart: Offset? = null,
    val selectionBoxEnd: Offset? = null,
    val isRectangularSelectionActive: Boolean = false
) {
    /**
     * Set of currently selected node IDs (derived from selectedElements)
     */
    val selectedNodeIds: Set<String>
        get() = selectedElements.nodeIds()

    /**
     * Set of currently selected connection IDs (derived from selectedElements)
     */
    val selectedConnectionIds: Set<String>
        get() = selectedElements.connectionIds()

    /**
     * Whether any nodes or connections are selected
     */
    val hasSelection: Boolean
        get() = selectedElements.isNotEmpty()

    /**
     * Whether any nodes are selected
     */
    val hasNodeSelection: Boolean
        get() = selectedElements.any { it is SelectableElement.Node }

    /**
     * Whether any connections are selected
     */
    val hasConnectionSelection: Boolean
        get() = selectedElements.any { it is SelectableElement.Connection }

    /**
     * Number of selected nodes
     */
    val nodeSelectionCount: Int
        get() = selectedElements.count { it is SelectableElement.Node }

    /**
     * Number of selected connections
     */
    val connectionSelectionCount: Int
        get() = selectedElements.count { it is SelectableElement.Connection }

    /**
     * Total number of selected items (nodes + connections)
     */
    val totalSelectionCount: Int
        get() = selectedElements.size

    /**
     * Whether the current selection can be grouped into a GraphNode.
     * Requires at least 2 nodes selected.
     */
    val canGroup: Boolean
        get() = nodeSelectionCount >= 2

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

    /**
     * Creates a new SelectionState with a node toggled in/out of selection
     */
    fun toggleNode(nodeId: String): SelectionState {
        val element = SelectableElement.Node(nodeId)
        val newElements = if (element in selectedElements) {
            selectedElements - element
        } else {
            selectedElements + element
        }
        return copy(selectedElements = newElements)
    }

    /**
     * Creates a new SelectionState with a connection toggled in/out of selection
     */
    fun toggleConnection(connectionId: String): SelectionState {
        val element = SelectableElement.Connection(connectionId)
        val newElements = if (element in selectedElements) {
            selectedElements - element
        } else {
            selectedElements + element
        }
        return copy(selectedElements = newElements)
    }

    /**
     * Creates a new SelectionState with an element toggled in/out of selection
     */
    fun toggle(element: SelectableElement): SelectionState {
        val newElements = if (element in selectedElements) {
            selectedElements - element
        } else {
            selectedElements + element
        }
        return copy(selectedElements = newElements)
    }

    /**
     * Checks if a node is in the selection
     */
    fun containsNode(nodeId: String): Boolean =
        selectedElements.containsNode(nodeId)

    /**
     * Checks if a connection is in the selection
     */
    fun containsConnection(connectionId: String): Boolean =
        selectedElements.containsConnection(connectionId)

    companion object {
        /**
         * Creates a SelectionState with the given node IDs selected
         * (for backward compatibility)
         */
        fun withNodes(nodeIds: Set<String>): SelectionState =
            SelectionState(
                selectedElements = nodeIds.map { SelectableElement.Node(it) }.toSet()
            )

        /**
         * Creates a SelectionState with the given connection IDs selected
         * (for backward compatibility)
         */
        fun withConnections(connectionIds: Set<String>): SelectionState =
            SelectionState(
                selectedElements = connectionIds.map { SelectableElement.Connection(it) }.toSet()
            )
    }
}
