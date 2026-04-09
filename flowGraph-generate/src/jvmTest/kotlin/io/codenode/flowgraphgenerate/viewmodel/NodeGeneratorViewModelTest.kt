/*
 * NodeGeneratorViewModelTest - Unit tests for NodeGeneratorViewModel
 * Verifies state transitions and business logic without Compose UI dependencies
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.viewmodel

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeGeneratorViewModelTest {

    private fun createViewModel(): NodeGeneratorViewModel {
        return NodeGeneratorViewModel()
    }

    @Test
    fun `initial state has default values`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.state.first()

        assertEquals("", state.name)
        assertEquals(1, state.inputCount)
        assertEquals(1, state.outputCount)
        assertFalse(state.isExpanded)
        assertFalse(state.inputDropdownExpanded)
        assertFalse(state.outputDropdownExpanded)
    }

    @Test
    fun `setName updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setName("TestNode")

        val state = viewModel.state.first()
        assertEquals("TestNode", state.name)
    }

    @Test
    fun `setInputCount updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setInputCount(3)

        val state = viewModel.state.first()
        assertEquals(3, state.inputCount)
    }

    @Test
    fun `setInputCount coerces to valid range`() = runTest {
        val viewModel = createViewModel()

        viewModel.setInputCount(10)
        assertEquals(3, viewModel.state.first().inputCount)

        viewModel.setInputCount(-1)
        assertEquals(0, viewModel.state.first().inputCount)
    }

    @Test
    fun `setOutputCount updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setOutputCount(2)

        val state = viewModel.state.first()
        assertEquals(2, state.outputCount)
    }

    @Test
    fun `setOutputCount coerces to valid range`() = runTest {
        val viewModel = createViewModel()

        viewModel.setOutputCount(5)
        assertEquals(3, viewModel.state.first().outputCount)

        viewModel.setOutputCount(-2)
        assertEquals(0, viewModel.state.first().outputCount)
    }

    @Test
    fun `toggleExpanded flips expansion state`() = runTest {
        val viewModel = createViewModel()

        // Initially not expanded
        assertFalse(viewModel.state.first().isExpanded)

        // Toggle to expand
        viewModel.toggleExpanded()
        assertTrue(viewModel.state.first().isExpanded)

        // Toggle to collapse
        viewModel.toggleExpanded()
        assertFalse(viewModel.state.first().isExpanded)
    }

    @Test
    fun `setInputDropdownExpanded updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setInputDropdownExpanded(true)
        assertTrue(viewModel.state.first().inputDropdownExpanded)

        viewModel.setInputDropdownExpanded(false)
        assertFalse(viewModel.state.first().inputDropdownExpanded)
    }

    @Test
    fun `setOutputDropdownExpanded updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setOutputDropdownExpanded(true)
        assertTrue(viewModel.state.first().outputDropdownExpanded)

        viewModel.setOutputDropdownExpanded(false)
        assertFalse(viewModel.state.first().outputDropdownExpanded)
    }

    @Test
    fun `reset clears form but preserves expansion`() = runTest {
        val viewModel = createViewModel()

        // Set various state values
        viewModel.setName("MyNode")
        viewModel.setInputCount(3)
        viewModel.setOutputCount(2)
        viewModel.toggleExpanded() // Expand

        // Reset
        viewModel.reset()

        // Verify form is cleared but expansion preserved
        val state = viewModel.state.first()
        assertEquals("", state.name)
        assertEquals(1, state.inputCount)
        assertEquals(1, state.outputCount)
        assertTrue(state.isExpanded) // Expansion preserved
    }

    @Test
    fun `isValid returns true when name is not blank and has at least one port`() = runTest {
        val viewModel = createViewModel()

        // Initially invalid (empty name)
        assertFalse(viewModel.state.first().isValid)

        // Set a name with default ports (1 in, 1 out) - valid
        viewModel.setName("ValidNode")
        assertTrue(viewModel.state.first().isValid)

        // Clear name - invalid
        viewModel.setName("   ")
        assertFalse(viewModel.state.first().isValid)
    }

    @Test
    fun `isValid returns false when both port counts are zero`() = runTest {
        val viewModel = createViewModel()

        viewModel.setName("NoPortsNode")
        viewModel.setInputCount(0)
        viewModel.setOutputCount(0)

        assertFalse(viewModel.state.first().isValid)
    }

    @Test
    fun `genericType follows inXoutY pattern`() = runTest {
        val viewModel = createViewModel()

        assertEquals("in1out1", viewModel.state.first().genericType)

        viewModel.setInputCount(2)
        viewModel.setOutputCount(3)
        assertEquals("in2out3", viewModel.state.first().genericType)

        viewModel.setInputCount(0)
        viewModel.setOutputCount(1)
        assertEquals("in0out1", viewModel.state.first().genericType)
    }
}
