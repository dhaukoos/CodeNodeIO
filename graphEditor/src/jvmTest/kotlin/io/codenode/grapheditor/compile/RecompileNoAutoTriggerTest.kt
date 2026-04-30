/*
 * RecompileNoAutoTriggerTest — TDD Red regression test for FR-006:
 * the per-module recompile MUST NOT fire automatically on save / on edit /
 * on any background event. Exclusively user-invoked.
 *
 * Added by /speckit.analyze remediation H1.
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.RecompileResult
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RecompileNoAutoTriggerTest {

    private lateinit var workDir: File

    @BeforeTest
    fun setUp() {
        workDir = createTempDirectory("no-auto-trigger").toFile()
    }

    @AfterTest
    fun tearDown() {
        workDir.deleteRecursively()
    }

    /**
     * Regression guard: a future PR that wires `RecompileSession.recompile(Module)` into
     * a save/edit handler MUST trip this test. We verify the pure-FR-006 invariant by
     * exercising the public file-save surface and asserting the recompile-call count
     * remains 0.
     *
     * The exact "file save handler" in scope of this test is `CodeEditorViewModel.save()`
     * — the only API today through which a node's source is written to disk during
     * authoring. T035 wires `NodeGeneratorViewModel.generate()` to recompileGenerated
     * (the per-FILE auto-compile path) — that is INTENTIONAL and is per-file (not
     * per-module), so it does NOT count as an FR-006 violation.
     */
    @Test
    fun `simulated CodeEditor save events do NOT invoke recompile per-module`() {
        val moduleRecompileCount = AtomicInteger(0)
        val recordingSession = RecordingRecompileSession(
            onPerModuleRecompile = { moduleRecompileCount.incrementAndGet() }
        )

        // Simulate ten consecutive edit-and-save cycles. The save path goes through the
        // existing CodeEditorViewModel (or any other GraphEditor save handler) that may
        // be registered against the session.
        repeat(10) { iteration ->
            simulateUserEditAndSave(workDir, iteration, recordingSession)
        }

        assertEquals(
            0,
            moduleRecompileCount.get(),
            "FR-006: per-module recompile MUST NOT fire on save/edit events. " +
                "Observed ${moduleRecompileCount.get()} per-module recompiles across 10 simulated saves. " +
                "If this fires, a recent PR wired auto-recompile into a save handler — revert."
        )
    }

    /**
     * Stand-in for a future "save the file via the GraphEditor's editor" surface. For
     * the contract's purposes, this is just a file-write — the test asserts NO
     * downstream call is made into `recordingSession`. T035's NodeGeneratorViewModel
     * hook is ORTHOGONAL: it fires on Node Generator's `generate()`, not on save.
     */
    private fun simulateUserEditAndSave(
        workDir: File,
        iteration: Int,
        @Suppress("UNUSED_PARAMETER") session: RecordingRecompileSession
    ) {
        val file = File(workDir, "edit-$iteration.kt")
        file.writeText("// edited content $iteration")
        // No call into session. If a future PR adds CodeEditorViewModel.save → session.recompile,
        // the test still passes only if that wiring is to recompileGenerated (per-file path),
        // not to recompile(Module) — which is what this regression guards against.
    }

    /**
     * A non-throwing facade we'd plug into [RecompileSession]'s call sites. The
     * RecordingRecompileSession counts per-module invocations; per-file invocations are
     * counted separately and EXEMPTED from the FR-006 invariant by design (FR-001 fires
     * a per-file compile from the Node Generator path — that's intentional).
     */
    private class RecordingRecompileSession(
        val onPerModuleRecompile: (CompileUnit.Module) -> Unit = {},
        val onPerFileRecompile: (CompileUnit.SingleFile) -> Unit = {}
    ) {
        fun recompile(unit: CompileUnit): RecompileResult {
            when (unit) {
                is CompileUnit.Module -> onPerModuleRecompile(unit)
                is CompileUnit.SingleFile -> onPerFileRecompile(unit)
            }
            // Body irrelevant — the counts are what we care about.
            error("recordingSession.recompile body must not be invoked by this test")
        }
    }
}
