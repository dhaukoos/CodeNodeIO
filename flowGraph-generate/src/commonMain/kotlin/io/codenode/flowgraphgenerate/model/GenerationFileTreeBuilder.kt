/*
 * GenerationFileTreeBuilder - Builds file trees for each generation path
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.model

object GenerationFileTreeBuilder {

    fun buildForGenerateModule(moduleName: String, hasPersistence: Boolean = false): GenerationFileTree {
        val folders = mutableListOf(
            FolderNode(
                name = "scaffolding",
                files = listOf(
                    FileNode("build.gradle.kts", generatorId = "ModuleGenerator"),
                    FileNode("settings.gradle.kts", generatorId = "ModuleGenerator")
                ),
                selectionState = TriState.ALL
            ),
            FolderNode(
                name = "flow",
                files = listOf(
                    FileNode("$moduleName.flow.kt", generatorId = "FlowKtGenerator")
                ),
                selectionState = TriState.ALL
            ),
            FolderNode(
                name = "controller",
                files = listOf(
                    FileNode("${moduleName}ControllerInterface.kt", generatorId = "RuntimeControllerInterfaceGenerator"),
                    FileNode("${moduleName}Runtime.kt", generatorId = "ModuleRuntimeGenerator")
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
                    name = "scaffolding",
                    files = listOf(
                        FileNode("build.gradle.kts", generatorId = "ModuleGenerator"),
                        FileNode("settings.gradle.kts", generatorId = "ModuleGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "flow",
                    files = listOf(
                        FileNode("$pluralName.flow.kt", generatorId = "FlowKtGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "controller",
                    files = listOf(
                        FileNode("${pluralName}ControllerInterface.kt", generatorId = "RuntimeControllerInterfaceGenerator"),
                        FileNode("${pluralName}Runtime.kt", generatorId = "ModuleRuntimeGenerator")
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
                        FileNode("${pluralName}CUDCodeNode.kt", generatorId = "EntityCUDGenerator"),
                        FileNode("${pluralName}RepositoryCodeNode.kt", generatorId = "EntityRepositoryGenerator"),
                        FileNode("${pluralName}DisplayCodeNode.kt", generatorId = "EntityDisplayGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "userInterface",
                    files = listOf(
                        FileNode("$pluralName.kt", generatorId = "UserInterfaceStubGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "persistence",
                    files = listOf(
                        FileNode("${pluralName}Persistence.kt", generatorId = "EntityPersistenceGenerator")
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
                    name = "scaffolding",
                    files = listOf(
                        FileNode("build.gradle.kts", generatorId = "ModuleGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "flow",
                    files = listOf(
                        FileNode("$moduleName.flow.kt", generatorId = "FlowKtGenerator")
                    ),
                    selectionState = TriState.ALL
                ),
                FolderNode(
                    name = "controller",
                    files = listOf(
                        FileNode("${moduleName}ControllerInterface.kt", generatorId = "RuntimeControllerInterfaceGenerator"),
                        FileNode("${moduleName}Runtime.kt", generatorId = "ModuleRuntimeGenerator")
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
                        FileNode("${moduleName}SourceCodeNode.kt", generatorId = "UIFBPSourceGenerator"),
                        FileNode("${moduleName}SinkCodeNode.kt", generatorId = "UIFBPSinkGenerator")
                    ),
                    selectionState = TriState.ALL
                )
            )
        )
    }
}
