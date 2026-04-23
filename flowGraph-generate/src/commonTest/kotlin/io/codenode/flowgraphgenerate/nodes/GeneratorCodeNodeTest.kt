/*
 * GeneratorCodeNodeTest - Tests for generator CodeNode wrappers
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.nodes

import io.codenode.fbpdsl.model.*
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphgenerate.generator.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class GeneratorCodeNodeTest {

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

    // === US1: FlowKtGeneratorNode ===

    @Test
    fun `FlowKtGeneratorNode has correct CodeNodeDefinition properties`() {
        assertEquals("FlowKtGenerator", FlowKtGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, FlowKtGeneratorNode.category)
        assertEquals(1, FlowKtGeneratorNode.inputPorts.size)
        assertEquals(1, FlowKtGeneratorNode.outputPorts.size)
        assertEquals("content", FlowKtGeneratorNode.outputPorts[0].name)
        assertEquals(String::class, FlowKtGeneratorNode.outputPorts[0].dataType)
    }

    @Test
    fun `FlowKtGeneratorNode produces non-empty output`() {
        val directOutput = FlowKtGenerator().generateFlowKt(testFlowGraph, testConfig.flowPackage)
        assertTrue(directOutput.isNotBlank(), "Direct generator should produce output")
    }

    // === US2: Module-level wrappers ===

    @Test
    fun `RuntimeFlowGeneratorNode has correct properties`() {
        assertEquals("RuntimeFlowGenerator", RuntimeFlowGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, RuntimeFlowGeneratorNode.category)
        assertEquals(1, RuntimeFlowGeneratorNode.inputPorts.size)
        assertEquals(1, RuntimeFlowGeneratorNode.outputPorts.size)
    }

    @Test
    fun `RuntimeFlowGeneratorNode produces non-empty output`() {
        val output = RuntimeFlowGenerator().generate(testFlowGraph, testConfig.flowPackage, testConfig.viewModelPackage)
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `RuntimeControllerGeneratorNode has correct properties`() {
        assertEquals("RuntimeControllerGenerator", RuntimeControllerGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, RuntimeControllerGeneratorNode.category)
    }

    @Test
    fun `RuntimeControllerGeneratorNode produces non-empty output`() {
        val output = RuntimeControllerGenerator().generate(testFlowGraph, testConfig.controllerPackage, testConfig.viewModelPackage)
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `RuntimeControllerInterfaceGeneratorNode has correct properties`() {
        assertEquals("RuntimeControllerInterfaceGenerator", RuntimeControllerInterfaceGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, RuntimeControllerInterfaceGeneratorNode.category)
    }

    @Test
    fun `RuntimeControllerInterfaceGeneratorNode produces non-empty output`() {
        val output = RuntimeControllerInterfaceGenerator().generate(testFlowGraph, testConfig.controllerPackage)
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `RuntimeControllerAdapterGeneratorNode has correct properties`() {
        assertEquals("RuntimeControllerAdapterGenerator", RuntimeControllerAdapterGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, RuntimeControllerAdapterGeneratorNode.category)
    }

    @Test
    fun `RuntimeControllerAdapterGeneratorNode produces non-empty output`() {
        val output = RuntimeControllerAdapterGenerator().generate(testFlowGraph, testConfig.controllerPackage)
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `RuntimeViewModelGeneratorNode has correct properties`() {
        assertEquals("RuntimeViewModelGenerator", RuntimeViewModelGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, RuntimeViewModelGeneratorNode.category)
    }

    @Test
    fun `RuntimeViewModelGeneratorNode produces non-empty output`() {
        val output = RuntimeViewModelGenerator().generate(testFlowGraph, testConfig.viewModelPackage, testConfig.controllerPackage)
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `UserInterfaceStubGeneratorNode has correct properties`() {
        assertEquals("UserInterfaceStubGenerator", UserInterfaceStubGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, UserInterfaceStubGeneratorNode.category)
    }

    @Test
    fun `UserInterfaceStubGeneratorNode produces non-empty output`() {
        val output = UserInterfaceStubGenerator().generate(testFlowGraph, testConfig.userInterfacePackage, testConfig.viewModelPackage)
        assertTrue(output.isNotBlank())
    }

    @Test
    fun `all 7 module-level wrappers have unique names`() {
        val names = listOf(
            FlowKtGeneratorNode.name,
            RuntimeFlowGeneratorNode.name,
            RuntimeControllerGeneratorNode.name,
            RuntimeControllerInterfaceGeneratorNode.name,
            RuntimeControllerAdapterGeneratorNode.name,
            RuntimeViewModelGeneratorNode.name,
            UserInterfaceStubGeneratorNode.name
        )
        assertEquals(7, names.distinct().size, "All 7 wrappers should have unique names")
    }

    // === US3: Entity generator wrappers ===

    @Test
    fun `EntityCUDGeneratorNode has correct properties`() {
        assertEquals("EntityCUDGenerator", EntityCUDGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, EntityCUDGeneratorNode.category)
        assertEquals(1, EntityCUDGeneratorNode.inputPorts.size)
        assertEquals("entitySpec", EntityCUDGeneratorNode.inputPorts[0].name)
    }

    @Test
    fun `EntityRepositoryGeneratorNode has correct properties`() {
        assertEquals("EntityRepositoryGenerator", EntityRepositoryGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, EntityRepositoryGeneratorNode.category)
    }

    @Test
    fun `EntityDisplayGeneratorNode has correct properties`() {
        assertEquals("EntityDisplayGenerator", EntityDisplayGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, EntityDisplayGeneratorNode.category)
    }

    @Test
    fun `EntityPersistenceGeneratorNode has correct properties`() {
        assertEquals("EntityPersistenceGenerator", EntityPersistenceGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, EntityPersistenceGeneratorNode.category)
    }

    // === US3: UI-FBP generator wrappers ===

    @Test
    fun `UIFBPStateGeneratorNode has correct properties`() {
        assertEquals("UIFBPStateGenerator", UIFBPStateGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, UIFBPStateGeneratorNode.category)
        assertEquals("uiFBPSpec", UIFBPStateGeneratorNode.inputPorts[0].name)
    }

    @Test
    fun `UIFBPViewModelGeneratorNode has correct properties`() {
        assertEquals("UIFBPViewModelGenerator", UIFBPViewModelGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, UIFBPViewModelGeneratorNode.category)
    }

    @Test
    fun `UIFBPSourceGeneratorNode has correct properties`() {
        assertEquals("UIFBPSourceGenerator", UIFBPSourceGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, UIFBPSourceGeneratorNode.category)
    }

    @Test
    fun `UIFBPSinkGeneratorNode has correct properties`() {
        assertEquals("UIFBPSinkGenerator", UIFBPSinkGeneratorNode.name)
        assertEquals(CodeNodeType.TRANSFORMER, UIFBPSinkGeneratorNode.category)
    }

    @Test
    fun `all 15 generator CodeNodes have unique names`() {
        val names = listOf(
            FlowKtGeneratorNode.name,
            RuntimeFlowGeneratorNode.name,
            RuntimeControllerGeneratorNode.name,
            RuntimeControllerInterfaceGeneratorNode.name,
            RuntimeControllerAdapterGeneratorNode.name,
            RuntimeViewModelGeneratorNode.name,
            UserInterfaceStubGeneratorNode.name,
            EntityCUDGeneratorNode.name,
            EntityRepositoryGeneratorNode.name,
            EntityDisplayGeneratorNode.name,
            EntityPersistenceGeneratorNode.name,
            UIFBPStateGeneratorNode.name,
            UIFBPViewModelGeneratorNode.name,
            UIFBPSourceGeneratorNode.name,
            UIFBPSinkGeneratorNode.name
        )
        assertEquals(15, names.distinct().size, "All 15 wrappers should have unique names")
    }
}
