/*
 * ViewSynchronizer - Bidirectional Synchronization Manager
 * Coordinates synchronization between visual and textual graph representations
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import androidx.compose.runtime.*
import io.codenode.fbpdsl.dsl.TextGenerator
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Connection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages bidirectional synchronization between visual and textual views
 *
 * The synchronization strategy:
 * - Visual → Textual: Automatic (GraphState updates trigger DSL text regeneration)
 * - Textual → Visual: On-demand (user must explicitly apply changes from text editor)
 *
 * This prevents edit conflicts and gives users control over when text changes
 * are applied to the visual graph.
 */
class ViewSynchronizer(
    private val graphState: GraphState
) {
    // Generated DSL text (automatically updated when graph changes)
    private val _dslText = MutableStateFlow("")
    val dslText: StateFlow<String> = _dslText.asStateFlow()

    // Text being edited by user (may differ from generated text)
    private val _editedText = MutableStateFlow("")
    val editedText: StateFlow<String> = _editedText.asStateFlow()

    // Whether edited text differs from generated text
    private val _hasUnappliedChanges = MutableStateFlow(false)
    val hasUnappliedChanges: StateFlow<Boolean> = _hasUnappliedChanges.asStateFlow()

    // Sync errors
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    init {
        // Initialize with current graph
        regenerateTextFromGraph()
    }

    /**
     * Regenerate DSL text from the current graph state
     * Called automatically when graph changes in visual view
     */
    fun regenerateTextFromGraph() {
        val newText = TextGenerator.generate(graphState.flowGraph)
        _dslText.value = newText

        // If user hasn't edited the text, update edited text too
        if (!_hasUnappliedChanges.value) {
            _editedText.value = newText
        }
    }

    /**
     * Update the edited text (called as user types in textual view)
     *
     * @param newText The new text from the text editor
     */
    fun updateEditedText(newText: String) {
        _editedText.value = newText
        _hasUnappliedChanges.value = (newText != _dslText.value)
    }

    /**
     * Apply changes from edited text to the graph
     * Parses the edited DSL text and updates the graph state
     *
     * @return Result indicating success or failure with error message
     */
    fun applyTextChangesToGraph(): Result<Unit> {
        return try {
            // TODO: Implement DSL parsing to convert text back to FlowGraph
            // For now, this would require a DSL parser which is beyond the current scope
            // In a real implementation, you would:
            // 1. Parse _editedText.value to create a new FlowGraph
            // 2. Call graphState.setGraph(parsedGraph)
            // 3. Call regenerateTextFromGraph() to sync

            _syncError.value = "DSL parsing not yet implemented. " +
                "Text-to-graph conversion requires a Kotlin DSL parser."
            Result.failure(NotImplementedError("DSL parsing not implemented"))
        } catch (e: Exception) {
            _syncError.value = "Failed to parse DSL: ${e.message}"
            Result.failure(e)
        }
    }

    /**
     * Discard edited text changes and revert to generated text
     */
    fun discardTextChanges() {
        _editedText.value = _dslText.value
        _hasUnappliedChanges.value = false
        _syncError.value = null
    }

    /**
     * Clear any sync errors
     */
    fun clearError() {
        _syncError.value = null
    }

    /**
     * Manual sync trigger for visual changes
     * Called when graph is modified in visual view
     */
    fun onVisualGraphChanged() {
        regenerateTextFromGraph()
    }

    /**
     * Get sync statistics for debugging/monitoring
     */
    fun getSyncStats(): SyncStats {
        return SyncStats(
            generatedTextLength = _dslText.value.length,
            editedTextLength = _editedText.value.length,
            hasUnappliedChanges = _hasUnappliedChanges.value,
            nodeCount = graphState.flowGraph.rootNodes.size,
            connectionCount = graphState.flowGraph.connections.size
        )
    }
}

/**
 * Statistics about current synchronization state
 */
data class SyncStats(
    val generatedTextLength: Int,
    val editedTextLength: Int,
    val hasUnappliedChanges: Boolean,
    val nodeCount: Int,
    val connectionCount: Int
)

/**
 * Remember a ViewSynchronizer instance in Compose
 *
 * @param graphState The graph state to synchronize
 * @return A remembered ViewSynchronizer instance
 */
@Composable
fun rememberViewSynchronizer(graphState: GraphState): ViewSynchronizer {
    return remember(graphState) {
        ViewSynchronizer(graphState)
    }
}
