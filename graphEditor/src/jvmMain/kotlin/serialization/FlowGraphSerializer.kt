/*
 * FlowGraphSerializer - Graph Serialization to .flow.kts DSL Format
 * Converts FlowGraph model instances to Kotlin DSL script format for persistence
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.*
import io.codenode.fbpdsl.model.PassThruPort
import java.io.File
import java.io.Writer

/**
 * Serializes FlowGraph instances to .flow.kts Kotlin DSL format
 * Generates human-readable, executable Kotlin script files
 */
object FlowGraphSerializer {

    /**
     * Serializes a FlowGraph to a Kotlin DSL string
     *
     * @param graph The flow graph to serialize
     * @param includeImports Whether to include import statements (default: true)
     * @param indentSize Number of spaces per indentation level (default: 4)
     * @return Kotlin DSL string representation
     */
    fun serialize(
        graph: FlowGraph,
        includeImports: Boolean = true,
        indentSize: Int = 4
    ): String {
        val builder = StringBuilder()
        val indent = " ".repeat(indentSize)

        // Add file header comment
        builder.appendLine("/*")
        builder.appendLine(" * Flow Graph: ${graph.name}")
        builder.appendLine(" * Version: ${graph.version}")
        graph.description?.let { desc ->
            builder.appendLine(" * Description: $desc")
        }
        builder.appendLine(" * Generated: ${java.time.LocalDateTime.now()}")
        builder.appendLine(" */")
        builder.appendLine()

        // Add imports if requested
        if (includeImports) {
            builder.appendLine("import io.codenode.fbpdsl.dsl.*")
            builder.appendLine("import io.codenode.fbpdsl.model.*")
            builder.appendLine()
        }

        // Start flowGraph DSL
        builder.append("val graph = flowGraph(")
        builder.append("\"${escapeString(graph.name)}\"")
        builder.append(", version = \"${graph.version}\"")
        graph.description?.let { desc ->
            builder.append(", description = \"${escapeString(desc)}\"")
        }
        builder.appendLine(") {")

        // Add metadata if present
        if (graph.metadata.isNotEmpty()) {
            builder.appendLine("${indent}// Metadata")
            graph.metadata.forEach { (key, value) ->
                builder.appendLine("${indent}metadata(\"${escapeString(key)}\", \"${escapeString(value)}\")")
            }
            builder.appendLine()
        }

        // Add target platforms if present
        if (graph.targetPlatforms.isNotEmpty()) {
            builder.appendLine("${indent}// Target Platforms")
            graph.targetPlatforms.forEach { platform ->
                builder.appendLine("${indent}targetPlatform(FlowGraph.TargetPlatform.${platform.name})")
            }
            builder.appendLine()
        }

        // Add nodes
        if (graph.rootNodes.isNotEmpty()) {
            builder.appendLine("${indent}// Nodes")
            val nodeVariables = mutableMapOf<String, String>()
            val allNodes = mutableMapOf<String, Node>()

            graph.rootNodes.forEachIndexed { index, node ->
                val varName = sanitizeVariableName(node.name) + if (index > 0) index else ""
                nodeVariables[node.id] = varName
                allNodes[node.id] = node

                when (node) {
                    is CodeNode -> serializeCodeNode(node, varName, builder, indent)
                    is GraphNode -> serializeGraphNode(node, varName, builder, indent, graph.connections)
                    else -> builder.appendLine("${indent}// Unknown node type: ${node::class.simpleName}")
                }
                builder.appendLine()
            }

            // Add connections
            if (graph.connections.isNotEmpty()) {
                builder.appendLine("${indent}// Connections")
                graph.connections.forEach { connection ->
                    serializeConnection(connection, nodeVariables, allNodes, builder, indent)
                }
            }
        }

        builder.appendLine("}")

        return builder.toString()
    }

    /**
     * Serializes a CodeNode to DSL format
     */
    private fun serializeCodeNode(
        node: CodeNode,
        varName: String,
        builder: StringBuilder,
        indent: String
    ) {
        builder.append("${indent}val $varName = codeNode(\"${escapeString(node.name)}\"")

        // Add node type if specified
        if (node.codeNodeType != CodeNodeType.CUSTOM) {
            builder.append(", nodeType = \"${node.codeNodeType.name}\"")
        }

        builder.appendLine(") {")

        val innerIndent = indent + " ".repeat(4)

        // Add description if present
        node.description?.let { desc ->
            builder.appendLine("${innerIndent}description = \"${escapeString(desc)}\"")
        }

        // Add position
        builder.appendLine("${innerIndent}position(${node.position.x}, ${node.position.y})")

        // Add input ports
        node.inputPorts.forEach { port ->
            builder.append("${innerIndent}input(\"${escapeString(port.name)}\", ${port.dataType.simpleName}::class")
            if (port.required) {
                builder.append(", required = true")
            }
            builder.appendLine(")")
        }

        // Add output ports
        node.outputPorts.forEach { port ->
            builder.append("${innerIndent}output(\"${escapeString(port.name)}\", ${port.dataType.simpleName}::class")
            if (port.required) {
                builder.append(", required = true")
            }
            builder.appendLine(")")
        }

        // Add configuration if present
        if (node.configuration.isNotEmpty()) {
            node.configuration.forEach { (key, value) ->
                builder.appendLine("${innerIndent}config(\"${escapeString(key)}\", \"${escapeString(value)}\")")
            }
        }

        builder.appendLine("${indent}}")
    }

