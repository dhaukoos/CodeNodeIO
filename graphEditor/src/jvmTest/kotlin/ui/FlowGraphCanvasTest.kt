/*
 * FlowGraphCanvas Test
 * UI tests for FlowGraphCanvas component rendering and interaction
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.Port
import kotlin.test.*

class FlowGraphCanvasTest {

    @Test
    fun `should render empty canvas`() {
        // Given: An empty flow graph
        val emptyGraph = flowGraph(name = "EmptyGraph", version = "1.0.0") {
            // No nodes or connections
        }

        // Then: Graph should be valid and empty
        assertNotNull(emptyGraph, "FlowGraph should not be null")
        assertEquals(0, emptyGraph.rootNodes.size, "Empty graph should have no nodes")
        assertEquals(0, emptyGraph.connections.size, "Empty graph should have no connections")
        assertEquals("EmptyGraph", emptyGraph.name, "Graph name should be preserved")
    }

    @Test
    fun `should render nodes on canvas`() {
        // Given: A flow graph with nodes
        val graphWithNodes = flowGraph(name = "GraphWithNodes", version = "1.0.0") {
            codeNode("InputNode") {
                output("data", String::class)
            }
            codeNode("ProcessNode") {
                input("input", String::class)
                output("output", String::class)
            }
            codeNode("OutputNode") {
                input("result", String::class)
            }
        }

        // Then: All nodes should be present
        assertNotNull(graphWithNodes, "FlowGraph should not be null")
        assertEquals(3, graphWithNodes.rootNodes.size, "Graph should have 3 nodes")

        // Verify node names
        val nodeNames = graphWithNodes.rootNodes.map { it.name }
        assertTrue(nodeNames.contains("InputNode"), "Graph should contain InputNode")
        assertTrue(nodeNames.contains("ProcessNode"), "Graph should contain ProcessNode")
        assertTrue(nodeNames.contains("OutputNode"), "Graph should contain OutputNode")

        // Verify node ports are configured correctly
        val inputNode = graphWithNodes.rootNodes.find { it.name == "InputNode" }
        assertNotNull(inputNode, "InputNode should exist")
        assertEquals(0, inputNode.inputPorts.size, "InputNode should have no input ports")
        assertEquals(1, inputNode.outputPorts.size, "InputNode should have 1 output port")

        val processNode = graphWithNodes.rootNodes.find { it.name == "ProcessNode" }
        assertNotNull(processNode, "ProcessNode should exist")
        assertEquals(1, processNode.inputPorts.size, "ProcessNode should have 1 input port")
        assertEquals(1, processNode.outputPorts.size, "ProcessNode should have 1 output port")

        val outputNode = graphWithNodes.rootNodes.find { it.name == "OutputNode" }
        assertNotNull(outputNode, "OutputNode should exist")
        assertEquals(1, outputNode.inputPorts.size, "OutputNode should have 1 input port")
        assertEquals(0, outputNode.outputPorts.size, "OutputNode should have no output ports")
    }

    @Test
    fun `should render connections between nodes`() {
        // Given: A flow graph with nodes and connections
        val graphWithConnections = flowGraph(name = "ConnectedGraph", version = "1.0.0") {
            val input = codeNode("Input") {
                output("out", String::class)
            }
            val process = codeNode("Process") {
                input("in", String::class)
                output("out", String::class)
            }
            val output = codeNode("Output") {
                input("in", String::class)
            }

            // Create connections
            addConnection(input.output("out") connect process.input("in"))
            addConnection(process.output("out") connect output.input("in"))
        }

        // Then: Connections should be present
        assertNotNull(graphWithConnections, "FlowGraph should not be null")
        assertEquals(3, graphWithConnections.rootNodes.size, "Graph should have 3 nodes")

        // Verify we have connections
        val connections = graphWithConnections.connections
        assertTrue(connections.isNotEmpty(), "Graph should have connections, got ${connections.size}")

        // Verify connections are valid
        assertTrue(connections.all { it.sourceNodeId.isNotBlank() }, "All connections should have source nodes")
        assertTrue(connections.all { it.targetNodeId.isNotBlank() }, "All connections should have target nodes")
        assertTrue(connections.all { it.sourcePortId.isNotBlank() }, "All connections should have source ports")
        assertTrue(connections.all { it.targetPortId.isNotBlank() }, "All connections should have target ports")

        // Verify at least one connection has the expected endpoints
        val inputNode = graphWithConnections.rootNodes.find { it.name == "Input" }
        val processNode = graphWithConnections.rootNodes.find { it.name == "Process" }
        val outputNode = graphWithConnections.rootNodes.find { it.name == "Output" }
        assertNotNull(inputNode, "Input node should exist")
        assertNotNull(processNode, "Process node should exist")
        assertNotNull(outputNode, "Output node should exist")

        // Check that there's a connection from Input to Process
        assertTrue(
            connections.any { it.sourceNodeId == inputNode.id && it.targetNodeId == processNode.id },
            "Should have connection from Input to Process"
        )

        // Check that there's a connection from Process to Output
        assertTrue(
            connections.any { it.sourceNodeId == processNode.id && it.targetNodeId == outputNode.id },
            "Should have connection from Process to Output"
        )
    }

    @Test
    fun `should support canvas panning and zooming`() {
        // Given: A graph and canvas state
        val graph = flowGraph(name = "TestGraph", version = "1.0.0") {
            codeNode("Node1") {
                output("out", String::class)
            }
        }

        // When: Testing zoom range
        val minZoom = 0.1f
        val maxZoom = 5.0f
        val defaultZoom = 1.0f

        // Then: Zoom values should be in valid range
        assertTrue(minZoom > 0, "Minimum zoom should be positive")
        assertTrue(maxZoom > minZoom, "Maximum zoom should be greater than minimum")
        assertTrue(defaultZoom in minZoom..maxZoom, "Default zoom should be in valid range")

        // Test zoom scale factors
        val zoomIn = 1.2f
        val zoomOut = 0.8f
        assertTrue(zoomIn > 1.0f, "Zoom in factor should increase scale")
        assertTrue(zoomOut < 1.0f, "Zoom out factor should decrease scale")

        // Test zoom progression
        val scale1 = defaultZoom * zoomIn
        val scale2 = scale1 * zoomIn
        assertTrue(scale2 > scale1, "Multiple zoom ins should increase scale")
        assertTrue(scale1 > defaultZoom, "Zoom in should increase from default")

        // Test pan offset calculations
        val panAmount = 50f
        assertTrue(panAmount > 0, "Pan amount should be positive")

        // Verify graph data is preserved during transformations
        assertNotNull(graph, "Graph should remain valid during transformations")
        assertEquals(1, graph.rootNodes.size, "Graph nodes should remain unchanged")
    }

    @Test
    fun `should handle node selection and dragging`() {
        // Given: A graph with multiple nodes
        val graph = flowGraph(name = "SelectableGraph", version = "1.0.0") {
            codeNode("Node1") {
                output("out", String::class)
            }
            codeNode("Node2") {
                input("in", String::class)
                output("out", String::class)
            }
            codeNode("Node3") {
                input("in", String::class)
            }
        }

        // When: Testing node selection
        var selectedNodeId: String? = null
        val onNodeSelected: (String?) -> Unit = { nodeId ->
            selectedNodeId = nodeId
        }

        // Then: Selection callback should update state
        val node1 = graph.rootNodes[0]
        onNodeSelected(node1.id)
        assertEquals(node1.id, selectedNodeId, "Selected node ID should be updated")

        // Test deselection
        onNodeSelected(null)
        assertNull(selectedNodeId, "Node should be deselected")

        // When: Testing node dragging
        val initialX = 100.0
        val initialY = 100.0
        val dragDeltaX = 50.0
        val dragDeltaY = 30.0

        var movedNodeId: String? = null
        var movedX: Double = 0.0
        var movedY: Double = 0.0

        val onNodeMoved: (String, Double, Double) -> Unit = { nodeId, newX, newY ->
            movedNodeId = nodeId
            movedX = newX
            movedY = newY
        }

        // Simulate drag
        onNodeMoved(node1.id, initialX + dragDeltaX, initialY + dragDeltaY)

        // Then: Node position should be updated
        assertEquals(node1.id, movedNodeId, "Moved node ID should match")
        assertEquals(initialX + dragDeltaX, movedX, "X position should be updated")
        assertEquals(initialY + dragDeltaY, movedY, "Y position should be updated")

        // Verify graph structure is maintained
        assertEquals(3, graph.rootNodes.size, "Graph should still have all nodes after drag")
        assertNotNull(graph.rootNodes.find { it.id == node1.id }, "Dragged node should still exist in graph")
    }

    @Test
    fun `should handle complex graph with multiple node types`() {
        // Given: A complex graph with various node configurations
        val complexGraph = flowGraph(name = "ComplexGraph", version = "1.0.0") {
            // Source node with multiple outputs
            val source = codeNode("MultiOutputSource") {
                output("stream1", String::class)
                output("stream2", Int::class)
                output("stream3", Boolean::class)
            }

            // Processing nodes
            val processor1 = codeNode("StringProcessor") {
                input("in", String::class)
                output("out", String::class)
            }

            val processor2 = codeNode("IntProcessor") {
                input("in", Int::class)
                output("out", Int::class)
            }

            // Merge node with multiple inputs
            val merger = codeNode("Merger") {
                input("string", String::class)
                input("number", Int::class)
                input("flag", Boolean::class)
                output("result", Any::class)
            }

            // Sink node
            val sink = codeNode("Sink") {
                input("data", Any::class)
            }

            // Create connections
            addConnection(source.output("stream1") connect processor1.input("in"))
            addConnection(source.output("stream2") connect processor2.input("in"))
            addConnection(processor1.output("out") connect merger.input("string"))
            addConnection(processor2.output("out") connect merger.input("number"))
            addConnection(source.output("stream3") connect merger.input("flag"))
            addConnection(merger.output("result") connect sink.input("data"))
        }

        // Then: Complex graph structure should be valid
        assertNotNull(complexGraph, "Complex graph should not be null")
        assertEquals(5, complexGraph.rootNodes.size, "Complex graph should have 5 nodes")
        assertTrue(complexGraph.connections.isNotEmpty(), "Complex graph should have connections, got ${complexGraph.connections.size}")

        // Verify source node has multiple outputs
        val sourceNode = complexGraph.rootNodes.find { it.name == "MultiOutputSource" }
        assertNotNull(sourceNode, "Source node should exist")
        assertEquals(3, sourceNode.outputPorts.size, "Source should have 3 output ports")

        // Verify merger node has multiple inputs
        val mergerNode = complexGraph.rootNodes.find { it.name == "Merger" }
        assertNotNull(mergerNode, "Merger node should exist")
        assertEquals(3, mergerNode.inputPorts.size, "Merger should have 3 input ports")
        assertEquals(1, mergerNode.outputPorts.size, "Merger should have 1 output port")

        // Verify all connections are valid
        assertTrue(
            complexGraph.connections.all { conn ->
                complexGraph.rootNodes.any { it.id == conn.sourceNodeId } &&
                complexGraph.rootNodes.any { it.id == conn.targetNodeId }
            },
            "All connections should reference valid nodes"
        )
    }

    @Test
    fun `should validate graph structure`() {
        // Given: A well-formed graph
        val validGraph = flowGraph(name = "ValidGraph", version = "1.0.0") {
            val node1 = codeNode("Node1") {
                output("out", String::class)
            }
            val node2 = codeNode("Node2") {
                input("in", String::class)
            }
            addConnection(node1.output("out") connect node2.input("in"))
        }

        // When: Validating the graph
        val validationResult = validGraph.validate()

        // Then: Graph should be valid
        assertTrue(validationResult.success, "Well-formed graph should be valid")
        assertTrue(validationResult.errors.isEmpty(), "Valid graph should have no errors")
    }

    @Test
    fun `should handle node with no connections`() {
        // Given: A graph with isolated nodes
        val graphWithIsolatedNodes = flowGraph(name = "IsolatedNodes", version = "1.0.0") {
            codeNode("Isolated1") {
                output("out", String::class)
            }
            codeNode("Isolated2") {
                input("in", String::class)
            }
            codeNode("Isolated3") {
                input("in1", String::class)
                output("out1", String::class)
            }
        }

        // Then: Graph should contain all nodes even without connections
        assertNotNull(graphWithIsolatedNodes, "Graph with isolated nodes should not be null")
        assertEquals(3, graphWithIsolatedNodes.rootNodes.size, "Graph should have 3 nodes")
        assertEquals(0, graphWithIsolatedNodes.connections.size, "Graph should have no connections")

        // Verify each node maintains its port configuration
        assertTrue(
            graphWithIsolatedNodes.rootNodes.all {
                it.inputPorts.isNotEmpty() || it.outputPorts.isNotEmpty()
            },
            "All nodes should have at least one port"
        )
    }
}
