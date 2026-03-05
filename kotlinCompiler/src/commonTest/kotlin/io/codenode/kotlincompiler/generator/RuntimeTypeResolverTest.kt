/*
 * RuntimeTypeResolver Test
 * Tests for mapping node port counts to factory methods and runtime types
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for RuntimeTypeResolver - maps (inputPortCount, outputPortCount)
 * to CodeNodeFactory method names, runtime type names, and tick parameter names.
 */
class RuntimeTypeResolverTest {

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

    private val resolver = RuntimeTypeResolver()

    // ========== Test: 0 in, 2 out → createSourceOut2 ==========

    @Test
    fun `0 in 2 out returns createSourceOut2`() {
        val node = createTestCodeNode(
            id = "gen", name = "TimerEmitter", type = CodeNodeType.SOURCE,
            outputPorts = listOf(
                outputPort("o1", "seconds", Int::class, "gen"),
                outputPort("o2", "minutes", Int::class, "gen")
            )
        )

        assertEquals("createSourceOut2", resolver.getFactoryMethodName(node))
        assertEquals("generate", resolver.getTickParamName(node))
        assertEquals("SourceOut2Runtime<Int, Int>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: 2 in, 0 out → createSinkIn2 ==========

    @Test
    fun `2 in 0 out returns createSinkIn2`() {
        val node = createTestCodeNode(
            id = "sink", name = "DisplayReceiver", type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("i1", "seconds", Int::class, "sink"),
                inputPort("i2", "minutes", Int::class, "sink")
            )
        )

        assertEquals("createSinkIn2", resolver.getFactoryMethodName(node))
        assertEquals("consume", resolver.getTickParamName(node))
        assertEquals("SinkIn2Runtime<Int, Int>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: 1 in, 1 out (different types) → createContinuousTransformer ==========

    @Test
    fun `1 in 1 out different types returns createContinuousTransformer`() {
        val node = createTestCodeNode(
            id = "trans", name = "DataTransformer", type = CodeNodeType.TRANSFORMER,
            inputPorts = listOf(inputPort("in", "input", String::class, "trans")),
            outputPorts = listOf(outputPort("out", "output", Int::class, "trans"))
        )

        assertEquals("createContinuousTransformer", resolver.getFactoryMethodName(node))
        assertEquals("transform", resolver.getTickParamName(node))
        assertEquals("TransformerRuntime<String, Int>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: 0 in, 1 out → createContinuousSource ==========

    @Test
    fun `0 in 1 out returns createContinuousSource`() {
        val node = createTestCodeNode(
            id = "gen", name = "ValueGenerator", type = CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("out", "value", Int::class, "gen"))
        )

        assertEquals("createContinuousSource", resolver.getFactoryMethodName(node))
        assertEquals("generate", resolver.getTickParamName(node))
        assertEquals("SourceRuntime<Int>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: 1 in, 0 out → createContinuousSink ==========

    @Test
    fun `1 in 0 out returns createContinuousSink`() {
        val node = createTestCodeNode(
            id = "sink", name = "Logger", type = CodeNodeType.SINK,
            inputPorts = listOf(inputPort("in", "message", String::class, "sink"))
        )

        assertEquals("createContinuousSink", resolver.getFactoryMethodName(node))
        assertEquals("consume", resolver.getTickParamName(node))
        assertEquals("SinkRuntime<String>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: 1 in, 1 out (same types) → createContinuousFilter ==========

    @Test
    fun `1 in 1 out same types returns createContinuousFilter`() {
        val node = createTestCodeNode(
            id = "filt", name = "EvenFilter", type = CodeNodeType.FILTER,
            inputPorts = listOf(inputPort("in", "value", Int::class, "filt")),
            outputPorts = listOf(outputPort("out", "value", Int::class, "filt"))
        )

        assertEquals("createContinuousFilter", resolver.getFactoryMethodName(node))
        assertEquals("filter", resolver.getTickParamName(node))
        assertEquals("FilterRuntime<Int>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: 0 in, 3 out → createSourceOut3 ==========

    @Test
    fun `0 in 3 out returns createSourceOut3`() {
        val node = createTestCodeNode(
            id = "gen", name = "TriGenerator", type = CodeNodeType.SOURCE,
            outputPorts = listOf(
                outputPort("o1", "first", Int::class, "gen"),
                outputPort("o2", "second", String::class, "gen"),
                outputPort("o3", "third", Boolean::class, "gen")
            )
        )

        assertEquals("createSourceOut3", resolver.getFactoryMethodName(node))
        assertEquals("generate", resolver.getTickParamName(node))
        assertEquals("SourceOut3Runtime<Int, String, Boolean>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: 3 in, 0 out → createSinkIn3 ==========

    @Test
    fun `3 in 0 out returns createSinkIn3`() {
        val node = createTestCodeNode(
            id = "sink", name = "TriSink", type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "sink"),
                inputPort("i2", "b", String::class, "sink"),
                inputPort("i3", "c", Boolean::class, "sink")
            )
        )

        assertEquals("createSinkIn3", resolver.getFactoryMethodName(node))
        assertEquals("consume", resolver.getTickParamName(node))
        assertEquals("SinkIn3Runtime<Int, String, Boolean>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: Multi-input, multi-output processors ==========

    @Test
    fun `2 in 1 out returns createIn2Out1Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "Merger",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", Int::class, "proc")
            ),
            outputPorts = listOf(outputPort("o1", "result", Int::class, "proc"))
        )

        assertEquals("createIn2Out1Processor", resolver.getFactoryMethodName(node))
        assertEquals("process", resolver.getTickParamName(node))
        assertEquals("In2Out1Runtime<Int, Int, Int>", resolver.getRuntimeTypeName(node))
    }

    @Test
    fun `3 in 1 out returns createIn3Out1Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "TriMerger",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc"),
                inputPort("i3", "c", Boolean::class, "proc")
            ),
            outputPorts = listOf(outputPort("o1", "result", Int::class, "proc"))
        )

        assertEquals("createIn3Out1Processor", resolver.getFactoryMethodName(node))
        assertEquals("process", resolver.getTickParamName(node))
        assertEquals("In3Out1Runtime<Int, String, Boolean, Int>", resolver.getRuntimeTypeName(node))
    }

    @Test
    fun `1 in 2 out returns createIn1Out2Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "Splitter",
            inputPorts = listOf(inputPort("i1", "input", String::class, "proc")),
            outputPorts = listOf(
                outputPort("o1", "left", Int::class, "proc"),
                outputPort("o2", "right", Boolean::class, "proc")
            )
        )

        assertEquals("createIn1Out2Processor", resolver.getFactoryMethodName(node))
        assertEquals("process", resolver.getTickParamName(node))
        assertEquals("In1Out2Runtime<String, Int, Boolean>", resolver.getRuntimeTypeName(node))
    }

    @Test
    fun `1 in 3 out returns createIn1Out3Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "TriSplitter",
            inputPorts = listOf(inputPort("i1", "input", Int::class, "proc")),
            outputPorts = listOf(
                outputPort("o1", "a", Int::class, "proc"),
                outputPort("o2", "b", String::class, "proc"),
                outputPort("o3", "c", Boolean::class, "proc")
            )
        )

        assertEquals("createIn1Out3Processor", resolver.getFactoryMethodName(node))
        assertEquals("process", resolver.getTickParamName(node))
        assertEquals("In1Out3Runtime<Int, Int, String, Boolean>", resolver.getRuntimeTypeName(node))
    }

    @Test
    fun `2 in 2 out returns createIn2Out2Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "DualProcessor",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc")
            ),
            outputPorts = listOf(
                outputPort("o1", "x", Double::class, "proc"),
                outputPort("o2", "y", Boolean::class, "proc")
            )
        )

        assertEquals("createIn2Out2Processor", resolver.getFactoryMethodName(node))
        assertEquals("process", resolver.getTickParamName(node))
        assertEquals("In2Out2Runtime<Int, String, Double, Boolean>", resolver.getRuntimeTypeName(node))
    }

    @Test
    fun `3 in 3 out returns createIn3Out3Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "FullProcessor",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc"),
                inputPort("i3", "c", Boolean::class, "proc")
            ),
            outputPorts = listOf(
                outputPort("o1", "x", Double::class, "proc"),
                outputPort("o2", "y", Float::class, "proc"),
                outputPort("o3", "z", Long::class, "proc")
            )
        )

        assertEquals("createIn3Out3Processor", resolver.getFactoryMethodName(node))
        assertEquals("process", resolver.getTickParamName(node))
        assertEquals("In3Out3Runtime<Int, String, Boolean, Double, Float, Long>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: getTypeParams ==========

    @Test
    fun `getTypeParams returns comma separated input then output types`() {
        val node = createTestCodeNode(
            id = "proc", name = "Processor",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc")
            ),
            outputPorts = listOf(outputPort("o1", "result", Boolean::class, "proc"))
        )

        assertEquals("Int, String, Boolean", resolver.getTypeParams(node))
    }

    @Test
    fun `getTypeParams for generator with no inputs returns only output types`() {
        val node = createTestCodeNode(
            id = "gen", name = "Generator",
            outputPorts = listOf(
                outputPort("o1", "value", Int::class, "gen"),
                outputPort("o2", "flag", Boolean::class, "gen")
            )
        )

        assertEquals("Int, Boolean", resolver.getTypeParams(node))
    }

    // ========== Any-Input Variant Tests ==========

    @Test
    fun `2 in 0 out anyInput returns createSinkIn2Any`() {
        val node = createTestCodeNode(
            id = "sink", name = "AnySink", type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "sink"),
                inputPort("i2", "b", Int::class, "sink")
            )
        )

        assertEquals("createSinkIn2Any", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("consume", resolver.getTickParamName(node, anyInput = true))
        assertEquals("SinkIn2AnyRuntime<Int, Int>", resolver.getRuntimeTypeName(node, anyInput = true))
    }

    @Test
    fun `2 in 1 out anyInput returns createIn2AnyOut1Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "AnyAdder",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", Int::class, "proc")
            ),
            outputPorts = listOf(outputPort("o1", "result", Int::class, "proc"))
        )

        assertEquals("createIn2AnyOut1Processor", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("process", resolver.getTickParamName(node, anyInput = true))
        assertEquals("In2AnyOut1Runtime<Int, Int, Int>", resolver.getRuntimeTypeName(node, anyInput = true))
    }

    @Test
    fun `2 in 2 out anyInput returns createIn2AnyOut2Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "AnyDual",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc")
            ),
            outputPorts = listOf(
                outputPort("o1", "x", Double::class, "proc"),
                outputPort("o2", "y", Boolean::class, "proc")
            )
        )

        assertEquals("createIn2AnyOut2Processor", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("In2AnyOut2Runtime<Int, String, Double, Boolean>", resolver.getRuntimeTypeName(node, anyInput = true))
    }

    @Test
    fun `2 in 3 out anyInput returns createIn2AnyOut3Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "AnyTriOut",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc")
            ),
            outputPorts = listOf(
                outputPort("o1", "x", Int::class, "proc"),
                outputPort("o2", "y", String::class, "proc"),
                outputPort("o3", "z", Boolean::class, "proc")
            )
        )

        assertEquals("createIn2AnyOut3Processor", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("In2AnyOut3Runtime<Int, String, Int, String, Boolean>", resolver.getRuntimeTypeName(node, anyInput = true))
    }

    @Test
    fun `3 in 0 out anyInput returns createSinkIn3Any`() {
        val node = createTestCodeNode(
            id = "sink", name = "AnyTriSink", type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "sink"),
                inputPort("i2", "b", String::class, "sink"),
                inputPort("i3", "c", Boolean::class, "sink")
            )
        )

        assertEquals("createSinkIn3Any", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("SinkIn3AnyRuntime<Int, String, Boolean>", resolver.getRuntimeTypeName(node, anyInput = true))
    }

    @Test
    fun `3 in 1 out anyInput returns createIn3AnyOut1Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "AnyTriMerger",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc"),
                inputPort("i3", "c", Boolean::class, "proc")
            ),
            outputPorts = listOf(outputPort("o1", "result", Int::class, "proc"))
        )

