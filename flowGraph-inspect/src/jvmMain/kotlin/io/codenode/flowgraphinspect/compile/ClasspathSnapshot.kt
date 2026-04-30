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
            throw NotImplementedError("T023 will implement ClasspathSnapshot.load")
        }
    }
}
