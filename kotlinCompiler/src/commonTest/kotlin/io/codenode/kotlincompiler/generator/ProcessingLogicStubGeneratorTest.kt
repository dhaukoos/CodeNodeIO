/*
 * ProcessingLogicStubGenerator Test
 * Tests for generating tick function stub files for CodeNodes
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for ProcessingLogicStubGenerator - generates tick function stub files.
 *
 * Tests cover all 16 node configurations (input/output combinations),
 * file naming, val naming, KDoc, imports, default return values, and edge cases.
 */
class ProcessingLogicStubGeneratorTest {

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
            position = Node.Position(100.0, 200.0),
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

    // ========== File Naming ==========

    @Test
    fun `getStubFileName returns NodeNameProcessLogic format`() {
        val node = createTestCodeNode("timer", "TimerEmitter")
        val generator = ProcessingLogicStubGenerator()

        val filename = generator.getStubFileName(node)

        assertEquals("TimerEmitterProcessLogic.kt", filename)
    }

    @Test
    fun `getStubFileName handles node name with spaces`() {
        val node = createTestCodeNode("node1", "My Timer Emitter")
        val generator = ProcessingLogicStubGenerator()

        val filename = generator.getStubFileName(node)

        assertEquals("MyTimerEmitterProcessLogic.kt", filename)
    }

    // ========== Val Naming ==========

    @Test
    fun `getTickValName returns camelCase with Tick suffix`() {
        val node = createTestCodeNode("timer", "TimerEmitter")
        val generator = ProcessingLogicStubGenerator()

        val valName = generator.getTickValName(node)

        assertEquals("timerEmitterTick", valName)
    }

    // ========== shouldGenerateStub ==========

    @Test
    fun `shouldGenerateStub returns false for 0 inputs and 0 outputs`() {
        val node = createTestCodeNode("empty", "EmptyNode")
        val generator = ProcessingLogicStubGenerator()

        assertFalse(generator.shouldGenerateStub(node))
    }

    @Test
    fun `shouldGenerateStub returns false when inputs exceed 3`() {
        val node = createTestCodeNode(
            "big", "BigNode",
            inputPorts = (1..4).map { inputPort("in$it", "input$it", Int::class, "big") }
        )
        val generator = ProcessingLogicStubGenerator()

        assertFalse(generator.shouldGenerateStub(node))
    }

    @Test
    fun `shouldGenerateStub returns true for valid non-source node`() {
        val node = createTestCodeNode(
            "trans", "Transformer",
            inputPorts = listOf(inputPort("in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("out", "value", Int::class, "trans"))
        )
        val generator = ProcessingLogicStubGenerator()

        assertTrue(generator.shouldGenerateStub(node))
    }

    // ========== Source Nodes (0 inputs) — no stubs ==========

    @Test
    fun `source node 0 in 1 out shouldGenerateStub returns false`() {
        val node = createTestCodeNode(
            "gen", "DataGenerator",
            type = CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("gen_value", "value", Int::class, "gen"))
        )
        val generator = ProcessingLogicStubGenerator()

        assertFalse(generator.shouldGenerateStub(node),
            "Source nodes (0 inputs) should not generate stubs")
        assertEquals("", generator.getTickTypeAlias(node),
            "Source nodes should return empty tick type alias")
    }

    @Test
    fun `source node 0 in 2 out shouldGenerateStub returns false`() {
        val node = createTestCodeNode(
            "timer", "TimerEmitter",
            type = CodeNodeType.SOURCE,
            outputPorts = listOf(
                outputPort("timer_sec", "elapsedSeconds", Int::class, "timer"),
                outputPort("timer_min", "elapsedMinutes", Int::class, "timer")
            )
        )
        val generator = ProcessingLogicStubGenerator()

        assertFalse(generator.shouldGenerateStub(node),
            "Source nodes (0 inputs, 2 outputs) should not generate stubs")
        assertEquals("", generator.getTickTypeAlias(node),
            "Source nodes should return empty tick type alias")
    }

    @Test
    fun `source node 0 in 3 out shouldGenerateStub returns false`() {
        val node = createTestCodeNode(
            "multi", "MultiOutput",
            type = CodeNodeType.SOURCE,
            outputPorts = listOf(
                outputPort("o1", "first", Int::class, "multi"),
                outputPort("o2", "second", String::class, "multi"),
                outputPort("o3", "third", Boolean::class, "multi")
            )
        )
        val generator = ProcessingLogicStubGenerator()

        assertFalse(generator.shouldGenerateStub(node),
            "Source nodes (0 inputs, 3 outputs) should not generate stubs")
        assertEquals("", generator.getTickTypeAlias(node),
            "Source nodes should return empty tick type alias")
    }

    // ========== Sink Nodes (0 outputs) ==========

    @Test
    fun `sink node 1 in 0 out shouldGenerateStub returns false`() {
        val node = createTestCodeNode(
            "sink", "DataSink",
            type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("sink_data", "data", String::class, "sink"))
        )
        val generator = ProcessingLogicStubGenerator()

        assertFalse(generator.shouldGenerateStub(node),
            "Sink nodes (0 outputs) should not generate stubs")
        assertEquals("", generator.getTickTypeAlias(node),
            "Sink nodes should return empty tick type alias")
        assertEquals("", generator.generateStub(node, "io.codenode.generated"),
            "Sink nodes should return empty stub")
    }

