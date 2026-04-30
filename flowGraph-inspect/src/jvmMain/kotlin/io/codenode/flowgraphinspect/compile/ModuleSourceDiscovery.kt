/*
 * ModuleSourceDiscovery - walks a module's source directories to build a
 * CompileUnit.Module covering every CodeNode source file.
 *
 * License: Apache 2.0
 */

package io.codenode.flowgraphinspect.compile

import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File

/**
 * Builds a [CompileUnit.Module] from a designated host module by walking standard
 * KMP source-directory layouts. Used by the GraphEditor's "Recompile module" control
 * (US2 / FR-004) to assemble the module's compile unit on demand.
 *
 * Conventions:
 *   - For [PlacementLevel.MODULE] / [PlacementLevel.PROJECT] the function walks
 *     `src/commonMain/kotlin/**/nodes/` and `src/jvmMain/kotlin/**/nodes/` for `.kt`
 *     files. (Same scan locations the GraphEditor's startup-time discovery uses.)
 *   - For [PlacementLevel.UNIVERSAL] the source root is a flat directory
 *     (`~/.codenode/nodes/` by convention); every `.kt` directly under it counts.
 */
object ModuleSourceDiscovery {

    /**
     * Builds a [CompileUnit.Module] for [moduleName] rooted at [moduleDir].
     *
     * @return non-null `CompileUnit.Module` when the directory exists AND has at
     *     least one `.kt` source under the conventional locations; null when no
     *     compilable source is found (an empty `Module` would violate
     *     `CompileUnit.Module.sources` non-empty invariant).
     */
    fun forModule(
        moduleDir: File,
        moduleName: String,
        tier: PlacementLevel
    ): CompileUnit.Module? {
        if (!moduleDir.isDirectory) return null

        val sources: List<CompileSource> = when (tier) {
            PlacementLevel.UNIVERSAL -> walkUniversalDir(moduleDir, moduleName)
            else -> walkModuleSourceSets(moduleDir, moduleName, tier)
        }

        if (sources.isEmpty()) return null
        return CompileUnit.Module(
            moduleName = moduleName,
            tier = tier,
            sources = sources
        )
    }

    /**
     * Convenience for the Universal tier — wraps the user's `~/.codenode/nodes/`
     * directory as a synthetic compile unit. Module name is the literal "Universal";
     * `hostModuleName` on each [CompileSource] is null per [CompileSource]'s rule
     * for UNIVERSAL tier.
     */
    fun forUniversal(universalDir: File): CompileUnit.Module? =
        forModule(universalDir, moduleName = "Universal", tier = PlacementLevel.UNIVERSAL)

    private fun walkModuleSourceSets(
        moduleDir: File,
        moduleName: String,
        tier: PlacementLevel
    ): List<CompileSource> {
        val sourceSets = listOf("src/commonMain/kotlin", "src/jvmMain/kotlin")
        return sourceSets.flatMap { sourceSet ->
            val srcDir = File(moduleDir, sourceSet)
            if (!srcDir.isDirectory) return@flatMap emptyList<CompileSource>()
            srcDir.walkTopDown()
                // Restrict to files under a `nodes/` parent — matches the existing
                // GraphEditor scan path (registry.scanDirectory).
                .filter { it.isFile && it.name.endsWith(".kt") && it.parentFile?.name == "nodes" }
                .map { file ->
                    CompileSource(
                        absolutePath = file.absolutePath,
                        tier = tier,
                        hostModuleName = moduleName
                    )
                }
                .toList()
        }
    }

    private fun walkUniversalDir(universalDir: File, @Suppress("UNUSED_PARAMETER") moduleName: String): List<CompileSource> {
        return universalDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".kt") }
            ?.map { file ->
                CompileSource(
                    absolutePath = file.absolutePath,
                    tier = PlacementLevel.UNIVERSAL,
                    hostModuleName = null
                )
            }
            ?: emptyList()
    }
}
