/*
 * ClasspathSnapshot - launch-time classpath snapshot for the in-process compiler
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import java.io.File

/**
 * Snapshot of the GraphEditor's launch-time classpath. Read once at startup; reused for
 * every in-process compile invocation per Decision 4.
 *
 * Source-of-truth: `${projectRoot}/build/grapheditor-runtime-classpath.txt` (newline-
 * separated absolute JAR + class-dir paths) emitted by the DemoProject's
 * `writeRuntimeClasspath` task. Falls back to `System.getProperty("java.class.path")`
 * (path-separator-split) when that file is missing.
 */
class ClasspathSnapshot(
    val entries: List<String>,
    val source: Source
) {
    enum class Source { CLASSPATH_FILE, JAVA_CLASS_PATH_PROPERTY }

    /** Joins the entries with the platform path separator for `kotlinc`'s `-classpath`. */
    fun asPathString(separator: String = File.pathSeparator): String =
        entries.joinToString(separator)

    companion object {
        /**
         * Builds the snapshot by UNIONing entries from [classpathFile] (when present)
         * with [fallbackProperty] (`java.class.path` of the running JVM). Why both:
         *
         *  - **`java.class.path`** has every JAR the running JVM was launched with —
         *    including tool JARs (`fbpDsl`, `flowGraph-*`, `graphEditor`) that the
         *    compiler MUST see to resolve `CodeNodeDefinition`, `PortSpec`, etc.
         *  - **`grapheditor-runtime-classpath.txt`** has every project-module JAR
         *    DemoProject's `graphEditorRuntime` configuration resolves — but it is
         *    DELIBERATELY filtered to EXCLUDE tool JARs (those are passed via
         *    `JavaExec.classpath` separately). Reading it alone would omit fbpDsl.
         *
         * Non-existent entries are filtered silently — they're typically stale build
         * outputs from removed modules and would only generate compile-warning noise
         * without affecting correctness.
         */
        fun load(
            classpathFile: File?,
            fallbackProperty: String? = System.getProperty("java.class.path")
        ): ClasspathSnapshot {
            val fileEntries: List<String> = if (classpathFile != null && classpathFile.isFile) {
                classpathFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            } else emptyList()

            val propEntries: List<String> = (fallbackProperty ?: "")
                .split(File.pathSeparator)
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            // Union, de-dupe, and drop non-existent entries. java.class.path is the more
            // complete source for tool JARs; classpath.txt complements it with project
            // modules that may be project()-built but not yet on the launch classpath.
            val combined = (propEntries + fileEntries)
                .distinct()
                .filter { File(it).exists() }

            // Source enum captures provenance for diagnostics. Prefer the file when present
            // (it's the explicit project artifact); otherwise the JVM property.
            val source = if (fileEntries.isNotEmpty()) Source.CLASSPATH_FILE
                         else Source.JAVA_CLASS_PATH_PROPERTY

            return ClasspathSnapshot(entries = combined, source = source)
        }
    }
}
