/*
 * PassThruPortFactory - Factory for creating PassThruPorts
 * Creates PassThruPorts for boundary-crossing connections on GraphNodes
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.factory

import io.codenode.fbpdsl.model.PassThruPort
import io.codenode.fbpdsl.model.Port
import kotlin.reflect.KClass

/**
 * Factory for creating PassThruPorts for boundary-crossing connections.
 *
 * PassThruPorts are created when grouping nodes results in connections
 * that cross the GraphNode boundary. They serve as bridge ports between
 * the interior and exterior of a GraphNode.
 *
 * For INPUT PassThruPort:
 * - upstream is the external node/port (outside the GraphNode)
 * - downstream is the internal node/port (inside the GraphNode)
 *
 * For OUTPUT PassThruPort:
 * - upstream is the internal node/port (inside the GraphNode)
 * - downstream is the external node/port (outside the GraphNode)
 */
object PassThruPortFactory {

    /**
     * Creates a PassThruPort for a connection crossing a GraphNode boundary.
     *
     * @param graphNodeId The GraphNode that will own this port
     * @param upstreamNodeId Node on the upstream (source) side of the data flow
     * @param upstreamPortId Port on the upstream side
     * @param downstreamNodeId Node on the downstream (sink) side of the data flow
     * @param downstreamPortId Port on the downstream side
     * @param direction Direction of the port (INPUT or OUTPUT)
     * @param dataType The data type for the port
     * @return Result containing PassThruPort or validation errors
     */
    fun <T : Any> create(
        graphNodeId: String,
        upstreamNodeId: String,
        upstreamPortId: String,
        downstreamNodeId: String,
        downstreamPortId: String,
        direction: Port.Direction,
        dataType: KClass<T>
    ): Result<PassThruPort<T>> {
        // Validate required parameters are not blank
        val validationError = validateParameters(
            graphNodeId = graphNodeId,
            upstreamNodeId = upstreamNodeId,
            upstreamPortId = upstreamPortId,
            downstreamNodeId = downstreamNodeId,
            downstreamPortId = downstreamPortId
        )
        if (validationError != null) {
            return Result.failure(IllegalArgumentException(validationError))
        }

        // Generate unique port ID
        val portId = generatePortId(direction, upstreamPortId, downstreamPortId)

        // Generate meaningful port name
        val portName = generatePortName(direction, upstreamPortId, downstreamPortId)

        // Create the underlying Port
        val underlyingPort = Port(
            id = portId,
            name = portName,
            direction = direction,
            dataType = dataType,
            owningNodeId = graphNodeId
        )

        // Create the PassThruPort
        val passThruPort = PassThruPort(
            port = underlyingPort,
            upstreamNodeId = upstreamNodeId,
            upstreamPortId = upstreamPortId,
            downstreamNodeId = downstreamNodeId,
            downstreamPortId = downstreamPortId
        )

        return Result.success(passThruPort)
    }

    /**
     * Creates a PassThruPort from existing port information during grouping.
     *
     * @param graphNodeId The GraphNode that will own this port
     * @param externalNodeId The node outside the GraphNode boundary
     * @param externalPortId The port on the external node
     * @param internalNodeId The node inside the GraphNode
     * @param internalPortId The port on the internal node
     * @param direction Direction of the port (INPUT or OUTPUT)
     * @param dataType The data type for the port (defaults to Any)
     * @return Result containing PassThruPort or validation errors
     */
    fun createFromBoundaryCrossing(
        graphNodeId: String,
        externalNodeId: String,
        externalPortId: String,
        internalNodeId: String,
        internalPortId: String,
        direction: Port.Direction,
        dataType: KClass<*> = Any::class
    ): Result<PassThruPort<Any>> {
        // For INPUT: external is upstream, internal is downstream
        // For OUTPUT: internal is upstream, external is downstream
        val (upstreamNodeId, upstreamPortId, downstreamNodeId, downstreamPortId) = when (direction) {
            Port.Direction.INPUT -> listOf(externalNodeId, externalPortId, internalNodeId, internalPortId)
            Port.Direction.OUTPUT -> listOf(internalNodeId, internalPortId, externalNodeId, externalPortId)
        }

        @Suppress("UNCHECKED_CAST")
        return create(
            graphNodeId = graphNodeId,
            upstreamNodeId = upstreamNodeId,
            upstreamPortId = upstreamPortId,
            downstreamNodeId = downstreamNodeId,
            downstreamPortId = downstreamPortId,
            direction = direction,
            dataType = Any::class
        ) as Result<PassThruPort<Any>>
    }

    /**
     * Validates that all required parameters are non-blank.
     *
     * @return Error message if validation fails, null if valid
     */
    private fun validateParameters(
        graphNodeId: String,
        upstreamNodeId: String,
        upstreamPortId: String,
        downstreamNodeId: String,
        downstreamPortId: String
    ): String? {
        if (graphNodeId.isBlank()) {
            return "graphNodeId cannot be blank"
        }
        if (upstreamNodeId.isBlank()) {
            return "upstreamNodeId cannot be blank"
        }
        if (upstreamPortId.isBlank()) {
            return "upstreamPortId cannot be blank"
        }
        if (downstreamNodeId.isBlank()) {
            return "downstreamNodeId cannot be blank"
        }
        if (downstreamPortId.isBlank()) {
            return "downstreamPortId cannot be blank"
        }
        return null
    }

    /**
     * Generates a unique port ID for the PassThruPort.
     */
    private fun generatePortId(
        direction: Port.Direction,
        upstreamPortId: String,
        downstreamPortId: String
    ): String {
        val prefix = when (direction) {
            Port.Direction.INPUT -> "passthru_in"
            Port.Direction.OUTPUT -> "passthru_out"
        }
        val timestamp = System.nanoTime()
        val portRef = if (direction == Port.Direction.INPUT) downstreamPortId else upstreamPortId
        return "${prefix}_${portRef}_$timestamp"
    }

    /**
     * Generates a meaningful port name for the PassThruPort.
     */
    private fun generatePortName(
        direction: Port.Direction,
        upstreamPortId: String,
        downstreamPortId: String
    ): String {
        return when (direction) {
            Port.Direction.INPUT -> "in_$downstreamPortId"
            Port.Direction.OUTPUT -> "out_$upstreamPortId"
        }
    }
}
