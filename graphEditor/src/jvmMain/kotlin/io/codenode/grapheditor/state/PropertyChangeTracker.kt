/*
 * PropertyChangeTracker - Undo/Redo Support for Property Changes
 * Tracks property value changes and integrates with UndoRedoManager
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import androidx.compose.runtime.*

/**
 * Tracks property changes for undo/redo functionality.
 *
 * This class works with [UndoRedoManager] to provide undo/redo support
 * for node property changes in the properties panel.
 *
 * Usage:
 * ```kotlin
 * val tracker = PropertyChangeTracker(undoRedoManager, graphState)
 *
 * // Track a property change
 * tracker.trackChange("node_1", "timeout", "30", "60")
 *
 * // Changes are automatically added to the undo stack
 * undoRedoManager.undo(graphState)  // Reverts timeout to "30"
 * ```
 *
 * @property undoRedoManager The undo/redo manager to record changes to
 * @property graphState The graph state for executing commands
 */
class PropertyChangeTracker(
    private val undoRedoManager: UndoRedoManager,
    private val graphState: GraphState
) {
    /**
     * Tracks a single property change.
     *
     * @param nodeId The ID of the node being modified
     * @param propertyKey The property key being changed
     * @param oldValue The previous value
     * @param newValue The new value
     */
    fun trackChange(
        nodeId: String,
        propertyKey: String,
        oldValue: String,
        newValue: String
    ) {
        if (oldValue == newValue) return // No actual change

        val command = PropertyChangeCommand(
            nodeId = nodeId,
            propertyKey = propertyKey,
            oldValue = oldValue,
            newValue = newValue
        )
        undoRedoManager.execute(command, graphState)
    }

    /**
     * Tracks multiple property changes as a single undoable operation.
     *
     * Useful when multiple properties are changed together (e.g., form submission).
     *
     * @param nodeId The ID of the node being modified
     * @param changes Map of property keys to (oldValue, newValue) pairs
     * @param description Optional description for the composite command
     */
    fun trackMultipleChanges(
        nodeId: String,
        changes: Map<String, Pair<String, String>>,
        description: String = "Update multiple properties"
    ) {
        // Filter out non-changes
        val actualChanges = changes.filter { (_, values) -> values.first != values.second }
        if (actualChanges.isEmpty()) return

        val commands = actualChanges.map { (key, values) ->
            PropertyChangeCommand(
                nodeId = nodeId,
                propertyKey = key,
                oldValue = values.first,
                newValue = values.second
            )
        }

        val compositeCommand = CompositePropertyChangeCommand(
            commands = commands,
            nodeId = nodeId,
            description = description
        )
        undoRedoManager.execute(compositeCommand, graphState)
    }

    /**
     * Tracks all property changes between two configuration states.
     *
     * Useful when the entire configuration is replaced (e.g., reset to defaults).
     *
     * @param nodeId The ID of the node being modified
     * @param oldConfig The previous configuration
     * @param newConfig The new configuration
     */
    fun trackConfigurationChange(
        nodeId: String,
        oldConfig: Map<String, String>,
        newConfig: Map<String, String>
    ) {
        val changes = mutableMapOf<String, Pair<String, String>>()

        // Track changed values
        newConfig.forEach { (key, newValue) ->
            val oldValue = oldConfig[key] ?: ""
            if (oldValue != newValue) {
                changes[key] = oldValue to newValue
            }
        }

        // Track removed values
        oldConfig.forEach { (key, oldValue) ->
            if (!newConfig.containsKey(key)) {
                changes[key] = oldValue to ""
            }
        }

        if (changes.isNotEmpty()) {
            trackMultipleChanges(nodeId, changes, "Update configuration")
        }
    }
}

/**
 * Command for a single property change.
 *
 * @property nodeId The ID of the node being modified
 * @property propertyKey The property key being changed
 * @property oldValue The previous value
 * @property newValue The new value
 */
class PropertyChangeCommand(
    private val nodeId: String,
    private val propertyKey: String,
    private val oldValue: String,
    private val newValue: String
) : Command {

    override val description: String
        get() = "Change $propertyKey from '$oldValue' to '$newValue'"

    override fun execute(graphState: GraphState) {
        graphState.updateNodeProperty(nodeId, propertyKey, newValue)
    }

    override fun undo(graphState: GraphState) {
        graphState.updateNodeProperty(nodeId, propertyKey, oldValue)
    }
}

/**
 * Composite command for multiple property changes.
 *
 * @property commands The individual property change commands
 * @property nodeId The node ID for description purposes
 * @property description Human-readable description of the change
 */
class CompositePropertyChangeCommand(
    private val commands: List<PropertyChangeCommand>,
    private val nodeId: String,
    override val description: String
) : Command {

    override fun execute(graphState: GraphState) {
        commands.forEach { it.execute(graphState) }
    }

    override fun undo(graphState: GraphState) {
        // Undo in reverse order
        commands.reversed().forEach { it.undo(graphState) }
    }
}

/**
 * Creates a PropertyChangeTracker that automatically tracks changes.
 *
 * @param undoRedoManager The undo/redo manager
 * @param graphState The graph state
 * @return A remembered PropertyChangeTracker instance
 */
@Composable
fun rememberPropertyChangeTracker(
    undoRedoManager: UndoRedoManager,
    graphState: GraphState
): PropertyChangeTracker {
    return remember(undoRedoManager, graphState) {
        PropertyChangeTracker(undoRedoManager, graphState)
    }
}

/**
 * State wrapper for tracking property changes during editing.
 *
 * This class helps manage property editing state while deferring
 * command creation until the edit is committed.
 *
 * @property nodeId The ID of the node being edited
 * @property propertyKey The property key being edited
 * @property initialValue The value when editing started
 */
class PropertyEditSession(
    val nodeId: String,
    val propertyKey: String,
    val initialValue: String
) {
    private var currentValue: String = initialValue

    /**
     * Updates the current value during editing.
     * Does not create undo commands until [commit] is called.
     */
    fun updateValue(value: String) {
        currentValue = value
    }

    /**
     * Gets the current value being edited.
     */
    fun getValue(): String = currentValue

    /**
     * Checks if the value has been modified from the initial value.
     */
    fun hasChanged(): Boolean = currentValue != initialValue

    /**
     * Commits the edit, creating an undo command if the value changed.
     *
     * @param tracker The property change tracker to record the change
     */
    fun commit(tracker: PropertyChangeTracker) {
        if (hasChanged()) {
            tracker.trackChange(nodeId, propertyKey, initialValue, currentValue)
        }
    }

    /**
     * Cancels the edit, reverting to the initial value.
     *
     * @return The initial value to restore in the UI
     */
    fun cancel(): String {
        currentValue = initialValue
        return initialValue
    }
}
