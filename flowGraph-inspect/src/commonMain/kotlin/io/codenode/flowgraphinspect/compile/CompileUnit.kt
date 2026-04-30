/*
 * CompileUnit - the atomic recompile group (per-file or per-module)
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel

/**
 * The atomic unit that the in-process compiler processes in one invocation.
 *
 * Two concrete shapes corresponding to FR-001 (per-file) and FR-004 (per-module).
 */
sealed class CompileUnit {
    /** All sources processed atomically by this unit. */
    abstract val sources: List<CompileSource>

    /** User-facing one-line description (e.g., for status-bar feedback). */
    abstract val description: String

    /**
     * One source file produced by an in-session generator (Node Generator only — FR-001 / FR-012).
     * Always exactly one source.
     */
    data class SingleFile(
        val source: CompileSource
    ) : CompileUnit() {
        override val sources: List<CompileSource> get() = listOf(source)
        override val description: String
            get() = "File: ${source.absolutePath.substringAfterLast('/')}"
    }

    /**
     * Every CodeNode source in a designated host module, compiled atomically by one
     * user-invoked "Recompile module" action (FR-004).
     */
    data class Module(
        val moduleName: String,
        val tier: PlacementLevel,
        override val sources: List<CompileSource>
    ) : CompileUnit() {
        init {
            require(moduleName.isNotBlank()) { "CompileUnit.Module.moduleName must not be blank" }
            require(sources.isNotEmpty()) { "CompileUnit.Module.sources must not be empty" }
        }

        override val description: String
            get() = "Module: $moduleName (${tier.name.lowercase()})"
    }
}
