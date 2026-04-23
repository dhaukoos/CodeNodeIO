/*
 * GenerationFileWriter - Writes GenerationResult content to disk
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.runner

import java.io.File

class GenerationFileWriter {

    private val generatorToPath = mapOf(
        "FlowKtGenerator" to { moduleName: String -> "flow/$moduleName.flow.kt" },
        "RuntimeFlowGenerator" to { moduleName: String -> "flow/${moduleName}Flow.kt" },
        "RuntimeControllerGenerator" to { moduleName: String -> "controller/${moduleName}Controller.kt" },
        "RuntimeControllerInterfaceGenerator" to { moduleName: String -> "controller/${moduleName}ControllerInterface.kt" },
        "RuntimeControllerAdapterGenerator" to { moduleName: String -> "controller/${moduleName}ControllerAdapter.kt" },
        "RuntimeViewModelGenerator" to { moduleName: String -> "viewmodel/${moduleName}ViewModel.kt" },
        "UserInterfaceStubGenerator" to { moduleName: String -> "userInterface/$moduleName.kt" },
        "EntityCUDGenerator" to { moduleName: String -> "nodes/${moduleName}CUDCodeNode.kt" },
        "EntityRepositoryGenerator" to { moduleName: String -> "nodes/${moduleName}RepositoryCodeNode.kt" },
        "EntityDisplayGenerator" to { moduleName: String -> "nodes/${moduleName}DisplayCodeNode.kt" },
        "EntityPersistenceGenerator" to { moduleName: String -> "nodes/${moduleName}Persistence.kt" },
        "UIFBPStateGenerator" to { moduleName: String -> "viewmodel/${moduleName}State.kt" },
        "UIFBPViewModelGenerator" to { moduleName: String -> "viewmodel/${moduleName}ViewModel.kt" },
        "UIFBPSourceGenerator" to { moduleName: String -> "nodes/${moduleName}SourceCodeNode.kt" },
        "UIFBPSinkGenerator" to { moduleName: String -> "nodes/${moduleName}SinkCodeNode.kt" }
    )

    fun write(
        result: GenerationResult,
        moduleDir: File,
        basePackage: String,
        moduleName: String
    ): List<String> {
        val basePackagePath = basePackage.replace(".", "/")
        val baseSrcDir = File(moduleDir, "src/commonMain/kotlin/$basePackagePath")
        val filesWritten = mutableListOf<String>()

        for ((generatorId, content) in result.generatedFiles) {
            val pathMapper = generatorToPath[generatorId]
            if (pathMapper != null) {
                val relativePath = pathMapper(moduleName)
                val targetFile = File(baseSrcDir, relativePath)
                targetFile.parentFile?.mkdirs()
                targetFile.writeText(content)
                filesWritten.add(relativePath)
            }
        }

        return filesWritten
    }
}
