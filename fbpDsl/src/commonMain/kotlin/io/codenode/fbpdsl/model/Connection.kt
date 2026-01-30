/*
 * Connection - Data Flow Link Between Ports
 * Represents edge in FBP graph, implemented as channels in Kotlin/Go
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable

/**
 * Connection represents a data flow link between two ports in the FBP graph.
 * Connections are the edges that link nodes together, implemented as channels
 * in Kotlin (using coroutines) or Go (using goroutines).
 *
 * Based on FBP principles and the Single Responsibility Principle, connections:
 * - Link exactly one source OUTPUT port to one target INPUT port
 * - Carry InformationPackets from source to target without modification
 * - Can buffer packets based on channelCapacity
 * - Enforce type compatibility between connected ports
 *
 * Note: Data transformation should be handled by dedicated transformer nodes,
 * not by connections. This keeps the Connection class focused on its single
 * responsibility: linking ports and managing data flow.
 *
 * @property id Unique identifier for this connection
 * @property sourceNodeId Reference to the source Node
 * @property sourcePortId Reference to the source OUTPUT Port
 * @property targetNodeId Reference to the target Node
 * @property targetPortId Reference to the target INPUT Port
 * @property channelCapacity Buffer size for the channel (0 = unbuffered, default)
 * @property parentScopeId Optional reference to parent GraphNode or FlowGraph
 * @property ipTypeId Optional reference to InformationPacketType for typed connections
 *
 * @sample
 * ```kotlin
 * val connection = Connection(
 *     id = "conn_123",
 *     sourceNodeId = "node_1",
 *     sourcePortId = "port_out",
 *     targetNodeId = "node_2",
 *     targetPortId = "port_in",
 *     channelCapacity = 10  // Buffer up to 10 packets
 * )
 * ```
 */
