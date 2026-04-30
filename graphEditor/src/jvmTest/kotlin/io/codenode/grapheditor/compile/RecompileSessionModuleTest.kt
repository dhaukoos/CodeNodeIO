/*
 * RecompileSessionModuleTest — TDD test for T039 (US2): RecompileSession.recompile(Module)
 * end-to-end against fixture multi-file modules.
 *
 * Pins the recompile-session contract cases that the foundational T017 deferred:
 *   - module-recompile-supersedes-prior-install
 *   - module with intra-module cross-reference compiles atomically and resolves
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphinspect.compile.ClasspathSnapshot
import io.codenode.flowgraphinspect.compile.CompileSource
import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.InProcessCompiler
import io.codenode.flowgraphinspect.compile.ModuleSourceDiscovery
import io.codenode.flowgraphinspect.compile.SessionCompileCache
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RecompileSessionModuleTest {

    private lateinit var workDir: File
    private lateinit var session: RecompileSession
    private lateinit var registry: NodeDefinitionRegistry

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("recompile-session-module").toFile()
        registry = NodeDefinitionRegistry()
        val classpath = ClasspathSnapshot(
            entries = System.getProperty("java.class.path").split(File.pathSeparator),
            source = ClasspathSnapshot.Source.JAVA_CLASS_PATH_PROPERTY
        )
        val cacheDir = File(workDir, "cache").apply { mkdirs() }
        val cache = SessionCompileCache(cacheDir)
        val compiler = InProcessCompiler(classpath, cache)
        val publisher = RecompileFeedbackPublisher(onErrorEntry = {}, onStatusMessage = {})
        session = RecompileSession(
            compiler = compiler,
            registry = registry,
            pipelineQuiescer = PipelineQuiescer(),
            publisher = publisher,
            sessionCacheDir = cacheDir
        )
    }

    @AfterTest
    fun tearDown() {
        session.shutdown()
        workDir.deleteRecursively()
    }

    private fun fixtureSourceFile(moduleDir: File, name: String, content: String): File {
        val nodesDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/demo/nodes").apply { mkdirs() }
        return File(nodesDir, name).apply { writeText(content) }
    }

    private fun validCodeNodeSource(name: String, marker: String = "v1"): String = """
        package io.codenode.demo.nodes

        import io.codenode.fbpdsl.runtime.CodeNodeDefinition
        import io.codenode.fbpdsl.runtime.NodeRuntime
        import io.codenode.fbpdsl.runtime.PortSpec
        import io.codenode.fbpdsl.model.CodeNodeType

        object ${name}CodeNode : CodeNodeDefinition {
            override val name = "$name"
            override val category = CodeNodeType.TRANSFORMER
            override val description = "$marker"
            override val inputPorts = listOf(PortSpec("input1", Any::class))
            override val outputPorts = listOf(PortSpec("output1", Any::class))
            override fun createRuntime(name: String): NodeRuntime = TODO("fixture")
        }
    """.trimIndent()

    @Test
    fun `module-recompile-supersedes-prior-install`() = runTest {
        val moduleDir = File(workDir, "Demo").apply { mkdirs() }
        fixtureSourceFile(moduleDir, "FooCodeNode.kt", validCodeNodeSource("Foo", marker = "v1"))

        // First recompile: module produces "Foo" version 1.
        val unit1 = ModuleSourceDiscovery.forModule(moduleDir, "Demo", PlacementLevel.MODULE)
        assertNotNull(unit1)
        val r1 = session.recompile(unit1)
        assertTrue(r1.success, "first compile must succeed")
        val foo1 = registry.getByName("Foo")
        assertNotNull(foo1)
        assertEquals("v1", foo1.description)

        // Mutate the source so version 2 differs.
        fixtureSourceFile(moduleDir, "FooCodeNode.kt", validCodeNodeSource("Foo", marker = "v2"))
        val unit2 = ModuleSourceDiscovery.forModule(moduleDir, "Demo", PlacementLevel.MODULE)
        assertNotNull(unit2)
        val r2 = session.recompile(unit2)
        assertTrue(r2.success, "second compile must succeed")

        val foo2 = registry.getByName("Foo")
        assertNotNull(foo2)
        assertEquals("v2", foo2.description, "module recompile MUST supersede the prior session install")
        assertNotSame(foo1, foo2, "the supersede produces a fresh definition instance from a new ClassloaderScope")
    }

    @Test
    fun `intra-module cross-reference resolves atomically across two files`() = runTest {
        val moduleDir = File(workDir, "Demo").apply { mkdirs() }
        // Helper.kt — referenced by UserCodeNode.kt.
        fixtureSourceFile(moduleDir, "Helper.kt", """
            package io.codenode.demo.nodes
            data class Helper(val n: Int)
        """.trimIndent())
        // UserCodeNode.kt — imports Helper from the same module.
        fixtureSourceFile(moduleDir, "UserCodeNode.kt", """
            package io.codenode.demo.nodes

            import io.codenode.fbpdsl.runtime.CodeNodeDefinition
            import io.codenode.fbpdsl.runtime.NodeRuntime
            import io.codenode.fbpdsl.runtime.PortSpec
            import io.codenode.fbpdsl.model.CodeNodeType

            object UserCodeNode : CodeNodeDefinition {
                override val name = "User"
                override val category = CodeNodeType.TRANSFORMER
                override val description = "uses Helper"
                override val inputPorts = listOf(PortSpec("input1", Any::class))
                override val outputPorts = listOf(PortSpec("output1", Any::class))
                private val h = Helper(42)
                override fun createRuntime(name: String): NodeRuntime = TODO("fixture")
            }
        """.trimIndent())

        val unit = ModuleSourceDiscovery.forModule(moduleDir, "Demo", PlacementLevel.MODULE)
        assertNotNull(unit)
        assertEquals(2, unit.sources.size)

        val result = session.recompile(unit)
        assertTrue(
            result.success,
            "both files must compile atomically; the cross-reference Helper → UserCodeNode resolves"
        )
        val user = registry.getByName("User")
        assertNotNull(user, "User definition must be registered after a successful module recompile")
    }

    @Test
    fun `module recompile produces RecompileResult unit description naming the module`() = runTest {
        val moduleDir = File(workDir, "Demo").apply { mkdirs() }
        fixtureSourceFile(moduleDir, "FooCodeNode.kt", validCodeNodeSource("Foo"))
        val unit = ModuleSourceDiscovery.forModule(moduleDir, "Demo", PlacementLevel.MODULE)
        assertNotNull(unit)
        val result = session.recompile(unit)

        assertTrue(result.unit.description.contains("Demo"), "result must surface the module name in description")
    }
}
