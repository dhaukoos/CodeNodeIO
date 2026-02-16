/*
 * GraphEditorViewModel - Main Orchestration ViewModel for the Graph Editor
 * Encapsulates state and business logic for file operations, edit actions, navigation, and dialogs
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.codenode.fbpdsl.model.FlowGraph

/**
 * Enum representing the various dialogs that can be shown in the editor.
 */
enum class EditorDialog {
    NONE,
    OPEN_FILE,
    SAVE_MODULE,
    COMPILE,
    FLOW_GRAPH_PROPERTIES
}

/**
 * State data class for the Graph Editor ViewModel.
 * Contains orchestration-level state for the entire editor.
 *
 * @param statusMessage Current status message displayed to the user
 * @param activeDialog The currently active dialog (NONE if no dialog is open)
 * @param canGroup Whether the "Group" action is available (2+ nodes selected)
 * @param canUngroup Whether the "Ungroup" action is available (single GraphNode selected)
 * @param isInsideGraphNode Whether currently navigated inside a GraphNode (not at root)
 * @param currentGraphNodeName Name of the current GraphNode (null if at root)
 * @param flowGraphName Name of the current flow graph
 * @param canUndo Whether undo is available
 * @param canRedo Whether redo is available
 * @param undoDescription Description of the action that would be undone
 * @param redoDescription Description of the action that would be redone
 * @param isDirty Whether the graph has unsaved changes
 */
data class GraphEditorState(
    val statusMessage: String = "Ready - Create a new graph or open an existing one",
    val activeDialog: EditorDialog = EditorDialog.NONE,
    val canGroup: Boolean = false,
    val canUngroup: Boolean = false,
    val isInsideGraphNode: Boolean = false,
    val currentGraphNodeName: String? = null,
    val flowGraphName: String = "New Graph",
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val undoDescription: String? = null,
    val redoDescription: String? = null,
    val isDirty: Boolean = false
) : BaseState {
    /** Whether any dialog is currently open */
    val hasActiveDialog: Boolean
        get() = activeDialog != EditorDialog.NONE

    /** Whether navigation back is available */
    val canNavigateBack: Boolean
        get() = isInsideGraphNode
}

/**
 * ViewModel for the main Graph Editor orchestration.
 * Manages high-level state and coordinates actions across the editor.
 *
 * This ViewModel encapsulates:
 * - File operations (new, open, save)
 * - Edit actions (undo, redo, group, ungroup)
 * - Navigation actions (navigate back, compile)
 * - Dialog management
 * - Status message display
 *
 * @param onCreateNewGraph Callback to create a new graph
 * @param onOpenGraph Callback to open a graph file
 * @param onSaveGraph Callback to save the current graph
 * @param onUndo Callback to perform undo
 * @param onRedo Callback to perform redo
 * @param onGroupSelectedNodes Callback to group selected nodes
 * @param onUngroupSelectedNode Callback to ungroup a selected node
 * @param onNavigateBack Callback to navigate back from a GraphNode
 * @param onCompile Callback to compile the graph
 */
