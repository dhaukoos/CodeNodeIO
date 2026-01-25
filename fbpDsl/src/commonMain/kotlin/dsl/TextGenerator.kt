/*
 * TextGenerator - FlowGraph to Textual DSL Converter
 * Generates readable Kotlin DSL text representation from FlowGraph models
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.dsl

import io.codenode.fbpdsl.model.*
import kotlin.reflect.KClass

/**
 * Generates textual DSL representation from FlowGraph models
 *
 * Converts FlowGraph objects into readable Kotlin DSL text that matches
 * the original DSL syntax used to create the graphs.
 */
object TextGenerator {

    /**
     * Generate DSL text from a FlowGraph
     *
     * @param flowGraph The flow graph to convert to text
     * @return Formatted DSL text representation
     */
    fun generate(flowGraph: FlowGraph): String {
        return buildString {
            // Generate flowGraph declaration
            appendLine("flowGraph(")
            appendLine("    name = \"${flowGraph.name}\",")
            appendLine("    version = \"${flowGraph.version}\"")

            // Add description if present
            if (!flowGraph.description.isNullOrBlank()) {
                appendLine(",")
                appendLine("    description = \"${flowGraph.description}\"")
            }

            appendLine(") {")

            // Generate node declarations
            if (flowGraph.rootNodes.isNotEmpty()) {
                flowGraph.rootNodes.forEach { node ->
                    generateNodeDeclaration(node, this)
                }

                // Add blank line before connections if there are any
                if (flowGraph.connections.isNotEmpty()) {
                    appendLine()
                }
            }

            // Generate connections
            flowGraph.connections.forEach { connection ->
                generateConnection(connection, flowGraph, this)
            }

            appendLine("}")
        }
    }

    /**
     * Generate a node declaration
     */
    private fun generateNodeDeclaration(node: Node, builder: StringBuilder) {
        when (node) {
            is CodeNode -> generateCodeNode(node, builder)
            is GraphNode -> generateGraphNode(node, builder)
        }
    }

    /**
     * Generate a CodeNode declaration
     */
    private fun generateCodeNode(node: CodeNode, builder: StringBuilder) {
        builder.apply {
            appendLine()
            appendLine("    val ${sanitizeVariableName(node.name)} = codeNode(\"${node.name}\") {")

            // Generate input ports
            node.inputPorts.forEach { port ->
                appendLine("        input(\"${port.name}\", ${formatType(port.dataType)})")
            }

            // Generate output ports
            node.outputPorts.forEach { port ->
                appendLine("        output(\"${port.name}\", ${formatType(port.dataType)})")
            }

            appendLine("    }")
        }
    }

    /**
     * Generate a GraphNode declaration
     */
    private fun generateGraphNode(node: GraphNode, builder: StringBuilder) {
        builder.apply {
            appendLine()
            appendLine("    val ${sanitizeVariableName(node.name)} = graphNode(\"${node.name}\") {")

            // Generate input ports
            node.inputPorts.forEach { port ->
                appendLine("        input(\"${port.name}\", ${formatType(port.dataType)})")
            }

            // Generate output ports
            node.outputPorts.forEach { port ->
                appendLine("        output(\"${port.name}\", ${formatType(port.dataType)})")
            }

            appendLine("    }")
        }
    }

    /**
     * Generate a connection statement
     */
    private fun generateConnection(
        connection: Connection,
        flowGraph: FlowGraph,
        builder: StringBuilder
    ) {
        // Find source and target nodes
        val sourceNode = flowGraph.findNode(connection.sourceNodeId)
        val targetNode = flowGraph.findNode(connection.targetNodeId)

        if (sourceNode == null || targetNode == null) {
            return // Skip invalid connections
        }

        // Find port names
        val sourcePort = sourceNode.outputPorts.find { it.id == connection.sourcePortId }
        val targetPort = targetNode.inputPorts.find { it.id == connection.targetPortId }

        if (sourcePort == null || targetPort == null) {
            return // Skip invalid connections
        }

        builder.appendLine(
            "    ${sanitizeVariableName(sourceNode.name)}.output(\"${sourcePort.name}\") connect " +
            "${sanitizeVariableName(targetNode.name)}.input(\"${targetPort.name}\")"
        )
    }

    /**
     * Format a KClass type for DSL output
     */
    private fun formatType(type: KClass<*>): String {
        return when (type.simpleName) {
            "String" -> "String::class"
            "Int" -> "Int::class"
            "Long" -> "Long::class"
            "Float" -> "Float::class"
            "Double" -> "Double::class"
            "Boolean" -> "Boolean::class"
            "Any" -> "Any::class"
            else -> "${type.simpleName}::class"
        }
    }

    /**
     * Sanitize a node name to be a valid Kotlin variable name
     */
    private fun sanitizeVariableName(name: String): String {
        // Convert name to camelCase variable name
        // Remove spaces and special characters, capitalize after spaces
        return name
            .split(Regex("[\\s-_]+"))
            .mapIndexed { index, part ->
                if (index == 0) {
                    part.replaceFirstChar { it.lowercase() }
                } else {
                    part.replaceFirstChar { it.uppercase() }
                }
            }
            .joinToString("")
            .let { varName ->
                // Ensure it starts with a letter or underscore
                if (varName.firstOrNull()?.isLetter() == true || varName.startsWith("_")) {
                    varName
                } else {
                    "node$varName"
                }
            }
    }
}
