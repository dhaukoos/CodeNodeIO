/*
 * CustomNodeDefinitionTest - Unit tests for CustomNodeDefinition
 * License: Apache 2.0
 */

package io.codenode.grapheditor.repository

import io.codenode.fbpdsl.model.NodeTypeDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CustomNodeDefinitionTest {

    @Test
    fun `create generates unique IDs`() {
        val node1 = CustomNodeDefinition.create("Node1", 1, 1)
        val node2 = CustomNodeDefinition.create("Node2", 1, 1)
        assertNotEquals(node1.id, node2.id)
    }

    @Test
    fun `create sets correct name`() {
        val node = CustomNodeDefinition.create("MyNode", 2, 1)
        assertEquals("MyNode", node.name)
    }

    @Test
    fun `create sets correct input and output counts`() {
        val node = CustomNodeDefinition.create("TestNode", 2, 3)
        assertEquals(2, node.inputCount)
        assertEquals(3, node.outputCount)
    }

    @Test
    fun `create generates correct genericType`() {
        val node = CustomNodeDefinition.create("TestNode", 2, 1)
        assertEquals("in2out1", node.genericType)
    }

    @Test
    fun `create sets timestamp`() {
        val beforeCreate = System.currentTimeMillis()
        val node = CustomNodeDefinition.create("TestNode", 1, 1)
        val afterCreate = System.currentTimeMillis()
        assertTrue(node.createdAt >= beforeCreate)
        assertTrue(node.createdAt <= afterCreate)
    }

    @Test
    fun `create generates id with custom_node prefix`() {
        val node = CustomNodeDefinition.create("TestNode", 1, 1)
        assertTrue(node.id.startsWith("custom_node_"))
    }

    @Test
    fun `toNodeTypeDefinition returns correct category`() {
        val node = CustomNodeDefinition.create("TestNode", 2, 1)
        val nodeType = node.toNodeTypeDefinition()
        assertEquals(NodeTypeDefinition.NodeCategory.GENERIC, nodeType.category)
    }

    @Test
    fun `toNodeTypeDefinition returns correct name`() {
        val node = CustomNodeDefinition.create("DataMerger", 2, 1)
        val nodeType = node.toNodeTypeDefinition()
        assertEquals("DataMerger", nodeType.name)
    }

    @Test
    fun `toNodeTypeDefinition returns correct input port count`() {
        val node = CustomNodeDefinition.create("TestNode", 2, 1)
        val nodeType = node.toNodeTypeDefinition()
        assertEquals(2, nodeType.getInputPortTemplates().size)
    }

    @Test
    fun `toNodeTypeDefinition returns correct output port count`() {
        val node = CustomNodeDefinition.create("TestNode", 2, 3)
        val nodeType = node.toNodeTypeDefinition()
        assertEquals(3, nodeType.getOutputPortTemplates().size)
    }

    @Test
    fun `toNodeTypeDefinition works for generator (0 inputs)`() {
        val node = CustomNodeDefinition.create("Generator", 0, 2)
        val nodeType = node.toNodeTypeDefinition()
        assertEquals(0, nodeType.getInputPortTemplates().size)
        assertEquals(2, nodeType.getOutputPortTemplates().size)
    }

    @Test
    fun `toNodeTypeDefinition works for sink (0 outputs)`() {
        val node = CustomNodeDefinition.create("Sink", 2, 0)
        val nodeType = node.toNodeTypeDefinition()
        assertEquals(2, nodeType.getInputPortTemplates().size)
        assertEquals(0, nodeType.getOutputPortTemplates().size)
    }
}
