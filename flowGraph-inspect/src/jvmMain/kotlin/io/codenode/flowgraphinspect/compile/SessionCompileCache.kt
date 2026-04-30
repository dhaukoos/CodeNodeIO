/*
 * SessionCompileCache - per-GraphEditor-session temp directory for compiled .class output
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import java.io.File

/**
 * Owns the on-disk directory tree holding per-unit `.class` outputs for the current
 * GraphEditor session. One instance per [RecompileSession]; lifetime bounded by the
 * GraphEditor process.
 *
 * Layout: `${rootDir}/units/${slug}-${counter}/` per `allocate(unit)` invocation.
 * Counter increments per call so consecutive `allocate` calls for the same unit yield
 * distinct directories (Decision 6: old class files become GC-eligible when the
 * registry's strong reference is replaced).
 *
 * @property rootDir Session root directory. Conventionally `~/.codenode/cache/sessions/{uuid}/`.
 */
class SessionCompileCache(
    val rootDir: File
) {
    private var counter: Long = 0L

    /** Allocates a fresh subdirectory for [unit]'s output. Never reuses prior subdirs. */
    fun allocate(unit: CompileUnit): File {
        val slug = slugFor(unit)
        // Counter monotonically increases across all allocate calls in this session, so
        // consecutive calls for the SAME unit yield distinct subdirectories. Critical for
        // Decision 6 — old class files become GC-eligible only when their dir survives
        // the registry-replacement transition.
        val n = synchronized(this) { ++counter }
        val unitsRoot = File(rootDir, "units")
        val sub = File(unitsRoot, "$slug-$n")
        sub.mkdirs()
        return sub
    }

    /** Best-effort recursive deletion of [rootDir]. Idempotent. */
    fun deleteAll() {
        if (!rootDir.exists()) return
        rootDir.deleteRecursively()
    }

    /** Filesystem-safe identifier derived from [unit]'s human description. */
    private fun slugFor(unit: CompileUnit): String {
        val raw = when (unit) {
            is CompileUnit.SingleFile -> unit.source.absolutePath.substringAfterLast('/').removeSuffix(".kt")
            is CompileUnit.Module -> unit.moduleName
        }
        return raw.replace(Regex("[^A-Za-z0-9_]"), "_").ifEmpty { "unit" }
    }
}
