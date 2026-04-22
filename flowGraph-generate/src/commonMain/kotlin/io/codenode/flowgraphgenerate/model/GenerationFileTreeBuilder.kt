/*
 * GenerationFileTreeBuilder - Builds file trees for each generation path
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.model

object GenerationFileTreeBuilder {

    fun buildForGenerateModule(moduleName: String, hasPersistence: Boolean = false): GenerationFileTree {
        val folders = mutableListOf(
            FolderNode(
                name = "flow",
                files = listOf(
                    FileNode("$moduleName.flow.kt", generatorId = "FlowKtGenerator"),
                    FileNode("${moduleName}Flow.kt", generatorId = "RuntimeFlowGenerator")
                ),
                selectionState = TriState.ALL
            ),
            FolderNode(
                name = "controller",
                files = listOf(
                    FileNode("${moduleName}Controller.kt", generatorId = "RuntimeControllerGenerator"),
                    FileNode("${moduleName}ControllerInterface.kt", generatorId = "RuntimeControllerInterfaceGenerator"),
                    FileNode("${moduleName}ControllerAdapter.kt", generatorId = "RuntimeControllerAdapterGenerator")
                ),
                selectionState = TriState.ALL
            ),
            FolderNode(
                name = "viewmodel",
                files = listOf(
                    FileNode("${moduleName}ViewModel.kt", generatorId = "RuntimeViewModelGenerator")
                ),
                selectionState = TriState.ALL
            ),
            FolderNode(
                name = "userInterface",
                files = listOf(
                    FileNode("$moduleName.kt", generatorId = "UserInterfaceStubGenerator")
                ),
                selectionState = TriState.ALL
            )
        )

        if (hasPersistence) {
            folders.add(FolderNode(
                name = "persistence",
                files = listOf(
                    FileNode("Entity.kt", generatorId = "RepositoryCodeGenerator"),
                    FileNode("Dao.kt", generatorId = "RepositoryCodeGenerator"),
                    FileNode("Repository.kt", generatorId = "RepositoryCodeGenerator")
                ),
                selectionState = TriState.ALL
            ))
        }

        return GenerationFileTree(folders = folders)
    }

    fun buildForRepository(entityName: String, pluralName: String): GenerationFileTree {
        return GenerationFileTree(
            folders = listOf(
                FolderNode(
                    name = "flow",
                    files = listOf(
                        FileNode("$pluralName.flow.kt", generatorId = "FlowKtGenerator"),
                        FileNode("${pluralName}Flow.kt", generatorId = "RuntimeFlowGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "controller",
                    files = listOf(
                        FileNode("${pluralName}Controller.kt", generatorId = "RuntimeControllerGenerator"),
                        FileNode("${pluralName}ControllerInterface.kt", generatorId = "RuntimeControllerInterfaceGenerator"),
                        FileNode("${pluralName}ControllerAdapter.kt", generatorId = "RuntimeControllerAdapterGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "viewmodel",
                    files = listOf(
                        FileNode("${pluralName}ViewModel.kt", generatorId = "RuntimeViewModelGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "nodes",
                    files = listOf(
                        FileNode("${entityName}CUDCodeNode.kt", generatorId = "EntityCUDCodeNodeGenerator"),
                        FileNode("${entityName}RepositoryCodeNode.kt", generatorId = "EntityRepositoryCodeNodeGenerator"),
                        FileNode("${pluralName}DisplayCodeNode.kt", generatorId = "EntityDisplayCodeNodeGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "userInterface",
                    files = listOf(
                        FileNode("$pluralName.kt", generatorId = "EntityUIGenerator"),
                        FileNode("AddUpdate$entityName.kt", generatorId = "EntityUIGenerator"),
                        FileNode("${entityName}Row.kt", generatorId = "EntityUIGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "persistence",
                    files = listOf(
                        FileNode("${pluralName}Persistence.kt", generatorId = "EntityPersistenceGenerator"),
                        FileNode("${entityName}Converters.kt", generatorId = "EntityConverterGenerator")
                    ),
                    selectionState = TriState.ALL
                )
            )
        )
    }

    fun buildForUIFBP(moduleName: String): GenerationFileTree {
        return GenerationFileTree(
            folders = listOf(
                FolderNode(
                    name = "flow",
                    files = listOf(
                        FileNode("$moduleName.flow.kt", generatorId = "FlowKtGenerator"),
                        FileNode("${moduleName}Flow.kt", generatorId = "RuntimeFlowGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "controller",
                    files = listOf(
                        FileNode("${moduleName}Controller.kt", generatorId = "RuntimeControllerGenerator"),
                        FileNode("${moduleName}ControllerInterface.kt", generatorId = "RuntimeControllerInterfaceGenerator"),
                        FileNode("${moduleName}ControllerAdapter.kt", generatorId = "RuntimeControllerAdapterGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "viewmodel",
                    files = listOf(
                        FileNode("${moduleName}ViewModel.kt", generatorId = "UIFBPViewModelGenerator"),
                        FileNode("${moduleName}State.kt", generatorId = "UIFBPStateGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "nodes",
                    files = listOf(
                        FileNode("${moduleName}SourceCodeNode.kt", generatorId = "UIFBPSourceCodeNodeGenerator"),
                        FileNode("${moduleName}SinkCodeNode.kt", generatorId = "UIFBPSinkCodeNodeGenerator")
                    ),
                    selectionState = TriState.ALL
                )
            )
        )
    }
}
