/*
 * NavigationContext - Hierarchical Navigation State for Graph Editor
 * Manages navigation through nested GraphNodes (zoom in/out)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import androidx.compose.ui.geometry.Offset

/**
 * Stores the pan offset and zoom scale for a navigation level.
 */
data class ViewState(
    val panOffset: Offset,
    val scale: Float
)

/**
 * Represents the navigation context for hierarchical GraphNode traversal.
 * Maintains a path stack of GraphNode IDs representing the current view hierarchy,
 * along with the saved view state (pan/zoom) for each level.
 *
 * When the path is empty, the view is at the root FlowGraph level.
 * Each entry in the path represents a GraphNode that has been "zoomed into".
 *
 * @property path List of GraphNode IDs from root to current view level
 * @property viewStates Saved view states corresponding to each path entry
 */
data class NavigationContext(
    val path: List<String> = emptyList(),
    val viewStates: List<ViewState> = emptyList()
) {
    /**
     * Whether the current view is at the root FlowGraph level
     */
    val isAtRoot: Boolean
        get() = path.isEmpty()

    /**
     * The ID of the currently viewed GraphNode, or null if at root
     */
    val currentGraphNodeId: String?
        get() = path.lastOrNull()

    /**
     * The ID of the parent GraphNode (one level up), or null if at root or depth 1
     */
    val parentGraphNodeId: String?
        get() = path.dropLast(1).lastOrNull()

    /**
     * The current navigation depth (0 = root, 1 = inside first GraphNode, etc.)
     */
    val depth: Int
        get() = path.size

    /**
     * Whether navigation out (zoom out) is possible.
     * Only possible when not at root level.
     */
    val canNavigateOut: Boolean
        get() = !isAtRoot

    /**
     * The most recently saved ViewState, or null if at root.
     */
    val lastViewState: ViewState?
        get() = viewStates.lastOrNull()

    /**
     * Navigate into a GraphNode (zoom in).
     * Creates a new NavigationContext with the graphNodeId appended to the path.
     *
     * @param graphNodeId The ID of the GraphNode to navigate into
     * @param viewState The current view state to save before navigating in
     * @return New NavigationContext with updated path and saved view state
     */
    fun pushInto(graphNodeId: String, viewState: ViewState? = null): NavigationContext {
        return NavigationContext(
            path = path + graphNodeId,
            viewStates = if (viewState != null) viewStates + viewState else viewStates
        )
    }

    /**
     * Navigate out of the current GraphNode (zoom out).
     * Creates a new NavigationContext with the last path entry removed.
     * If already at root, returns the same context (path remains empty).
     *
     * @return New NavigationContext with updated path
     */
    fun popOut(): NavigationContext {
        return if (path.isEmpty()) {
            this
        } else {
            NavigationContext(
                path = path.dropLast(1),
                viewStates = if (viewStates.isNotEmpty()) viewStates.dropLast(1) else viewStates
            )
        }
    }

    /**
     * Reset navigation to root level.
     * Creates a new NavigationContext with an empty path.
     *
     * @return New NavigationContext at root level
     */
    fun reset(): NavigationContext {
        return NavigationContext(path = emptyList(), viewStates = emptyList())
    }

    /**
     * Check if a GraphNode ID is in the current navigation path.
     *
     * @param graphNodeId The ID to check
     * @return true if the ID is in the path
     */
    fun contains(graphNodeId: String): Boolean {
        return graphNodeId in path
    }
}
