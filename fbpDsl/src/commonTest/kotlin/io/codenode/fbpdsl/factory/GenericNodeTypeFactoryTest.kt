/*
 * GenericNodeTypeFactoryTest - Unit Tests for GenericNodeTypeFactory
 * TDD tests for creating NodeTypeDefinition instances for generic nodes
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.factory

import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.Port
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for GenericNodeTypeFactory.
 * Tests factory functions for creating generic NodeTypeDefinition instances.
 *
 * Test organization follows TDD task breakdown:
 * - T007: Valid input ranges (0-5)
 * - T008: Invalid inputs (negative, >5)
 * - T009: Default naming pattern "in{M}out{N}"
 * - T010: Port template generation
 * - T011: getAllGenericNodeTypes returns 36 combinations
 * - T012: getCommonGenericNodeTypes returns 5 common types
 */
class GenericNodeTypeFactoryTest {

    // ========== T007: Valid Input Range Tests (0-5) ==========

    @Test
    fun `createGenericNodeType accepts numInputs 0`() {
        val nodeType = createGenericNodeType(numInputs = 0, numOutputs = 1)
        assertEquals(0, nodeType.getInputPortTemplates().size)
    }

    @Test
    fun `createGenericNodeType accepts numInputs 5`() {
        val nodeType = createGenericNodeType(numInputs = 5, numOutputs = 0)
        assertEquals(5, nodeType.getInputPortTemplates().size)
    }

    @Test
    fun `createGenericNodeType accepts numOutputs 0`() {
        val nodeType = createGenericNodeType(numInputs = 1, numOutputs = 0)
        assertEquals(0, nodeType.getOutputPortTemplates().size)
    }

    @Test
    fun `createGenericNodeType accepts numOutputs 5`() {
        val nodeType = createGenericNodeType(numInputs = 0, numOutputs = 5)
        assertEquals(5, nodeType.getOutputPortTemplates().size)
    }

    @Test
    fun `createGenericNodeType accepts all valid combinations 0 to 5`() {
        for (inputs in 0..5) {
            for (outputs in 0..5) {
                val nodeType = createGenericNodeType(numInputs = inputs, numOutputs = outputs)
                assertEquals(inputs, nodeType.getInputPortTemplates().size, "Failed for inputs=$inputs")
                assertEquals(outputs, nodeType.getOutputPortTemplates().size, "Failed for outputs=$outputs")
            }
        }
    }

    @Test
    fun `createGenericNodeType returns category GENERIC`() {
        val nodeType = createGenericNodeType(numInputs = 1, numOutputs = 1)
        assertEquals(NodeTypeDefinition.NodeCategory.GENERIC, nodeType.category)
    }

    // ========== T008: Invalid Input Tests ==========

    @Test
    fun `createGenericNodeType throws for negative numInputs`() {
        assertFailsWith<IllegalArgumentException> {
            createGenericNodeType(numInputs = -1, numOutputs = 0)
        }
    }

    @Test
    fun `createGenericNodeType throws for negative numOutputs`() {
        assertFailsWith<IllegalArgumentException> {
            createGenericNodeType(numInputs = 0, numOutputs = -1)
        }
    }

    @Test
    fun `createGenericNodeType throws for numInputs greater than 5`() {
        assertFailsWith<IllegalArgumentException> {
            createGenericNodeType(numInputs = 6, numOutputs = 0)
        }
    }

    @Test
    fun `createGenericNodeType throws for numOutputs greater than 5`() {
        assertFailsWith<IllegalArgumentException> {
            createGenericNodeType(numInputs = 0, numOutputs = 6)
        }
    }

    @Test
    fun `createGenericNodeType throws for both inputs and outputs out of range`() {
        assertFailsWith<IllegalArgumentException> {
            createGenericNodeType(numInputs = 10, numOutputs = -5)
        }
    }

    // ========== T009: Default Naming Pattern Tests ==========

