/*
 * Node - Base Entity for FBP Graph Components
 * Abstract class representing a unit of processing or organizational container
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable

/**
 * Node represents a component in the FBP graph.
 * Base entity for both terminal processing nodes (CodeNode) and
 * hierarchical container nodes (GraphNode).
 *
 * Based on FBP principles, nodes are the processing units connected
 * by ports and connections to form flow-based programs.
 *
 * @property id Unique identifier for this node
 * @property name Human-readable name
 * @property nodeType String descriptor of node type (e.g., "Transformer", "Validator", "APIEndpoint")
 * @property description Optional documentation for this node
 * @property position Visual canvas position (x, y coordinates in pixels)
 * @property inputPorts List of INPUT ports for receiving data
 * @property outputPorts List of OUTPUT ports for emitting data
 * @property configuration Key-value property map for node-specific settings
 * @property parentNodeId Optional reference to parent GraphNode (null for root-level nodes)
 */
@Serializable
sealed class Node {
    abstract val id: String
    abstract val name: String
    abstract val nodeType: String
    abstract val description: String?
    abstract val position: Position
    abstract val inputPorts: List<Port<*>>
    abstract val outputPorts: List<Port<*>>
    abstract val configuration: Map<String, String>
    abstract val parentNodeId: String?

    /**
     * Position on the visual canvas
     *
     * @property x Horizontal coordinate (pixels from left)
     * @property y Vertical coordinate (pixels from top)
     */
    @Serializable
    data class Position(
        val x: Double,
        val y: Double
    ) {
        init {
            require(x >= 0.0) { "Position x must be non-negative, got $x" }
            require(y >= 0.0) { "Position y must be non-negative, got $y" }
        }

        companion object {
            /** Default position at origin */
            val ORIGIN = Position(0.0, 0.0)
        }
    }

    /**
     * Gets all ports (both input and output) for this node
     *
     * @return Combined list of all ports
     */
    fun getAllPorts(): List<Port<*>> = inputPorts + outputPorts

    /**
     * Finds a port by name
     *
     * @param name The port name to search for
     * @return The port if found, null otherwise
     */
    fun findPort(name: String): Port<*>? {
        return getAllPorts().find { it.name == name }
    }

    /**
     * Finds an input port by name
     *
     * @param name The port name to search for
     * @return The input port if found, null otherwise
     */
    fun findInputPort(name: String): Port<*>? {
        return inputPorts.find { it.name == name }
    }

    /**
     * Finds an output port by name
     *
     * @param name The port name to search for
     * @return The output port if found, null otherwise
     */
    fun findOutputPort(name: String): Port<*>? {
        return outputPorts.find { it.name == name }
    }

    /**
     * Validates that this node is well-formed
     *
     * @return Validation result with success flag and error messages
     */
    open fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate basic attributes
        if (id.isBlank()) {
            errors.add("Node ID cannot be blank")
        }
        if (name.isBlank()) {
            errors.add("Node name cannot be blank")
        }
        if (nodeType.isBlank()) {
            errors.add("Node type cannot be blank")
        }

        // At least one port must exist
        if (inputPorts.isEmpty() && outputPorts.isEmpty()) {
            errors.add("Node must have at least one port (input or output)")
        }

        // Validate all ports
        getAllPorts().forEach { port ->
            val portValidation = port.isValid()
            if (!portValidation) {
                errors.add("Invalid port '${port.name}': port validation failed")
            }

            // Verify port ownership
            if (port.owningNodeId != id) {
                errors.add("Port '${port.name}' has incorrect owningNodeId: expected '$id', got '${port.owningNodeId}'")
            }
        }

        // Validate port name uniqueness
        val portNames = getAllPorts().map { it.name }
        val duplicates = portNames.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            errors.add("Duplicate port names: ${duplicates.keys.joinToString(", ")}")
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Gets a configuration value by key
     *
     * @param key Configuration key
     * @return Configuration value or null if not found
     */
    fun getConfig(key: String): String? = configuration[key]

    /**
     * Checks if this node is a root-level node (no parent)
     *
     * @return true if node has no parent
     */
    fun isRoot(): Boolean = parentNodeId == null

    /**
     * Creates a copy of this node at a new position
     *
     * @param newPosition The new position
     * @return New node instance at the new position
     */
    abstract fun withPosition(newPosition: Position): Node

    /**
     * Creates a copy of this node with a new parent
     *
     * @param newParentId The new parent node ID
     * @return New node instance with updated parent
     */
    abstract fun withParent(newParentId: String?): Node

}

/**
 * Utility for generating Node IDs
 */
object NodeIdGenerator {
    /**
     * Generates a unique ID for a Node
     *
     * @param prefix Optional prefix for the ID (default: "node")
     * @return Unique identifier string
     */
    fun generateId(prefix: String = "node"): String {
        val timestamp = System.currentTimeMillis()
        val random = (0..999999).random()
        return "${prefix}_${timestamp}_$random"
    }
}
