/*
 * TestFakeNoOpCodeNode — fixture for ModuleSessionFactoryRegressionTest
 * License: Apache 2.0
 */

package io.codenode.testfake.nodes

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec

/**
 * Minimal CodeNodeDefinition that participates in DynamicPipelineBuilder
 * validation but never has its `createRuntime()` invoked — the regression
 * tests stop short of starting the pipeline. The node has no ports so the
 * one-node FlowGraph has no connections to validate.
 */
object TestFakeNoOpCodeNode : CodeNodeDefinition {
    override val name: String = "TestFakeNoOp"
    override val category: CodeNodeType = CodeNodeType.TRANSFORMER
    override val inputPorts: List<PortSpec> = emptyList()
    override val outputPorts: List<PortSpec> = emptyList()

    override fun createRuntime(name: String): NodeRuntime =
        throw UnsupportedOperationException(
            "TestFakeNoOpCodeNode.createRuntime() must not be called; " +
                "regression tests stop at session creation, not pipeline execution"
        )
}
