/*
 * GraphNodeTemplateInstantiator - Creates independent copies of saved GraphNode templates
 * Handles deep copy with ID remapping for all nodes, connections, ports, and port mappings
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.model.*
import io.codenode.flowgraphtypes.registry.IPTypeRegistry
import io.codenode.grapheditor.model.GraphNodeTemplateMeta
import io.codenode.grapheditor.serialization.GraphNodeTemplateSerializer
import java.io.File

/**
 * Creates independent deep copies of saved GraphNode templates.
 *
 * When instantiating a template, all IDs are regenerated to ensure
 * the new instance is fully independent from the template and from
 * other instances created from the same template.
 */
object GraphNodeTemplateInstantiator {

    /**
     * Loads a GraphNode template and creates an independent copy with fresh IDs.
     *
     * @param meta Template metadata (contains file path for loading)
     * @param registry The registry (used for file path lookup)
     * @return A new GraphNode with remapped IDs, or null if loading fails
     */
    fun instantiate(
        meta: GraphNodeTemplateMeta,
        registry: GraphNodeTemplateRegistry,
        ipTypeRegistry: IPTypeRegistry? = null
    ): GraphNode? {
        val templateFile = File(meta.filePath)
        val template = GraphNodeTemplateSerializer.loadTemplate(templateFile, ipTypeRegistry) ?: return null
        return deepCopyWithNewIds(template)
    }

    /**
     * Creates a deep copy of a GraphNode with all IDs remapped.
     *
     * Remaps:
     * - GraphNode ID
     * - All child node IDs (recursively for nested GraphNodes)
     * - All internal connection source/target node IDs
     * - All port owningNodeId references
     * - All portMapping childNodeId references
     */
    fun deepCopyWithNewIds(original: GraphNode): GraphNode {
        // Build ID mapping: old ID -> new ID
        val idMap = mutableMapOf<String, String>()

        // Generate new ID for the GraphNode itself
        val newGraphNodeId = generateId("graphnode")
        idMap[original.id] = newGraphNodeId

        // Generate new IDs for all child nodes (recursively)
        collectNodeIds(original.childNodes, idMap)

        // Remap child nodes
        val remappedChildren = remapNodes(original.childNodes, idMap, newGraphNodeId)

        // Remap internal connections
        val remappedConnections = original.internalConnections.map { conn ->
            Connection(
                id = generateId("conn"),
                sourceNodeId = idMap[conn.sourceNodeId] ?: conn.sourceNodeId,
                sourcePortId = remapPortId(conn.sourcePortId, idMap),
                targetNodeId = idMap[conn.targetNodeId] ?: conn.targetNodeId,
                targetPortId = remapPortId(conn.targetPortId, idMap),
                ipTypeId = conn.ipTypeId
            )
        }

        // Remap input ports
        val remappedInputPorts = original.inputPorts.map { port ->
            @Suppress("UNCHECKED_CAST")
            Port<Any>(
                id = remapPortId(port.id, idMap),
                name = port.name,
                direction = port.direction,
                dataType = port.dataType as kotlin.reflect.KClass<Any>,
                required = port.required,
                defaultValue = null,
                validationRules = emptyList(),
                owningNodeId = newGraphNodeId
            )
        }

        // Remap output ports
        val remappedOutputPorts = original.outputPorts.map { port ->
            @Suppress("UNCHECKED_CAST")
            Port<Any>(
                id = remapPortId(port.id, idMap),
                name = port.name,
                direction = port.direction,
                dataType = port.dataType as kotlin.reflect.KClass<Any>,
                required = port.required,
                defaultValue = null,
                validationRules = emptyList(),
                owningNodeId = newGraphNodeId
            )
        }

        // Remap port mappings
        val remappedPortMappings = original.portMappings.mapValues { (_, mapping) ->
            GraphNode.PortMapping(
                childNodeId = idMap[mapping.childNodeId] ?: mapping.childNodeId,
                childPortName = mapping.childPortName
            )
        }

        return GraphNode(
            id = newGraphNodeId,
            name = original.name,
            description = original.description,
            position = original.position,
            inputPorts = remappedInputPorts,
            outputPorts = remappedOutputPorts,
            configuration = original.configuration,
            parentNodeId = null,
            childNodes = remappedChildren,
            internalConnections = remappedConnections,
            portMappings = remappedPortMappings,
            executionState = ExecutionState.IDLE,
            controlConfig = ControlConfig()
        )
    }

    /**
     * Recursively collects all node IDs and generates new IDs for each.
     */
    private fun collectNodeIds(nodes: List<Node>, idMap: MutableMap<String, String>) {
        for (node in nodes) {
            idMap[node.id] = generateId(if (node is GraphNode) "graphnode" else "node")
            if (node is GraphNode) {
                collectNodeIds(node.childNodes, idMap)
            }
        }
    }

