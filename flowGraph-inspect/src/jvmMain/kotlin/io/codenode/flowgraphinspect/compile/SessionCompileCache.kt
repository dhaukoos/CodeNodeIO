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
    /** Allocates a fresh subdirectory for [unit]'s output. Never reuses prior subdirs. */
    fun allocate(unit: CompileUnit): File {
        throw NotImplementedError("T024 will implement SessionCompileCache.allocate")
    }

    /** Best-effort recursive deletion of [rootDir]. Idempotent. */
    fun deleteAll() {
        throw NotImplementedError("T024 will implement SessionCompileCache.deleteAll")
    }
}