        assertEquals("createIn3AnyOut1Processor", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("In3AnyOut1Runtime<Int, String, Boolean, Int>", resolver.getRuntimeTypeName(node, anyInput = true))
    }

    @Test
    fun `3 in 2 out anyInput returns createIn3AnyOut2Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "AnyTriDual",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc"),
                inputPort("i3", "c", Boolean::class, "proc")
            ),
            outputPorts = listOf(
                outputPort("o1", "x", Double::class, "proc"),
                outputPort("o2", "y", Float::class, "proc")
            )
        )

        assertEquals("createIn3AnyOut2Processor", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("In3AnyOut2Runtime<Int, String, Boolean, Double, Float>", resolver.getRuntimeTypeName(node, anyInput = true))
    }

    @Test
    fun `3 in 3 out anyInput returns createIn3AnyOut3Processor`() {
        val node = createTestCodeNode(
            id = "proc", name = "AnyFullProcessor",
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "proc"),
                inputPort("i2", "b", String::class, "proc"),
                inputPort("i3", "c", Boolean::class, "proc")
            ),
            outputPorts = listOf(
                outputPort("o1", "x", Double::class, "proc"),
                outputPort("o2", "y", Float::class, "proc"),
                outputPort("o3", "z", Long::class, "proc")
            )
        )

        assertEquals("createIn3AnyOut3Processor", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("In3AnyOut3Runtime<Int, String, Boolean, Double, Float, Long>", resolver.getRuntimeTypeName(node, anyInput = true))
    }

    @Test
    fun `anyInput has no effect on single-input nodes`() {
        val node = createTestCodeNode(
            id = "trans", name = "Transformer",
            inputPorts = listOf(inputPort("in", "input", String::class, "trans")),
            outputPorts = listOf(outputPort("out", "output", Int::class, "trans"))
        )

        // anyInput=true should have no effect for 1-input nodes
        assertEquals("createContinuousTransformer", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("TransformerRuntime<String, Int>", resolver.getRuntimeTypeName(node, anyInput = true))
    }

    @Test
    fun `anyInput has no effect on sources`() {
        val node = createTestCodeNode(
            id = "gen", name = "Generator", type = CodeNodeType.SOURCE,
            outputPorts = listOf(outputPort("out", "value", Int::class, "gen"))
        )

        assertEquals("createContinuousSource", resolver.getFactoryMethodName(node, anyInput = true))
        assertEquals("SourceRuntime<Int>", resolver.getRuntimeTypeName(node, anyInput = true))
    }
}
