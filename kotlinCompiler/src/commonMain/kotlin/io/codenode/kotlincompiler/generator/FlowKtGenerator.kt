/*
 * FlowKtGenerator
 * Generates .flow.kt compiled Kotlin files from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*

/**
 * Generator for .flow.kt compiled Kotlin files.
 *
 * Generates a Kotlin source file that represents a FlowGraph using the FBP DSL.
 * The generated file can be compiled as regular Kotlin code (not a script).
 *
 * T018: Create FlowKtGenerator class with generateFlowKt method
 * T019: Implement flowGraph DSL block generation
 * T020: Implement codeNode DSL block generation
 * T021: Implement connection DSL statement generation
 */
class FlowKtGenerator {

    companion object {
        private const val INDENT_SIZE = 4
    }

    /**
     * Generates a .flow.kt file content from a FlowGraph.
     *
     * @param flowGraph The flow graph to serialize
     * @param packageName The package name for the generated file
     * @param unused Unused parameter, retained for API compatibility
     * @return Generated Kotlin source code
     */
    fun generateFlowKt(
        flowGraph: FlowGraph,
        packageName: String,
        unused: String? = null,
        ipTypeNames: Map<String, String> = emptyMap(),
        ipTypeImports: List<String> = emptyList()
    ): String {
        // Build portId → typeName map from connections with IP type assignments
        val portTypeOverrides = buildMap<String, String> {
            for (connection in flowGraph.connections) {
                val typeId = connection.ipTypeId ?: continue
                val typeName = ipTypeNames[typeId] ?: continue
                put(connection.sourcePortId, typeName)
                put(connection.targetPortId, typeName)
            }
        }

        val builder = StringBuilder()
        val indent = " ".repeat(INDENT_SIZE)

        // T012: Generate package declaration
        builder.appendLine("package $packageName")
        builder.appendLine()

        // Generate imports
        builder.appendLine("import io.codenode.fbpdsl.dsl.*")
        builder.appendLine("import io.codenode.fbpdsl.model.*")
        for (importLine in ipTypeImports) {
            builder.appendLine("import $importLine")
        }
        builder.appendLine()

        // T019: Generate flowGraph DSL block
        val graphVarName = sanitizeVariableName(flowGraph.name) + "FlowGraph"
        builder.append("val $graphVarName = flowGraph(")
        builder.append("\"${escapeString(flowGraph.name)}\"")
        builder.append(", version = \"${flowGraph.version}\"")
        flowGraph.description?.let { desc ->
            builder.append(", description = \"${escapeString(desc)}\"")
        }
        builder.appendLine(") {")

        // Add metadata if present
        if (flowGraph.metadata.isNotEmpty()) {
            flowGraph.metadata.forEach { (key, value) ->
                builder.appendLine("${indent}metadata(\"${escapeString(key)}\", \"${escapeString(value)}\")")
            }
            builder.appendLine()
        }

        // Add target platforms if present
        if (flowGraph.targetPlatforms.isNotEmpty()) {
            flowGraph.targetPlatforms.forEach { platform ->
                builder.appendLine("${indent}targetPlatform(FlowGraph.TargetPlatform.${platform.name})")
            }
            builder.appendLine()
        }

        // T020: Generate codeNode DSL blocks
        val nodeVariables = mutableMapOf<String, String>()
        val allNodes = mutableMapOf<String, Node>()

        flowGraph.rootNodes.forEachIndexed { index, node ->
            val varName = sanitizeVariableName(node.name) +
                if (flowGraph.rootNodes.count { it.name == node.name } > 1) "_$index" else ""
            nodeVariables[node.id] = varName
            allNodes[node.id] = node

            when (node) {
                is CodeNode -> generateCodeNode(node, varName, builder, indent, portTypeOverrides)
                is GraphNode -> generateGraphNode(node, varName, builder, indent, flowGraph.connections, nodeVariables, allNodes, portTypeOverrides)
                else -> builder.appendLine("${indent}// Unknown node type: ${node::class.simpleName}")
            }
            builder.appendLine()
        }

        // T021: Generate connection DSL statements
        if (flowGraph.connections.isNotEmpty()) {
            flowGraph.connections.forEach { connection ->
                generateConnection(connection, nodeVariables, allNodes, builder, indent)
            }
        }

        builder.appendLine("}")

        return builder.toString()
    }

