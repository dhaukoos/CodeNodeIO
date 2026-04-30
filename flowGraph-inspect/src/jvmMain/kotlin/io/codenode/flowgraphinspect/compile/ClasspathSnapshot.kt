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
        /** Reads the classpath file at [classpathFile]; falls back to [fallbackProperty] when absent. */
        fun load(classpathFile: File?, fallbackProperty: String? = System.getProperty("java.class.path")): ClasspathSnapshot {
            if (classpathFile != null && classpathFile.isFile) {
                val entries = classpathFile.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                return ClasspathSnapshot(entries = entries, source = Source.CLASSPATH_FILE)
            }
            // Fall back to the running JVM's java.class.path. Surface a one-time stderr
            // warning so launches that bypass `writeRuntimeClasspath` are visible.
            if (classpathFile != null) {
                System.err.println(
                    "[ClasspathSnapshot] grapheditor-runtime-classpath.txt not found at " +
                        "${classpathFile.absolutePath}; falling back to java.class.path. " +
                        "Run `./gradlew writeRuntimeClasspath` from the project root for a complete classpath."
                )
            }
            val entries = (fallbackProperty ?: "")
                .split(File.pathSeparator)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return ClasspathSnapshot(entries = entries, source = Source.JAVA_CLASS_PATH_PROPERTY)
        }
    }
}
