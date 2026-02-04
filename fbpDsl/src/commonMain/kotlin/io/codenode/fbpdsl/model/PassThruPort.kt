/*
 * PassThruPort - Boundary Port for GraphNode Data Flow
 * Bridges connections between interior and exterior scopes of a GraphNode
 * Uses composition pattern to wrap underlying Port
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.reflect.KClass

/**
 * PassThruPort represents a specialized port type used exclusively on GraphNode boundaries
 * to bridge connections between interior and exterior scopes.
 *
 * Uses composition pattern to wrap an underlying Port, delegating common properties
 * while adding upstream/downstream references for boundary-crossing connections.
 *
 * For INPUT PassThruPort:
 * - upstream is the external node/port (outside the GraphNode)
 * - downstream is the internal node/port (inside the GraphNode)
 *
 * For OUTPUT PassThruPort:
 * - upstream is the internal node/port (inside the GraphNode)
 * - downstream is the external node/port (outside the GraphNode)
 *
 * @param T The type of data this port accepts/emits
 * @property port The underlying port representation (provides id, name, direction, dataType, owningNodeId)
 * @property upstreamNodeId Node ID on the upstream (source) side of the data flow
 * @property upstreamPortId Port ID on the upstream side
 * @property downstreamNodeId Node ID on the downstream (sink) side of the data flow
 * @property downstreamPortId Port ID on the downstream side
 *
 * @sample
 * ```kotlin
 * // Create an INPUT PassThruPort (external -> internal)
 * val inputPassThru = PassThruPort(
 *     port = Port(
 *         id = "passthru-in-1",
 *         name = "data_in",
 *         direction = Port.Direction.INPUT,
 *         dataType = String::class,
 *         owningNodeId = "graphNode-1"
 *     ),
 *     upstreamNodeId = "external-source",
 *     upstreamPortId = "output",
 *     downstreamNodeId = "internal-processor",
 *     downstreamPortId = "input"
 * )
 * ```
 */
data class PassThruPort<T : Any>(
    val port: Port<T>,
    val upstreamNodeId: String,
    val upstreamPortId: String,
    val downstreamNodeId: String,
    val downstreamPortId: String
) {
    // ==================== Delegated Properties ====================

    /**
     * Unique identifier for this port (delegated from underlying port)
     */
    val id: String get() = port.id

    /**
     * Human-readable name for this port (delegated from underlying port)
     */
    val name: String get() = port.name

    /**
     * Direction of data flow - INPUT or OUTPUT (delegated from underlying port)
     */
    val direction: Port.Direction get() = port.direction

    /**
     * The KClass representing the expected data type (delegated from underlying port)
     */
    val dataType: KClass<T> get() = port.dataType

    /**
     * Reference to the GraphNode that owns this port (delegated from underlying port)
     */
    val owningNodeId: String get() = port.owningNodeId

    /**
     * Gets the simple name of the data type (delegated from underlying port)
     */
    val typeName: String get() = port.typeName

    // ==================== PassThruPort-Specific Methods ====================

    /**
     * Returns true to indicate this is a PassThruPort.
     * Used for type checking without explicit instanceof checks.
     */
    fun isPassThruPort(): Boolean = true

    /**
     * Validates that this PassThruPort is well-formed
     *
     * @return true if port is valid, false otherwise
     */
    fun isValid(): Boolean {
        return port.isValid() &&
                upstreamNodeId.isNotBlank() &&
                upstreamPortId.isNotBlank() &&
                downstreamNodeId.isNotBlank() &&
                downstreamPortId.isNotBlank()
    }

    /**
     * Checks if this port is an INPUT PassThruPort (receives data from outside GraphNode)
     */
    fun isInput(): Boolean = direction == Port.Direction.INPUT

    /**
     * Checks if this port is an OUTPUT PassThruPort (sends data outside GraphNode)
     */
    fun isOutput(): Boolean = direction == Port.Direction.OUTPUT

    /**
     * Gets a human-readable description of this PassThruPort
     *
     * @return String describing the port flow
     */
    fun getDescription(): String {
        return "PassThruPort[$name]: $upstreamNodeId:$upstreamPortId -> $downstreamNodeId:$downstreamPortId"
    }
}
