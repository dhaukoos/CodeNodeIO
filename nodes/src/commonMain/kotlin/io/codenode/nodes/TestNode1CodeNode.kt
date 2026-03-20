package io.codenode.nodes

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.*
import kotlin.reflect.KClass

object TestNode1CodeNode : CodeNodeDefinition {
    override val name = "TestNode1"
    override val category = CodeNodeType.TRANSFORMER
    override val description = "Transformer node with 1 input(s) and 1 output(s)"
    override val inputPorts = listOf(PortSpec("input1", Any::class))
    override val outputPorts = listOf(PortSpec("output1", Any::class))

    // TODO: Replace with your processing logic
    private val processBlock: ContinuousTransformBlock<Any, Any> = { input -> input }

    override fun createRuntime(name: String): NodeRuntime {
        return CodeNodeFactory.createContinuousTransformer<Any, Any>(
            name = name,
            transform = processBlock
        )
    }
}

