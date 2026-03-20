/*
 * GenericNodePaletteTest - Tests for Generic Node Types
 * Verifies generic node types are correctly configured for palette display
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.factory.createGenericNodeType
import io.codenode.fbpdsl.factory.getCommonGenericNodeTypes
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.Port
import kotlin.test.*

/**
 * Tests for generic node type definitions used in the palette.
 * Verifies generic nodes have correct port configuration and metadata.
 */
class GenericNodePaletteTest {

    @Test
    fun `getCommonGenericNodeTypes returns 7 types`() {
        val genericTypes = getCommonGenericNodeTypes()
        assertEquals(7, genericTypes.size, "Should include 7 common generic node types")
    }

    @Test
    fun `common types contain in0out1 generator type`() {
        val nodeTypes = getCommonGenericNodeTypes()

        val generatorType = nodeTypes.find { it.name == "in0out1" }
        assertNotNull(generatorType, "Should contain in0out1 (Generator/Source)")
        assertEquals(CodeNodeType.TRANSFORMER, generatorType.category)
        assertEquals(0, generatorType.getInputPortTemplates().size)
        assertEquals(1, generatorType.getOutputPortTemplates().size)
    }

    @Test
    fun `common types contain in1out0 sink type`() {
        val nodeTypes = getCommonGenericNodeTypes()

        val sinkType = nodeTypes.find { it.name == "in1out0" }
        assertNotNull(sinkType, "Should contain in1out0 (Sink)")
        assertEquals(CodeNodeType.TRANSFORMER, sinkType.category)
        assertEquals(1, sinkType.getInputPortTemplates().size)
        assertEquals(0, sinkType.getOutputPortTemplates().size)
    }

    @Test
    fun `common types contain in1out1 simple transformer type`() {
        val nodeTypes = getCommonGenericNodeTypes()

        val transformerType = nodeTypes.find { it.name == "in1out1" }
        assertNotNull(transformerType, "Should contain in1out1 (Simple Transformer)")
        assertEquals(CodeNodeType.TRANSFORMER, transformerType.category)
        assertEquals(1, transformerType.getInputPortTemplates().size)
        assertEquals(1, transformerType.getOutputPortTemplates().size)
    }

    @Test
    fun `common types contain in1out2 splitter type`() {
        val nodeTypes = getCommonGenericNodeTypes()

        val splitterType = nodeTypes.find { it.name == "in1out2" }
        assertNotNull(splitterType, "Should contain in1out2 (Splitter)")
        assertEquals(CodeNodeType.TRANSFORMER, splitterType.category)
        assertEquals(1, splitterType.getInputPortTemplates().size)
        assertEquals(2, splitterType.getOutputPortTemplates().size)
    }

    @Test
    fun `common types contain in2out1 merger type`() {
        val nodeTypes = getCommonGenericNodeTypes()

        val mergerType = nodeTypes.find { it.name == "in2out1" }
        assertNotNull(mergerType, "Should contain in2out1 (Merger)")
        assertEquals(CodeNodeType.TRANSFORMER, mergerType.category)
        assertEquals(2, mergerType.getInputPortTemplates().size)
        assertEquals(1, mergerType.getOutputPortTemplates().size)
    }

    @Test
    fun `common types contain in0out2 dual output generator type`() {
        val nodeTypes = getCommonGenericNodeTypes()

        val generatorType = nodeTypes.find { it.name == "in0out2" }
        assertNotNull(generatorType, "Should contain in0out2 (Generator/Source dual output)")
        assertEquals(CodeNodeType.TRANSFORMER, generatorType.category)
        assertEquals(0, generatorType.getInputPortTemplates().size)
        assertEquals(2, generatorType.getOutputPortTemplates().size)
    }

    @Test
    fun `common types contain in2out0 dual input sink type`() {
        val nodeTypes = getCommonGenericNodeTypes()

        val sinkType = nodeTypes.find { it.name == "in2out0" }
        assertNotNull(sinkType, "Should contain in2out0 (Sink dual input)")
        assertEquals(CodeNodeType.TRANSFORMER, sinkType.category)
        assertEquals(2, sinkType.getInputPortTemplates().size)
        assertEquals(0, sinkType.getOutputPortTemplates().size)
    }

    @Test
    fun `generic nodes have correct port directions`() {
        val genericTypes = getCommonGenericNodeTypes()

        for (nodeType in genericTypes) {
            val inputPorts = nodeType.getInputPortTemplates()
            val outputPorts = nodeType.getOutputPortTemplates()

            assertTrue(
                inputPorts.all { it.direction == Port.Direction.INPUT },
                "All input ports should have INPUT direction for ${nodeType.name}"
            )

            assertTrue(
                outputPorts.all { it.direction == Port.Direction.OUTPUT },
                "All output ports should have OUTPUT direction for ${nodeType.name}"
            )
        }
    }

    @Test
    fun `generic nodes have unique IDs`() {
        val genericTypes = getCommonGenericNodeTypes()

        val ids = genericTypes.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Generic node IDs should be unique")
    }

    @Test
    fun `generic nodes have descriptions`() {
        val genericTypes = getCommonGenericNodeTypes()

        for (nodeType in genericTypes) {
            assertTrue(
                nodeType.description.isNotBlank(),
                "Generic node ${nodeType.name} should have description"
            )
            assertTrue(
                nodeType.description.contains("Generic processing node"),
                "Description should indicate it's a generic node: ${nodeType.description}"
            )
        }
    }

    @Test
    fun `generic nodes have defaultConfiguration with _genericType`() {
        val genericTypes = getCommonGenericNodeTypes()

        for (nodeType in genericTypes) {
            assertTrue(
                nodeType.defaultConfiguration.containsKey("_genericType"),
                "Generic node ${nodeType.name} should have _genericType in defaultConfiguration"
            )
            assertEquals(
                nodeType.name,
                nodeType.defaultConfiguration["_genericType"],
                "_genericType should match node name"
            )
        }
    }

    @Test
    fun `all generic nodes use TRANSFORMER category`() {
        val genericTypes = getCommonGenericNodeTypes()

        assertTrue(
            genericTypes.all { it.category == CodeNodeType.TRANSFORMER },
            "All generic nodes should use TRANSFORMER category"
        )
    }
}