    @Test
    fun `sink node 2 in 0 out shouldGenerateStub returns false`() {
        val node = createTestCodeNode(
            "display", "DisplayReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("d_sec", "seconds", Int::class, "display"),
                inputPort("d_min", "minutes", Int::class, "display")
            )
        )
        val generator = ProcessingLogicStubGenerator()

        assertFalse(generator.shouldGenerateStub(node),
            "Sink nodes (0 outputs) should not generate stubs")
        assertEquals("", generator.getTickTypeAlias(node),
            "Sink nodes should return empty tick type alias")
        assertEquals("", generator.generateStub(node, "io.codenode.generated"),
            "Sink nodes should return empty stub")
    }

    @Test
    fun `sink node 3 in 0 out shouldGenerateStub returns false`() {
        val node = createTestCodeNode(
            "tri", "TriSink",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("i1", "alpha", Int::class, "tri"),
                inputPort("i2", "beta", String::class, "tri"),
                inputPort("i3", "gamma", Boolean::class, "tri")
            )
        )
        val generator = ProcessingLogicStubGenerator()

        assertFalse(generator.shouldGenerateStub(node),
            "Sink nodes (0 outputs) should not generate stubs")
        assertEquals("", generator.getTickTypeAlias(node),
            "Sink nodes should return empty tick type alias")
        assertEquals("", generator.generateStub(node, "io.codenode.generated"),
            "Sink nodes should return empty stub")
    }

    // ========== Transformer and Filter (1 in, 1 out) ==========

    @Test
    fun `transformer node different types produces TransformerTickBlock`() {
        val node = createTestCodeNode(
            "trans", "DataTransformer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "rawData", String::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "processedData", Int::class, "trans"))
        )
        val generator = ProcessingLogicStubGenerator()

        val typeAlias = generator.getTickTypeAlias(node)
        assertEquals("TransformerTickBlock<String, Int>", typeAlias)

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("Transformer (1 inputs, 1 outputs)"))
        assertTrue(result.contains("rawData ->"))
        assertTrue(result.contains("0"))  // default Int return
    }

    @Test
    fun `filter node same types produces FilterTickBlock`() {
        val node = createTestCodeNode(
            "filt", "EvenFilter",
            type = CodeNodeType.FILTER,
            inputPorts = listOf(inputPort("f_in", "value", Int::class, "filt")),
            outputPorts = listOf(outputPort("f_out", "value", Int::class, "filt"))
        )
        val generator = ProcessingLogicStubGenerator()

        val typeAlias = generator.getTickTypeAlias(node)
        assertEquals("FilterTickBlock<Int>", typeAlias)

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("Filter (1 inputs, 1 outputs)"))
        assertTrue(result.contains("value ->"))
        assertTrue(result.contains("true"))  // filter default return
    }

    // ========== Processor Nodes (multi-input/multi-output) ==========

    @Test
    fun `processor node 2 in 1 out produces In2Out1TickBlock`() {
        val node = createTestCodeNode(
            "proc", "Merger",
            inputPorts = listOf(
                inputPort("p_a", "first", Int::class, "proc"),
                inputPort("p_b", "second", Int::class, "proc")
            ),
            outputPorts = listOf(outputPort("p_r", "result", Int::class, "proc"))
        )
        val generator = ProcessingLogicStubGenerator()

        assertEquals("In2Out1TickBlock<Int, Int, Int>", generator.getTickTypeAlias(node))
    }

    @Test
    fun `processor node 2 in 2 out produces In2Out2TickBlock`() {
        val node = createTestCodeNode(
            "proc", "DualProcessor",
            inputPorts = listOf(
                inputPort("p_a", "inputA", Int::class, "proc"),
                inputPort("p_b", "inputB", String::class, "proc")
            ),
            outputPorts = listOf(
                outputPort("p_u", "outA", Double::class, "proc"),
                outputPort("p_v", "outB", Boolean::class, "proc")
            )
        )
        val generator = ProcessingLogicStubGenerator()

        assertEquals("In2Out2TickBlock<Int, String, Double, Boolean>", generator.getTickTypeAlias(node))

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("Processor (2 inputs, 2 outputs)"))
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.ProcessResult2"))
        assertTrue(result.contains("ProcessResult2.both(0.0, false)"))
    }

    @Test
    fun `processor node 1 in 2 out produces In1Out2TickBlock`() {
        val node = createTestCodeNode(
            "split", "Splitter",
            inputPorts = listOf(inputPort("s_in", "input", String::class, "split")),
            outputPorts = listOf(
                outputPort("s_o1", "left", Int::class, "split"),
                outputPort("s_o2", "right", Boolean::class, "split")
            )
        )
        val generator = ProcessingLogicStubGenerator()

        assertEquals("In1Out2TickBlock<String, Int, Boolean>", generator.getTickTypeAlias(node))
    }

    @Test
    fun `processor node 3 in 1 out produces In3Out1TickBlock`() {
        val node = createTestCodeNode(
            "merge", "TriMerger",
            inputPorts = listOf(
                inputPort("m_a", "a", Int::class, "merge"),
                inputPort("m_b", "b", Int::class, "merge"),
                inputPort("m_c", "c", Int::class, "merge")
            ),
            outputPorts = listOf(outputPort("m_r", "result", Int::class, "merge"))
        )
        val generator = ProcessingLogicStubGenerator()

        assertEquals("In3Out1TickBlock<Int, Int, Int, Int>", generator.getTickTypeAlias(node))
    }

    // ========== Edge Cases ==========

    @Test
    fun `generateStub returns empty string for 0 in 0 out node`() {
        val node = createTestCodeNode("empty", "EmptyNode")
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertEquals("", result)
    }

    @Test
    fun `generateStub returns empty string for node with more than 3 inputs`() {
        val node = createTestCodeNode(
            "big", "BigNode",
            inputPorts = (1..4).map { inputPort("in$it", "input$it", Int::class, "big") },
            outputPorts = listOf(outputPort("out", "output", Int::class, "big"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertEquals("", result)
    }

    @Test
    fun `stub does not contain StateProperties import`() {
        val node = createTestCodeNode(
            "trans", "DataTransformer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", String::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "value", Int::class, "trans"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")

        assertFalse(result.contains("StateProperties"),
            "Stub should not contain StateProperties import")
    }

    // ========== KDoc Content ==========

    @Test
    fun `KDoc lists input ports`() {
        val node = createTestCodeNode(
            "proc", "DataProcessor",
            inputPorts = listOf(
                inputPort("p_data", "data", String::class, "proc"),
                inputPort("p_config", "config", Int::class, "proc")
            ),
            outputPorts = listOf(outputPort("p_out", "result", Boolean::class, "proc"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("- data: String"))
        assertTrue(result.contains("- config: Int"))
    }

    @Test
    fun `KDoc lists output ports`() {
        val node = createTestCodeNode(
            "trans", "ValueTransformer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "value", Double::class, "trans"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("- value: Double"))
    }

    // ========== Default Return Values ==========

    @Test
    fun `default return value for String output is empty string`() {
        val node = createTestCodeNode(
            "trans", "StringTransformer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "text", String::class, "trans"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("\"\""))
    }

    @Test
    fun `default return value for Boolean output is false`() {
        val node = createTestCodeNode(
            "trans", "BoolTransformer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "flag", Boolean::class, "trans"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("false"))
    }

    @Test
    fun `default return value for Long output is 0L`() {
        val node = createTestCodeNode(
            "trans", "LongTransformer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "count", Long::class, "trans"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("0L"))
    }

    @Test
    fun `default return value for Double output is 0_0`() {
        val node = createTestCodeNode(
            "trans", "DoubleTransformer",
            type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("t_in", "input", Int::class, "trans")),
            outputPorts = listOf(outputPort("t_out", "value", Double::class, "trans"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("0.0"))
    }

    // ========== Any-Input Tick Type Aliases ==========

    @Test
    fun `any-input 2 in 1 out produces In2AnyOut1TickBlock`() {
        val node = CodeNode(
            id = "proc",
            name = "AnyAdder",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                inputPort("p_a", "first", Int::class, "proc"),
                inputPort("p_b", "second", Int::class, "proc")
            ),
            outputPorts = listOf(outputPort("p_r", "result", Int::class, "proc")),
            configuration = mapOf("_genericType" to "in2anyout1")
        )
        val generator = ProcessingLogicStubGenerator()

        assertEquals("In2AnyOut1TickBlock<Int, Int, Int>", generator.getTickTypeAlias(node, anyInput = true))

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.In2AnyOut1TickBlock"),
            "Should import any-input tick type alias")
        assertTrue(result.contains("val anyAdderTick: In2AnyOut1TickBlock<Int, Int, Int>"),
            "Should use any-input tick type alias in val declaration")
    }

    @Test
    fun `any-input 2 in 0 out shouldGenerateStub returns false`() {
        val node = CodeNode(
            id = "sink",
            name = "AnySink",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                inputPort("s_a", "data", Int::class, "sink"),
                inputPort("s_b", "label", String::class, "sink")
            ),
            outputPorts = emptyList(),
            configuration = mapOf("_genericType" to "in2anyout0")
        )
        val generator = ProcessingLogicStubGenerator()

        assertFalse(generator.shouldGenerateStub(node),
            "Any-input sink nodes (0 outputs) should not generate stubs")
        assertEquals("", generator.getTickTypeAlias(node, anyInput = true),
            "Any-input sink nodes should return empty tick type alias")
        assertEquals("", generator.generateStub(node, "io.codenode.generated"),
            "Any-input sink nodes should return empty stub")
    }

    @Test
    fun `any-input 3 in 3 out produces In3AnyOut3TickBlock`() {
        val node = CodeNode(
            id = "proc",
            name = "AnyFull",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc"),
                inputPort("i3", "c", Boolean::class, "proc")
            ),
            outputPorts = listOf(
                outputPort("o1", "x", Double::class, "proc"),
                outputPort("o2", "y", Float::class, "proc"),
                outputPort("o3", "z", Long::class, "proc")
            ),
            configuration = mapOf("_genericType" to "in3anyout3")
        )
        val generator = ProcessingLogicStubGenerator()

        assertEquals("In3AnyOut3TickBlock<Int, String, Boolean, Double, Float, Long>",
            generator.getTickTypeAlias(node, anyInput = true))
    }

    @Test
    fun `generateStub auto-detects anyInput from genericType configuration`() {
        val node = CodeNode(
            id = "proc",
            name = "AnyMerger",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                inputPort("p_a", "first", Int::class, "proc"),
                inputPort("p_b", "second", String::class, "proc")
            ),
            outputPorts = listOf(outputPort("p_r", "result", Int::class, "proc")),
            configuration = mapOf("_genericType" to "in2anyout1")
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("In2AnyOut1TickBlock"),
            "generateStub should auto-detect anyInput from _genericType")
    }

    @Test
    fun `generateStubWithPreservedBody auto-detects anyInput from genericType`() {
        val node = CodeNode(
            id = "proc",
            name = "AnyMerger",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                inputPort("p_a", "first", Int::class, "proc"),
                inputPort("p_b", "second", String::class, "proc")
            ),
            outputPorts = listOf(outputPort("p_r", "result", Int::class, "proc")),
            configuration = mapOf("_genericType" to "in2anyout1")
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStubWithPreservedBody(node, "io.codenode.generated", "    first.length")
        assertTrue(result.contains("In2AnyOut1TickBlock"),
            "generateStubWithPreservedBody should auto-detect anyInput from _genericType")
        assertTrue(result.contains("first.length"),
            "Should preserve the body")
    }
}
