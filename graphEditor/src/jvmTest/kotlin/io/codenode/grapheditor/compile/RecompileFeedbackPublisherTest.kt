/*
 * RecompileFeedbackPublisherTest - TDD Red tests for diagnostic → ErrorConsoleEntry mapping
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphinspect.compile.CompileDiagnostic
import io.codenode.flowgraphinspect.compile.CompileResult
import io.codenode.flowgraphinspect.compile.CompileSource
import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.RecompileResult
import io.codenode.grapheditor.ui.ErrorConsoleEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecompileFeedbackPublisherTest {

    private val fixtureUnit = CompileUnit.SingleFile(
        CompileSource("/tmp/Foo.kt", PlacementLevel.MODULE, "Demo")
    )

    private fun makePublisher(
        entries: MutableList<ErrorConsoleEntry> = mutableListOf(),
        statuses: MutableList<String> = mutableListOf()
    ): Triple<RecompileFeedbackPublisher, List<ErrorConsoleEntry>, List<String>> {
        val publisher = RecompileFeedbackPublisher(
            onErrorEntry = { entries.add(it) },
            onStatusMessage = { statuses.add(it) }
        )
        return Triple(publisher, entries, statuses)
    }

    @Test
    fun `toEntry sets source to Compile`() {
        val (pub, _, _) = makePublisher()
        val d = CompileDiagnostic(
            severity = CompileDiagnostic.Severity.ERROR,
            filePath = "/abs/Foo.kt", line = 5, column = 1,
            message = "Unresolved reference"
        )
        val entry = pub.toEntry(d)
        assertEquals(RecompileFeedbackPublisher.SOURCE_TAG, entry.source)
    }

    @Test
    fun `toEntry message carries formatForConsole output`() {
        val (pub, _, _) = makePublisher()
        val d = CompileDiagnostic(
            severity = CompileDiagnostic.Severity.ERROR,
            filePath = "/abs/Foo.kt", line = 7, column = 1,
            message = "Bad token"
        )
        val entry = pub.toEntry(d)
        assertEquals(d.formatForConsole(), entry.message)
    }

    @Test
    fun `publish on Failure produces one entry per ERROR diagnostic`() {
        val (pub, entries, _) = makePublisher()
        val errors = listOf(
            CompileDiagnostic(
                severity = CompileDiagnostic.Severity.ERROR,
                filePath = "/abs/Foo.kt", line = 1, column = 1, message = "first error"
            ),
            CompileDiagnostic(
                severity = CompileDiagnostic.Severity.ERROR,
                filePath = "/abs/Foo.kt", line = 5, column = 1, message = "second error"
            )
        )
        pub.publish(
            RecompileResult(
                compileResult = CompileResult.Failure(unit = fixtureUnit, diagnostics = errors),
                durationMs = 100
            )
        )
        assertEquals(2, entries.size)
    }

    @Test
    fun `publish on Success with no warnings produces zero entries`() {
        val (pub, entries, statuses) = makePublisher()
        pub.publish(
            RecompileResult(
                compileResult = CompileResult.Success(
                    unit = fixtureUnit,
                    diagnostics = emptyList(),
                    classOutputDir = "/tmp/out",
                    loadedDefinitionsByName = mapOf("Foo" to "io.codenode.demo.FooCodeNode")
                ),
                durationMs = 100
            )
        )
        assertEquals(0, entries.size, "Success-with-no-warnings must produce zero error-console entries")
        assertEquals(1, statuses.size, "Success must surface a status-line summary")
    }

    @Test
    fun `publish on Success with warnings produces a WARNING entry`() {
        val (pub, entries, _) = makePublisher()
        val warning = CompileDiagnostic(
            severity = CompileDiagnostic.Severity.WARNING,
            filePath = "/abs/Foo.kt", line = 9, column = 1,
            message = "deprecated API usage"
        )
        pub.publish(
            RecompileResult(
                compileResult = CompileResult.Success(
                    unit = fixtureUnit,
                    diagnostics = listOf(warning),
                    classOutputDir = "/tmp/out",
                    loadedDefinitionsByName = mapOf("Foo" to "io.codenode.demo.FooCodeNode")
                ),
                durationMs = 100
            )
        )
        assertEquals(1, entries.size)
        assertTrue(entries.first().message.contains("deprecated"))
    }

    @Test
    fun `status-line summary names the recompiled unit`() {
        val (pub, _, statuses) = makePublisher()
        pub.publish(
            RecompileResult(
                compileResult = CompileResult.Success(
                    unit = fixtureUnit,
                    diagnostics = emptyList(),
                    classOutputDir = "/tmp/out",
                    loadedDefinitionsByName = mapOf("Foo" to "io.codenode.demo.FooCodeNode")
                ),
                durationMs = 100
            )
        )
        assertEquals(1, statuses.size)
        assertTrue(
            statuses.first().contains("Foo.kt") || statuses.first().contains(fixtureUnit.description),
            "status summary must reference the unit (file or module name); got: ${statuses.first()}"
        )
    }
}
