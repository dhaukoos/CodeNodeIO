/*
 * CompileResultTest - TDD Red tests for CompileResult invariants
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompileResultTest {

    private val fixtureUnit = CompileUnit.SingleFile(
        CompileSource("/tmp/Foo.kt", PlacementLevel.MODULE, "Demo")
    )

    @Test
    fun `Failure with no diagnostics is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CompileResult.Failure(unit = fixtureUnit, diagnostics = emptyList())
        }
    }

    @Test
    fun `Failure with only WARNING diagnostics is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CompileResult.Failure(
                unit = fixtureUnit,
                diagnostics = listOf(
                    CompileDiagnostic(
                        severity = CompileDiagnostic.Severity.WARNING,
                        filePath = "/tmp/Foo.kt", line = 1, column = 1,
                        message = "deprecated API"
                    )
                )
            )
        }
    }

    @Test
    fun `Failure with at least one ERROR is permitted`() {
        val failure = CompileResult.Failure(
            unit = fixtureUnit,
            diagnostics = listOf(
                CompileDiagnostic(
                    severity = CompileDiagnostic.Severity.ERROR,
                    filePath = "/tmp/Foo.kt", line = 1, column = 1,
                    message = "Expecting '}'"
                )
            )
        )
        assertTrue(failure.diagnostics.any { it.severity == CompileDiagnostic.Severity.ERROR })
    }

    @Test
    fun `Success with empty loadedDefinitionsByName is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CompileResult.Success(
                unit = fixtureUnit,
                diagnostics = emptyList(),
                classOutputDir = "/tmp/out",
                loadedDefinitionsByName = emptyMap()
            )
        }
    }

    @Test
    fun `Success with blank classOutputDir is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CompileResult.Success(
                unit = fixtureUnit,
                diagnostics = emptyList(),
                classOutputDir = "",
                loadedDefinitionsByName = mapOf("Foo" to "io.codenode.demo.FooCodeNode")
            )
        }
    }

    @Test
    fun `Success carries warnings on the success path`() {
        val warning = CompileDiagnostic(
            severity = CompileDiagnostic.Severity.WARNING,
            filePath = "/tmp/Foo.kt", line = 5, column = 1,
            message = "deprecated"
        )
        val success = CompileResult.Success(
            unit = fixtureUnit,
            diagnostics = listOf(warning),
            classOutputDir = "/tmp/out",
            loadedDefinitionsByName = mapOf("Foo" to "io.codenode.demo.FooCodeNode")
        )
        assertEquals(listOf(warning), success.diagnostics)
    }
}
