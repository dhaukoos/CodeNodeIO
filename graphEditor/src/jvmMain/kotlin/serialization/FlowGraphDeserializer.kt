/*
 * FlowGraphDeserializer - Graph Deserialization from .flow.kts Files
 * Loads FlowGraph instances from Kotlin DSL script files using the Kotlin scripting engine
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.ValidationResult
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.Connection
import java.io.File
import java.io.Reader
import javax.script.ScriptEngineManager
import javax.script.ScriptException

/**
 * Deserializes FlowGraph instances from .flow.kts Kotlin DSL files
 * Uses Kotlin scripting engine to evaluate DSL scripts
 */
object FlowGraphDeserializer {

    /**
     * Deserializes a FlowGraph from a Kotlin DSL string
     *
     * @param dslContent The Kotlin DSL script content
     * @return DeserializationResult with the graph or error information
     */
    fun deserialize(dslContent: String): DeserializationResult {
        return try {
            // Try to use Kotlin scripting engine if available
            val engine = try {
                ScriptEngineManager().getEngineByExtension("kts")
            } catch (e: Exception) {
                null
            }

            if (engine != null) {
                // Use Kotlin scripting engine
                try {
                    val result = engine.eval(dslContent)

                    if (result is FlowGraph) {
                        DeserializationResult.success(result)
                    } else {
                        DeserializationResult.error(
                            "Script did not produce a FlowGraph. Got: ${result?.javaClass?.simpleName ?: "null"}"
                        )
                    }
                } catch (e: ScriptException) {
                    DeserializationResult.error(
                        "Script evaluation failed: ${e.message}",
                        e
                    )
                }
            } else {
                // Fallback: Parse DSL manually (simplified parsing)
                deserializeManually(dslContent)
            }
        } catch (e: Exception) {
            DeserializationResult.error(
                "Deserialization failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Deserializes a FlowGraph from a file
     *
     * @param file The .flow.kts file to read
     * @return DeserializationResult with the graph or error information
     */
    fun deserializeFromFile(file: File): DeserializationResult {
        return try {
            if (!file.exists()) {
                return DeserializationResult.error("File not found: ${file.absolutePath}")
            }

            if (!file.canRead()) {
                return DeserializationResult.error("File not readable: ${file.absolutePath}")
            }

            val content = file.readText()
            deserialize(content)
        } catch (e: Exception) {
            DeserializationResult.error(
                "Failed to read file: ${e.message}",
                e
            )
        }
    }

    /**
     * Deserializes a FlowGraph from a Reader
     *
     * @param reader The reader to read from
     * @return DeserializationResult with the graph or error information
     */
    fun deserializeFromReader(reader: Reader): DeserializationResult {
        return try {
            val content = reader.readText()
            deserialize(content)
        } catch (e: Exception) {
            DeserializationResult.error(
                "Failed to read from reader: ${e.message}",
                e
            )
        }
    }

    /**
     * Manual deserialization fallback with full GraphNode support (T081)
     * Handles recursive parsing of nested GraphNodes, internal connections, and port mappings
     */
    private fun deserializeManually(dslContent: String): DeserializationResult {
        return try {
            // Extract graph name and version from flowGraph(...) declaration
            val flowGraphPattern = Regex("""flowGraph\s*\(\s*"([^"]*)"\s*,\s*version\s*=\s*"([^"]*)"\s*(?:,\s*description\s*=\s*"([^"]*)"\s*)?\)\s*\{""")
            val flowGraphMatch = flowGraphPattern.find(dslContent)
                ?: return DeserializationResult.error("Could not find flowGraph declaration in file")

            val name = flowGraphMatch.groupValues[1]
            val version = flowGraphMatch.groupValues[2]
            val description = flowGraphMatch.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }

            // Parse all nodes (both CodeNodes and GraphNodes)
            val nodes = mutableListOf<Node>()
            val nodeIdMap = mutableMapOf<String, String>() // variable name -> node ID
            val allNodes = mutableMapOf<String, Node>() // node ID -> Node (for connection resolution)

            // Parse CodeNodes at root level
            parseCodeNodes(dslContent, nodes, nodeIdMap, allNodes)

            // Parse GraphNodes at root level (with recursive child parsing)
            parseGraphNodes(dslContent, nodes, nodeIdMap, allNodes)

            // Parse root-level connections (with optional IP type)
            val connections = parseConnections(dslContent, nodeIdMap, allNodes)

            // Create the FlowGraph manually
            val graphId = "graph_${System.currentTimeMillis()}_${(0..999999).random()}"
            val graph = FlowGraph(
                id = graphId,
                name = name,
                version = version,
                description = description,
                rootNodes = nodes,
                connections = connections,
                metadata = emptyMap(),
                targetPlatforms = emptyList()
            )

            DeserializationResult.success(graph)
        } catch (e: Exception) {
            DeserializationResult.error(
                "Manual deserialization failed: ${e.message}",
                e
            )
        }
    }

    /**
     * Finds the ranges of all graphNode blocks in the content.
     * Used to exclude nested CodeNodes from root-level parsing.
     */
    private fun findGraphNodeRanges(content: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val graphNodeStarts = Regex("""val\s+\w+\s*=\s*graphNode\s*\(\s*"[^"]*"\s*\)\s*\{""")

        graphNodeStarts.findAll(content).forEach { match ->
            val startIndex = match.range.first
            val bodyStart = match.range.last + 1
            val body = extractBalancedBraces(content, bodyStart)
            val endIndex = bodyStart + body.length + 1 // +1 for closing brace
            ranges.add(startIndex..endIndex)
        }

        return ranges
    }

    /**
     * Checks if a position is inside any of the given ranges
     */
    private fun isInsideAnyRange(position: Int, ranges: List<IntRange>): Boolean {
        return ranges.any { position in it }
    }

    /**
     * Parses CodeNode declarations from DSL content (root level only)
     */
    private fun parseCodeNodes(
        content: String,
        nodes: MutableList<Node>,
        nodeIdMap: MutableMap<String, String>,
        allNodes: MutableMap<String, Node>
    ) {
        // Find all graphNode block ranges to exclude nested CodeNodes
        val graphNodeRanges = findGraphNodeRanges(content)

        val nodePattern = Regex("""val\s+(\w+)\s*=\s*codeNode\s*\(\s*"([^"]*)"\s*(?:,\s*nodeType\s*=\s*"([^"]*)"\s*)?\)\s*\{([^}]*)\}""", RegexOption.DOT_MATCHES_ALL)
        nodePattern.findAll(content).forEach { match ->
            // Skip CodeNodes that are inside a graphNode block
            if (isInsideAnyRange(match.range.first, graphNodeRanges)) {
                return@forEach
            }

            val varName = match.groupValues[1]
            val nodeName = match.groupValues[2]
            val nodeBody = match.groupValues[4]

            val node = parseCodeNodeBody(varName, nodeName, nodeBody)
            nodes.add(node)
            nodeIdMap[varName] = node.id
            allNodes[node.id] = node
        }
    }

    /**
     * Parses a CodeNode body and creates a CodeNode instance
     */
    private fun parseCodeNodeBody(varName: String, nodeName: String, nodeBody: String): CodeNode {
        // Parse position
        val posPattern = Regex("""position\s*\(\s*([\d.]+)\s*,\s*([\d.]+)\s*\)""")
        val posMatch = posPattern.find(nodeBody)
        val x = posMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val y = posMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0

        // Parse description
        val descPattern = Regex("""description\s*=\s*"([^"]*)"""")
        val nodeDesc = descPattern.find(nodeBody)?.groupValues?.get(1)

        val nodeId = "node_${System.currentTimeMillis()}_${(0..9999).random()}"

        // Parse input ports
        val inputPorts = mutableListOf<Port<Any>>()
        val inputPattern = Regex("""input\s*\(\s*"([^"]*)"\s*,\s*(\w+)::class(?:\s*,\s*required\s*=\s*(true|false))?\s*\)""")
        inputPattern.findAll(nodeBody).forEach { inputMatch ->
            val portName = inputMatch.groupValues[1]
            val portId = "port_${System.currentTimeMillis()}_${(0..9999).random()}_$portName"
            inputPorts.add(Port<Any>(
                id = portId,
                name = portName,
                direction = Port.Direction.INPUT,
                dataType = Any::class,
                owningNodeId = nodeId
            ))
        }

        // Parse output ports
        val outputPorts = mutableListOf<Port<Any>>()
        val outputPattern = Regex("""output\s*\(\s*"([^"]*)"\s*,\s*(\w+)::class(?:\s*,\s*required\s*=\s*(true|false))?\s*\)""")
        outputPattern.findAll(nodeBody).forEach { outputMatch ->
            val portName = outputMatch.groupValues[1]
            val portId = "port_${System.currentTimeMillis()}_${(0..9999).random()}_$portName"
            outputPorts.add(Port<Any>(
                id = portId,
                name = portName,
                direction = Port.Direction.OUTPUT,
                dataType = Any::class,
                owningNodeId = nodeId
            ))
        }

        // Parse configuration
        val configuration = mutableMapOf<String, String>()
        val configPattern = Regex("""config\s*\(\s*"([^"]*)"\s*,\s*"([^"]*)"\s*\)""")
        configPattern.findAll(nodeBody).forEach { configMatch ->
            val configKey = configMatch.groupValues[1]
            val configValue = configMatch.groupValues[2]
            configuration[configKey] = configValue
        }

        return CodeNode(
            id = nodeId,
            name = nodeName,
            codeNodeType = CodeNodeType.CUSTOM,
            description = nodeDesc,
            position = Node.Position(x, y),
            inputPorts = inputPorts,
            outputPorts = outputPorts,
            configuration = configuration
        )
    }

    /**
     * Parses GraphNode declarations from DSL content with recursive child handling (T081)
     * Only parses root-level GraphNodes, nested ones are handled recursively by parseGraphNodeBody
     */
    private fun parseGraphNodes(
        content: String,
        nodes: MutableList<Node>,
        nodeIdMap: MutableMap<String, String>,
        allNodes: MutableMap<String, Node>
    ) {
        // Find all graphNode block ranges to exclude nested GraphNodes
        val graphNodeRanges = findGraphNodeRanges(content)

        // Find graphNode declarations - need to handle nested braces
        val graphNodeStarts = Regex("""val\s+(\w+)\s*=\s*graphNode\s*\(\s*"([^"]*)"\s*\)\s*\{""")
        graphNodeStarts.findAll(content).forEach { match ->
            // Skip GraphNodes that are STRICTLY inside another graphNode block (they're handled recursively)
            // A graphNode at the start of a range is the root of that range, not inside it
            val isStrictlyInsideAnotherBlock = graphNodeRanges.any { range ->
                match.range.first > range.first && match.range.first in range
            }
            if (isStrictlyInsideAnotherBlock) {
                return@forEach
            }

            val varName = match.groupValues[1]
            val nodeName = match.groupValues[2]
            val startIndex = match.range.last + 1

            // Extract the body by counting braces
            val body = extractBalancedBraces(content, startIndex)

            val graphNode = parseGraphNodeBody(varName, nodeName, body, allNodes)
            nodes.add(graphNode)
            nodeIdMap[varName] = graphNode.id
            allNodes[graphNode.id] = graphNode
        }
    }

    /**
     * Extracts content within balanced braces starting from a given index
     */
    private fun extractBalancedBraces(content: String, startIndex: Int): String {
        var braceCount = 1
        var endIndex = startIndex

        while (braceCount > 0 && endIndex < content.length) {
            when (content[endIndex]) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
            if (braceCount > 0) endIndex++
        }

        return content.substring(startIndex, endIndex)
    }

    /**
     * Parses a GraphNode body and creates a GraphNode instance with children (T081)
     */
    private fun parseGraphNodeBody(
        varName: String,
        nodeName: String,
        nodeBody: String,
        allNodes: MutableMap<String, Node>
    ): GraphNode {
        val nodeId = "graphnode_${System.currentTimeMillis()}_${(0..9999).random()}"

        // Parse position
        val posPattern = Regex("""position\s*\(\s*([\d.]+)\s*,\s*([\d.]+)\s*\)""")
        val posMatch = posPattern.find(nodeBody)
        val x = posMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val y = posMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0

        // Parse description
        val descPattern = Regex("""description\s*=\s*"([^"]*)"""")
        val graphNodeDesc = descPattern.find(nodeBody)?.groupValues?.get(1)

        // Parse child nodes recursively
        val childNodes = mutableListOf<Node>()
        val childNodeIdMap = mutableMapOf<String, String>()

        // First, find all nested GraphNode ranges (needed to exclude nested content from other parsing)
        val nestedGraphNodeRanges = mutableListOf<IntRange>()
        val nestedGraphNodePattern = Regex("""val\s+(child_\w+)\s*=\s*graphNode\s*\(\s*"([^"]*)"\s*\)\s*\{""")
        nestedGraphNodePattern.findAll(nodeBody).forEach { match ->
            val startIndex = match.range.last + 1
            val childBody = extractBalancedBraces(nodeBody, startIndex)
            nestedGraphNodeRanges.add(match.range.first..(startIndex + childBody.length))
        }

        // Parse child CodeNodes (excluding those inside nested GraphNode blocks)
        val childCodeNodePattern = Regex("""val\s+(child_\w+)\s*=\s*codeNode\s*\(\s*"([^"]*)"\s*(?:,\s*nodeType\s*=\s*"([^"]*)"\s*)?\)\s*\{([^}]*)\}""", RegexOption.DOT_MATCHES_ALL)
        childCodeNodePattern.findAll(nodeBody).forEach { match ->
            // Skip if this CodeNode is inside a nested GraphNode block
            if (nestedGraphNodeRanges.any { match.range.first in it }) {
                return@forEach
            }

            val childVarName = match.groupValues[1]
            val childNodeName = match.groupValues[2]
            val childNodeBody = match.groupValues[4]

            val childNode = parseCodeNodeBody(childVarName, childNodeName, childNodeBody)
            childNodes.add(childNode)
            childNodeIdMap[childVarName] = childNode.id
            allNodes[childNode.id] = childNode
        }

        // Parse nested GraphNodes recursively
        nestedGraphNodePattern.findAll(nodeBody).forEach { match ->
            val childVarName = match.groupValues[1]
            val childNodeName = match.groupValues[2]
            val startIndex = match.range.last + 1

            val childBody = extractBalancedBraces(nodeBody, startIndex)
            val nestedGraphNode = parseGraphNodeBody(childVarName, childNodeName, childBody, allNodes)
            childNodes.add(nestedGraphNode)
            childNodeIdMap[childVarName] = nestedGraphNode.id
            allNodes[nestedGraphNode.id] = nestedGraphNode
        }

        // Parse internal connections (excluding those inside nested GraphNodes)
        val internalConnections = parseInternalConnections(nodeBody, childNodeIdMap, allNodes, nestedGraphNodeRanges)

        // Parse port mappings (resolve variable names to node IDs, excluding those inside nested GraphNodes)
        val portMappings = parsePortMappings(nodeBody, childNodeIdMap, nestedGraphNodeRanges)

        // Parse exposed input ports (excluding those inside nested GraphNodes)
        // T062: Updated regex to handle optional upstream/downstream parameters
        val inputPorts = mutableListOf<Port<Any>>()
        val exposeInputPattern = Regex("""exposeInput\s*\(\s*"([^"]*)"\s*,\s*(\w+)::class[^)]*\)""")
        exposeInputPattern.findAll(nodeBody).forEach { inputMatch ->
            // Skip if this match is inside a nested GraphNode block
            if (nestedGraphNodeRanges.any { inputMatch.range.first in it }) {
                return@forEach
            }
            val portName = inputMatch.groupValues[1]
            val portId = "port_${System.currentTimeMillis()}_${(0..9999).random()}_$portName"
            inputPorts.add(Port<Any>(
                id = portId,
                name = portName,
                direction = Port.Direction.INPUT,
                dataType = Any::class,
                owningNodeId = nodeId
            ))
        }

        // Parse exposed output ports (excluding those inside nested GraphNodes)
        // T062: Updated regex to handle optional upstream/downstream parameters
        val outputPorts = mutableListOf<Port<Any>>()
        val exposeOutputPattern = Regex("""exposeOutput\s*\(\s*"([^"]*)"\s*,\s*(\w+)::class[^)]*\)""")
        exposeOutputPattern.findAll(nodeBody).forEach { outputMatch ->
            // Skip if this match is inside a nested GraphNode block
            if (nestedGraphNodeRanges.any { outputMatch.range.first in it }) {
                return@forEach
            }
            val portName = outputMatch.groupValues[1]
            val portId = "port_${System.currentTimeMillis()}_${(0..9999).random()}_$portName"
            outputPorts.add(Port<Any>(
                id = portId,
                name = portName,
                direction = Port.Direction.OUTPUT,
                dataType = Any::class,
                owningNodeId = nodeId
            ))
        }

        return GraphNode(
            id = nodeId,
            name = nodeName,
            description = graphNodeDesc,
            position = Node.Position(x, y),
            childNodes = childNodes,
            internalConnections = internalConnections,
            inputPorts = inputPorts,
            outputPorts = outputPorts,
            portMappings = portMappings
        )
    }

    /**
     * Parses internal connections within a GraphNode body
     * Excludes connections that appear inside nested GraphNode blocks
     */
    private fun parseInternalConnections(
        nodeBody: String,
        childNodeIdMap: Map<String, String>,
        allNodes: Map<String, Node>,
        nestedGraphNodeRanges: List<IntRange> = emptyList()
    ): List<Connection> {
        val connections = mutableListOf<Connection>()

        // Pattern for internalConnection(source, "port", target, "port")
        val internalConnPattern = Regex("""internalConnection\s*\(\s*(\w+)\s*,\s*"([^"]*)"\s*,\s*(\w+)\s*,\s*"([^"]*)"\s*\)(?:\s*withType\s*"([^"]*)")?""")
        internalConnPattern.findAll(nodeBody).forEach { match ->
            // Skip if this match is inside a nested GraphNode block
            if (nestedGraphNodeRanges.any { match.range.first in it }) {
                return@forEach
            }

            val sourceVar = match.groupValues[1]
            val sourcePortName = match.groupValues[2]
            val targetVar = match.groupValues[3]
            val targetPortName = match.groupValues[4]
            val ipTypeId = match.groupValues.getOrNull(5)?.takeIf { it.isNotEmpty() }

            val sourceNodeId = childNodeIdMap[sourceVar]
            val targetNodeId = childNodeIdMap[targetVar]

            if (sourceNodeId != null && targetNodeId != null) {
                val sourceNode = allNodes[sourceNodeId]
                val targetNode = allNodes[targetNodeId]

                val sourcePort = sourceNode?.outputPorts?.find { it.name == sourcePortName }
                val targetPort = targetNode?.inputPorts?.find { it.name == targetPortName }

                if (sourcePort != null && targetPort != null) {
                    connections.add(Connection(
                        id = "internal_conn_${System.currentTimeMillis()}_${(0..9999).random()}",
                        sourceNodeId = sourceNodeId,
                        sourcePortId = sourcePort.id,
                        targetNodeId = targetNodeId,
                        targetPortId = targetPort.id,
                        ipTypeId = ipTypeId
                    ))
                }
            }
        }

        return connections
    }

    /**
     * Parses port mappings from GraphNode body
     * Resolves variable names (like "child_in1out1_1") to actual node IDs using childNodeIdMap
     * Excludes port mappings that appear inside nested GraphNode blocks
     *
     * @param nodeBody The DSL body content to parse
     * @param childNodeIdMap Map from variable names to node IDs for resolving references
     * @param nestedGraphNodeRanges Ranges of nested GraphNode blocks to exclude
     */
    private fun parsePortMappings(
        nodeBody: String,
        childNodeIdMap: Map<String, String>,
        nestedGraphNodeRanges: List<IntRange> = emptyList()
    ): Map<String, GraphNode.PortMapping> {
        val mappings = mutableMapOf<String, GraphNode.PortMapping>()

        // Pattern for portMapping("portName", "childVarName", "childPortName")
        val mappingPattern = Regex("""portMapping\s*\(\s*"([^"]*)"\s*,\s*"([^"]*)"\s*,\s*"([^"]*)"\s*\)""")
        mappingPattern.findAll(nodeBody).forEach { match ->
            // Skip if this match is inside a nested GraphNode block
            if (nestedGraphNodeRanges.any { match.range.first in it }) {
                return@forEach
            }

            val portName = match.groupValues[1]
            val childVarName = match.groupValues[2]
            val childPortName = match.groupValues[3]

            // Resolve variable name to node ID (fall back to using var name as ID for backward compatibility)
            val childNodeId = childNodeIdMap[childVarName] ?: childVarName

            mappings[portName] = GraphNode.PortMapping(childNodeId, childPortName)
        }

        return mappings
    }

    /**
     * Parses connections from DSL content
     */
    private fun parseConnections(
        content: String,
        nodeIdMap: Map<String, String>,
        allNodes: Map<String, Node>
    ): List<Connection> {
        val connections = mutableListOf<Connection>()
        val connPattern = Regex("""(\w+)\.output\s*\(\s*"([^"]*)"\s*\)\s*connect\s*(\w+)\.input\s*\(\s*"([^"]*)"\s*\)(?:\s*withType\s*"([^"]*)")?""")

        connPattern.findAll(content).forEach { match ->
            val sourceVar = match.groupValues[1]
            val sourcePortName = match.groupValues[2]
            val targetVar = match.groupValues[3]
            val targetPortName = match.groupValues[4]
            val ipTypeId = match.groupValues.getOrNull(5)?.takeIf { it.isNotEmpty() }

            val sourceNodeId = nodeIdMap[sourceVar]
            val targetNodeId = nodeIdMap[targetVar]

            if (sourceNodeId != null && targetNodeId != null) {
                val sourceNode = allNodes[sourceNodeId]
                val targetNode = allNodes[targetNodeId]

                val sourcePort = sourceNode?.outputPorts?.find { it.name == sourcePortName }
                val targetPort = targetNode?.inputPorts?.find { it.name == targetPortName }

                if (sourcePort != null && targetPort != null) {
                    connections.add(Connection(
                        id = "conn_${System.currentTimeMillis()}_${(0..9999).random()}",
                        sourceNodeId = sourceNodeId,
                        sourcePortId = sourcePort.id,
                        targetNodeId = targetNodeId,
                        targetPortId = targetPort.id,
                        ipTypeId = ipTypeId
                    ))
                }
            }
        }

        return connections
    }

    /**
     * Validates a .flow.kts file before attempting to deserialize
     *
     * @param file The file to validate
     * @return ValidationResult with any pre-deserialization issues
     */
    fun validateFile(file: File): ValidationResult {
        val errors = mutableListOf<String>()

        // Check file exists
        if (!file.exists()) {
            errors.add("File does not exist: ${file.absolutePath}")
        }

        // Check file is readable
        if (file.exists() && !file.canRead()) {
            errors.add("File is not readable: ${file.absolutePath}")
        }

        // Check file extension
        // Accept .flow (preferred - no IDE Kotlin script confusion), .flow.kts, or .kts
        if (!file.name.endsWith(".flow") && !file.name.endsWith(".flow.kts") && !file.name.endsWith(".kts")) {
            errors.add("File does not have .flow, .flow.kts, or .kts extension: ${file.name}")
        }

        // Check file is not empty
        if (file.exists() && file.length() == 0L) {
            errors.add("File is empty: ${file.absolutePath}")
        }

        // Basic syntax check (look for flowGraph declaration)
        if (file.exists() && file.canRead()) {
            try {
                val content = file.readText()
                if (!content.contains("flowGraph")) {
                    errors.add("File does not contain 'flowGraph' DSL declaration")
                }
            } catch (e: Exception) {
                errors.add("Failed to read file for validation: ${e.message}")
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Loads a FlowGraph from a file, throwing exception on failure
     *
     * @param file The file to load from
     * @return The loaded FlowGraph
     * @throws DeserializationException if deserialization fails
     */
    fun load(file: File): FlowGraph {
        val result = deserializeFromFile(file)
        if (result.isSuccess && result.graph != null) {
            return result.graph
        } else {
            throw DeserializationException(
                result.errorMessage ?: "Unknown deserialization error",
                result.exception
            )
        }
    }

    /**
     * Loads a FlowGraph from a string, throwing exception on failure
     *
     * @param dslContent The DSL content to load from
     * @return The loaded FlowGraph
     * @throws DeserializationException if deserialization fails
     */
    fun load(dslContent: String): FlowGraph {
        val result = deserialize(dslContent)
        if (result.isSuccess && result.graph != null) {
            return result.graph
        } else {
            throw DeserializationException(
                result.errorMessage ?: "Unknown deserialization error",
                result.exception
            )
        }
    }

    /**
     * Attempts to deserialize and returns null on failure (safe operation)
     *
     * @param file The file to load from
     * @return The loaded FlowGraph or null if deserialization failed
     */
    fun tryLoad(file: File): FlowGraph? {
        return try {
            val result = deserializeFromFile(file)
            if (result.isSuccess) result.graph else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Attempts to deserialize and returns null on failure (safe operation)
     *
     * @param dslContent The DSL content to load from
     * @return The loaded FlowGraph or null if deserialization failed
     */
    fun tryLoad(dslContent: String): FlowGraph? {
        return try {
            val result = deserialize(dslContent)
            if (result.isSuccess) result.graph else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Result of deserialization operation
 *
 * @property isSuccess Whether deserialization was successful
 * @property graph The deserialized FlowGraph (null if failed)
 * @property errorMessage Error message if deserialization failed
 * @property exception Exception that caused the failure (if any)
 */
data class DeserializationResult(
    val isSuccess: Boolean,
    val graph: FlowGraph?,
    val errorMessage: String?,
    val exception: Exception?
) {
    companion object {
        /**
         * Creates a successful deserialization result
         */
        fun success(graph: FlowGraph): DeserializationResult {
            return DeserializationResult(
                isSuccess = true,
                graph = graph,
                errorMessage = null,
                exception = null
            )
        }

        /**
         * Creates a failed deserialization result
         */
        fun error(message: String, exception: Exception? = null): DeserializationResult {
            return DeserializationResult(
                isSuccess = false,
                graph = null,
                errorMessage = message,
                exception = exception
            )
        }
    }

    /**
     * Returns the graph or throws an exception if deserialization failed
     */
    fun getOrThrow(): FlowGraph {
        if (isSuccess && graph != null) {
            return graph
        } else {
            throw DeserializationException(
                errorMessage ?: "Deserialization failed",
                exception
            )
        }
    }

    /**
     * Returns the graph or null if deserialization failed
     */
    fun getOrNull(): FlowGraph? {
        return if (isSuccess) graph else null
    }

    /**
     * Returns the graph or a default value if deserialization failed
     */
    fun getOrDefault(default: FlowGraph): FlowGraph {
        return if (isSuccess && graph != null) graph else default
    }
}

/**
 * Exception thrown when deserialization fails
 */
class DeserializationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Loads a FlowGraph from a .flow.kts file (extension function on File)
 *
 * @return DeserializationResult with the graph or error information
 */
fun File.loadFlowGraph(): DeserializationResult {
    return FlowGraphDeserializer.deserializeFromFile(this)
}

/**
 * Parses a FlowGraph from a DSL string (extension function on String)
 *
 * @return DeserializationResult with the graph or error information
 */
fun String.parseFlowGraph(): DeserializationResult {
    return FlowGraphDeserializer.deserialize(this)
}