    @Test
    fun `createGenericNodeType uses default name in0out0`() {
        val nodeType = createGenericNodeType(numInputs = 0, numOutputs = 0)
        assertEquals("in0out0", nodeType.name)
    }

    @Test
    fun `createGenericNodeType uses default name in1out0`() {
        val nodeType = createGenericNodeType(numInputs = 1, numOutputs = 0)
        assertEquals("in1out0", nodeType.name)
    }

    @Test
    fun `createGenericNodeType uses default name in0out1`() {
        val nodeType = createGenericNodeType(numInputs = 0, numOutputs = 1)
        assertEquals("in0out1", nodeType.name)
    }

    @Test
    fun `createGenericNodeType uses default name in1out1`() {
        val nodeType = createGenericNodeType(numInputs = 1, numOutputs = 1)
        assertEquals("in1out1", nodeType.name)
    }

    @Test
    fun `createGenericNodeType uses default name in2out3`() {
        val nodeType = createGenericNodeType(numInputs = 2, numOutputs = 3)
        assertEquals("in2out3", nodeType.name)
    }

    @Test
    fun `createGenericNodeType uses default name in5out5`() {
        val nodeType = createGenericNodeType(numInputs = 5, numOutputs = 5)
        assertEquals("in5out5", nodeType.name)
    }

    @Test
    fun `createGenericNodeType generates correct id from default name`() {
        val nodeType = createGenericNodeType(numInputs = 2, numOutputs = 1)
        assertEquals("generic_in2_out1", nodeType.id)
    }

    @Test
    fun `createGenericNodeType generates description for default node`() {
        val nodeType = createGenericNodeType(numInputs = 2, numOutputs = 3)
        assertTrue(nodeType.description.contains("2 inputs"))
        assertTrue(nodeType.description.contains("3 outputs"))
    }

    // ========== T010: Port Template Generation Tests ==========

    @Test
    fun `createGenericNodeType generates input port templates with default names`() {
        val nodeType = createGenericNodeType(numInputs = 3, numOutputs = 0)
        val inputPorts = nodeType.getInputPortTemplates()
        assertEquals(3, inputPorts.size)
        assertEquals("input1", inputPorts[0].name)
        assertEquals("input2", inputPorts[1].name)
        assertEquals("input3", inputPorts[2].name)
    }

    @Test
    fun `createGenericNodeType generates output port templates with default names`() {
        val nodeType = createGenericNodeType(numInputs = 0, numOutputs = 2)
        val outputPorts = nodeType.getOutputPortTemplates()
        assertEquals(2, outputPorts.size)
        assertEquals("output1", outputPorts[0].name)
        assertEquals("output2", outputPorts[1].name)
    }

    @Test
    fun `createGenericNodeType generates no input ports for numInputs 0`() {
        val nodeType = createGenericNodeType(numInputs = 0, numOutputs = 1)
        assertEquals(emptyList(), nodeType.getInputPortTemplates())
    }

    @Test
    fun `createGenericNodeType generates no output ports for numOutputs 0`() {
        val nodeType = createGenericNodeType(numInputs = 1, numOutputs = 0)
        assertEquals(emptyList(), nodeType.getOutputPortTemplates())
    }

    @Test
    fun `createGenericNodeType port templates have INPUT direction for inputs`() {
        val nodeType = createGenericNodeType(numInputs = 2, numOutputs = 0)
        val inputPorts = nodeType.getInputPortTemplates()
        assertTrue(inputPorts.all { it.direction == Port.Direction.INPUT })
    }

    @Test
    fun `createGenericNodeType port templates have OUTPUT direction for outputs`() {
        val nodeType = createGenericNodeType(numInputs = 0, numOutputs = 2)
        val outputPorts = nodeType.getOutputPortTemplates()
        assertTrue(outputPorts.all { it.direction == Port.Direction.OUTPUT })
    }

    @Test
    fun `createGenericNodeType total ports equals numInputs plus numOutputs`() {
        val nodeType = createGenericNodeType(numInputs = 3, numOutputs = 2)
        assertEquals(5, nodeType.portTemplates.size)
    }