@Serializable
data class Connection(
    val id: String,
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String,
    val channelCapacity: Int = 0,
    val parentScopeId: String? = null,
    val ipTypeId: String? = null
) {

    init {
        require(id.isNotBlank()) { "Connection ID cannot be blank" }
        require(sourceNodeId.isNotBlank()) { "Source node ID cannot be blank" }
        require(sourcePortId.isNotBlank()) { "Source port ID cannot be blank" }
        require(targetNodeId.isNotBlank()) { "Target node ID cannot be blank" }
        require(targetPortId.isNotBlank()) { "Target port ID cannot be blank" }
        require(channelCapacity >= 0) { "Channel capacity cannot be negative, got $channelCapacity" }
    }

    /**
     * Validates that this connection is well-formed
     *
     * Note: This method performs basic structural validation.
     * Full validation (port direction, type compatibility, cycle detection)
     * requires access to the full graph context and should be performed
     * at the FlowGraph or GraphNode level.
     *
     * @return Validation result with success flag and error messages
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate basic attributes
        if (id.isBlank()) {
            errors.add("Connection ID cannot be blank")
        }
        if (sourceNodeId.isBlank()) {
            errors.add("Source node ID cannot be blank")
        }
        if (sourcePortId.isBlank()) {
            errors.add("Source port ID cannot be blank")
        }
        if (targetNodeId.isBlank()) {
            errors.add("Target node ID cannot be blank")
        }
        if (targetPortId.isBlank()) {
            errors.add("Target port ID cannot be blank")
        }

        // Validate channel capacity
        if (channelCapacity < 0) {
            errors.add("Channel capacity cannot be negative, got $channelCapacity")
        }

        // Cannot connect a node to itself on the same port
        if (sourceNodeId == targetNodeId && sourcePortId == targetPortId) {
            errors.add("Cannot create self-loop connection on same port")
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Validates this connection with access to actual port objects
     *
     * This performs full validation including port direction and type compatibility.
     *
     * @param sourcePort The source port object
     * @param targetPort The target port object
     * @return Validation result with success flag and error messages
     */
    fun validateWithPorts(sourcePort: Port<*>, targetPort: Port<*>): ValidationResult {
        val errors = mutableListOf<String>()

        // First run basic validation
        val basicValidation = validate()
        errors.addAll(basicValidation.errors)

        // Verify port IDs match
        if (sourcePort.id != sourcePortId) {
            errors.add("Source port ID mismatch: expected '$sourcePortId', got '${sourcePort.id}'")
        }
        if (targetPort.id != targetPortId) {
            errors.add("Target port ID mismatch: expected '$targetPortId', got '${targetPort.id}'")
        }

        // Verify source port is OUTPUT
        if (sourcePort.direction != Port.Direction.OUTPUT) {
            errors.add("Source port '${sourcePort.name}' must be OUTPUT direction, got ${sourcePort.direction}")
        }

        // Verify target port is INPUT
        if (targetPort.direction != Port.Direction.INPUT) {
            errors.add("Target port '${targetPort.name}' must be INPUT direction, got ${targetPort.direction}")
        }

        // Verify type compatibility
        if (!sourcePort.isCompatibleWith(targetPort)) {
            errors.add("Incompatible port types: source port '${sourcePort.name}' type '${sourcePort.typeName}' cannot connect to target port '${targetPort.name}' type '${targetPort.typeName}'")
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Creates a copy of this connection with a new channel capacity
     *
     * @param capacity The new buffer size
     * @return New Connection instance with updated capacity
     */
    fun withChannelCapacity(capacity: Int): Connection {
        require(capacity >= 0) { "Channel capacity cannot be negative" }
        return copy(channelCapacity = capacity)
    }

    /**
     * Creates a copy of this connection with a new parent scope
     *
     * @param scopeId The new parent GraphNode or FlowGraph ID
     * @return New Connection instance with updated parent
     */
    fun withParentScope(scopeId: String?): Connection {
        return copy(parentScopeId = scopeId)
    }

    /**
     * Checks if this connection is buffered (capacity > 0)
     *
     * @return true if channelCapacity is greater than 0
     */
    fun isBuffered(): Boolean = channelCapacity > 0

    /**
     * Checks if this connection is unbuffered (capacity = 0)
     *
     * @return true if channelCapacity is 0
     */
    fun isUnbuffered(): Boolean = channelCapacity == 0

    /**
     * Checks if this connection links two different nodes
     *
     * @return true if source and target node IDs are different
     */
    fun connectsDifferentNodes(): Boolean = sourceNodeId != targetNodeId

    /**
     * Checks if this connection is a self-loop (connects a node to itself)
     *
     * @return true if source and target node IDs are the same
     */
    fun isSelfLoop(): Boolean = sourceNodeId == targetNodeId

    /**
     * Gets a human-readable description of this connection
     *
     * @return String describing the connection flow
     */
    fun getDescription(): String {
        return "$sourceNodeId:$sourcePortId -> $targetNodeId:$targetPortId"
    }

    /**
     * Checks if this connection belongs to a parent scope
     *
     * @return true if parentScopeId is not null
     */
    fun hasParentScope(): Boolean = parentScopeId != null

    /**
     * Checks if this connection is at root level (no parent scope)
     *
     * @return true if parentScopeId is null
     */
    fun isRootLevel(): Boolean = parentScopeId == null

    /**
     * Creates a copy of this connection with a new IP type
     *
     * @param typeId The InformationPacketType ID to assign (or null to clear)
     * @return New Connection instance with updated ipTypeId
     */
    fun withIPType(typeId: String?): Connection {
        return copy(ipTypeId = typeId)
    }

    /**
     * Checks if this connection has an assigned IP type
     *
     * @return true if ipTypeId is not null
     */
    fun hasIPType(): Boolean = ipTypeId != null

    /**
     * Checks if this connection is untyped (no IP type assigned)
     *
     * @return true if ipTypeId is null
     */
    fun isUntyped(): Boolean = ipTypeId == null
}
