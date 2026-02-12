/*
 * FlowGraphFactoryGenerator Test
 * TDD tests for generating factory functions that instantiate FlowGraph with ProcessingLogic
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD tests for FlowGraphFactoryGenerator - generates factory functions.
 *
 * These tests are written FIRST and should FAIL until FlowGraphFactoryGenerator is implemented.
 *
 * T038: Test for factory function instantiating ProcessingLogic from module
 * T039: Test for factory function using class references from .flow.kt
 * T040: Test for compile validation: all ProcessingLogic classes exist
 */
class FlowGraphFactoryGeneratorTest {

    // ========== Test Fixtures ==========

    private fun createTestCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
        processingLogicClass: String? = null,
        inputPorts: List<Port<*>> = listOf(
            Port(
                id = "${id}_input",
                name = "input",
                direction = Port.Direction.INPUT,
                dataType = String::class,
                owningNodeId = id
            )
        ),
        outputPorts: List<Port<*>> = listOf(
            Port(
                id = "${id}_output",
                name = "output",
                direction = Port.Direction.OUTPUT,
                dataType = String::class,
                owningNodeId = id
            )
        )
    ): CodeNode {
        val config = mutableMapOf<String, String>()
        if (processingLogicClass != null) {
            config["_useCaseClass"] = processingLogicClass
        }
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(100.0, 200.0),
            inputPorts = inputPorts,
            outputPorts = outputPorts,
            configuration = config
        )
    }

    private fun createTestFlowGraph(
        name: String = "TestFlow",
        nodes: List<Node> = listOf(
            createTestCodeNode("timer", "TimerEmitter", CodeNodeType.GENERATOR, "TimerEmitterComponent"),
            createTestCodeNode("display", "DisplayReceiver", CodeNodeType.SINK, "DisplayReceiverComponent")
        ),
        connections: List<Connection> = listOf(
            Connection(
                id = "conn_1",
                sourceNodeId = "timer",
                sourcePortId = "timer_output",
                targetNodeId = "display",
                targetPortId = "display_input"
            )
        )
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            description = "Test flow for factory generation",
            rootNodes = nodes,
            connections = connections
        )
    }

    // ========== T038: Factory Function Instantiates ProcessingLogic ==========

    @Test
    fun `T038 - generateFactory produces non-empty output`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        // Then
        assertTrue(result.isNotBlank(), "Generated factory should not be blank")
    }

    @Test
    fun `T038 - generateFactory creates factory function`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        // Then
        assertTrue(result.contains("fun createStopWatchFlowGraph"),
            "Should create factory function with graph name")
    }

    @Test
    fun `T038 - factory function instantiates ProcessingLogic components`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        // Then
        assertTrue(result.contains("TimerEmitterComponent()"),
            "Should instantiate TimerEmitterComponent")
        assertTrue(result.contains("DisplayReceiverComponent()"),
            "Should instantiate DisplayReceiverComponent")
    }

    @Test
    fun `T038 - factory function returns FlowGraph`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        // Then
        assertTrue(result.contains(": FlowGraph"),
            "Factory function should return FlowGraph")
    }

    @Test
    fun `T038 - factory function includes package declaration`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val packageName = "io.codenode.generated.stopwatch"
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, packageName)

        // Then
        assertTrue(result.contains("package $packageName"),
            "Should include correct package declaration")
    }

    // ========== T039: Factory Uses Class References from .flow.kt ==========

    @Test
    fun `T039 - factory uses processingLogic class from node configuration`() {
        // Given
        val node = createTestCodeNode(
            id = "proc",
            name = "DataProcessor",
            processingLogicClass = "io.codenode.generated.myflow.DataProcessorComponent"
        )
        val flowGraph = FlowGraph(
            id = "flow_1",
            name = "MyFlow",
            version = "1.0.0",
            rootNodes = listOf(node)
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.myflow")

        // Then
        assertTrue(result.contains("DataProcessorComponent"),
            "Should use class name from processingLogic configuration")
    }

    @Test
    fun `T039 - factory handles fully qualified class names`() {
        // Given
        val node = createTestCodeNode(
            id = "proc",
            name = "DataProcessor",
            processingLogicClass = "com.example.custom.CustomProcessor"
        )
        val flowGraph = FlowGraph(
            id = "flow_1",
            name = "MyFlow",
            version = "1.0.0",
            rootNodes = listOf(node)
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.myflow")

        // Then
        // Should import or use the fully qualified name
        assertTrue(
            result.contains("import com.example.custom.CustomProcessor") ||
                result.contains("com.example.custom.CustomProcessor()"),
            "Should handle fully qualified class name"
        )
    }

    @Test
    fun `T039 - factory generates default component name when no processingLogic configured`() {
        // Given
        val node = createTestCodeNode(
            id = "proc",
            name = "DataProcessor",
            processingLogicClass = null // No explicit class
        )
        val flowGraph = FlowGraph(
            id = "flow_1",
            name = "MyFlow",
            version = "1.0.0",
            rootNodes = listOf(node)
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.myflow")

        // Then
        // Should default to NodeNameComponent
        assertTrue(result.contains("DataProcessorComponent"),
            "Should use default Component naming convention")
    }

    @Test
    fun `T039 - factory handles multiple nodes with processingLogic`() {
        // Given
        val nodes = listOf(
            createTestCodeNode("gen", "Generator", CodeNodeType.GENERATOR, "GeneratorComponent"),
            createTestCodeNode("proc", "Processor", CodeNodeType.TRANSFORMER, "ProcessorComponent"),
            createTestCodeNode("sink", "Receiver", CodeNodeType.SINK, "ReceiverComponent")
        )
        val flowGraph = FlowGraph(
            id = "flow_1",
            name = "Pipeline",
            version = "1.0.0",
            rootNodes = nodes
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.pipeline")

        // Then
        assertTrue(result.contains("GeneratorComponent()"),
            "Should instantiate GeneratorComponent")
        assertTrue(result.contains("ProcessorComponent()"),
            "Should instantiate ProcessorComponent")
        assertTrue(result.contains("ReceiverComponent()"),
            "Should instantiate ReceiverComponent")
    }

    // ========== T040: Compile Validation - ProcessingLogic Classes Exist ==========

    @Test
    fun `T040 - getRequiredComponents returns list of required ProcessingLogic classes`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        // When
        val required = generator.getRequiredComponents(flowGraph)

        // Then
        assertTrue(required.contains("TimerEmitterComponent"),
            "Should list TimerEmitterComponent as required")
        assertTrue(required.contains("DisplayReceiverComponent"),
            "Should list DisplayReceiverComponent as required")
    }

    @Test
    fun `T040 - getRequiredComponents handles node without explicit processingLogic`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor", processingLogicClass = null)
        val flowGraph = FlowGraph(
            id = "flow_1",
            name = "MyFlow",
            version = "1.0.0",
            rootNodes = listOf(node)
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val required = generator.getRequiredComponents(flowGraph)

        // Then
        assertTrue(required.contains("DataProcessorComponent"),
            "Should include default component name")
    }

    @Test
    fun `T040 - getRequiredComponents returns unique class names`() {
        // Given - two nodes with same component (edge case)
        val nodes = listOf(
            createTestCodeNode("proc1", "Processor1", processingLogicClass = "SharedComponent"),
            createTestCodeNode("proc2", "Processor2", processingLogicClass = "SharedComponent")
        )
        val flowGraph = FlowGraph(
            id = "flow_1",
            name = "MyFlow",
            version = "1.0.0",
            rootNodes = nodes
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val required = generator.getRequiredComponents(flowGraph)

        // Then
        assertEquals(1, required.count { it == "SharedComponent" },
            "Should not duplicate class names")
    }

    @Test
    fun `T040 - validateComponents returns success when all components exist`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()
        val existingFiles = setOf(
            "TimerEmitterComponent.kt",
            "DisplayReceiverComponent.kt"
        )

        // When
        val result = generator.validateComponents(flowGraph, existingFiles)

        // Then
        assertTrue(result.isValid, "Validation should pass when all components exist")
        assertTrue(result.missingComponents.isEmpty(), "No missing components expected")
    }

    @Test
    fun `T040 - validateComponents returns failure with missing components`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()
        val existingFiles = setOf(
            "TimerEmitterComponent.kt"
            // DisplayReceiverComponent.kt is missing
        )

        // When
        val result = generator.validateComponents(flowGraph, existingFiles)

        // Then
        assertFalse(result.isValid, "Validation should fail when components are missing")
        assertTrue(result.missingComponents.contains("DisplayReceiverComponent"),
            "Should report DisplayReceiverComponent as missing")
    }

    // ========== Additional Tests: Factory File Structure ==========

    @Test
    fun `factory includes required imports`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        // Then
        assertTrue(result.contains("import io.codenode.fbpdsl.model.FlowGraph"),
            "Should import FlowGraph")
    }

    @Test
    fun `factory function has KDoc documentation`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.stopwatch")

        // Then
        assertTrue(result.contains("/**") && result.contains("*/"),
            "Should include KDoc documentation")
        assertTrue(result.contains("@return"),
            "Should include @return in KDoc")
    }

    @Test
    fun `factory file name follows convention`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()

        // When
        val fileName = generator.getFactoryFileName(flowGraph)

        // Then
        assertEquals("StopWatchFactory.kt", fileName,
            "Factory file should be named {GraphName}Factory.kt")
    }

    @Test
    fun `factory handles GraphNode with nested CodeNodes`() {
        // Given
        val innerNode = createTestCodeNode(
            id = "inner",
            name = "InnerProcessor",
            processingLogicClass = "InnerProcessorComponent"
        )
        val graphNode = GraphNode(
            id = "group",
            name = "ProcessingGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(innerNode)
        )
        val flowGraph = FlowGraph(
            id = "flow_1",
            name = "NestedFlow",
            version = "1.0.0",
            rootNodes = listOf(graphNode)
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val result = generator.generateFactory(flowGraph, "io.codenode.generated.nested")

        // Then
        assertTrue(result.contains("InnerProcessorComponent"),
            "Should include components from nested GraphNodes")
    }
}
