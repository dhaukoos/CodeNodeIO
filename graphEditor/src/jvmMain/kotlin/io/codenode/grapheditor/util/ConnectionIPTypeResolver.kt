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
 */
fun resolveConnectionIPTypes(
    graph: FlowGraph,
    ipTypeRegistry: IPTypeRegistry
): FlowGraph {
    val resolvedConnections = graph.connections.map { conn: Connection ->
        if (conn.ipTypeId != null) return@map conn

        val sourceNode = graph.findNode(conn.sourceNodeId)
        val sourcePort = sourceNode?.outputPorts?.find { it.id == conn.sourcePortId }
        if (sourcePort != null) {
            val typeName = sourcePort.dataType.simpleName ?: return@map conn
            val ipType = ipTypeRegistry.getByTypeName(typeName)
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
