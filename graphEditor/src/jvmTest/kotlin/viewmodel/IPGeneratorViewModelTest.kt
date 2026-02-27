/*
 * IPGeneratorViewModelTest - Unit tests for IPGeneratorViewModel
 * Verifies state transitions, property management, and validation logic
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.grapheditor.model.CustomIPTypeDefinition
import io.codenode.grapheditor.model.IPProperty
import io.codenode.grapheditor.repository.FileIPTypeRepository
import io.codenode.grapheditor.state.IPTypeRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IPGeneratorViewModelTest {

    private lateinit var tempDir: File
    private lateinit var registry: IPTypeRegistry
    private lateinit var repository: FileIPTypeRepository

    @BeforeTest
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "ip-gen-vm-test-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val testFilePath = File(tempDir, "custom-ip-types.json").absolutePath
        registry = IPTypeRegistry.withDefaults()
        repository = FileIPTypeRepository(testFilePath)
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createViewModel(): IPGeneratorViewModel {
        return IPGeneratorViewModel(registry, repository)
    }

    @Test
    fun `initial state has empty name and no properties`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.state.first()

        assertEquals("", state.typeName)
        assertTrue(state.properties.isEmpty())
        assertFalse(state.isExpanded)
    }

    @Test
    fun `setTypeName updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.setTypeName("UserProfile")

        assertEquals("UserProfile", viewModel.state.first().typeName)
    }

    @Test
    fun `createType registers type in registry and saves to repository and resets form`() = runTest {
        val viewModel = createViewModel()

        viewModel.setTypeName("Order")
        viewModel.toggleExpanded()

        val created = viewModel.createType()

        assertNotNull(created)
        assertEquals("Order", created.typeName)

        // Verify registered in registry
        assertNotNull(registry.getByTypeName("Order"))

        // Verify saved in repository
        assertEquals(1, repository.getAll().size)

        // Verify form was reset but expansion preserved
        val state = viewModel.state.first()
        assertEquals("", state.typeName)
        assertTrue(state.properties.isEmpty())
        assertTrue(state.isExpanded)
    }

    @Test
    fun `createType with properties maps IPPropertyState to IPProperty correctly`() = runTest {
        val viewModel = createViewModel()

        viewModel.setTypeName("Person")
        viewModel.addProperty()
        viewModel.addProperty()

        val state = viewModel.state.first()
        val prop1Id = state.properties[0].id
        val prop2Id = state.properties[1].id

        viewModel.updatePropertyName(prop1Id, "name")
        viewModel.updatePropertyType(prop1Id, "ip_string")
        viewModel.updatePropertyRequired(prop1Id, true)

        viewModel.updatePropertyName(prop2Id, "age")
        viewModel.updatePropertyType(prop2Id, "ip_int")
        viewModel.updatePropertyRequired(prop2Id, false)

        val created = viewModel.createType()

        assertNotNull(created)
        assertEquals(2, created.properties.size)

        assertEquals("name", created.properties[0].name)
        assertEquals("ip_string", created.properties[0].typeId)
        assertTrue(created.properties[0].isRequired)

        assertEquals("age", created.properties[1].name)
        assertEquals("ip_int", created.properties[1].typeId)
        assertFalse(created.properties[1].isRequired)
    }

    @Test
    fun `reset clears form but preserves isExpanded`() = runTest {
        val viewModel = createViewModel()

        viewModel.setTypeName("Something")
        viewModel.addProperty()
        viewModel.toggleExpanded()

        viewModel.reset()

        val state = viewModel.state.first()
        assertEquals("", state.typeName)
        assertTrue(state.properties.isEmpty())
        assertTrue(state.isExpanded)
    }

    @Test
    fun `addProperty adds row with defaults`() = runTest {
        val viewModel = createViewModel()

        viewModel.addProperty()

        val state = viewModel.state.first()
        assertEquals(1, state.properties.size)

        val prop = state.properties[0]
        assertEquals("", prop.name)
        assertEquals("ip_any", prop.selectedTypeId)
        assertTrue(prop.isRequired)
    }

    @Test
    fun `removeProperty removes correct row`() = runTest {
        val viewModel = createViewModel()

        viewModel.addProperty()
        viewModel.addProperty()
        viewModel.addProperty()

        val state = viewModel.state.first()
        assertEquals(3, state.properties.size)
        val middleId = state.properties[1].id

        viewModel.removeProperty(middleId)

        val updated = viewModel.state.first()
        assertEquals(2, updated.properties.size)
        assertTrue(updated.properties.none { it.id == middleId })
    }

    @Test
    fun `isValid false when name blank`() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.state.first().isValid)

        viewModel.setTypeName("   ")
        assertFalse(viewModel.state.first().isValid)
    }

    @Test
    fun `isValid false when name conflicts with existing type`() = runTest {
        val viewModel = createViewModel()

        // "String" is a built-in type in the default registry
        viewModel.setTypeName("String")
        assertFalse(viewModel.state.first().isValid)
        assertTrue(viewModel.state.first().hasNameConflict)

        // Case-insensitive conflict
        viewModel.setTypeName("string")
        assertFalse(viewModel.state.first().isValid)
        assertTrue(viewModel.state.first().hasNameConflict)
    }

    @Test
    fun `isValid false when property name empty`() = runTest {
        val viewModel = createViewModel()

        viewModel.setTypeName("ValidName")
        viewModel.addProperty()
        // Property name is empty by default

        assertFalse(viewModel.state.first().isValid)
        assertTrue(viewModel.state.first().hasEmptyPropertyNames)
    }

    @Test
    fun `isValid false when duplicate property names`() = runTest {
        val viewModel = createViewModel()

        viewModel.setTypeName("ValidName")
        viewModel.addProperty()
        viewModel.addProperty()

        val state = viewModel.state.first()
        viewModel.updatePropertyName(state.properties[0].id, "field")
        viewModel.updatePropertyName(state.properties[1].id, "field")

        assertFalse(viewModel.state.first().isValid)
        assertTrue(viewModel.state.first().hasDuplicatePropertyNames)
    }

    @Test
    fun `isValid true when all valid`() = runTest {
        val viewModel = createViewModel()

        viewModel.setTypeName("NewType")
        assertTrue(viewModel.state.first().isValid)

        // Also valid with properly named properties
        viewModel.addProperty()
        viewModel.addProperty()

        val state = viewModel.state.first()
        viewModel.updatePropertyName(state.properties[0].id, "fieldA")
        viewModel.updatePropertyName(state.properties[1].id, "fieldB")

        assertTrue(viewModel.state.first().isValid)
    }

    @Test
    fun `createType returns null when state is invalid`() = runTest {
        val viewModel = createViewModel()

        // Empty name - invalid
        val result = viewModel.createType()

        assertNull(result)
        assertEquals(0, repository.getAll().size)
    }

    @Test
    fun `createType updates existingTypeNames so same name cannot be reused`() = runTest {
        val viewModel = createViewModel()

        viewModel.setTypeName("UniqueType")
        val created = viewModel.createType()
        assertNotNull(created)

        // Now try the same name again
        viewModel.setTypeName("UniqueType")
        assertTrue(viewModel.state.first().hasNameConflict)
        assertFalse(viewModel.state.first().isValid)
    }
}
