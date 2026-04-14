/*
 * ConnectionIPTypeResolver - Auto-resolves connection IP types from port data types
 * License: Apache 2.0
 */

package io.codenode.grapheditor.util

import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.flowgraphtypes.registry.IPTypeRegistry

/**
 * Resolves connection IP types from source port data types.
 * For each connection without an ipTypeId, looks up the source port's dataType
 * and finds the matching InformationPacketType in the registry.
 *
 * @param portTypeNameHints Maps portId → original type name for ports where KClass
 *        resolution fell back to Any::class (e.g., typealias IP types). Used as fallback
 *        when dataType.simpleName is "Any".
 */
fun resolveConnectionIPTypes(
    graph: FlowGraph,
    ipTypeRegistry: IPTypeRegistry,
    portTypeNameHints: Map<String, String> = emptyMap()
): FlowGraph {
    val resolvedConnections = graph.connections.map { conn: Connection ->
        if (conn.ipTypeId != null) return@map conn

        val sourceNode = graph.findNode(conn.sourceNodeId)
        val sourcePort = sourceNode?.outputPorts?.find { it.id == conn.sourcePortId }
        if (sourcePort != null) {
            val typeName = sourcePort.dataType.simpleName ?: return@map conn
            // Use hint if dataType resolved to Any (e.g., typealias IP types)
            val effectiveTypeName = if (typeName == "Any") {
                portTypeNameHints[sourcePort.id] ?: return@map conn
            } else {
                typeName
            }
            val ipType = ipTypeRegistry.getByTypeName(effectiveTypeName)
            if (ipType != null) {
                Connection(
                    id = conn.id,
                    sourceNodeId = conn.sourceNodeId,
                    sourcePortId = conn.sourcePortId,
                    targetNodeId = conn.targetNodeId,
                    targetPortId = conn.targetPortId,
                    channelCapacity = conn.channelCapacity,
                    parentScopeId = conn.parentScopeId,
                    ipTypeId = ipType.id
                )
            } else {
                conn
            }
        } else {
            conn
        }
    }

    return if (resolvedConnections != graph.connections) {
        var result = graph
        graph.connections.forEach { c: Connection -> result = result.removeConnection(c.id) }
        resolvedConnections.forEach { c: Connection -> result = result.addConnection(c) }
        result
    } else {
        graph
    }
}
