/*
 * ViewModelCharacterizationTest - Characterization tests for ViewModel state management
 * Pins current behavior of GraphEditorViewModel, NodePaletteViewModel, and
 * PropertiesPanelViewModel. These tests capture WHAT the code does, not what it SHOULD do.
 * License: Apache 2.0
 */

package io.codenode.grapheditor.characterization

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Connection
import io.codenode.fbpdsl.model.Node
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.viewmodel.EditorDialog
import io.codenode.grapheditor.viewmodel.GraphEditorViewModel
import io.codenode.grapheditor.viewmodel.NodePaletteViewModel
import io.codenode.grapheditor.viewmodel.PropertiesPanelViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Characterization tests that pin the current behavior of the ViewModel layer.
 * These tests verify that ViewModels expose the right state after mutations,
 * without launching Compose. They cover the seam between UI composables and
 * the underlying state management.
 */
class ViewModelCharacterizationTest {

    // ============================================
    // Test Fixtures
    // ============================================

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
            position = Node.Position(100.0, 100.0),
            inputPorts = listOf(
                Port(id = "${id}_in", name = "input", direction = Port.Direction.INPUT,
                    dataType = String::class, owningNodeId = id)
            ),
            outputPorts = listOf(
                Port(id = "${id}_out", name = "output", direction = Port.Direction.OUTPUT,
                    dataType = String::class, owningNodeId = id)
            ),
            configuration = configuration
        )
    }

    private fun createTestConnection(
        id: String = "conn1",
        sourceNodeId: String = "n1",
        sourcePortId: String = "n1_out",
        targetNodeId: String = "n2",
        targetPortId: String = "n2_in"
    ): Connection {
        return Connection(
            id = id,
            sourceNodeId = sourceNodeId,
            sourcePortId = sourcePortId,
            targetNodeId = targetNodeId,
            targetPortId = targetPortId
        )
    }

    // ============================================
    // GraphEditorViewModel — Initial State
    // ============================================

    @Test
    fun `GraphEditorViewModel initial state has default values`() = runTest {
        val viewModel = GraphEditorViewModel()
        val state = viewModel.state.first()

        assertEquals("Ready - Create a new graph or open an existing one", state.statusMessage)
        assertEquals(EditorDialog.NONE, state.activeDialog)
        assertFalse(state.canGroup)
        assertFalse(state.canUngroup)
        assertFalse(state.isInsideGraphNode)
        assertNull(state.currentGraphNodeName)
        assertEquals("New Graph", state.flowGraphName)
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
        assertNull(state.undoDescription)
        assertNull(state.redoDescription)
        assertFalse(state.isDirty)
    }

    @Test
    fun `GraphEditorViewModel initial state has no active dialog`() = runTest {
        val viewModel = GraphEditorViewModel()
        val state = viewModel.state.first()

        assertFalse(state.hasActiveDialog)
    }

    // ============================================
    // GraphEditorViewModel — Status Messages
    // ============================================

    @Test
    fun `setStatusMessage updates status`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.setStatusMessage("Graph loaded successfully")

        val state = viewModel.state.first()
        assertEquals("Graph loaded successfully", state.statusMessage)
    }

    @Test
    fun `clearStatusMessage sets empty string`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.setStatusMessage("Temporary message")
        viewModel.clearStatusMessage()

        val state = viewModel.state.first()
        assertEquals("", state.statusMessage)
    }

    // ============================================
    // GraphEditorViewModel — Dialog Management
    // ============================================

    @Test
    fun `showDialog sets active dialog`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.showDialog(EditorDialog.OPEN_FILE)

        val state = viewModel.state.first()
        assertEquals(EditorDialog.OPEN_FILE, state.activeDialog)
        assertTrue(state.hasActiveDialog)
    }

    @Test
    fun `hideDialog clears active dialog`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.showDialog(EditorDialog.SAVE_MODULE)
        viewModel.hideDialog()

        val state = viewModel.state.first()
        assertEquals(EditorDialog.NONE, state.activeDialog)
        assertFalse(state.hasActiveDialog)
    }

    @Test
    fun `showFlowGraphProperties sets correct dialog`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.showFlowGraphProperties()

        val state = viewModel.state.first()
        assertEquals(EditorDialog.FLOW_GRAPH_PROPERTIES, state.activeDialog)
    }

    // ============================================
    // GraphEditorViewModel — Grouping State
    // ============================================

    @Test
    fun `updateGroupingState changes canGroup and canUngroup`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateGroupingState(canGroup = true, canUngroup = false)

        val state = viewModel.state.first()
        assertTrue(state.canGroup)
        assertFalse(state.canUngroup)
    }

    @Test
    fun `updateGroupingState can enable both`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateGroupingState(canGroup = true, canUngroup = true)

        val state = viewModel.state.first()
        assertTrue(state.canGroup)
        assertTrue(state.canUngroup)
    }

    // ============================================
    // GraphEditorViewModel — Navigation State
    // ============================================

    @Test
    fun `updateNavigationState reflects GraphNode drill-in`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateNavigationState(isInsideGraphNode = true, currentGraphNodeName = "MyGroup")

        val state = viewModel.state.first()
        assertTrue(state.isInsideGraphNode)
        assertEquals("MyGroup", state.currentGraphNodeName)
        assertTrue(state.canNavigateBack)
    }

    @Test
    fun `updateNavigationState at root level`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateNavigationState(isInsideGraphNode = false, currentGraphNodeName = null)

        val state = viewModel.state.first()
        assertFalse(state.isInsideGraphNode)
        assertNull(state.currentGraphNodeName)
        assertFalse(state.canNavigateBack)
    }

    // ============================================
    // GraphEditorViewModel — Graph Name and Dirty State
    // ============================================

    @Test
    fun `updateFlowGraphName changes the displayed name`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateFlowGraphName("StopWatch")

        val state = viewModel.state.first()
        assertEquals("StopWatch", state.flowGraphName)
    }

    @Test
    fun `markDirty sets isDirty true`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.markDirty()

        val state = viewModel.state.first()
        assertTrue(state.isDirty)
    }

    @Test
    fun `markClean sets isDirty false`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.markDirty()
        viewModel.markClean()

        val state = viewModel.state.first()
        assertFalse(state.isDirty)
    }

    // ============================================
    // GraphEditorViewModel — Undo/Redo State
    // ============================================

    @Test
    fun `updateUndoRedoState reflects undo availability`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateUndoRedoState(
            canUndo = true, canRedo = false,
            undoDescription = "Add node", redoDescription = null
        )

        val state = viewModel.state.first()
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
        assertEquals("Add node", state.undoDescription)
        assertNull(state.redoDescription)
    }

    // ============================================
    // GraphEditorViewModel — Callback Invocations
    // ============================================

    @Test
    fun `createNewGraph invokes callback`() = runTest {
        var callbackInvoked = false
        val viewModel = GraphEditorViewModel(onCreateNewGraph = { callbackInvoked = true })

        viewModel.createNewGraph()

        assertTrue(callbackInvoked)
    }

    @Test
    fun `saveGraph invokes callback`() = runTest {
        var callbackInvoked = false
        val viewModel = GraphEditorViewModel(onSaveGraph = { callbackInvoked = true })

        viewModel.saveGraph()

        assertTrue(callbackInvoked)
    }

    @Test
    fun `groupSelectedNodes invokes callback when canGroup is true`() = runTest {
        var callbackInvoked = false
        val viewModel = GraphEditorViewModel(onGroupSelectedNodes = { callbackInvoked = true })

        viewModel.updateGroupingState(canGroup = true, canUngroup = false)
        viewModel.groupSelectedNodes()

        assertTrue(callbackInvoked)
    }

    @Test
    fun `groupSelectedNodes does not invoke callback when canGroup is false`() = runTest {
        var callbackInvoked = false
        val viewModel = GraphEditorViewModel(onGroupSelectedNodes = { callbackInvoked = true })

        viewModel.groupSelectedNodes()

        assertFalse(callbackInvoked)
    }

    // ============================================
    // GraphEditorViewModel — Reset
    // ============================================

    @Test
    fun `reset restores initial state`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.setStatusMessage("Modified")
        viewModel.markDirty()
        viewModel.updateFlowGraphName("Changed")
        viewModel.showDialog(EditorDialog.OPEN_FILE)
        viewModel.reset()

        val state = viewModel.state.first()
        assertEquals("Ready - Create a new graph or open an existing one", state.statusMessage)
        assertFalse(state.isDirty)
        assertEquals("New Graph", state.flowGraphName)
        assertEquals(EditorDialog.NONE, state.activeDialog)
    }

    // ============================================
    // NodePaletteViewModel — Initial State
    // ============================================

    @Test
    fun `NodePaletteViewModel initial state has empty search and no expanded categories`() = runTest {
        val viewModel = NodePaletteViewModel()
        val state = viewModel.state.first()

        assertEquals("", state.searchQuery)
        assertTrue(state.expandedCategories.isEmpty())
    }

    // ============================================
    // NodePaletteViewModel — Search
    // ============================================

    @Test
    fun `setSearchQuery updates search state`() = runTest {
        val viewModel = NodePaletteViewModel()

        viewModel.setSearchQuery("Timer")

        val state = viewModel.state.first()
        assertEquals("Timer", state.searchQuery)
    }

    @Test
    fun `clearSearch resets query to empty`() = runTest {
        val viewModel = NodePaletteViewModel()

        viewModel.setSearchQuery("Filter")
        viewModel.clearSearch()

        val state = viewModel.state.first()
        assertEquals("", state.searchQuery)
    }

    // ============================================
    // NodePaletteViewModel — Category Expansion
    // ============================================

    @Test
    fun `expandCategory adds category to expanded set`() = runTest {
        val viewModel = NodePaletteViewModel()

        viewModel.expandCategory(CodeNodeType.SOURCE)

        val state = viewModel.state.first()
        assertTrue(state.expandedCategories.contains(CodeNodeType.SOURCE))
    }

    @Test
    fun `collapseCategory removes category from expanded set`() = runTest {
        val viewModel = NodePaletteViewModel()

        viewModel.expandCategory(CodeNodeType.SOURCE)
        viewModel.collapseCategory(CodeNodeType.SOURCE)

        val state = viewModel.state.first()
        assertFalse(state.expandedCategories.contains(CodeNodeType.SOURCE))
    }

    @Test
    fun `toggleCategory toggles expansion state`() = runTest {
        val viewModel = NodePaletteViewModel()

        viewModel.toggleCategory(CodeNodeType.TRANSFORMER)
        val afterExpand = viewModel.state.first()
        assertTrue(afterExpand.expandedCategories.contains(CodeNodeType.TRANSFORMER))

        viewModel.toggleCategory(CodeNodeType.TRANSFORMER)
        val afterCollapse = viewModel.state.first()
        assertFalse(afterCollapse.expandedCategories.contains(CodeNodeType.TRANSFORMER))
    }

    @Test
    fun `collapseAllCategories clears expanded set`() = runTest {
        val viewModel = NodePaletteViewModel()

        viewModel.expandCategory(CodeNodeType.SOURCE)
        viewModel.expandCategory(CodeNodeType.SINK)
        viewModel.expandCategory(CodeNodeType.TRANSFORMER)
        viewModel.collapseAllCategories()

        val state = viewModel.state.first()
        assertTrue(state.expandedCategories.isEmpty())
    }

    @Test
    fun `multiple categories can be expanded simultaneously`() = runTest {
        val viewModel = NodePaletteViewModel()

        viewModel.expandCategory(CodeNodeType.SOURCE)
        viewModel.expandCategory(CodeNodeType.SINK)

        val state = viewModel.state.first()
        assertEquals(2, state.expandedCategories.size)
        assertTrue(state.expandedCategories.contains(CodeNodeType.SOURCE))
        assertTrue(state.expandedCategories.contains(CodeNodeType.SINK))
    }

    // ============================================
    // PropertiesPanelViewModel — Initial State
    // ============================================

    @Test
    fun `PropertiesPanelViewModel initial state is empty`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val state = viewModel.state.first()

        assertNull(state.selectedNodeId)
        assertNull(state.selectedConnectionId)
        assertEquals("", state.nodeName)
        assertTrue(state.properties.isEmpty())
        assertTrue(state.isEmptyState)
        assertFalse(state.isDirty)
        assertFalse(state.hasValidationErrors)
        assertTrue(state.isValid)
    }

    // ============================================
    // PropertiesPanelViewModel — Node Selection
    // ============================================

    @Test
    fun `selectNode populates state with node data`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val node = createTestNode(id = "n1", name = "MyProcessor",
            configuration = mapOf("timeout" to "5000"))

        viewModel.selectNode(node)

        val state = viewModel.state.first()
        assertEquals("n1", state.selectedNodeId)
        assertEquals("MyProcessor", state.nodeName)
        assertFalse(state.isEmptyState)
    }

    @Test
    fun `selectNode with null clears selection`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val node = createTestNode()

        viewModel.selectNode(node)
        viewModel.selectNode(null)

        val state = viewModel.state.first()
        assertNull(state.selectedNodeId)
        assertTrue(state.isEmptyState)
    }

    // ============================================
    // PropertiesPanelViewModel — Connection Selection
    // ============================================

    @Test
    fun `selectConnection populates connection state`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val conn = createTestConnection(id = "c1")

        viewModel.selectConnection(conn)

        val state = viewModel.state.first()
        assertEquals("c1", state.selectedConnectionId)
        assertFalse(state.isEmptyState)
    }

    @Test
    fun `selectConnection with null clears selection`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val conn = createTestConnection()

        viewModel.selectConnection(conn)
        viewModel.selectConnection(null)

        val state = viewModel.state.first()
        assertNull(state.selectedConnectionId)
        assertTrue(state.isEmptyState)
    }

    // ============================================
    // PropertiesPanelViewModel — Editing
    // ============================================

    @Test
    fun `updateNodeName changes name in state`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val node = createTestNode(name = "Original")

        viewModel.selectNode(node)
        viewModel.updateNodeName("Renamed")

        val state = viewModel.state.first()
        assertEquals("Renamed", state.nodeName)
    }

    @Test
    fun `updateNodeName makes state dirty`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val node = createTestNode(name = "Original")

        viewModel.selectNode(node)
        viewModel.updateNodeName("Changed")

        val state = viewModel.state.first()
        assertTrue(state.isDirty)
    }

    // ============================================
    // PropertiesPanelViewModel — Validation
    // ============================================

    @Test
    fun `setValidationError adds error to state`() = runTest {
        val viewModel = PropertiesPanelViewModel()

        viewModel.setValidationError("name", "Name cannot be empty")

        val state = viewModel.state.first()
        assertTrue(state.hasValidationErrors)
        assertFalse(state.isValid)
        assertEquals("Name cannot be empty", state.getErrorForProperty("name"))
    }

    @Test
    fun `clearValidationErrors removes all errors`() = runTest {
        val viewModel = PropertiesPanelViewModel()

        viewModel.setValidationError("name", "Error")
        viewModel.clearValidationErrors()

        val state = viewModel.state.first()
        assertFalse(state.hasValidationErrors)
        assertTrue(state.isValid)
    }

    // ============================================
    // PropertiesPanelViewModel — Clear and Reset
    // ============================================

    @Test
    fun `PropertiesPanel clearSelection resets to empty state`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val node = createTestNode()

        viewModel.selectNode(node)
        viewModel.clearSelection()

        val state = viewModel.state.first()
        assertTrue(state.isEmptyState)
        assertEquals("", state.nodeName)
    }

    @Test
    fun `PropertiesPanel reset reverts to original values but keeps selection`() = runTest {
        val viewModel = PropertiesPanelViewModel()
        val node = createTestNode(name = "Original")

        viewModel.selectNode(node)
        viewModel.updateNodeName("Modified")
        viewModel.setValidationError("key", "error")
        viewModel.reset()

        val state = viewModel.state.first()
        // reset() reverts name/properties to originals but preserves selection
        assertFalse(state.isEmptyState)
        assertEquals("Original", state.nodeName)
        assertFalse(state.isDirty)
        assertFalse(state.hasValidationErrors)
    }

    // ============================================
    // PropertiesPanelViewModel — Callback Invocations
    // ============================================

    @Test
    fun `updatePortName invokes callback`() = runTest {
        var capturedPortId = ""
        var capturedName = ""
        val viewModel = PropertiesPanelViewModel(
            onPortNameChanged = { portId, name ->
                capturedPortId = portId
                capturedName = name
            }
        )

        viewModel.updatePortName("port1", "newName")

        assertEquals("port1", capturedPortId)
        assertEquals("newName", capturedName)
    }

    @Test
    fun `updatePortType invokes callback`() = runTest {
        var capturedPortId = ""
        var capturedType = ""
        val viewModel = PropertiesPanelViewModel(
            onPortTypeChanged = { portId, typeName ->
                capturedPortId = portId
                capturedType = typeName
            }
        )

        viewModel.updatePortType("port1", "Int")

        assertEquals("port1", capturedPortId)
        assertEquals("Int", capturedType)
    }
}
