/*
 * SessionCompileCacheTest - TDD Red tests for SessionCompileCache lifecycle
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionCompileCacheTest {

    private lateinit var rootDir: File

    @BeforeTest
    fun setUp() {
        rootDir = createTempDirectory("compile-cache-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        rootDir.deleteRecursively()
    }

    private fun fixtureSingleFileUnit(suffix: String = "Foo") = CompileUnit.SingleFile(
        CompileSource("/tmp/${suffix}.kt", PlacementLevel.MODULE, "Demo")
    )

    @Test
    fun `allocate returns a fresh empty subdirectory under rootDir`() {
        val cache = SessionCompileCache(rootDir)
        val dir = cache.allocate(fixtureSingleFileUnit())
        assertTrue(dir.isDirectory, "allocate must return a directory")
        assertTrue(dir.listFiles().isNullOrEmpty(), "allocated dir must start empty")
        assertTrue(dir.absolutePath.startsWith(rootDir.absolutePath), "allocated dir must live under rootDir")
    }

    @Test
    fun `consecutive allocate for same unit returns distinct directories`() {
        val cache = SessionCompileCache(rootDir)
        val unit = fixtureSingleFileUnit()
        val first = cache.allocate(unit)
        val second = cache.allocate(unit)
        assertNotEquals(first.absolutePath, second.absolutePath,
            "allocate must NEVER reuse a directory across calls — old class files would conflict with new output"
        )
        assertTrue(first.isDirectory)
        assertTrue(second.isDirectory)
    }

    @Test
    fun `allocate for different units returns distinct directories`() {
        val cache = SessionCompileCache(rootDir)
        val a = cache.allocate(fixtureSingleFileUnit("A"))
        val b = cache.allocate(fixtureSingleFileUnit("B"))
        assertNotEquals(a.absolutePath, b.absolutePath)
    }

    @Test
    fun `deleteAll removes the entire root tree`() {
        val cache = SessionCompileCache(rootDir)
        cache.allocate(fixtureSingleFileUnit())
        assertTrue(rootDir.exists())
        cache.deleteAll()
        assertFalse(rootDir.exists(), "deleteAll must remove rootDir recursively")
    }

    @Test
    fun `deleteAll on already-removed root is idempotent`() {
        val cache = SessionCompileCache(rootDir)
        cache.deleteAll()
        cache.deleteAll() // must not throw
    }
}
