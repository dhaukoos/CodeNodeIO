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
 * After redesign: No configuration properties are required. Port counts and types
 * are derived directly from the CodeNode model by RuntimeTypeResolver.
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

    // ========== T005: validate returns success when config properties present ==========

    @Test
    fun `T005 - validate returns success when config properties present`() {
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
        assertTrue(result.success, "Validation should succeed")
        assertTrue(result.errors.isEmpty(), "No errors expected")
        assertEquals("", result.toErrorMessage(), "Error message should be empty")
    }

    // ========== T006: validate succeeds for GENERIC node with no config (no longer required) ==========

    @Test
    fun `T006 - validate succeeds for GENERIC node with no config properties`() {
        // Given - A GENERIC node with no configuration properties
        // Neither _useCaseClass nor _genericType are required after redesign
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
        assertTrue(result.success, "Validation should succeed - no config properties are required")
        assertTrue(result.errors.isEmpty(), "No errors expected")
    }

    // ========== T007: validate succeeds for GENERIC node with partial config ==========

    @Test
    fun `T007 - validate succeeds for GENERIC node with only _genericType`() {
        // Given - A GENERIC node with _genericType but missing _useCaseClass
        val node = createGenericNode(
            id = "timer",
            name = "TimerEmitter",
            useCaseClass = null,
            genericType = "in0out2"
        )
        val flowGraph = createTestFlowGraph("StopWatch", listOf(node))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertTrue(result.success, "Validation should succeed")
        assertTrue(result.errors.isEmpty())
    }

    // ========== T008: validate succeeds for multiple GENERIC nodes with no config ==========

    @Test
    fun `T008 - validate succeeds for multiple GENERIC nodes with empty config`() {
        // Given - Two GENERIC nodes, both with empty configuration
        val timerNode = createGenericNode(
            id = "timer",
            name = "TimerEmitter",
            useCaseClass = null,
            genericType = null
        )
        val displayNode = createGenericNode(
            id = "display",
            name = "DisplayReceiver",
            useCaseClass = null,
            genericType = null
        )
        val flowGraph = createTestFlowGraph("StopWatch", listOf(timerNode, displayNode))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertTrue(result.success, "Validation should succeed - no config properties are required")
        assertTrue(result.errors.isEmpty())
    }

    // ========== T009: validate ignores non-GENERIC node types ==========

    @Test
    fun `T009 - validate ignores non-GENERIC node types`() {
        // Given - Non-GENERIC nodes without any config
        val transformerNode = createNonGenericNode("transform", "DataTransformer", CodeNodeType.TRANSFORMER)
        val apiNode = createNonGenericNode("api", "APIEndpoint", CodeNodeType.API_ENDPOINT)
        val dbNode = createNonGenericNode("db", "DatabaseQuery", CodeNodeType.DATABASE)

        val flowGraph = createTestFlowGraph("MixedFlow", listOf(transformerNode, apiNode, dbNode))

        // When
        val result = validator.validate(flowGraph)

        // Then
        assertTrue(result.success, "Validation should succeed for non-GENERIC nodes")
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

    // ========== Additional tests ==========

    @Test
    fun `getRequiredProperties returns empty set for GENERIC type`() {
        // After redesign, no config properties are required
        val required = validator.getRequiredProperties(CodeNodeType.GENERIC)
        assertTrue(required.isEmpty(), "No properties should be required for GENERIC nodes")
    }

    @Test
    fun `getRequiredProperties returns empty set for non-GENERIC types`() {
        assertTrue(validator.getRequiredProperties(CodeNodeType.TRANSFORMER).isEmpty())
        assertTrue(validator.getRequiredProperties(CodeNodeType.API_ENDPOINT).isEmpty())
        assertTrue(validator.getRequiredProperties(CodeNodeType.DATABASE).isEmpty())
        assertTrue(validator.getRequiredProperties(CodeNodeType.GENERATOR).isEmpty())
        assertTrue(validator.getRequiredProperties(CodeNodeType.SINK).isEmpty())
    }
}
