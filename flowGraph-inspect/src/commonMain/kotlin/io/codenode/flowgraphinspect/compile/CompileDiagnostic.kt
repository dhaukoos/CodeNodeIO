/*
 * CompileDiagnostic - one message produced by the in-process compiler
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

/**
 * One diagnostic message captured from the embedded Kotlin compiler.
 *
 * @property severity Diagnostic severity. ERROR diagnostics signal compile failure.
 * @property filePath Absolute path to the offending source file. Null only for
 *     compiler-internal messages without a source location.
 * @property line 1-based line number; 0 if not file-local.
 * @property column 1-based column number; 0 if not file-local.
 * @property message Human-readable diagnostic text.
 * @property lineContent The offending source line, when the compiler provides it.
 */
data class CompileDiagnostic(
    val severity: Severity,
    val filePath: String?,
    val line: Int,
    val column: Int,
    val message: String,
    val lineContent: String? = null
) {
    enum class Severity { ERROR, WARNING, INFO }

    /**
     * One-line copyable form: `[file:line] message`. Falls back to the bare message
     * when [filePath] is null. Multi-line messages are preserved as-is.
     */
    fun formatForConsole(): String = buildString {
        if (filePath != null) {
            append("[")
            append(filePath.substringAfterLast('/'))
            if (line > 0) {
                append(":")
                append(line)
            }
            append("] ")
        }
        append(message)
    }
}
