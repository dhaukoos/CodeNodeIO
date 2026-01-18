/*
 * InformationPacket (IP) - Core FBP Data Carrier
 * Represents immutable data flowing through the FBP graph
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * InformationPacket (IP) represents data flowing through the FBP graph.
 * IPs are immutable data carriers that move between nodes through connections.
 *
 * Based on J. Paul Morrison's Flow-based Programming principles, where IPs
 * are discrete units of data that maintain identity as they flow through the system.
 *
 * @property id Unique identifier for this IP instance
 * @property type Data type descriptor (e.g., "String", "Number", "User", "ApiResponse")
 * @property payload The actual data content (polymorphic - can be primitive or complex object)
 * @property metadata Optional metadata (source node, timestamp, trace ID for debugging)
 */
@Serializable
data class InformationPacket(
    val id: String,
    val type: String,
    val payload: JsonElement,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * State of the Information Packet in its lifecycle
     */
    enum class State {
        /** IP has been created but not yet sent */
        CREATED,

        /** IP is currently in transit through a connection */
        IN_TRANSIT,

        /** IP has been consumed by a target node */
        CONSUMED,

        /** IP has been archived for debugging/replay */
        ARCHIVED
    }

    /**
     * Validates that this IP is well-formed and ready for transmission
     *
     * @return true if IP is valid, false otherwise
     */
    fun isValid(): Boolean {
        return id.isNotBlank() && type.isNotBlank()
    }

    /**
     * Creates a copy of this IP with updated metadata
     *
     * @param additionalMetadata New metadata to merge with existing
     * @return New IP with combined metadata
     */
    fun withMetadata(additionalMetadata: Map<String, String>): InformationPacket {
        return copy(metadata = metadata + additionalMetadata)
    }

    /**
     * Adds a trace entry to track IP flow through the graph
     *
     * @param nodeId ID of the node processing this IP
     * @param timestamp Processing timestamp
     * @return New IP with trace entry added
     */
    fun addTrace(nodeId: String, timestamp: Long = System.currentTimeMillis()): InformationPacket {
        val traceEntry = "node:$nodeId,time:$timestamp"
        val traces = metadata["traces"]?.let { "$it;$traceEntry" } ?: traceEntry
        return withMetadata(mapOf("traces" to traces))
    }

}

/**
 * Factory for creating InformationPacket instances
 */
object InformationPacketFactory {
    /**
     * Creates a simple InformationPacket with primitive payload
     *
     * @param type Data type descriptor
     * @param payload The data content
     * @return New InformationPacket instance
     */
    fun create(type: String, payload: JsonElement): InformationPacket {
        return InformationPacket(
            id = generateId(),
            type = type,
            payload = payload
        )
    }

    /**
     * Generates a unique ID for an InformationPacket
     * Using timestamp + random to ensure uniqueness
     *
     * @return Unique identifier string
     */
    private fun generateId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (0..999999).random()
        return "ip_${timestamp}_$random"
    }
}
