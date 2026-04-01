/*
 * GraphNodeTemplateRegistry - Discovery, caching, save/remove for GraphNode templates
 * Scans three-tier filesystem (Module/Project/Universal) for .flow.kts template files
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.model.GraphNode
import io.codenode.grapheditor.model.GraphNodeTemplateMeta
import io.codenode.grapheditor.model.PlacementLevel
import io.codenode.grapheditor.serialization.GraphNodeTemplateSerializer
import java.io.File

/**
 * Central registry for saved GraphNode templates.
 *
 * Discovers templates from three filesystem tiers on startup:
 * - **Module**: `{module}/src/commonMain/kotlin/io/codenode/{modname}/graphnodes/`
 * - **Project**: `{projectRoot}/graphnodes/`
 * - **Universal**: `~/.codenode/graphnodes/`
 *
 * Templates are deduplicated by name with Module > Project > Universal precedence.
 */
class GraphNodeTemplateRegistry {

    /** All discovered templates, keyed by name (deduplicated by tier precedence) */
    private val templates = mutableMapOf<String, GraphNodeTemplateMeta>()

    /** Project root directory, set during discoverAll() */
    private var projectRoot: File? = null

    /**
     * Scans all three tier directories for `.flow.kts` GraphNode template files.
     * Parses metadata headers and populates the in-memory cache.
     *
     * @param projectRoot Project root directory (null if no project loaded)
     * @param activeModulePaths Paths to currently loaded modules (for Module tier)
     */
    fun discoverAll(projectRoot: File?, activeModulePaths: List<File> = emptyList()) {
        this.projectRoot = projectRoot
        templates.clear()

        val allTemplates = mutableListOf<GraphNodeTemplateMeta>()

        // Scan Module directories (highest precedence)
        for (modulePath in activeModulePaths) {
            val graphNodesDir = findGraphNodesDir(modulePath)
            if (graphNodesDir != null) {
                allTemplates.addAll(scanDirectory(graphNodesDir, PlacementLevel.MODULE))
            }
        }

        // Scan Project directory
        if (projectRoot != null) {
            val projectGraphNodesDir = File(projectRoot, "graphnodes")
            if (projectGraphNodesDir.isDirectory) {
                allTemplates.addAll(scanDirectory(projectGraphNodesDir, PlacementLevel.PROJECT))
            }
        }

        // Scan Universal directory
        val universalDir = File(System.getProperty("user.home"), ".codenode/graphnodes")
        if (universalDir.isDirectory) {
            allTemplates.addAll(scanDirectory(universalDir, PlacementLevel.UNIVERSAL))
        }

        // Deduplicate: keep the most specific tier for each template name
        val deduplicated = allTemplates
            .groupBy { it.name }
            .mapValues { (_, candidates) ->
                candidates.minByOrNull { it.tier.ordinal } ?: candidates.first()
            }

        templates.clear()
        templates.putAll(deduplicated.mapValues { it.value })
    }

    // ==================== Tier Path Resolution ====================

    /**
     * Resolves the output file path for a template at the given tier.
     *
     * @param name Template name (used as filename)
     * @param level Target placement level
     * @param activeModulePath Path to the active module (required for MODULE level)
     * @return The resolved file path, or null if MODULE level with no active module
     */
    fun resolveOutputPath(name: String, level: PlacementLevel, activeModulePath: String? = null): File? {
        val fileName = "${name}.flow.kts"
        return when (level) {
            PlacementLevel.MODULE -> {
                val modulePath = activeModulePath ?: return null
                val moduleDir = File(modulePath)
                val moduleName = moduleDir.name.lowercase()
                moduleDir.resolve("src/commonMain/kotlin/io/codenode/$moduleName/graphnodes/$fileName")
            }
            PlacementLevel.PROJECT -> {
                val root = projectRoot ?: return null
                root.resolve("graphnodes/$fileName")
            }
            PlacementLevel.UNIVERSAL -> {
                val home = System.getProperty("user.home")
                File(home, ".codenode/graphnodes/$fileName")
            }
        }
    }