    /**
     * T020: Generates a codeNode DSL block with all properties.
     */
    private fun generateCodeNode(
        node: CodeNode,
        varName: String,
        builder: StringBuilder,
        indent: String,
        portTypeOverrides: Map<String, String> = emptyMap()
    ) {
        builder.append("${indent}val $varName = codeNode(\"${escapeString(node.name)}\"")
        builder.append(", nodeType = \"${node.codeNodeType.name}\"")
        builder.appendLine(") {")

        val innerIndent = indent + " ".repeat(INDENT_SIZE)

        // Add description if present
        node.description?.let { desc ->
            builder.appendLine("${innerIndent}description = \"${escapeString(desc)}\"")
        }

        // Add position
        builder.appendLine("${innerIndent}position(${node.position.x}, ${node.position.y})")

        // Add input ports
        node.inputPorts.forEach { port ->
            builder.append("${innerIndent}input(\"${escapeString(port.name)}\", ${getTypeName(port, portTypeOverrides)}::class")
            if (port.required) {
                builder.append(", required = true")
            }
            builder.appendLine(")")
        }

        // Add output ports
        node.outputPorts.forEach { port ->
            builder.append("${innerIndent}output(\"${escapeString(port.name)}\", ${getTypeName(port, portTypeOverrides)}::class")
            if (port.required) {
                builder.append(", required = true")
            }
            builder.appendLine(")")
        }

        // Add configuration (excluding internal keys prefixed with _, except preserved metadata keys)
        val preservedInternalKeys = setOf("_codeNodeClass", "_genericType", "_codeNodeDefinition")
        node.configuration.filter { !it.key.startsWith("_") || it.key in preservedInternalKeys }.forEach { (key, value) ->
            builder.appendLine("${innerIndent}config(\"${escapeString(key)}\", \"${escapeString(value)}\")")
        }

        builder.appendLine("${indent}}")
    }

    /**
     * Generates a graphNode DSL block with recursive child serialization.
     */
    private fun generateGraphNode(
        node: GraphNode,
        varName: String,
        builder: StringBuilder,
        indent: String,
        externalConnections: List<Connection>,
        parentNodeVariables: MutableMap<String, String>,
        parentAllNodes: MutableMap<String, Node>,
        portTypeOverrides: Map<String, String> = emptyMap()
    ) {
        builder.appendLine("${indent}val $varName = graphNode(\"${escapeString(node.name)}\") {")

        val innerIndent = indent + " ".repeat(INDENT_SIZE)

        // Add description if present
        node.description?.let { desc ->
            builder.appendLine("${innerIndent}description = \"${escapeString(desc)}\"")
        }

        // Add position
        builder.appendLine("${innerIndent}position(${node.position.x}, ${node.position.y})")

        // Build child variable map
        val childVariables = mutableMapOf<String, String>()
        node.childNodes.forEachIndexed { index, childNode ->
            val childVarName = "child_${sanitizeVariableName(childNode.name)}" +
                if (index > 0) "_$index" else ""
            childVariables[childNode.id] = childVarName
        }

        // Serialize child nodes
        if (node.childNodes.isNotEmpty()) {
            node.childNodes.forEachIndexed { index, childNode ->
                val childVarName = childVariables[childNode.id]!!

                when (childNode) {
                    is CodeNode -> generateCodeNode(childNode, childVarName, builder, innerIndent, portTypeOverrides)
                    is GraphNode -> generateGraphNode(
                        childNode, childVarName, builder, innerIndent,
                        node.internalConnections, childVariables, mutableMapOf(), portTypeOverrides
                    )
                    else -> builder.appendLine("${innerIndent}// Unknown child node type")
                }
                builder.appendLine()
            }

            // Internal connections
            if (node.internalConnections.isNotEmpty()) {
                node.internalConnections.forEach { connection ->
                    generateInternalConnection(connection, childVariables, node.childNodes, builder, innerIndent)
                }
                builder.appendLine()
            }
        }

        // Port mappings
        if (node.portMappings.isNotEmpty()) {
            node.portMappings.forEach { (portName, mapping) ->
                val childNode = node.childNodes.find { it.id == mapping.childNodeId }
                val childVarName = childVariables[mapping.childNodeId] ?: childNode?.name ?: mapping.childNodeId
                val childPortName = mapping.childPortName
                builder.appendLine("${innerIndent}portMapping(\"${escapeString(portName)}\", \"${escapeString(childVarName)}\", \"${escapeString(childPortName)}\")")
            }
            builder.appendLine()
        }

        // Exposed ports
        if (node.inputPorts.isNotEmpty()) {
            node.inputPorts.forEach { port ->
                builder.appendLine("${innerIndent}exposeInput(\"${escapeString(port.name)}\", ${getTypeName(port, portTypeOverrides)}::class)")
            }
        }
        if (node.outputPorts.isNotEmpty()) {
            node.outputPorts.forEach { port ->
                builder.appendLine("${innerIndent}exposeOutput(\"${escapeString(port.name)}\", ${getTypeName(port, portTypeOverrides)}::class)")
            }
        }

        builder.appendLine("${indent}}")
    }

