/*
 * GraphNodeTemplateSerializer - Save/load GraphNode templates as .flow.kts files
 * Wraps FlowGraphSerializer/FlowKtParser with a metadata comment header
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.*
import io.codenode.grapheditor.model.GraphNodeTemplateMeta
import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.grapheditor.state.IPTypeRegistry
import java.io.File

/**
 * Serializes and deserializes GraphNode templates as `.flow.kts` files with metadata headers.
 *
 * Each template file has a metadata comment header (with markers like @GraphNodeTemplate,
 * @TemplateName, @InputPorts, etc.) followed by standard .flow.kts DSL content generated
 * by FlowGraphSerializer.
 */
object GraphNodeTemplateSerializer {

    // Regex patterns for metadata extraction (applied to first ~15 lines)
    private val templateMarkerPattern = Regex("""@GraphNodeTemplate""")
    private val templateNamePattern = Regex("""@TemplateName\s+(.+)""")
    private val descriptionPattern = Regex("""@Description\s+(.+)""")
    private val inputPortsPattern = Regex("""@InputPorts\s+(\d+)""")
    private val outputPortsPattern = Regex("""@OutputPorts\s+(\d+)""")
    private val childNodesPattern = Regex("""@ChildNodes\s+(\d+)""")

    /**
     * Saves a GraphNode as a `.flow.kts` template file with metadata header.
     *
     * Wraps the GraphNode in a minimal FlowGraph for serialization via FlowGraphSerializer,
     * then prepends the metadata comment header.
     *
     * @param graphNode The GraphNode to save
     * @param outputFile The file to write to (parent directories created if needed)
     */
    fun saveTemplate(graphNode: GraphNode, outputFile: File) {
        outputFile.parentFile?.mkdirs()

        val header = buildMetadataHeader(graphNode)

        // Wrap the GraphNode in a FlowGraph for serialization
        val wrapperGraph = FlowGraph(
            id = "template_${graphNode.id}",
            name = graphNode.name,
            version = "1.0.0",
            description = graphNode.description,
            rootNodes = listOf(graphNode),
            connections = emptyList()
        )

        val dslContent = FlowGraphSerializer.serialize(wrapperGraph, includeImports = true)

        outputFile.writeText(header + "\n" + dslContent)
    }

