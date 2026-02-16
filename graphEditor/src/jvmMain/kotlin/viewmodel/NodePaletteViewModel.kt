/*
 * NodePaletteViewModel - ViewModel for the Node Palette Panel
 * Encapsulates state and business logic for browsing and selecting node types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.grapheditor.repository.CustomNodeRepository

/**
 * State data class for the Node Palette Panel.
 * Contains search, expansion state, and deletable node tracking.
 *
 * @param searchQuery Current search filter text
 * @param expandedCategories Set of categories that are currently expanded
 * @param deletableNodeNames Set of node names that can be deleted (custom nodes)
 */
data class NodePaletteState(
    val searchQuery: String = "",
    val expandedCategories: Set<NodeTypeDefinition.NodeCategory> = emptySet(),
    val deletableNodeNames: Set<String> = emptySet()
) : BaseState

/**
 * ViewModel for the Node Palette Panel.
 * Manages state and business logic for node type browsing and selection.
 *
 * This ViewModel encapsulates:
 * - Search query state for filtering nodes
 * - Category expansion/collapse state
 * - Deletable node tracking for custom nodes
 * - Custom node deletion through repository
 *
 * @param customNodeRepository Repository for managing custom node persistence
 * @param onCustomNodesChanged Callback invoked when custom nodes are modified (for refreshing node list)
 */
class NodePaletteViewModel(
    private val customNodeRepository: CustomNodeRepository,
    private val onCustomNodesChanged: () -> Unit = {}
) : ViewModel() {

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
    fun toggleCategory(category: NodeTypeDefinition.NodeCategory) {
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
    fun expandCategory(category: NodeTypeDefinition.NodeCategory) {
        _state.update { currentState ->
            currentState.copy(expandedCategories = currentState.expandedCategories + category)
        }
    }

    /**
     * Collapses a specific category.
     *
     * @param category The category to collapse
     */
    fun collapseCategory(category: NodeTypeDefinition.NodeCategory) {
        _state.update { currentState ->
            currentState.copy(expandedCategories = currentState.expandedCategories - category)
        }
    }

    /**
     * Updates the set of deletable node names.
     * Called when the list of custom nodes changes.
     *
     * @param names Set of node names that can be deleted
     */
    fun updateDeletableNodeNames(names: Set<String>) {
        _state.update { it.copy(deletableNodeNames = names) }
    }

    /**
     * Deletes a custom node by its name.
     * Finds the node in the repository and removes it.
     *
     * @param nodeName The name of the node to delete
     * @return true if the node was found and deleted, false otherwise
     */
    fun deleteCustomNode(nodeName: String): Boolean {
        val allNodes = customNodeRepository.getAll()
        val nodeToDelete = allNodes.find { it.name == nodeName }

        return if (nodeToDelete != null) {
            val removed = customNodeRepository.remove(nodeToDelete.id)
            if (removed) {
                // Update deletable names after deletion
                val updatedNames = customNodeRepository.getAll().map { it.name }.toSet()
                _state.update { it.copy(deletableNodeNames = updatedNames) }
                onCustomNodesChanged()
            }
            removed
        } else {
            false
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
