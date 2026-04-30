/*
 * NodeGeneratorAutoCompileTest — TDD Red test for T033 (US1).
 *
 * Verifies that NodeGeneratorViewModel.generateCodeNode() invokes the
 * NodeAutoCompileHook exactly once, with the right (file, tier, hostModule)
 * triple, AFTER the source file is written to disk. Uses a recording fake hook;
 * the real RecompileSession-backed hook is integration-tested in
 * GenerateAndPaletteIntegrationTest (graphEditor jvmTest).
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.viewmodel

import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphinspect.registry.NodeDefinitionRegistry
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeGeneratorAutoCompileTest {

    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("auto-compile-hook").toFile()
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    /** Recording hook that captures every onGenerated call for assertion. */
    private class RecordingHook : NodeAutoCompileHook {
        data class Call(val file: File, val tier: PlacementLevel, val hostModule: String?)
        val calls = mutableListOf<Call>()
        override fun onGenerated(file: File, tier: PlacementLevel, hostModule: String?) {
            calls += Call(file, tier, hostModule)
        }
    }

    /** Builds a minimal fake module skeleton at workDir/Demo/. */
    private fun fixtureModuleDir(name: String = "Demo"): File =
        File(workDir, name).apply { mkdirs() }

    @Test
    fun `generateCodeNode invokes hook exactly once with correct triple for MODULE tier`() {
        val moduleDir = fixtureModuleDir("Demo")
        val hook = RecordingHook()

        val viewmodel = NodeGeneratorViewModel(
            registry = NodeDefinitionRegistry(),
            projectRoot = workDir,
            autoCompileHook = hook
        )
        viewmodel.setModuleLoaded(true, moduleDir.absolutePath)
        viewmodel.setName("Foo")
        viewmodel.setCategory(CodeNodeType.TRANSFORMER)
        viewmodel.setInputCount(1)
        viewmodel.setOutputCount(1)
        viewmodel.setPlacementLevel(PlacementLevel.MODULE)

        val outputFile = viewmodel.generateCodeNode()
        assertNotNull(outputFile, "generation must succeed for valid input")
        assertTrue(outputFile.exists(), "source file must be written before hook fires")

        assertEquals(1, hook.calls.size, "hook must be invoked exactly once per generation")
        val call = hook.calls.single()
        assertEquals(outputFile.absolutePath, call.file.absolutePath, "hook receives the generated file")
        assertEquals(PlacementLevel.MODULE, call.tier)
        assertEquals(moduleDir.name, call.hostModule, "hook receives the host module's directory name")
    }

    @Test
    fun `hook is NOT invoked when generation fails`() {
        val moduleDir = fixtureModuleDir("Demo")
        val hook = RecordingHook()

        // Make generation fail by reusing a name that conflicts with an existing registered node.
        val registry = NodeDefinitionRegistry().apply {
            // Registering a template with the same name triggers nameExists check.
            registerTemplate(
                io.codenode.flowgraphinspect.registry.NodeTemplateMeta(
                    name = "Conflict",
                    category = CodeNodeType.TRANSFORMER,
                    inputCount = 1, outputCount = 1,
                    filePath = "/tmp/Conflict.kt"
                )
            )
        }

        val viewmodel = NodeGeneratorViewModel(
            registry = registry,
            projectRoot = workDir,
            autoCompileHook = hook
        )
        viewmodel.setModuleLoaded(true, moduleDir.absolutePath)
        viewmodel.setName("Conflict") // conflict
        viewmodel.setCategory(CodeNodeType.TRANSFORMER)
        viewmodel.setInputCount(1)
        viewmodel.setOutputCount(1)
        viewmodel.setPlacementLevel(PlacementLevel.MODULE)

        val outputFile = viewmodel.generateCodeNode()
        assertNull(outputFile, "generation must return null on conflict")
        assertEquals(0, hook.calls.size, "hook must NOT fire when generation fails")
    }

    @Test
    fun `default NoOp hook is safe — viewmodel works without explicit hook injection`() {
        val moduleDir = fixtureModuleDir("Demo")

        // Construct without explicit autoCompileHook — should default to NoOp.
        val viewmodel = NodeGeneratorViewModel(
            registry = NodeDefinitionRegistry(),
            projectRoot = workDir
        )
        viewmodel.setModuleLoaded(true, moduleDir.absolutePath)
        viewmodel.setName("Bar")
        viewmodel.setCategory(CodeNodeType.TRANSFORMER)
        viewmodel.setInputCount(1)
        viewmodel.setOutputCount(1)
        viewmodel.setPlacementLevel(PlacementLevel.MODULE)

        // Must not throw — NoOp is benign.
        val outputFile = viewmodel.generateCodeNode()
        assertNotNull(outputFile)
    }

    @Test
    fun `hook fires AFTER the file is on disk (file readable when hook is invoked)`() {
        val moduleDir = fixtureModuleDir("Demo")
        val capturedFileExistsAtCallTime = AtomicReference<Boolean?>(null)
        val hook = NodeAutoCompileHook { file, _, _ ->
            capturedFileExistsAtCallTime.set(file.exists() && file.isFile && file.readText().isNotEmpty())
        }

        val viewmodel = NodeGeneratorViewModel(
            registry = NodeDefinitionRegistry(),
            projectRoot = workDir,
            autoCompileHook = hook
        )
        viewmodel.setModuleLoaded(true, moduleDir.absolutePath)
        viewmodel.setName("Baz")
        viewmodel.setCategory(CodeNodeType.TRANSFORMER)
        viewmodel.setInputCount(1)
        viewmodel.setOutputCount(1)
        viewmodel.setPlacementLevel(PlacementLevel.MODULE)

        viewmodel.generateCodeNode()
        assertEquals(
            true, capturedFileExistsAtCallTime.get(),
            "hook MUST fire after the file is written + has content"
        )
    }
}
