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
        "RuntimeFlowGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.RuntimeFlowGenerator()
                .generate(config.flowGraph, config.flowPackage, config.viewModelPackage)
        },
        "RuntimeControllerGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.RuntimeControllerGenerator()
                .generate(config.flowGraph, config.controllerPackage, config.viewModelPackage)
        },
        "RuntimeControllerInterfaceGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.RuntimeControllerInterfaceGenerator()
                .generate(config.flowGraph, config.controllerPackage)
        },
        "RuntimeControllerAdapterGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.RuntimeControllerAdapterGenerator()
                .generate(config.flowGraph, config.controllerPackage)
        },
        "RuntimeViewModelGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.RuntimeViewModelGenerator()
                .generate(config.flowGraph, config.viewModelPackage, config.controllerPackage)
        },
        "UserInterfaceStubGenerator" to { config: GenerationConfig ->
            io.codenode.flowgraphgenerate.generator.UserInterfaceStubGenerator()
                .generate(config.flowGraph, config.userInterfacePackage, config.viewModelPackage)
        }
    )

    private val generatorsByPath = mapOf(
        GenerationPath.GENERATE_MODULE to listOf(
            "FlowKtGenerator", "RuntimeFlowGenerator",
            "RuntimeControllerGenerator", "RuntimeControllerInterfaceGenerator",
            "RuntimeControllerAdapterGenerator", "RuntimeViewModelGenerator",
            "UserInterfaceStubGenerator"
        ),
        GenerationPath.REPOSITORY to listOf(
            "FlowKtGenerator", "RuntimeFlowGenerator",
            "RuntimeControllerGenerator", "RuntimeControllerInterfaceGenerator",
            "RuntimeControllerAdapterGenerator", "RuntimeViewModelGenerator",
            "UserInterfaceStubGenerator",
            "EntityCUDGenerator", "EntityRepositoryGenerator",
            "EntityDisplayGenerator", "EntityPersistenceGenerator"
        ),
        GenerationPath.UI_FBP to listOf(
            "FlowKtGenerator", "RuntimeFlowGenerator",
            "RuntimeControllerGenerator", "RuntimeControllerInterfaceGenerator",
            "RuntimeControllerAdapterGenerator",
            "UIFBPStateGenerator", "UIFBPViewModelGenerator",
            "UIFBPSourceGenerator", "UIFBPSinkGenerator"
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
