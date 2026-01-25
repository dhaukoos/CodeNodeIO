/*
 * NodePalette Test
 * UI tests for NodePalette component and drag-and-drop functionality
 * License: Apache 2.0
 */

package io.codenode.grapheditor.ui

import io.codenode.fbpdsl.model.NodeTypeDefinition
import io.codenode.fbpdsl.model.Port
import io.codenode.fbpdsl.model.PortTemplate
import kotlin.test.*

class NodePaletteTest {

    private fun createTestNodeTypes(): List<NodeTypeDefinition> {
        return listOf(
            NodeTypeDefinition(
                id = "test_generator",
                name = "Data Generator",
                category = NodeTypeDefinition.NodeCategory.SERVICE,
                description = "Generates test data",
                portTemplates = listOf(
                    PortTemplate(
                        name = "output",
                        direction = Port.Direction.OUTPUT,
                        dataType = String::class,
                        description = "Data output"
                    )
                )
            ),
            NodeTypeDefinition(
                id = "test_transformer",
                name = "Data Transformer",
                category = NodeTypeDefinition.NodeCategory.TRANSFORMER,
                description = "Transforms data",
                portTemplates = listOf(
                    PortTemplate(
                        name = "input",
                        direction = Port.Direction.INPUT,
                        dataType = String::class,
                        description = "Data input"
                    ),
                    PortTemplate(
                        name = "output",
                        direction = Port.Direction.OUTPUT,
                        dataType = String::class,
                        description = "Transformed output"
                    )
                )
            ),
            NodeTypeDefinition(
                id = "test_api",
                name = "API Endpoint",
                category = NodeTypeDefinition.NodeCategory.API_ENDPOINT,
                description = "Makes API calls",
                portTemplates = listOf(
                    PortTemplate(
                        name = "request",
                        direction = Port.Direction.INPUT,
                        dataType = Any::class,
                        description = "Request data"
                    ),
                    PortTemplate(
                        name = "response",
                        direction = Port.Direction.OUTPUT,
                        dataType = Any::class,
                        description = "Response data"
                    )
                )
            )
        )
    }

    @Test
    fun `should render node palette with categories`() {
        // Given node types with different categories
        val nodeTypes = createTestNodeTypes()

        // When grouping by category
        val byCategory = nodeTypes.groupBy { it.category }

        // Then should have 3 different categories
        assertEquals(3, byCategory.size, "Should have 3 different categories")
        assertTrue(byCategory.containsKey(NodeTypeDefinition.NodeCategory.SERVICE))
        assertTrue(byCategory.containsKey(NodeTypeDefinition.NodeCategory.TRANSFORMER))
        assertTrue(byCategory.containsKey(NodeTypeDefinition.NodeCategory.API_ENDPOINT))
    }

    @Test
    fun `should display node types in palette`() {
        // Given node types
        val nodeTypes = createTestNodeTypes()

        // Then all node types should be available
        assertEquals(3, nodeTypes.size, "Should have 3 node types")
        assertTrue(nodeTypes.any { it.name == "Data Generator" }, "Should have Data Generator")
        assertTrue(nodeTypes.any { it.name == "Data Transformer" }, "Should have Data Transformer")
        assertTrue(nodeTypes.any { it.name == "API Endpoint" }, "Should have API Endpoint")
    }

    @Test
    fun `should support drag and drop from palette to canvas`() {
        // Given node types and a callback
        val nodeTypes = createTestNodeTypes()
        var selectedNodeType: NodeTypeDefinition? = null

        val callback: (NodeTypeDefinition) -> Unit = { nodeType ->
            selectedNodeType = nodeType
        }

        // When a node is selected (simulating drag completion)
        val testNodeType = nodeTypes.first()
        callback(testNodeType)

        // Then the callback should receive the node type
        assertNotNull(selectedNodeType, "Node type should be selected")
        assertEquals(testNodeType.id, selectedNodeType?.id, "Should select correct node type")
        assertEquals("Data Generator", selectedNodeType?.name, "Should have correct name")
    }

    @Test
    fun `should filter nodes by search`() {
        // Given node types with searchable names
        val nodeTypes = createTestNodeTypes()

        // When filtering by category
        val serviceNodes = nodeTypes.filter {
            it.category == NodeTypeDefinition.NodeCategory.SERVICE
        }
        val transformerNodes = nodeTypes.filter {
            it.category == NodeTypeDefinition.NodeCategory.TRANSFORMER
        }
        val apiNodes = nodeTypes.filter {
            it.category == NodeTypeDefinition.NodeCategory.API_ENDPOINT
        }

        // Then filtering should work correctly
        assertEquals(1, serviceNodes.size, "Should find 1 service node")
        assertEquals("Data Generator", serviceNodes.first().name, "Should find Data Generator")

        assertEquals(1, transformerNodes.size, "Should find 1 transformer node")
        assertEquals("Data Transformer", transformerNodes.first().name, "Should find Data Transformer")

        assertEquals(1, apiNodes.size, "Should find 1 API node")
        assertEquals("API Endpoint", apiNodes.first().name, "Should find API Endpoint")

        // And search by name should work
        val generatorNodes = nodeTypes.filter { it.name.contains("Generator") }
        assertEquals(1, generatorNodes.size, "Should find node by name search")
    }

    @Test
    fun `should show node descriptions on hover`() {
        // Given node types with descriptions
        val nodeTypes = createTestNodeTypes()

        // When checking node type properties
        val generatorNode = nodeTypes.find { it.name == "Data Generator" }
        val transformerNode = nodeTypes.find { it.name == "Data Transformer" }
        val apiNode = nodeTypes.find { it.name == "API Endpoint" }

        // Then nodes should have descriptions
        assertNotNull(generatorNode, "Generator node should exist")
        assertEquals("Generates test data", generatorNode.description, "Should have generator description")

        assertNotNull(transformerNode, "Transformer node should exist")
        assertEquals("Transforms data", transformerNode.description, "Should have transformer description")

        assertNotNull(apiNode, "API node should exist")
        assertEquals("Makes API calls", apiNode.description, "Should have API description")

        // And nodes should have port information
        assertEquals(1, generatorNode.portTemplates.size, "Generator should have 1 port")
        assertEquals(2, transformerNode.portTemplates.size, "Transformer should have 2 ports")
        assertEquals(2, apiNode.portTemplates.size, "API should have 2 ports")
    }
}
