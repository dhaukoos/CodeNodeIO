/*
 * RecompileFeedbackPublisher - bridges RecompileResult / CompileDiagnostic to the
 * GraphEditor's ErrorConsolePanel + status line.
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.flowgraphinspect.compile.CompileDiagnostic
import io.codenode.flowgraphinspect.compile.CompileResult
import io.codenode.flowgraphinspect.compile.RecompileResult
import io.codenode.grapheditor.ui.ErrorConsoleEntry

/**
 * Publishes recompile outcomes to the GraphEditor's user-visible surfaces.
 *
 * Each ERROR or WARNING [CompileDiagnostic] becomes one [ErrorConsoleEntry] in the
 * existing panel introduced by feature 084. Successful recompiles with no warnings
 * produce zero entries. The status line receives a compact one-line summary.
 */
class RecompileFeedbackPublisher(
    private val onErrorEntry: (ErrorConsoleEntry) -> Unit,
    private val onStatusMessage: (String) -> Unit
) {
    /**
     * Surfaces [result] to both surfaces. Idempotent under repeated invocation with the
     * same result (each call appends to the console; the status line shows the latest).
     */
    fun publish(result: RecompileResult) {
        // Surface every ERROR + WARNING as a console entry; INFO is dropped (compiler
        // chatter not useful at the user level).
        for (d in result.compileResult.diagnostics) {
            if (d.severity == CompileDiagnostic.Severity.INFO) continue
            onErrorEntry(toEntry(d))
        }
        // Status-line summary always fires — both Success and Failure.
        onStatusMessage(formatStatusLine(result))
    }

    /** Maps a single diagnostic to one [ErrorConsoleEntry]. Pure — no side effects. */
    fun toEntry(diagnostic: CompileDiagnostic, timestamp: Long = System.currentTimeMillis()): ErrorConsoleEntry =
        ErrorConsoleEntry(
            timestamp = timestamp,
            source = SOURCE_TAG,
            message = diagnostic.formatForConsole()
        )

    /** Formats a one-line summary for the GraphEditor's status bar. */
    private fun formatStatusLine(result: RecompileResult): String {
        val outcome = if (result.success) "Compiled" else "Compile failed:"
        val errorCount = result.compileResult.diagnostics.count {
            it.severity == CompileDiagnostic.Severity.ERROR
        }
        val warnCount = result.compileResult.diagnostics.count {
            it.severity == CompileDiagnostic.Severity.WARNING
        }
        val countSuffix = buildString {
            when (val cr = result.compileResult) {
                is CompileResult.Success -> {
                    val n = cr.loadedDefinitionsByName.size
                    append(" ($n definition${if (n == 1) "" else "s"}")
                    if (warnCount > 0) append(", $warnCount warning${if (warnCount == 1) "" else "s"}")
                    append(")")
                }
                is CompileResult.Failure -> {
                    append(" ($errorCount error${if (errorCount == 1) "" else "s"}")
                    if (warnCount > 0) append(", $warnCount warning${if (warnCount == 1) "" else "s"}")
                    append(")")
                }
            }
        }
        val quiesce = if (result.pipelinesQuiesced > 0) {
            " · stopped ${result.pipelinesQuiesced} pipeline${if (result.pipelinesQuiesced == 1) "" else "s"} first"
        } else ""
        return "$outcome ${result.unit.description}$countSuffix · ${result.durationMs}ms$quiesce"
    }

    companion object {
        /** Source tag used for compile-related entries; lets the user distinguish from runtime errors. */
        const val SOURCE_TAG: String = "Compile"
    }
}
