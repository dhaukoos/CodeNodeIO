/*
 * GraphNodeFactory - Factory for creating GraphNodes from selected nodes
 * Creates hierarchical GraphNodes with auto-generated port mappings
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.factory

import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port

/**
 * Factory for creating GraphNodes from a selection of nodes.
 * Handles auto-generation of port mappings based on external connections.
 */
object GraphNodeFactory {

    /**
     * Result of port mapping generation.
     * Describes a port that needs to be created on the GraphNode to expose
     * an internal node's port to external connections.
     */
    data class GeneratedPortMapping(
        val graphNodePortId: String,
        val graphNodePortName: String,
        val graphNodePortDirection: Port.Direction,
        val childNodeId: String,
        val childPortId: String,
        val externalConnectionId: String
    )

    /**
     * Creates a GraphNode from a selection of nodes.
     *
     * This method:
     * 1. Validates that at least 2 nodes are selected
     * 2. Collects the selected nodes and sets their parentNodeId
     * 3. Identifies internal connections (between selected nodes)
     * 4. Generates port mappings for external connections
     * 5. Creates input/output ports on the GraphNode
     * 6. Positions the GraphNode at the centroid of selected nodes
     *
     * @param selectedNodeIds IDs of the nodes to include in the GraphNode
     * @param allNodes List of all available nodes (to look up selected nodes)
     * @param allConnections List of all connections (to determine internal vs external)
     * @param graphNodeName Name for the new GraphNode
     * @return The created GraphNode, or null if creation is not possible (e.g., fewer than 2 nodes)
     */
    fun createFromSelection(
        selectedNodeIds: Set<String>,
        allNodes: List<Node>,
        allConnections: List<Connection>,
        graphNodeName: String
    ): GraphNode? {
        // Validate: need at least 2 nodes to group
        if (selectedNodeIds.size < 2) {
            return null
        }

        // Generate unique ID for the GraphNode
        val graphNodeId = "graphNode_${System.currentTimeMillis()}_${(0..999).random()}"

        // Collect selected nodes
        val selectedNodes = allNodes.filter { it.id in selectedNodeIds }
        if (selectedNodes.size < 2) {
            return null // Not enough valid nodes found
        }

        // Update child nodes with new parentNodeId
        val childNodes = selectedNodes.map { node ->
            node.withParent(graphNodeId)
        }

        // Identify internal connections (both endpoints are selected)
        val internalConnections = allConnections.filter { conn ->
            conn.sourceNodeId in selectedNodeIds && conn.targetNodeId in selectedNodeIds
        }

        // Generate port mappings for external connections
        val portMappings = generatePortMappings(selectedNodeIds, allConnections)

        // Create input ports from incoming external connections
        val inputPorts = portMappings
            .filter { it.graphNodePortDirection == Port.Direction.INPUT }
            .distinctBy { "${it.childNodeId}_${it.childPortId}" } // Deduplicate same target port
            .map { mapping ->
                createPort(
                    id = mapping.graphNodePortId,
                    name = mapping.graphNodePortName,
                    direction = Port.Direction.INPUT,
                    owningNodeId = graphNodeId
                )
            }

        // Create output ports from outgoing external connections
        val outputPorts = portMappings
            .filter { it.graphNodePortDirection == Port.Direction.OUTPUT }
            .distinctBy { "${it.childNodeId}_${it.childPortId}" } // Deduplicate same source port
            .map { mapping ->
                createPort(
                    id = mapping.graphNodePortId,
                    name = mapping.graphNodePortName,
                    direction = Port.Direction.OUTPUT,
                    owningNodeId = graphNodeId
                )
            }

        // Build port mappings map (GraphNode port name -> child port reference)
        val portMappingsMap = portMappings
            .distinctBy { "${it.childNodeId}_${it.childPortId}" }
            .associate { mapping ->
                mapping.graphNodePortName to GraphNode.PortMapping(
                    childNodeId = mapping.childNodeId,
                    childPortName = mapping.childPortId
                )
            }

        // Calculate centroid position
        val centroidX = selectedNodes.map { it.position.x }.average()
        val centroidY = selectedNodes.map { it.position.y }.average()

        // Create the GraphNode
        return GraphNode(
            id = graphNodeId,
            name = graphNodeName,
            description = "Grouped from ${selectedNodes.size} nodes",
            position = Node.Position(centroidX, centroidY),
            inputPorts = inputPorts,
            outputPorts = outputPorts,
            childNodes = childNodes,
            internalConnections = internalConnections,
            portMappings = portMappingsMap
        )
    }

    /**
     * Generates port mappings for external connections to/from selected nodes.
     *
     * External connections are those where exactly one endpoint (source or target)
     * is in the selection. These need to be exposed as ports on the GraphNode.
     *
     * - If source is selected and target is not: need OUTPUT port on GraphNode
     * - If target is selected and source is not: need INPUT port on GraphNode
     *
     * @param selectedNodeIds IDs of the nodes that will be in the GraphNode
     * @param allConnections List of all connections to analyze
     * @return List of port mappings describing external connections
     */
    fun generatePortMappings(
        selectedNodeIds: Set<String>,
        allConnections: List<Connection>
    ): List<GeneratedPortMapping> {
        val mappings = mutableListOf<GeneratedPortMapping>()

        allConnections.forEach { conn ->
            val sourceSelected = conn.sourceNodeId in selectedNodeIds
            val targetSelected = conn.targetNodeId in selectedNodeIds

            // External connection: exactly one endpoint is selected
            if (sourceSelected xor targetSelected) {
                if (sourceSelected) {
                    // Source is inside, target is outside -> need OUTPUT port
                    val portId = "port_out_${conn.sourcePortId}_${System.nanoTime()}"
                    mappings.add(
                        GeneratedPortMapping(
                            graphNodePortId = portId,
                            graphNodePortName = "out_${conn.sourcePortId}",
                            graphNodePortDirection = Port.Direction.OUTPUT,
                            childNodeId = conn.sourceNodeId,
                            childPortId = conn.sourcePortId,
                            externalConnectionId = conn.id
                        )
                    )
                } else {
                    // Target is inside, source is outside -> need INPUT port
                    val portId = "port_in_${conn.targetPortId}_${System.nanoTime()}"
                    mappings.add(
                        GeneratedPortMapping(
                            graphNodePortId = portId,
                            graphNodePortName = "in_${conn.targetPortId}",
                            graphNodePortDirection = Port.Direction.INPUT,
                            childNodeId = conn.targetNodeId,
                            childPortId = conn.targetPortId,
                            externalConnectionId = conn.id
                        )
                    )
                }
            }
            // If both or neither selected, it's an internal or unrelated connection
        }

        return mappings
    }

    /**
     * Creates a Port with the specified properties.
     * Uses String as the default data type since we don't have type information
     * from the connection alone.
     */
    private fun createPort(
        id: String,
        name: String,
        direction: Port.Direction,
        owningNodeId: String
    ): Port<String> {
        return Port(
            id = id,
            name = name,
            direction = direction,
            dataType = String::class,
            owningNodeId = owningNodeId
        )
    }
}
