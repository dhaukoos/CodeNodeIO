/*
 * NodeGeneratorStateTest - Unit tests for NodeGeneratorState validation logic
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeGeneratorStateTest {

    @Test
    fun `default state has expected values`() {
        val state = NodeGeneratorState()
        assertEquals("", state.name)
        assertEquals(1, state.inputCount)
        assertEquals(1, state.outputCount)
    }

    @Test
    fun `isValid returns false when name is empty`() {
        val state = NodeGeneratorState(name = "", inputCount = 1, outputCount = 1)
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns false when name is whitespace only`() {
        val state = NodeGeneratorState(name = "   ", inputCount = 1, outputCount = 1)
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns false when both inputs and outputs are 0`() {
        val state = NodeGeneratorState(name = "TestNode", inputCount = 0, outputCount = 0)
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns true when name is non-blank and has at least one port`() {
        val state = NodeGeneratorState(name = "TestNode", inputCount = 1, outputCount = 1)
        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns true with 0 inputs and non-zero outputs`() {
        val state = NodeGeneratorState(name = "Generator", inputCount = 0, outputCount = 1)
        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns true with non-zero inputs and 0 outputs`() {
        val state = NodeGeneratorState(name = "Sink", inputCount = 1, outputCount = 0)
        assertTrue(state.isValid)
    }

    @Test
    fun `genericType format is correct`() {
        assertEquals("in1out1", NodeGeneratorState(inputCount = 1, outputCount = 1).genericType)
        assertEquals("in0out2", NodeGeneratorState(inputCount = 0, outputCount = 2).genericType)
        assertEquals("in3out0", NodeGeneratorState(inputCount = 3, outputCount = 0).genericType)
        assertEquals("in2out3", NodeGeneratorState(inputCount = 2, outputCount = 3).genericType)
    }

    @Test
    fun `reset returns default state`() {
        val state = NodeGeneratorState(name = "TestNode", inputCount = 2, outputCount = 3)
        val resetState = state.reset()
        assertEquals("", resetState.name)
        assertEquals(1, resetState.inputCount)
        assertEquals(1, resetState.outputCount)
    }

    @Test
    fun `withName returns new state with updated name`() {
        val state = NodeGeneratorState()
        val newState = state.withName("NewName")
        assertEquals("NewName", newState.name)
        assertEquals(state.inputCount, newState.inputCount)
        assertEquals(state.outputCount, newState.outputCount)
    }

    @Test
    fun `withInputCount coerces value to valid range`() {
        val state = NodeGeneratorState()
        assertEquals(0, state.withInputCount(-1).inputCount)
        assertEquals(0, state.withInputCount(0).inputCount)
        assertEquals(3, state.withInputCount(3).inputCount)
        assertEquals(3, state.withInputCount(5).inputCount)
    }

    @Test
    fun `withOutputCount coerces value to valid range`() {
        val state = NodeGeneratorState()
        assertEquals(0, state.withOutputCount(-1).outputCount)
        assertEquals(0, state.withOutputCount(0).outputCount)
        assertEquals(3, state.withOutputCount(3).outputCount)
        assertEquals(3, state.withOutputCount(5).outputCount)
    }

    @Test
    fun `all 15 valid input output combinations are valid`() {
        // Test all combinations except 0/0
        val validCombinations = listOf(
            0 to 1, 0 to 2, 0 to 3,
            1 to 0, 1 to 1, 1 to 2, 1 to 3,
            2 to 0, 2 to 1, 2 to 2, 2 to 3,
            3 to 0, 3 to 1, 3 to 2, 3 to 3
        )

        validCombinations.forEach { (inputs, outputs) ->
            val state = NodeGeneratorState(name = "Test", inputCount = inputs, outputCount = outputs)
            assertTrue(state.isValid, "Expected in${inputs}out${outputs} to be valid")
        }
    }

    @Test
    fun `0-0 combination is invalid`() {
        val state = NodeGeneratorState(name = "Test", inputCount = 0, outputCount = 0)
        assertFalse(state.isValid, "Expected in0out0 to be invalid")
    }
}
