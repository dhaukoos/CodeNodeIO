/*
 * RecompileResultTest - TDD Red tests for RecompileResult derived properties
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RecompileResultTest {

    private val fixtureUnit = CompileUnit.SingleFile(
        CompileSource("/tmp/Foo.kt", PlacementLevel.MODULE, "Demo")
    )

    @Test
    fun `success is true when compileResult is Success`() {
        val result = RecompileResult(
            compileResult = CompileResult.Success(
                unit = fixtureUnit,
                diagnostics = emptyList(),
                classOutputDir = "/tmp/out",
                loadedDefinitionsByName = mapOf("Foo" to "io.codenode.demo.FooCodeNode")
            ),
            durationMs = 750
        )
        assertTrue(result.success)
    }

    @Test
    fun `success is false when compileResult is Failure`() {
        val result = RecompileResult(
            compileResult = CompileResult.Failure(
                unit = fixtureUnit,
                diagnostics = listOf(
                    CompileDiagnostic(
                        severity = CompileDiagnostic.Severity.ERROR,
                        filePath = "/tmp/Foo.kt", line = 1, column = 1,
                        message = "Expecting '}'"
                    )
                )
            ),
            durationMs = 200
        )
        assertFalse(result.success)
    }

    @Test
    fun `unit accessor returns compileResult unit`() {
        val result = RecompileResult(
            compileResult = CompileResult.Success(
                unit = fixtureUnit,
                diagnostics = emptyList(),
                classOutputDir = "/tmp/out",
                loadedDefinitionsByName = mapOf("Foo" to "io.codenode.demo.FooCodeNode")
            ),
            durationMs = 100
        )
        assertSame(fixtureUnit, result.unit)
    }

    @Test
    fun `pipelinesQuiesced defaults to zero`() {
        val result = RecompileResult(
            compileResult = CompileResult.Success(
                unit = fixtureUnit,
                diagnostics = emptyList(),
                classOutputDir = "/tmp/out",
                loadedDefinitionsByName = mapOf("Foo" to "io.codenode.demo.FooCodeNode")
            ),
            durationMs = 100
        )
        assertEquals(0, result.pipelinesQuiesced)
    }

    @Test
    fun `pipelinesQuiesced surfaces explicit count`() {
        val result = RecompileResult(
            compileResult = CompileResult.Success(
                unit = fixtureUnit,
                diagnostics = emptyList(),
                classOutputDir = "/tmp/out",
                loadedDefinitionsByName = mapOf("Foo" to "io.codenode.demo.FooCodeNode")
            ),
            durationMs = 100,
            pipelinesQuiesced = 1
        )
        assertEquals(1, result.pipelinesQuiesced)
    }
}
