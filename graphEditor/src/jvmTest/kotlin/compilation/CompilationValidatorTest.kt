/*
 * CompilationValidator Test
 * TDD tests for pre-compilation validation of FlowGraph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compilation

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import java.io.File
import kotlin.test.*

/**
 * TDD tests for CompilationValidator.
 *
 * These tests verify that CompilationValidator:
 * - T022: Returns success for valid graphs with all required properties
 * - T023: Fails validation for graphs missing _useCaseClass
 * - T024: Reports all nodes with missing properties
 * - T025: Validates file existence
 * - T026: Validates .kt file extension
 *
 * Note: These tests are designed to FAIL initially (TDD Red phase)
 * until CompilationValidator.validate() is implemented.
 */
class CompilationValidatorTest {

    private lateinit var projectRoot: File
    private lateinit var testFile: File

    @BeforeTest
    fun setUp() {
        // Create a temporary project root with a test file
        projectRoot = File(System.getProperty("java.io.tmpdir"), "test_project_${System.currentTimeMillis()}")
        projectRoot.mkdirs()

        // Create a valid Kotlin file for testing
        val demosDir = File(projectRoot, "demos/stopwatch")
        demosDir.mkdirs()
        testFile = File(demosDir, "TestComponent.kt")
        testFile.writeText("class TestComponent")
    }

    @AfterTest
    fun tearDown() {
        // Clean up temp files
        projectRoot.deleteRecursively()
    }

    // ============================================
    // T022: Valid Graph Validation Tests
    // ============================================

    @Test
    fun `should pass validation for graph with all required properties configured`() {
        // Given: A graph with _useCaseClass configured for all nodes
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = "demos/stopwatch/TestComponent.kt"
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should pass
        assertTrue(result.isValid, "Graph with valid _useCaseClass should pass validation")
        assertTrue(result.success, "Success flag should be true")
        assertTrue(result.nodeErrors.isEmpty(), "Should have no errors")
    }

    @Test
    fun `should pass validation for empty graph`() {
        // Given: A graph with no nodes
        val graph = createGraphWithNodes(emptyList())

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should pass (no nodes to validate)
        assertTrue(result.isValid, "Empty graph should pass validation")
    }

    @Test
    fun `should pass validation for graph with multiple valid nodes`() {
        // Given: Multiple nodes all with valid _useCaseClass
        val node1 = createNodeWithUseCaseClass(
            id = "node1",
            name = "Node1",
            useCaseClass = "demos/stopwatch/TestComponent.kt"
        )
        val node2 = createNodeWithUseCaseClass(
            id = "node2",
            name = "Node2",
            useCaseClass = "demos/stopwatch/TestComponent.kt"
        )
        val graph = createGraphWithNodes(listOf(node1, node2))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should pass
        assertTrue(result.isValid, "Graph with all valid nodes should pass validation")
    }

    // ============================================
    // T023: Missing _useCaseClass Validation Tests
    // ============================================

