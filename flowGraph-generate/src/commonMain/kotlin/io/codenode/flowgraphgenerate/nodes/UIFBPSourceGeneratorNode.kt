/*
 * UIFBPSourceGeneratorNode - CodeNode wrapper for UIFBPSourceCodeNodeGenerator
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphgenerate.generator.UIFBPSourceCodeNodeGenerator
import io.codenode.flowgraphgenerate.parser.UIFBPSpec

object UIFBPSourceGeneratorNode : CodeNodeDefinition {
    override val name = "UIFBPSourceGenerator"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Generates Source CodeNode from UIFBPSpec"
    override val inputPorts = listOf(PortSpec("uiFBPSpec", Any::class))
    override val outputPorts = listOf(PortSpec("content", String::class))

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Any, String>(
            name = name,
            transform = { input ->
                val spec = input as UIFBPSpec
                UIFBPSourceCodeNodeGenerator().generate(spec) ?: ""
            }
        )
    }
}
