/*
 * InProcessCompiler - thin wrapper over kotlin-compiler-embeddable's K2JVMCompiler
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

/**
 * In-process Kotlin compiler tied to the GraphEditor session. Wraps
 * [`K2JVMCompiler`][org.jetbrains.kotlin.cli.jvm.K2JVMCompiler] to compile [CompileUnit]
 * inputs against a [ClasspathSnapshot] and emit `.class` files under
 * [SessionCompileCache]-allocated directories.
 *
 * Concurrency: not internally synchronized. [RecompileSession] serializes calls.
 *
 * @property classpathSnapshot Read once at GraphEditor startup; reused across compiles.
 * @property cache Provides per-unit output directories.
 */
class InProcessCompiler(
    private val classpathSnapshot: ClasspathSnapshot,
    private val cache: SessionCompileCache
) {
    /**
     * Compile [unit]'s sources atomically. Returns a structured [CompileResult] —
     * never throws on compilation failure.
     */
    suspend fun compile(unit: CompileUnit): CompileResult {
        throw NotImplementedError("T027 will implement InProcessCompiler.compile")
    }
}
