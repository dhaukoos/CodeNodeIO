/*
 * EntityDisplayGeneratorNode - CodeNode wrapper for EntityDisplayCodeNodeGenerator
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphgenerate.generator.EntityDisplayCodeNodeGenerator
import io.codenode.flowgraphgenerate.generator.EntityModuleSpec

object EntityDisplayGeneratorNode : CodeNodeDefinition {
    override val name = "EntityDisplayGenerator"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Generates Display CodeNode from EntityModuleSpec"
    override val inputPorts = listOf(PortSpec("entitySpec", Any::class))
    override val outputPorts = listOf(PortSpec("content", String::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Any, String>(
            name = name,
            transform = { input ->
                val spec = input as EntityModuleSpec
                EntityDisplayCodeNodeGenerator().generate(spec)
            }
        )
    }
}
