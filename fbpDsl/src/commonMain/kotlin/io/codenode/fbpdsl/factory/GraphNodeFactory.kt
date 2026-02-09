/*
 * GraphNodeFactory - Factory for creating GraphNodes from selected nodes
 * Creates hierarchical GraphNodes with auto-generated port mappings
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.factory

import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.PassThruPort
import io.codenode.fbpdsl.model.Port
import kotlinx.datetime.Clock

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
        val externalConnectionId: String,
        /** The external node ID (outside the GraphNode boundary) */
        val externalNodeId: String = "",
        /** The external port ID (on the external node) */
        val externalPortId: String = ""
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
        val graphNodeId = "graphNode_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"

        // Collect selected nodes
        val selectedNodes = allNodes.filter { it.id in selectedNodeIds }
        if (selectedNodes.size < 2) {
            return null // Not enough valid nodes found
        }

        // Calculate bounding box of selected nodes for normalization
        val minX = selectedNodes.minOf { it.position.x }
        val minY = selectedNodes.minOf { it.position.y }

        // Shift child nodes so their bounding box starts at a small margin
        // This keeps all positions non-negative (Position requires x,y >= 0)
        // The UI layer will calculate the actual center for display centering
        val margin = 50.0
        val childNodes = selectedNodes.map { node ->
            node.withParent(graphNodeId).withPosition(
                Node.Position(
                    x = node.position.x - minX + margin,
                    y = node.position.y - minY + margin
                )
            )
        }

        // Identify internal connections (both endpoints are selected)
        // Set parentScopeId to the GraphNode ID so segments have correct scopeNodeId
        val internalConnections = allConnections
            .filter { conn ->
                conn.sourceNodeId in selectedNodeIds && conn.targetNodeId in selectedNodeIds
            }
            .map { conn ->
                conn.copy(parentScopeId = graphNodeId)
            }

        // Generate port mappings for external connections
        val portMappings = generatePortMappings(selectedNodeIds, allConnections)

        // Create PassThruPorts for boundary-crossing connections
        val passThruPortResults = createPassThruPorts(graphNodeId, portMappings)

        // Check for any failures in PassThruPort creation (T022: error handling)
        val failures = passThruPortResults.filter { it.isFailure }
        if (failures.isNotEmpty()) {
            // Return null if any PassThruPort creation fails (type mismatch, etc.)
            return null
        }

        // Extract successful PassThruPorts
        val passThruPorts = passThruPortResults.mapNotNull { it.getOrNull() }

        // Separate into input and output ports
        // Note: We store the underlying Port from PassThruPort for GraphNode compatibility.
        // PassThruPort metadata (upstream/downstream refs) is preserved in portMappings.
        val inputPorts: List<Port<*>> = passThruPorts
            .filter { it.direction == Port.Direction.INPUT }
            .map { it.port }

        val outputPorts: List<Port<*>> = passThruPorts
            .filter { it.direction == Port.Direction.OUTPUT }
            .map { it.port }

        // Build port mappings map (GraphNode port name -> child port reference)
        // Use PassThruPort names for consistency
        val portMappingsMap = passThruPorts.associate { passThru ->
            passThru.name to GraphNode.PortMapping(
                childNodeId = if (passThru.direction == Port.Direction.INPUT)
                    passThru.downstreamNodeId else passThru.upstreamNodeId,
                childPortName = if (passThru.direction == Port.Direction.INPUT)
                    passThru.downstreamPortId else passThru.upstreamPortId
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
                    val portId = "port_out_${conn.sourcePortId}_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}"
                    mappings.add(
                        GeneratedPortMapping(
                            graphNodePortId = portId,
                            graphNodePortName = "out_${conn.sourcePortId}",
                            graphNodePortDirection = Port.Direction.OUTPUT,
                            childNodeId = conn.sourceNodeId,
                            childPortId = conn.sourcePortId,
                            externalConnectionId = conn.id,
                            externalNodeId = conn.targetNodeId,
                            externalPortId = conn.targetPortId
                        )
                    )
                } else {
                    // Target is inside, source is outside -> need INPUT port
                    val portId = "port_in_${conn.targetPortId}_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}"
                    mappings.add(
                        GeneratedPortMapping(
                            graphNodePortId = portId,
                            graphNodePortName = "in_${conn.targetPortId}",
                            graphNodePortDirection = Port.Direction.INPUT,
                            childNodeId = conn.targetNodeId,
                            childPortId = conn.targetPortId,
                            externalConnectionId = conn.id,
                            externalNodeId = conn.sourceNodeId,
                            externalPortId = conn.sourcePortId
                        )
                    )
                }
            }
            // If both or neither selected, it's an internal or unrelated connection
        }

        return mappings
    }

    /**
     * Creates PassThruPorts for all port mappings using PassThruPortFactory.
     *
     * @param graphNodeId The ID of the GraphNode being created
     * @param portMappings The port mappings describing boundary-crossing connections
     * @return List of Results, each containing a PassThruPort or an error
     */
    private fun createPassThruPorts(
        graphNodeId: String,
        portMappings: List<GeneratedPortMapping>
    ): List<Result<PassThruPort<Any>>> {
        return portMappings
            .distinctBy { "${it.childNodeId}_${it.childPortId}" }
            .map { mapping ->
                PassThruPortFactory.createFromBoundaryCrossing(
                    graphNodeId = graphNodeId,
                    externalNodeId = mapping.externalNodeId,
                    externalPortId = mapping.externalPortId,
                    internalNodeId = mapping.childNodeId,
                    internalPortId = mapping.childPortId,
                    direction = mapping.graphNodePortDirection
                )
            }
    }

    /**
     * Creates a Port with the specified properties.
     * Uses String as the default data type since we don't have type information
     * from the connection alone.
     * @deprecated Use PassThruPortFactory.create() instead for boundary ports
     */
    @Deprecated("Use PassThruPortFactory.create() instead", ReplaceWith("PassThruPortFactory.create(...)"))
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
