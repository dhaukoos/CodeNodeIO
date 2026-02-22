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
    fun `shouldGenerateStub returns true for valid node`() {
        val node = createTestCodeNode(
            "gen", "Generator",
            outputPorts = listOf(outputPort("out", "value", Int::class, "gen"))
        )
        val generator = ProcessingLogicStubGenerator()

        assertTrue(generator.shouldGenerateStub(node))
    }

    // ========== Generator Nodes (0 inputs) ==========

    @Test
    fun `generator node 0 in 1 out produces GeneratorTickBlock`() {
        val node = createTestCodeNode(
            "gen", "DataGenerator",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("gen_value", "value", Int::class, "gen"))
        )
        val generator = ProcessingLogicStubGenerator()

        val typeAlias = generator.getTickTypeAlias(node)
        assertEquals("GeneratorTickBlock<Int>", typeAlias)

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("package io.codenode.generated"))
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.GeneratorTickBlock"))
        assertTrue(result.contains("val dataGeneratorTick: GeneratorTickBlock<Int>"))
        assertTrue(result.contains("Generator (0 inputs, 1 outputs)"))
        assertTrue(result.contains("0"))  // default Int return
    }

    @Test
    fun `generator node 0 in 2 out produces Out2TickBlock`() {
        val node = createTestCodeNode(
            "timer", "TimerEmitter",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("timer_sec", "elapsedSeconds", Int::class, "timer"),
                outputPort("timer_min", "elapsedMinutes", Int::class, "timer")
            )
        )
        val generator = ProcessingLogicStubGenerator()

        val typeAlias = generator.getTickTypeAlias(node)
        assertEquals("Out2TickBlock<Int, Int>", typeAlias)

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.Out2TickBlock"))
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.ProcessResult2"))
        assertTrue(result.contains("val timerEmitterTick: Out2TickBlock<Int, Int>"))
        assertTrue(result.contains("ProcessResult2.both(0, 0)"))
    }

    @Test
    fun `generator node 0 in 3 out produces Out3TickBlock`() {
        val node = createTestCodeNode(
            "multi", "MultiOutput",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("o1", "first", Int::class, "multi"),
                outputPort("o2", "second", String::class, "multi"),
                outputPort("o3", "third", Boolean::class, "multi")
            )
        )
        val generator = ProcessingLogicStubGenerator()

        val typeAlias = generator.getTickTypeAlias(node)
        assertEquals("Out3TickBlock<Int, String, Boolean>", typeAlias)

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.ProcessResult3"))
        assertTrue(result.contains("ProcessResult3(0, \"\", false)"))
    }

    // ========== Sink Nodes (0 outputs) ==========

    @Test
    fun `sink node 1 in 0 out produces SinkTickBlock`() {
        val node = createTestCodeNode(
            "sink", "DataSink",
            type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("sink_data", "data", String::class, "sink"))
        )
        val generator = ProcessingLogicStubGenerator()

        val typeAlias = generator.getTickTypeAlias(node)
        assertEquals("SinkTickBlock<String>", typeAlias)

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("import io.codenode.fbpdsl.runtime.SinkTickBlock"))
        assertTrue(result.contains("val dataSinkTick: SinkTickBlock<String>"))
        assertTrue(result.contains("Sink (1 inputs, 0 outputs)"))
        assertTrue(result.contains("data ->"))
        // Sink has no return value — no ProcessResult import
        assertFalse(result.contains("ProcessResult"))
    }

    @Test
    fun `sink node 2 in 0 out produces In2SinkTickBlock`() {
        val node = createTestCodeNode(
            "display", "DisplayReceiver",
            type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("d_sec", "seconds", Int::class, "display"),
                inputPort("d_min", "minutes", Int::class, "display")
            )
        )
        val generator = ProcessingLogicStubGenerator()

        val typeAlias = generator.getTickTypeAlias(node)
        assertEquals("In2SinkTickBlock<Int, Int>", typeAlias)

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("val displayReceiverTick: In2SinkTickBlock<Int, Int>"))
        assertTrue(result.contains("seconds, minutes ->"))
    }

    @Test
    fun `sink node 3 in 0 out produces In3SinkTickBlock`() {
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

        assertEquals("In3SinkTickBlock<Int, String, Boolean>", generator.getTickTypeAlias(node))
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
            "gen", "ValueGenerator",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("gen_val", "value", Double::class, "gen"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("- value: Double"))
    }

    // ========== Default Return Values ==========

    @Test
    fun `default return value for String output is empty string`() {
        val node = createTestCodeNode(
            "gen", "StringGen",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("gen_s", "text", String::class, "gen"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("\"\""))
    }

    @Test
    fun `default return value for Boolean output is false`() {
        val node = createTestCodeNode(
            "gen", "BoolGen",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("gen_b", "flag", Boolean::class, "gen"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("false"))
    }

    @Test
    fun `default return value for Long output is 0L`() {
        val node = createTestCodeNode(
            "gen", "LongGen",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("gen_l", "count", Long::class, "gen"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("0L"))
    }

    @Test
    fun `default return value for Double output is 0_0`() {
        val node = createTestCodeNode(
            "gen", "DoubleGen",
            type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("gen_d", "value", Double::class, "gen"))
        )
        val generator = ProcessingLogicStubGenerator()

        val result = generator.generateStub(node, "io.codenode.generated")
        assertTrue(result.contains("0.0"))
    }
}
