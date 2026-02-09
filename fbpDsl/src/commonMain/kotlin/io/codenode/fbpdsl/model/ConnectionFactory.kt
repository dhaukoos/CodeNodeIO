/*
 * ConnectionFactory - Factory for Creating Connection Instances
 * Provides convenience methods for connection creation with validation
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.datetime.Clock

/**
 * Utility for generating Connection IDs
 */
object ConnectionIdGenerator {
    /**
     * Generates a unique ID for a Connection
     *
     * @param prefix Optional prefix for the ID (default: "conn")
     * @return Unique identifier string
     */
    fun generateId(prefix: String = "conn"): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val random = (0..999999).random()
        return "${prefix}_${timestamp}_$random"
    }
}

/**
 * Factory for creating Connection instances with convenience methods
 */
object ConnectionFactory {
    /**
     * Creates a basic unbuffered connection between two ports
     *
     * @param sourceNodeId Source node ID
     * @param sourcePortId Source port ID
     * @param targetNodeId Target node ID
     * @param targetPortId Target port ID
     * @return New Connection instance
     */
    fun create(
        sourceNodeId: String,
        sourcePortId: String,
        targetNodeId: String,
        targetPortId: String
    ): Connection {
        return Connection(
            id = ConnectionIdGenerator.generateId(),
            sourceNodeId = sourceNodeId,
            sourcePortId = sourcePortId,
            targetNodeId = targetNodeId,
            targetPortId = targetPortId
        )
    }

    /**
     * Creates a buffered connection with specified capacity
     *
     * @param sourceNodeId Source node ID
     * @param sourcePortId Source port ID
     * @param targetNodeId Target node ID
     * @param targetPortId Target port ID
     * @param capacity Buffer size for the channel
     * @return New Connection instance
     */
    fun createBuffered(
        sourceNodeId: String,
        sourcePortId: String,
        targetNodeId: String,
        targetPortId: String,
        capacity: Int
    ): Connection {
        require(capacity > 0) { "Buffered connection must have capacity > 0" }
        return Connection(
            id = ConnectionIdGenerator.generateId(),
            sourceNodeId = sourceNodeId,
            sourcePortId = sourcePortId,
            targetNodeId = targetNodeId,
            targetPortId = targetPortId,
            channelCapacity = capacity
        )
    }

    /**
     * Creates a connection from port objects (validates compatibility)
     *
     * @param sourcePort Source port object
     * @param targetPort Target port object
     * @return New Connection instance if ports are compatible
     * @throws IllegalArgumentException if ports are incompatible
     */
    fun createFromPorts(
        sourcePort: Port<*>,
        targetPort: Port<*>
    ): Connection {
        // Validate port compatibility
        require(sourcePort.direction == Port.Direction.OUTPUT) {
            "Source port '${sourcePort.name}' must be OUTPUT direction"
        }
        require(targetPort.direction == Port.Direction.INPUT) {
            "Target port '${targetPort.name}' must be INPUT direction"
        }
        require(sourcePort.isCompatibleWith(targetPort)) {
            "Incompatible port types: source '${sourcePort.typeName}' cannot connect to target '${targetPort.typeName}'"
        }

        return Connection(
            id = ConnectionIdGenerator.generateId(),
            sourceNodeId = sourcePort.owningNodeId,
            sourcePortId = sourcePort.id,
            targetNodeId = targetPort.owningNodeId,
            targetPortId = targetPort.id
        )
    }
}
