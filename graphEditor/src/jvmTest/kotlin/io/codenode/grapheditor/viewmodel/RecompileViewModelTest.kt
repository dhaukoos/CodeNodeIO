/*
 * RecompileViewModelTest — TDD test for T045 (US3)
 * License: Apache 2.0
 */

package io.codenode.grapheditor.viewmodel

import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphinspect.compile.ClasspathSnapshot
import io.codenode.flowgraphinspect.compile.CompileSource
import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.InProcessCompiler
import io.codenode.flowgraphinspect.compile.SessionCompileCache
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import io.codenode.grapheditor.compile.PipelineQuiescer
import io.codenode.grapheditor.compile.RecompileFeedbackPublisher
import io.codenode.grapheditor.compile.RecompileSession
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

class RecompileViewModelTest {

    private lateinit var workDir: File
    private lateinit var session: RecompileSession
    private lateinit var bgScope: CoroutineScope

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("recompile-vm").toFile()
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
            registry = NodeDefinitionRegistry(),
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

    private fun fixtureUnit() = CompileUnit.SingleFile(
        CompileSource(
            absolutePath = File(workDir, "Missing.kt").absolutePath, // doesn't exist → compile fails
            tier = PlacementLevel.MODULE,
            hostModuleName = "Demo"
        )
    )

    @Test
    fun `initial state is not compiling and lastResult is null`() {
        val vm = RecompileViewModel(session, bgScope)
        assertFalse(vm.isCompiling.value)
        assertTrue(vm.lastResult.value == null)
    }

    @Test
    fun `recompile sets isCompiling and produces a non-null lastResult on completion`() = runBlocking {
        val vm = RecompileViewModel(session, bgScope)
        vm.recompile(fixtureUnit())

        // Poll briefly for completion (background coroutine — kotlinc cold start ≤ 30s).
        val deadline = System.currentTimeMillis() + 30_000
        while (vm.lastResult.value == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertNotNull(vm.lastResult.value, "lastResult must be populated after the background compile completes")
        assertFalse(vm.isCompiling.value, "isCompiling must clear after completion")
    }
}