    /**
     * Parses only the metadata header from a `.flow.kts` file.
     * Returns null if the file does not contain the `@GraphNodeTemplate` marker.
     *
     * @param file The `.flow.kts` file to parse
     * @return Metadata if valid template, null otherwise
     */
    fun parseMetadata(file: File): GraphNodeTemplateMeta? {
        if (!file.exists() || !file.isFile) return null

        // Read only the first 20 lines for metadata extraction
        val headerLines = file.useLines { lines ->
            lines.take(20).toList()
        }
        val headerText = headerLines.joinToString("\n")

        // Must contain the @GraphNodeTemplate marker
        if (!templateMarkerPattern.containsMatchIn(headerText)) return null

        val name = templateNamePattern.find(headerText)?.groupValues?.get(1)?.trim()
            ?: return null
        val description = descriptionPattern.find(headerText)?.groupValues?.get(1)?.trim()
        val inputPortCount = inputPortsPattern.find(headerText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val outputPortCount = outputPortsPattern.find(headerText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val childNodeCount = childNodesPattern.find(headerText)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return GraphNodeTemplateMeta(
            name = name,
            description = description,
            inputPortCount = inputPortCount,
            outputPortCount = outputPortCount,
            childNodeCount = childNodeCount,
            filePath = file.absolutePath,
            tier = PlacementLevel.PROJECT // Default; caller overrides via copy()
        )
    }

    /**
     * Fully deserializes a `.flow.kts` template file, returning the GraphNode
     * with child nodes, internal connections, and port mappings.
     *
     * @param file The `.flow.kts` template file to load
     * @return The reconstructed GraphNode, or null if parsing fails
     */
    fun loadTemplate(file: File, ipTypeRegistry: IPTypeRegistry? = null): GraphNode? {
        if (!file.exists() || !file.isFile) return null

        return try {
            val content = file.readText()
            val parser = FlowKtParser()

            // Wire IP type registry for custom type resolution
            if (ipTypeRegistry != null) {
                parser.setTypeResolver { typeName ->
                    ipTypeRegistry.getByTypeName(typeName)?.payloadType
                }
            }

            val result = parser.parseFlowKt(content)

            if (!result.isSuccess || result.graph == null) {
                println("Warning: Failed to parse GraphNode template ${file.name}: ${result.errorMessage}")
                return null
            }

            // The template wraps the GraphNode as a root node of a FlowGraph
            // (parser may also pick up child codeNodes as root-level due to flat regex matching)
            val graphNode = result.graph.rootNodes.filterIsInstance<GraphNode>().firstOrNull()
            if (graphNode == null) {
                println("Warning: Template ${file.name} does not contain a GraphNode as root")
                return null
            }

            // Enrich GraphNode with port type name hints from parsing.
            // (Hints cover the case where child port types resolve to Any::class)
            if (result.portTypeNameHints.isNotEmpty()) {
                enrichWithTypeHints(graphNode, result.portTypeNameHints)
            } else {
                graphNode
            }
        } catch (e: Exception) {
            println("Warning: Failed to load GraphNode template ${file.name}: ${e.message}")
            null
        }
    }

    /**
     * Stores port type name hints on GraphNode configuration for display.
     * Keys are by port name (stable across ID remapping during instantiation).
     * For each exposed port, traces through portMappings to find the child port's
     * original type name from the parser hints.
     */
    private fun enrichWithTypeHints(graphNode: GraphNode, hints: Map<String, String>): GraphNode {
        val typeHintConfig = mutableMapOf<String, String>()

        // Build child port lookup from hints: childNodeId:portName → typeName
        val childPortTypeNames = mutableMapOf<String, String>()
        for (childNode in graphNode.childNodes) {
            for (port in childNode.inputPorts + childNode.outputPorts) {
                hints[port.id]?.let { typeName ->
                    childPortTypeNames["${childNode.id}:${port.name}"] = typeName
                }
            }
        }

        // For each exposed port, trace through portMapping to find the child port type
        for (port in graphNode.inputPorts + graphNode.outputPorts) {
            val mapping = graphNode.portMappings[port.name] ?: graphNode.portMappings[port.id]
            if (mapping != null) {
                val typeName = childPortTypeNames["${mapping.childNodeId}:${mapping.childPortName}"]
                if (typeName != null) {
                    // Key by port name (stable across instantiation ID remapping)
                    typeHintConfig["_portTypeHint_${port.name}"] = typeName
                }
            }
        }

        if (typeHintConfig.isEmpty()) return graphNode

        return graphNode.copy(
            configuration = graphNode.configuration + typeHintConfig
        )
    }

    /**
     * Builds the metadata comment header for a GraphNode template file.
     */
    private fun buildMetadataHeader(graphNode: GraphNode): String {
        val builder = StringBuilder()
        builder.appendLine("/*")
        builder.appendLine(" * GraphNode Template: ${graphNode.name}")
        builder.appendLine(" * @GraphNodeTemplate")
        builder.appendLine(" * @TemplateName ${graphNode.name}")
        graphNode.description?.let { desc ->
            builder.appendLine(" * @Description $desc")
        }
        builder.appendLine(" * @InputPorts ${graphNode.inputPorts.size}")
        builder.appendLine(" * @OutputPorts ${graphNode.outputPorts.size}")
        builder.appendLine(" * @ChildNodes ${graphNode.childNodes.size}")
        builder.appendLine(" * Created: ${java.time.LocalDateTime.now()}")
        builder.appendLine(" * License: Apache 2.0")
        builder.appendLine(" */")
        return builder.toString()
    }
}
