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

    /** The owned-package set for the loader: the union of every CompileSource's derived package. */
    private val ownedPackages: Set<String> = unit.sources
        .mapNotNull { source -> derivePackagePrefix(source) }
        .toSet()

    private val loader: ChildFirstURLClassLoader = ChildFirstURLClassLoader(
        urls = arrayOf(classOutputDir.toURI().toURL()),
        parent = parent,
        ownedPackages = ownedPackages
    )

    private var closed: Boolean = false

    /**
     * Loads the [fqcn] singleton via this scope; reads the Kotlin object's `INSTANCE`
     * field reflectively. Returns null if the FQCN can't be loaded or doesn't expose a
     * `CodeNodeDefinition` instance.
     */
    fun loadDefinition(fqcn: String): CodeNodeDefinition? {
        if (closed) return null
        return try {
            val clazz = loader.loadClass(fqcn)
            val instanceField = try {
                clazz.getField("INSTANCE")
            } catch (_: NoSuchFieldException) {
                return null // not a Kotlin singleton object
            }
            val instance = instanceField.get(null)
            instance as? CodeNodeDefinition
        } catch (_: ClassNotFoundException) {
            null
        } catch (_: ExceptionInInitializerError) {
            null
        } catch (_: NoClassDefFoundError) {
            null
        }
    }

    /** Releases JAR file handles. Idempotent. */
    override fun close() {
        if (closed) return
        closed = true
        try {
            loader.close()
        } catch (_: Exception) {
            // best-effort
        }
    }

    /**
     * Derives the package prefix from a [CompileSource]'s on-disk path. Best-effort —
     * walks up the directory tree looking for a recognized source-set boundary
     * (`commonMain/kotlin`, `jvmMain/kotlin`, `commonTest/kotlin`, `jvmTest/kotlin`,
     * `nodes`, `userInterface`, `controller`, `viewmodel`) and returns the directory
     * tree below as a dotted package name. Returns null when no recognizable boundary
     * is found.
     */
    private fun derivePackagePrefix(source: CompileSource): String? {
        val path = source.absolutePath
        val markers = listOf("/commonMain/kotlin/", "/jvmMain/kotlin/", "/commonTest/kotlin/", "/jvmTest/kotlin/")
        for (marker in markers) {
            val idx = path.indexOf(marker)
            if (idx >= 0) {
                val rel = path.substring(idx + marker.length).substringBeforeLast('/')
                if (rel.isNotEmpty()) return rel.replace('/', '.')
            }
        }
        // Heuristic fallback for Universal-tier sources at ~/.codenode/nodes/Foo.kt:
        // owned package is unknown; the loader will treat this as an unowned class.
        return null
    }
}
