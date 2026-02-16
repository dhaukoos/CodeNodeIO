/*
 * GraphEditorViewModelTest - Unit tests for GraphEditorViewModel
 * Verifies file actions, edit actions, navigation, and dialog management without Compose UI dependencies
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphEditorViewModelTest {

    // ========== Initial State Tests ==========

    @Test
    fun `initial state has default values`() = runTest {
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
        assertFalse(state.isDirty)
        assertFalse(state.hasActiveDialog)
        assertFalse(state.canNavigateBack)
    }

    // ========== File Actions Tests ==========

    @Test
    fun `createNewGraph calls callback and updates state`() = runTest {
        var callbackCalled = false

        val viewModel = GraphEditorViewModel(
            onCreateNewGraph = { callbackCalled = true }
        )

        viewModel.createNewGraph()

        assertTrue(callbackCalled)

        val state = viewModel.state.first()
        assertEquals("New graph created", state.statusMessage)
        assertEquals("New Graph", state.flowGraphName)
        assertFalse(state.isDirty)
        assertFalse(state.isInsideGraphNode)
    }

    @Test
    fun `openGraph shows open dialog and calls callback`() = runTest {
        var callbackCalled = false

        val viewModel = GraphEditorViewModel(
            onOpenGraph = { callbackCalled = true }
        )

        viewModel.openGraph()

        assertTrue(callbackCalled)
        assertEquals(EditorDialog.OPEN_FILE, viewModel.state.first().activeDialog)
    }

    @Test
    fun `saveGraph shows save dialog and calls callback`() = runTest {
        var callbackCalled = false

        val viewModel = GraphEditorViewModel(
            onSaveGraph = { callbackCalled = true }
        )

        viewModel.saveGraph()

        assertTrue(callbackCalled)
        assertEquals(EditorDialog.SAVE_MODULE, viewModel.state.first().activeDialog)
    }

    @Test
    fun `onGraphLoaded updates state and hides dialog`() = runTest {
        val viewModel = GraphEditorViewModel()
        viewModel.showDialog(EditorDialog.OPEN_FILE)

        viewModel.onGraphLoaded("MyGraph")

        val state = viewModel.state.first()
        assertEquals("Loaded: MyGraph", state.statusMessage)
        assertEquals("MyGraph", state.flowGraphName)
        assertFalse(state.isDirty)
        assertEquals(EditorDialog.NONE, state.activeDialog)
    }

    @Test
    fun `onGraphSaved updates state and hides dialog`() = runTest {
        val viewModel = GraphEditorViewModel()
        viewModel.showDialog(EditorDialog.SAVE_MODULE)
        viewModel.markDirty()

        viewModel.onGraphSaved()

        val state = viewModel.state.first()
        assertEquals("Graph saved", state.statusMessage)
        assertFalse(state.isDirty)
        assertEquals(EditorDialog.NONE, state.activeDialog)
    }

    // ========== Edit Actions Tests ==========

    @Test
    fun `undo calls callback and updates status message`() = runTest {
        var undoCalled = false

        val viewModel = GraphEditorViewModel(
            onUndo = {
                undoCalled = true
                true
            }
        )

        // Set up redo description (which becomes the undo message after undo)
        viewModel.updateUndoRedoState(true, false, null, "move node")

        viewModel.undo()

        assertTrue(undoCalled)
        assertEquals("Undo: move node", viewModel.state.first().statusMessage)
    }

    @Test
    fun `undo does nothing when callback returns false`() = runTest {
        val viewModel = GraphEditorViewModel(
            onUndo = { false }
        )

        val initialMessage = viewModel.state.first().statusMessage
        viewModel.undo()

        // Message should not change when undo fails
        assertEquals(initialMessage, viewModel.state.first().statusMessage)
    }

    @Test
    fun `redo calls callback and updates status message`() = runTest {
        var redoCalled = false

        val viewModel = GraphEditorViewModel(
            onRedo = {
                redoCalled = true
                true
            }
        )

        viewModel.updateUndoRedoState(false, true, "add node", null)

        viewModel.redo()

        assertTrue(redoCalled)
        assertEquals("Redo: add node", viewModel.state.first().statusMessage)
    }

    @Test
    fun `groupSelectedNodes calls callback when canGroup is true`() = runTest {
        var groupCalled = false

        val viewModel = GraphEditorViewModel(
            onGroupSelectedNodes = { groupCalled = true }
        )

        viewModel.updateGroupingState(canGroup = true, canUngroup = false)
        viewModel.groupSelectedNodes()

        assertTrue(groupCalled)
    }

    @Test
    fun `groupSelectedNodes does nothing when canGroup is false`() = runTest {
        var groupCalled = false

        val viewModel = GraphEditorViewModel(
            onGroupSelectedNodes = { groupCalled = true }
        )

        viewModel.updateGroupingState(canGroup = false, canUngroup = false)
        viewModel.groupSelectedNodes()

        assertFalse(groupCalled)
    }

    @Test
    fun `ungroupSelectedNode calls callback when canUngroup is true`() = runTest {
        var ungroupCalled = false

        val viewModel = GraphEditorViewModel(
            onUngroupSelectedNode = { ungroupCalled = true }
        )

        viewModel.updateGroupingState(canGroup = false, canUngroup = true)
        viewModel.ungroupSelectedNode()

        assertTrue(ungroupCalled)
    }

    @Test
    fun `updateGroupingState updates state`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateGroupingState(canGroup = true, canUngroup = true)

        val state = viewModel.state.first()
        assertTrue(state.canGroup)
        assertTrue(state.canUngroup)
    }

    @Test
    fun `updateUndoRedoState updates state`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateUndoRedoState(
            canUndo = true,
            canRedo = true,
            undoDescription = "Delete node",
            redoDescription = "Add node"
        )

        val state = viewModel.state.first()
        assertTrue(state.canUndo)
        assertTrue(state.canRedo)
        assertEquals("Delete node", state.undoDescription)
        assertEquals("Add node", state.redoDescription)
    }

    // ========== Navigation Actions Tests ==========

    @Test
    fun `navigateBack calls callback and updates status when successful`() = runTest {
        var navigateCalled = false

        val viewModel = GraphEditorViewModel(
            onNavigateBack = {
                navigateCalled = true
                true
            }
        )

        viewModel.navigateBack()

        assertTrue(navigateCalled)
        assertEquals("Navigated back to parent", viewModel.state.first().statusMessage)
    }

    @Test
    fun `navigateBack does not update status when unsuccessful`() = runTest {
        val viewModel = GraphEditorViewModel(
            onNavigateBack = { false }
        )

        val initialMessage = viewModel.state.first().statusMessage
        viewModel.navigateBack()

        assertEquals(initialMessage, viewModel.state.first().statusMessage)
    }

    @Test
    fun `compile shows compile dialog and calls callback`() = runTest {
        var compileCalled = false

        val viewModel = GraphEditorViewModel(
            onCompile = { compileCalled = true }
        )

        viewModel.compile()

        assertTrue(compileCalled)
        assertEquals(EditorDialog.COMPILE, viewModel.state.first().activeDialog)
    }

    @Test
    fun `updateNavigationState updates state`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateNavigationState(isInsideGraphNode = true, currentGraphNodeName = "MyGraphNode")

        val state = viewModel.state.first()
        assertTrue(state.isInsideGraphNode)
        assertEquals("MyGraphNode", state.currentGraphNodeName)
        assertTrue(state.canNavigateBack)
    }

    // ========== Dialog Actions Tests ==========

    @Test
    fun `showDialog sets activeDialog`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.showDialog(EditorDialog.COMPILE)

        val state = viewModel.state.first()
        assertEquals(EditorDialog.COMPILE, state.activeDialog)
        assertTrue(state.hasActiveDialog)
    }

    @Test
    fun `hideDialog clears activeDialog`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.showDialog(EditorDialog.OPEN_FILE)
        viewModel.hideDialog()

        val state = viewModel.state.first()
        assertEquals(EditorDialog.NONE, state.activeDialog)
        assertFalse(state.hasActiveDialog)
    }

    @Test
    fun `showFlowGraphProperties shows properties dialog`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.showFlowGraphProperties()

        assertEquals(EditorDialog.FLOW_GRAPH_PROPERTIES, viewModel.state.first().activeDialog)
    }

    // ========== Status Message Tests ==========

    @Test
    fun `setStatusMessage updates message`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.setStatusMessage("Custom message")

        assertEquals("Custom message", viewModel.state.first().statusMessage)
    }

    @Test
    fun `clearStatusMessage sets empty message`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.setStatusMessage("Something")
        viewModel.clearStatusMessage()

        assertEquals("", viewModel.state.first().statusMessage)
    }

    // ========== Graph State Tests ==========

    @Test
    fun `updateFlowGraphName updates name`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.updateFlowGraphName("MyFlowGraph")

        assertEquals("MyFlowGraph", viewModel.state.first().flowGraphName)
    }

    @Test
    fun `markDirty sets isDirty to true`() = runTest {
        val viewModel = GraphEditorViewModel()

        assertFalse(viewModel.state.first().isDirty)

        viewModel.markDirty()

        assertTrue(viewModel.state.first().isDirty)
    }

    @Test
    fun `markClean sets isDirty to false`() = runTest {
        val viewModel = GraphEditorViewModel()

        viewModel.markDirty()
        viewModel.markClean()

        assertFalse(viewModel.state.first().isDirty)
    }

    @Test
    fun `reset restores default state`() = runTest {
        val viewModel = GraphEditorViewModel()

        // Modify state
        viewModel.setStatusMessage("Modified")
        viewModel.showDialog(EditorDialog.COMPILE)
        viewModel.updateGroupingState(true, true)
        viewModel.markDirty()

        // Reset
        viewModel.reset()

        val state = viewModel.state.first()
        assertEquals("Ready - Create a new graph or open an existing one", state.statusMessage)
        assertEquals(EditorDialog.NONE, state.activeDialog)
        assertFalse(state.canGroup)
        assertFalse(state.canUngroup)
        assertFalse(state.isDirty)
    }
}