    /**
     * Recursively remaps all child nodes with new IDs and parent references.
     */
    private fun remapNodes(
        nodes: List<Node>,
        idMap: Map<String, String>,
        parentId: String
    ): List<Node> {
        return nodes.map { node ->
            when (node) {
                is CodeNode -> {
                    val newNodeId = idMap[node.id] ?: node.id
                    node.copy(
                        id = newNodeId,
                        parentNodeId = parentId,
                        inputPorts = node.inputPorts.map { port ->
                            @Suppress("UNCHECKED_CAST")
                            Port<Any>(
                                id = remapPortId(port.id, idMap),
                                name = port.name,
                                direction = port.direction,
                                dataType = port.dataType as kotlin.reflect.KClass<Any>,
                                required = port.required,
                                defaultValue = null,
                                validationRules = emptyList(),
                                owningNodeId = newNodeId
                            )
                        },
                        outputPorts = node.outputPorts.map { port ->
                            @Suppress("UNCHECKED_CAST")
                            Port<Any>(
                                id = remapPortId(port.id, idMap),
                                name = port.name,
                                direction = port.direction,
                                dataType = port.dataType as kotlin.reflect.KClass<Any>,
                                required = port.required,
                                defaultValue = null,
                                validationRules = emptyList(),
                                owningNodeId = newNodeId
                            )
                        },
                        executionState = ExecutionState.IDLE,
                        coroutineHandle = null
                    )
                }
                is GraphNode -> {
                    val newNodeId = idMap[node.id] ?: node.id
                    val remappedChildren = remapNodes(node.childNodes, idMap, newNodeId)
                    val remappedConnections = node.internalConnections.map { conn ->
                        Connection(
                            id = generateId("conn"),
                            sourceNodeId = idMap[conn.sourceNodeId] ?: conn.sourceNodeId,
                            sourcePortId = remapPortId(conn.sourcePortId, idMap),
                            targetNodeId = idMap[conn.targetNodeId] ?: conn.targetNodeId,
                            targetPortId = remapPortId(conn.targetPortId, idMap),
                            ipTypeId = conn.ipTypeId
                        )
                    }
                    val remappedMappings = node.portMappings.mapValues { (_, mapping) ->
                        GraphNode.PortMapping(
                            childNodeId = idMap[mapping.childNodeId] ?: mapping.childNodeId,
                            childPortName = mapping.childPortName
                        )
                    }
                    node.copy(
                        id = newNodeId,
                        parentNodeId = parentId,
                        inputPorts = node.inputPorts.map { port ->
                            @Suppress("UNCHECKED_CAST")
                            Port<Any>(
                                id = remapPortId(port.id, idMap),
                                name = port.name,
                                direction = port.direction,
                                dataType = port.dataType as kotlin.reflect.KClass<Any>,
                                required = port.required,
                                defaultValue = null,
                                validationRules = emptyList(),
                                owningNodeId = newNodeId
                            )
                        },
                        outputPorts = node.outputPorts.map { port ->
                            @Suppress("UNCHECKED_CAST")
                            Port<Any>(
                                id = remapPortId(port.id, idMap),
                                name = port.name,
                                direction = port.direction,
                                dataType = port.dataType as kotlin.reflect.KClass<Any>,
                                required = port.required,
                                defaultValue = null,
                                validationRules = emptyList(),
                                owningNodeId = newNodeId
                            )
                        },
                        childNodes = remappedChildren,
                        internalConnections = remappedConnections,
                        portMappings = remappedMappings,
                        executionState = ExecutionState.IDLE,
                        controlConfig = ControlConfig()
                    )
                }
                else -> node
            }
        }
    }

    /**
     * Remaps a port ID by replacing the owning node ID prefix with the new node ID.
     * Port IDs follow the pattern "{nodeId}_{direction}_{portName}" or similar.
     */
    private fun remapPortId(portId: String, idMap: Map<String, String>): String {
        // Find which old node ID is a prefix of this port ID
        for ((oldId, newId) in idMap) {
            // Direct match: portId starts with nodeId (e.g., "node_httpfetcher_portName")
            if (portId.startsWith("${oldId}_")) {
                return portId.replaceFirst(oldId, newId)
            }
            // Name-based match: port IDs use nodeName without "node_"/"graphnode_" prefix
            // e.g., nodeId = "node_httpfetcher", portId = "httpfetcher_response"
            val oldNamePrefix = oldId.removePrefix("node_").removePrefix("graphnode_")
            if (oldNamePrefix.isNotEmpty() && portId.startsWith("${oldNamePrefix}_")) {
                val newNamePrefix = newId.removePrefix("node_").removePrefix("graphnode_")
                return portId.replaceFirst(oldNamePrefix, newNamePrefix)
            }
        }
        // If no match, generate a fresh ID to ensure uniqueness
        return "port_${System.currentTimeMillis()}_${(0..9999).random()}_${portId.substringAfterLast("_")}"
    }

    /**
     * Generates a unique ID with the given prefix.
     */
    private fun generateId(prefix: String): String {
        return "${prefix}_${System.currentTimeMillis()}_${(0..99999).random()}"
    }
}