    /**
     * T021: Generates a connection DSL statement.
     */
    private fun generateConnection(
        connection: Connection,
        nodeVariables: Map<String, String>,
        allNodes: Map<String, Node>,
        builder: StringBuilder,
        indent: String
    ) {
        val sourceVar = nodeVariables[connection.sourceNodeId] ?: return
        val targetVar = nodeVariables[connection.targetNodeId] ?: return

        // Look up actual port names from nodes
        val sourceNode = allNodes[connection.sourceNodeId]
        val targetNode = allNodes[connection.targetNodeId]

        val sourcePort = sourceNode?.outputPorts?.find { it.id == connection.sourcePortId }
        val targetPort = targetNode?.inputPorts?.find { it.id == connection.targetPortId }

        val sourcePortName = sourcePort?.name ?: connection.sourcePortId.substringAfterLast("_")
        val targetPortName = targetPort?.name ?: connection.targetPortId.substringAfterLast("_")

        builder.append("${indent}$sourceVar.output(\"$sourcePortName\") connect ")
        builder.append("$targetVar.input(\"$targetPortName\")")
        if (connection.ipTypeId != null) {
            builder.append(" withType \"${connection.ipTypeId}\"")
        }
        builder.appendLine()
    }

    /**
     * Generates an internal connection within a GraphNode.
     */
    private fun generateInternalConnection(
        connection: Connection,
        childVariables: Map<String, String>,
        childNodes: List<Node>,
        builder: StringBuilder,
        indent: String
    ) {
        val sourceVar = childVariables[connection.sourceNodeId] ?: return
        val targetVar = childVariables[connection.targetNodeId] ?: return

        val sourceNode = childNodes.find { it.id == connection.sourceNodeId }
        val targetNode = childNodes.find { it.id == connection.targetNodeId }

        val sourcePort = sourceNode?.outputPorts?.find { it.id == connection.sourcePortId }
        val targetPort = targetNode?.inputPorts?.find { it.id == connection.targetPortId }

        val sourcePortName = sourcePort?.name ?: connection.sourcePortId.substringAfterLast("_")
        val targetPortName = targetPort?.name ?: connection.targetPortId.substringAfterLast("_")

        builder.append("${indent}$sourceVar.output(\"$sourcePortName\") connect $targetVar.input(\"$targetPortName\")")
        if (connection.ipTypeId != null) {
            builder.append(" withType \"${connection.ipTypeId}\"")
        }
        builder.appendLine()
    }

    /**
     * Gets the type name for a port, checking overrides from IP type assignments first.
     */
    private fun getTypeName(port: Port<*>, portTypeOverrides: Map<String, String> = emptyMap()): String {
        return portTypeOverrides[port.id] ?: port.dataType.simpleName ?: "Any"
    }

    /**
     * Escapes special characters in strings for Kotlin code.
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
     * Sanitizes a string to be a valid Kotlin variable name.
     */
    private fun sanitizeVariableName(name: String): String {
        val sanitized = name
            .replace(Regex("[^a-zA-Z0-9_]"), "")
            .replaceFirstChar { it.lowercase() }

        // Handle empty result or starts with digit
        val result = when {
            sanitized.isEmpty() -> "node"
            sanitized.first().isDigit() -> "_$sanitized"
            else -> sanitized
        }

        // Ensure it's not a Kotlin keyword
        return if (isKotlinKeyword(result)) "${result}Node" else result
    }

    /**
     * Checks if a string is a Kotlin keyword.
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
}
