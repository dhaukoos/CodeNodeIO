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

    // ========== Test: 0 in, 2 out → createTimedOut2Generator ==========

    @Test
    fun `0 in 2 out returns createTimedOut2Generator`() {
        val node = createTestCodeNode(
            id = "gen", name = "TimerEmitter", type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("o1", "seconds", Int::class, "gen"),
                outputPort("o2", "minutes", Int::class, "gen")
            )
        )

        assertEquals("createTimedOut2Generator", resolver.getFactoryMethodName(node))
        assertEquals("tick", resolver.getTickParamName(node))
        assertEquals("Out2GeneratorRuntime<Int, Int>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: 2 in, 0 out → createIn2Sink ==========

    @Test
    fun `2 in 0 out returns createIn2Sink`() {
        val node = createTestCodeNode(
            id = "sink", name = "DisplayReceiver", type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("i1", "seconds", Int::class, "sink"),
                inputPort("i2", "minutes", Int::class, "sink")
            )
        )

        assertEquals("createIn2Sink", resolver.getFactoryMethodName(node))
        assertEquals("consume", resolver.getTickParamName(node))
        assertEquals("In2SinkRuntime<Int, Int>", resolver.getRuntimeTypeName(node))
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

    // ========== Test: 0 in, 1 out → createContinuousGenerator ==========

    @Test
    fun `0 in 1 out returns createContinuousGenerator`() {
        val node = createTestCodeNode(
            id = "gen", name = "ValueGenerator", type = CodeNodeType.GENERATOR,
            outputPorts = listOf(outputPort("out", "value", Int::class, "gen"))
        )

        assertEquals("createContinuousGenerator", resolver.getFactoryMethodName(node))
        assertEquals("tick", resolver.getTickParamName(node))
        assertEquals("GeneratorRuntime<Int>", resolver.getRuntimeTypeName(node))
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

    // ========== Test: 0 in, 3 out → createTimedOut3Generator ==========

    @Test
    fun `0 in 3 out returns createTimedOut3Generator`() {
        val node = createTestCodeNode(
            id = "gen", name = "TriGenerator", type = CodeNodeType.GENERATOR,
            outputPorts = listOf(
                outputPort("o1", "first", Int::class, "gen"),
                outputPort("o2", "second", String::class, "gen"),
                outputPort("o3", "third", Boolean::class, "gen")
            )
        )

        assertEquals("createTimedOut3Generator", resolver.getFactoryMethodName(node))
        assertEquals("tick", resolver.getTickParamName(node))
        assertEquals("Out3GeneratorRuntime<Int, String, Boolean>", resolver.getRuntimeTypeName(node))
    }

    // ========== Test: 3 in, 0 out → createIn3Sink ==========

    @Test
    fun `3 in 0 out returns createIn3Sink`() {
        val node = createTestCodeNode(
            id = "sink", name = "TriSink", type = CodeNodeType.SINK,
            inputPorts = listOf(
                inputPort("i1", "a", Int::class, "sink"),
                inputPort("i2", "b", String::class, "sink"),
                inputPort("i3", "c", Boolean::class, "sink")
            )
        )

        assertEquals("createIn3Sink", resolver.getFactoryMethodName(node))
        assertEquals("consume", resolver.getTickParamName(node))
        assertEquals("In3SinkRuntime<Int, String, Boolean>", resolver.getRuntimeTypeName(node))
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
}
