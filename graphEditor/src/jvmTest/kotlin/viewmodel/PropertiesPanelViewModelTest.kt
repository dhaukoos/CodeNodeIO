/*
 * PropertiesPanelViewModelTest - Unit tests for PropertiesPanelViewModel
 * Verifies property editing, validation, and node selection without Compose UI dependencies
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Connection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PropertiesPanelViewModelTest {

    private fun createTestNode(
        id: String = "node1",
        name: String = "TestNode",
        configuration: Map<String, String> = emptyMap(),
        codeNodeType: CodeNodeType = CodeNodeType.TRANSFORMER
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = codeNodeType,
            position = Node.Position(0.0, 0.0),
            configuration = configuration
        )
    }

    private fun createTestConnection(id: String = "conn1"): Connection {
        return Connection(
            id = id,
            sourceNodeId = "node1",
            sourcePortId = "port1",
            targetNodeId = "node2",
            targetPortId = "port2"
        )
    }

    @Test
    fun `initial state is empty`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val state = viewModel.state.first()

        assertTrue(state.isEmptyState)
        assertNull(state.selectedNodeId)
        assertNull(state.selectedConnectionId)
        assertEquals("", state.nodeName)
        assertTrue(state.properties.isEmpty())
        assertFalse(state.isDirty)
    }

    @Test
    fun `selectNode updates state with node data`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val node = createTestNode(
            id = "node123",
            name = "MyNode",
            configuration = mapOf("key1" to "value1")
        )

        viewModel.selectNode(node)

        val state = viewModel.state.first()
        assertEquals("node123", state.selectedNodeId)
        assertNull(state.selectedConnectionId)
        assertEquals("MyNode", state.nodeName)
        assertEquals(mapOf("key1" to "value1"), state.properties)
        assertFalse(state.isEmptyState)
    }

    @Test
    fun `selectNode with null clears selection`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        viewModel.selectNode(createTestNode())

        viewModel.selectNode(null)

        val state = viewModel.state.first()
        assertTrue(state.isEmptyState)
        assertNull(state.selectedNodeId)
    }

    @Test
    fun `selectConnection updates state`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val connection = createTestConnection("conn123")

        viewModel.selectConnection(connection)

        val state = viewModel.state.first()
        assertEquals("conn123", state.selectedConnectionId)
        assertNull(state.selectedNodeId)
        assertFalse(state.isEmptyState)
    }

    @Test
    fun `clearSelection resets to empty state`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        viewModel.selectNode(createTestNode())

        viewModel.clearSelection()

        assertTrue(viewModel.state.first().isEmptyState)
    }

    @Test
    fun `startEditing sets editingPropertyKey`() = runTest {
        val viewModel = PropertiesPanelViewModel()

        viewModel.startEditing("myProperty")

        assertEquals("myProperty", viewModel.state.first().editingPropertyKey)
    }

    @Test
    fun `updatePendingChange adds to pendingChanges`() = runTest {
        val viewModel = PropertiesPanelViewModel()

        viewModel.updatePendingChange("key1", "newValue1")
        viewModel.updatePendingChange("key2", "newValue2")

        val state = viewModel.state.first()
        assertEquals("newValue1", state.pendingChanges["key1"])
        assertEquals("newValue2", state.pendingChanges["key2"])
        assertTrue(state.isDirty)
    }

    @Test
    fun `commitChanges calls callback and updates properties`() = runTest {
        val changedProperties = mutableMapOf<String, String>()

        val viewModel = PropertiesPanelViewModel(
            onPropertyChanged = { key, value -> changedProperties[key] = value }
        )

        viewModel.selectNode(createTestNode())
        viewModel.updatePendingChange("prop1", "value1")
        viewModel.updatePendingChange("prop2", "value2")

        viewModel.commitChanges()

        // Verify callbacks were called
        assertEquals("value1", changedProperties["prop1"])
        assertEquals("value2", changedProperties["prop2"])

        // Verify pending changes are cleared
        val state = viewModel.state.first()
        assertTrue(state.pendingChanges.isEmpty())
        assertNull(state.editingPropertyKey)
    }

    @Test
    fun `cancelEditing clears pending changes and validation errors`() = runTest {
        val viewModel = PropertiesPanelViewModel()

        viewModel.updatePendingChange("key", "value")
        viewModel.setValidationError("key", "Error message")
        viewModel.startEditing("key")

        viewModel.cancelEditing()

        val state = viewModel.state.first()
        assertTrue(state.pendingChanges.isEmpty())
        assertTrue(state.validationErrors.isEmpty())
        assertNull(state.editingPropertyKey)
    }

    @Test
    fun `updateNodeName calls callback`() = runTest {
        var newNodeName: String? = null

        val viewModel = PropertiesPanelViewModel(
            onNodeNameChanged = { newNodeName = it }
        )

        viewModel.updateNodeName("NewNodeName")

        assertEquals("NewNodeName", newNodeName)
        assertEquals("NewNodeName", viewModel.state.first().nodeName)
    }

    @Test
    fun `updatePortName calls callback`() = runTest {
        var updatedPortId: String? = null
        var updatedPortName: String? = null

        val viewModel = PropertiesPanelViewModel(
            onPortNameChanged = { portId, name ->
                updatedPortId = portId
                updatedPortName = name
            }
        )

        viewModel.updatePortName("port123", "NewPortName")

        assertEquals("port123", updatedPortId)
        assertEquals("NewPortName", updatedPortName)
    }

    @Test
    fun `setValidationError adds error`() = runTest {
        val viewModel = PropertiesPanelViewModel()

        viewModel.setValidationError("field1", "Error message")

        val state = viewModel.state.first()
        assertEquals("Error message", state.getErrorForProperty("field1"))
        assertTrue(state.hasValidationErrors)
        assertFalse(state.isValid)
    }

    @Test
    fun `setValidationError with null removes error`() = runTest {
        val viewModel = PropertiesPanelViewModel()

        viewModel.setValidationError("field1", "Error")
        viewModel.setValidationError("field1", null)

        val state = viewModel.state.first()
        assertNull(state.getErrorForProperty("field1"))
        assertFalse(state.hasValidationErrors)
        assertTrue(state.isValid)
    }

    @Test
    fun `clearValidationErrors removes all errors`() = runTest {
        val viewModel = PropertiesPanelViewModel()

        viewModel.setValidationError("field1", "Error1")
        viewModel.setValidationError("field2", "Error2")

        viewModel.clearValidationErrors()

        val state = viewModel.state.first()
        assertTrue(state.validationErrors.isEmpty())
        assertTrue(state.isValid)
    }

    @Test
    fun `reset reverts to original values`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val node = createTestNode(name = "OriginalName", configuration = mapOf("key" to "original"))

        viewModel.selectNode(node)
        viewModel.updateNodeName("ModifiedName")
        viewModel.updatePendingChange("key", "modified")
        viewModel.setValidationError("key", "Error")

        viewModel.reset()

        val state = viewModel.state.first()
        assertEquals("OriginalName", state.nodeName)
        assertEquals(mapOf("key" to "original"), state.properties)
        assertTrue(state.pendingChanges.isEmpty())
        assertTrue(state.validationErrors.isEmpty())
        assertFalse(state.isDirty)
    }

    @Test
    fun `markSaved resets dirty tracking`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        viewModel.selectNode(createTestNode())
        viewModel.updateNodeName("NewName")

        assertTrue(viewModel.state.first().isDirty)

        viewModel.markSaved()

        assertFalse(viewModel.state.first().isDirty)
    }

    @Test
    fun `getCurrentValue returns pending value if exists`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        viewModel.selectNode(createTestNode(configuration = mapOf("key" to "original")))

        assertEquals("original", viewModel.state.first().getCurrentValue("key"))

        viewModel.updatePendingChange("key", "pending")

        assertEquals("pending", viewModel.state.first().getCurrentValue("key"))
    }

    @Test
    fun `isGenericNode is set correctly for generic nodes`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val genericNode = createTestNode(codeNodeType = CodeNodeType.GENERIC)

        viewModel.selectNode(genericNode)

        assertTrue(viewModel.state.first().isGenericNode)

        val nonGenericNode = createTestNode(codeNodeType = CodeNodeType.TRANSFORMER)
        viewModel.selectNode(nonGenericNode)

        assertFalse(viewModel.state.first().isGenericNode)
    }
}
