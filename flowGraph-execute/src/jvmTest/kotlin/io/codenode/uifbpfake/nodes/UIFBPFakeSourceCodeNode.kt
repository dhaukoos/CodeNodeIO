/*
 * UIFBPFakeSourceCodeNode + UIFBPFakeSinkCodeNode — minimal CodeNode fixtures
 * License: Apache 2.0
 */

package io.codenode.uifbpfake.nodes

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec

/**
 * Minimal Source-flavor CodeNode for the UI-FBP fixture. Like [TestFakeNoOpCodeNode],
 * `createRuntime()` is never invoked because the regression tests stop at session
 * creation — they verify the reflection-proxy contract, not pipeline execution.
 */
object UIFBPFakeSourceCodeNode : CodeNodeDefinition {
    override val name: String = "UIFBPFakeSource"
    override val category: CodeNodeType = CodeNodeType.SOURCE
    override val inputPorts: List<PortSpec> = emptyList()
    override val outputPorts: List<PortSpec> = emptyList()

    override fun createRuntime(name: String): NodeRuntime =
        throw UnsupportedOperationException(
            "UIFBPFakeSourceCodeNode.createRuntime() must not be called; " +
                "regression tests stop at session creation, not pipeline execution"
        )
}

object UIFBPFakeSinkCodeNode : CodeNodeDefinition {
    override val name: String = "UIFBPFakeSink"
    override val category: CodeNodeType = CodeNodeType.SINK
    override val inputPorts: List<PortSpec> = emptyList()
    override val outputPorts: List<PortSpec> = emptyList()

    override fun createRuntime(name: String): NodeRuntime =
        throw UnsupportedOperationException(
            "UIFBPFakeSinkCodeNode.createRuntime() must not be called; " +
                "regression tests stop at session creation, not pipeline execution"
        )
}
