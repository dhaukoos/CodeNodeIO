/*
 * LevelCompatibilityChecker - Checks child node level compatibility for GraphNode promotion
 * License: Apache 2.0
 */

package io.codenode.grapheditor.state

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.GraphNode
import io.codenode.fbpdsl.model.Node
import io.codenode.grapheditor.model.PlacementLevel
import java.io.File

/**
 * A child node that needs to be promoted to a higher (more general) level.
 *
 * @property nodeName Name of the child CodeNode
 * @property currentLevel The level where the child node currently exists
 * @property sourceFilePath Absolute path to the child node's source .kt file
 * @property promotable Whether this node can be promoted (false if it has unresolvable imports)
 */
data class PromotionCandidate(
    val nodeName: String,
    val currentLevel: PlacementLevel,
    val sourceFilePath: String,
    val promotable: Boolean = true
)

/**
 * Checks whether a GraphNode's child nodes are all available at the target save level.
 * If any child node exists at a more specific level than the target, it returns a list
 * of [PromotionCandidate]s that need to be copied to the target level.
 */
object LevelCompatibilityChecker {

    /**
     * Determines which child nodes need promotion when saving at [targetLevel].
     *
     * Walks all child nodes recursively (including nested GraphNodes), looks up each
     * CodeNode's source file path via the registry, infers its current level from the
     * path, and returns candidates where the current level is more specific than the target.
     *
     * @param graphNode The GraphNode being saved
     * @param targetLevel The level the user wants to save at
     * @param nodeRegistry The node definition registry for source file lookup
     * @return List of child nodes that need promotion (empty if all are compatible)
     */
    fun checkCompatibility(
        graphNode: GraphNode,
        targetLevel: PlacementLevel,
        nodeRegistry: NodeDefinitionRegistry
    ): List<PromotionCandidate> {
        val candidates = mutableListOf<PromotionCandidate>()
        val visited = mutableSetOf<String>()

        collectCandidates(graphNode.childNodes, targetLevel, nodeRegistry, candidates, visited)

        return candidates
    }

    private fun collectCandidates(
        nodes: List<Node>,
        targetLevel: PlacementLevel,
        nodeRegistry: NodeDefinitionRegistry,
        candidates: MutableList<PromotionCandidate>,
        visited: MutableSet<String>
    ) {
        for (node in nodes) {
            if (node.name in visited) continue
            visited.add(node.name)

            when (node) {
                is CodeNode -> {
                    val sourcePath = nodeRegistry.getSourceFilePath(node.name) ?: continue
                    val currentLevel = inferLevelFromPath(sourcePath) ?: continue

                    // If current level is more specific (lower ordinal) than target, no promotion needed.
                    // If current level is MORE SPECIFIC than target, that's fine — it's available.
                    // Promotion is needed when current level is MORE SPECIFIC than target? No —
                    // it's the opposite: a Module-level node is NOT available at Universal level.
                    // MODULE (ordinal 0) < PROJECT (1) < UNIVERSAL (2)
                    // If target is UNIVERSAL and node is MODULE, the node is NOT available at Universal.
                    // So promotion is needed when currentLevel.ordinal < targetLevel.ordinal
                    // (i.e., the node is at a more specific level than the target).
                    if (currentLevel.ordinal < targetLevel.ordinal) {
                        val sourceContent = try { java.io.File(sourcePath).readText() } catch (_: Exception) { "" }
                        val canPromote = !NodePromoter.hasUnresolvableImports(sourceContent, targetLevel)
                        candidates.add(
                            PromotionCandidate(
                                nodeName = node.name,
                                currentLevel = currentLevel,
                                sourceFilePath = sourcePath,
                                promotable = canPromote
                            )
                        )
                    }
                }
                is GraphNode -> {
                    // Recurse into nested GraphNode's children
                    collectCandidates(node.childNodes, targetLevel, nodeRegistry, candidates, visited)
                }
            }
        }
    }

    /**
     * Infers the placement level from a source file's absolute path.
     *
     * - Contains `/.codenode/` → UNIVERSAL
     * - Contains `/nodes/src/commonMain/kotlin/io/codenode/nodes/` → PROJECT
     *   (project-level nodes live under `{projectRoot}/nodes/src/commonMain/kotlin/io/codenode/nodes/`)
     * - Contains `/src/commonMain/kotlin/` with a module-specific package → MODULE
     * - Otherwise → PROJECT
     */
    internal fun inferLevelFromPath(path: String): PlacementLevel? {
        return when {
            path.contains("/.codenode/") -> PlacementLevel.UNIVERSAL
            path.contains("/nodes/src/commonMain/kotlin/io/codenode/nodes/") -> PlacementLevel.PROJECT
            path.contains("/src/commonMain/kotlin/") -> PlacementLevel.MODULE
            else -> PlacementLevel.PROJECT
        }
    }
}