    @Test
    fun `createGenericNodeType ports are not required by default`() {
        val nodeType = createGenericNodeType(numInputs = 2, numOutputs = 2)
        assertTrue(nodeType.portTemplates.all { !it.required })
    }

    // ========== T011: getAllGenericNodeTypes Returns 36 Combinations ==========

    @Test
    fun `getAllGenericNodeTypes returns 36 node types`() {
        val allTypes = getAllGenericNodeTypes()
        assertEquals(36, allTypes.size)
    }

    @Test
    fun `getAllGenericNodeTypes contains all combinations from 0-5 inputs and 0-5 outputs`() {
        val allTypes = getAllGenericNodeTypes()
        val names = allTypes.map { it.name }.toSet()

        for (inputs in 0..5) {
            for (outputs in 0..5) {
                val expectedName = "in${inputs}out${outputs}"
                assertTrue(
                    names.contains(expectedName),
                    "Missing node type: $expectedName"
                )
            }
        }
    }

    @Test
    fun `getAllGenericNodeTypes has unique ids`() {
        val allTypes = getAllGenericNodeTypes()
        val ids = allTypes.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "IDs should be unique")
    }

    @Test
    fun `getAllGenericNodeTypes has unique names`() {
        val allTypes = getAllGenericNodeTypes()
        val names = allTypes.map { it.name }
        assertEquals(names.size, names.toSet().size, "Names should be unique")
    }

    @Test
    fun `getAllGenericNodeTypes all have GENERIC category`() {
        val allTypes = getAllGenericNodeTypes()
        assertTrue(allTypes.all { it.category == NodeTypeDefinition.NodeCategory.GENERIC })
    }

    @Test
    fun `getAllGenericNodeTypes returns same instance on multiple calls`() {
        val firstCall = getAllGenericNodeTypes()
        val secondCall = getAllGenericNodeTypes()
        // Should return the same cached list
        assertTrue(firstCall === secondCall, "Should return cached list")
    }

    // ========== T012: getCommonGenericNodeTypes Returns 5 Common Types ==========

    @Test
    fun `getCommonGenericNodeTypes returns 5 node types`() {
        val commonTypes = getCommonGenericNodeTypes()
        assertEquals(5, commonTypes.size)
    }

    @Test
    fun `getCommonGenericNodeTypes contains in0out1 generator`() {
        val commonTypes = getCommonGenericNodeTypes()
        assertTrue(commonTypes.any { it.name == "in0out1" }, "Should contain in0out1 (Generator/Source)")
    }

    @Test
    fun `getCommonGenericNodeTypes contains in1out0 sink`() {
        val commonTypes = getCommonGenericNodeTypes()
        assertTrue(commonTypes.any { it.name == "in1out0" }, "Should contain in1out0 (Sink)")
    }

    @Test
    fun `getCommonGenericNodeTypes contains in1out1 simple transformer`() {
        val commonTypes = getCommonGenericNodeTypes()
        assertTrue(commonTypes.any { it.name == "in1out1" }, "Should contain in1out1 (Simple Transformer)")
    }

    @Test
    fun `getCommonGenericNodeTypes contains in1out2 splitter`() {
        val commonTypes = getCommonGenericNodeTypes()
        assertTrue(commonTypes.any { it.name == "in1out2" }, "Should contain in1out2 (Splitter)")
    }

    @Test
    fun `getCommonGenericNodeTypes contains in2out1 merger`() {
        val commonTypes = getCommonGenericNodeTypes()
        assertTrue(commonTypes.any { it.name == "in2out1" }, "Should contain in2out1 (Merger)")
    }

    @Test
    fun `getCommonGenericNodeTypes all have GENERIC category`() {
        val commonTypes = getCommonGenericNodeTypes()
        assertTrue(commonTypes.all { it.category == NodeTypeDefinition.NodeCategory.GENERIC })
    }

    @Test
    fun `getCommonGenericNodeTypes is subset of getAllGenericNodeTypes`() {
        val allTypes = getAllGenericNodeTypes()
        val commonTypes = getCommonGenericNodeTypes()
        val allIds = allTypes.map { it.id }.toSet()
        assertTrue(commonTypes.all { allIds.contains(it.id) }, "Common types should be subset of all types")
    }

    // ========== T019: Custom Name Override Tests ==========

    @Test
    fun `createGenericNodeType uses customName when provided`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 1,
            customName = "MyValidator"
        )
        assertEquals("MyValidator", nodeType.name)
    }

    @Test
    fun `createGenericNodeType generates id from customName`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 1,
            customName = "Email Validator"
        )
        assertEquals("email_validator", nodeType.id)
    }

    @Test
    fun `createGenericNodeType sanitizes customName for id`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 1,
            customName = "My-Node.v2!"
        )
        // ID should only contain alphanumeric and underscore
        assertTrue(nodeType.id.all { it.isLetterOrDigit() || it == '_' })
    }

    @Test
    fun `createGenericNodeType uses customDescription when provided`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 1,
            customDescription = "Validates email addresses"
        )
        assertEquals("Validates email addresses", nodeType.description)
    }

    @Test
    fun `createGenericNodeType uses default description when customDescription is null`() {
        val nodeType = createGenericNodeType(numInputs = 2, numOutputs = 1)
        assertTrue(nodeType.description.contains("Generic processing node"))
    }

    // ========== T020: Custom Port Names Tests ==========

    @Test
    fun `createGenericNodeType uses custom inputNames when provided`() {
        val nodeType = createGenericNodeType(
            numInputs = 2,
            numOutputs = 0,
            inputNames = listOf("email", "password")
        )
        val inputPorts = nodeType.getInputPortTemplates()
        assertEquals(2, inputPorts.size)
        assertEquals("email", inputPorts[0].name)
        assertEquals("password", inputPorts[1].name)
    }

    @Test
    fun `createGenericNodeType uses custom outputNames when provided`() {
        val nodeType = createGenericNodeType(
            numInputs = 0,
            numOutputs = 3,
            outputNames = listOf("success", "warning", "error")
        )
        val outputPorts = nodeType.getOutputPortTemplates()
        assertEquals(3, outputPorts.size)
        assertEquals("success", outputPorts[0].name)
        assertEquals("warning", outputPorts[1].name)
        assertEquals("error", outputPorts[2].name)
    }

    @Test
    fun `createGenericNodeType uses default inputNames when custom not provided`() {
        val nodeType = createGenericNodeType(numInputs = 2, numOutputs = 0)
        val inputPorts = nodeType.getInputPortTemplates()
        assertEquals("input1", inputPorts[0].name)
        assertEquals("input2", inputPorts[1].name)
    }

    @Test
    fun `createGenericNodeType uses default outputNames when custom not provided`() {
        val nodeType = createGenericNodeType(numInputs = 0, numOutputs = 2)
        val outputPorts = nodeType.getOutputPortTemplates()
        assertEquals("output1", outputPorts[0].name)
        assertEquals("output2", outputPorts[1].name)
    }

    @Test
    fun `createGenericNodeType can mix custom and default port names`() {
        val nodeType = createGenericNodeType(
            numInputs = 2,
            numOutputs = 2,
            inputNames = listOf("data", "config"),
            outputNames = null  // Use default output names
        )
        val inputPorts = nodeType.getInputPortTemplates()
        val outputPorts = nodeType.getOutputPortTemplates()

        assertEquals("data", inputPorts[0].name)
        assertEquals("config", inputPorts[1].name)
        assertEquals("output1", outputPorts[0].name)
        assertEquals("output2", outputPorts[1].name)
    }

    // ========== T021: iconResource Parameter Tests ==========

    @Test
    fun `createGenericNodeType includes iconResource in defaultConfiguration`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 1,
            iconResource = "icons/my-custom-node.svg"
        )
        assertEquals("icons/my-custom-node.svg", nodeType.defaultConfiguration["_iconResource"])
    }

    @Test
    fun `createGenericNodeType omits iconResource when null`() {
        val nodeType = createGenericNodeType(numInputs = 1, numOutputs = 1)
        assertTrue(!nodeType.defaultConfiguration.containsKey("_iconResource"))
    }

    @Test
    fun `createGenericNodeType iconResource can be any path format`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 1,
            iconResource = "/absolute/path/to/icon.png"
        )
        assertEquals("/absolute/path/to/icon.png", nodeType.defaultConfiguration["_iconResource"])
    }

    // ========== T022: useCaseClassName Parameter Tests ==========

    @Test
    fun `createGenericNodeType includes useCaseClassName in defaultConfiguration`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 1,
            useCaseClassName = "com.example.validators.EmailValidator"
        )
        assertEquals(
            "com.example.validators.EmailValidator",
            nodeType.defaultConfiguration["_useCaseClass"]
        )
    }

    @Test
    fun `createGenericNodeType omits useCaseClass when null`() {
        val nodeType = createGenericNodeType(numInputs = 1, numOutputs = 1)
        assertTrue(!nodeType.defaultConfiguration.containsKey("_useCaseClass"))
    }

    @Test
    fun `createGenericNodeType description includes UseCase name when provided`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 1,
            useCaseClassName = "com.example.validators.EmailValidator"
        )
        assertTrue(nodeType.description.contains("EmailValidator"))
        assertTrue(nodeType.description.contains("UseCase"))
    }

    @Test
    fun `createGenericNodeType code template references UseCase when provided`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 1,
            useCaseClassName = "com.example.MyUseCase"
        )
        val kmpTemplate = nodeType.getCodeTemplate("KMP")
        assertTrue(kmpTemplate != null)
        assertTrue(kmpTemplate!!.contains("com.example.MyUseCase"))
    }

    @Test
    fun `createGenericNodeType code template has TODO when no UseCase`() {
        val nodeType = createGenericNodeType(numInputs = 1, numOutputs = 1)
        val kmpTemplate = nodeType.getCodeTemplate("KMP")
        assertTrue(kmpTemplate != null)
        assertTrue(kmpTemplate!!.contains("TODO"))
    }

    // ========== T023: Port Name Count Mismatch Validation Tests ==========

    @Test
    fun `createGenericNodeType throws when inputNames size does not match numInputs`() {
        assertFailsWith<IllegalArgumentException> {
            createGenericNodeType(
                numInputs = 3,
                numOutputs = 0,
                inputNames = listOf("only_one")
            )
        }
    }

    @Test
    fun `createGenericNodeType throws when outputNames size does not match numOutputs`() {
        assertFailsWith<IllegalArgumentException> {
            createGenericNodeType(
                numInputs = 0,
                numOutputs = 2,
                outputNames = listOf("a", "b", "c", "d")
            )
        }
    }

    @Test
    fun `createGenericNodeType throws when inputNames has more than numInputs`() {
        assertFailsWith<IllegalArgumentException> {
            createGenericNodeType(
                numInputs = 1,
                numOutputs = 0,
                inputNames = listOf("a", "b", "c")
            )
        }
    }

    @Test
    fun `createGenericNodeType throws when outputNames has fewer than numOutputs`() {
        assertFailsWith<IllegalArgumentException> {
            createGenericNodeType(
                numInputs = 0,
                numOutputs = 3,
                outputNames = listOf("only_one")
            )
        }
    }

    @Test
    fun `createGenericNodeType accepts empty inputNames when numInputs is 0`() {
        val nodeType = createGenericNodeType(
            numInputs = 0,
            numOutputs = 1,
            inputNames = emptyList()
        )
        assertEquals(0, nodeType.getInputPortTemplates().size)
    }

    @Test
    fun `createGenericNodeType accepts empty outputNames when numOutputs is 0`() {
        val nodeType = createGenericNodeType(
            numInputs = 1,
            numOutputs = 0,
            outputNames = emptyList()
        )
        assertEquals(0, nodeType.getOutputPortTemplates().size)
    }
}
