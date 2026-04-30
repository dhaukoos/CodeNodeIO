/*
 * InProcessCompiler - thin wrapper over kotlin-compiler-embeddable's K2JVMCompiler
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.runtime.CodeNodeDefinition
import java.io.File
import java.net.URLClassLoader
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services

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
     * never throws on compilation failure (only on I/O errors).
     */
    suspend fun compile(unit: CompileUnit): CompileResult {
        val outputDir = cache.allocate(unit)

        val collector = CapturingMessageCollector()
        val friendPaths = deriveFriendPaths(unit)
        val args = K2JVMCompilerArguments().apply {
            // Source roots — the embeddable compiler accepts file paths in freeArgs.
            freeArgs = unit.sources.map { it.absolutePath }
            destination = outputDir.absolutePath
            classpath = classpathSnapshot.asPathString()
            // Stdlib + reflect are already on the parent classloader's classpath snapshot.
            noStdlib = true
            noReflect = true
            jvmTarget = "17"
            // -Xfriend-paths: declare the host module's existing build outputs as
            // "friend modules" so the in-process compile can access their `internal`
            // members. Without this, sibling files like DemoUISourceCodeNode.kt that
            // reference `internal val _a` in DemoUIState.kt fail with "Cannot access
            // 'val _a': it is internal in 'DemoUIState'" — the in-process compilation
            // is otherwise treated as a separate Kotlin module from the host JAR.
            if (friendPaths.isNotEmpty()) {
                this.friendPaths = friendPaths.toTypedArray()
            }
        }

        val exitCode = try {
            K2JVMCompiler().exec(collector, Services.EMPTY, args)
        } catch (t: Throwable) {
            // Defensive: any thrown exception becomes a synthetic ERROR diagnostic so
            // the caller never sees an uncaught throw.
            collector.report(
                CompilerMessageSeverity.ERROR,
                "Embedded compiler threw ${t::class.simpleName}: ${t.message}",
                null
            )
            ExitCode.INTERNAL_ERROR
        }

        if (exitCode != ExitCode.OK) {
            // Ensure at least one ERROR diagnostic exists (CompileResult.Failure invariant).
            if (collector.diagnostics.none { it.severity == CompileDiagnostic.Severity.ERROR }) {
                collector.report(
                    CompilerMessageSeverity.ERROR,
                    "Compiler returned non-OK exit code ($exitCode) with no diagnostics — likely a classpath or argument issue.",
                    null
                )
            }
            return CompileResult.Failure(unit = unit, diagnostics = collector.diagnostics)
        }

        // Walk the output dir; load every class via a discovery loader; pick out
        // CodeNodeDefinition singletons.
        val loaded = scanLoadedDefinitions(outputDir)
        if (loaded.isEmpty()) {
            // OK exit code but no CodeNodeDefinition found — degrade to Failure with a
            // synthetic diagnostic so callers get an actionable error rather than a
            // misleading Success-with-no-defs (CompileResult.Success requires
            // loadedDefinitionsByName non-empty).
            collector.report(
                CompilerMessageSeverity.ERROR,
                "Compile succeeded but produced no CodeNodeDefinition singletons. " +
                    "Sources: ${unit.sources.joinToString { it.absolutePath.substringAfterLast('/') }}",
                null
            )
            return CompileResult.Failure(unit = unit, diagnostics = collector.diagnostics)
        }

        return CompileResult.Success(
            unit = unit,
            diagnostics = collector.diagnostics,
            classOutputDir = outputDir.absolutePath,
            loadedDefinitionsByName = loaded
        )
    }

    /**
     * Computes `-Xfriend-paths` entries so the in-process compile can access
     * `internal` members of the host module's already-compiled classes.
     *
     * Heuristic: any classpath entry whose path contains `/${moduleName}/build/` for
     * some module name referenced by [unit]. This catches both the produced JAR
     * (`{moduleDir}/build/libs/{Module}-jvm-{version}.jar`) and the class output
     * directory (`{moduleDir}/build/classes/kotlin/jvm/main/`). Both carry the same
     * `.kotlin_module` metadata that defines the module identity for `internal`
     * visibility.
     */
    private fun deriveFriendPaths(unit: CompileUnit): List<String> {
        val moduleNames = unit.sources.mapNotNull { it.hostModuleName }.toSet()
        if (moduleNames.isEmpty()) return emptyList()
        return classpathSnapshot.entries.filter { entry ->
            moduleNames.any { mn -> entry.contains("/$mn/build/") }
        }
    }

    /**
     * Walks [outputDir] for `.class` files and discovers `CodeNodeDefinition` singletons
     * via reflection through a one-shot child-first loader (parent = current classloader).
     * Used only to populate `loadedDefinitionsByName`. The actual production
     * [ClassloaderScope] is constructed by [io.codenode.grapheditor.compile.RecompileSession]
     * from the same output dir; the discovery loader here is throwaway.
     */
    private fun scanLoadedDefinitions(outputDir: File): Map<String, String> {
        if (!outputDir.isDirectory) return emptyMap()
        val classFiles = outputDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".class") && !it.name.contains("$") }
            .toList()
        if (classFiles.isEmpty()) return emptyMap()

        val discoveryLoader = URLClassLoader(arrayOf(outputDir.toURI().toURL()), this::class.java.classLoader)
        val out = mutableMapOf<String, String>()
        for (cf in classFiles) {
            val rel = cf.relativeTo(outputDir).path
            val fqcn = rel.removeSuffix(".class").replace(File.separatorChar, '.').replace('/', '.')
            try {
                val clazz = discoveryLoader.loadClass(fqcn)
                val instanceField = runCatching { clazz.getField("INSTANCE") }.getOrNull() ?: continue
                val instance = runCatching { instanceField.get(null) }.getOrNull() ?: continue
                if (instance is CodeNodeDefinition) {
                    out[instance.name] = fqcn
                }
            } catch (_: ClassNotFoundException) {
                continue
            } catch (_: ExceptionInInitializerError) {
                continue
            } catch (_: NoClassDefFoundError) {
                continue
            } catch (_: LinkageError) {
                continue
            }
        }
        return out
    }

    /** Buffers every diagnostic the compiler emits into a [CompileDiagnostic] list. */
    private class CapturingMessageCollector : MessageCollector {
        val diagnostics: MutableList<CompileDiagnostic> = mutableListOf()
        private var hasErrors: Boolean = false

        override fun clear() = diagnostics.clear()
        override fun hasErrors(): Boolean = hasErrors

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            val mappedSeverity = when {
                severity.isError -> CompileDiagnostic.Severity.ERROR
                severity.isWarning -> CompileDiagnostic.Severity.WARNING
                else -> CompileDiagnostic.Severity.INFO
            }
            if (mappedSeverity == CompileDiagnostic.Severity.ERROR) hasErrors = true
            diagnostics += CompileDiagnostic(
                severity = mappedSeverity,
                filePath = location?.path,
                line = location?.line ?: 0,
                column = location?.column ?: 0,
                message = message,
                lineContent = location?.lineContent
            )
        }
    }
}
