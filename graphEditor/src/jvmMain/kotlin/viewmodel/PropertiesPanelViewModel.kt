/*
 * PropertiesPanelViewModel - ViewModel for the Properties Panel
 * Encapsulates state and business logic for node property editing
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection

/**
 * State data class for the Properties Panel ViewModel.
 * Contains selection state, editing state, pending changes, and validation state.
 *
 * @param selectedNodeId ID of the currently selected node (null if none)
 * @param selectedConnectionId ID of the currently selected connection (null if none)
 * @param nodeName Current node name being edited
 * @param properties Current property values being edited
 * @param originalNodeName Original node name before editing (for dirty detection)
 * @param originalProperties Original property values before editing (for dirty detection)
 * @param pendingChanges Map of property keys to pending values (not yet committed)
 * @param validationErrors Map of property keys to validation error messages
 * @param editingPropertyKey The property key currently being edited (null if none)
 * @param isGenericNode Whether the selected node is a generic node type
 */
data class PropertiesPanelViewModelState(
    val selectedNodeId: String? = null,
    val selectedConnectionId: String? = null,
    val nodeName: String = "",
    val properties: Map<String, String> = emptyMap(),
    val originalNodeName: String = "",
    val originalProperties: Map<String, String> = emptyMap(),
    val pendingChanges: Map<String, String> = emptyMap(),
    val validationErrors: Map<String, String> = emptyMap(),
    val editingPropertyKey: String? = null,
    val isGenericNode: Boolean = false
) : BaseState {
    /** Whether no node or connection is selected */
    val isEmptyState: Boolean
        get() = selectedNodeId == null && selectedConnectionId == null

    /** Whether properties or name have been modified since last save */
    val isDirty: Boolean
        get() = properties != originalProperties || nodeName != originalNodeName || pendingChanges.isNotEmpty()

    /** Whether there are validation errors */
    val hasValidationErrors: Boolean
        get() = validationErrors.isNotEmpty()

    /** Whether all validation passes */
    val isValid: Boolean
        get() = validationErrors.isEmpty()

    /** Gets the validation error for a specific property */
    fun getErrorForProperty(key: String): String? = validationErrors[key]

    /** Gets the current value for a property (pending or committed) */
    fun getCurrentValue(key: String): String =
        pendingChanges[key] ?: properties[key] ?: ""
}

/**
 * ViewModel for the Properties Panel.
 * Manages state and business logic for viewing and editing node properties.
 *
 * This ViewModel encapsulates:
 * - Selection tracking for nodes and connections
 * - Property editing with pending changes
 * - Validation of property values
 * - Name and port name updates
 *
 * @param onNodeNameChanged Callback when node name is committed
 * @param onPropertyChanged Callback when a property value is committed
 * @param onPortNameChanged Callback when a port name is committed
 */
class PropertiesPanelViewModel(
    private val onNodeNameChanged: (String) -> Unit = { _ -> },
    private val onPropertyChanged: (String, String) -> Unit = { _, _ -> },
    private val onPortNameChanged: (String, String) -> Unit = { _, _ -> }
) : ViewModel() {

    private val _state = MutableStateFlow(PropertiesPanelViewModelState())
    val state: StateFlow<PropertiesPanelViewModelState> = _state.asStateFlow()

    /**
     * Selects a node for property editing.
     *
     * @param node The node to select, or null to clear selection
     */
    fun selectNode(node: CodeNode?) {
        if (node == null) {
            clearSelection()
            return
        }

        _state.update {
            PropertiesPanelViewModelState(
                selectedNodeId = node.id,
                selectedConnectionId = null,
                nodeName = node.name,
                properties = node.configuration,
                originalNodeName = node.name,
                originalProperties = node.configuration,
                isGenericNode = node.codeNodeType == CodeNodeType.GENERIC
            )
        }
    }

    /**
     * Selects a connection for property display.
     *
     * @param connection The connection to select, or null to clear selection
     */
    fun selectConnection(connection: Connection?) {
        if (connection == null) {
            clearSelection()
            return
        }

        _state.update {
            PropertiesPanelViewModelState(
                selectedNodeId = null,
                selectedConnectionId = connection.id
            )
        }
    }

    /**
     * Clears the current selection.
     */
    fun clearSelection() {
        _state.update { PropertiesPanelViewModelState() }
    }

    /**
     * Starts editing a specific property.
     *
     * @param propertyKey The key of the property to edit
     */
    fun startEditing(propertyKey: String) {
        _state.update { it.copy(editingPropertyKey = propertyKey) }
    }

    /**
     * Updates a pending change for a property (not yet committed).
     *
     * @param key The property key
     * @param value The new value
     */
    fun updatePendingChange(key: String, value: String) {
        _state.update { currentState ->
            val newPendingChanges = currentState.pendingChanges + (key to value)
            currentState.copy(pendingChanges = newPendingChanges)
        }
    }

    /**
     * Commits all pending changes.
     * Calls the appropriate callbacks for each change.
     */
    fun commitChanges() {
        val currentState = _state.value

        // Commit pending property changes
        currentState.pendingChanges.forEach { (key, value) ->
            onPropertyChanged(key, value)
        }

        // Update state to reflect committed changes
        _state.update { state ->
            val newProperties = state.properties + state.pendingChanges
            state.copy(
                properties = newProperties,
                originalProperties = newProperties,
                pendingChanges = emptyMap(),
                editingPropertyKey = null
            )
        }
    }

    /**
     * Cancels all pending changes and reverts to original values.
     */
    fun cancelEditing() {
        _state.update { state ->
            state.copy(
                pendingChanges = emptyMap(),
                editingPropertyKey = null,
                validationErrors = emptyMap()
            )
        }
    }

    /**
     * Updates the node name.
     *
     * @param name The new node name
     */
    fun updateNodeName(name: String) {
        _state.update { it.copy(nodeName = name) }
        onNodeNameChanged(name)
    }

    /**
     * Updates a port name.
     *
     * @param portId The ID of the port to rename
     * @param newName The new port name
     */
    fun updatePortName(portId: String, newName: String) {
        onPortNameChanged(portId, newName)
    }

    /**
     * Updates a property value immediately (without pending).
     *
     * @param key The property key
     * @param value The new value
     */
    fun updateProperty(key: String, value: String) {
        _state.update { state ->
            state.copy(properties = state.properties + (key to value))
        }
        onPropertyChanged(key, value)
    }

    /**
     * Sets a validation error for a property.
     *
     * @param key The property key
     * @param error The error message, or null to clear the error
     */
    fun setValidationError(key: String, error: String?) {
        _state.update { state ->
            val newErrors = if (error == null) {
                state.validationErrors - key
            } else {
                state.validationErrors + (key to error)
            }
            state.copy(validationErrors = newErrors)
        }
    }

    /**
     * Clears all validation errors.
     */
    fun clearValidationErrors() {
        _state.update { it.copy(validationErrors = emptyMap()) }
    }

    /**
     * Resets the panel to its original state (discarding all changes).
     */
    fun reset() {
        _state.update { state ->
            state.copy(
                nodeName = state.originalNodeName,
                properties = state.originalProperties,
                pendingChanges = emptyMap(),
                validationErrors = emptyMap(),
                editingPropertyKey = null
            )
        }
    }

    /**
     * Marks the current state as saved (resets dirty tracking).
     */
    fun markSaved() {
        _state.update { state ->
            state.copy(
                originalNodeName = state.nodeName,
                originalProperties = state.properties,
                pendingChanges = emptyMap()
            )
        }
    }
}
