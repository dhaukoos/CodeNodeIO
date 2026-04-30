/*
 * RecompileFeedbackPublisher - bridges RecompileResult / CompileDiagnostic to the
 * GraphEditor's ErrorConsolePanel + status line.
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.flowgraphinspect.compile.CompileDiagnostic
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
        throw NotImplementedError("T030 will implement RecompileFeedbackPublisher.publish")
    }

    /** Maps a single diagnostic to one [ErrorConsoleEntry]. Pure — no side effects. */
    fun toEntry(diagnostic: CompileDiagnostic, timestamp: Long = System.currentTimeMillis()): ErrorConsoleEntry {
        throw NotImplementedError("T030 will implement RecompileFeedbackPublisher.toEntry")
    }

    companion object {
        /** Source tag used for compile-related entries; lets the user distinguish from runtime errors. */
        const val SOURCE_TAG: String = "Compile"
    }
}
