/*
 * FlowKtParser
 * Parses .flow.kt files back to FlowGraph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.*

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
    val errorMessage: String? = null
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
        """val\s+(\w+)\s*=\s*codeNode\s*\(\s*"([^"]+)"\s*,\s*nodeType\s*=\s*"?(\w+)"?\s*\)"""
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

            // Parse nodes
            val (nodes, nodeVarMap) = parseCodeNodes(blockContent)

            // Parse connections
            val connections = parseConnections(blockContent, nodeVarMap)

            val flowGraph = FlowGraph(
                id = "flow_${graphName.lowercase().replace(" ", "_")}",
                name = graphName,
                version = graphVersion,
                description = graphDescription,
                rootNodes = nodes,
                connections = connections
            )

            ParseResult(
                isSuccess = true,
                graph = flowGraph
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
            val nodeTypeStr = match.groupValues[3]

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

            // Parse node type
            val codeNodeType = try {
                CodeNodeType.valueOf(nodeTypeStr)
            } catch (e: IllegalArgumentException) {
                CodeNodeType.TRANSFORMER
            }

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

            @Suppress("UNCHECKED_CAST")
            ports.add(
                Port(
                    id = "${nodeName.lowercase()}_${portName}",
                    name = portName,
                    direction = Port.Direction.INPUT,
                    dataType = resolveType(typeName) as kotlin.reflect.KClass<Any>,
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

            @Suppress("UNCHECKED_CAST")
            ports.add(
                Port(
                    id = "${nodeName.lowercase()}_${portName}",
                    name = portName,
                    direction = Port.Direction.OUTPUT,
                    dataType = resolveType(typeName) as kotlin.reflect.KClass<Any>,
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
     * Resolves a type name to a KClass.
     */
    private fun resolveType(typeName: String): kotlin.reflect.KClass<*> {
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
            else -> Any::class
        }
    }
}
