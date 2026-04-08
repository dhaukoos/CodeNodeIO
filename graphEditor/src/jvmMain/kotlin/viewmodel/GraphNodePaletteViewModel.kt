/*
 * GraphNodePaletteViewModel - ViewModel for the GraphNodes palette section
 * Manages expand/collapse state and search filtering for GraphNode templates
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import io.codenode.flowgraphpersist.model.GraphNodeTemplateMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State for the GraphNodes palette section.
 *
 * @param isExpanded Whether the GraphNodes section is expanded
 */
data class GraphNodePaletteState(
    val isExpanded: Boolean = false
) : BaseState

/**
 * ViewModel for the GraphNodes section in the Node Palette.
 * Manages section expand/collapse state and provides search filtering.
 */
class GraphNodePaletteViewModel : ViewModel() {

    private val _state = MutableStateFlow(GraphNodePaletteState())
    val state: StateFlow<GraphNodePaletteState> = _state.asStateFlow()

    /**
     * Toggles the GraphNodes section expand/collapse state.
     */
    fun toggleSection() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    /**
     * Filters templates by search query, matching against name and description.
     *
     * @param templates All available GraphNode templates
     * @param query The current search query
     * @return Filtered list of matching templates
     */
    fun filteredTemplates(
        templates: List<GraphNodeTemplateMeta>,
        query: String
    ): List<GraphNodeTemplateMeta> {
        if (query.isBlank()) return templates
        return templates.filter { template ->
            template.name.contains(query, ignoreCase = true) ||
            template.description?.contains(query, ignoreCase = true) == true
        }
    }
}
