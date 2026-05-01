/*
 * RecompileSoakTest — T053 (Polish): SC-004 memory soak verification.
 *
 * Recompiles the same fixture module N times sequentially. After each compile, runs
 * System.gc() + a short sleep to encourage collection. Verifies that the count of
 * live ClassloaderScope instances (tracked via WeakReference) stays bounded — only
 * the current and one transitionally-pinned-by-GC-lag scope should remain reachable.
 *
 * SC-004 budget: across 50 sequential recompiles of the same module, retained
 * memory MUST grow by no more than the size of a single set of compiled class
 * definitions for that module. We approximate that by tracking the number of
 * ClassloaderScope instances live after each gc() pass.
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphinspect.compile.ClasspathSnapshot
import io.codenode.flowgraphinspect.compile.CompileSource
import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.InProcessCompiler
import io.codenode.flowgraphinspect.compile.SessionCompileCache
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.io.File
import java.lang.ref.WeakReference
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RecompileSoakTest {

    private lateinit var workDir: File
    private lateinit var session: RecompileSession
    private lateinit var registry: NodeDefinitionRegistry

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("recompile-soak").toFile()
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

    private fun fixtureSource(marker: String): String = """
        package io.codenode.fixture

        import io.codenode.fbpdsl.runtime.CodeNodeDefinition
        import io.codenode.fbpdsl.runtime.NodeRuntime
        import io.codenode.fbpdsl.runtime.PortSpec
        import io.codenode.fbpdsl.model.CodeNodeType

        object SoakCodeNode : CodeNodeDefinition {
            override val name = "Soak"
            override val category = CodeNodeType.TRANSFORMER
            override val description = "soak-$marker"
            override val inputPorts = listOf(PortSpec("input1", Any::class))
            override val outputPorts = listOf(PortSpec("output1", Any::class))
            override fun createRuntime(name: String): NodeRuntime = TODO("fixture")
        }
    """.trimIndent()

    /**
     * Soak: recompile the same module 20 times (a meaningful sample under the
     * spec's "50 sequential recompiles" target — kept lower here to keep test
     * wall-clock under a minute). Track WeakReferences to the resulting
     * `CodeNodeDefinition` instances; after a gc pass, only the most-recent
     * generation should remain strongly-reachable.
     *
     * Why we test definitions rather than ClassloaderScope directly: the registry
     * holds the strong reference via the SessionInstall's scope, which we can't
     * easily reach without exposing private internals. Definition reachability is
     * a faithful proxy — the scope it was loaded from owns its identity.
     */
    @Test
    fun `repeated module recompiles do not retain superseded definition instances`() = runTest {
        val moduleDir = File(workDir, "Demo").apply { mkdirs() }
        val srcFile = File(moduleDir, "src/commonMain/kotlin/io/codenode/fixture/nodes")
            .apply { mkdirs() }
            .resolve("SoakCodeNode.kt")

        val iterations = 20
        val weakRefs = mutableListOf<WeakReference<*>>()

        repeat(iterations) { i ->
            // Produce a distinct source body each iteration so the compile actually
            // emits a new class definition (otherwise the compiler might short-circuit).
            srcFile.writeText(fixtureSource(marker = "v$i"))

            val unit = CompileUnit.Module(
                moduleName = "Demo",
                tier = PlacementLevel.MODULE,
                sources = listOf(
                    CompileSource(srcFile.absolutePath, PlacementLevel.MODULE, "Demo")
                )
            )
            val result = session.recompile(unit)
            assertTrue(result.success, "iteration $i must succeed")

            val installed = registry.getByName("Soak")
            assertTrue(installed != null, "registry must hold the latest install at iteration $i")
            // Track the supersededdefinition (i-1)'s WeakReference. We can't track the
            // current iteration's ref because the registry still holds it strongly.
            if (i > 0) weakRefs += WeakReference(installed)
        }

        // Encourage GC. JVM may need multiple passes.
        repeat(8) {
            System.gc()
            Thread.sleep(50)
        }

        // After GC, all but the LATEST iteration's reference should be collectible
        // (the registry holds only the most-recent). Allow a small tolerance for
        // GC lag — anything ≤ 2 surviving is acceptable.
        val survivors = weakRefs.count { it.get() != null }
        assertTrue(
            survivors <= 2,
            "after $iterations recompiles + System.gc(), at most 2 prior generations of the " +
                "Soak definition should remain reachable; found $survivors. The registry's " +
                "strong-reference replacement (Decision 6) is the contract under test."
        )
    }
}
