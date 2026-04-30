/*
 * CrossUnitFailureIsolationTest — TDD Red regression test for FR-016:
 * a compile error in unit A MUST NOT prevent the user from working
 * with unrelated nodes B, C, etc. Failures are scoped to the failing
 * recompile unit.
 *
 * Added by /speckit.analyze remediation H3.
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import io.codenode.fbpdsl.runtime.NodeRuntime
import io.codenode.fbpdsl.runtime.PortSpec
import io.codenode.flowgraphinspect.compile.ClasspathSnapshot
import io.codenode.flowgraphinspect.compile.ClassloaderScope
import io.codenode.flowgraphinspect.compile.CompileSource
import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.InProcessCompiler
import io.codenode.flowgraphinspect.compile.SessionCompileCache
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CrossUnitFailureIsolationTest {

    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("xunit-failure").toFile()
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    private fun fixtureDefinition(name: String) = object : CodeNodeDefinition {
        override val name: String = name
        override val category: CodeNodeType = CodeNodeType.TRANSFORMER
        override val description: String = "fixture-$name"
        override val inputPorts: List<PortSpec> = listOf(PortSpec("input1", Any::class))
        override val outputPorts: List<PortSpec> = listOf(PortSpec("output1", Any::class))
        override fun createRuntime(name: String): NodeRuntime = TODO("fixture")
    }

    private fun fixtureScope(moduleName: String) = ClassloaderScope(
        unit = CompileUnit.Module(
            moduleName = moduleName,
            tier = PlacementLevel.MODULE,
            sources = listOf(CompileSource("/tmp/$moduleName/X.kt", PlacementLevel.MODULE, moduleName))
        ),
        classOutputDir = File(workDir, "$moduleName-out").apply { mkdirs() },
        parent = javaClass.classLoader
    )

    @Test
    fun `failed recompile of module A leaves both A's prior install AND module B's install intact`() = runTest {
        val registry = NodeDefinitionRegistry()
        val pipelineQuiescer = PipelineQuiescer()
        val publisher = RecompileFeedbackPublisher(onErrorEntry = {}, onStatusMessage = {})
        val classpath = ClasspathSnapshot(
            entries = System.getProperty("java.class.path").split(File.pathSeparator),
            source = ClasspathSnapshot.Source.JAVA_CLASS_PATH_PROPERTY
        )
        val cacheDir = File(workDir, "cache").apply { mkdirs() }
        val cache = SessionCompileCache(cacheDir)
        val compiler = InProcessCompiler(classpath, cache)
        val session = RecompileSession(
            compiler = compiler,
            registry = registry,
            pipelineQuiescer = pipelineQuiescer,
            publisher = publisher,
            sessionCacheDir = cacheDir
        )

        // Pre-install successful versions for module A and module B.
        val priorA = fixtureDefinition("NodeA")
        val priorB = fixtureDefinition("NodeB")
        registry.installSessionDefinition(fixtureScope("ModuleA"), "NodeA", priorA)
        registry.installSessionDefinition(fixtureScope("ModuleB"), "NodeB", priorB)

        // Attempt to recompile module A with a source guaranteed to fail (file doesn't exist).
        val failingUnit = CompileUnit.Module(
            moduleName = "ModuleA",
            tier = PlacementLevel.MODULE,
            sources = listOf(
                CompileSource(
                    File(workDir, "ModuleA-Missing.kt").absolutePath,
                    PlacementLevel.MODULE,
                    "ModuleA"
                )
            )
        )
        val result = session.recompile(failingUnit)
        assertTrue(!result.success, "missing source MUST yield Failure")

        // (a) Module A's prior install still resolves — FR-013.
        assertSame(
            priorA, registry.getByName("NodeA"),
            "FR-013: module A's prior session install MUST survive a failed recompile"
        )

        // (b) Module B's session install is untouched — FR-011.
        assertSame(
            priorB, registry.getByName("NodeB"),
            "FR-011: module B's session install MUST be untouched by a failed recompile of module A"
        )

        // (c) DynamicPipelineBuilder can still build a pipeline against module B's nodes.
        // We construct a tiny FlowGraph that references only NodeB and assert canBuildDynamic returns true.
        val flowGraph = io.codenode.fbpdsl.model.FlowGraph(
            id = "g_b",
            name = "BOnly",
            version = "1.0.0",
            rootNodes = listOf(
                io.codenode.fbpdsl.model.CodeNode(
                    id = "node_b",
                    name = "NodeB",
                    codeNodeType = CodeNodeType.TRANSFORMER,
                    position = io.codenode.fbpdsl.model.Node.Position(0.0, 0.0)
                )
            ),
            connections = emptyList()
        )
        assertTrue(
            io.codenode.fbpdsl.runtime.DynamicPipelineBuilder.canBuildDynamic(flowGraph) { name ->
                registry.getByName(name)
            },
            "module B's pipeline MUST still be constructable after module A's failed recompile (FR-016)"
        )
    }
}
