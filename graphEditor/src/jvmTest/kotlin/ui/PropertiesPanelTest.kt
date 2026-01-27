/*
 * PropertiesPanel Test
 * UI tests for the PropertiesPanel component
 * License: Apache 2.0
 */

package ui

import io.codenode.fbpdsl.model.*
import io.codenode.grapheditor.ui.*
import kotlin.test.*

/**
 * TDD tests for PropertiesPanel UI component.
 *
 * These tests verify that the PropertiesPanel:
 * - Displays properties for selected nodes
 * - Supports different property types (text, number, boolean, dropdown)
 * - Validates property values
 * - Notifies listeners of property changes
 * - Shows/hides based on node selection
 *
 * Note: These tests are designed to FAIL initially (TDD Red phase)
 * until PropertiesPanel is implemented.
 */
class PropertiesPanelTest {

    // ============================================
    // Panel State Tests
    // ============================================

    @Test
    fun `should be empty when no node is selected`() {
        // Given a properties panel state with no selection
        val state = PropertiesPanelState()

        // Then panel should show empty state
        assertNull(state.selectedNode, "No node should be selected")
        assertTrue(state.properties.isEmpty(), "Properties should be empty")
        assertTrue(state.isEmptyState, "Should indicate empty state")
    }

    @Test
    fun `should display node properties when node is selected`() {
        // Given a node with configuration
        val node = createTestNode(
            configuration = mapOf(
                "timeout" to "30",
                "retries" to "3",
                "enabled" to "true"
            )
        )

        // When selecting the node
        val state = PropertiesPanelState(selectedNode = node)

        // Then properties should be displayed
        assertNotNull(state.selectedNode)
        assertEquals(3, state.properties.size)
        assertTrue(state.properties.containsKey("timeout"))
        assertTrue(state.properties.containsKey("retries"))
        assertTrue(state.properties.containsKey("enabled"))
    }

    @Test
    fun `should update properties when different node is selected`() {
        // Given initial state with node A
        val nodeA = createTestNode("nodeA", mapOf("propA" to "valueA"))
        var state = PropertiesPanelState(selectedNode = nodeA)

        // When selecting node B
        val nodeB = createTestNode("nodeB", mapOf("propB" to "valueB"))
        state = state.selectNode(nodeB)

        // Then properties should update to node B
        assertEquals("nodeB", state.selectedNode?.id)
        assertTrue(state.properties.containsKey("propB"))
        assertFalse(state.properties.containsKey("propA"))
    }

    @Test
    fun `should clear properties when selection is cleared`() {
        // Given state with selected node
        val node = createTestNode(configuration = mapOf("key" to "value"))
        var state = PropertiesPanelState(selectedNode = node)

        // When clearing selection
        state = state.clearSelection()

        // Then properties should be empty
        assertNull(state.selectedNode)
        assertTrue(state.properties.isEmpty())
    }

    // ============================================
    // Property Editor Tests
    // ============================================

    @Test
    fun `should determine correct editor type for property`() {
        // Given property definitions with different types
        val propertyDefs = listOf(
            PropertyDefinition("name", PropertyType.STRING),
            PropertyDefinition("count", PropertyType.NUMBER),
            PropertyDefinition("enabled", PropertyType.BOOLEAN),
            PropertyDefinition("method", PropertyType.DROPDOWN, options = listOf("GET", "POST", "PUT"))
        )

        // When determining editor types
        // Then should match correct types
        assertEquals(EditorType.TEXT_FIELD, propertyDefs[0].editorType)
        assertEquals(EditorType.NUMBER_FIELD, propertyDefs[1].editorType)
        assertEquals(EditorType.CHECKBOX, propertyDefs[2].editorType)
        assertEquals(EditorType.DROPDOWN, propertyDefs[3].editorType)
    }

    @Test
    fun `should provide dropdown options for enum properties`() {
        // Given a dropdown property
        val propertyDef = PropertyDefinition(
            name = "httpMethod",
            type = PropertyType.DROPDOWN,
            options = listOf("GET", "POST", "PUT", "DELETE")
        )

        // Then should have correct options
        assertEquals(4, propertyDef.options.size)
        assertTrue(propertyDef.options.contains("GET"))
        assertTrue(propertyDef.options.contains("DELETE"))
    }

    // ============================================
    // Property Change Tests
    // ============================================

    @Test
    fun `should notify listeners when property value changes`() {
        // Given a properties panel state with listener
        var changedKey: String? = null
        var changedValue: String? = null

        val node = createTestNode(configuration = mapOf("timeout" to "30"))
        val state = PropertiesPanelState(
            selectedNode = node,
            onPropertyChanged = { key, value ->
                changedKey = key
                changedValue = value
            }
        )

        // When changing a property
        state.updateProperty("timeout", "60")

        // Then listener should be notified
        assertEquals("timeout", changedKey)
        assertEquals("60", changedValue)
    }

    @Test
    fun `should update local state when property changes`() {
        // Given a properties panel state
        val node = createTestNode(configuration = mapOf("timeout" to "30"))
        var state = PropertiesPanelState(selectedNode = node)

        // When changing a property
        state = state.withPropertyValue("timeout", "60")

        // Then local state should update
        assertEquals("60", state.properties["timeout"])
    }

