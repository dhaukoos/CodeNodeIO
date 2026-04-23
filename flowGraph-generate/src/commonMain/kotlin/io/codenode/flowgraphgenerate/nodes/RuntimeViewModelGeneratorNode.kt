/*
 * RuntimeViewModelGeneratorNode - CodeNode wrapper for RuntimeViewModelGenerator
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphgenerate.generator.RuntimeViewModelGenerator

object RuntimeViewModelGeneratorNode : CodeNodeDefinition {
    override val name = "RuntimeViewModelGenerator"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Generates ViewModel.kt from FlowGraph"
    override val inputPorts = listOf(PortSpec("config", Any::class))
    override val outputPorts = listOf(PortSpec("content", String::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Any, String>(
            name = name,
            transform = { input ->
                val config = input as GenerationConfig
                RuntimeViewModelGenerator().generate(config.flowGraph, config.viewModelPackage, config.controllerPackage)
            }
        )
    }
}