    /**
     * Serializes a GraphNode to DSL format with full support for:
     * - Recursive child node serialization (T078)
     * - Internal connections (T079)
     * - Port mappings (T080)
     * - T060: PassThruPort upstream/downstream references
     *
     * @param node The GraphNode to serialize
     * @param varName The variable name to use in DSL
     * @param builder The StringBuilder to write to
     * @param indent The current indentation level
     * @param externalConnections Connections from/to this GraphNode from parent scope
     */
    private fun serializeGraphNode(
        node: GraphNode,
        varName: String,
        builder: StringBuilder,
        indent: String,
        externalConnections: List<Connection> = emptyList()
    ) {
        builder.appendLine("${indent}val $varName = graphNode(\"${escapeString(node.name)}\") {")

        val innerIndent = indent + " ".repeat(4)

        // Add description if present
        node.description?.let { desc ->
            builder.appendLine("${innerIndent}description = \"${escapeString(desc)}\"")
        }

        // Add position
        builder.appendLine("${innerIndent}position(${node.position.x}, ${node.position.y})")

        // Build childVariables map for all child nodes (needed for port mappings)
        val childVariables = mutableMapOf<String, String>()
        node.childNodes.forEachIndexed { index, childNode ->
            val childVarName = "child_${sanitizeVariableName(childNode.name)}" +
                if (index > 0) "_$index" else ""
            childVariables[childNode.id] = childVarName
        }

        // T078: Serialize child nodes recursively
        if (node.childNodes.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("${innerIndent}// Child nodes")

            node.childNodes.forEachIndexed { index, childNode ->
                val childVarName = childVariables[childNode.id]!!

                when (childNode) {
                    is CodeNode -> serializeCodeNode(childNode, childVarName, builder, innerIndent)
                    is GraphNode -> serializeGraphNode(childNode, childVarName, builder, innerIndent, node.internalConnections)
                    else -> builder.appendLine("${innerIndent}// Unknown child node type: ${childNode::class.simpleName}")
                }
                builder.appendLine()
            }

            // T079: Serialize internal connections
            if (node.internalConnections.isNotEmpty()) {
                builder.appendLine("${innerIndent}// Internal connections")
                node.internalConnections.forEach { connection ->
                    serializeInternalConnection(connection, childVariables, node.childNodes, builder, innerIndent)
                }
                builder.appendLine()
            }
        }

        // T080: Serialize port mappings
        // Use child node VARIABLE NAME (unique) and port NAME (not IDs) for stable serialization
        if (node.portMappings.isNotEmpty()) {
            builder.appendLine("${innerIndent}// Port mappings")
            node.portMappings.forEach { (portName, mapping) ->
                // Look up child node to get its unique variable name (handles duplicate names)
                val childNode = node.childNodes.find { it.id == mapping.childNodeId }
                val childVarName = childVariables[mapping.childNodeId] ?: childNode?.name ?: mapping.childNodeId

                // Look up child port to get its name (more stable than ID)
                val childPort = if (mapping.childPortName.isNotEmpty()) {
                    childNode?.inputPorts?.find { it.id == mapping.childPortName || it.name == mapping.childPortName }
                        ?: childNode?.outputPorts?.find { it.id == mapping.childPortName || it.name == mapping.childPortName }
                } else null
                val childPortName = childPort?.name ?: mapping.childPortName

                builder.appendLine("${innerIndent}portMapping(\"${escapeString(portName)}\", \"${escapeString(childVarName)}\", \"${escapeString(childPortName)}\")")
            }
        }

        // Serialize GraphNode's own input/output ports if present
        // T060: Enhanced to include PassThruPort upstream/downstream references
        if (node.inputPorts.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("${innerIndent}// Exposed input ports")
            node.inputPorts.forEach { port ->
                serializeExposedPort(port, "exposeInput", builder, innerIndent, node.portMappings, node.id, externalConnections)
            }
        }

        if (node.outputPorts.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("${innerIndent}// Exposed output ports")
            node.outputPorts.forEach { port ->
                serializeExposedPort(port, "exposeOutput", builder, innerIndent, node.portMappings, node.id, externalConnections)
            }
        }

        builder.appendLine("${indent}}")
    }

