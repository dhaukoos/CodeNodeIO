/*
 * RecompileTargetResolverTest — TDD tests for T047 (US3): tier-aware target resolution
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecompileTargetResolverTest {

    private lateinit var workDir: File
    private lateinit var projectRoot: File
    private lateinit var universalDir: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("recompile-target-resolver").toFile()
        projectRoot = File(workDir, "project").apply { mkdirs() }
        universalDir = File(workDir, "universal").apply { mkdirs() }
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    private fun resolver() = RecompileTargetResolver(
        projectRoot = projectRoot,
        universalDirProvider = { universalDir }
    )

    /** Helper: write a fixture .kt source under {moduleDir}/src/commonMain/kotlin/.../nodes/{name}. */
    private fun fixtureNodeSource(moduleDir: File, fileName: String): File {
        val nodesDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/nodes").apply { mkdirs() }
        return File(nodesDir, fileName).apply { writeText("// fixture\n") }
    }

    @Test
    fun `MODULE tier resolves to the active module dir`() {
        val moduleDir = File(projectRoot, "Demo").apply { mkdirs() }
        fixtureNodeSource(moduleDir, "FooCodeNode.kt")
        val unit = resolver().resolve(PlacementLevel.MODULE, activeModuleDir = moduleDir)
        assertNotNull(unit)
        assertEquals("Demo", unit.moduleName)
        assertEquals(PlacementLevel.MODULE, unit.tier)
        assertTrue(unit.sources.first().absolutePath.endsWith("FooCodeNode.kt"))
    }

    @Test
    fun `MODULE tier returns null when activeModuleDir is null`() {
        val unit = resolver().resolve(PlacementLevel.MODULE, activeModuleDir = null)
        assertNull(unit)
    }

    @Test
    fun `PROJECT tier resolves to the projects shared nodes module`() {
        val nodesDir = File(projectRoot, "nodes").apply { mkdirs() }
        fixtureNodeSource(nodesDir, "ProjectScopeCodeNode.kt")
        val unit = resolver().resolve(PlacementLevel.PROJECT, activeModuleDir = null)
        assertNotNull(unit)
        assertEquals("nodes", unit.moduleName)
        assertEquals(PlacementLevel.PROJECT, unit.tier)
    }

    @Test
    fun `PROJECT tier returns null when nodes module does not exist`() {
        val unit = resolver().resolve(PlacementLevel.PROJECT, activeModuleDir = null)
        assertNull(unit)
    }

    @Test
    fun `UNIVERSAL tier resolves to the synthetic compile unit at universalDir`() {
        File(universalDir, "Foo.kt").writeText("// fixture\n")
        File(universalDir, "Bar.kt").writeText("// fixture\n")
        val unit = resolver().resolve(PlacementLevel.UNIVERSAL, activeModuleDir = null)
        assertNotNull(unit)
        assertEquals("Universal", unit.moduleName)
        assertEquals(PlacementLevel.UNIVERSAL, unit.tier)
        assertEquals(2, unit.sources.size)
        assertTrue(unit.sources.all { it.hostModuleName == null })
    }

    @Test
    fun `INTERNAL tier returns null`() {
        // Tool-managed nodes are never user-recompiled.
        val unit = resolver().resolve(PlacementLevel.INTERNAL, activeModuleDir = null)
        assertNull(unit)
    }

    @Test
    fun `resolveForFile classifies a Module tier source by its directory`() {
        val moduleDir = File(projectRoot, "Demo").apply { mkdirs() }
        val src = fixtureNodeSource(moduleDir, "FooCodeNode.kt")
        val unit = resolver().resolveForFile(src)
        assertNotNull(unit)
        assertEquals("Demo", unit.moduleName)
        assertEquals(PlacementLevel.MODULE, unit.tier)
    }

    @Test
    fun `resolveForFile classifies a Project tier source under projectRoot slash nodes`() {
        val nodesDir = File(projectRoot, "nodes").apply { mkdirs() }
        val src = fixtureNodeSource(nodesDir, "ProjectScopeCodeNode.kt")
        val unit = resolver().resolveForFile(src)
        assertNotNull(unit)
        assertEquals("nodes", unit.moduleName)
        assertEquals(PlacementLevel.PROJECT, unit.tier)
    }

    @Test
    fun `resolveForFile classifies a Universal tier source under universalDir`() {
        val src = File(universalDir, "Foo.kt").apply { writeText("// fixture\n") }
        val unit = resolver().resolveForFile(src)
        assertNotNull(unit)
        assertEquals(PlacementLevel.UNIVERSAL, unit.tier)
    }

    @Test
    fun `resolveForFile returns null for a path outside any known root`() {
        val outside = File(workDir, "outside").apply { mkdirs() }
        val src = File(outside, "Foo.kt").apply { writeText("// fixture\n") }
        val unit = resolver().resolveForFile(src)
        assertNull(unit)
    }
}
