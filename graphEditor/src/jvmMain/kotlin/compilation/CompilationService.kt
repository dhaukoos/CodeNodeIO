/*
 * Compilation Service
 * Integrates KMP module generation with the graphEditor
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compilation

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.kotlincompiler.generator.ModuleGenerator
import io.codenode.kotlincompiler.generator.GeneratedModule
import java.io.File

/**
 * Service for compiling FlowGraphs to KMP modules.
 *
 * Provides an integration point between the graphEditor UI and the
 * kotlinCompiler module generation functionality.
 *
 * @sample
 * ```kotlin
 * val service = CompilationService()
 * val result = service.compileToModule(
 *     flowGraph = graphState.flowGraph,
 *     outputDir = File("./output"),
 *     moduleName = "my-module"
 * )
 * if (result.success) {
 *     println("Compiled to: ${result.outputPath}")
 * } else {
 *     println("Error: ${result.errorMessage}")
 * }
 * ```
 */
class CompilationService {

    private val moduleGenerator = ModuleGenerator()

    /**
     * Compiles a FlowGraph to a KMP module.
     *
     * @param flowGraph The flow graph to compile
     * @param outputDir The output directory for the generated module
     * @param moduleName Optional module name (defaults to flow graph name)
     * @param packageName Optional base package name
     * @return CompilationResult with success status and details
     */
    fun compileToModule(
        flowGraph: FlowGraph,
        outputDir: File,
        moduleName: String = flowGraph.name.lowercase().replace(" ", "-"),
        packageName: String = ModuleGenerator.DEFAULT_PACKAGE
    ): CompilationResult {
        return try {
            // Validate flow graph
            val validation = flowGraph.validate()
            if (!validation.success) {
                return CompilationResult(
                    success = false,
                    errorMessage = "Flow graph validation failed: ${validation.errors.joinToString(", ")}"
                )
            }

            // Generate module
            val generatedModule = moduleGenerator.generateModule(flowGraph, moduleName, packageName)

            // Create output directory for this module
            val moduleDir = File(outputDir, moduleName)
            if (!moduleDir.exists()) {
                moduleDir.mkdirs()
            }

            // Write generated files
            generatedModule.writeTo(moduleDir)

            CompilationResult(
                success = true,
                outputPath = moduleDir.absolutePath,
                generatedModule = generatedModule,
                fileCount = generatedModule.fileCount()
            )
        } catch (e: Exception) {
            CompilationResult(
                success = false,
                errorMessage = "Compilation failed: ${e.message}"
            )
        }
    }

    /**
     * Generates module content without writing to disk.
     *
     * Useful for preview or validation purposes.
     *
     * @param flowGraph The flow graph to compile
     * @param moduleName The module name
     * @param packageName The base package name
     * @return GeneratedModule with all content
     */
    fun generateModulePreview(
        flowGraph: FlowGraph,
        moduleName: String,
        packageName: String = ModuleGenerator.DEFAULT_PACKAGE
    ): GeneratedModule {
        return moduleGenerator.generateModule(flowGraph, moduleName, packageName)
    }

    /**
     * Generates only the build.gradle.kts content for preview.
     *
     * @param flowGraph The flow graph
     * @param moduleName The module name
     * @return Build script content
     */
    fun generateBuildScriptPreview(flowGraph: FlowGraph, moduleName: String): String {
        return moduleGenerator.generateBuildGradle(flowGraph, moduleName)
    }

    /**
     * Generates only the FlowGraph class content for preview.
     *
     * @param flowGraph The flow graph
     * @param packageName The package name
     * @return Generated class content
     */
    fun generateFlowClassPreview(flowGraph: FlowGraph, packageName: String): String {
        return moduleGenerator.generateFlowGraphClass(flowGraph, packageName)
    }

    /**
     * Generates only the Controller class content for preview.
     *
     * @param flowGraph The flow graph
     * @param packageName The package name
     * @return Generated class content
     */
    fun generateControllerClassPreview(flowGraph: FlowGraph, packageName: String): String {
        return moduleGenerator.generateControllerClass(flowGraph, packageName)
    }
}

/**
 * Result of a compilation operation.
 *
 * @property success Whether compilation succeeded
 * @property outputPath Path to the generated module (if successful)
 * @property generatedModule The generated module object (if successful)
 * @property fileCount Number of files generated (if successful)
 * @property errorMessage Error message (if failed)
 */
data class CompilationResult(
    val success: Boolean,
    val outputPath: String? = null,
    val generatedModule: GeneratedModule? = null,
    val fileCount: Int = 0,
    val errorMessage: String? = null
)
