/*
 * NodePromoter - Copies child node files to target level for GraphNode promotion
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.model.PlacementLevel
import java.io.File

/**
 * Promotes child CodeNode `.kt` files to a target filesystem level by copying
 * and updating the `package` declaration. Also handles transitive IP type promotion.
 */
object NodePromoter {

    /**
     * Promotes a list of candidate CodeNode files to the target level.
     *
     * For each candidate:
     * 1. Reads the source `.kt` file
     * 2. Updates the `package` declaration to match the target level
     * 3. Copies to the target level's `nodes/` directory
     * 4. Scans for module-specific IP type imports and promotes those too
     *
     * @param candidates List of nodes needing promotion
     * @param targetLevel The target level to promote to
     * @param activeModulePath Path to the active module (for MODULE-level resolution)
     * @param projectRoot Project root directory (for PROJECT-level resolution)
     * @return List of file paths that were created (for status reporting)
     */
    fun promoteNodes(
        candidates: List<PromotionCandidate>,
        targetLevel: PlacementLevel,
        activeModulePath: String? = null,
        projectRoot: File? = null
    ): List<String> {
        val createdFiles = mutableListOf<String>()

        for (candidate in candidates) {
            val sourceFile = File(candidate.sourceFilePath)
            if (!sourceFile.exists()) continue

            val content = sourceFile.readText()

            // Skip nodes with unresolvable imports at the target level
            if (hasUnresolvableImports(content, targetLevel)) continue

            val targetDir = resolveNodesDir(targetLevel, activeModulePath, projectRoot) ?: continue
            targetDir.mkdirs()

            val targetFile = File(targetDir, sourceFile.name)

            // Skip if a file with the same name already exists at the target level
            if (targetFile.exists()) continue

            val updatedContent = updatePackageDeclaration(content, targetLevel)

            targetFile.writeText(updatedContent)
            createdFiles.add(targetFile.absolutePath)

            // Promote transitive IP type dependencies
            promoteIPTypeDependencies(content, candidate.currentLevel, targetLevel, activeModulePath, projectRoot, createdFiles)
        }

        return createdFiles
    }

    /**
     * Checks whether a CodeNode source file has imports that cannot be resolved
     * at the target level. Module-specific imports (e.g., `io.codenode.weatherforecast.*`)
     * and third-party library imports not available in the `nodes` module (e.g., Ktor)
     * make a node unpromotable.
     */
    internal fun hasUnresolvableImports(content: String, targetLevel: PlacementLevel): Boolean {
        if (targetLevel == PlacementLevel.MODULE) return false // Module-to-module keeps all imports

        val importPattern = Regex("""^import\s+(.+)$""", RegexOption.MULTILINE)
        val imports = importPattern.findAll(content).map { it.groupValues[1].trim() }

        // Allowed import prefixes at project/universal level
        val allowedPrefixes = listOf(
            "io.codenode.fbpdsl.",       // FBP DSL runtime (always available)
            "io.codenode.nodes.",         // Project-level nodes package
            "io.codenode.iptypes.",       // Project-level IP types package
            "kotlin.",                    // Kotlin stdlib
            "kotlinx.coroutines.",        // Coroutines (always available)
            "java.",                      // JDK
            "javax.",                     // JDK extensions
        )

        for (import in imports) {
            // Module-specific codenode imports (e.g., io.codenode.weatherforecast.*)
            if (import.startsWith("io.codenode.") &&
                !allowedPrefixes.any { import.startsWith(it) }) {
                return true
            }
            // Third-party libraries not available in the nodes module
            if (!import.startsWith("io.codenode.") &&
                !allowedPrefixes.any { import.startsWith(it) }) {
                return true
            }
        }
        return false
    }

