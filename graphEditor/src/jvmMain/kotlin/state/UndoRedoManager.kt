/*
 * UndoRedoManager - Undo/Redo Support Using Command Pattern
 * Provides undo/redo functionality for graph editing operations
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import androidx.compose.runtime.*
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Connection

/**
 * Manages undo/redo operations for graph editing using the Command pattern
 * Maintains history stacks and executes/reverses commands
 *
 * @property maxHistorySize Maximum number of commands to keep in history (default: 50)
 */
class UndoRedoManager(
    private val maxHistorySize: Int = 50
) {
    private val undoStack = mutableStateListOf<Command>()
    private val redoStack = mutableStateListOf<Command>()

    /**
     * Whether undo is available
     */
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    /**
     * Whether redo is available
     */
    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * Number of commands in undo history
     */
    val undoCount: Int
        get() = undoStack.size

    /**
     * Number of commands in redo history
     */
    val redoCount: Int
        get() = redoStack.size

    /**
     * Executes a command and adds it to the undo stack
     *
     * @param command The command to execute
     * @param graphState The graph state to operate on
     */
    fun execute(command: Command, graphState: GraphState) {
        command.execute(graphState)
        undoStack.add(command)

        // Clear redo stack when a new command is executed
        redoStack.clear()

        // Limit history size
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
    }

    /**
     * Undoes the last command
     *
     * @param graphState The graph state to operate on
     * @return true if undo was successful, false if no command to undo
     */
    fun undo(graphState: GraphState): Boolean {
        if (undoStack.isEmpty()) return false

        val command = undoStack.removeLast()
        command.undo(graphState)
        redoStack.add(command)

        return true
    }

    /**
     * Redoes the last undone command
     *
     * @param graphState The graph state to operate on
     * @return true if redo was successful, false if no command to redo
     */
    fun redo(graphState: GraphState): Boolean {
        if (redoStack.isEmpty()) return false

        val command = redoStack.removeLast()
        command.execute(graphState)
        undoStack.add(command)

        return true
    }

    /**
     * Clears all undo/redo history
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    /**
     * Gets description of the next undo command
     */
    fun getUndoDescription(): String? {
        return undoStack.lastOrNull()?.description
    }

    /**
     * Gets description of the next redo command
     */
    fun getRedoDescription(): String? {
        return redoStack.lastOrNull()?.description
    }
}

/**
 * Base interface for undoable commands
 */
interface Command {
    /**
     * Human-readable description of the command
     */
    val description: String

    /**
     * Executes the command
     */
    fun execute(graphState: GraphState)

    /**
     * Reverses the command
     */
    fun undo(graphState: GraphState)
}

/**
 * Command to add a node to the graph
 */
class AddNodeCommand(
    private val node: Node,
    private val position: androidx.compose.ui.geometry.Offset
) : Command {
    override val description = "Add node '${node.name}'"

    override fun execute(graphState: GraphState) {
        graphState.addNode(node, position)
    }

    override fun undo(graphState: GraphState) {
        graphState.removeNode(node.id)
    }
}

/**
 * Command to remove a node from the graph
 */
class RemoveNodeCommand(
    private val nodeId: String
) : Command {
    private var removedNode: Node? = null
    private var removedConnections: List<Connection> = emptyList()

    override val description = "Remove node"

    override fun execute(graphState: GraphState) {
        // Store the node and its connections before removing
        removedNode = graphState.flowGraph.findNode(nodeId)
        removedConnections = graphState.flowGraph.getConnectionsForNode(nodeId)

        graphState.removeNode(nodeId)
    }

    override fun undo(graphState: GraphState) {
        // Restore the node
        removedNode?.let { node ->
            graphState.addNode(node, androidx.compose.ui.geometry.Offset.Zero)
        }

        // Restore the connections
        removedConnections.forEach { connection ->
            graphState.addConnection(connection)
        }
    }
}

/**
 * Command to move a node
 */
