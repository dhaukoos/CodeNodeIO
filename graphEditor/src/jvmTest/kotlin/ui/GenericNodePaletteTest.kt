/*
 * GenericNodePaletteTest - Integration Tests for Generic Nodes in Palette
 * Verifies generic node types appear correctly in the palette
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.factory.createGenericNodeType
import io.codenode.fbpdsl.factory.getAllGenericNodeTypes
import io.codenode.fbpdsl.factory.getCommonGenericNodeTypes
import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.Port
import io.codenode.grapheditor.createSampleNodeTypes
import kotlin.test.*

/**
 * Integration tests for generic nodes in the node palette.
 * Verifies T016-T017: Generic nodes appear in palette with correct configuration.
 */
class GenericNodePaletteTest {

    // ========== T017: Generic Nodes in Palette ==========

    @Test
    fun `createSampleNodeTypes includes common generic node types`() {
        val nodeTypes = createSampleNodeTypes()

        // Should contain the 7 common generic types
        val genericTypes = nodeTypes.filter {
            it.category == NodeTypeDefinition.NodeCategory.GENERIC
        }

        assertEquals(7, genericTypes.size, "Should include 7 common generic node types")
    }

    @Test
    fun `palette contains in0out1 generator type`() {
        val nodeTypes = createSampleNodeTypes()

        val generatorType = nodeTypes.find { it.name == "in0out1" }
        assertNotNull(generatorType, "Should contain in0out1 (Generator/Source)")
        assertEquals(NodeTypeDefinition.NodeCategory.GENERIC, generatorType.category)
        assertEquals(0, generatorType.getInputPortTemplates().size)
        assertEquals(1, generatorType.getOutputPortTemplates().size)
    }

    @Test
    fun `palette contains in1out0 sink type`() {
        val nodeTypes = createSampleNodeTypes()

        val sinkType = nodeTypes.find { it.name == "in1out0" }
        assertNotNull(sinkType, "Should contain in1out0 (Sink)")
        assertEquals(NodeTypeDefinition.NodeCategory.GENERIC, sinkType.category)
        assertEquals(1, sinkType.getInputPortTemplates().size)
        assertEquals(0, sinkType.getOutputPortTemplates().size)
    }

    @Test
    fun `palette contains in1out1 simple transformer type`() {
        val nodeTypes = createSampleNodeTypes()

        val transformerType = nodeTypes.find { it.name == "in1out1" }
        assertNotNull(transformerType, "Should contain in1out1 (Simple Transformer)")
        assertEquals(NodeTypeDefinition.NodeCategory.GENERIC, transformerType.category)
        assertEquals(1, transformerType.getInputPortTemplates().size)
        assertEquals(1, transformerType.getOutputPortTemplates().size)
    }

    @Test
    fun `palette contains in1out2 splitter type`() {
        val nodeTypes = createSampleNodeTypes()

        val splitterType = nodeTypes.find { it.name == "in1out2" }
        assertNotNull(splitterType, "Should contain in1out2 (Splitter)")
        assertEquals(NodeTypeDefinition.NodeCategory.GENERIC, splitterType.category)
        assertEquals(1, splitterType.getInputPortTemplates().size)
        assertEquals(2, splitterType.getOutputPortTemplates().size)
    }

    @Test
    fun `palette contains in2out1 merger type`() {
        val nodeTypes = createSampleNodeTypes()

        val mergerType = nodeTypes.find { it.name == "in2out1" }
        assertNotNull(mergerType, "Should contain in2out1 (Merger)")
        assertEquals(NodeTypeDefinition.NodeCategory.GENERIC, mergerType.category)
        assertEquals(2, mergerType.getInputPortTemplates().size)
        assertEquals(1, mergerType.getOutputPortTemplates().size)
    }

    @Test
    fun `palette contains in0out2 dual output generator type`() {
        val nodeTypes = createSampleNodeTypes()

        val generatorType = nodeTypes.find { it.name == "in0out2" }
        assertNotNull(generatorType, "Should contain in0out2 (Generator/Source dual output)")
        assertEquals(NodeTypeDefinition.NodeCategory.GENERIC, generatorType.category)
        assertEquals(0, generatorType.getInputPortTemplates().size)
        assertEquals(2, generatorType.getOutputPortTemplates().size)
    }