    /**
     * Scans source content for module-specific IP type imports and promotes them.
     * Only promotes imports that are at a more specific level than the target.
     */
    private fun promoteIPTypeDependencies(
        sourceContent: String,
        currentLevel: PlacementLevel,
        targetLevel: PlacementLevel,
        activeModulePath: String?,
        projectRoot: File?,
        createdFiles: MutableList<String>
    ) {
        // Find IP type source files referenced by this CodeNode
        // Module-level IP types are in packages like: io.codenode.{modulename}.iptypes
        // We detect these by scanning import statements
        val importPattern = Regex("""^import\s+(io\.codenode\.(\w+)\.iptypes\.(\w+))""", RegexOption.MULTILINE)
        val matches = importPattern.findAll(sourceContent)

        for (match in matches) {
            val moduleName = match.groupValues[2]
            val ipTypeName = match.groupValues[3]

            // Locate the IP type source file
            val ipTypeSourceFile = resolveIPTypeSourceFile(moduleName, ipTypeName, activeModulePath, projectRoot)
                ?: continue

            if (!ipTypeSourceFile.exists()) continue

            val targetIPTypeDir = resolveIPTypesDir(targetLevel, activeModulePath, projectRoot) ?: continue
            targetIPTypeDir.mkdirs()

            val targetIPTypeFile = File(targetIPTypeDir, ipTypeSourceFile.name)
            if (targetIPTypeFile.exists()) continue

            val ipContent = ipTypeSourceFile.readText()
            val updatedIPContent = updatePackageDeclaration(ipContent, targetLevel)
            targetIPTypeFile.writeText(updatedIPContent)
            createdFiles.add(targetIPTypeFile.absolutePath)
        }
    }

    /**
     * Updates the `package` declaration in Kotlin source to match the target level.
     */
    internal fun updatePackageDeclaration(content: String, targetLevel: PlacementLevel): String {
        val packagePattern = Regex("""^(package\s+)io\.codenode\.\w+\.(nodes|iptypes)(.*)$""", RegexOption.MULTILINE)
        return packagePattern.replace(content) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val subpackage = matchResult.groupValues[2]
            val suffix = matchResult.groupValues[3]
            when (targetLevel) {
                PlacementLevel.UNIVERSAL -> "${prefix}io.codenode.$subpackage$suffix"
                PlacementLevel.PROJECT -> "${prefix}io.codenode.$subpackage$suffix"
                PlacementLevel.MODULE -> content // No change needed for module-to-module
            }
        }
    }

    /**
     * Resolves the `nodes/` directory for the target level.
     */
    private fun resolveNodesDir(level: PlacementLevel, activeModulePath: String?, projectRoot: File?): File? {
        return when (level) {
            PlacementLevel.MODULE -> {
                val modulePath = activeModulePath ?: return null
                val moduleDir = File(modulePath)
                val moduleName = moduleDir.name.lowercase()
                moduleDir.resolve("src/commonMain/kotlin/io/codenode/$moduleName/nodes")
            }
            PlacementLevel.PROJECT -> {
                val root = projectRoot ?: return null
                root.resolve("nodes/src/commonMain/kotlin/io/codenode/nodes")
            }
            PlacementLevel.UNIVERSAL -> {
                val home = System.getProperty("user.home")
                File(home, ".codenode/nodes")
            }
        }
    }

    /**
     * Resolves the `iptypes/` directory for the target level.
     */
    private fun resolveIPTypesDir(level: PlacementLevel, activeModulePath: String?, projectRoot: File?): File? {
        return when (level) {
            PlacementLevel.MODULE -> {
                val modulePath = activeModulePath ?: return null
                val moduleDir = File(modulePath)
                val moduleName = moduleDir.name.lowercase()
                moduleDir.resolve("src/commonMain/kotlin/io/codenode/$moduleName/iptypes")
            }
            PlacementLevel.PROJECT -> {
                val root = projectRoot ?: return null
                root.resolve("iptypes/src/commonMain/kotlin/io/codenode/iptypes")
            }
            PlacementLevel.UNIVERSAL -> {
                val home = System.getProperty("user.home")
                File(home, ".codenode/iptypes")
            }
        }
    }

    /**
     * Locates an IP type source file in a module's iptypes directory.
     */
    private fun resolveIPTypeSourceFile(
        moduleName: String,
        ipTypeName: String,
        activeModulePath: String?,
        projectRoot: File?
    ): File? {
        // Try active module first
        if (activeModulePath != null) {
            val moduleDir = File(activeModulePath)
            val ipTypeFile = moduleDir.resolve(
                "src/commonMain/kotlin/io/codenode/${moduleDir.name.lowercase()}/iptypes/${ipTypeName}.kt"
            )
            if (ipTypeFile.exists()) return ipTypeFile
        }

        // Try project root modules
        if (projectRoot != null) {
            val moduleDir = projectRoot.resolve(moduleName.replaceFirstChar { it.uppercase() })
            val ipTypeFile = moduleDir.resolve(
                "src/commonMain/kotlin/io/codenode/$moduleName/iptypes/${ipTypeName}.kt"
            )
            if (ipTypeFile.exists()) return ipTypeFile
        }

        return null
    }
}
