/*
 * GraphEditorKeyboardHandler - Keyboard shortcut handling for the graph editor
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.RemoveNodeCommand
import io.codenode.grapheditor.state.UndoRedoManager

/**
 * Handles keyboard events for the graph editor.
 *
 * Currently supports:
 * - Delete/Backspace: Delete selected node or connection(s)
 *
 * @return true if the key event was consumed, false otherwise
 */
fun handleGraphEditorKeyEvent(
    keyEvent: KeyEvent,
    graphState: GraphState,
    undoRedoManager: UndoRedoManager,
    onStatusMessage: (String) -> Unit,
): Boolean {
    if (keyEvent.type == KeyEventType.KeyDown) {
        return when (keyEvent.key) {
            Key.Delete, Key.Backspace -> {
                val nodeId = graphState.selectedNodeId
                val connectionIds = graphState.selectedConnectionIds

                if (nodeId != null) {
                    val command = RemoveNodeCommand(nodeId)
                    undoRedoManager.execute(command, graphState)
                    graphState.selectNode(null)
                    onStatusMessage("Deleted node")
                    true
                } else if (connectionIds.isNotEmpty()) {
                    connectionIds.forEach { connectionId ->
                        graphState.removeConnection(connectionId)
                    }
                    graphState.clearSelection()
                    onStatusMessage("Deleted connection")
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
    return false
}
