/*
 * GenericNodeConfigurationTest - Unit Tests for GenericNodeConfiguration
 * Tests validation rules, default naming, and configuration properties
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.factory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for GenericNodeConfiguration data class.
 * Tests validation, default naming patterns, and helper functions.
 */
class GenericNodeConfigurationTest {

    // ========== Validation Tests - Input Range ==========

    @Test
    fun `validate passes for valid numInputs range 0 to 5`() {
        for (inputs in 0..5) {
            val config = GenericNodeConfiguration(numInputs = inputs, numOutputs = 0)
            val result = config.validate()
            assertTrue(result.success, "numInputs=$inputs should be valid")
        }
    }

    @Test
    fun `validate passes for valid numOutputs range 0 to 5`() {
        for (outputs in 0..5) {
            val config = GenericNodeConfiguration(numInputs = 0, numOutputs = outputs)
            val result = config.validate()
            assertTrue(result.success, "numOutputs=$outputs should be valid")
        }
    }

    @Test
    fun `validate fails for negative numInputs`() {
        val config = GenericNodeConfiguration(numInputs = -1, numOutputs = 0)
        val result = config.validate()
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("numInputs") && it.contains("-1") })
    }

    @Test
    fun `validate fails for negative numOutputs`() {
        val config = GenericNodeConfiguration(numInputs = 0, numOutputs = -1)
        val result = config.validate()
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("numOutputs") && it.contains("-1") })
    }

    @Test
    fun `validate fails for numInputs greater than 5`() {
        val config = GenericNodeConfiguration(numInputs = 6, numOutputs = 0)
        val result = config.validate()
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("numInputs") && it.contains("6") })
    }

    @Test
    fun `validate fails for numOutputs greater than 5`() {
        val config = GenericNodeConfiguration(numInputs = 0, numOutputs = 6)
        val result = config.validate()
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("numOutputs") && it.contains("6") })
    }

    // ========== Validation Tests - Port Names ==========

    @Test
    fun `validate passes when inputNames size matches numInputs`() {
        val config = GenericNodeConfiguration(
            numInputs = 2,
            numOutputs = 0,
            inputNames = listOf("email", "password")
        )
        val result = config.validate()
        assertTrue(result.success)
    }

    @Test
    fun `validate passes when outputNames size matches numOutputs`() {
        val config = GenericNodeConfiguration(
            numInputs = 0,
            numOutputs = 3,
            outputNames = listOf("success", "warning", "error")
        )
        val result = config.validate()
        assertTrue(result.success)
    }

    @Test
    fun `validate fails when inputNames size does not match numInputs`() {
        val config = GenericNodeConfiguration(
            numInputs = 3,
            numOutputs = 0,
            inputNames = listOf("only_one")
        )
        val result = config.validate()
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("inputNames size") && it.contains("1") && it.contains("3") })
    }

    @Test
    fun `validate fails when outputNames size does not match numOutputs`() {
        val config = GenericNodeConfiguration(
            numInputs = 0,
            numOutputs = 2,
            outputNames = listOf("a", "b", "c", "d")
        )
        val result = config.validate()
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("outputNames size") && it.contains("4") && it.contains("2") })
    }

    @Test
    fun `validate fails when inputNames has duplicates`() {
        val config = GenericNodeConfiguration(
            numInputs = 3,
            numOutputs = 0,
            inputNames = listOf("data", "data", "other")
        )
        val result = config.validate()
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("duplicates") && it.contains("data") })
    }

    @Test
    fun `validate fails when outputNames has duplicates`() {
        val config = GenericNodeConfiguration(
            numInputs = 0,
            numOutputs = 2,
            outputNames = listOf("result", "result")
        )
        val result = config.validate()
        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("duplicates") && it.contains("result") })
    }

    // ========== Default Naming Tests ==========

    @Test
    fun `defaultName returns correct pattern for various combinations`() {
        assertEquals("in0out0", GenericNodeConfiguration(0, 0).defaultName())
        assertEquals("in1out0", GenericNodeConfiguration(1, 0).defaultName())
        assertEquals("in0out1", GenericNodeConfiguration(0, 1).defaultName())
        assertEquals("in1out1", GenericNodeConfiguration(1, 1).defaultName())
        assertEquals("in2out3", GenericNodeConfiguration(2, 3).defaultName())
        assertEquals("in5out5", GenericNodeConfiguration(5, 5).defaultName())
    }

    @Test
    fun `effectiveName returns customName when set`() {
        val config = GenericNodeConfiguration(
            numInputs = 1,
            numOutputs = 1,
            customName = "MyValidator"
        )
        assertEquals("MyValidator", config.effectiveName())
    }

    @Test
    fun `effectiveName returns defaultName when customName is null`() {
        val config = GenericNodeConfiguration(numInputs = 2, numOutputs = 1)
        assertEquals("in2out1", config.effectiveName())
    }

    // ========== Port Names Tests ==========

    @Test
    fun `defaultInputNames generates correct names`() {
        val config = GenericNodeConfiguration(numInputs = 3, numOutputs = 0)
        assertEquals(listOf("input1", "input2", "input3"), config.defaultInputNames())
    }

    @Test
    fun `defaultOutputNames generates correct names`() {
        val config = GenericNodeConfiguration(numInputs = 0, numOutputs = 2)
        assertEquals(listOf("output1", "output2"), config.defaultOutputNames())
    }

    @Test
    fun `defaultInputNames returns empty list for zero inputs`() {
        val config = GenericNodeConfiguration(numInputs = 0, numOutputs = 1)
        assertEquals(emptyList(), config.defaultInputNames())
    }

    @Test
    fun `defaultOutputNames returns empty list for zero outputs`() {
        val config = GenericNodeConfiguration(numInputs = 1, numOutputs = 0)
        assertEquals(emptyList(), config.defaultOutputNames())
    }

    @Test
    fun `effectiveInputNames returns custom names when provided`() {
        val config = GenericNodeConfiguration(
            numInputs = 2,
            numOutputs = 0,
            inputNames = listOf("email", "password")
        )
        assertEquals(listOf("email", "password"), config.effectiveInputNames())
    }

    @Test
    fun `effectiveInputNames returns default names when custom not provided`() {
        val config = GenericNodeConfiguration(numInputs = 2, numOutputs = 0)
        assertEquals(listOf("input1", "input2"), config.effectiveInputNames())
    }

    @Test
    fun `effectiveOutputNames returns custom names when provided`() {
        val config = GenericNodeConfiguration(
            numInputs = 0,
            numOutputs = 2,
            outputNames = listOf("valid", "invalid")
        )
        assertEquals(listOf("valid", "invalid"), config.effectiveOutputNames())
    }

    @Test
    fun `effectiveOutputNames returns default names when custom not provided`() {
        val config = GenericNodeConfiguration(numInputs = 0, numOutputs = 2)
        assertEquals(listOf("output1", "output2"), config.effectiveOutputNames())
    }

    // ========== ID Generation Tests ==========

    @Test
    fun `generateId returns pattern based on ports when no custom name`() {
        val config = GenericNodeConfiguration(numInputs = 2, numOutputs = 1)
        assertEquals("generic_in2_out1", config.generateId())
    }

    @Test
    fun `generateId derives from custom name when provided`() {
        val config = GenericNodeConfiguration(
            numInputs = 1,
            numOutputs = 1,
            customName = "Email Validator"
        )
        assertEquals("email_validator", config.generateId())
    }

    @Test
    fun `generateId sanitizes special characters`() {
        val config = GenericNodeConfiguration(
            numInputs = 1,
            numOutputs = 1,
            customName = "My-Node.v2!"
        )
        val id = config.generateId()
        assertTrue(id.all { it.isLetterOrDigit() || it == '_' })
    }

    // ========== Description Generation Tests ==========

    @Test
    fun `generateDescription handles singular and plural correctly`() {
        assertEquals(
            "Generic processing node with no inputs and no outputs",
            GenericNodeConfiguration(0, 0).generateDescription()
        )
        assertEquals(
            "Generic processing node with 1 input and 1 output",
            GenericNodeConfiguration(1, 1).generateDescription()
        )
        assertEquals(
            "Generic processing node with 2 inputs and 3 outputs",
            GenericNodeConfiguration(2, 3).generateDescription()
        )
    }

    @Test
    fun `generateDescription includes UseCase name when provided`() {
        val config = GenericNodeConfiguration(
            numInputs = 1,
            numOutputs = 1,
            useCaseClassName = "com.example.validators.EmailValidator"
        )
        val desc = config.generateDescription()
        assertTrue(desc.contains("EmailValidator"))
        assertTrue(desc.contains("UseCase"))
    }

    // ========== isValid Helper Tests ==========

    @Test
    fun `isValid returns true for valid configuration`() {
        val config = GenericNodeConfiguration(numInputs = 2, numOutputs = 1)
        assertTrue(config.isValid())
    }

    @Test
    fun `isValid returns false for invalid configuration`() {
        val config = GenericNodeConfiguration(numInputs = 6, numOutputs = 0)
        assertFalse(config.isValid())
    }

    // ========== Edge Cases ==========

    @Test
    fun `validate passes for in0out0 configuration`() {
        val config = GenericNodeConfiguration(numInputs = 0, numOutputs = 0)
        assertTrue(config.validate().success)
    }

    @Test
    fun `validate passes for in5out5 configuration`() {
        val config = GenericNodeConfiguration(numInputs = 5, numOutputs = 5)
        assertTrue(config.validate().success)
    }

    @Test
    fun `validate accumulates multiple errors`() {
        val config = GenericNodeConfiguration(
            numInputs = 6,
            numOutputs = -1,
            inputNames = listOf("a"),
            outputNames = listOf("b", "c")
        )
        val result = config.validate()
        assertFalse(result.success)
        assertTrue(result.errors.size >= 2, "Should have multiple errors")
    }

    @Test
    fun `empty custom names lists are valid when port count is zero`() {
        val config = GenericNodeConfiguration(
            numInputs = 0,
            numOutputs = 0,
            inputNames = emptyList(),
            outputNames = emptyList()
        )
        assertTrue(config.validate().success)
    }
}