class GraphEditorViewModel(
    private val onCreateNewGraph: () -> Unit = {},
    private val onOpenGraph: () -> Unit = {},
    private val onSaveGraph: () -> Unit = {},
    private val onUndo: () -> Boolean = { false },
    private val onRedo: () -> Boolean = { false },
    private val onGroupSelectedNodes: () -> Unit = {},
    private val onUngroupSelectedNode: () -> Unit = {},
    private val onNavigateBack: () -> Boolean = { false },
    private val onCompile: () -> Unit = {}
) : ViewModel() {

    private val _state = MutableStateFlow(GraphEditorState())
    val state: StateFlow<GraphEditorState> = _state.asStateFlow()

    // ========== File Actions ==========

    /**
     * Creates a new graph.
     * Clears the current graph and resets to default state.
     */
    fun createNewGraph() {
        onCreateNewGraph()
        _state.update {
            it.copy(
                statusMessage = "New graph created",
                flowGraphName = "New Graph",
                isDirty = false,
                isInsideGraphNode = false,
                currentGraphNodeName = null
            )
        }
    }

    /**
     * Opens the file open dialog.
     */
    fun openGraph() {
        showDialog(EditorDialog.OPEN_FILE)
        onOpenGraph()
    }

    /**
     * Opens the save dialog.
     */
    fun saveGraph() {
        showDialog(EditorDialog.SAVE_MODULE)
        onSaveGraph()
    }

    /**
     * Called after a graph has been loaded successfully.
     *
     * @param graphName The name of the loaded graph
     */
    fun onGraphLoaded(graphName: String) {
        _state.update {
            it.copy(
                statusMessage = "Loaded: $graphName",
                flowGraphName = graphName,
                isDirty = false,
                isInsideGraphNode = false,
                currentGraphNodeName = null
            )
        }
        hideDialog()
    }

    /**
     * Called after a graph has been saved successfully.
     */
    fun onGraphSaved() {
        _state.update {
            it.copy(
                statusMessage = "Graph saved",
                isDirty = false
            )
        }
        hideDialog()
    }

    // ========== Edit Actions ==========

    /**
     * Performs undo operation.
     */
    fun undo() {
        if (onUndo()) {
            val description = _state.value.redoDescription ?: "action"
            _state.update {
                it.copy(statusMessage = "Undo: $description")
            }
        }
    }

    /**
     * Performs redo operation.
     */
    fun redo() {
        if (onRedo()) {
            val description = _state.value.undoDescription ?: "action"
            _state.update {
                it.copy(statusMessage = "Redo: $description")
            }
        }
    }

    /**
     * Groups the currently selected nodes into a GraphNode.
     */
    fun groupSelectedNodes() {
        if (_state.value.canGroup) {
            onGroupSelectedNodes()
        }
    }

    /**
     * Ungroups the currently selected GraphNode.
     */
    fun ungroupSelectedNode() {
        if (_state.value.canUngroup) {
            onUngroupSelectedNode()
        }
    }

    /**
     * Updates group/ungroup availability based on selection state.
     *
     * @param canGroup Whether grouping is available
     * @param canUngroup Whether ungrouping is available
     */
    fun updateGroupingState(canGroup: Boolean, canUngroup: Boolean) {
        _state.update {
            it.copy(canGroup = canGroup, canUngroup = canUngroup)
        }
    }

    /**
     * Updates undo/redo state.
     *
     * @param canUndo Whether undo is available
     * @param canRedo Whether redo is available
     * @param undoDescription Description of undo action
     * @param redoDescription Description of redo action
     */
    fun updateUndoRedoState(
        canUndo: Boolean,
        canRedo: Boolean,
        undoDescription: String?,
        redoDescription: String?
    ) {
        _state.update {
            it.copy(
                canUndo = canUndo,
                canRedo = canRedo,
                undoDescription = undoDescription,
                redoDescription = redoDescription
            )
        }
    }

    // ========== Navigation Actions ==========

    /**
     * Navigates back from the current GraphNode to its parent.
     */
    fun navigateBack() {
        if (onNavigateBack()) {
            _state.update {
                it.copy(statusMessage = "Navigated back to parent")
            }
        }
    }

    /**
     * Opens the compile dialog.
     */
    fun compile() {
        showDialog(EditorDialog.COMPILE)
        onCompile()
    }

    /**
     * Updates navigation state.
     *
     * @param isInsideGraphNode Whether inside a GraphNode
     * @param currentGraphNodeName Name of current GraphNode (null if at root)
     */
    fun updateNavigationState(isInsideGraphNode: Boolean, currentGraphNodeName: String?) {
        _state.update {
            it.copy(
                isInsideGraphNode = isInsideGraphNode,
                currentGraphNodeName = currentGraphNodeName
            )
        }
    }

    // ========== Dialog Actions ==========

    /**
     * Shows a specific dialog.
     *
     * @param dialog The dialog to show
     */
    fun showDialog(dialog: EditorDialog) {
        _state.update {
            it.copy(activeDialog = dialog)
        }
    }

    /**
     * Hides the currently active dialog.
     */
    fun hideDialog() {
        _state.update {
            it.copy(activeDialog = EditorDialog.NONE)
        }
    }

    /**
     * Shows the flow graph properties dialog.
     */
    fun showFlowGraphProperties() {
        showDialog(EditorDialog.FLOW_GRAPH_PROPERTIES)
    }

    // ========== Status Message Actions ==========

    /**
     * Sets the status message.
     *
     * @param message The message to display
     */
    fun setStatusMessage(message: String) {
        _state.update {
            it.copy(statusMessage = message)
        }
    }

    /**
     * Clears the status message.
     */
    fun clearStatusMessage() {
        _state.update {
            it.copy(statusMessage = "")
        }
    }

    // ========== Graph State Updates ==========

    /**
     * Updates the flow graph name.
     *
     * @param name The new graph name
     */
    fun updateFlowGraphName(name: String) {
        _state.update {
            it.copy(flowGraphName = name)
        }
    }

    /**
     * Marks the graph as dirty (has unsaved changes).
     */
    fun markDirty() {
        _state.update {
            it.copy(isDirty = true)
        }
    }

    /**
     * Marks the graph as clean (no unsaved changes).
     */
    fun markClean() {
        _state.update {
            it.copy(isDirty = false)
        }
    }

    /**
     * Resets the ViewModel to its initial state.
     */
    fun reset() {
        _state.update {
            GraphEditorState()
        }
    }
}