class MoveNodeCommand(
    private val nodeId: String,
    private val oldPosition: androidx.compose.ui.geometry.Offset,
    private val newPosition: androidx.compose.ui.geometry.Offset
) : Command {
    override val description = "Move node"

    override fun execute(graphState: GraphState) {
        graphState.updateNodePosition(nodeId, newPosition.x.toDouble(), newPosition.y.toDouble())
    }

    override fun undo(graphState: GraphState) {
        graphState.updateNodePosition(nodeId, oldPosition.x.toDouble(), oldPosition.y.toDouble())
    }
}

/**
 * Command to add a connection
 */
class AddConnectionCommand(
    private val connection: Connection
) : Command {
    override val description = "Add connection"

    override fun execute(graphState: GraphState) {
        graphState.addConnection(connection)
    }

    override fun undo(graphState: GraphState) {
        graphState.removeConnection(connection.id)
    }
}

/**
 * Command to remove a connection
 */
class RemoveConnectionCommand(
    private val connectionId: String
) : Command {
    private var removedConnection: Connection? = null

    override val description = "Remove connection"

    override fun execute(graphState: GraphState) {
        removedConnection = graphState.flowGraph.findConnection(connectionId)
        graphState.removeConnection(connectionId)
    }

    override fun undo(graphState: GraphState) {
        removedConnection?.let { connection ->
            graphState.addConnection(connection)
        }
    }
}

/**
 * Command to update the entire graph
 */
class UpdateGraphCommand(
    private val oldGraph: FlowGraph,
    private val newGraph: FlowGraph
) : Command {
    override val description = "Update graph"

    override fun execute(graphState: GraphState) {
        graphState.setGraph(newGraph)
    }

    override fun undo(graphState: GraphState) {
        graphState.setGraph(oldGraph)
    }
}

/**
 * Composite command that groups multiple commands together
 */
class CompositeCommand(
    private val commands: List<Command>,
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
 * Command to group selected nodes into a GraphNode
 * Captures both before and after states to enable proper undo/redo
 */
class GroupNodesCommand(
    private val selectedNodeIds: Set<String>
) : Command {
    // State captured during first execute for undo/redo
    private var beforeGraph: FlowGraph? = null
    private var afterGraph: FlowGraph? = null
    private var isFirstExecution = true

    override val description = "Group ${selectedNodeIds.size} nodes"

    override fun execute(graphState: GraphState) {
        if (isFirstExecution) {
            // First execution: capture before state, perform grouping, capture after state
            beforeGraph = graphState.flowGraph
            graphState.groupSelectedNodes()
            afterGraph = graphState.flowGraph
            isFirstExecution = false
        } else {
            // Redo: restore the grouped state
            afterGraph?.let { graph ->
                graphState.setGraph(graph)
            }
        }
    }

    override fun undo(graphState: GraphState) {
        // Restore the original ungrouped graph
        beforeGraph?.let { graph ->
            graphState.setGraph(graph)
        }
    }
}

/**
 * Command to ungroup a GraphNode back into its constituent nodes
 * Captures both before and after states to enable proper undo/redo
 */
class UngroupNodeCommand(
    private val graphNodeId: String
) : Command {
    // State captured during first execute for undo/redo
    private var beforeGraph: FlowGraph? = null
    private var afterGraph: FlowGraph? = null
    private var isFirstExecution = true

    override val description = "Ungroup node"

    override fun execute(graphState: GraphState) {
        if (isFirstExecution) {
            // First execution: capture before state, perform ungrouping, capture after state
            beforeGraph = graphState.flowGraph
            graphState.ungroupGraphNode(graphNodeId)
            afterGraph = graphState.flowGraph
            isFirstExecution = false
        } else {
            // Redo: restore the ungrouped state
            afterGraph?.let { graph ->
                graphState.setGraph(graph)
            }
        }
    }

    override fun undo(graphState: GraphState) {
        // Restore the original grouped graph
        beforeGraph?.let { graph ->
            graphState.setGraph(graph)
        }
    }
}

/**
 * Creates a remembered UndoRedoManager instance
 *
 * @param maxHistorySize Maximum number of commands in history
 * @return A remembered UndoRedoManager
 */
@Composable
fun rememberUndoRedoManager(maxHistorySize: Int = 50): UndoRedoManager {
    return remember(maxHistorySize) {
        UndoRedoManager(maxHistorySize)
    }
}
