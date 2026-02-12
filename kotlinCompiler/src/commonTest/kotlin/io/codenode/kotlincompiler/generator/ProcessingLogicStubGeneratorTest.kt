/*
 * ProcessingLogicStubGenerator Test
 * TDD tests for generating ProcessingLogic stub files for CodeNodes
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD tests for ProcessingLogicStubGenerator - generates ProcessingLogic stub files.
 *
 * These tests are written FIRST and should FAIL until ProcessingLogicStubGenerator is implemented.
 *
 * T027: Test for generating stub class name from node name
 * T028: Test for stub implementing ProcessingLogic interface
 * T029: Test for stub invoke() method with correct signature
 * T030: Test for stub KDoc describing node type
 * T031: Test for stub listing input/output ports in KDoc
 */
class ProcessingLogicStubGeneratorTest {

    // ========== Test Fixtures ==========

    private fun createTestCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
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
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(100.0, 200.0),
            inputPorts = inputPorts,
            outputPorts = outputPorts
        )
    }

    // ========== T027: Stub Class Name Generation ==========

    @Test
    fun `T027 - generateStub produces non-empty output`() {
        // Given
        val node = createTestCodeNode("timer", "TimerEmitter", CodeNodeType.GENERATOR)
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated.stopwatch")

        // Then
        assertTrue(result.isNotBlank(), "Generated stub should not be blank")
    }

    @Test
    fun `T027 - generateStub creates class with Component suffix`() {
        // Given
        val node = createTestCodeNode("timer", "TimerEmitter", CodeNodeType.GENERATOR)
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated.stopwatch")

        // Then
        assertTrue(result.contains("class TimerEmitterComponent"),
            "Should create class with Component suffix")
    }

    @Test
    fun `T027 - generateStub handles node name with spaces`() {
        // Given
        val node = createTestCodeNode("node1", "My Timer Emitter", CodeNodeType.GENERATOR)
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("class MyTimerEmitterComponent"),
            "Should convert spaces to PascalCase")
    }

    @Test
    fun `T027 - generateStub includes correct package declaration`() {
        // Given
        val node = createTestCodeNode("node1", "Processor")
        val packageName = "io.codenode.generated.mymodule"
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, packageName)

        // Then
        assertTrue(result.contains("package $packageName"),
            "Should include correct package declaration")
    }

    @Test
    fun `T027 - getStubFileName returns correct filename`() {
        // Given
        val node = createTestCodeNode("timer", "TimerEmitter")
        val generator = ProcessingLogicStubGenerator()

        // When
        val filename = generator.getStubFileName(node)

        // Then
        assertEquals("TimerEmitterComponent.kt", filename,
            "Filename should be NodeNameComponent.kt")
    }

    // ========== T028: ProcessingLogic Interface Implementation ==========

    @Test
    fun `T028 - generateStub implements ProcessingLogic interface`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor")
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains(": ProcessingLogic"),
            "Class should implement ProcessingLogic interface")
    }

    @Test
    fun `T028 - generateStub includes ProcessingLogic import`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor")
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("import io.codenode.fbpdsl.model.ProcessingLogic"),
            "Should import ProcessingLogic interface")
    }

    @Test
    fun `T028 - generateStub includes InformationPacket import`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor")
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("import io.codenode.fbpdsl.model.InformationPacket"),
            "Should import InformationPacket for method signature")
    }

    // ========== T029: invoke() Method Stub ==========

    @Test
    fun `T029 - generateStub includes suspend invoke method`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor")
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("override suspend operator fun invoke"),
            "Should include suspend operator fun invoke")
    }

    @Test
    fun `T029 - invoke method has correct parameter type`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor")
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("inputs: Map<String, InformationPacket<*>>"),
            "Should have correct input parameter type")
    }

    @Test
    fun `T029 - invoke method has correct return type`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor")
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("): Map<String, InformationPacket<*>>"),
            "Should have correct return type")
    }

    @Test
    fun `T029 - invoke method throws NotImplementedError`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor")
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("TODO") || result.contains("NotImplementedError"),
            "Should indicate implementation needed")
    }

    // ========== T030: KDoc with Node Type ==========

    @Test
    fun `T030 - generateStub includes KDoc comment`() {
        // Given
        val node = createTestCodeNode("timer", "TimerEmitter", CodeNodeType.GENERATOR)
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("/**") && result.contains("*/"),
            "Should include KDoc comment block")
    }

    @Test
    fun `T030 - KDoc describes GENERATOR node type`() {
        // Given
        val node = createTestCodeNode("timer", "TimerEmitter", CodeNodeType.GENERATOR)
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("GENERATOR") || result.contains("Generator"),
            "KDoc should mention GENERATOR node type")
    }

    @Test
    fun `T030 - KDoc describes TRANSFORMER node type`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor", CodeNodeType.TRANSFORMER)
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("TRANSFORMER") || result.contains("Transformer"),
            "KDoc should mention TRANSFORMER node type")
    }

    @Test
    fun `T030 - KDoc describes SINK node type`() {
        // Given
        val node = createTestCodeNode("display", "DisplayReceiver", CodeNodeType.SINK)
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("SINK") || result.contains("Sink"),
            "KDoc should mention SINK node type")
    }

    // ========== T031: KDoc with Port Descriptions ==========

    @Test
    fun `T031 - KDoc lists input ports`() {
        // Given
        val node = createTestCodeNode(
            id = "proc",
            name = "DataProcessor",
            inputPorts = listOf(
                Port(
                    id = "proc_data",
                    name = "data",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "proc"
                ),
                Port(
                    id = "proc_config",
                    name = "config",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = "proc"
                )
            )
        )
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("data") && result.contains("String"),
            "KDoc should list input port 'data' with type String")
        assertTrue(result.contains("config") && result.contains("Int"),
            "KDoc should list input port 'config' with type Int")
    }

    @Test
    fun `T031 - KDoc lists output ports`() {
        // Given
        val node = createTestCodeNode(
            id = "proc",
            name = "DataProcessor",
            outputPorts = listOf(
                Port(
                    id = "proc_result",
                    name = "result",
                    direction = Port.Direction.OUTPUT,
                    dataType = Boolean::class,
                    owningNodeId = "proc"
                )
            )
        )
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("result") && result.contains("Boolean"),
            "KDoc should list output port 'result' with type Boolean")
    }

    @Test
    fun `T031 - KDoc indicates Input and Output sections`() {
        // Given
        val node = createTestCodeNode("proc", "DataProcessor")
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.contains("Input") || result.contains("input"),
            "KDoc should have Input section")
        assertTrue(result.contains("Output") || result.contains("output"),
            "KDoc should have Output section")
    }

    @Test
    fun `T031 - generator handles node with no input ports`() {
        // Given
        val node = createTestCodeNode(
            id = "gen",
            name = "DataGenerator",
            type = CodeNodeType.GENERATOR,
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "gen_value",
                    name = "value",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = "gen"
                )
            )
        )
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.isNotBlank(), "Should generate stub even with no inputs")
        assertTrue(result.contains("value"),
            "Should still list output port")
    }

    @Test
    fun `T031 - generator handles node with no output ports`() {
        // Given
        val node = createTestCodeNode(
            id = "sink",
            name = "DataSink",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                Port(
                    id = "sink_data",
                    name = "data",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "sink"
                )
            ),
            outputPorts = emptyList()
        )
        val generator = ProcessingLogicStubGenerator()

        // When
        val result = generator.generateStub(node, "io.codenode.generated")

        // Then
        assertTrue(result.isNotBlank(), "Should generate stub even with no outputs")
        assertTrue(result.contains("data"),
            "Should still list input port")
    }
}
