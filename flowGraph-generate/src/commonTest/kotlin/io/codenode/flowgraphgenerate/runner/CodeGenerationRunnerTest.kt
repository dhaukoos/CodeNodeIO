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

    @Test
    fun `execute with GENERATE_MODULE produces 7 entries`() = runTest {
        val runner = CodeGenerationRunner()
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig)

        assertEquals(7, result.totalGenerated, "Should produce 7 generated files")
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

        val expectedIds = setOf(
            "FlowKtGenerator", "RuntimeFlowGenerator",
            "RuntimeControllerGenerator", "RuntimeControllerInterfaceGenerator",
            "RuntimeControllerAdapterGenerator", "RuntimeViewModelGenerator",
            "UserInterfaceStubGenerator"
        )
        assertEquals(expectedIds, result.generatedFiles.keys)
    }

    @Test
    fun `execute with selection filter excludes specified generators`() = runTest {
        val runner = CodeGenerationRunner()
        val filter = SelectionFilter(excludedGeneratorIds = setOf("RuntimeControllerGenerator"))
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig, filter)

        assertEquals(6, result.totalGenerated, "Should produce 6 files (7 minus 1 excluded)")
        assertFalse(result.generatedFiles.containsKey("RuntimeControllerGenerator"))
        assertTrue(result.skipped.contains("RuntimeControllerGenerator"))
        assertEquals(1, result.totalSkipped)
    }

    @Test
    fun `execute with all excluded produces empty result`() = runTest {
        val runner = CodeGenerationRunner()
        val allIds = setOf(
            "FlowKtGenerator", "RuntimeFlowGenerator",
            "RuntimeControllerGenerator", "RuntimeControllerInterfaceGenerator",
            "RuntimeControllerAdapterGenerator", "RuntimeViewModelGenerator",
            "UserInterfaceStubGenerator"
        )
        val filter = SelectionFilter(excludedGeneratorIds = allIds)
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig, filter)

        assertEquals(0, result.totalGenerated)
        assertEquals(7, result.totalSkipped)
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
    fun `RuntimeControllerGenerator output contains controller package`() = runTest {
        val runner = CodeGenerationRunner()
        val result = runner.execute(GenerationPath.GENERATE_MODULE, testConfig)

        val controllerContent = result.generatedFiles["RuntimeControllerGenerator"]!!
        assertTrue(controllerContent.contains("package io.codenode.testmodule.controller"),
            "Controller output should contain controller package declaration")
    }
}
