/*
 * FlowGraphFactoryGenerator Test
 * TDD tests for factory function generation from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD tests for FlowGraphFactoryGenerator - verifies factory function generation from FlowGraph.
 *
 * These tests are written FIRST and should FAIL until FlowGraphFactoryGenerator is implemented.
 *
 * The generator creates a top-level factory function like:
 * ```kotlin
 * fun createStopWatchFlowGraph(): FlowGraph {
 *     val timerEmitter = CodeNode(...)
 *     val displayReceiver = CodeNode(...)
 *     return FlowGraph(
 *         rootNodes = listOf(timerEmitter, displayReceiver),
 *         connections = listOf(...)
 *     )
 * }
 * ```
 */
class FlowGraphFactoryGeneratorTest {

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
            description = "Test flow for factory generation",
            rootNodes = nodes,
            connections = connections,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }

    // ========== T035: Factory Function Signature Tests ==========

    @Test
    fun `should generate factory function with correct name pattern`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val generator = FlowGraphFactoryGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, packageName)

        // Then
        assertTrue(
            generatedCode.contains("fun createStopWatchFlowGraph()") ||
            generatedCode.contains("fun createStopWatchFlowGraph("),
            "Should generate function named createStopWatchFlowGraph, got: ${generatedCode.take(500)}"
        )
    }

    @Test
    fun `should generate factory function returning FlowGraph type`() {
        // Given
        val flowGraph = createTestFlowGraph("UserValidation")
        val generator = FlowGraphFactoryGenerator()
        val packageName = "io.codenode.generated.uservalidation"

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, packageName)

        // Then
        assertTrue(
            generatedCode.contains(": FlowGraph") || generatedCode.contains("return FlowGraph("),
            "Should return FlowGraph type, got: ${generatedCode.take(500)}"
        )
    }

    @Test
    fun `should generate public visibility for factory function`() {
        // Given
        val flowGraph = createTestFlowGraph("PublicFlow")
        val generator = FlowGraphFactoryGenerator()
        val packageName = "io.codenode.generated"

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, packageName)

        // Then
        // Public is default in Kotlin, so either explicit "public" or no visibility modifier is valid
        // Just verify it's a fun declaration at top level (not private/internal)
        assertFalse(
            generatedCode.contains("private fun create"),
            "Factory function should not be private"
        )
        assertFalse(
            generatedCode.contains("internal fun create"),
            "Factory function should not be internal"
        )
    }

    // ========== T036: CodeNode Creation Tests ==========

    @Test
    fun `should generate CodeNode creation for each node in graph`() {
        // Given
        val timerEmitter = createTestCodeNode(
            id = "timer-emitter",
            name = "TimerEmitter",
            type = CodeNodeType.GENERATOR
        )
        val displayReceiver = createTestCodeNode(
            id = "display-receiver",
            name = "DisplayReceiver",
            type = CodeNodeType.SINK
        )
        val flowGraph = createTestFlowGraph(
            name = "StopWatch",
            nodes = listOf(timerEmitter, displayReceiver)
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("timerEmitter") || generatedCode.contains("timer_emitter"),
            "Should create timerEmitter node variable"
        )
        assertTrue(
            generatedCode.contains("displayReceiver") || generatedCode.contains("display_receiver"),
            "Should create displayReceiver node variable"
        )
    }

    @Test
    fun `should generate correct node ID in CodeNode creation`() {
        // Given
        val node = createTestCodeNode(id = "my-unique-node-id", name = "TestNode")
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("\"my-unique-node-id\"") || generatedCode.contains("my-unique-node-id"),
            "Should include exact node ID in generated code"
        )
    }

    @Test
    fun `should generate correct node name in CodeNode creation`() {
        // Given
        val node = createTestCodeNode(id = "node1", name = "DataProcessor")
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("\"DataProcessor\"") || generatedCode.contains("DataProcessor"),
            "Should include node name in generated code"
        )
    }

    @Test
    fun `should generate correct codeNodeType in CodeNode creation`() {
        // Given
        val generatorNode = createTestCodeNode(
            id = "gen1",
            name = "Source",
            type = CodeNodeType.GENERATOR
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(generatorNode))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("CodeNodeType.GENERATOR") || generatedCode.contains("GENERATOR"),
            "Should include codeNodeType in generated code"
        )
    }

    @Test
    fun `should generate node position in CodeNode creation`() {
        // Given
        val node = createTestCodeNode(id = "node1", name = "Positioned")
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("position") || generatedCode.contains("Position"),
            "Should include position in generated code"
        )
    }

    // ========== T037: ProcessingLogic Instantiation Tests ==========

    @Test
    fun `should generate ProcessingLogic instantiation from configuration`() {
        // Given
        val node = createTestCodeNode(
            id = "timer-emitter",
            name = "TimerEmitter",
            configuration = mapOf(
                "_useCaseClass" to "demos/stopwatch/TimerEmitterComponent.kt"
            )
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("TimerEmitterComponent()") ||
            generatedCode.contains("TimerEmitterComponent"),
            "Should instantiate TimerEmitterComponent from _useCaseClass config"
        )
    }

    @Test
    fun `should assign ProcessingLogic to node processingLogic property`() {
        // Given
        val node = createTestCodeNode(
            id = "display",
            name = "DisplayReceiver",
            configuration = mapOf(
                "_useCaseClass" to "src/DisplayReceiverComponent.kt"
            )
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("processingLogic") ||
            generatedCode.contains("DisplayReceiverComponent"),
            "Should assign processingLogic property with instantiated component"
        )
    }

    @Test
    fun `should extract class name from file path correctly`() {
        // Given
        val node = createTestCodeNode(
            id = "node1",
            name = "TestNode",
            configuration = mapOf(
                "_useCaseClass" to "path/to/deep/folder/MyCustomComponent.kt"
            )
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("MyCustomComponent"),
            "Should extract MyCustomComponent class name from file path"
        )
    }

    @Test
    fun `should handle nodes without ProcessingLogic configuration`() {
        // Given - node without _useCaseClass configuration
        val node = createTestCodeNode(
            id = "node1",
            name = "SimpleNode",
            configuration = emptyMap()
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        // Should still generate valid code (processingLogic = null or omitted)
        assertTrue(
            generatedCode.contains("SimpleNode") || generatedCode.contains("simpleNode"),
            "Should generate node even without processingLogic"
        )
        assertFalse(
            generatedCode.contains("processingLogic = null") &&
            generatedCode.contains("// ERROR"),
            "Should not generate error comments for missing processingLogic"
        )
    }

    // ========== T038: Port Definition Tests ==========

    @Test
    fun `should generate inputPorts list for each CodeNode`() {
        // Given
        val node = createTestCodeNode(
            id = "merger",
            name = "DataMerger",
            inputPorts = listOf(
                Port(
                    id = "merger_input1",
                    name = "input1",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "merger"
                ),
                Port(
                    id = "merger_input2",
                    name = "input2",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = "merger"
                )
            )
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("inputPorts") || generatedCode.contains("input1"),
            "Should include inputPorts list"
        )
    }

    @Test
    fun `should generate outputPorts list for each CodeNode`() {
        // Given
        val node = createTestCodeNode(
            id = "splitter",
            name = "DataSplitter",
            outputPorts = listOf(
                Port(
                    id = "splitter_output1",
                    name = "output1",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "splitter"
                ),
                Port(
                    id = "splitter_output2",
                    name = "output2",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = "splitter"
                )
            )
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("outputPorts") || generatedCode.contains("output1"),
            "Should include outputPorts list"
        )
    }

    @Test
    fun `should generate Port with correct properties`() {
        // Given
        val node = createTestCodeNode(
            id = "node1",
            name = "TestNode",
            inputPorts = listOf(
                Port(
                    id = "node1_data",
                    name = "data",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "node1"
                )
            )
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("Port") || generatedCode.contains("port"),
            "Should generate Port class usage"
        )
        // Port should have id, name, direction, dataType, owningNodeId
        assertTrue(
            generatedCode.contains("node1_data") || generatedCode.contains("\"data\""),
            "Should include port ID or name"
        )
    }

    @Test
    fun `should generate Port direction correctly`() {
        // Given
        val node = createTestCodeNode(
            id = "node1",
            name = "TestNode",
            inputPorts = listOf(
                Port(
                    id = "node1_in",
                    name = "in",
                    direction = Port.Direction.INPUT,
                    dataType = Any::class,
                    owningNodeId = "node1"
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "node1_out",
                    name = "out",
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    owningNodeId = "node1"
                )
            )
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("Direction.INPUT") || generatedCode.contains("INPUT"),
            "Should include INPUT direction"
        )
        assertTrue(
            generatedCode.contains("Direction.OUTPUT") || generatedCode.contains("OUTPUT"),
            "Should include OUTPUT direction"
        )
    }

    // ========== T039: Connection Definition Tests ==========

    @Test
    fun `should generate connections list in FlowGraph return`() {
        // Given
        val source = createTestCodeNode(id = "source", name = "Source")
        val sink = createTestCodeNode(id = "sink", name = "Sink")
        val connection = Connection(
            id = "conn1",
            sourceNodeId = "source",
            sourcePortId = "source_output",
            targetNodeId = "sink",
            targetPortId = "sink_input"
        )
        val flowGraph = createTestFlowGraph(
            nodes = listOf(source, sink),
            connections = listOf(connection)
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("connections") || generatedCode.contains("Connection"),
            "Should include connections in FlowGraph return"
        )
    }

    @Test
    fun `should generate Connection with correct source and target`() {
        // Given
        val source = createTestCodeNode(id = "timer", name = "Timer")
        val sink = createTestCodeNode(id = "display", name = "Display")
        val connection = Connection(
            id = "timer_to_display",
            sourceNodeId = "timer",
            sourcePortId = "timer_output",
            targetNodeId = "display",
            targetPortId = "display_input"
        )
        val flowGraph = createTestFlowGraph(
            nodes = listOf(source, sink),
            connections = listOf(connection)
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("sourceNodeId") || generatedCode.contains("\"timer\""),
            "Should include sourceNodeId"
        )
        assertTrue(
            generatedCode.contains("targetNodeId") || generatedCode.contains("\"display\""),
            "Should include targetNodeId"
        )
    }

    @Test
    fun `should generate Connection with port IDs`() {
        // Given
        val source = createTestCodeNode(id = "source", name = "Source")
        val sink = createTestCodeNode(id = "sink", name = "Sink")
        val connection = Connection(
            id = "data_flow",
            sourceNodeId = "source",
            sourcePortId = "source_data_out",
            targetNodeId = "sink",
            targetPortId = "sink_data_in"
        )
        val flowGraph = createTestFlowGraph(
            nodes = listOf(source, sink),
            connections = listOf(connection)
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("sourcePortId") || generatedCode.contains("source_data_out"),
            "Should include sourcePortId"
        )
        assertTrue(
            generatedCode.contains("targetPortId") || generatedCode.contains("sink_data_in"),
            "Should include targetPortId"
        )
    }

    @Test
    fun `should generate multiple connections when present`() {
        // Given
        val timer = createTestCodeNode(
            id = "timer",
            name = "Timer",
            outputPorts = listOf(
                Port(
                    id = "timer_seconds",
                    name = "seconds",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = "timer"
                ),
                Port(
                    id = "timer_minutes",
                    name = "minutes",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = "timer"
                )
            )
        )
        val display = createTestCodeNode(
            id = "display",
            name = "Display",
            inputPorts = listOf(
                Port(
                    id = "display_sec",
                    name = "seconds",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = "display"
                ),
                Port(
                    id = "display_min",
                    name = "minutes",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = "display"
                )
            )
        )
        val connections = listOf(
            Connection(
                id = "conn_seconds",
                sourceNodeId = "timer",
                sourcePortId = "timer_seconds",
                targetNodeId = "display",
                targetPortId = "display_sec"
            ),
            Connection(
                id = "conn_minutes",
                sourceNodeId = "timer",
                sourcePortId = "timer_minutes",
                targetNodeId = "display",
                targetPortId = "display_min"
            )
        )
        val flowGraph = createTestFlowGraph(
            nodes = listOf(timer, display),
            connections = connections
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        // Should contain both connection IDs or both port references
        assertTrue(
            (generatedCode.contains("conn_seconds") || generatedCode.contains("timer_seconds")) &&
            (generatedCode.contains("conn_minutes") || generatedCode.contains("timer_minutes")),
            "Should include all connections"
        )
    }

    @Test
    fun `should handle empty connections list`() {
        // Given
        val node = createTestCodeNode(id = "standalone", name = "Standalone")
        val flowGraph = createTestFlowGraph(
            nodes = listOf(node),
            connections = emptyList()
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated")

        // Then
        assertTrue(
            generatedCode.contains("connections = emptyList()") ||
            generatedCode.contains("connections = listOf()") ||
            generatedCode.contains("FlowGraph"),
            "Should generate valid FlowGraph even with no connections"
        )
    }

    // ========== Integration Tests ==========

    @Test
    fun `should generate complete factory function for StopWatch example`() {
        // Given - Simulating the StopWatch virtual circuit
        val timerEmitter = CodeNode(
            id = "timer-emitter",
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "timer-emitter_seconds",
                    name = "elapsedSeconds",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = "timer-emitter"
                ),
                Port(
                    id = "timer-emitter_minutes",
                    name = "elapsedMinutes",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = "timer-emitter"
                )
            ),
            configuration = mapOf(
                "_useCaseClass" to "demos/stopwatch/TimerEmitterComponent.kt",
                "speedAttenuation" to "1000"
            )
        )
        val displayReceiver = CodeNode(
            id = "display-receiver",
            name = "DisplayReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                Port(
                    id = "display-receiver_seconds",
                    name = "seconds",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = "display-receiver"
                ),
                Port(
                    id = "display-receiver_minutes",
                    name = "minutes",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = "display-receiver"
                )
            ),
            outputPorts = emptyList(),
            configuration = mapOf(
                "_useCaseClass" to "demos/stopwatch/DisplayReceiverComponent.kt"
            )
        )
        val connections = listOf(
            Connection(
                id = "conn_seconds",
                sourceNodeId = "timer-emitter",
                sourcePortId = "timer-emitter_seconds",
                targetNodeId = "display-receiver",
                targetPortId = "display-receiver_seconds"
            ),
            Connection(
                id = "conn_minutes",
                sourceNodeId = "timer-emitter",
                sourcePortId = "timer-emitter_minutes",
                targetNodeId = "display-receiver",
                targetPortId = "display-receiver_minutes"
            )
        )
        val flowGraph = FlowGraph(
            id = "stopwatch-flow",
            name = "StopWatch",
            version = "1.0.0",
            description = "Virtual circuit demo for stopwatch functionality",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = connections,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
        val generator = FlowGraphFactoryGenerator()

        // When
        val generatedCode = generator.generateFactoryFunction(flowGraph, "io.codenode.generated.stopwatch")

        // Then
        assertTrue(
            generatedCode.contains("createStopWatchFlowGraph"),
            "Should name function createStopWatchFlowGraph"
        )
        assertTrue(
            generatedCode.contains("TimerEmitter") && generatedCode.contains("DisplayReceiver"),
            "Should include both node names"
        )
        assertTrue(
            generatedCode.contains("TimerEmitterComponent") && generatedCode.contains("DisplayReceiverComponent"),
            "Should instantiate both ProcessingLogic components"
        )
        assertTrue(
            generatedCode.contains("FlowGraph"),
            "Should return FlowGraph"
        )
    }

    // ========== T049: ProcessingLogic Import Generation Tests ==========

    @Test
    fun `should collect ProcessingLogic class names from nodes`() {
        // Given
        val node1 = createTestCodeNode(
            id = "node1",
            name = "Node1",
            configuration = mapOf("_useCaseClass" to "src/Component1.kt")
        )
        val node2 = createTestCodeNode(
            id = "node2",
            name = "Node2",
            configuration = mapOf("_useCaseClass" to "src/Component2.kt")
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node1, node2))
        val generator = FlowGraphFactoryGenerator()

        // When
        val classNames = generator.collectProcessingLogicClassNames(flowGraph)

        // Then
        assertEquals(2, classNames.size, "Should collect 2 class names")
        assertTrue(classNames.contains("Component1"), "Should include Component1")
        assertTrue(classNames.contains("Component2"), "Should include Component2")
    }

    @Test
    fun `should remove duplicates from collected class names`() {
        // Given - two nodes with the same ProcessingLogic class
        val node1 = createTestCodeNode(
            id = "node1",
            name = "Node1",
            configuration = mapOf("_useCaseClass" to "src/SharedComponent.kt")
        )
        val node2 = createTestCodeNode(
            id = "node2",
            name = "Node2",
            configuration = mapOf("_useCaseClass" to "src/SharedComponent.kt")
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node1, node2))
        val generator = FlowGraphFactoryGenerator()

        // When
        val classNames = generator.collectProcessingLogicClassNames(flowGraph)

        // Then
        assertEquals(1, classNames.size, "Should deduplicate class names")
        assertEquals("SharedComponent", classNames.first())
    }

    @Test
    fun `should generate import statements from file paths`() {
        // Given
        val node = createTestCodeNode(
            id = "timer",
            name = "Timer",
            configuration = mapOf(
                "_useCaseClass" to "demos/stopwatch/TimerEmitterComponent.kt"
            )
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(node))
        val generator = FlowGraphFactoryGenerator()

        // When
        val imports = generator.generateProcessingLogicImports(flowGraph)

        // Then
        assertEquals(1, imports.size, "Should generate 1 import")
        assertEquals(
            "demos.stopwatch.TimerEmitterComponent",
            imports.first(),
            "Should convert path to package notation"
        )
    }

    @Test
    fun `should derive import from path with directory structure`() {
        // Given
        val generator = FlowGraphFactoryGenerator()

        // When
        val import = generator.deriveImportFromPath(
            "path/to/deep/folder/MyComponent.kt",
            "io.codenode.generated"
        )

        // Then
        assertEquals(
            "path.to.deep.folder.MyComponent",
            import,
            "Should convert directory path to package"
        )
    }

    @Test
    fun `should use default package for filename-only paths`() {
        // Given
        val generator = FlowGraphFactoryGenerator()

        // When
        val import = generator.deriveImportFromPath(
            "SimpleComponent.kt",
            "io.codenode.mypackage"
        )

        // Then
        assertEquals(
            "io.codenode.mypackage.SimpleComponent",
            import,
            "Should use default package for simple filenames"
        )
    }

    @Test
    fun `should skip nodes without useCaseClass when generating imports`() {
        // Given
        val nodeWithConfig = createTestCodeNode(
            id = "node1",
            name = "Node1",
            configuration = mapOf("_useCaseClass" to "src/HasComponent.kt")
        )
        val nodeWithoutConfig = createTestCodeNode(
            id = "node2",
            name = "Node2",
            configuration = emptyMap()
        )
        val flowGraph = createTestFlowGraph(nodes = listOf(nodeWithConfig, nodeWithoutConfig))
        val generator = FlowGraphFactoryGenerator()

        // When
        val imports = generator.generateProcessingLogicImports(flowGraph)

        // Then
        assertEquals(1, imports.size, "Should only generate import for node with config")
        assertTrue(imports.first().contains("HasComponent"))
    }

    @Test
    fun `should generate raw factory function for embedding`() {
        // Given
        val flowGraph = createTestFlowGraph("TestFlow")
        val generator = FlowGraphFactoryGenerator()

        // When
        val rawFunction = generator.generateFactoryFunctionRaw(flowGraph)

        // Then
        assertTrue(
            rawFunction.contains("fun createTestFlowFlowGraph()"),
            "Raw function should contain function declaration"
        )
        assertTrue(
            rawFunction.contains("FlowGraph"),
            "Raw function should reference FlowGraph"
        )
        // Should NOT contain package declaration (that's for embedding)
        assertFalse(
            rawFunction.contains("package "),
            "Raw function should not contain package declaration"
        )
    }
}
