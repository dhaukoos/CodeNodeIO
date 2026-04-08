/*
 * NodePaletteViewModel - ViewModel for the Node Palette Panel
 * Encapsulates state and business logic for browsing and selecting node types
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.NodeTypeDefinition

/**
 * State data class for the Node Palette Panel.
 * Contains search and expansion state.
 *
 * @param searchQuery Current search filter text
 * @param expandedCategories Set of categories that are currently expanded
 */
data class NodePaletteState(
    val searchQuery: String = "",
    val expandedCategories: Set<CodeNodeType> = emptySet()
)

/**
 * ViewModel for the Node Palette Panel.
 * Manages state and business logic for node type browsing and selection.
 *
 * This ViewModel encapsulates:
 * - Search query state for filtering nodes
 * - Category expansion/collapse state
 */
class NodePaletteViewModel : ViewModel() {

    private val _state = MutableStateFlow(NodePaletteState())
    val state: StateFlow<NodePaletteState> = _state.asStateFlow()

    /**
     * Updates the search query for filtering nodes.
     *
     * @param query The new search query text
     */
    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    /**
     * Toggles the expansion state of a category.
     * If expanded, it will collapse. If collapsed, it will expand.
     *
     * @param category The category to toggle
     */
    fun toggleCategory(category: CodeNodeType) {
        _state.update { currentState ->
            val newExpanded = if (category in currentState.expandedCategories) {
                currentState.expandedCategories - category
            } else {
                currentState.expandedCategories + category
            }
            currentState.copy(expandedCategories = newExpanded)
        }
    }

    /**
     * Expands a specific category.
     *
     * @param category The category to expand
     */
    fun expandCategory(category: CodeNodeType) {
        _state.update { currentState ->
            currentState.copy(expandedCategories = currentState.expandedCategories + category)
        }
    }

    /**
     * Collapses a specific category.
     *
     * @param category The category to collapse
     */
    fun collapseCategory(category: CodeNodeType) {
        _state.update { currentState ->
            currentState.copy(expandedCategories = currentState.expandedCategories - category)
        }
    }

    /**
     * Clears the search query.
     */
    fun clearSearch() {
        _state.update { it.copy(searchQuery = "") }
    }

    /**
     * Collapses all categories.
     */
    fun collapseAllCategories() {
        _state.update { it.copy(expandedCategories = emptySet()) }
    }
}