    @Test
    fun `should fail validation for node missing _useCaseClass`() {
        // Given: A node without _useCaseClass configuration
        val node = createNodeWithoutUseCaseClass(
            id = "test_node",
            name = "TestNode"
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should fail
        assertFalse(result.isValid, "Graph with missing _useCaseClass should fail validation")
        assertFalse(result.success, "Success flag should be false")
        assertEquals(1, result.nodeErrors.size, "Should have one node error")
    }

    @Test
    fun `should report node name in error for missing _useCaseClass`() {
        // Given: A node without _useCaseClass
        val node = createNodeWithoutUseCaseClass(
            id = "timer_emitter",
            name = "TimerEmitter"
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Error should identify the node
        val error = result.nodeErrors.first()
        assertEquals("timer_emitter", error.nodeId, "Error should contain node ID")
        assertEquals("TimerEmitter", error.nodeName, "Error should contain node name")
    }

    @Test
    fun `should fail validation for node with empty _useCaseClass`() {
        // Given: A node with empty _useCaseClass
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = ""
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should fail
        assertFalse(result.isValid, "Graph with empty _useCaseClass should fail validation")
    }

    @Test
    fun `should fail validation for node with blank _useCaseClass`() {
        // Given: A node with whitespace-only _useCaseClass
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = "   "
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should fail
        assertFalse(result.isValid, "Graph with blank _useCaseClass should fail validation")
    }

    // ============================================
    // T024: Multiple Nodes Missing Properties Tests
    // ============================================

    @Test
    fun `should report all nodes with missing properties`() {
        // Given: Multiple nodes all missing _useCaseClass
        val node1 = createNodeWithoutUseCaseClass(id = "node1", name = "Node1")
        val node2 = createNodeWithoutUseCaseClass(id = "node2", name = "Node2")
        val node3 = createNodeWithoutUseCaseClass(id = "node3", name = "Node3")
        val graph = createGraphWithNodes(listOf(node1, node2, node3))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: All nodes should be reported
        assertFalse(result.isValid)
        assertEquals(3, result.nodeErrors.size, "Should report all 3 nodes with errors")

        val nodeIds = result.nodeErrors.map { it.nodeId }
        assertTrue(nodeIds.contains("node1"), "Should report node1")
        assertTrue(nodeIds.contains("node2"), "Should report node2")
        assertTrue(nodeIds.contains("node3"), "Should report node3")
    }

    @Test
    fun `should report mix of valid and invalid nodes correctly`() {
        // Given: Some valid nodes and some invalid nodes
        val validNode = createNodeWithUseCaseClass(
            id = "valid_node",
            name = "ValidNode",
            useCaseClass = "demos/stopwatch/TestComponent.kt"
        )
        val invalidNode1 = createNodeWithoutUseCaseClass(id = "invalid1", name = "Invalid1")
        val invalidNode2 = createNodeWithoutUseCaseClass(id = "invalid2", name = "Invalid2")
        val graph = createGraphWithNodes(listOf(validNode, invalidNode1, invalidNode2))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Only invalid nodes should be reported
        assertFalse(result.isValid)
        assertEquals(2, result.nodeErrors.size, "Should report only 2 invalid nodes")

        val nodeIds = result.nodeErrors.map { it.nodeId }
        assertFalse(nodeIds.contains("valid_node"), "Should not report valid node")
        assertTrue(nodeIds.contains("invalid1"), "Should report invalid1")
        assertTrue(nodeIds.contains("invalid2"), "Should report invalid2")
    }

    @Test
    fun `error summary should list all nodes with issues`() {
        // Given: Multiple invalid nodes
        val node1 = createNodeWithoutUseCaseClass(id = "node1", name = "TimerEmitter")
        val node2 = createNodeWithoutUseCaseClass(id = "node2", name = "DisplayReceiver")
        val graph = createGraphWithNodes(listOf(node1, node2))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Error summary should contain all node names
        val summary = result.errorSummary
        assertTrue(summary.contains("TimerEmitter"), "Summary should mention TimerEmitter")
        assertTrue(summary.contains("DisplayReceiver"), "Summary should mention DisplayReceiver")
    }

    // ============================================
    // T025: File Existence Validation Tests
    // ============================================

    @Test
    fun `should fail validation for non-existent file`() {
        // Given: A node with _useCaseClass pointing to non-existent file
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = "demos/stopwatch/NonExistent.kt"
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should fail with file not found error
        assertFalse(result.isValid, "Graph with non-existent file should fail validation")
        assertEquals(1, result.nodeErrors.size)

        val error = result.nodeErrors.first()
        assertTrue(
            error.invalidProperties.any { it.reason.contains("not found") || it.reason.contains("File not found") },
            "Error should indicate file not found"
        )
    }

    @Test
    fun `should report file path in error message for missing file`() {
        // Given: A node with non-existent file
        val missingPath = "src/missing/Component.kt"
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = missingPath
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Error should contain the file path
        assertFalse(result.isValid)
        val errorMessage = result.nodeErrors.first().message
        assertTrue(
            errorMessage.contains(missingPath) || result.nodeErrors.first().invalidProperties.any { it.reason.contains(missingPath) },
            "Error should contain the missing file path"
        )
    }

    @Test
    fun `should validate file existence relative to project root`() {
        // Given: A file that exists at the correct relative path
        val relativePath = "demos/stopwatch/TestComponent.kt"
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = relativePath
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should pass (file exists)
        assertTrue(result.isValid, "Validation should pass when file exists at relative path")
    }

    // ============================================
    // T026: .kt File Extension Validation Tests
    // ============================================

    @Test
    fun `should fail validation for non-kotlin file extension`() {
        // Given: A node with _useCaseClass pointing to non-kt file
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = "demos/stopwatch/readme.md"
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should fail
        assertFalse(result.isValid, "Graph with non-kt file should fail validation")
    }

    @Test
    fun `should report error for java file extension`() {
        // Given: A node with java file instead of kt
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = "src/Component.java"
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Error should indicate wrong extension
        assertFalse(result.isValid)
        val error = result.nodeErrors.first()
        assertTrue(
            error.invalidProperties.any { it.reason.contains("kt") },
            "Error should mention kt requirement"
        )
    }

    @Test
    fun `should accept valid kt file extension`() {
        // Given: A node with proper kt file that exists
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = "demos/stopwatch/TestComponent.kt"
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should pass
        assertTrue(result.isValid, "Validation should pass for kt file")
    }

    @Test
    fun `should be case sensitive for kt extension`() {
        // Given: A node with KT (uppercase) extension
        val node = createNodeWithUseCaseClass(
            id = "test_node",
            name = "TestNode",
            useCaseClass = "demos/Component.KT"
        )
        val graph = createGraphWithNodes(listOf(node))

        // When: Validating the graph
        val result = CompilationValidator.validate(graph, projectRoot)

        // Then: Validation should fail (extension is case-sensitive)
        assertFalse(result.isValid, "Validation should be case-sensitive for kt extension")
    }

    // ============================================
    // Helper Functions
    // ============================================

    private fun createNodeWithUseCaseClass(
        id: String,
        name: String,
        useCaseClass: String
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    owningNodeId = id
                )
            ),
            configuration = mapOf("_useCaseClass" to useCaseClass)
        )
    }

    private fun createNodeWithoutUseCaseClass(
        id: String,
        name: String
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = Any::class,
                    owningNodeId = id
                )
            ),
            configuration = emptyMap()  // No _useCaseClass
        )
    }

    private fun createGraphWithNodes(nodes: List<CodeNode>): FlowGraph {
        return FlowGraph(
            id = "test_graph",
            name = "TestGraph",
            version = "1.0.0",
            description = "Test graph for validation",
            rootNodes = nodes,
            connections = emptyList(),
            metadata = emptyMap(),
            targetPlatforms = emptyList()
        )
    }
}
