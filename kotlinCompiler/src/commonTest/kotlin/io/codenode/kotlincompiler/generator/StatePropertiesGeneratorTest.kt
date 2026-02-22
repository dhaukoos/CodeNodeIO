/*
 * StatePropertiesGenerator Test
 * Tests for generating {NodeName}StateProperties.kt from CodeNode
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for StatePropertiesGenerator - generates per-node state property
 * objects with MutableStateFlow/StateFlow pairs for each port.
 */
class StatePropertiesGeneratorTest {

    // ========== Test Fixtures ==========

    private fun createTestCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
        inputPorts: List<Port<*>> = emptyList(),
        outputPorts: List<Port<*>> = emptyList()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(0.0, 0.0),
            inputPorts = inputPorts,
            outputPorts = outputPorts
        )
    }

    private fun inputPort(id: String, name: String, dataType: kotlin.reflect.KClass<*>, owningNodeId: String): Port<*> {
        return Port(
            id = id,
            name = name,
            direction = Port.Direction.INPUT,
            dataType = dataType,
            owningNodeId = owningNodeId
        )
    }

    private fun outputPort(id: String, name: String, dataType: kotlin.reflect.KClass<*>, owningNodeId: String): Port<*> {
        return Port(
            id = id,
            name = name,
            direction = Port.Direction.OUTPUT,
            dataType = dataType,
            owningNodeId = owningNodeId
        )
    }

    private val generator = StatePropertiesGenerator()
    private val testPackage = "io.codenode.testapp.stateProperties"

    // ========== shouldGenerate() tests ==========

    @Test
    fun `shouldGenerate returns true for node with output ports`() {
        val node = createTestCodeNode(
            "gen", "Generator", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Int::class, "gen"))
        )
        assertTrue(generator.shouldGenerate(node))
    }

    @Test
    fun `shouldGenerate returns true for node with input ports`() {
        val node = createTestCodeNode(
            "sink", "Receiver", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "value", Int::class, "sink"))
        )
        assertTrue(generator.shouldGenerate(node))
    }

    @Test
    fun `shouldGenerate returns true for node with both input and output ports`() {
        val node = createTestCodeNode(
            "trans", "Transformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", String::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", Int::class, "trans"))
        )
        assertTrue(generator.shouldGenerate(node))
    }

    @Test
    fun `shouldGenerate returns false for node with no ports`() {
        val node = createTestCodeNode("empty", "EmptyNode")
        assertFalse(generator.shouldGenerate(node))
    }

    // ========== getStatePropertiesFileName() tests ==========

    @Test
    fun `getStatePropertiesFileName returns correct filename`() {
        val node = createTestCodeNode("gen", "TimerEmitter")
        assertEquals("TimerEmitterStateProperties.kt", generator.getStatePropertiesFileName(node))
    }

    @Test
    fun `getStatePropertiesFileName handles single word name`() {
        val node = createTestCodeNode("gen", "Generator")
        assertEquals("GeneratorStateProperties.kt", generator.getStatePropertiesFileName(node))
    }

    // ========== getStatePropertiesObjectName() tests ==========

    @Test
    fun `getStatePropertiesObjectName returns correct object name`() {
        val node = createTestCodeNode("gen", "TimerEmitter")
        assertEquals("TimerEmitterStateProperties", generator.getStatePropertiesObjectName(node))
    }

    @Test
    fun `getStatePropertiesObjectName handles single word name`() {
        val node = createTestCodeNode("sink", "Display")
        assertEquals("DisplayStateProperties", generator.getStatePropertiesObjectName(node))
    }

    // ========== generateStateProperties() for 2-output generator ==========

    @Test
    fun `generates package declaration`() {
        val node = createGeneratorNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("package $testPackage"))
    }

    @Test
    fun `generates coroutine flow imports`() {
        val node = createGeneratorNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("import kotlinx.coroutines.flow.MutableStateFlow"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.StateFlow"))
        assertTrue(result.contains("import kotlinx.coroutines.flow.asStateFlow"))
    }

    @Test
    fun `generates object declaration for generator node`() {
        val node = createGeneratorNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("object TimerEmitterStateProperties {"))
    }

    @Test
    fun `generates internal MutableStateFlow for output ports`() {
        val node = createGeneratorNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("internal val _elapsedSeconds = MutableStateFlow(0)"))
        assertTrue(result.contains("internal val _elapsedMinutes = MutableStateFlow(0)"))
    }

    @Test
    fun `generates public StateFlow accessors for output ports`() {
        val node = createGeneratorNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()"))
        assertTrue(result.contains("val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()"))
    }

    @Test
    fun `generates reset method for generator node`() {
        val node = createGeneratorNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("fun reset()"))
        assertTrue(result.contains("_elapsedSeconds.value = 0"))
        assertTrue(result.contains("_elapsedMinutes.value = 0"))
    }

    // ========== generateStateProperties() for 2-input sink ==========

    @Test
    fun `generates MutableStateFlow for sink input ports`() {
        val node = createSinkNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("object DisplayReceiverStateProperties {"))
        assertTrue(result.contains("internal val _seconds = MutableStateFlow(0)"))
        assertTrue(result.contains("val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()"))
        assertTrue(result.contains("internal val _minutes = MutableStateFlow(0)"))
        assertTrue(result.contains("val minutesFlow: StateFlow<Int> = _minutes.asStateFlow()"))
    }

    @Test
    fun `generates reset method for sink node`() {
        val node = createSinkNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("_seconds.value = 0"))
        assertTrue(result.contains("_minutes.value = 0"))
    }

    // ========== generateStateProperties() for transformer (input + output) ==========

    @Test
    fun `generates state properties for both input and output ports on transformer`() {
        val node = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", String::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", Int::class, "trans"))
        )
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("object DataTransformerStateProperties {"))
        // Input port
        assertTrue(result.contains("internal val _input = MutableStateFlow(\"\")"))
        assertTrue(result.contains("val inputFlow: StateFlow<String> = _input.asStateFlow()"))
        // Output port
        assertTrue(result.contains("internal val _output = MutableStateFlow(0)"))
        assertTrue(result.contains("val outputFlow: StateFlow<Int> = _output.asStateFlow()"))
    }

    @Test
    fun `generates reset for both input and output ports`() {
        val node = createTestCodeNode(
            "trans", "DataTransformer", CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", String::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "output", Int::class, "trans"))
        )
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("_input.value = \"\""))
        assertTrue(result.contains("_output.value = 0"))
    }

    // ========== Type-specific default values ==========

    @Test
    fun `generates correct defaults for String type`() {
        val node = createTestCodeNode(
            "sink", "Logger", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "message", String::class, "sink"))
        )
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("MutableStateFlow(\"\")"))
    }

    @Test
    fun `generates correct defaults for Boolean type`() {
        val node = createTestCodeNode(
            "sink", "Toggle", CodeNodeType.SINK,
            inputPorts = listOf(inputPort("s_in", "enabled", Boolean::class, "sink"))
        )
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("MutableStateFlow(false)"))
    }

    @Test
    fun `generates correct defaults for Long type`() {
        val node = createTestCodeNode(
            "gen", "Counter", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "count", Long::class, "gen"))
        )
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("MutableStateFlow(0L)"))
    }

    @Test
    fun `generates correct defaults for Double type`() {
        val node = createTestCodeNode(
            "gen", "Sensor", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Double::class, "gen"))
        )
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("MutableStateFlow(0.0)"))
    }

    @Test
    fun `generates correct defaults for Float type`() {
        val node = createTestCodeNode(
            "gen", "Sensor", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "value", Float::class, "gen"))
        )
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("MutableStateFlow(0.0f)"))
    }

    @Test
    fun `generates TODO default for non-primitive types`() {
        val node = createTestCodeNode(
            "gen", "DataSource", CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("g_out", "data", Any::class, "gen"))
        )
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("TODO(\"Provide initial value for Any\")"))
    }

    // ========== Edge cases ==========

    @Test
    fun `returns empty string for node with no ports`() {
        val node = createTestCodeNode("empty", "EmptyNode")
        val result = generator.generateStateProperties(node, testPackage)

        assertEquals("", result)
    }

    @Test
    fun `generates KDoc with port descriptions`() {
        val node = createGeneratorNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("State properties for the TimerEmitter node."))
        assertTrue(result.contains("Output ports:"))
        assertTrue(result.contains("- elapsedSeconds: Int"))
        assertTrue(result.contains("- elapsedMinutes: Int"))
    }

    @Test
    fun `generates KDoc with input port descriptions for sink`() {
        val node = createSinkNode()
        val result = generator.generateStateProperties(node, testPackage)

        assertTrue(result.contains("Input ports:"))
        assertTrue(result.contains("- seconds: Int"))
        assertTrue(result.contains("- minutes: Int"))
    }

    // ========== Helpers ==========

    private fun createGeneratorNode(): CodeNode {
        return createTestCodeNode(
            id = "timer",
            name = "TimerEmitter",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("timer_sec", "elapsedSeconds", Int::class, "timer"),
                outputPort("timer_min", "elapsedMinutes", Int::class, "timer")
            )
        )
    }

    private fun createSinkNode(): CodeNode {
        return createTestCodeNode(
            id = "display",
            name = "DisplayReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("display_sec", "seconds", Int::class, "display"),
                inputPort("display_min", "minutes", Int::class, "display")
            )
        )
    }
}
