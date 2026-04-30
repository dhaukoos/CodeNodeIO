/*
 * GenerateAndPaletteIntegrationTest — T034 (US1) end-to-end test.
 *
 * Wires NodeGeneratorViewModel through a real RecompileSession and a real
 * NodeDefinitionRegistry; invokes generateCodeNode() against a temp module dir;
 * asserts the new node lands on the registry (FR-001 / FR-002 / FR-017) and that
 * its createRuntime() returns a usable NodeRuntime instance.
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphgenerate.viewmodel.NodeAutoCompileHook
import io.codenode.flowgraphgenerate.viewmodel.NodeGeneratorViewModel
import io.codenode.flowgraphinspect.compile.ClasspathSnapshot
import io.codenode.flowgraphinspect.compile.InProcessCompiler
import io.codenode.flowgraphinspect.compile.SessionCompileCache
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GenerateAndPaletteIntegrationTest {

    private lateinit var workDir: File
    private lateinit var session: RecompileSession
    private lateinit var registry: NodeDefinitionRegistry
    private lateinit var bgScope: CoroutineScope

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("gen-and-palette").toFile()
        registry = NodeDefinitionRegistry()
        bgScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
        bgScope.cancel()
        workDir.deleteRecursively()
    }

    /** Sets up a fixture module dir at workDir/Demo/ with the standard KMP layout. */
    private fun fixtureModuleDir(): File =
        File(workDir, "Demo").apply { mkdirs() }

    /**
     * Real auto-compile hook bridging NodeGeneratorViewModel to RecompileSession via a
     * background coroutine scope. Returns a CompletableFuture so the test can await
     * the recompile's completion before asserting.
     */
    private fun makeAwaitableHook(): Pair<NodeAutoCompileHook, CompletableFuture<Unit>> {
        val done = CompletableFuture<Unit>()
        val hook = NodeAutoCompileHook { file, tier, hostModule ->
            bgScope.launch {
                try {
                    session.recompileGenerated(file = file, tier = tier, hostModule = hostModule)
                } finally {
                    done.complete(Unit)
                }
            }
        }
        return hook to done
    }

    @Test
    fun `generated CodeNode lands on the registry within 30 seconds`() = runBlocking {
        val moduleDir = fixtureModuleDir()
        val (hook, done) = makeAwaitableHook()

        val viewmodel = NodeGeneratorViewModel(
            registry = registry,
            projectRoot = workDir,
            autoCompileHook = hook
        )
        viewmodel.setModuleLoaded(true, moduleDir.absolutePath)
        viewmodel.setName("MyGeneratedNode")
        viewmodel.setCategory(CodeNodeType.TRANSFORMER)
        viewmodel.setInputCount(1)
        viewmodel.setOutputCount(1)
        viewmodel.setPlacementLevel(PlacementLevel.MODULE)

        val outputFile = viewmodel.generateCodeNode()
        assertNotNull(outputFile, "generateCodeNode must succeed for valid input")
        assertTrue(outputFile.exists(), "the source file must exist on disk before the hook fires")

        // Wait up to 30s — generous for the cold compile.
        done.get(30, TimeUnit.SECONDS)

        // Registry should now resolve the node via session install (FR-017).
        val resolved = registry.getByName("MyGeneratedNode")
        assertNotNull(
            resolved,
            "after the auto-compile hook completes, registry.getByName must return the new definition"
        )
        assertEquals("MyGeneratedNode", resolved.name)
        // The placeholder skeleton's createRuntime() must return a real NodeRuntime
        // (proves the placeholder logic compiles and is executable — FR-002).
        val runtime = resolved.createRuntime("MyGeneratedNode")
        assertNotNull(runtime, "createRuntime must return a non-null NodeRuntime instance")
    }

    @Test
    fun `registry version flow ticks on session install`() = runBlocking {
        val moduleDir = fixtureModuleDir()
        val (hook, done) = makeAwaitableHook()

        val versionBefore = registry.version.value

        val viewmodel = NodeGeneratorViewModel(
            registry = registry,
            projectRoot = workDir,
            autoCompileHook = hook
        )
        viewmodel.setModuleLoaded(true, moduleDir.absolutePath)
        viewmodel.setName("VersionFlowProbe")
        viewmodel.setCategory(CodeNodeType.TRANSFORMER)
        viewmodel.setInputCount(1)
        viewmodel.setOutputCount(1)
        viewmodel.setPlacementLevel(PlacementLevel.MODULE)
        viewmodel.generateCodeNode()
        done.get(30, TimeUnit.SECONDS)

        val versionAfter = registry.version.value
        assertTrue(
            versionAfter > versionBefore,
            "registry.version must tick after a session install (palette can subscribe to refresh)"
        )
    }
}
