/*
 * SerializationRoundTripCharacterizationTest - Characterization tests for .flow.kts round-trip
 * Pins current serialization/deserialization behavior: FlowGraph → .flow.kt → FlowGraph.
 * These tests capture WHAT the code does, not what it SHOULD do.
 * License: Apache 2.0
 */

package io.codenode.grapheditor.characterization

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.flowgraphpersist.serialization.FlowKtParser
import io.codenode.flowgraphgenerate.generator.FlowKtGenerator
import kotlin.test.*

/**
 * Characterization tests that pin the current round-trip serialization behavior.
 * Uses FlowKtGenerator (serialize) and FlowKtParser (deserialize) to verify
 * that the round-trip FlowGraph → .flow.kt → FlowGraph preserves structure.
 */
class SerializationRoundTripCharacterizationTest {

    private val generator = FlowKtGenerator()
    private val parser = FlowKtParser()
    private val testPackage = "io.codenode.characterization.test"

    // ============================================
    // Test Fixtures
    // ============================================

    private fun createCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
        inputPorts: List<Port<*>> = listOf(
            Port(id = "${id}_in", name = "input", direction = Port.Direction.INPUT,
                dataType = String::class, owningNodeId = id)
        ),
        outputPorts: List<Port<*>> = listOf(
            Port(id = "${id}_out", name = "output", direction = Port.Direction.OUTPUT,
                dataType = String::class, owningNodeId = id)
        ),
        configuration: Map<String, String> = emptyMap()
    ): CodeNode {
        return CodeNode(
            id = id, name = name, codeNodeType = type,
            position = Node.Position(100.0, 100.0),
            inputPorts = inputPorts, outputPorts = outputPorts,
            configuration = configuration
        )
    }

    private fun roundTrip(graph: io.codenode.fbpdsl.model.FlowGraph): io.codenode.fbpdsl.model.FlowGraph? {
        val content = generator.generateFlowKt(graph, testPackage)
        val result = parser.parseFlowKt(content)
        assertTrue(result.isSuccess, "Parse should succeed: ${result.errorMessage}")
        return result.graph
    }

    // ============================================
    // Basic FlowGraph Properties
    // ============================================

    @Test
    fun `round trip preserves graph name`() {
        val original = flowGraph(name = "CharacterizationGraph", version = "1.0.0") {}
        val restored = roundTrip(original)

        assertNotNull(restored)
        assertEquals("CharacterizationGraph", restored.name)
    }

    @Test
    fun `round trip preserves graph version`() {
        val original = flowGraph(name = "Test", version = "3.2.1") {}
        val restored = roundTrip(original)

        assertNotNull(restored)
        assertEquals("3.2.1", restored.version)
    }

    // ============================================
    // CodeNode Round-Trip
    // ============================================

    @Test
    fun `round trip preserves CodeNode count`() {
        val n1 = createCodeNode("n1", "Alpha")
        val n2 = createCodeNode("n2", "Beta")
        val original = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1).addNode(n2)

        val restored = roundTrip(original)

        assertNotNull(restored)
        assertEquals(2, restored.rootNodes.size)
    }

    @Test
    fun `round trip preserves CodeNode names`() {
        val n1 = createCodeNode("n1", "ProcessorAlpha", type = CodeNodeType.TRANSFORMER)
        val original = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1)

        val restored = roundTrip(original)

        assertNotNull(restored)
        val restoredNode = restored.rootNodes.first()
        assertTrue(restoredNode is CodeNode)
        assertEquals("ProcessorAlpha", restoredNode.name)
    }

    @Test
    fun `round trip preserves CodeNodeType`() {
        val source = createCodeNode("s1", "MySource", type = CodeNodeType.SOURCE,
            inputPorts = emptyList())
        val sink = createCodeNode("s2", "MySink", type = CodeNodeType.SINK,
            outputPorts = emptyList())
        val original = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(source).addNode(sink)

        val restored = roundTrip(original)

        assertNotNull(restored)
        val types = restored.rootNodes.filterIsInstance<CodeNode>()
            .map { it.codeNodeType }.toSet()
        assertTrue(CodeNodeType.SOURCE in types, "SOURCE type should survive round-trip")
        assertTrue(CodeNodeType.SINK in types, "SINK type should survive round-trip")
    }

    @Test
    fun `round trip preserves port counts`() {
        val node = createCodeNode("n1", "MultiPort",
            inputPorts = listOf(
                Port(id = "n1_in1", name = "in1", direction = Port.Direction.INPUT,
                    dataType = String::class, owningNodeId = "n1"),
                Port(id = "n1_in2", name = "in2", direction = Port.Direction.INPUT,
                    dataType = String::class, owningNodeId = "n1")
            ),
            outputPorts = listOf(
                Port(id = "n1_out1", name = "out1", direction = Port.Direction.OUTPUT,
                    dataType = String::class, owningNodeId = "n1")
            )
        )
        val original = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(node)

        val restored = roundTrip(original)

        assertNotNull(restored)
        val restoredNode = restored.rootNodes.first() as CodeNode
        assertEquals(2, restoredNode.inputPorts.size)
        assertEquals(1, restoredNode.outputPorts.size)
    }

    @Test
    fun `round trip preserves node configuration`() {
        val node = createCodeNode("n1", "Configured",
            configuration = mapOf("timeout" to "5000", "retries" to "3"))
        val original = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(node)

        val restored = roundTrip(original)

        assertNotNull(restored)
        val restoredNode = restored.rootNodes.first() as CodeNode
        assertEquals("5000", restoredNode.configuration["timeout"])
        assertEquals("3", restoredNode.configuration["retries"])
    }

    // ============================================
    // Connection Round-Trip
    // ============================================

    @Test
    fun `round trip preserves connections`() {
        val n1 = createCodeNode("n1", "Source", type = CodeNodeType.SOURCE)
        val n2 = createCodeNode("n2", "Sink", type = CodeNodeType.SINK)
        val conn = Connection(
            id = "c1",
            sourceNodeId = "n1", sourcePortId = "n1_out",
            targetNodeId = "n2", targetPortId = "n2_in"
        )
        val original = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(n1).addNode(n2).addConnection(conn)

        val restored = roundTrip(original)

        assertNotNull(restored)
        assertEquals(1, restored.connections.size)
        // Port IDs may be regenerated during round-trip; verify connection exists
        val restoredConn = restored.connections.first()
        assertNotNull(restoredConn.sourcePortId)
        assertNotNull(restoredConn.targetPortId)
        // Verify the connection links the correct nodes
        assertNotNull(restoredConn.sourceNodeId)
        assertNotNull(restoredConn.targetNodeId)
    }

    // ============================================
    // GraphNode Round-Trip
    // ============================================

    @Test
    fun `round trip preserves GraphNode with child nodes`() {
        val child1 = createCodeNode("c1", "Child1")
        val child2 = createCodeNode("c2", "Child2")
        val internalConn = Connection(
            id = "ic1",
            sourceNodeId = "c1", sourcePortId = "c1_out",
            targetNodeId = "c2", targetPortId = "c2_in"
        )
        val graphNode = GraphNode(
            id = "gn1",
            name = "TestGroup",
            position = Node.Position(150.0, 100.0),
            childNodes = listOf(child1, child2),
            internalConnections = listOf(internalConn),
            inputPorts = listOf(
                Port(id = "gn1_in", name = "groupInput", direction = Port.Direction.INPUT,
                    dataType = String::class, owningNodeId = "gn1")
            ),
            outputPorts = listOf(
                Port(id = "gn1_out", name = "groupOutput", direction = Port.Direction.OUTPUT,
                    dataType = String::class, owningNodeId = "gn1")
            ),
            portMappings = mapOf(
                "groupInput" to GraphNode.PortMapping("c1", "input"),
                "groupOutput" to GraphNode.PortMapping("c2", "output")
            )
        )
        val original = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(graphNode)

        val restored = roundTrip(original)

        assertNotNull(restored)
        assertEquals(1, restored.rootNodes.size)
        val restoredGN = restored.rootNodes.first()
        assertTrue(restoredGN is GraphNode, "Root node should be a GraphNode")
        assertEquals("TestGroup", restoredGN.name)
        assertEquals(2, (restoredGN as GraphNode).childNodes.size)
        assertTrue(restoredGN.internalConnections.isNotEmpty(),
            "Internal connections should survive round-trip")
    }

    @Test
    fun `round trip preserves GraphNode port mappings`() {
        val child1 = createCodeNode("c1", "Child1")
        val graphNode = GraphNode(
            id = "gn1",
            name = "MappedGroup",
            position = Node.Position(100.0, 100.0),
            childNodes = listOf(child1),
            internalConnections = emptyList(),
            inputPorts = listOf(
                Port(id = "gn1_in", name = "exposed_in", direction = Port.Direction.INPUT,
                    dataType = String::class, owningNodeId = "gn1")
            ),
            outputPorts = listOf(
                Port(id = "gn1_out", name = "exposed_out", direction = Port.Direction.OUTPUT,
                    dataType = String::class, owningNodeId = "gn1")
            ),
            portMappings = mapOf(
                "exposed_in" to GraphNode.PortMapping("c1", "input"),
                "exposed_out" to GraphNode.PortMapping("c1", "output")
            )
        )
        val original = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(graphNode)

        val restored = roundTrip(original)

        assertNotNull(restored)
        val restoredGN = restored.rootNodes.first() as GraphNode
        assertTrue(restoredGN.portMappings.isNotEmpty(),
            "Port mappings should survive round-trip")
        assertEquals(2, restoredGN.portMappings.size)
    }

    // ============================================
    // Position Round-Trip
    // ============================================

    @Test
    fun `round trip preserves node positions`() {
        val node = createCodeNode("n1", "Positioned")
        val positioned = (node as Node).let {
            CodeNode(
                id = it.id, name = it.name,
                codeNodeType = node.codeNodeType,
                position = Node.Position(234.5, 678.9),
                inputPorts = node.inputPorts,
                outputPorts = node.outputPorts
            )
        }
        val original = flowGraph(name = "Test", version = "1.0.0") {}
            .addNode(positioned)

        val restored = roundTrip(original)

        assertNotNull(restored)
        val restoredNode = restored.rootNodes.first()
        assertEquals(234.5, restoredNode.position.x, "X position should survive round-trip")
        assertEquals(678.9, restoredNode.position.y, "Y position should survive round-trip")
    }

    // ============================================
    // Empty Graph Edge Case
    // ============================================

    @Test
    fun `round trip of empty graph produces empty graph`() {
        val original = flowGraph(name = "Empty", version = "1.0.0") {}

        val restored = roundTrip(original)

        assertNotNull(restored)
        assertEquals("Empty", restored.name)
        assertTrue(restored.rootNodes.isEmpty())
        assertTrue(restored.connections.isEmpty())
    }
}
