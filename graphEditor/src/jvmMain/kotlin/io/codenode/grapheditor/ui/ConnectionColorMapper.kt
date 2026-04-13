/*
 * ConnectionColorMapper - Computes connection colors from IP type registry
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.flowgraphtypes.registry.IPTypeRegistry

/**
 * Holds computed connection color maps for the graph canvas.
 *
 * @property connectionColors Map of connection ID to color (for top-level and internal connections)
 * @property boundaryConnectionColors Map of boundary port ID to color (for GraphNode interior view)
 */
class ConnectionColorState(
    val connectionColors: Map<String, Color>,
    val boundaryConnectionColors: Map<String, Color>,
)

/**
 * Computes and remembers connection colors based on IP types.
 *
 * Derives colors from:
 * 1. Top-level flow graph connections
 * 2. Internal connections of the current GraphNode (for interior view)
 * 3. Boundary port colors from parent-level connections and internal port mappings
 */
@Composable
fun rememberConnectionColors(
    connections: List<Connection>,
    ipTypeRegistry: IPTypeRegistry,
    currentGraphNode: GraphNode?,
): ConnectionColorState {
    val connectionColors: Map<String, Color> = remember(
        connections,
        ipTypeRegistry,
        currentGraphNode?.id,
        currentGraphNode?.internalConnections
    ) {
        val colorMap = mutableMapOf<String, Color>()
        // Top-level connections
        connections.forEach { connection: Connection ->
            connection.ipTypeId?.let { typeId ->
                ipTypeRegistry.getById(typeId)?.let { ipType ->
                    val ipColor = ipType.color
                    colorMap[connection.id] = Color(
                        red = ipColor.red / 255f,
                        green = ipColor.green / 255f,
                        blue = ipColor.blue / 255f
                    )
                }
            }
        }
        // Internal connections of current GraphNode (for interior view)
        currentGraphNode?.internalConnections?.forEach { connection: Connection ->
            connection.ipTypeId?.let { typeId ->
                ipTypeRegistry.getById(typeId)?.let { ipType ->
                    val ipColor = ipType.color
                    colorMap[connection.id] = Color(
                        red = ipColor.red / 255f,
                        green = ipColor.green / 255f,
                        blue = ipColor.blue / 255f
                    )
                }
            }
        }
        colorMap
    }

    val boundaryConnectionColors: Map<String, Color> = remember(
        connections,
        ipTypeRegistry,
        currentGraphNode?.id,
        currentGraphNode?.internalConnections
    ) {
        if (currentGraphNode == null) {
            emptyMap()
        } else {
            val colorMap = mutableMapOf<String, Color>()
            // 1. Colors from parent-level connections (external wiring)
            connections.forEach { connection: Connection ->
                if (connection.targetNodeId == currentGraphNode.id) {
                    connection.ipTypeId?.let { typeId ->
                        ipTypeRegistry.getById(typeId)?.let { ipType ->
                            val ipColor = ipType.color
                            colorMap[connection.targetPortId] = Color(
                                red = ipColor.red / 255f,
                                green = ipColor.green / 255f,
                                blue = ipColor.blue / 255f
                            )
                        }
                    }
                }
                if (connection.sourceNodeId == currentGraphNode.id) {
                    connection.ipTypeId?.let { typeId ->
                        ipTypeRegistry.getById(typeId)?.let { ipType ->
                            val ipColor = ipType.color
                            colorMap[connection.sourcePortId] = Color(
                                red = ipColor.red / 255f,
                                green = ipColor.green / 255f,
                                blue = ipColor.blue / 255f
                            )
                        }
                    }
                }
            }
            // 2. Derive boundary port colors from internal connections via port mappings
            // This covers cases where the GraphNode isn't externally connected (e.g., instantiated templates)
            val childPortToIpType = mutableMapOf<String, String>()
            currentGraphNode.internalConnections.forEach { conn: Connection ->
                conn.ipTypeId?.let { typeId ->
                    childPortToIpType[conn.sourcePortId] = typeId
                    childPortToIpType[conn.targetPortId] = typeId
                }
            }
            for (port in currentGraphNode.inputPorts + currentGraphNode.outputPorts) {
                if (colorMap.containsKey(port.id)) continue
                val mapping = currentGraphNode.portMappings[port.name] ?: currentGraphNode.portMappings[port.id]
                if (mapping != null) {
                    // Find the child port ID that matches this mapping
                    val childNode = currentGraphNode.childNodes.find { it.id == mapping.childNodeId }
                    if (childNode != null) {
                        val childPort = (childNode.inputPorts + childNode.outputPorts).find { it.name == mapping.childPortName }
                        if (childPort != null) {
                            childPortToIpType[childPort.id]?.let { typeId ->
                                ipTypeRegistry.getById(typeId)?.let { ipType ->
                                    val ipColor = ipType.color
                                    colorMap[port.id] = Color(
                                        red = ipColor.red / 255f,
                                        green = ipColor.green / 255f,
                                        blue = ipColor.blue / 255f
                                    )
                                }
                            }
                        }
                    }
                }
                // Fallback: trace through port mapping to child port's actual type
                if (!colorMap.containsKey(port.id)) {
                    val fallbackMapping = currentGraphNode.portMappings[port.name] ?: currentGraphNode.portMappings[port.id]
                    if (fallbackMapping != null) {
                        val childNode = currentGraphNode.childNodes.find { it.id == fallbackMapping.childNodeId }
                        val childPort = childNode?.let { cn ->
                            (cn.inputPorts + cn.outputPorts).find { it.name == fallbackMapping.childPortName }
                        }
                        val typeName = childPort?.dataType?.simpleName
                        if (typeName != null && typeName != "Any") {
                            ipTypeRegistry.getByTypeName(typeName)?.let { ipType ->
                                val ipColor = ipType.color
                                colorMap[port.id] = Color(
                                    red = ipColor.red / 255f,
                                    green = ipColor.green / 255f,
                                    blue = ipColor.blue / 255f
                                )
                            }
                        }
                    }
                }
            }
            colorMap
        }
    }

    return ConnectionColorState(
        connectionColors = connectionColors,
        boundaryConnectionColors = boundaryConnectionColors,
    )
}
