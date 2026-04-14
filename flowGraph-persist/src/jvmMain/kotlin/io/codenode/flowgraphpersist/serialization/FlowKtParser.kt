/*
 * FlowKtParser
 * Parses .flow.kt files back to FlowGraph
 * License: Apache 2.0
 */

package io.codenode.flowgraphpersist.serialization

import io.codenode.fbpdsl.model.*
import kotlin.reflect.KClass

/**
 * Result of parsing a .flow.kt file.
 *
 * @property isSuccess Whether parsing succeeded
 * @property graph The parsed FlowGraph (if successful)
 * @property errorMessage Error message (if failed)
 */
data class ParseResult(
    val isSuccess: Boolean,
    val graph: FlowGraph? = null,
    val errorMessage: String? = null,
    /** Maps port IDs to original type name strings for ports where KClass resolution fell back to Any::class */
    val portTypeNameHints: Map<String, String> = emptyMap()
)

/**
 * Parser for .flow.kt compiled Kotlin files.
 *
 * Parses a .flow.kt file back into a FlowGraph model for editing.
 * This enables round-trip serialization: FlowGraph → .flow.kt → FlowGraph
 *
 * T023: Create FlowKtParser class with parseFlowKt method
 * T024: Implement Kotlin source parsing to extract FlowGraph structure
 */
class FlowKtParser {

    // Regex patterns for parsing DSL constructs
    private val flowGraphPattern = Regex(
        """flowGraph\s*\(\s*"([^"]+)"\s*,\s*version\s*=\s*"([^"]+)"(?:\s*,\s*description\s*=\s*"([^"]*)")?\s*\)"""
    )

    private val codeNodePattern = Regex(
        """val\s+(\w+)\s*=\s*codeNode\s*\(\s*"([^"]+)"(?:\s*,\s*nodeType\s*=\s*"?(\w+)"?)?\s*\)"""
    )

    private val positionPattern = Regex(
        """position\s*\(\s*([\d.]+)\s*,\s*([\d.]+)\s*\)"""
    )

    private val inputPortPattern = Regex(
        """input\s*\(\s*"([^"]+)"\s*,\s*(\w+)::class(?:\s*,\s*required\s*=\s*(true|false))?\s*\)"""
    )

    private val outputPortPattern = Regex(
        """output\s*\(\s*"([^"]+)"\s*,\s*(\w+)::class(?:\s*,\s*required\s*=\s*(true|false))?\s*\)"""
    )

    private val connectionPattern = Regex(
        """(\w+)\.output\s*\(\s*"([^"]+)"\s*\)\s*connect\s+(\w+)\.input\s*\(\s*"([^"]+)"\s*\)(?:\s*withType\s*"([^"]+)")?"""
    )

    private val configPattern = Regex(
        """config\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)"""
    )

    private val importPattern = Regex(
        """import\s+([\w.]+)"""
    )

    private val targetPlatformPattern = Regex(
        """targetPlatform\s*\(\s*FlowGraph\.TargetPlatform\.(\w+)\s*\)"""
    )

    // GraphNode DSL patterns
    private val graphNodePattern = Regex(
        """val\s+(\w+)\s*=\s*graphNode\s*\(\s*"([^"]+)"\s*\)"""
    )

    private val descriptionPattern = Regex(
        """description\s*=\s*"([^"]*)""""
    )

    private val internalConnectionPattern = Regex(
        """internalConnection\s*\(\s*(\w+)\s*,\s*"([^"]+)"\s*,\s*(\w+)\s*,\s*"([^"]+)"\s*\)(?:\s*withType\s*"([^"]+)")?"""
    )

    private val portMappingPattern = Regex(
        """portMapping\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)"""
    )

    private val exposeInputPattern = Regex(
        """exposeInput\s*\(\s*"([^"]+)"\s*,\s*(\w+)::class(?:\s*,\s*required\s*=\s*(true|false))?(?:\s*,\s*upstream\s*=\s*"([^"]*)")?(?:\s*,\s*downstream\s*=\s*"([^"]*)")?\s*\)"""
    )

    private val exposeOutputPattern = Regex(
        """exposeOutput\s*\(\s*"([^"]+)"\s*,\s*(\w+)::class(?:\s*,\s*required\s*=\s*(true|false))?(?:\s*,\s*upstream\s*=\s*"([^"]*)")?(?:\s*,\s*downstream\s*=\s*"([^"]*)")?\s*\)"""
    )

