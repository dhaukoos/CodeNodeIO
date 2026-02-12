/*
 * RequiredPropertyValidator Test
 * Tests for required property validation before compilation
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compilation

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for RequiredPropertyValidator - validates required properties on CodeNodes.
 *
 * T005-T010: Tests for compile-time required property validation
 */
class RequiredPropertyValidatorTest {

    // ========== Test Fixtures ==========

    private lateinit var validator: RequiredPropertyValidator

    @BeforeTest
    fun setUp() {
        validator = RequiredPropertyValidator()
    }

    private fun createGenericNode(
        id: String,
        name: String,
        useCaseClass: String? = null,
        genericType: String? = null,
        additionalConfig: Map<String, String> = emptyMap()
    ): CodeNode {
        val config = mutableMapOf<String, String>()
        if (useCaseClass != null) {
            config["_useCaseClass"] = useCaseClass
        }
        if (genericType != null) {
            config["_genericType"] = genericType
        }
        config.putAll(additionalConfig)

        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.GENERIC,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            configuration = config
        )
    }

    private fun createNonGenericNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(100.0, 200.0),
            inputPorts = listOf(
                Port(
                    id = "${id}_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            configuration = emptyMap()
        )
    }

    private fun createTestFlowGraph(
        name: String = "TestFlow",
        nodes: List<Node> = emptyList()
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            rootNodes = nodes
        )
    }

    // ========== T005: validate returns success when all required properties present ==========

    @Test
    fun `T005 - validate returns success when all required properties present`() {
        // Given - A GENERIC node with both _useCaseClass and _genericType defined
        val node = createGenericNode(
            id = "timer",
            name = "TimerEmitter",
            useCaseClass = "io.codenode.stopwatch.usecases.TimerEmitterComponent",
            genericType = "in0out2"
        )
        val flowGraph = createTestFlowGraph("StopWatch", listOf(node))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertTrue(result.success, "Validation should succeed when all required properties present")
        assertTrue(result.errors.isEmpty(), "No errors expected")
        assertEquals("", result.toErrorMessage(), "Error message should be empty")
    }

    // ========== T006: validate returns error for GENERIC node missing _useCaseClass ==========

    @Test
    fun `T006 - validate returns error for GENERIC node missing _useCaseClass`() {
        // Given - A GENERIC node with _genericType but missing _useCaseClass
        val node = createGenericNode(
            id = "timer",
            name = "TimerEmitter",
            useCaseClass = null,  // Missing!
            genericType = "in0out2"
        )
        val flowGraph = createTestFlowGraph("StopWatch", listOf(node))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertFalse(result.success, "Validation should fail when _useCaseClass missing")
        assertEquals(1, result.errors.size, "Should have one error")
        assertEquals("timer", result.errors[0].nodeId)
        assertEquals("TimerEmitter", result.errors[0].nodeName)
        assertEquals("_useCaseClass", result.errors[0].propertyName)
        assertTrue(
            result.toErrorMessage().contains("TimerEmitter") &&
            result.toErrorMessage().contains("_useCaseClass"),
            "Error message should mention node name and missing property"
        )
    }

    // ========== T007: validate returns error for GENERIC node missing _genericType ==========

    @Test
    fun `T007 - validate returns error for GENERIC node missing _genericType`() {
        // Given - A GENERIC node with _useCaseClass but missing _genericType
        val node = createGenericNode(
            id = "timer",
            name = "TimerEmitter",
            useCaseClass = "io.codenode.stopwatch.usecases.TimerEmitterComponent",
            genericType = null  // Missing!
        )
        val flowGraph = createTestFlowGraph("StopWatch", listOf(node))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertFalse(result.success, "Validation should fail when _genericType missing")
        assertEquals(1, result.errors.size, "Should have one error")
        assertEquals("_genericType", result.errors[0].propertyName)
        assertTrue(
            result.toErrorMessage().contains("_genericType"),
            "Error message should mention missing property"
        )
    }

    // ========== T008: validate returns multiple errors for multiple nodes with missing properties ==========

    @Test
    fun `T008 - validate returns multiple errors for multiple nodes with missing properties`() {
        // Given - Two GENERIC nodes, both missing _useCaseClass
        val timerNode = createGenericNode(
            id = "timer",
            name = "TimerEmitter",
            useCaseClass = null,  // Missing
            genericType = "in0out2"
        )
        val displayNode = createGenericNode(
            id = "display",
            name = "DisplayReceiver",
            useCaseClass = null,  // Missing
            genericType = "in2out0"
        )
        val flowGraph = createTestFlowGraph("StopWatch", listOf(timerNode, displayNode))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertFalse(result.success, "Validation should fail")
        assertEquals(2, result.errors.size, "Should have errors for both nodes")

        val nodeIds = result.errors.map { it.nodeId }.toSet()
        assertTrue(nodeIds.contains("timer"), "Should report error for timer node")
        assertTrue(nodeIds.contains("display"), "Should report error for display node")

        val errorMessage = result.toErrorMessage()
        assertTrue(errorMessage.contains("TimerEmitter"), "Error message should mention TimerEmitter")
        assertTrue(errorMessage.contains("DisplayReceiver"), "Error message should mention DisplayReceiver")
    }

    // ========== T009: validate ignores non-GENERIC node types ==========

    @Test
    fun `T009 - validate ignores non-GENERIC node types`() {
        // Given - Non-GENERIC nodes without _useCaseClass (should be allowed)
        val transformerNode = createNonGenericNode("transform", "DataTransformer", CodeNodeType.TRANSFORMER)
        val apiNode = createNonGenericNode("api", "APIEndpoint", CodeNodeType.API_ENDPOINT)
        val dbNode = createNonGenericNode("db", "DatabaseQuery", CodeNodeType.DATABASE)

        val flowGraph = createTestFlowGraph("MixedFlow", listOf(transformerNode, apiNode, dbNode))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertTrue(result.success, "Validation should succeed for non-GENERIC nodes without required properties")
        assertTrue(result.errors.isEmpty(), "No errors expected for non-GENERIC nodes")
    }

    // ========== T010: validate returns success for empty graph ==========

    @Test
    fun `T010 - validate returns success for empty graph`() {
        // Given - An empty flow graph with no nodes
        val flowGraph = createTestFlowGraph("EmptyFlow", emptyList())

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertTrue(result.success, "Validation should succeed for empty graph")
        assertTrue(result.errors.isEmpty(), "No errors expected for empty graph")
    }

    // ========== Additional edge case tests ==========

    @Test
    fun `validate handles node with blank _useCaseClass as missing`() {
        // Given - A node with blank (empty string) _useCaseClass
        val node = createGenericNode(
            id = "timer",
            name = "TimerEmitter",
            useCaseClass = "   ",  // Blank
            genericType = "in0out2"
        )
        val flowGraph = createTestFlowGraph("StopWatch", listOf(node))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertFalse(result.success, "Blank _useCaseClass should be treated as missing")
    }

    @Test
    fun `validate groups multiple missing properties per node in error message`() {
        // Given - A GENERIC node missing both required properties
        val node = createGenericNode(
            id = "timer",
            name = "TimerEmitter",
            useCaseClass = null,
            genericType = null
        )
        val flowGraph = createTestFlowGraph("StopWatch", listOf(node))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertFalse(result.success)
        assertEquals(2, result.errors.size, "Should have two errors")

        // Error message should group properties for the same node
        val errorMessage = result.toErrorMessage()
        // Should have a single line for the node with both properties listed
        val lines = errorMessage.lines().filter { it.isNotBlank() }
        assertEquals(1, lines.size, "Should group errors by node into single message")
        assertTrue(
            errorMessage.contains("_useCaseClass") && errorMessage.contains("_genericType"),
            "Error message should list both missing properties"
        )
    }

    @Test
    fun `getRequiredProperties returns correct set for GENERIC type`() {
        // When
        val required = validator.getRequiredProperties(CodeNodeType.GENERIC)

        // Then
        assertEquals(setOf("_useCaseClass", "_genericType"), required)
    }

    @Test
    fun `getRequiredProperties returns empty set for non-GENERIC types`() {
        // When/Then
        assertTrue(validator.getRequiredProperties(CodeNodeType.TRANSFORMER).isEmpty())
        assertTrue(validator.getRequiredProperties(CodeNodeType.API_ENDPOINT).isEmpty())
        assertTrue(validator.getRequiredProperties(CodeNodeType.DATABASE).isEmpty())
        assertTrue(validator.getRequiredProperties(CodeNodeType.GENERATOR).isEmpty())
        assertTrue(validator.getRequiredProperties(CodeNodeType.SINK).isEmpty())
    }
}
