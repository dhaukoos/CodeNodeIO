/*
 * GenerateContextAggregatorCodeNode - Aggregates flowGraphModel + serializedOutput into generationContext
 * Part of the two-node sub-FlowGraph for the flowGraph-generate module boundary
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.node

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object GenerateContextAggregatorCodeNode : CodeNodeDefinition {
    override val name = "GenerateContextAggregator"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Aggregates flowGraphModel and serializedOutput into a unified generationContext"
    override val inputPorts = listOf(
        PortSpec("flowGraphModel", String::class),
        PortSpec("serializedOutput", String::class)
    )
    override val outputPorts = listOf(
        PortSpec("generationContext", String::class)
    )
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createIn2AnyOut1Processor<String, String, String>(
            name = name,
            initialValue1 = "",
            initialValue2 = ""
        ) { flowGraphModel, serializedOutput ->
            buildJsonObject {
                put("flowGraphModel", flowGraphModel)
                put("serializedOutput", serializedOutput)
            }.toString()
        }
    }
}