    /**
     * Resolves the output directory for a tier, creating it if needed.
     *
     * @param level Target placement level
     * @param activeModulePath Path to the active module (required for MODULE level)
     * @return The resolved directory, or null if MODULE level with no active module
     */
    fun resolveOutputDir(level: PlacementLevel, activeModulePath: String? = null): File? {
        val dir = when (level) {
            PlacementLevel.MODULE -> {
                val modulePath = activeModulePath ?: return null
                val moduleDir = File(modulePath)
                val moduleName = moduleDir.name.lowercase()
                moduleDir.resolve("src/commonMain/kotlin/io/codenode/$moduleName/graphnodes")
            }
            PlacementLevel.PROJECT -> {
                val root = projectRoot ?: return null
                root.resolve("graphnodes")
            }
            PlacementLevel.UNIVERSAL -> {
                val home = System.getProperty("user.home")
                File(home, ".codenode/graphnodes")
            }
        }
        dir.mkdirs()
        return dir
    }

    // ==================== Save / Remove ====================

    /**
     * Saves a GraphNode as a template at the specified tier.
     * Serializes to `.flow.kts` with metadata header and registers in the in-memory cache.
     *
     * @param graphNode The GraphNode to save
     * @param level Target placement level
     * @param activeModulePath Path to the active module (required for MODULE level)
     * @return The created metadata entry, or null if the output path could not be resolved
     */
    fun saveGraphNode(
        graphNode: GraphNode,
        level: PlacementLevel,
        activeModulePath: String? = null
    ): GraphNodeTemplateMeta? {
        val outputFile = resolveOutputPath(graphNode.name, level, activeModulePath) ?: return null

        GraphNodeTemplateSerializer.saveTemplate(graphNode, outputFile)

        val meta = GraphNodeTemplateMeta(
            name = graphNode.name,
            description = graphNode.description,
            inputPortCount = graphNode.inputPorts.size,
            outputPortCount = graphNode.outputPorts.size,
            childNodeCount = graphNode.childNodes.size,
            filePath = outputFile.absolutePath,
            tier = level
        )

        templates[meta.name] = meta
        return meta
    }

    /**
     * Removes a saved template from disk and the in-memory cache.
     *
     * @param name Template name to remove
     * @param level Tier to remove from (used if multiple tiers have same name)
     * @return true if the template was found and removed
     */
    fun removeTemplate(name: String, level: PlacementLevel): Boolean {
        val meta = templates[name] ?: return false
        if (meta.tier != level) return false

        val file = File(meta.filePath)
        if (file.exists()) {
            file.delete()
        }

        templates.remove(name)
        return true
    }

    // ==================== Query Methods ====================

    /** Returns all discovered/saved templates (deduplicated). */
    fun getAll(): List<GraphNodeTemplateMeta> = templates.values.toList()

    /** Looks up a template by name. */
    fun getByName(name: String): GraphNodeTemplateMeta? = templates[name]

    /** Checks whether a template with the given name exists. */
    fun nameExists(name: String): Boolean = templates.containsKey(name)

    // ==================== Internal Helpers ====================

    /**
     * Scans a directory for `.flow.kts` files and parses their metadata headers.
     */
    private fun scanDirectory(dir: File, tier: PlacementLevel): List<GraphNodeTemplateMeta> {
        if (!dir.isDirectory) return emptyList()

        return dir.listFiles { file -> file.extension == "kts" && file.name.endsWith(".flow.kts") }
            ?.mapNotNull { file ->
                try {
                    GraphNodeTemplateSerializer.parseMetadata(file)?.copy(tier = tier)
                } catch (e: Exception) {
                    println("Warning: Failed to parse GraphNode template ${file.name}: ${e.message}")
                    null
                }
            }
            ?: emptyList()
    }

    /**
     * Finds the `graphnodes/` directory within a module's source tree.
     */
    private fun findGraphNodesDir(modulePath: File): File? {
        val moduleName = modulePath.name.lowercase()
        val graphNodesDir = File(modulePath, "src/commonMain/kotlin/io/codenode/$moduleName/graphnodes")
        return if (graphNodesDir.isDirectory) graphNodesDir else null
    }
}
