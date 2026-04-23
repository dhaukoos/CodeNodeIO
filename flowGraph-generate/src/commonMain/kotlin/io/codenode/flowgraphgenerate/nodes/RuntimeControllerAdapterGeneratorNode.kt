/*
 * RuntimeControllerAdapterGeneratorNode - CodeNode wrapper for RuntimeControllerAdapterGenerator
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphgenerate.generator.RuntimeControllerAdapterGenerator

object RuntimeControllerAdapterGeneratorNode : CodeNodeDefinition {
    override val name = "RuntimeControllerAdapterGenerator"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Generates runtime ControllerAdapter.kt from FlowGraph"
    override val inputPorts = listOf(PortSpec("config", Any::class))
    override val outputPorts = listOf(PortSpec("content", String::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Any, String>(
            name = name,
            transform = { input ->
                val config = input as GenerationConfig
                RuntimeControllerAdapterGenerator().generate(config.flowGraph, config.controllerPackage)
            }
        )
    }
}
