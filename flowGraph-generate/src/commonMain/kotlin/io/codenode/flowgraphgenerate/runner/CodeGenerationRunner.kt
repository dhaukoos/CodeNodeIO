/*
 * CodeGenerationRunner - Executes generation flow graphs via FBP runtime
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.runner

import io.codenode.flowgraphgenerate.model.GenerationPath
import io.codenode.flowgraphgenerate.nodes.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CodeGenerationRunner {

    private val generatorRegistry = mapOf(
        "FlowKtGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.FlowKtGenerator()
                .generateFlowKt(config.flowGraph, config.flowPackage)
        },
        // Feature 085 (universal-runtime collapse): replaces RuntimeFlowGenerator +
        // RuntimeControllerGenerator + RuntimeControllerAdapterGenerator. Emits a
        // single ~30-line `{Module}Runtime.kt` that wires DynamicPipelineController
        // to a module-local NodeRegistry and exposes the typed ControllerInterface.
        "ModuleRuntimeGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.ModuleRuntimeGenerator()
                .generate(
                    config.flowGraph,
                    config.basePackage,
                    config.controllerPackage,
                    config.viewModelPackage
                )
        },
        "RuntimeControllerInterfaceGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.RuntimeControllerInterfaceGenerator()
                .generate(config.flowGraph, config.controllerPackage)
        },
        "RuntimeViewModelGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.RuntimeViewModelGenerator()
                .generate(config.flowGraph, config.viewModelPackage, config.controllerPackage)
        },
        "UserInterfaceStubGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.UserInterfaceStubGenerator()
                .generate(config.flowGraph, config.userInterfacePackage, config.viewModelPackage)
        },
        "PreviewProviderGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.PreviewProviderGenerator()
                .generate(config.flowGraph, config.basePackage, config.viewModelPackage)
        }
    )

    private val generatorsByPath = mapOf(
        GenerationPath.GENERATE_MODULE to listOf(
            "FlowKtGenerator",
            "ModuleRuntimeGenerator",
            "RuntimeControllerInterfaceGenerator",
            "RuntimeViewModelGenerator",
            "UserInterfaceStubGenerator",
            "PreviewProviderGenerator"
        ),
        GenerationPath.REPOSITORY to listOf(
            "FlowKtGenerator",
            "ModuleRuntimeGenerator",
            "RuntimeControllerInterfaceGenerator",
            "RuntimeViewModelGenerator",
            "UserInterfaceStubGenerator",
            "PreviewProviderGenerator",
            "EntityCUDGenerator", "EntityRepositoryGenerator",
            "EntityDisplayGenerator", "EntityPersistenceGenerator"
        ),
        GenerationPath.UI_FBP to listOf(
            "FlowKtGenerator",
            "ModuleRuntimeGenerator",
            "RuntimeControllerInterfaceGenerator",
            "UIFBPStateGenerator", "UIFBPViewModelGenerator",
            "UIFBPSourceGenerator", "UIFBPSinkGenerator",
            "PreviewProviderGenerator"
        )
    )

    suspend fun execute(
        generationPath: GenerationPath,
        config: GenerationConfig,
        selectionFilter: SelectionFilter = SelectionFilter()
    ): GenerationResult {
        val allGenerators = generatorsByPath[generationPath] ?: emptyList()
        val included = allGenerators.filter { selectionFilter.isIncluded(it) }
        val skipped = allGenerators.filter { !selectionFilter.isIncluded(it) }.toSet()

        val generatedFiles = mutableMapOf<String, String>()
        val errors = mutableMapOf<String, String>()

        coroutineScope {
            val results = included.map { generatorId ->
                async {
                    try {
                        val generator = generatorRegistry[generatorId]
                        if (generator != null) {
                            val content = generator(config)
                            generatorId to Result.success(content)
                        } else {
                            generatorId to Result.failure<String>(
                                IllegalStateException("Unknown generator: $generatorId")
                            )
                        }
                    } catch (e: Exception) {
                        generatorId to Result.failure<String>(e)
                    }
                }
            }.awaitAll()

            for ((generatorId, result) in results) {
                result.fold(
                    onSuccess = { content -> generatedFiles[generatorId] = content },
                    onFailure = { error -> errors[generatorId] = error.message ?: "Unknown error" }
                )
            }
        }

        return GenerationResult(
            generatedFiles = generatedFiles,
            errors = errors,
            skipped = skipped
        )
    }
}
