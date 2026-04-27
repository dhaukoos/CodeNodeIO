/*
 * CodeGenerationRunnerTest - Tests for FBP-based code generation execution
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.runner

import io.codenode.fbpdsl.model.*
import io.codenode.flowgraphgenerate.model.GenerationPath
import io.codenode.flowgraphgenerate.nodes.GenerationConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CodeGenerationRunnerTest {

    private val testFlowGraph = FlowGraph(
        id = "flow_test",
        name = "TestModule",
        version = "1.0.0",
        rootNodes = listOf(
            CodeNode(
                id = "src1", name = "Source", codeNodeType = CodeNodeType.SOURCE,
                position = Node.Position(100.0, 100.0),
                inputPorts = emptyList(),
                outputPorts = listOf(
                    Port(id = "src1_out", name = "value", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "src1")
                )
            ),
            CodeNode(
                id = "sink1", name = "Display", codeNodeType = CodeNodeType.SINK,
                position = Node.Position(400.0, 100.0),
                inputPorts = listOf(
                    Port(id = "sink1_in", name = "data", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "sink1")
                ),
                outputPorts = emptyList()
            )
        ),
        connections = listOf(
            Connection("c1", "src1", "src1_out", "sink1", "sink1_in")
        )
    )

    private val testConfig = GenerationConfig(
        flowGraph = testFlowGraph,
        basePackage = "io.codenode.testmodule",
        flowPackage = "io.codenode.testmodule.flow",
        controllerPackage = "io.codenode.testmodule.controller",
        viewModelPackage = "io.codenode.testmodule.viewmodel",
        userInterfacePackage = "io.codenode.testmodule.userInterface",
        moduleName = "TestModule"
    )

    // Feature 085: GENERATE_MODULE path now produces 5 entries (was 7).
    // Eliminated: RuntimeFlowGenerator, RuntimeControllerGenerator, RuntimeControllerAdapterGenerator.
    // Replaced by: ModuleRuntimeGenerator (single ~30-line file).
    private val expectedGenerateModuleIds = setOf(
        "FlowKtGenerator",
        "ModuleRuntimeGenerator",
        "RuntimeControllerInterfaceGenerator",
        "RuntimeViewModelGenerator",
        "UserInterfaceStubGenerator"
    )

    @Test
    fun `execute with GENERATE_MODULE produces 5 entries after universal-runtime collapse`() = runTest {
        val runner = CodeGenerationRunner()
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig)

        assertEquals(expectedGenerateModuleIds.size, result.totalGenerated,
            "GENERATE_MODULE path emits the post-collapse generator set")
        assertTrue(result.isSuccess, "Should have no errors")
        assertEquals(0, result.totalSkipped)
    }

    @Test
    fun `execute produces non-empty content for each generator`() = runTest {
        val runner = CodeGenerationRunner()
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig)

        for ((generatorId, content) in result.generatedFiles) {
            assertTrue(content.isNotBlank(), "$generatorId should produce non-empty content")
        }
    }

    @Test
    fun `execute includes all expected generator IDs`() = runTest {
        val runner = CodeGenerationRunner()
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig)

        assertEquals(expectedGenerateModuleIds, result.generatedFiles.keys)
    }

    @Test
    fun `execute with selection filter excludes specified generators`() = runTest {
        val runner = CodeGenerationRunner()
        val filter = SelectionFilter(excludedGeneratorIds = setOf("ModuleRuntimeGenerator"))
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig, filter)

        assertEquals(
            expectedGenerateModuleIds.size - 1,
            result.totalGenerated,
            "Should produce post-collapse set minus 1 excluded"
        )
        assertFalse(result.generatedFiles.containsKey("ModuleRuntimeGenerator"))
        assertTrue(result.skipped.contains("ModuleRuntimeGenerator"))
        assertEquals(1, result.totalSkipped)
    }

    @Test
    fun `execute with all excluded produces empty result`() = runTest {
        val runner = CodeGenerationRunner()
        val filter = SelectionFilter(excludedGeneratorIds = expectedGenerateModuleIds)
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig, filter)

        assertEquals(0, result.totalGenerated)
        assertEquals(expectedGenerateModuleIds.size, result.totalSkipped)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `FlowKtGenerator output contains package declaration`() = runTest {
        val runner = CodeGenerationRunner()
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig)

        val flowKtContent = result.generatedFiles["FlowKtGenerator"]!!
        assertTrue(flowKtContent.contains("package io.codenode.testmodule.flow"),
            "FlowKt output should contain flow package declaration")
    }

    @Test
    fun `ModuleRuntimeGenerator output contains base package and registry`() = runTest {
        val runner = CodeGenerationRunner()
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig)

        val runtimeContent = result.generatedFiles["ModuleRuntimeGenerator"]!!
        assertTrue(
            runtimeContent.contains("package io.codenode.testmodule"),
            "ModuleRuntime output must declare the base package, not a subpackage"
        )
        assertTrue(
            runtimeContent.contains("object TestModuleNodeRegistry"),
            "ModuleRuntime output must declare the per-module NodeRegistry"
        )
        assertTrue(
            runtimeContent.contains("fun createTestModuleRuntime"),
            "ModuleRuntime output must declare the factory function"
        )
    }
}