    @Test
    fun `should track dirty state when properties are modified`() {
        // Given a properties panel with original values
        val node = createTestNode(configuration = mapOf("timeout" to "30"))
        var state = PropertiesPanelState(selectedNode = node)

        // Initially should not be dirty
        assertFalse(state.isDirty, "Should not be dirty initially")

        // When changing a property
        state = state.withPropertyValue("timeout", "60")

        // Then should be dirty
        assertTrue(state.isDirty, "Should be dirty after change")
    }

    @Test
    fun `should reset dirty state when changes are saved`() {
        // Given dirty state
        val node = createTestNode(configuration = mapOf("timeout" to "30"))
        var state = PropertiesPanelState(selectedNode = node)
        state = state.withPropertyValue("timeout", "60")
        assertTrue(state.isDirty)

        // When saving changes
        state = state.markSaved()

        // Then should not be dirty
        assertFalse(state.isDirty)
    }

    // ============================================
    // Validation Tests
    // ============================================

    @Test
    fun `should validate required properties`() {
        // Given a state with required property definition
        val node = createTestNode()
        val state = PropertiesPanelState(
            selectedNode = node,
            propertyDefinitions = listOf(
                PropertyDefinition("url", PropertyType.STRING, required = true)
            )
        )

        // When validating with empty required field
        val validation = state.withPropertyValue("url", "").validate()

        // Then should have validation error
        assertFalse(validation.isValid)
        assertTrue(validation.validationErrors.containsKey("url"))
    }

    @Test
    fun `should validate number range constraints`() {
        // Given a state with number range constraint
        val node = createTestNode()
        val state = PropertiesPanelState(
            selectedNode = node,
            propertyDefinitions = listOf(
                PropertyDefinition(
                    "timeout",
                    PropertyType.NUMBER,
                    minValue = 1.0,
                    maxValue = 120.0
                )
            )
        )

        // When validating out-of-range value
        val validation = state.withPropertyValue("timeout", "200").validate()

        // Then should have validation error
        assertFalse(validation.isValid)
        assertTrue(validation.validationErrors.containsKey("timeout"))
    }

    @Test
    fun `should validate pattern constraints`() {
        // Given a state with pattern constraint
        val node = createTestNode()
        val state = PropertiesPanelState(
            selectedNode = node,
            propertyDefinitions = listOf(
                PropertyDefinition(
                    "email",
                    PropertyType.STRING,
                    pattern = "^[\\w.-]+@[\\w.-]+\\.\\w+$"
                )
            )
        )

        // When validating invalid email
        val invalidValidation = state.withPropertyValue("email", "not-an-email").validate()
        assertFalse(invalidValidation.isValid)

        // And when validating valid email
        val validValidation = state.withPropertyValue("email", "test@example.com").validate()
        assertTrue(validValidation.isValid)
    }

    @Test
    fun `should show validation errors in UI state`() {
        // Given a state with validation errors
        val node = createTestNode()
        var state = PropertiesPanelState(
            selectedNode = node,
            propertyDefinitions = listOf(
                PropertyDefinition("url", PropertyType.STRING, required = true)
            )
        )

        // When validating with errors
        state = state.withPropertyValue("url", "").validate()

        // Then UI should show error state
        assertTrue(state.hasValidationErrors)
        assertNotNull(state.getErrorForProperty("url"))
    }

    // ============================================
    // Node Type Integration Tests
    // ============================================

    @Test
    fun `should derive property definitions from NodeTypeDefinition`() {
        // Given a NodeTypeDefinition with configuration schema
        val nodeTypeDef = NodeTypeDefinition(
            id = "nodeType_http",
            name = "HTTP Request",
            category = NodeTypeDefinition.NodeCategory.API_ENDPOINT,
            description = "HTTP client node",
            defaultConfiguration = mapOf(
                "url" to "",
                "method" to "GET",
                "timeout" to "30"
            ),
            configurationSchema = """
                {
                    "type": "object",
                    "properties": {
                        "url": {"type": "string"},
                        "method": {"type": "string", "enum": ["GET", "POST", "PUT", "DELETE"]},
                        "timeout": {"type": "integer", "minimum": 1, "maximum": 120}
                    },
                    "required": ["url"]
                }
            """.trimIndent()
        )

        // When deriving property definitions
        val definitions = PropertiesPanelState.derivePropertyDefinitions(nodeTypeDef)

        // Then should have correct definitions
        assertEquals(3, definitions.size)

        val urlDef = definitions.find { it.name == "url" }
        assertNotNull(urlDef)
        assertTrue(urlDef!!.required)

        val methodDef = definitions.find { it.name == "method" }
        assertNotNull(methodDef)
        assertEquals(PropertyType.DROPDOWN, methodDef!!.type)
        assertEquals(4, methodDef.options.size)
    }

    // ============================================
    // Helper Functions and Data Classes
    // ============================================

    private fun createTestNode(
        id: String = "test_node",
        configuration: Map<String, String> = emptyMap()
    ): CodeNode {
        return CodeNode(
            id = id,
            name = "TestNode",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(100.0, 100.0),
            configuration = configuration
        )
    }
}
