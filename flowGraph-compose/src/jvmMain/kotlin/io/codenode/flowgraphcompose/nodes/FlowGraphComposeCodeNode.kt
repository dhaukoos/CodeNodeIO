/*
 * FlowGraphComposeCodeNode - Produces graphState from flowGraphModel, nodeDescriptors, and ipTypeMetadata
 * Module boundary CodeNode for the flowGraph-compose extraction
 * License: Apache 2.0
 */

package io.codenode.flowgraphcompose.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.fbpdsl.runtime.resolveSourceFilePath
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object FlowGraphComposeCodeNode : CodeNodeDefinition {
    override val name = "FlowGraphCompose"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Composes graphState from flowGraphModel, nodeDescriptors, and ipTypeMetadata"
    override val sourceFilePath: String? get() = resolveSourceFilePath(this::class.java)
    override val inputPorts = listOf(
        PortSpec("flowGraphModel", String::class),
        PortSpec("nodeDescriptors", String::class),
        PortSpec("ipTypeMetadata", String::class)
    )
    override val outputPorts = listOf(
        PortSpec("graphState", String::class)
    )
    override val anyInput = true

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createIn3AnyOut1Processor<String, String, String, String>(
            name = name,
            initialValue1 = "",
            initialValue2 = "",
            initialValue3 = ""
        ) { flowGraphModel, nodeDescriptors, ipTypeMetadata ->
            buildJsonObject {
                put("flowGraphModel", flowGraphModel)
                put("nodeDescriptors", nodeDescriptors)
                put("ipTypeMetadata", ipTypeMetadata)
            }.toString()
        }
    }
}
