/*
 * SelectableElement - Unified Selection Model for Graph Editor
 * Represents any element that can be selected in the graph editor
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

/**
 * Sealed interface representing an element that can be selected in the graph editor.
 * This unifies selection handling for nodes and connections.
 */
sealed interface SelectableElement {
    val id: String

    /**
     * Represents a selected node
     */
    data class Node(override val id: String) : SelectableElement

    /**
     * Represents a selected connection
     */
    data class Connection(override val id: String) : SelectableElement
}

/**
 * Extension function to get all node IDs from a set of selectable elements
 */
fun Set<SelectableElement>.nodeIds(): Set<String> =
    filterIsInstance<SelectableElement.Node>().map { it.id }.toSet()

/**
 * Extension function to get all connection IDs from a set of selectable elements
 */
fun Set<SelectableElement>.connectionIds(): Set<String> =
    filterIsInstance<SelectableElement.Connection>().map { it.id }.toSet()

/**
 * Extension function to check if a node ID is in the selection
 */
fun Set<SelectableElement>.containsNode(nodeId: String): Boolean =
    any { it is SelectableElement.Node && it.id == nodeId }

/**
 * Extension function to check if a connection ID is in the selection
 */
fun Set<SelectableElement>.containsConnection(connectionId: String): Boolean =
    any { it is SelectableElement.Connection && it.id == connectionId }
