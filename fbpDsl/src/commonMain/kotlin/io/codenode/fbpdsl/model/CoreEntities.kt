/*
 * CodeNodeIO IDE Plugin Platform
 * Core Flow-Based Programming domain model
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlinx.serialization.Serializable
import java.util.*

/**
 * InformationPacket (IP)
 *
 * Represents data flowing through the FBP graph. IPs are immutable data carriers
 * that move between nodes through connections.
 */
@Serializable
data class InformationPacket(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val payload: String, // JSON-serialized payload
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Port
 *
 * Entry or exit point on a Node for data flow. Ports define how nodes can be
 * connected and what types of data they accept/emit.
 */
@Serializable
data class Port(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val direction: PortDirection,
    val dataType: String,
    val required: Boolean = false,
    val defaultValue: String? = null,
    val owningNodeId: String
)

enum class PortDirection {
    INPUT, OUTPUT
}

/**
 * CodeNode
 *
 * Core entity representing a component in the FBP graph.
 */
@Serializable
data class CodeNode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val description: String? = null,
    val ports: List<Port> = emptyList(),
    val config: Map<String, String> = emptyMap()
)

/**
 * Connection
 *
 * Link between two ports representing data flow.
 */
@Serializable
data class Connection(
    val id: String = UUID.randomUUID().toString(),
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String
)

/**
 * FlowGraph
 *
 * Complete directed acyclic graph of nodes and connections.
 */
@Serializable
data class FlowGraph(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nodes: List<CodeNode> = emptyList(),
    val connections: List<Connection> = emptyList()
)

