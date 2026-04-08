/*
 * FlowGraphPersistCodeNode - Coarse-grained CodeNode wrapping flow graph persistence
 * 2 inputs (flowGraphModel, ipTypeMetadata), 3 outputs (serializedOutput, loadedFlowGraph, graphNodeTemplates)
 * License: Apache 2.0
 */

package io.codenode.flowgraphpersist.node

import io.codenode.fbpdsl.model.*
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.ProcessResult3
import io.codenode.flowgraphpersist.serialization.FlowGraphSerializer
import io.codenode.flowgraphpersist.serialization.FlowKtParser
import io.codenode.flowgraphpersist.state.GraphNodeTemplateRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Coarse-grained CodeNode that wraps all flow graph persistence functionality:
 * serialization, deserialization, and GraphNode template management.
 *
 * Ports:
 * - Input 1: flowGraphModel — JSON commands (serialize, deserialize, listTemplates, etc.)
 * - Input 2: ipTypeMetadata — cached IP type registry state for type resolution
 * - Output 1: serializedOutput — .flow.kt text (serialize result)
 * - Output 2: loadedFlowGraph — parsed FlowGraph data (deserialize result)
 * - Output 3: graphNodeTemplates — template metadata list (template CRUD results)
 *
 * Uses anyInput mode: ipTypeMetadata updates are cached independently.
 */
object FlowGraphPersistCodeNode : CodeNodeDefinition {
    override val name = "FlowGraphPersist"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Flow graph serialization, deserialization, and template management"
    override val inputPorts = listOf(
        PortSpec("flowGraphModel", String::class),
        PortSpec("ipTypeMetadata", String::class)
    )
    override val outputPorts = listOf(
        PortSpec("serializedOutput", String::class),
        PortSpec("loadedFlowGraph", String::class),
        PortSpec("graphNodeTemplates", String::class)
    )
    override val anyInput = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun createRuntime(name: String): NodeRuntime {
        val templateRegistry = GraphNodeTemplateRegistry()
        val parser = FlowKtParser()
        var cachedIpTypeMetadata = ""

        return CodeNodeFactory.createIn2AnyOut3Processor<String, String, String, String, String>(
            name = name,
            initialValue1 = "",
            initialValue2 = ""
        ) { flowGraphModel, ipTypeMetadata ->
            // Cache ipTypeMetadata when it changes
            if (ipTypeMetadata.isNotEmpty()) {
                cachedIpTypeMetadata = ipTypeMetadata
            }

            // If flowGraphModel is empty, no-op (return all nulls)
            if (flowGraphModel.isEmpty()) {
                return@createIn2AnyOut3Processor ProcessResult3(null, null, null)
            }

            // Parse the command from flowGraphModel
            try {
                val jsonObj = json.parseToJsonElement(flowGraphModel) as? JsonObject
                    ?: return@createIn2AnyOut3Processor ProcessResult3(null, null, null)

                val action = jsonObj["action"]?.jsonPrimitive?.content
                    ?: return@createIn2AnyOut3Processor ProcessResult3(null, null, null)

                when (action) {
                    "serialize" -> {
                        val graphName = jsonObj["graphName"]?.jsonPrimitive?.content ?: "Untitled"
                        val graphVersion = jsonObj["graphVersion"]?.jsonPrimitive?.content ?: "1.0.0"

                        // Build a minimal FlowGraph from the command data
                        val graph = FlowGraph(
                            id = "flow_${graphName.lowercase().replace(" ", "_")}",
                            name = graphName,
                            version = graphVersion,
                            rootNodes = emptyList(),
                            connections = emptyList()
                        )

                        val serialized = FlowGraphSerializer.serialize(graph)
                        ProcessResult3(serialized, null, null)
                    }

                    "deserialize" -> {
                        val content = jsonObj["content"]?.jsonPrimitive?.content
                            ?: return@createIn2AnyOut3Processor ProcessResult3(null, null, null)

                        val result = parser.parseFlowKt(content)
                        if (result.isSuccess && result.graph != null) {
                            val graph = result.graph
                            // Serialize graph info as JSON for the loadedFlowGraph output
                            val graphJson = buildString {
                                append("{\"name\":\"${graph.name}\"")
                                append(",\"version\":\"${graph.version}\"")
                                graph.description?.let { append(",\"description\":\"$it\"") }
                                append(",\"nodeCount\":${graph.rootNodes.size}")
                                append(",\"connectionCount\":${graph.connections.size}")
                                append("}")
                            }
                            ProcessResult3(null, graphJson, null)
                        } else {
                            ProcessResult3(null, null, null)
                        }
                    }

                    "listTemplates" -> {
                        val templates = templateRegistry.getAll()
                        val templatesJson = buildString {
                            append("{\"templates\":[")
                            templates.forEachIndexed { index, meta ->
                                if (index > 0) append(",")
                                append("{\"name\":\"${meta.name}\"")
                                meta.description?.let { append(",\"description\":\"$it\"") }
                                append(",\"inputPorts\":${meta.inputPortCount}")
                                append(",\"outputPorts\":${meta.outputPortCount}")
                                append(",\"childNodes\":${meta.childNodeCount}")
                                append(",\"tier\":\"${meta.tier.name}\"")
                                append("}")
                            }
                            append("]}")
                        }
                        ProcessResult3(null, null, templatesJson)
                    }

                    else -> ProcessResult3(null, null, null)
                }
            } catch (_: Exception) {
                // Malformed command — no output
                ProcessResult3(null, null, null)
            }
        }
    }
}