    /** Map of simple class name → fully qualified class name, built from import statements */
    private var importMap: Map<String, String> = emptyMap()

    /** Optional external type resolver for custom IP types (typeName → KClass) */
    private var externalTypeResolver: ((String) -> KClass<*>?)? = null

    /** Collects portId → original type name for ports where KClass resolution fell back to Any */
    private val portTypeNameHints = mutableMapOf<String, String>()

    /**
     * Sets an external type resolver for custom IP types.
     * Called before parseFlowKt when IP type resolution is needed.
     */
    fun setTypeResolver(resolver: (String) -> KClass<*>?) {
        externalTypeResolver = resolver
    }

    /**
     * Parses .flow.kt content into a FlowGraph.
     *
     * @param content The .flow.kt file content
     * @return ParseResult with success status and parsed graph
     */
    fun parseFlowKt(content: String): ParseResult {
        return try {
            // Check for invalid syntax (basic validation)
            if (!isValidKotlinSyntax(content)) {
                return ParseResult(
                    isSuccess = false,
                    errorMessage = "Invalid Kotlin syntax"
                )
            }

            // Reset state for this parse
            portTypeNameHints.clear()

            // Build import map (simple name → FQCN) for resolving custom types
            importMap = buildImportMap(content)

            // Parse flowGraph header
            val flowGraphMatch = flowGraphPattern.find(content)
                ?: return ParseResult(
                    isSuccess = false,
                    errorMessage = "No flowGraph declaration found"
                )

            val graphName = flowGraphMatch.groupValues[1]
            val graphVersion = flowGraphMatch.groupValues[2]
            val graphDescription = flowGraphMatch.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }

            // Find the flowGraph block content
            val blockStart = content.indexOf("{", flowGraphMatch.range.last)
            val blockEnd = findMatchingBrace(content, blockStart)
            if (blockStart == -1 || blockEnd == -1) {
                return ParseResult(
                    isSuccess = false,
                    errorMessage = "Could not find flowGraph block"
                )
            }

            val blockContent = content.substring(blockStart + 1, blockEnd)

            // Parse target platforms
            val targetPlatforms = parseTargetPlatforms(blockContent)

            // Parse GraphNodes first to identify their block ranges
            val (graphNodes, graphNodeVarMap) = parseGraphNodes(blockContent)

            // Parse top-level-only CodeNodes (exclude content inside graphNode blocks)
            val topLevelContent = maskGraphNodeBlocks(blockContent)
            val (codeNodes, codeNodeVarMap) = parseCodeNodes(topLevelContent)

            val allNodes: List<Node> = codeNodes + graphNodes
            val nodeVarMap = codeNodeVarMap + graphNodeVarMap

            // Parse top-level connections only (not internal connections inside graphNode blocks)
            val connections = parseConnections(topLevelContent, nodeVarMap)

            val flowGraph = FlowGraph(
                id = "flow_${graphName.lowercase().replace(" ", "_")}",
                name = graphName,
                version = graphVersion,
                description = graphDescription,
                rootNodes = allNodes,
                connections = connections,
                targetPlatforms = targetPlatforms
            )

            ParseResult(
                isSuccess = true,
                graph = flowGraph,
                portTypeNameHints = portTypeNameHints.toMap()
            )
        } catch (e: Exception) {
            ParseResult(
                isSuccess = false,
                errorMessage = "Parse error: ${e.message}"
            )
        }
    }

    /**
     * Basic validation of Kotlin syntax.
     */
    private fun isValidKotlinSyntax(content: String): Boolean {
        // Check for balanced braces
        var braceCount = 0
        for (char in content) {
            when (char) {
                '{' -> braceCount++
                '}' -> braceCount--
            }
            if (braceCount < 0) return false
        }
        if (braceCount != 0) return false

        // Check for basic Kotlin structure (has some recognizable keywords or patterns)
        val hasValidStructure = content.contains("val ") ||
            content.contains("fun ") ||
            content.contains("package ") ||
            content.contains("flowGraph")

        // Check for obvious invalid syntax
        val hasInvalidPatterns = content.contains("{{{") ||
            content.contains("}}}") ||
            (content.contains("random garbage") && !content.contains("\"random garbage\""))

        return hasValidStructure && !hasInvalidPatterns
    }

    /**
     * Finds the matching closing brace for an opening brace.
     */
    private fun findMatchingBrace(content: String, openBraceIndex: Int): Int {
        if (openBraceIndex < 0 || openBraceIndex >= content.length || content[openBraceIndex] != '{') {
            return -1
        }

        var depth = 0
        var inString = false
        var escape = false

        for (i in openBraceIndex until content.length) {
            val char = content[i]

            if (escape) {
                escape = false
                continue
            }

            when {
                char == '\\' && inString -> escape = true
                char == '"' && !escape -> inString = !inString
                !inString && char == '{' -> depth++
                !inString && char == '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }

        return -1
    }

    /**
     * Returns a copy of blockContent with all graphNode block bodies replaced by spaces.
     * This prevents nested codeNode/connection declarations from matching top-level patterns.
     */
    private fun maskGraphNodeBlocks(blockContent: String): String {
        val result = StringBuilder(blockContent)
        val graphNodeMatches = graphNodePattern.findAll(blockContent)

        for (match in graphNodeMatches) {
            val blockStart = blockContent.indexOf("{", match.range.last)
            val blockEnd = findMatchingBrace(blockContent, blockStart)
            if (blockStart == -1 || blockEnd == -1) continue

            // Replace everything from the start of "val xxx = graphNode" through closing "}" with spaces
            for (i in match.range.first..blockEnd) {
                if (result[i] != '\n') {
                    result.setCharAt(i, ' ')
                }
            }
        }

        return result.toString()
    }

    /**
     * Parses targetPlatform declarations from the block content.
     */
    private fun parseTargetPlatforms(blockContent: String): List<FlowGraph.TargetPlatform> {
        return targetPlatformPattern.findAll(blockContent).mapNotNull { match ->
            try {
                FlowGraph.TargetPlatform.valueOf(match.groupValues[1])
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toList()
    }

    /**
     * Parses all codeNode declarations from the block content.
     */
    private fun parseCodeNodes(blockContent: String): Pair<List<CodeNode>, Map<String, String>> {
        val nodes = mutableListOf<CodeNode>()
        val nodeVarMap = mutableMapOf<String, String>() // varName -> nodeId

        // Find all codeNode declarations
        val nodeMatches = codeNodePattern.findAll(blockContent)

        for (match in nodeMatches) {
            val varName = match.groupValues[1]
            val nodeName = match.groupValues[2]
            val nodeTypeStr = match.groupValues[3].ifEmpty { "TRANSFORMER" }

            // Find the block for this node
            val nodeBlockStart = blockContent.indexOf("{", match.range.last)
            val nodeBlockEnd = findMatchingBrace(blockContent, nodeBlockStart)

            if (nodeBlockStart == -1 || nodeBlockEnd == -1) continue

            val nodeBlockContent = blockContent.substring(nodeBlockStart + 1, nodeBlockEnd)

            // Parse position
            val position = parsePosition(nodeBlockContent)

            // Parse input ports
            val inputPorts = parseInputPorts(nodeBlockContent, nodeName)

            // Parse output ports
            val outputPorts = parseOutputPorts(nodeBlockContent, nodeName)

            // Parse node type (with backward compatibility for renamed enums)
            val codeNodeType = parseCodeNodeType(nodeTypeStr)

            // Parse configuration
            val config = parseConfiguration(nodeBlockContent)

            val finalConfig = config

            val nodeId = "node_${nodeName.lowercase().replace(" ", "_")}"
            nodeVarMap[varName] = nodeId

            val node = CodeNode(
                id = nodeId,
                name = nodeName,
                codeNodeType = codeNodeType,
                position = position,
                inputPorts = inputPorts.map { it.copy(owningNodeId = nodeId) },
                outputPorts = outputPorts.map { it.copy(owningNodeId = nodeId) },
                configuration = finalConfig
            )

            nodes.add(node)
        }

        return Pair(nodes, nodeVarMap)
    }

    /**
     * Parses graphNode declarations from block content.
     * Each graphNode contains child codeNodes, internal connections, port mappings, and exposed ports.
     */
    private fun parseGraphNodes(blockContent: String): Pair<List<GraphNode>, Map<String, String>> {
        val nodes = mutableListOf<GraphNode>()
        val nodeVarMap = mutableMapOf<String, String>()

        val graphNodeMatches = graphNodePattern.findAll(blockContent)

        for (match in graphNodeMatches) {
            val varName = match.groupValues[1]
            val nodeName = match.groupValues[2]

            // Find the block for this graphNode
            val nodeBlockStart = blockContent.indexOf("{", match.range.last)
            val nodeBlockEnd = findMatchingBrace(blockContent, nodeBlockStart)

            if (nodeBlockStart == -1 || nodeBlockEnd == -1) continue

            val nodeBlockContent = blockContent.substring(nodeBlockStart + 1, nodeBlockEnd)

            // Parse description
            val description = descriptionPattern.find(nodeBlockContent)?.groupValues?.get(1)

            // Parse position
            val position = parsePosition(nodeBlockContent)

            // Parse child CodeNodes within this graphNode block
            val (childCodeNodes, childVarMap) = parseCodeNodes(nodeBlockContent)

            // Parse child GraphNodes recursively
            val (childGraphNodes, childGraphVarMap) = parseGraphNodes(nodeBlockContent)
            val allChildVarMap = childVarMap + childGraphVarMap

            val allChildNodes: List<Node> = childCodeNodes + childGraphNodes

            // Set parent IDs on child nodes
            val graphNodeId = "node_${nodeName.lowercase().replace(" ", "_")}"
            val childNodesWithParent = allChildNodes.map { child ->
                when (child) {
                    is CodeNode -> child.copy(parentNodeId = graphNodeId)
                    is GraphNode -> child.copy(parentNodeId = graphNodeId)
                    else -> child
                }
            }

            // Parse internal connections
            val internalConnections = parseInternalConnections(nodeBlockContent, allChildVarMap)

            // Parse port mappings
            val portMappings = parsePortMappings(nodeBlockContent, allChildVarMap)

            // Parse exposed input ports
            val inputPorts = parseExposedPorts(nodeBlockContent, graphNodeId, Port.Direction.INPUT)

            // Parse exposed output ports
            val outputPorts = parseExposedPorts(nodeBlockContent, graphNodeId, Port.Direction.OUTPUT)

            nodeVarMap[varName] = graphNodeId

            val graphNode = GraphNode(
                id = graphNodeId,
                name = nodeName,
                description = description,
                position = position,
                inputPorts = inputPorts,
                outputPorts = outputPorts,
                parentNodeId = null,
                childNodes = childNodesWithParent,
                internalConnections = internalConnections,
                portMappings = portMappings,
                executionState = ExecutionState.IDLE,
                controlConfig = ControlConfig()
            )

            nodes.add(graphNode)
        }

        return Pair(nodes, nodeVarMap)
    }

    /**
     * Parses internal connections within a graphNode block.
     * Supports both formats:
     * - `internalConnection(sourceVar, "portName", targetVar, "portName")` (FlowGraphSerializer)
     * - `sourceVar.output("portName") connect targetVar.input("portName")` (FlowKtGenerator)
     */
    private fun parseInternalConnections(
        blockContent: String,
        childVarMap: Map<String, String>
    ): List<Connection> {
        val connections = mutableListOf<Connection>()
        var index = 0

        // Format 1: internalConnection(sourceVar, "portName", targetVar, "portName")
        internalConnectionPattern.findAll(blockContent).forEach { match ->
            val sourceVar = match.groupValues[1]
            val sourcePortName = match.groupValues[2]
            val targetVar = match.groupValues[3]
            val targetPortName = match.groupValues[4]
            val ipTypeId = match.groupValues.getOrNull(5)?.takeIf { it.isNotEmpty() }

            val sourceNodeId = childVarMap[sourceVar] ?: return@forEach
            val targetNodeId = childVarMap[targetVar] ?: return@forEach

            connections.add(
                Connection(
                    id = "iconn_${index++}",
                    sourceNodeId = sourceNodeId,
                    sourcePortId = "${sourceNodeId.removePrefix("node_")}_${sourcePortName}",
                    targetNodeId = targetNodeId,
                    targetPortId = "${targetNodeId.removePrefix("node_")}_${targetPortName}",
                    ipTypeId = ipTypeId
                )
            )
        }

        // Format 2: sourceVar.output("portName") connect targetVar.input("portName")
        connectionPattern.findAll(blockContent).forEach { match ->
            val sourceVar = match.groupValues[1]
            val sourcePortName = match.groupValues[2]
            val targetVar = match.groupValues[3]
            val targetPortName = match.groupValues[4]
            val ipTypeId = match.groupValues.getOrNull(5)?.takeIf { it.isNotEmpty() }

            val sourceNodeId = childVarMap[sourceVar] ?: return@forEach
            val targetNodeId = childVarMap[targetVar] ?: return@forEach

            connections.add(
                Connection(
                    id = "iconn_${index++}",
                    sourceNodeId = sourceNodeId,
                    sourcePortId = "${sourceNodeId.removePrefix("node_")}_${sourcePortName}",
                    targetNodeId = targetNodeId,
                    targetPortId = "${targetNodeId.removePrefix("node_")}_${targetPortName}",
                    ipTypeId = ipTypeId
                )
            )
        }

        return connections
    }

    /**
     * Parses portMapping declarations within a graphNode block.
     * Format: portMapping("portName", "childVarName", "childPortName")
     */
    private fun parsePortMappings(
        blockContent: String,
        childVarMap: Map<String, String>
    ): Map<String, GraphNode.PortMapping> {
        val mappings = mutableMapOf<String, GraphNode.PortMapping>()

        portMappingPattern.findAll(blockContent).forEach { match ->
            val portName = match.groupValues[1]
            val childVarName = match.groupValues[2]
            val childPortName = match.groupValues[3]

            // Resolve childVarName to childNodeId
            val childNodeId = childVarMap[childVarName] ?: childVarName

            mappings[portName] = GraphNode.PortMapping(
                childNodeId = childNodeId,
                childPortName = childPortName
            )
        }

        return mappings
    }

    /**
     * Parses exposeInput/exposeOutput declarations within a graphNode block.
     * Port IDs use the same convention as CodeNode ports: `{nodeName}_{portName}`
     * so that top-level connection port ID resolution works consistently.
     */
    private fun parseExposedPorts(
        blockContent: String,
        owningNodeId: String,
        direction: Port.Direction
    ): List<Port<Any>> {
        val ports = mutableListOf<Port<Any>>()
        val nodeNamePrefix = owningNodeId.removePrefix("node_").removePrefix("graphnode_")

        val pattern = if (direction == Port.Direction.INPUT) exposeInputPattern else exposeOutputPattern
        pattern.findAll(blockContent).forEach { match ->
            val portName = match.groupValues[1]
            val typeName = match.groupValues[2]
            val required = match.groupValues.getOrNull(3) == "true"
            val portId = "${nodeNamePrefix}_${portName}"
            val resolvedType = resolveType(typeName)

            // Store original type name if resolution fell back to Any (e.g., typealias IP types)
            if (resolvedType == Any::class && typeName != "Any") {
                portTypeNameHints[portId] = typeName
            }

            @Suppress("UNCHECKED_CAST")
            ports.add(
                Port(
                    id = portId,
                    name = portName,
                    direction = direction,
                    dataType = resolvedType as KClass<Any>,
                    required = required,
                    owningNodeId = owningNodeId
                )
            )
        }

        return ports
    }

    /**
     * Parses position from node block content.
     */
    private fun parsePosition(nodeBlockContent: String): Node.Position {
        val posMatch = positionPattern.find(nodeBlockContent)
        return if (posMatch != null) {
            Node.Position(
                x = posMatch.groupValues[1].toDouble(),
                y = posMatch.groupValues[2].toDouble()
            )
        } else {
            Node.Position(0.0, 0.0)
        }
    }

    /**
     * Parses input ports from node block content.
     */
    private fun parseInputPorts(nodeBlockContent: String, nodeName: String): List<Port<Any>> {
        val ports = mutableListOf<Port<Any>>()

        inputPortPattern.findAll(nodeBlockContent).forEach { match ->
            val portName = match.groupValues[1]
            val typeName = match.groupValues[2]
            val required = match.groupValues.getOrNull(3) == "true"
            val resolvedType = resolveType(typeName)
            val portId = "${nodeName.lowercase()}_${portName}"

            // Store original type name if resolution fell back to Any
            if (resolvedType == Any::class && typeName != "Any") {
                portTypeNameHints[portId] = typeName
            }

            @Suppress("UNCHECKED_CAST")
            ports.add(
                Port(
                    id = portId,
                    name = portName,
                    direction = Port.Direction.INPUT,
                    dataType = resolvedType as kotlin.reflect.KClass<Any>,
                    required = required,
                    owningNodeId = ""
                )
            )
        }

        return ports
    }

    /**
     * Parses output ports from node block content.
     */
    private fun parseOutputPorts(nodeBlockContent: String, nodeName: String): List<Port<Any>> {
        val ports = mutableListOf<Port<Any>>()

        outputPortPattern.findAll(nodeBlockContent).forEach { match ->
            val portName = match.groupValues[1]
            val typeName = match.groupValues[2]
            val required = match.groupValues.getOrNull(3) == "true"
            val resolvedType = resolveType(typeName)
            val portId = "${nodeName.lowercase()}_${portName}"

            // Store original type name if resolution fell back to Any
            if (resolvedType == Any::class && typeName != "Any") {
                portTypeNameHints[portId] = typeName
            }

            @Suppress("UNCHECKED_CAST")
            ports.add(
                Port(
                    id = portId,
                    name = portName,
                    direction = Port.Direction.OUTPUT,
                    dataType = resolvedType as kotlin.reflect.KClass<Any>,
                    required = required,
                    owningNodeId = ""
                )
            )
        }

        return ports
    }

    /**
     * Parses configuration key-value pairs from node block content.
     */
    private fun parseConfiguration(nodeBlockContent: String): Map<String, String> {
        val config = mutableMapOf<String, String>()

        configPattern.findAll(nodeBlockContent).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2]
            config[key] = value
        }

        return config
    }

    /**
     * Parses connections from the block content.
     */
    private fun parseConnections(blockContent: String, nodeVarMap: Map<String, String>): List<Connection> {
        val connections = mutableListOf<Connection>()

        connectionPattern.findAll(blockContent).forEachIndexed { index, match ->
            val sourceVar = match.groupValues[1]
            val sourcePortName = match.groupValues[2]
            val targetVar = match.groupValues[3]
            val targetPortName = match.groupValues[4]
            val ipTypeId = match.groupValues.getOrNull(5)?.takeIf { it.isNotEmpty() }

            val sourceNodeId = nodeVarMap[sourceVar] ?: return@forEachIndexed
            val targetNodeId = nodeVarMap[targetVar] ?: return@forEachIndexed

            connections.add(
                Connection(
                    id = "conn_$index",
                    sourceNodeId = sourceNodeId,
                    sourcePortId = "${sourceNodeId.removePrefix("node_")}_${sourcePortName}",
                    targetNodeId = targetNodeId,
                    targetPortId = "${targetNodeId.removePrefix("node_")}_${targetPortName}",
                    ipTypeId = ipTypeId
                )
            )
        }

        return connections
    }

    /**
     * Parses a CodeNodeType string with backward compatibility for renamed enums.
     * Legacy .flow.kt files may contain "GENERATOR" which was renamed to "SOURCE".
     */
    private fun parseCodeNodeType(nodeTypeStr: String): CodeNodeType {
        // Backward compatibility: map legacy enum names to current values
        val normalized = when (nodeTypeStr) {
            "GENERATOR" -> "SOURCE"
            "GENERIC", "CUSTOM" -> "TRANSFORMER"
            else -> nodeTypeStr
        }
        return try {
            CodeNodeType.valueOf(normalized)
        } catch (e: IllegalArgumentException) {
            CodeNodeType.TRANSFORMER
        }
    }

    /**
     * Builds a map of simple class name → fully qualified class name from import statements.
     */
    private fun buildImportMap(content: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        importPattern.findAll(content).forEach { match ->
            val fqcn = match.groupValues[1]
            val simpleName = fqcn.substringAfterLast('.')
            // Skip wildcard imports and framework imports
            if (simpleName != "*" && !fqcn.startsWith("io.codenode.fbpdsl.")) {
                map[simpleName] = fqcn
            }
        }
        return map
    }

    /**
     * Resolves a type name to a KClass.
     * First checks built-in types, then attempts reflection via import map.
     */
    private fun resolveType(typeName: String): KClass<*> {
        return when (typeName) {
            "String" -> String::class
            "Int" -> Int::class
            "Long" -> Long::class
            "Double" -> Double::class
            "Float" -> Float::class
            "Boolean" -> Boolean::class
            "Byte" -> Byte::class
            "Short" -> Short::class
            "Char" -> Char::class
            "Unit" -> Unit::class
            "Any" -> Any::class
            else -> externalTypeResolver?.invoke(typeName)
                ?: resolveCustomType(typeName)
        }
    }

    /**
     * Attempts to resolve a custom type name via the import map and reflection.
     * Falls back to Any::class if the class is not on the classpath.
     */
    private fun resolveCustomType(typeName: String): KClass<*> {
        val fqcn = importMap[typeName] ?: return Any::class
        return try {
            Class.forName(fqcn).kotlin
        } catch (_: ClassNotFoundException) {
            Any::class
        }
    }
}
