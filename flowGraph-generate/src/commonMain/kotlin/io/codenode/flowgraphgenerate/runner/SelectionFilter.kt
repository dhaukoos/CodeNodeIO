/*
 * SelectionFilter - Maps file-tree checkbox selections to generator inclusion/exclusion
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.runner

import io.codenode.flowgraphgenerate.model.GenerationFileTree

data class SelectionFilter(
    val excludedGeneratorIds: Set<String> = emptySet()
) {
    fun isIncluded(generatorId: String): Boolean = generatorId !in excludedGeneratorIds

    companion object {
        fun fromFileTree(fileTree: GenerationFileTree): SelectionFilter {
            val excluded = fileTree.folders.flatMap { folder ->
                folder.files.filter { !it.isSelected }.map { it.generatorId }
            }.filter { it.isNotEmpty() }.toSet()
            return SelectionFilter(excludedGeneratorIds = excluded)
        }
    }
}
