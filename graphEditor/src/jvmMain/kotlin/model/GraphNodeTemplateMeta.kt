/*
 * GraphNodeTemplateMeta - Lightweight metadata for saved GraphNode templates
 * Used for palette display without full deserialization
 * License: Apache 2.0
 */

package io.codenode.grapheditor.model

import io.codenode.fbpdsl.model.PlacementLevel

/**
 * Lightweight metadata extracted from a saved GraphNode `.flow.kts` template file.
 * Parsed from the metadata comment header during filesystem discovery.
 * Full deserialization is deferred to instantiation time.
 *
 * @property name Display name of the GraphNode template
 * @property description Optional description extracted from metadata header
 * @property inputPortCount Number of exposed input ports
 * @property outputPortCount Number of exposed output ports
 * @property childNodeCount Number of child nodes in the composition
 * @property filePath Absolute path to the `.flow.kts` template file
 * @property tier Which tier the file was discovered at (MODULE/PROJECT/UNIVERSAL)
 */
data class GraphNodeTemplateMeta(
    val name: String,
    val description: String? = null,
    val inputPortCount: Int,
    val outputPortCount: Int,
    val childNodeCount: Int,
    val filePath: String,
    val tier: PlacementLevel
)
