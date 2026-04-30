/*
 * ClassloaderScope - per-recompile-unit child-first classloader wrapper
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import java.io.File

/**
 * Wraps a [ChildFirstURLClassLoader] tied to one [CompileUnit]'s output. Lifetime: one
 * generation of the unit's compiled artifacts. When the next recompile of the same unit
 * lands, the registry replaces this scope's strong reference; this scope becomes
 * GC-eligible (Decision 6 / SC-004).
 *
 * @property unit The compile unit whose output this scope owns.
 * @property classOutputDir The directory holding the produced `.class` files.
 */
class ClassloaderScope(
    val unit: CompileUnit,
    val classOutputDir: File,
    parent: ClassLoader
) : AutoCloseable {

    /**
     * Loads the [fqcn] singleton via this scope; reads the Kotlin object's `INSTANCE`
     * field reflectively. Returns null if the FQCN can't be loaded or doesn't expose a
     * `CodeNodeDefinition` instance.
     */
    fun loadDefinition(fqcn: String): CodeNodeDefinition? {
        throw NotImplementedError("T026 will implement ClassloaderScope.loadDefinition")
    }

    /** Releases JAR file handles. Idempotent. */
    override fun close() {
        throw NotImplementedError("T026 will implement ClassloaderScope.close")
    }
}
