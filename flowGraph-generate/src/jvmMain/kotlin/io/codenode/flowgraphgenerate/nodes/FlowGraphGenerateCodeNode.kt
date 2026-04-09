/*
 * FlowGraphGenerateCodeNode - Produces generated output from context, descriptors, and metadata
 * Part of the two-node sub-FlowGraph for the flowGraph-generate module boundary
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.resolveSourceFilePath
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object FlowGraphGenerateCodeNode : CodeNodeDefinition {
    override val name = "FlowGraphGenerate"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Generates deployable code from generationContext, nodeDescriptors, and ipTypeMetadata"
    override val sourceFilePath: String? get() = resolveSourceFilePath(this::class.java)
    override val inputPorts = listOf(
        PortSpec("generationContext", String::class),
        PortSpec("nodeDescriptors", String::class),
        PortSpec("ipTypeMetadata", String::class)
    )
    override val outputPorts = listOf(
        PortSpec("generatedOutput", String::class)
    )
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createIn3AnyOut1Processor<String, String, String, String>(
            name = name,
            initialValue1 = "",
            initialValue2 = "",
            initialValue3 = ""
        ) { generationContext, nodeDescriptors, ipTypeMetadata ->
            buildJsonObject {
                put("generationContext", generationContext)
                put("nodeDescriptors", nodeDescriptors)
                put("ipTypeMetadata", ipTypeMetadata)
            }.toString()
        }
    }
}
