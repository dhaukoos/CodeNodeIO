/*
 * FlowKtGeneratorNode - CodeNode wrapper for FlowKtGenerator
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphgenerate.generator.FlowKtGenerator

object FlowKtGeneratorNode : CodeNodeDefinition {
    override val name = "FlowKtGenerator"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Generates .flow.kt file from FlowGraph"
    override val inputPorts = listOf(PortSpec("config", Any::class))
    override val outputPorts = listOf(PortSpec("content", String::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Any, String>(
            name = name,
            transform = { input ->
                val config = input as GenerationConfig
                FlowKtGenerator().generateFlowKt(config.flowGraph, config.flowPackage)
            }
        )
    }
}