    /**
     * Serializes an exposed port (input or output) on a GraphNode.
     * T060: Includes PassThruPort upstream/downstream references if present.
     *
     * @param port The port to serialize (may be a PassThruPort or regular Port)
     * @param keyword The DSL keyword to use (exposeInput or exposeOutput)
     * @param builder The StringBuilder to write to
     * @param indent The current indentation level
     * @param portMappings The GraphNode's port mappings for reference
     * @param graphNodeId The ID of the GraphNode owning this port
     * @param externalConnections Connections from/to this GraphNode from parent scope
     */
    private fun serializeExposedPort(
        port: Port<*>,
        keyword: String,
        builder: StringBuilder,
        indent: String,
        portMappings: Map<String, GraphNode.PortMapping>,
        graphNodeId: String,
        externalConnections: List<Connection>
    ) {
        builder.append("${indent}$keyword(\"${escapeString(port.name)}\", ${port.dataType.simpleName ?: "Any"}::class")

        if (port.required) {
            builder.append(", required = true")
        }

        // T060: Add upstream/downstream references from port mapping and external connections
        val mapping = portMappings[port.name]

        if (keyword == "exposeInput") {
            // For INPUT: upstream is external (from connections), downstream is internal (from mapping)
            val incomingConn = externalConnections.find {
                it.targetNodeId == graphNodeId && it.targetPortId == port.id
            }
            if (incomingConn != null) {
                builder.append(", upstream = \"${escapeString(incomingConn.sourceNodeId)}:${escapeString(incomingConn.sourcePortId)}\"")
            }
            if (mapping != null) {
                builder.append(", downstream = \"${escapeString(mapping.childNodeId)}:${escapeString(mapping.childPortName)}\"")
            }
        } else {
            // For OUTPUT: upstream is internal (from mapping), downstream is external (from connections)
            if (mapping != null) {
                builder.append(", upstream = \"${escapeString(mapping.childNodeId)}:${escapeString(mapping.childPortName)}\"")
            }
            val outgoingConn = externalConnections.find {
                it.sourceNodeId == graphNodeId && it.sourcePortId == port.id
            }
            if (outgoingConn != null) {
                builder.append(", downstream = \"${escapeString(outgoingConn.targetNodeId)}:${escapeString(outgoingConn.targetPortId)}\"")
            }
        }

        builder.appendLine(")")
    }

    /**
     * Serializes an internal connection within a GraphNode.
     * Looks up actual port names from the child nodes instead of extracting from IDs.
     */
    private fun serializeInternalConnection(
        connection: Connection,
        childVariables: Map<String, String>,
        childNodes: List<Node>,
        builder: StringBuilder,
        indent: String
    ) {
        val sourceVar = childVariables[connection.sourceNodeId] ?: "unknownChild"
        val targetVar = childVariables[connection.targetNodeId] ?: "unknownChild"

        // Look up actual port names from the child nodes
        val sourceNode = childNodes.find { it.id == connection.sourceNodeId }
        val targetNode = childNodes.find { it.id == connection.targetNodeId }

        val sourcePort = sourceNode?.outputPorts?.find { it.id == connection.sourcePortId }
        val targetPort = targetNode?.inputPorts?.find { it.id == connection.targetPortId }

        val sourcePortName = sourcePort?.name ?: connection.sourcePortId.substringAfterLast("_")
        val targetPortName = targetPort?.name ?: connection.targetPortId.substringAfterLast("_")

        builder.append("${indent}internalConnection($sourceVar, \"$sourcePortName\", $targetVar, \"$targetPortName\")")

        // Add IP type if specified
        connection.ipTypeId?.let { ipTypeId ->
            builder.append(" withType \"$ipTypeId\"")
        }

        builder.appendLine()
    }

