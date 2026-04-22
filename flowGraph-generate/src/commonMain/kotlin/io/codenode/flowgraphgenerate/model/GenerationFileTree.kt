/*
 * GenerationFileTree - Hierarchical model for configurable code generation file selection
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.model

enum class GenerationPath {
    GENERATE_MODULE,
    REPOSITORY,
    UI_FBP
}

enum class TriState {
    ALL, NONE, PARTIAL
}

data class FileNode(
    val name: String,
    val isSelected: Boolean = true,
    val generatorId: String = ""
)

data class FolderNode(
    val name: String,
    val files: List<FileNode>,
    val selectionState: TriState = TriState.ALL
) {
    companion object {
        fun computeSelectionState(files: List<FileNode>): TriState {
            if (files.isEmpty()) return TriState.NONE
            val selectedCount = files.count { it.isSelected }
            return when (selectedCount) {
                0 -> TriState.NONE
                files.size -> TriState.ALL
                else -> TriState.PARTIAL
            }
        }
    }
}

data class GenerationFileTree(
    val folders: List<FolderNode> = emptyList()
) {
    companion object {
        fun empty(): GenerationFileTree = GenerationFileTree()
    }

    fun toggleFolder(folderName: String): GenerationFileTree {
        return copy(folders = folders.map { folder ->
            if (folder.name == folderName) {
                val newSelected = folder.selectionState != TriState.ALL
                val updatedFiles = folder.files.map { it.copy(isSelected = newSelected) }
                folder.copy(
                    files = updatedFiles,
                    selectionState = if (newSelected) TriState.ALL else TriState.NONE
                )
            } else folder
        })
    }

    fun toggleFile(folderName: String, fileName: String): GenerationFileTree {
        return copy(folders = folders.map { folder ->
            if (folder.name == folderName) {
                val updatedFiles = folder.files.map { file ->
                    if (file.name == fileName) file.copy(isSelected = !file.isSelected) else file
                }
                folder.copy(
                    files = updatedFiles,
                    selectionState = FolderNode.computeSelectionState(updatedFiles)
                )
            } else folder
        })
    }

    val totalFiles: Int get() = folders.sumOf { it.files.size }
    val selectedFiles: Int get() = folders.sumOf { f -> f.files.count { it.isSelected } }
}
