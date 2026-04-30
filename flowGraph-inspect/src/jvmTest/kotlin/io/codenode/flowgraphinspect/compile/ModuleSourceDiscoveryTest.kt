/*
 * ModuleSourceDiscoveryTest — TDD test for T040 (US2)
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

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

class ModuleSourceDiscoveryTest {

    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("module-source-discovery").toFile()
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    /** Builds a fixture KMP module at workDir/{name}/ with the given .kt files in nodes/. */
    private fun fixtureModule(
        name: String,
        commonMainNodes: List<String> = emptyList(),
        jvmMainNodes: List<String> = emptyList(),
        nonNodeFiles: List<String> = emptyList()
    ): File {
        val moduleDir = File(workDir, name).apply { mkdirs() }
        commonMainNodes.forEach { fileName ->
            val nodesDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/${name.lowercase()}/nodes").apply { mkdirs() }
            File(nodesDir, fileName).writeText("// fixture\n")
        }
        jvmMainNodes.forEach { fileName ->
            val nodesDir = File(moduleDir, "src/jvmMain/kotlin/io/codenode/${name.lowercase()}/nodes").apply { mkdirs() }
            File(nodesDir, fileName).writeText("// fixture\n")
        }
        nonNodeFiles.forEach { rel ->
            val f = File(moduleDir, rel)
            f.parentFile.mkdirs()
            f.writeText("// not a node file\n")
        }
        return moduleDir
    }

    @Test
    fun `forModule returns null when moduleDir does not exist`() {
        val nonExistent = File(workDir, "NotThere")
        val result = ModuleSourceDiscovery.forModule(nonExistent, "NotThere", PlacementLevel.MODULE)
        assertNull(result)
    }

    @Test
    fun `forModule returns null when no kt sources exist under nodes`() {
        val moduleDir = fixtureModule("Empty")
        val result = ModuleSourceDiscovery.forModule(moduleDir, "Empty", PlacementLevel.MODULE)
        assertNull(result, "an empty module produces null (CompileUnit.Module.sources non-empty invariant)")
    }

    @Test
    fun `forModule discovers commonMain nodes`() {
        val moduleDir = fixtureModule(
            "Demo",
            commonMainNodes = listOf("FooCodeNode.kt", "BarCodeNode.kt")
        )
        val result = ModuleSourceDiscovery.forModule(moduleDir, "Demo", PlacementLevel.MODULE)
        assertNotNull(result)
        assertEquals(2, result.sources.size)
        assertTrue(result.sources.all { it.absolutePath.endsWith(".kt") })
        assertTrue(result.sources.all { it.tier == PlacementLevel.MODULE })
        assertTrue(result.sources.all { it.hostModuleName == "Demo" })
    }

    @Test
    fun `forModule discovers jvmMain nodes alongside commonMain`() {
        val moduleDir = fixtureModule(
            "Demo",
            commonMainNodes = listOf("CommonNode.kt"),
            jvmMainNodes = listOf("JvmNode.kt")
        )
        val result = ModuleSourceDiscovery.forModule(moduleDir, "Demo", PlacementLevel.MODULE)
        assertNotNull(result)
        assertEquals(2, result.sources.size)
        val names = result.sources.map { it.absolutePath.substringAfterLast('/') }.toSet()
        assertEquals(setOf("CommonNode.kt", "JvmNode.kt"), names)
    }

    @Test
    fun `forModule excludes non-nodes files`() {
        val moduleDir = fixtureModule(
            "Demo",
            commonMainNodes = listOf("RealCodeNode.kt"),
            nonNodeFiles = listOf(
                "src/commonMain/kotlin/io/codenode/demo/utils/Helper.kt",
                "src/commonMain/kotlin/io/codenode/demo/viewmodel/State.kt",
                "build.gradle.kts"
            )
        )
        val result = ModuleSourceDiscovery.forModule(moduleDir, "Demo", PlacementLevel.MODULE)
        assertNotNull(result)
        assertEquals(1, result.sources.size, "only files under */nodes/ should be included")
        assertTrue(result.sources.first().absolutePath.endsWith("RealCodeNode.kt"))
    }

    @Test
    fun `forModule sets PROJECT tier when given PROJECT`() {
        val moduleDir = fixtureModule("nodes", commonMainNodes = listOf("ProjectNode.kt"))
        val result = ModuleSourceDiscovery.forModule(moduleDir, "nodes", PlacementLevel.PROJECT)
        assertNotNull(result)
        assertEquals(PlacementLevel.PROJECT, result.tier)
        assertTrue(result.sources.all { it.tier == PlacementLevel.PROJECT })
    }

    @Test
    fun `forUniversal walks the flat universal-tier directory`() {
        val universalDir = File(workDir, "universal").apply { mkdirs() }
        File(universalDir, "Foo.kt").writeText("// fixture\n")
        File(universalDir, "Bar.kt").writeText("// fixture\n")
        // Sub-directories are NOT walked at Universal tier.
        File(universalDir, "subdir").mkdirs()
        File(universalDir, "subdir/Ignored.kt").writeText("// should not be picked up\n")

        val result = ModuleSourceDiscovery.forUniversal(universalDir)
        assertNotNull(result)
        assertEquals(2, result.sources.size)
        assertEquals(PlacementLevel.UNIVERSAL, result.tier)
        assertTrue(
            result.sources.all { it.hostModuleName == null },
            "Universal tier sources MUST have null hostModuleName"
        )
    }

    @Test
    fun `forUniversal returns null when directory has no kt files`() {
        val universalDir = File(workDir, "empty-universal").apply { mkdirs() }
        File(universalDir, "README.md").writeText("not a kotlin file")
        val result = ModuleSourceDiscovery.forUniversal(universalDir)
        assertNull(result)
    }
}
