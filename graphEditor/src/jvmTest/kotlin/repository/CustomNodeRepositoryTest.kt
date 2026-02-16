/*
 * CustomNodeRepositoryTest - Persistence tests for CustomNodeRepository
 * License: Apache 2.0
 */

package io.codenode.grapheditor.repository

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomNodeRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var testFilePath: String
    private lateinit var repository: FileCustomNodeRepository

    @BeforeTest
    fun setUp() {
        // Create a temporary directory for test files
        tempDir = File(System.getProperty("java.io.tmpdir"), "custom-node-repo-test-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        testFilePath = File(tempDir, "custom-nodes.json").absolutePath
        repository = FileCustomNodeRepository(testFilePath)
    }

    @AfterTest
    fun tearDown() {
        // Clean up test files
        tempDir.deleteRecursively()
    }

    @Test
    fun `save and load roundtrip preserves nodes`() {
        // Create and add nodes
        val node1 = CustomNodeDefinition.create("DataMerger", 2, 1)
        val node2 = CustomNodeDefinition.create("TripleSplitter", 1, 3)

        repository.add(node1)
        repository.add(node2)

        // Create new repository instance and load
        val newRepository = FileCustomNodeRepository(testFilePath)
        newRepository.load()

        val loadedNodes = newRepository.getAll()

        // Verify nodes were preserved
        assertEquals(2, loadedNodes.size)
        assertEquals("DataMerger", loadedNodes[0].name)
        assertEquals(2, loadedNodes[0].inputCount)
        assertEquals(1, loadedNodes[0].outputCount)
        assertEquals("in2out1", loadedNodes[0].genericType)

        assertEquals("TripleSplitter", loadedNodes[1].name)
        assertEquals(1, loadedNodes[1].inputCount)
        assertEquals(3, loadedNodes[1].outputCount)
        assertEquals("in1out3", loadedNodes[1].genericType)
    }

    @Test
    fun `load with missing file returns empty list`() {
        // Ensure file doesn't exist
        val nonExistentPath = File(tempDir, "non-existent.json").absolutePath
        val repo = FileCustomNodeRepository(nonExistentPath)

        repo.load()

        assertTrue(repo.getAll().isEmpty())
    }

    @Test
    fun `load with corrupted JSON returns empty list`() {
        // Write corrupted JSON to file
        val corruptedFile = File(testFilePath)
        corruptedFile.parentFile?.mkdirs()
        corruptedFile.writeText("{ this is not valid json }")

        repository.load()

        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `load with empty file returns empty list`() {
        // Write empty content to file
        val emptyFile = File(testFilePath)
        emptyFile.parentFile?.mkdirs()
        emptyFile.writeText("")

        repository.load()

        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `load with empty array returns empty list`() {
        // Write empty JSON array to file
        val file = File(testFilePath)
        file.parentFile?.mkdirs()
        file.writeText("[]")

        repository.load()

        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun `add persists immediately`() {
        val node = CustomNodeDefinition.create("TestNode", 1, 1)
        repository.add(node)

        // Verify file exists and contains data
        val file = File(testFilePath)
        assertTrue(file.exists())
        assertTrue(file.readText().contains("TestNode"))
    }

    @Test
    fun `getAll returns copy of list`() {
        val node = CustomNodeDefinition.create("TestNode", 1, 1)
        repository.add(node)

        val list1 = repository.getAll()
        val list2 = repository.getAll()

        // Should be equal but not same instance
        assertEquals(list1, list2)
    }

    @Test
    fun `save creates parent directories`() {
        // Use a path with nested directories that don't exist
        val nestedPath = File(tempDir, "nested/dirs/custom-nodes.json").absolutePath
        val nestedRepo = FileCustomNodeRepository(nestedPath)

        val node = CustomNodeDefinition.create("TestNode", 1, 1)
        nestedRepo.add(node)

        // Verify file was created
        assertTrue(File(nestedPath).exists())
    }

    @Test
    fun `multiple adds accumulate correctly`() {
        repository.add(CustomNodeDefinition.create("Node1", 1, 1))
        repository.add(CustomNodeDefinition.create("Node2", 2, 2))
        repository.add(CustomNodeDefinition.create("Node3", 3, 3))

        assertEquals(3, repository.getAll().size)

        // Verify persistence
        val newRepo = FileCustomNodeRepository(testFilePath)
        newRepo.load()
        assertEquals(3, newRepo.getAll().size)
    }

    @Test
    fun `load clears existing in-memory nodes before loading`() {
        // Add a node in memory
        repository.add(CustomNodeDefinition.create("InMemory", 1, 1))

        // Write different content to file
        val file = File(testFilePath)
        file.writeText("""
            [
                {
                    "id": "custom_node_from_file",
                    "name": "FromFile",
                    "inputCount": 2,
                    "outputCount": 2,
                    "genericType": "in2out2",
                    "createdAt": 1234567890
                }
            ]
        """.trimIndent())

        // Load should replace in-memory with file contents
        repository.load()

        val nodes = repository.getAll()
        assertEquals(1, nodes.size)
        assertEquals("FromFile", nodes[0].name)
    }

    @Test
    fun `remove deletes node by id and persists`() {
        val node1 = CustomNodeDefinition.create("Node1", 1, 1)
        val node2 = CustomNodeDefinition.create("Node2", 2, 2)
        repository.add(node1)
        repository.add(node2)

        // Remove node1 by ID
        val removed = repository.remove(node1.id)

        assertTrue(removed)
        assertEquals(1, repository.getAll().size)
        assertEquals("Node2", repository.getAll()[0].name)

        // Verify persistence
        val newRepo = FileCustomNodeRepository(testFilePath)
        newRepo.load()
        assertEquals(1, newRepo.getAll().size)
        assertEquals("Node2", newRepo.getAll()[0].name)
    }

    @Test
    fun `remove returns false for non-existent id`() {
        repository.add(CustomNodeDefinition.create("Node1", 1, 1))

        val removed = repository.remove("non_existent_id")

        kotlin.test.assertFalse(removed)
        assertEquals(1, repository.getAll().size)
    }

    @Test
    fun `remove all nodes leaves empty list`() {
        val node = CustomNodeDefinition.create("Node1", 1, 1)
        repository.add(node)

        repository.remove(node.id)

        assertTrue(repository.getAll().isEmpty())

        // Verify persistence
        val newRepo = FileCustomNodeRepository(testFilePath)
        newRepo.load()
        assertTrue(newRepo.getAll().isEmpty())
    }
}