    @Test
    fun `palette contains in2out0 dual input sink type`() {
        val nodeTypes = createSampleNodeTypes()

        val sinkType = nodeTypes.find { it.name == "in2out0" }
        assertNotNull(sinkType, "Should contain in2out0 (Sink dual input)")
        assertEquals(NodeTypeDefinition.NodeCategory.GENERIC, sinkType.category)
        assertEquals(2, sinkType.getInputPortTemplates().size)
        assertEquals(0, sinkType.getOutputPortTemplates().size)
    }

    @Test
    fun `generic nodes can be grouped by GENERIC category`() {
        val nodeTypes = createSampleNodeTypes()

        val byCategory = nodeTypes.groupBy { it.category }

        assertTrue(
            byCategory.containsKey(NodeTypeDefinition.NodeCategory.GENERIC),
            "Should have GENERIC category in palette"
        )
        assertEquals(
            7,
            byCategory[NodeTypeDefinition.NodeCategory.GENERIC]?.size,
            "GENERIC category should have 7 nodes"
        )
    }

    @Test
    fun `generic nodes have correct port directions`() {
        val nodeTypes = createSampleNodeTypes()
        val genericTypes = nodeTypes.filter {
            it.category == NodeTypeDefinition.NodeCategory.GENERIC
        }

        for (nodeType in genericTypes) {
            val inputPorts = nodeType.getInputPortTemplates()
            val outputPorts = nodeType.getOutputPortTemplates()

            // All input ports should have INPUT direction
            assertTrue(
                inputPorts.all { it.direction == Port.Direction.INPUT },
                "All input ports should have INPUT direction for ${nodeType.name}"
            )

            // All output ports should have OUTPUT direction
            assertTrue(
                outputPorts.all { it.direction == Port.Direction.OUTPUT },
                "All output ports should have OUTPUT direction for ${nodeType.name}"
            )
        }
    }

    @Test
    fun `generic nodes have unique IDs`() {
        val nodeTypes = createSampleNodeTypes()
        val genericTypes = nodeTypes.filter {
            it.category == NodeTypeDefinition.NodeCategory.GENERIC
        }

        val ids = genericTypes.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Generic node IDs should be unique")
    }

    @Test
    fun `generic nodes have descriptions`() {
        val nodeTypes = createSampleNodeTypes()
        val genericTypes = nodeTypes.filter {
            it.category == NodeTypeDefinition.NodeCategory.GENERIC
        }

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
        val nodeTypes = createSampleNodeTypes()
        val genericTypes = nodeTypes.filter {
            it.category == NodeTypeDefinition.NodeCategory.GENERIC
        }

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
    fun `specialized node types still present in palette`() {
        val nodeTypes = createSampleNodeTypes()

        // Verify specialized types are still present
        assertTrue(
            nodeTypes.any { it.category == NodeTypeDefinition.NodeCategory.SERVICE },
            "Should still have SERVICE category nodes"
        )
        assertTrue(
            nodeTypes.any { it.category == NodeTypeDefinition.NodeCategory.TRANSFORMER },
            "Should still have TRANSFORMER category nodes"
        )
        assertTrue(
            nodeTypes.any { it.category == NodeTypeDefinition.NodeCategory.API_ENDPOINT },
            "Should still have API_ENDPOINT category nodes"
        )
        assertTrue(
            nodeTypes.any { it.category == NodeTypeDefinition.NodeCategory.DATABASE },
            "Should still have DATABASE category nodes"
        )
    }

    @Test
    fun `palette has both specialized and generic nodes`() {
        val nodeTypes = createSampleNodeTypes()

        // Should have specialized nodes (5 from the original list)
        val specializedCount = nodeTypes.count {
            it.category != NodeTypeDefinition.NodeCategory.GENERIC
        }
        assertEquals(5, specializedCount, "Should have 5 specialized node types")

        // Should have generic nodes (7 common types)
        val genericCount = nodeTypes.count {
            it.category == NodeTypeDefinition.NodeCategory.GENERIC
        }
        assertEquals(7, genericCount, "Should have 7 generic node types")

        // Total should be 12
        assertEquals(12, nodeTypes.size, "Should have 12 total node types in palette")
    }
}
