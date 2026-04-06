/*
 * FileIPTypeRepositoryTest - Persistence tests for FileIPTypeRepository
 * License: Apache 2.0
 */

package io.codenode.grapheditor.repository

import io.codenode.fbpdsl.model.IPColor
import io.codenode.flowgraphtypes.model.CustomIPTypeDefinition
import io.codenode.flowgraphtypes.model.IPProperty
import io.codenode.flowgraphtypes.repository.FileIPTypeRepository
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileIPTypeRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var testFilePath: String
    private lateinit var repository: FileIPTypeRepository

    @BeforeTest
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "ip-type-repo-test-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        testFilePath = File(tempDir, "custom-ip-types.json").absolutePath
        repository = FileIPTypeRepository(testFilePath)
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `save and load roundtrip preserves all fields`() {
        val definition = CustomIPTypeDefinition(
            id = "ip_user",
            typeName = "User",
            properties = listOf(
                IPProperty(name = "name", typeId = "ip_string", isRequired = true),
                IPProperty(name = "age", typeId = "ip_int", isRequired = false)
            ),
            color = IPColor(233, 30, 99)
        )

        repository.add(definition, description = "A user type")

        val newRepository = FileIPTypeRepository(testFilePath)
        newRepository.load()

        val loaded = newRepository.getAllDefinitions()
        assertEquals(1, loaded.size)

        val loadedDef = loaded[0]
        assertEquals("ip_user", loadedDef.id)
        assertEquals("User", loadedDef.typeName)
        assertEquals(IPColor(233, 30, 99), loadedDef.color)
        assertEquals(2, loadedDef.properties.size)

        assertEquals("name", loadedDef.properties[0].name)
        assertEquals("ip_string", loadedDef.properties[0].typeId)
        assertTrue(loadedDef.properties[0].isRequired)

        assertEquals("age", loadedDef.properties[1].name)
        assertEquals("ip_int", loadedDef.properties[1].typeId)
        assertFalse(loadedDef.properties[1].isRequired)
    }

    @Test
    fun `load from missing file returns empty list`() {
        val nonExistentPath = File(tempDir, "non-existent.json").absolutePath
        val repo = FileIPTypeRepository(nonExistentPath)

        repo.load()

        assertTrue(repo.getAll().isEmpty())
        assertTrue(repo.getAllDefinitions().isEmpty())
    }

    @Test
    fun `load from corrupt file returns empty list gracefully`() {
        val file = File(testFilePath)
        file.parentFile?.mkdirs()
        file.writeText("{ this is not valid json }")

        repository.load()

        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `save creates parent directories`() {
        val nestedPath = File(tempDir, "nested/dirs/custom-ip-types.json").absolutePath
        val nestedRepo = FileIPTypeRepository(nestedPath)

        val definition = CustomIPTypeDefinition(
            id = "ip_test",
            typeName = "Test",
            color = IPColor(0, 0, 0)
        )
        nestedRepo.add(definition)

        assertTrue(File(nestedPath).exists())
    }

    @Test
    fun `multiple types with properties serialize correctly`() {
        val def1 = CustomIPTypeDefinition(
            id = "ip_address",
            typeName = "Address",
            properties = listOf(
                IPProperty(name = "street", typeId = "ip_string", isRequired = true),
                IPProperty(name = "city", typeId = "ip_string", isRequired = true),
                IPProperty(name = "zip", typeId = "ip_int", isRequired = false)
            ),
            color = IPColor(0, 188, 212)
        )
        val def2 = CustomIPTypeDefinition(
            id = "ip_marker",
            typeName = "Marker",
            properties = emptyList(),
            color = IPColor(121, 85, 72)
        )

        repository.add(def1)
        repository.add(def2)

        val newRepository = FileIPTypeRepository(testFilePath)
        newRepository.load()

        val loaded = newRepository.getAllDefinitions()
        assertEquals(2, loaded.size)

        assertEquals("Address", loaded[0].typeName)
        assertEquals(3, loaded[0].properties.size)

        assertEquals("Marker", loaded[1].typeName)
        assertTrue(loaded[1].properties.isEmpty())
    }

    @Test
    fun `remove deletes type by id and persists`() {
        val def1 = CustomIPTypeDefinition(
            id = "ip_first",
            typeName = "First",
            color = IPColor(0, 0, 0)
        )
        val def2 = CustomIPTypeDefinition(
            id = "ip_second",
            typeName = "Second",
            color = IPColor(0, 0, 0)
        )
        repository.add(def1)
        repository.add(def2)

        val removed = repository.remove("ip_first")

        assertTrue(removed)
        assertEquals(1, repository.getAll().size)

        val newRepo = FileIPTypeRepository(testFilePath)
        newRepo.load()
        assertEquals(1, newRepo.getAll().size)
        assertEquals("ip_second", newRepo.getAll()[0].id)
    }

    @Test
    fun `remove returns false for non-existent id`() {
        repository.add(CustomIPTypeDefinition(
            id = "ip_exists",
            typeName = "Exists",
            color = IPColor(0, 0, 0)
        ))

        val removed = repository.remove("ip_not_here")

        assertFalse(removed)
        assertEquals(1, repository.getAll().size)
    }

    @Test
    fun `load with empty file returns empty list`() {
        val file = File(testFilePath)
        file.parentFile?.mkdirs()
        file.writeText("")

        repository.load()

        assertTrue(repository.getAll().isEmpty())
    }
}
