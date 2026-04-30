/*
 * RecompileTargetResolver - resolves a node / source file to its CompileUnit.Module
 *
 * Routes Module-tier nodes to their host module's directory; Project-tier nodes to
 * the project's shared `:nodes` module; Universal-tier nodes to `~/.codenode/nodes/`
 * (treated as a synthetic compilation unit). Reused by the toolbar's "Recompile
 * module" button and the Code Editor's "Recompile" action so both paths produce
 * consistent feedback through the same RecompileSession invocation.
 *
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compile

import io.codenode.fbpdsl.model.PlacementLevel
import io.codenode.flowgraphinspect.compile.CompileUnit
import io.codenode.flowgraphinspect.compile.ModuleSourceDiscovery
import java.io.File

class RecompileTargetResolver(
    private val projectRoot: File,
    /**
     * Override-able for tests; defaults to the user's home `~/.codenode/nodes/`.
     */
    private val universalDirProvider: () -> File = {
        File(System.getProperty("user.home"), ".codenode/nodes")
    }
) {
    /**
     * Resolves the appropriate [CompileUnit.Module] for [tier], using [activeModuleDir]
     * as the host module when the tier is MODULE.
     *
     * @return the unit to pass to `RecompileSession.recompile(...)`, or null when the
     *     tier's source root has no compilable sources (an empty `Module` would
     *     violate the unit's non-empty invariant).
     */
    fun resolve(tier: PlacementLevel, activeModuleDir: File?): CompileUnit.Module? = when (tier) {
        PlacementLevel.MODULE -> activeModuleDir?.let { dir ->
            ModuleSourceDiscovery.forModule(dir, dir.name, PlacementLevel.MODULE)
        }
        PlacementLevel.PROJECT -> {
            // Project-tier nodes live in the project's shared `:nodes` module.
            val nodesDir = File(projectRoot, "nodes")
            if (nodesDir.isDirectory) {
                ModuleSourceDiscovery.forModule(nodesDir, "nodes", PlacementLevel.PROJECT)
            } else null
        }
        PlacementLevel.UNIVERSAL -> {
            // Universal tier — synthetic compile unit at ~/.codenode/nodes/.
            val universalDir = universalDirProvider()
            ModuleSourceDiscovery.forUniversal(universalDir)
        }
        PlacementLevel.INTERNAL -> null  // tool-managed; never recompiled by users
    }

    /**
     * Convenience: derives the tier from a source file's absolute path and resolves
     * the appropriate compile unit. Returns null when the path can't be classified.
     *
     * Heuristics:
     *  - Path under `${universalDir}/` → UNIVERSAL.
     *  - Path under `${projectRoot}/nodes/` → PROJECT.
     *  - Path under `${projectRoot}/${someModuleDir}/` → MODULE (with that module dir).
     */
    fun resolveForFile(file: File): CompileUnit.Module? {
        val absolutePath = file.absolutePath

        // UNIVERSAL — under ~/.codenode/nodes/
        val universalRoot = universalDirProvider()
        if (absolutePath.startsWith(universalRoot.absolutePath + File.separator)) {
            return resolve(PlacementLevel.UNIVERSAL, activeModuleDir = null)
        }

        // PROJECT — under ${projectRoot}/nodes/
        val projectNodesRoot = File(projectRoot, "nodes")
        if (absolutePath.startsWith(projectNodesRoot.absolutePath + File.separator)) {
            return resolve(PlacementLevel.PROJECT, activeModuleDir = null)
        }

        // MODULE — under ${projectRoot}/${moduleName}/
        if (absolutePath.startsWith(projectRoot.absolutePath + File.separator)) {
            val rel = absolutePath.removePrefix(projectRoot.absolutePath + File.separator)
            val moduleName = rel.substringBefore(File.separator)
            if (moduleName.isNotEmpty()) {
                val moduleDir = File(projectRoot, moduleName)
                if (moduleDir.isDirectory) {
                    return resolve(PlacementLevel.MODULE, activeModuleDir = moduleDir)
                }
            }
        }

        return null
    }
}
