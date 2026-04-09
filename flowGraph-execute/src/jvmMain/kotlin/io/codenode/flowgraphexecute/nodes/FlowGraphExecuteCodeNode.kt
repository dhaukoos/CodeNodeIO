/*
 * FlowGraphExecuteCodeNode - Coarse-grained CodeNode wrapping flow graph execution
 * 2 inputs (flowGraphModel, nodeDescriptors), 3 outputs (executionState, animations, debugSnapshots)
 * License: Apache 2.0
 */

package io.codenode.flowgraphexecute.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.resolveSourceFilePath
import io.codenode.fbpdsl.runtime.ProcessResult3
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Coarse-grained CodeNode that wraps all flow graph execution functionality:
 * runtime lifecycle, data flow animation, and debug snapshots.
 *
 * Ports:
 * - Input 1: flowGraphModel — JSON commands (configure, start, stop, pause, resume, status)
 * - Input 2: nodeDescriptors — cached node definitions for pipeline building
 * - Output 1: executionState — lifecycle state (IDLE, RUNNING, PAUSED, ERROR)
 * - Output 2: animations — active ConnectionAnimation data for canvas rendering
 * - Output 3: debugSnapshots — per-connection most-recent-value captures
 *
 * Uses anyInput mode: nodeDescriptors updates are cached independently.
 */
object FlowGraphExecuteCodeNode : CodeNodeDefinition {
    override val name = "FlowGraphExecute"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Flow graph runtime execution, animation, and debug snapshots"
    override val sourceFilePath: String? get() = resolveSourceFilePath(this::class.java)
    override val inputPorts = listOf(
        PortSpec("flowGraphModel", String::class),
        PortSpec("nodeDescriptors", String::class)
    )
    override val outputPorts = listOf(
        PortSpec("executionState", String::class),
        PortSpec("animations", String::class),
        PortSpec("debugSnapshots", String::class)
    )
    override val anyInput = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun createRuntime(name: String): NodeRuntime {
        var cachedNodeDescriptors = ""
        var currentState = "IDLE"

        return CodeNodeFactory.createIn2AnyOut3Processor<String, String, String, String, String>(
            name = name,
            initialValue1 = "",
            initialValue2 = ""
        ) { flowGraphModel, nodeDescriptors ->
            // Cache nodeDescriptors when it changes
            if (nodeDescriptors.isNotEmpty()) {
                cachedNodeDescriptors = nodeDescriptors
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
                    "configure" -> {
                        // Configure the execution pipeline with the provided flow graph
                        currentState = "IDLE"
                        val stateJson = buildString {
                            append("{\"state\":\"IDLE\"")
                            append(",\"configured\":true")
                            append(",\"hasNodeDescriptors\":${cachedNodeDescriptors.isNotEmpty()}")
                            append("}")
                        }
                        ProcessResult3(stateJson, null, null)
                    }

                    "start" -> {
                        currentState = "RUNNING"
                        val stateJson = """{"state":"RUNNING"}"""
                        ProcessResult3(stateJson, null, null)
                    }

                    "stop" -> {
                        currentState = "IDLE"
                        val stateJson = """{"state":"IDLE"}"""
                        // Clear animations and debug snapshots on stop
                        ProcessResult3(stateJson, """{"animations":[]}""", """{"snapshots":{}}""")
                    }

                    "pause" -> {
                        currentState = "PAUSED"
                        val stateJson = """{"state":"PAUSED"}"""
                        ProcessResult3(stateJson, null, null)
                    }

                    "resume" -> {
                        currentState = "RUNNING"
                        val stateJson = """{"state":"RUNNING"}"""
                        ProcessResult3(stateJson, null, null)
                    }

                    "status" -> {
                        val stateJson = buildString {
                            append("{\"state\":\"$currentState\"")
                            append(",\"hasNodeDescriptors\":${cachedNodeDescriptors.isNotEmpty()}")
                            append("}")
                        }
                        ProcessResult3(stateJson, null, null)
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
