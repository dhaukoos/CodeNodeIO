/*
 * CompileResult - structured outcome of one in-process compile invocation
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

/**
 * Sealed outcome of one [InProcessCompiler.compile] invocation.
 *
 * Sealed (rather than carrying a Boolean) so consumers pattern-match without
 * second-class fields, and so the `Failure.diagnostics-non-empty-with-ERROR` and
 * `Success.loadedDefinitionsByName-non-empty` invariants are enforced at construction.
 */
sealed class CompileResult {
    abstract val unit: CompileUnit
    abstract val diagnostics: List<CompileDiagnostic>

    data class Success(
        override val unit: CompileUnit,
        override val diagnostics: List<CompileDiagnostic>,
        val classOutputDir: String,
        val loadedDefinitionsByName: Map<String, String>
    ) : CompileResult() {
        init {
            require(loadedDefinitionsByName.isNotEmpty()) {
                "CompileResult.Success.loadedDefinitionsByName must be non-empty — a compile that " +
                    "produced zero CodeNodeDefinitions is not a meaningful success."
            }
            require(classOutputDir.isNotBlank()) {
                "CompileResult.Success.classOutputDir must not be blank"
            }
        }
    }

    data class Failure(
        override val unit: CompileUnit,
        override val diagnostics: List<CompileDiagnostic>
    ) : CompileResult() {
        init {
            require(diagnostics.any { it.severity == CompileDiagnostic.Severity.ERROR }) {
                "CompileResult.Failure.diagnostics must contain at least one ERROR severity entry."
            }
        }
    }
}
