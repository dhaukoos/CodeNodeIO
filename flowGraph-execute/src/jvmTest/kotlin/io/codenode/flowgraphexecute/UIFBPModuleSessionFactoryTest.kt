/*
 * UIFBPModuleSessionFactoryTest
 * Validates that the GraphEditor's Runtime Preview path (ModuleSessionFactory →
 * DynamicPipelineController + reflection proxy) accepts UI-FBP-shaped fixtures.
 *
 * UI-FBP differs from entity modules in two ways feature 084 surfaces:
 *   - Source + Sink CodeNode roles (instead of TRANSFORMER)
 *   - Multiple typed sink-input flows on the ControllerInterface (UI-FBP modules
 *     observe N display outputs)
 *
 * The contract every UI-FBP-generated module satisfies after this feature is
 * structurally identical to what feature 085 established — extending
 * ModuleController, exposing typed sink-input flows, providing a Runtime factory
 * that constructs DynamicPipelineController. This test pins that
 * ModuleSessionFactory.createSession works against UI-FBP-shape fixtures.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphexecute

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.FlowExecutionStatus
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import io.codenode.uifbpfake.nodes.UIFBPFakeSinkCodeNode
import io.codenode.uifbpfake.nodes.UIFBPFakeSourceCodeNode
import io.codenode.uifbpfake.viewmodel.UIFBPFakeState
import io.codenode.uifbpfake.viewmodel.UIFBPFakeViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UIFBPModuleSessionFactoryTest {

    private fun buildUIFBPFakeFlowGraph(): FlowGraph =
        flowGraph(name = "UIFBPFake", version = "1.0.0") {
            codeNode("UIFBPFakeSource", nodeType = "SOURCE") {
                position(100.0, 200.0)
            }
            codeNode("UIFBPFakeSink", nodeType = "SINK") {
                position(400.0, 200.0)
            }
        }

    @BeforeTest
    fun setUp() {
        ModuleSessionFactory.registry = NodeDefinitionRegistry().apply {
            register(UIFBPFakeSourceCodeNode)
            register(UIFBPFakeSinkCodeNode)
        }
        UIFBPFakeState.reset()
    }

    @AfterTest
    fun tearDown() {
        ModuleSessionFactory.registry = null
    }

    // ========== T023: createSession returns a session whose ViewModel is castable ==========

    @Test
    fun `createSession returns a non-null RuntimeSession for a UI-FBP-shaped module`() {
        val flowGraph = buildUIFBPFakeFlowGraph()

        val session = ModuleSessionFactory.createSession(
            moduleName = "UIFBPFake",
            editorFlowGraph = flowGraph,
            flowGraphProvider = { flowGraph }
        )

        assertNotNull(
            session,
            "ModuleSessionFactory.createSession must return a non-null session when (a) the " +
                "registry resolves every node name in the flow graph and (b) the typed " +
                "ControllerInterface + ViewModel + State live at the canonical FQCN locations"
        )
    }

    @Test
    fun `session viewModel is castable to the typed UI-FBP ViewModel`() {
        val flowGraph = buildUIFBPFakeFlowGraph()
        val session = ModuleSessionFactory.createSession(
            moduleName = "UIFBPFake",
            editorFlowGraph = flowGraph,
            flowGraphProvider = { flowGraph }
        )
        assertNotNull(session)

        // If the proxy / reflection wiring fails, ModuleSessionFactory falls back to
        // viewModel = Any() and this cast throws ClassCastException.
        val typed = session.viewModel as? UIFBPFakeViewModel
        assertNotNull(
            typed,
            "session.viewModel MUST be a real UIFBPFakeViewModel instance; the Any() fallback " +
                "indicates the ControllerInterface or ViewModel constructor wasn't resolved"
        )
    }

    // ========== T023: getStatus through the proxy returns FlowExecutionStatus ==========

    @Test
    fun `getStatus through the reflection proxy returns non-null FlowExecutionStatus`() {
        val flowGraph = buildUIFBPFakeFlowGraph()
        val session = ModuleSessionFactory.createSession(
            moduleName = "UIFBPFake",
            editorFlowGraph = flowGraph,
            flowGraphProvider = { flowGraph }
        )
        assertNotNull(session)
        val viewModel = session.viewModel as UIFBPFakeViewModel

        // viewModel.statusViaProxy() routes through controller.getStatus(), which is the
        // post-feature-085 reflection-proxy case. UI-FBP-shaped interfaces inherit the
        // method from ModuleController, so the proxy's "getStatus" handler MUST route correctly.
        val status: FlowExecutionStatus = viewModel.statusViaProxy()
        assertNotNull(status,
            "getStatus() routed through the reflection proxy must return a non-null status")
        assertEquals(
            ExecutionState.IDLE,
            status.overallState,
            "before start(), the controller's status overallState must be IDLE"
        )
    }
}
