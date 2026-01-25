/*
 * ViewSynchronizer Test
 * Unit tests for bidirectional view synchronization
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.dsl.flowGraph
import io.codenode.grapheditor.state.GraphState
import io.codenode.grapheditor.state.ViewSynchronizer
import kotlin.test.*

class ViewSynchronizerTest {

    private fun createTestGraph() = flowGraph("TestGraph", version = "1.0.0") {
        val gen = codeNode("Generator") {
            output("data", String::class)
        }

        val proc = codeNode("Processor") {
            input("input", String::class)
            output("result", String::class)
        }

        gen.output("data") connect proc.input("input")
    }

    @Test
    fun `should initialize with generated DSL text`() {
        // Given a graph state
        val graphState = GraphState(createTestGraph())

        // When creating synchronizer
        val synchronizer = ViewSynchronizer(graphState)

        // Then DSL text should be generated
        assertFalse(synchronizer.dslText.value.isEmpty(), "DSL text should be generated")
        assertTrue(synchronizer.dslText.value.contains("flowGraph"), "Should contain flowGraph keyword")
        assertTrue(synchronizer.dslText.value.contains("Generator"), "Should contain Generator node")
    }

    @Test
    fun `should mark changes when edited text differs from generated`() {
        // Given a synchronizer
        val graphState = GraphState(createTestGraph())
        val synchronizer = ViewSynchronizer(graphState)

        // When updating edited text
        synchronizer.updateEditedText("modified text")

        // Then should mark as having unapplied changes
        assertTrue(synchronizer.hasUnappliedChanges.value, "Should have unapplied changes")
    }

    @Test
    fun `should clear unapplied changes flag when texts match`() {
        // Given a synchronizer with unapplied changes
        val graphState = GraphState(createTestGraph())
        val synchronizer = ViewSynchronizer(graphState)
        synchronizer.updateEditedText("modified text")
        assertTrue(synchronizer.hasUnappliedChanges.value, "Should have unapplied changes initially")

        // When updating edited text to match generated text
        synchronizer.updateEditedText(synchronizer.dslText.value)

        // Then should clear unapplied changes flag
        assertFalse(synchronizer.hasUnappliedChanges.value, "Should not have unapplied changes")
    }

    @Test
    fun `should discard text changes and revert to generated text`() {
        // Given a synchronizer with modified text
        val graphState = GraphState(createTestGraph())
        val synchronizer = ViewSynchronizer(graphState)
        val originalText = synchronizer.dslText.value
        synchronizer.updateEditedText("completely different text")

        // When discarding changes
        synchronizer.discardTextChanges()

        // Then edited text should revert to generated text
        assertEquals(originalText, synchronizer.editedText.value, "Should revert to original text")
        assertFalse(synchronizer.hasUnappliedChanges.value, "Should not have unapplied changes")
    }

    @Test
    fun `should regenerate text when graph changes`() {
        // Given a synchronizer
        val graphState = GraphState(createTestGraph())
        val synchronizer = ViewSynchronizer(graphState)
        val initialText = synchronizer.dslText.value

        // When graph changes (add a node)
        val newNode = io.codenode.fbpdsl.model.CodeNode(
            id = "node_test_123",
            name = "NewNode",
            codeNodeType = io.codenode.fbpdsl.model.CodeNodeType.CUSTOM,
            position = io.codenode.fbpdsl.model.Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = emptyList()
        )
        graphState.addNode(newNode, androidx.compose.ui.geometry.Offset(100f, 100f))
        synchronizer.onVisualGraphChanged()

        // Then DSL text should be regenerated
        assertNotEquals(initialText, synchronizer.dslText.value, "Text should be regenerated")
        assertTrue(synchronizer.dslText.value.contains("NewNode"), "Should contain new node")
    }

    @Test
    fun `should preserve edited text when graph changes if user has modifications`() {
        // Given a synchronizer with user edits
        val graphState = GraphState(createTestGraph())
        val synchronizer = ViewSynchronizer(graphState)
        val userEditedText = "// User's custom text"
        synchronizer.updateEditedText(userEditedText)

        // When graph changes
        val newNode = io.codenode.fbpdsl.model.CodeNode(
            id = "node_test_456",
            name = "AnotherNode",
            codeNodeType = io.codenode.fbpdsl.model.CodeNodeType.CUSTOM,
            position = io.codenode.fbpdsl.model.Node.Position(200.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = emptyList()
        )
        graphState.addNode(newNode, androidx.compose.ui.geometry.Offset(200f, 200f))
        synchronizer.onVisualGraphChanged()

        // Then edited text should be preserved
        assertEquals(userEditedText, synchronizer.editedText.value,
            "User's edited text should be preserved")
        assertTrue(synchronizer.hasUnappliedChanges.value,
            "Should still have unapplied changes")
    }

    @Test
    fun `should provide sync statistics`() {
        // Given a synchronizer
        val graphState = GraphState(createTestGraph())
        val synchronizer = ViewSynchronizer(graphState)

        // When getting sync stats
        val stats = synchronizer.getSyncStats()

        // Then stats should reflect current state
        assertTrue(stats.generatedTextLength > 0, "Generated text should have length")
        assertTrue(stats.editedTextLength > 0, "Edited text should have length")
        assertEquals(2, stats.nodeCount, "Should have 2 nodes")
        assertEquals(1, stats.connectionCount, "Should have 1 connection")
        assertFalse(stats.hasUnappliedChanges, "Should not have unapplied changes initially")
    }

    @Test
    fun `should clear sync errors`() {
        // Given a synchronizer with an error
        val graphState = GraphState(createTestGraph())
        val synchronizer = ViewSynchronizer(graphState)

        // Trigger an error by trying to apply changes (which will fail as parsing is not implemented)
        synchronizer.applyTextChangesToGraph()
        assertNotNull(synchronizer.syncError.value, "Should have sync error")

        // When clearing error
        synchronizer.clearError()

        // Then error should be cleared
        assertNull(synchronizer.syncError.value, "Sync error should be cleared")
    }

    @Test
    fun `should handle empty graph`() {
        // Given an empty graph
        val emptyGraph = flowGraph("Empty", version = "1.0.0") {}
        val graphState = GraphState(emptyGraph)

        // When creating synchronizer
        val synchronizer = ViewSynchronizer(graphState)

        // Then should handle gracefully
        assertTrue(synchronizer.dslText.value.contains("flowGraph"), "Should have flowGraph declaration")
        assertEquals(0, synchronizer.getSyncStats().nodeCount, "Should have 0 nodes")
        assertEquals(0, synchronizer.getSyncStats().connectionCount, "Should have 0 connections")
    }

    @Test
    fun `should detect when texts are synchronized`() {
        // Given a synchronizer
        val graphState = GraphState(createTestGraph())
        val synchronizer = ViewSynchronizer(graphState)

        // When texts are in sync
        // Initially they should be in sync
        assertFalse(synchronizer.hasUnappliedChanges.value, "Should be in sync initially")

        // And edited text matches generated text
        assertEquals(synchronizer.dslText.value, synchronizer.editedText.value,
            "Texts should match")
    }
}
