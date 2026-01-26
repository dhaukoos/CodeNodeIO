/*
 * PropertyConfiguration Test
 * Unit tests for PropertyConfiguration validation
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.model

import kotlin.test.*

/**
 * TDD tests for PropertyConfiguration validation.
 * These tests verify that PropertyConfiguration correctly validates:
 * - Required fields and basic constraints
 * - Property type conversions
 * - Expression references to other nodes
 * - Schema validation against NodeTypeDefinition
 * - Merge operations with defaults
 */
class PropertyConfigurationTest {

    // ============================================
    // Basic Validation Tests
    // ============================================

    @Test
    fun `should validate basic configuration with nodeId and properties`() {
        // Given a valid property configuration
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf(
                "timeout" to "30",
                "retries" to "3"
            )
        )

        // When validating
        val result = config.validate()

        // Then validation should pass
        assertTrue(result.success, "Valid configuration should pass validation")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
    }

    @Test
    fun `should fail validation when nodeId is blank`() {
        // Given/When attempting to create config with blank nodeId
        // Then should throw IllegalArgumentException due to init block
        assertFailsWith<IllegalArgumentException> {
            PropertyConfiguration(
                nodeId = "",
                properties = emptyMap()
            )
        }
    }

    @Test
    fun `should allow empty properties map`() {
        // Given a configuration with no properties
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = emptyMap()
        )

        // When validating
        val result = config.validate()

        // Then validation should pass (empty properties are valid)
        assertTrue(result.success, "Empty properties should be valid")
    }

    @Test
    fun `should track validation errors from external sources`() {
        // Given a configuration with stored validation errors
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf("key" to "value"),
            validationErrors = listOf("External error 1", "External error 2")
        )

        // When validating
        val result = config.validate()

        // Then validation should include stored errors
        assertFalse(result.success, "Should fail when validation errors exist")
        assertTrue(result.errors.contains("External error 1"))
        assertTrue(result.errors.contains("External error 2"))
    }

    // ============================================
    // Property Access Tests
    // ============================================

    @Test
    fun `should get string property with default fallback`() {
        // Given a configuration with string property
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf("url" to "https://api.example.com")
        )

        // When getting existing property
        assertEquals("https://api.example.com", config.getStringProperty("url"))

        // And when getting non-existent property with default
        assertEquals("default", config.getStringProperty("missing", "default"))
    }

    @Test
    fun `should get int property with conversion`() {
        // Given a configuration with numeric properties
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf(
                "timeout" to "30",
                "invalid" to "not-a-number"
            )
        )

        // When getting valid int
        assertEquals(30, config.getIntProperty("timeout"))

        // And when getting invalid int, should return default
        assertEquals(0, config.getIntProperty("invalid"))
        assertEquals(-1, config.getIntProperty("invalid", -1))

        // And when property doesn't exist
        assertEquals(100, config.getIntProperty("missing", 100))
    }

    @Test
    fun `should get long property with conversion`() {
        // Given a configuration with long value
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf("largeValue" to "9876543210")
        )

        // When getting long property
        assertEquals(9876543210L, config.getLongProperty("largeValue"))

        // And when property doesn't exist
        assertEquals(0L, config.getLongProperty("missing"))
    }

    @Test
    fun `should get double property with conversion`() {
        // Given a configuration with double value
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf("rate" to "0.95")
        )

        // When getting double property
        assertEquals(0.95, config.getDoubleProperty("rate"), 0.001)

        // And when property doesn't exist
        assertEquals(1.0, config.getDoubleProperty("missing", 1.0), 0.001)
    }

    @Test
    fun `should get boolean property with various truthy values`() {
        // Given a configuration with boolean properties
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf(
                "enabled1" to "true",
                "enabled2" to "1",
                "enabled3" to "yes",
                "enabled4" to "on",
                "disabled1" to "false",
                "disabled2" to "0",
                "disabled3" to "no",
                "disabled4" to "off"
            )
        )

        // When getting truthy values
        assertTrue(config.getBooleanProperty("enabled1"))
        assertTrue(config.getBooleanProperty("enabled2"))
        assertTrue(config.getBooleanProperty("enabled3"))
        assertTrue(config.getBooleanProperty("enabled4"))

        // And when getting falsy values
        assertFalse(config.getBooleanProperty("disabled1"))
        assertFalse(config.getBooleanProperty("disabled2"))
        assertFalse(config.getBooleanProperty("disabled3"))
        assertFalse(config.getBooleanProperty("disabled4"))

        // And when property doesn't exist, returns default
        assertTrue(config.getBooleanProperty("missing", true))
        assertFalse(config.getBooleanProperty("missing", false))
    }

    // ============================================
    // Expression Reference Tests
    // ============================================

    @Test
    fun `should detect expression references in property values`() {
        // Given a configuration with expression references
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf(
                "static" to "plain value",
                "dynamic" to "\${auth.token}",
                "mixed" to "Bearer \${auth.token}"
            )
        )

        // When checking for expressions
        assertFalse(config.isExpression("static"), "Static value should not be expression")
        assertTrue(config.isExpression("dynamic"), "Dynamic value should be expression")
        assertTrue(config.isExpression("mixed"), "Mixed value should contain expression")
    }

    @Test
    fun `should extract expression references from property values`() {
        // Given a configuration with expression references
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf(
                "singleRef" to "\${auth.token}",
                "multipleRefs" to "\${user.id} - \${user.name}",
                "nestedField" to "\${response.data.items}"
            )
        )

        // When extracting references
        val singleRefs = config.getExpressionReferences("singleRef")
        val multipleRefs = config.getExpressionReferences("multipleRefs")
        val nestedRefs = config.getExpressionReferences("nestedField")

        // Then references should be extracted correctly
        assertEquals(listOf("auth.token"), singleRefs)
        assertEquals(listOf("user.id", "user.name"), multipleRefs)
        assertEquals(listOf("response.data.items"), nestedRefs)
    }

    @Test
    fun `should return empty list when no expression references exist`() {
        // Given a configuration without expressions
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf("static" to "plain value")
        )

        // When extracting references
        val refs = config.getExpressionReferences("static")

        // Then should return empty list
        assertTrue(refs.isEmpty())
    }

    // ============================================
    // Schema Validation Tests
    // ============================================

    @Test
    fun `should pass schema validation when no schema is defined`() {
        // Given a node type definition without schema
        val nodeTypeDef = NodeTypeDefinition(
            id = "nodeType_test",
            name = "Test Node",
            category = NodeTypeDefinition.NodeCategory.TRANSFORMER,
            description = "Test node type",
            configurationSchema = null,
            defaultConfiguration = emptyMap()
        )

        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf("anyKey" to "anyValue")
        )

        // When validating against schema
        val result = config.validateAgainstSchema(nodeTypeDef)

        // Then validation should pass
        assertTrue(result.success, "Should pass when no schema is defined")
    }

    @Test
    fun `should merge with default configuration correctly`() {
        // Given a node type definition with defaults
        val nodeTypeDef = NodeTypeDefinition(
            id = "nodeType_http",
            name = "HTTP Client",
            category = NodeTypeDefinition.NodeCategory.API_ENDPOINT,
            description = "HTTP client node",
            defaultConfiguration = mapOf(
                "timeout" to "30",
                "retries" to "3",
                "method" to "GET"
            )
        )

        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf(
                "timeout" to "60",  // Override default
                "url" to "https://api.example.com"  // New property
            )
        )

        // When merging with defaults
        val merged = config.mergeWithDefaults(nodeTypeDef)

        // Then should have all defaults plus overrides
        assertEquals("60", merged.getProperty("timeout"), "Should use override value")
        assertEquals("3", merged.getProperty("retries"), "Should use default value")
        assertEquals("GET", merged.getProperty("method"), "Should use default value")
        assertEquals("https://api.example.com", merged.getProperty("url"), "Should include new property")
    }

    @Test
    fun `should detect customizations from defaults`() {
        // Given a node type definition and customized config
        val nodeTypeDef = NodeTypeDefinition(
            id = "nodeType_test",
            name = "Test Node",
            category = NodeTypeDefinition.NodeCategory.TRANSFORMER,
            description = "Test node",
            defaultConfiguration = mapOf(
                "timeout" to "30",
                "retries" to "3"
            )
        )

        val customized = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf(
                "timeout" to "60",  // Changed
                "retries" to "3"    // Same as default
            )
        )

        val notCustomized = PropertyConfiguration(
            nodeId = "node_456",
            properties = mapOf(
                "timeout" to "30",
                "retries" to "3"
            )
        )

        // When checking for customizations
        assertTrue(customized.hasCustomizations(nodeTypeDef), "Should detect customizations")
        assertFalse(notCustomized.hasCustomizations(nodeTypeDef), "Should not detect customizations when same as defaults")
    }

    @Test
    fun `should get only customized properties`() {
        // Given a node type definition and customized config
        val nodeTypeDef = NodeTypeDefinition(
            id = "nodeType_test",
            name = "Test Node",
            category = NodeTypeDefinition.NodeCategory.TRANSFORMER,
            description = "Test node",
            defaultConfiguration = mapOf(
                "timeout" to "30",
                "retries" to "3"
            )
        )

        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf(
                "timeout" to "60",  // Changed
                "retries" to "3",   // Same
                "extra" to "value"  // New (not in defaults)
            )
        )

        // When getting customizations
        val customizations = config.getCustomizations(nodeTypeDef)

        // Then should only include changed and new properties
        assertEquals(2, customizations.size)
        assertEquals("60", customizations["timeout"])
        assertEquals("value", customizations["extra"])
        assertFalse(customizations.containsKey("retries"), "Should not include unchanged defaults")
    }

    // ============================================
    // Immutable Update Tests
    // ============================================

    @Test
    fun `should create new instance when setting property`() {
        // Given a configuration
        val original = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf("key1" to "value1")
        )

        // When setting a new property
        val updated = original.setProperty("key2", "value2")

        // Then original should be unchanged
        assertNull(original.getProperty("key2"))
        assertEquals("value2", updated.getProperty("key2"))
        assertEquals("value1", updated.getProperty("key1"))
        assertNotSame(original, updated)
    }

    @Test
    fun `should create new instance when removing property`() {
        // Given a configuration
        val original = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf("key1" to "value1", "key2" to "value2")
        )

        // When removing a property
        val updated = original.removeProperty("key1")

        // Then original should be unchanged
        assertEquals("value1", original.getProperty("key1"))
        assertNull(updated.getProperty("key1"))
        assertEquals("value2", updated.getProperty("key2"))
    }

    @Test
    fun `should track property count correctly`() {
        // Given a configuration with properties
        val config = PropertyConfiguration(
            nodeId = "node_123",
            properties = mapOf("a" to "1", "b" to "2", "c" to "3")
        )

        // When checking count
        assertEquals(3, config.getPropertyCount())
        assertEquals(setOf("a", "b", "c"), config.getPropertyKeys())
    }

    // ============================================
    // Error Management Tests
    // ============================================

    @Test
    fun `should track and clear validation errors`() {
        // Given a configuration without errors
        var config = PropertyConfiguration(nodeId = "node_123")

        // When adding errors
        config = config.addValidationError("Error 1")
        config = config.addValidationError("Error 2")

        // Then should track errors
        assertTrue(config.hasErrors())
        assertEquals(2, config.validationErrors.size)

        // And when clearing errors
        config = config.clearValidationErrors()
        assertFalse(config.hasErrors())
        assertTrue(config.validationErrors.isEmpty())
    }
}
