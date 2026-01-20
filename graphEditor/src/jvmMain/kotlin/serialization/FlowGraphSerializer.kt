/*
 * FlowGraphSerializer - Graph Serialization to .flow.kts DSL Format
 * Converts FlowGraph model instances to Kotlin DSL script format for persistence
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.*
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

            graph.rootNodes.forEachIndexed { index, node ->
                val varName = sanitizeVariableName(node.name) + if (index > 0) index else ""
                nodeVariables[node.id] = varName

                when (node) {
                    is CodeNode -> serializeCodeNode(node, varName, builder, indent)
                    is GraphNode -> serializeGraphNode(node, varName, builder, indent)
                    else -> builder.appendLine("${indent}// Unknown node type: ${node::class.simpleName}")
                }
                builder.appendLine()
            }

            // Add connections
            if (graph.connections.isNotEmpty()) {
                builder.appendLine("${indent}// Connections")
                graph.connections.forEach { connection ->
                    serializeConnection(connection, nodeVariables, builder, indent)
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
     * Serializes a GraphNode to DSL format
     */
    private fun serializeGraphNode(
        node: GraphNode,
        varName: String,
        builder: StringBuilder,
        indent: String
    ) {
        builder.appendLine("${indent}val $varName = graphNode(\"${escapeString(node.name)}\") {")

        val innerIndent = indent + " ".repeat(4)

        // Add description if present
        node.description?.let { desc ->
            builder.appendLine("${innerIndent}description = \"${escapeString(desc)}\"")
        }

        // Add position
        builder.appendLine("${innerIndent}position(${node.position.x}, ${node.position.y})")

        // Add child nodes recursively (simplified - would need full recursive handling)
        if (node.childNodes.isNotEmpty()) {
            builder.appendLine("${innerIndent}// Child nodes (${node.childNodes.size})")
            builder.appendLine("${innerIndent}// Note: Nested nodes serialization not yet implemented")
        }

        builder.appendLine("${indent}}")
    }

    /**
     * Serializes a Connection to DSL format
     */
    private fun serializeConnection(
        connection: Connection,
        nodeVariables: Map<String, String>,
        builder: StringBuilder,
        indent: String
    ) {
        val sourceVar = nodeVariables[connection.sourceNodeId] ?: "unknownNode"
        val targetVar = nodeVariables[connection.targetNodeId] ?: "unknownNode"

        // Find port names (simplified - assumes port IDs contain port names)
        val sourcePortName = connection.sourcePortId.substringAfterLast("_")
        val targetPortName = connection.targetPortId.substringAfterLast("_")

        builder.append("${indent}$sourceVar.output(\"$sourcePortName\") connect ")
        builder.appendLine("$targetVar.input(\"$targetPortName\")")
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
