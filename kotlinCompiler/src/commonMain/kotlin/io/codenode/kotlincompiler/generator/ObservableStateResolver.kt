/*
 * ObservableStateResolver
 * Extracts observable state properties from source output ports and sink input ports
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.Port

/**
 * Represents an observable state property derived from a boundary port
 * (source node output port or sink node input port).
 *
 * @property name camelCase property name (e.g., "seconds")
 * @property typeName Kotlin type name (e.g., "Int", "String")
 * @property sourceNodeName Node name for KDoc reference
 * @property sourcePortName Port name for KDoc reference
 * @property defaultValue Default value for MutableStateFlow initialization
 */
data class ObservableProperty(
    val name: String,
    val typeName: String,
    val sourceNodeName: String,
    val sourcePortName: String,
    val defaultValue: String
)

/**
 * Resolves observable state properties from a FlowGraph.
 *
 * Observable state is derived from boundary ports:
 * - Source node output ports (data entering the graph from the UI)
 * - Sink node input ports (data exiting the graph to the UI)
 * - The port name becomes the StateFlow property name
 * - The port data type becomes the StateFlow generic type parameter
 * - When multiple boundary ports share the same name, names are
 *   disambiguated by prefixing with the node name
 */
class ObservableStateResolver {

    /**
     * Extracts observable state properties from all source and sink nodes in the FlowGraph.
     *
     * Source output ports are collected first, then sink input ports.
     *
     * @param flowGraph The FlowGraph to analyze
     * @return List of observable properties derived from boundary ports
     */
    fun getObservableStateProperties(flowGraph: FlowGraph): List<ObservableProperty> {
        val allNodes = flowGraph.getAllCodeNodes()
        val sourceNodes = allNodes.filter { isSourceNode(it) }
        val sinkNodes = allNodes.filter { isSinkNode(it) }
        if (sourceNodes.isEmpty() && sinkNodes.isEmpty()) return emptyList()

        // Collect all boundary port names to detect collisions
        val allPortNames = mutableListOf<Pair<CodeNode, Port<*>>>()
        for (source in sourceNodes) {
            for (port in source.outputPorts) {
                allPortNames.add(source to port)
            }
        }
        for (sink in sinkNodes) {
            for (port in sink.inputPorts) {
                allPortNames.add(sink to port)
            }
        }

        // Detect name collisions
        val nameCounts = allPortNames.groupBy { it.second.name.camelCase() }
        val needsDisambiguation = nameCounts.filter { it.value.size > 1 }.keys

        return allPortNames.map { (node, port) ->
            val portName = port.name.camelCase()
            val propertyName = if (portName in needsDisambiguation) {
                "${node.name.camelCase()}${port.name.pascalCase()}"
            } else {
                portName
            }
            val typeName = port.dataType.simpleName ?: "Any"

            ObservableProperty(
                name = propertyName,
                typeName = typeName,
                sourceNodeName = node.name,
                sourcePortName = port.name,
                defaultValue = defaultForType(typeName)
            )
        }
    }

    /**
     * Determines if a node is a source (has output ports, no input ports).
     */
    private fun isSourceNode(node: CodeNode): Boolean {
        return node.inputPorts.isEmpty() && node.outputPorts.isNotEmpty()
    }

    /**
     * Determines if a node is a sink (has input ports, no output ports).
     */
    private fun isSinkNode(node: CodeNode): Boolean {
        return node.inputPorts.isNotEmpty() && node.outputPorts.isEmpty()
    }

    /**
     * Gets the default value for a given Kotlin type name.
     */
    private fun defaultForType(typeName: String): String = when (typeName) {
        "Int" -> "0"
        "Long" -> "0L"
        "Double" -> "0.0"
        "Float" -> "0.0f"
        "String" -> "\"\""
        "Boolean" -> "false"
        else -> "null"
    }
}
