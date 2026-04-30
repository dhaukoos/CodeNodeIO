/*
 * CompileDiagnosticTest - TDD Red tests for CompileDiagnostic.formatForConsole()
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompileDiagnosticTest {

    @Test
    fun `formatForConsole emits file colon line prefix when both present`() {
        val d = CompileDiagnostic(
            severity = CompileDiagnostic.Severity.ERROR,
            filePath = "/abs/path/Foo.kt",
            line = 42,
            column = 7,
            message = "Unresolved reference: Bar"
        )
        assertEquals("[Foo.kt:42] Unresolved reference: Bar", d.formatForConsole())
    }

    @Test
    fun `formatForConsole strips parent directories from filePath`() {
        val d = CompileDiagnostic(
            severity = CompileDiagnostic.Severity.WARNING,
            filePath = "/Users/me/CodeNodeIO/foo/bar/baz/Quux.kt",
            line = 3,
            column = 1,
            message = "deprecation"
        )
        assertTrue(d.formatForConsole().startsWith("[Quux.kt:3]"))
    }

    @Test
    fun `formatForConsole omits line when line is 0`() {
        val d = CompileDiagnostic(
            severity = CompileDiagnostic.Severity.ERROR,
            filePath = "/abs/Foo.kt",
            line = 0,
            column = 0,
            message = "compiler internal"
        )
        assertEquals("[Foo.kt] compiler internal", d.formatForConsole())
    }

    @Test
    fun `formatForConsole returns bare message when filePath is null`() {
        val d = CompileDiagnostic(
            severity = CompileDiagnostic.Severity.ERROR,
            filePath = null,
            line = 0,
            column = 0,
            message = "no file context"
        )
        assertEquals("no file context", d.formatForConsole())
    }

    @Test
    fun `formatForConsole preserves multi-line messages`() {
        val d = CompileDiagnostic(
            severity = CompileDiagnostic.Severity.ERROR,
            filePath = "/abs/Foo.kt",
            line = 5,
            column = 1,
            message = "first line\nsecond line"
        )
        val out = d.formatForConsole()
        assertTrue(out.contains("\n"), "multi-line message must survive in formatForConsole output: $out")
    }
}
