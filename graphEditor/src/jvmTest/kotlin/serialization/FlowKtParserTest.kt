/*
 * FlowKtParser Test
 * TDD tests for parsing .flow.kt files back to FlowGraph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.serialization

import io.codenode.fbpdsl.model.*
import io.codenode.kotlincompiler.generator.FlowKtGenerator
import kotlin.test.*

/**
 * TDD tests for FlowKtParser - parses .flow.kt files back to FlowGraph.
 *
 * These tests are written FIRST and should FAIL until FlowKtParser is implemented.
 *
 * T016: Test for parsing .flow.kt back to FlowGraph
 * T017: Test for round-trip: FlowGraph → .flow.kt → FlowGraph equality
 */
class FlowKtParserTest {

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
        ),
        configuration: Map<String, String> = emptyMap()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(100.0, 200.0),
            inputPorts = inputPorts,
            outputPorts = outputPorts,
            configuration = configuration
        )
    }

    private fun createTestFlowGraph(
        name: String = "TestFlow",
        nodes: List<Node> = listOf(createTestCodeNode("node1", "Processor")),
        connections: List<Connection> = emptyList()
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            description = "Test flow for parsing",
            rootNodes = nodes,
            connections = connections,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }

    // ========== T016: Parse .flow.kt to FlowGraph ==========

    @Test
    fun `T016 - parseFlowKt returns ParseResult`() {
        // Given
        val flowKtContent = """
            package io.codenode.generated.test

            import io.codenode.fbpdsl.dsl.*

            val testFlowGraph = flowGraph("TestFlow", version = "1.0.0") {
                val processor = codeNode("Processor", nodeType = TRANSFORMER) {
                    position(100.0, 200.0)
                    input("input", String::class)
                    output("output", String::class)
                }
            }
        """.trimIndent()
        val parser = FlowKtParser()

        // When
        val result = parser.parseFlowKt(flowKtContent)

        // Then
        assertNotNull(result, "Parser should return a result")
    }

    @Test
    fun `T016 - parseFlowKt extracts FlowGraph name`() {
        // Given
        val flowKtContent = """
            package io.codenode.generated.myflow

            import io.codenode.fbpdsl.dsl.*

            val myFlowGraph = flowGraph("MyCustomFlow", version = "2.0.0") {
                val node = codeNode("Node1", nodeType = TRANSFORMER) {
                    position(0.0, 0.0)
                }
            }
        """.trimIndent()
        val parser = FlowKtParser()

        // When
        val result = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(result.isSuccess, "Parse should succeed")
        assertEquals("MyCustomFlow", result.graph?.name, "Should extract correct FlowGraph name")
    }

    @Test
    fun `T016 - parseFlowKt extracts FlowGraph version`() {
        // Given
        val flowKtContent = """
            package io.codenode.generated

            val graph = flowGraph("VersionTest", version = "3.5.1") {
                val n = codeNode("N", nodeType = TRANSFORMER) { position(0.0, 0.0) }
            }
        """.trimIndent()
        val parser = FlowKtParser()

        // When
        val result = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("3.5.1", result.graph?.version, "Should extract correct version")
    }

    @Test
    fun `T016 - parseFlowKt extracts CodeNode names`() {
        // Given
        val flowKtContent = """
            package io.codenode.generated

            val graph = flowGraph("NodeNameTest", version = "1.0.0") {
                val timerEmitter = codeNode("TimerEmitter", nodeType = GENERATOR) {
                    position(100.0, 100.0)
                }
                val displayReceiver = codeNode("DisplayReceiver", nodeType = SINK) {
                    position(400.0, 100.0)
                }
            }
        """.trimIndent()
        val parser = FlowKtParser()

        // When
        val result = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(result.isSuccess)
        val nodeNames = result.graph?.getAllCodeNodes()?.map { it.name } ?: emptyList()
        assertTrue(nodeNames.contains("TimerEmitter"), "Should have TimerEmitter node")
        assertTrue(nodeNames.contains("DisplayReceiver"), "Should have DisplayReceiver node")
    }

    @Test
    fun `T016 - parseFlowKt extracts node positions`() {
        // Given
        val flowKtContent = """
            package io.codenode.generated

            val graph = flowGraph("PositionTest", version = "1.0.0") {
                val node = codeNode("PositionedNode", nodeType = TRANSFORMER) {
                    position(150.5, 250.5)
                }
            }
        """.trimIndent()
        val parser = FlowKtParser()

        // When
        val result = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(result.isSuccess)
        val node = result.graph?.getAllCodeNodes()?.firstOrNull()
        assertNotNull(node)
        assertEquals(150.5, node.position.x, 0.1, "Should extract correct X position")
        assertEquals(250.5, node.position.y, 0.1, "Should extract correct Y position")
    }

    @Test
    fun `T016 - parseFlowKt extracts connections`() {
        // Given
        val flowKtContent = """
            package io.codenode.generated

            val graph = flowGraph("ConnectionTest", version = "1.0.0") {
                val source = codeNode("Source", nodeType = GENERATOR) {
                    position(100.0, 100.0)
                    output("data", Int::class)
                }
                val sink = codeNode("Sink", nodeType = SINK) {
                    position(300.0, 100.0)
                    input("data", Int::class)
                }

                source.output("data") connect sink.input("data")
            }
        """.trimIndent()
        val parser = FlowKtParser()

        // When
        val result = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(result.isSuccess)
        assertTrue((result.graph?.connections?.size ?: 0) >= 1,
            "Should have at least one connection")
    }

    @Test
    fun `T016 - parseFlowKt returns error for invalid syntax`() {
        // Given
        val invalidContent = """
            this is not valid kotlin {{{
            random garbage
        """.trimIndent()
        val parser = FlowKtParser()

        // When
        val result = parser.parseFlowKt(invalidContent)

        // Then
        assertFalse(result.isSuccess, "Should fail for invalid syntax")
        assertNotNull(result.errorMessage, "Should have error message")
    }

    // ========== T017: Round-Trip Equality ==========

    @Test
    fun `T017 - round trip preserves FlowGraph name`() {
        // Given
        val original = createTestFlowGraph("RoundTripName")
        val generator = FlowKtGenerator()
        val parser = FlowKtParser()

        // When
        val flowKtContent = generator.generateFlowKt(original, "io.codenode.generated.roundtrip")
        val parseResult = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(parseResult.isSuccess, "Parse should succeed")
        assertEquals(original.name, parseResult.graph?.name, "Name should survive round-trip")
    }

    @Test
    fun `T017 - round trip preserves FlowGraph version`() {
        // Given
        val original = createTestFlowGraph("VersionRoundTrip").copy(version = "2.3.4")
        val generator = FlowKtGenerator()
        val parser = FlowKtParser()

        // When
        val flowKtContent = generator.generateFlowKt(original, "io.codenode.generated")
        val parseResult = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(parseResult.isSuccess)
        assertEquals(original.version, parseResult.graph?.version, "Version should survive round-trip")
    }

    @Test
    fun `T017 - round trip preserves node count`() {
        // Given
        val nodes = listOf(
            createTestCodeNode("n1", "Node1"),
            createTestCodeNode("n2", "Node2"),
            createTestCodeNode("n3", "Node3")
        )
        val original = createTestFlowGraph("NodeCountTest", nodes)
        val generator = FlowKtGenerator()
        val parser = FlowKtParser()

        // When
        val flowKtContent = generator.generateFlowKt(original, "io.codenode.generated")
        val parseResult = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(parseResult.isSuccess)
        assertEquals(3, parseResult.graph?.getAllCodeNodes()?.size,
            "Node count should survive round-trip")
    }

    @Test
    fun `T017 - round trip preserves node names`() {
        // Given
        val nodes = listOf(
            createTestCodeNode("timer", "TimerEmitter"),
            createTestCodeNode("display", "DisplayReceiver")
        )
        val original = createTestFlowGraph("NodeNamesTest", nodes)
        val generator = FlowKtGenerator()
        val parser = FlowKtParser()

        // When
        val flowKtContent = generator.generateFlowKt(original, "io.codenode.generated")
        val parseResult = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(parseResult.isSuccess)
        val parsedNames = parseResult.graph?.getAllCodeNodes()?.map { it.name }?.toSet() ?: emptySet()
        assertTrue(parsedNames.contains("TimerEmitter"), "TimerEmitter should survive round-trip")
        assertTrue(parsedNames.contains("DisplayReceiver"), "DisplayReceiver should survive round-trip")
    }

    @Test
    fun `T017 - round trip preserves connection count`() {
        // Given
        val node1 = createTestCodeNode("src", "Source",
            outputPorts = listOf(Port(id = "out1", name = "output", direction = Port.Direction.OUTPUT, dataType = String::class, owningNodeId = "src")))
        val node2 = createTestCodeNode("dest", "Destination",
            inputPorts = listOf(Port(id = "in1", name = "input", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "dest")))
        val connections = listOf(
            Connection("conn1", "src", "out1", "dest", "in1")
        )
        val original = createTestFlowGraph("ConnCountTest", listOf(node1, node2), connections)
        val generator = FlowKtGenerator()
        val parser = FlowKtParser()

        // When
        val flowKtContent = generator.generateFlowKt(original, "io.codenode.generated")
        val parseResult = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(parseResult.isSuccess)
        assertEquals(1, parseResult.graph?.connections?.size,
            "Connection count should survive round-trip")
    }

    @Test
    fun `T017 - round trip preserves node positions approximately`() {
        // Given
        val node = createTestCodeNode("pos", "PositionNode").copy(
            position = Node.Position(123.0, 456.0)
        )
        val original = createTestFlowGraph("PosRoundTrip", listOf(node))
        val generator = FlowKtGenerator()
        val parser = FlowKtParser()

        // When
        val flowKtContent = generator.generateFlowKt(original, "io.codenode.generated")
        val parseResult = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(parseResult.isSuccess)
        val parsedNode = parseResult.graph?.getAllCodeNodes()?.firstOrNull()
        assertNotNull(parsedNode)
        assertEquals(123.0, parsedNode.position.x, 1.0, "X position should survive round-trip")
        assertEquals(456.0, parsedNode.position.y, 1.0, "Y position should survive round-trip")
    }

    // ========== Quoted nodeType format tests ==========

    @Test
    fun `parseFlowKt handles quoted nodeType values`() {
        // Given - nodeType with quotes (as used in compiled .flow.kt files)
        val flowKtContent = """
            package io.codenode.stopwatch

            import io.codenode.fbpdsl.dsl.*
            import io.codenode.fbpdsl.model.*

            val stopWatchFlowGraph = flowGraph("StopWatch", version = "1.0.0", description = "Test") {
                val timerEmitter = codeNode("TimerEmitter", nodeType = "GENERATOR") {
                    position(100.0, 100.0)
                    output("elapsedSeconds", Int::class)
                }

                val displayReceiver = codeNode("DisplayReceiver", nodeType = "SINK") {
                    position(400.0, 100.0)
                    input("seconds", Int::class)
                }

                timerEmitter.output("elapsedSeconds") connect displayReceiver.input("seconds")
            }
        """.trimIndent()
        val parser = FlowKtParser()

        // When
        val result = parser.parseFlowKt(flowKtContent)

        // Then
        assertTrue(result.isSuccess, "Should parse .flow.kt with quoted nodeType: ${result.errorMessage}")
        assertEquals("StopWatch", result.graph?.name)
        val nodeNames = result.graph?.getAllCodeNodes()?.map { it.name } ?: emptyList()
        assertTrue(nodeNames.contains("TimerEmitter"), "Should have TimerEmitter node")
        assertTrue(nodeNames.contains("DisplayReceiver"), "Should have DisplayReceiver node")
        assertEquals(1, result.graph?.connections?.size, "Should have 1 connection")
    }
}