    /**
     * Serializes a Connection to DSL format
     *
     * @param connection The connection to serialize
     * @param nodeVariables Map of node ID to variable name
     * @param allNodes Map of node ID to Node (for port lookup)
     * @param builder The StringBuilder to write to
     * @param indent The current indentation level
     */
    private fun serializeConnection(
        connection: Connection,
        nodeVariables: Map<String, String>,
        allNodes: Map<String, Node>,
        builder: StringBuilder,
        indent: String
    ) {
        val sourceVar = nodeVariables[connection.sourceNodeId] ?: "unknownNode"
        val targetVar = nodeVariables[connection.targetNodeId] ?: "unknownNode"

        // Look up actual port names from nodes
        val sourceNode = allNodes[connection.sourceNodeId]
        val targetNode = allNodes[connection.targetNodeId]

        val sourcePort = sourceNode?.outputPorts?.find { it.id == connection.sourcePortId }
        val targetPort = targetNode?.inputPorts?.find { it.id == connection.targetPortId }

        val sourcePortName = sourcePort?.name ?: connection.sourcePortId.substringAfterLast("_")
        val targetPortName = targetPort?.name ?: connection.targetPortId.substringAfterLast("_")

        builder.append("${indent}$sourceVar.output(\"$sourcePortName\") connect ")
        builder.append("$targetVar.input(\"$targetPortName\")")

        // Add IP type if specified
        connection.ipTypeId?.let { ipTypeId ->
            builder.append(" withType \"$ipTypeId\"")
        }

        builder.appendLine()
    }

    /**
     * Serializes a FlowGraph to a file
     *
     * @param graph The flow graph to serialize
     * @param file The target file
     * @param includeImports Whether to include import statements
     */
    fun serializeToFile(
        graph: FlowGraph,
        file: File,
        includeImports: Boolean = true
    ) {
        val content = serialize(graph, includeImports)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    /**
     * Serializes a FlowGraph to a Writer
     *
     * @param graph The flow graph to serialize
     * @param writer The writer to write to
     * @param includeImports Whether to include import statements
     */
    fun serializeToWriter(
        graph: FlowGraph,
        writer: Writer,
        includeImports: Boolean = true
    ) {
        val content = serialize(graph, includeImports)
        writer.write(content)
        writer.flush()
    }

    /**
     * Escapes special characters in strings for Kotlin code
     */
    private fun escapeString(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Sanitizes a string to be a valid Kotlin variable name
     */
    private fun sanitizeVariableName(name: String): String {
        // Replace invalid characters with underscore
        val sanitized = name
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .replace(Regex("^[0-9]"), "_$0") // Variables can't start with numbers
            .lowercase()

        // Ensure it's not a Kotlin keyword
        return if (isKotlinKeyword(sanitized)) "${sanitized}_node" else sanitized
    }

    /**
     * Checks if a string is a Kotlin keyword
     */
    private fun isKotlinKeyword(name: String): Boolean {
        val keywords = setOf(
            "abstract", "actual", "annotation", "as", "break", "by", "catch", "class",
            "companion", "const", "constructor", "continue", "crossinline", "data",
            "delegate", "do", "dynamic", "else", "enum", "expect", "external", "false",
            "field", "file", "final", "finally", "for", "fun", "get", "if", "import",
            "in", "infix", "init", "inline", "inner", "interface", "internal", "is",
            "it", "lateinit", "noinline", "null", "object", "open", "operator", "out",
            "override", "package", "param", "private", "property", "protected", "public",
            "receiver", "reified", "return", "sealed", "set", "setparam", "super",
            "suspend", "tailrec", "this", "throw", "true", "try", "typealias", "typeof",
            "val", "var", "vararg", "when", "where", "while"
        )
        return name in keywords
    }

    /**
     * Validates that a graph can be serialized
     *
     * @param graph The graph to validate
     * @return ValidationResult with any serialization issues
     */
    fun validateForSerialization(graph: FlowGraph): ValidationResult {
        val errors = mutableListOf<String>()

        // Check basic graph validity
        if (graph.id.isBlank()) {
            errors.add("Graph ID cannot be blank")
        }
        if (graph.name.isBlank()) {
            errors.add("Graph name cannot be blank")
        }
        if (graph.version.isBlank()) {
            errors.add("Graph version cannot be blank")
        }

        // Check for duplicate node names (would cause variable name conflicts)
        val nodeNames = graph.rootNodes.map { it.name }
        val duplicates = nodeNames.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            errors.add("Duplicate node names found: ${duplicates.keys.joinToString(", ")}")
        }

        // Validate connections reference existing nodes
        graph.connections.forEach { conn ->
            if (graph.findNode(conn.sourceNodeId) == null) {
                errors.add("Connection references non-existent source node: ${conn.sourceNodeId}")
            }
            if (graph.findNode(conn.targetNodeId) == null) {
                errors.add("Connection references non-existent target node: ${conn.targetNodeId}")
            }
        }

        return ValidationResult(
            success = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Generates a filename for a flow graph
     *
     * @param graph The graph
     * @param extension File extension (default: ".flow.kts")
     * @return Suggested filename
     */
    fun generateFilename(graph: FlowGraph, extension: String = ".flow.kts"): String {
        val sanitizedName = graph.name
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .lowercase()
        return "$sanitizedName$extension"
    }
}
