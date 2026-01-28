/*
 * Code Generation Service
 * Project-level service for generating code from flow graphs
 * License: Apache 2.0
 */

package io.codenode.ideplugin.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.kotlincompiler.generator.BuildScriptGenerator
import io.codenode.kotlincompiler.generator.GeneratedProject
import io.codenode.kotlincompiler.generator.KotlinCodeGenerator
import io.codenode.kotlincompiler.validator.LicenseValidator
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Project-level service that manages code generation from flow graphs.
 *
 * Provides a unified interface for:
 * - Generating Kotlin Multiplatform code from flow graphs
 * - Generating build scripts (Gradle)
 * - Validating licenses for generated dependencies
 * - Progress tracking and reporting
 * - Caching generation results
 *
 * This service coordinates the KotlinCodeGenerator, BuildScriptGenerator,
 * and LicenseValidator to produce complete, deployable KMP projects.
 */
@Service(Service.Level.PROJECT)
class CodeGenerationService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(CodeGenerationService::class.java)

    private val kotlinCodeGenerator = KotlinCodeGenerator()
    private val buildScriptGenerator = BuildScriptGenerator()
    private val licenseValidator = LicenseValidator()

    /**
     * Cache of recent generation results for quick access.
     */
    private val generationCache = ConcurrentHashMap<String, GenerationResult>()

    /**
     * Listeners for generation events.
     */
    private val listeners = mutableListOf<CodeGenerationListener>()

    /**
     * Generates KMP code from a flow graph synchronously.
     *
     * Use this method when you need the result immediately and are already
     * in a background thread. For UI thread operations, use [generateKMPCodeAsync].
     *
     * @param flowGraph The flow graph to generate code from
     * @param config Target platform configuration
     * @param outputDir Directory to write generated files
     * @return GenerationResult with success/failure status and details
     */
    fun generateKMPCode(
        flowGraph: FlowGraph,
        config: BuildScriptGenerator.TargetConfig,
        outputDir: File
    ): GenerationResult {
        logger.info("Starting KMP code generation for: ${flowGraph.name}")

        try {
            // Step 1: Validate the flow graph
            val validation = flowGraph.validate()
            if (!validation.success) {
                val error = "Flow graph validation failed:\n${validation.errors.joinToString("\n")}"
                logger.warn(error)
                return GenerationResult.failure(error)
            }

            // Step 2: Generate component classes
            val generatedProject = kotlinCodeGenerator.generateProject(flowGraph)
            logger.info("Generated ${generatedProject.fileCount()} source files")

            // Step 3: Generate build scripts
            val buildScript = buildScriptGenerator.generateBuildScript(flowGraph, config)
            val settingsScript = buildScriptGenerator.generateSettingsScript(flowGraph.name)
            val gradleProperties = buildScriptGenerator.generateGradleProperties()

            // Step 4: Validate licenses
            val licenseResult = licenseValidator.validateBuildScript(buildScript)
            if (!licenseResult.isValid) {
                val report = licenseValidator.generateReport(licenseResult)
                val error = "License validation failed:\n\n$report"
                logger.warn(error)
                return GenerationResult.failure(error, GenerationErrorType.LICENSE_VIOLATION)
            }

            // Step 5: Write files to output directory
            val projectDir = File(outputDir, flowGraph.name)
            writeGeneratedFiles(projectDir, generatedProject, buildScript, settingsScript, gradleProperties)

            // Step 6: Create result
            val result = GenerationResult.success(
                projectName = flowGraph.name,
                outputPath = projectDir.absolutePath,
                fileCount = generatedProject.fileCount() + 3, // +3 for build files
                targetSummary = getTargetSummary(config),
                generatedFiles = generatedProject.fileNames() + listOf(
                    "build.gradle.kts",
                    "settings.gradle.kts",
                    "gradle.properties"
                )
            )

            // Cache the result
            generationCache[flowGraph.name] = result

            // Notify listeners
            notifyGenerationComplete(result)

            logger.info("KMP code generation completed successfully: ${projectDir.absolutePath}")
            return result

        } catch (ex: Exception) {
            logger.error("KMP code generation failed", ex)
            val result = GenerationResult.failure("Code generation failed: ${ex.message}")
            notifyGenerationFailed(result)
            return result
        }
    }

    /**
     * Generates KMP code from a flow graph asynchronously with progress tracking.
     *
     * This method runs the generation in a background task with progress indicator.
     * Use this when calling from the UI thread.
     *
     * @param flowGraph The flow graph to generate code from
     * @param config Target platform configuration
     * @param outputDir Directory to write generated files
     * @param onComplete Callback invoked when generation completes
     */
    fun generateKMPCodeAsync(
        flowGraph: FlowGraph,
        config: BuildScriptGenerator.TargetConfig,
        outputDir: File,
        onComplete: (GenerationResult) -> Unit
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "Generating KMP Code for ${flowGraph.name}",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false

                try {
                    indicator.text = "Validating flow graph..."
                    indicator.fraction = 0.1

                    // Validate
                    val validation = flowGraph.validate()
                    if (!validation.success) {
                        val error = "Flow graph validation failed:\n${validation.errors.joinToString("\n")}"
                        invokeOnComplete(GenerationResult.failure(error), onComplete)
                        return
                    }

                    indicator.text = "Generating component classes..."
                    indicator.fraction = 0.3

                    // Generate code
                    val generatedProject = kotlinCodeGenerator.generateProject(flowGraph)

                    indicator.text = "Generating build scripts..."
                    indicator.fraction = 0.5

                    // Generate build files
                    val buildScript = buildScriptGenerator.generateBuildScript(flowGraph, config)
                    val settingsScript = buildScriptGenerator.generateSettingsScript(flowGraph.name)
                    val gradleProperties = buildScriptGenerator.generateGradleProperties()

                    indicator.text = "Validating licenses..."
                    indicator.fraction = 0.7

                    // Validate licenses
                    val licenseResult = licenseValidator.validateBuildScript(buildScript)
                    if (!licenseResult.isValid) {
                        val report = licenseValidator.generateReport(licenseResult)
                        val error = "License validation failed:\n\n$report"
                        invokeOnComplete(GenerationResult.failure(error, GenerationErrorType.LICENSE_VIOLATION), onComplete)
                        return
                    }

                    indicator.text = "Writing files..."
                    indicator.fraction = 0.9

                    // Write files
                    val projectDir = File(outputDir, flowGraph.name)
                    writeGeneratedFiles(projectDir, generatedProject, buildScript, settingsScript, gradleProperties)

                    // Refresh VFS
                    VirtualFileManager.getInstance().refreshAndFindFileByUrl("file://${projectDir.absolutePath}")

                    indicator.fraction = 1.0

                    val result = GenerationResult.success(
                        projectName = flowGraph.name,
                        outputPath = projectDir.absolutePath,
                        fileCount = generatedProject.fileCount() + 3,
                        targetSummary = getTargetSummary(config),
                        generatedFiles = generatedProject.fileNames() + listOf(
                            "build.gradle.kts",
                            "settings.gradle.kts",
                            "gradle.properties"
                        )
                    )

                    generationCache[flowGraph.name] = result
                    notifyGenerationComplete(result)
                    invokeOnComplete(result, onComplete)

                } catch (ex: Exception) {
                    logger.error("Async KMP code generation failed", ex)
                    val result = GenerationResult.failure("Code generation failed: ${ex.message}")
                    notifyGenerationFailed(result)
                    invokeOnComplete(result, onComplete)
                }
            }
        })
    }

    /**
     * Generates a single component from a CodeNode.
     *
     * Useful for preview or incremental generation.
     *
     * @param flowGraph The flow graph containing the node
     * @param nodeId The ID of the node to generate code for
     * @return Generated Kotlin code as a string, or null if node not found
     */
    fun generateSingleComponent(flowGraph: FlowGraph, nodeId: String): String? {
        val node = flowGraph.findNode(nodeId) as? io.codenode.fbpdsl.model.CodeNode
            ?: return null

        return try {
            // Generate to a temporary directory and read back
            val tempDir = createTempDir("codenode_preview_")
            try {
                val fileSpec = kotlinCodeGenerator.generateNodeComponent(node)
                fileSpec.writeTo(tempDir)

                // Find and read the generated file
                val generatedFile = tempDir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .firstOrNull()

                generatedFile?.readText()
            } finally {
                tempDir.deleteRecursively()
            }
        } catch (ex: Exception) {
            logger.warn("Failed to generate single component preview: ${ex.message}")
            null
        }
    }

    /**
     * Gets the recommended target configuration from a flow graph.
     *
     * Analyzes the flow graph's target platforms and returns an appropriate
     * TargetConfig for build script generation.
     *
     * @param flowGraph The flow graph to analyze
     * @return TargetConfig based on flow graph's target platforms
     */
    fun getRecommendedConfig(flowGraph: FlowGraph): BuildScriptGenerator.TargetConfig {
        return buildScriptGenerator.configFromFlowGraph(flowGraph)
    }

    /**
     * Gets the last generation result for a project name.
     *
     * @param projectName The name of the project
     * @return Cached GenerationResult or null if not found
     */
    fun getLastGenerationResult(projectName: String): GenerationResult? {
        return generationCache[projectName]
    }

    /**
     * Clears the generation cache.
     */
    fun clearCache() {
        generationCache.clear()
        logger.debug("Generation cache cleared")
    }

    /**
     * Adds a generation listener.
     */
    fun addListener(listener: CodeGenerationListener) {
        listeners.add(listener)
    }

    /**
     * Removes a generation listener.
     */
    fun removeListener(listener: CodeGenerationListener) {
        listeners.remove(listener)
    }

    /**
     * Writes all generated files to the output directory.
     */
    private fun writeGeneratedFiles(
        projectDir: File,
        generatedProject: GeneratedProject,
        buildScript: String,
        settingsScript: String,
        gradleProperties: String
    ) {
        projectDir.mkdirs()

        // Write build.gradle.kts
        File(projectDir, "build.gradle.kts").writeText(buildScript)

        // Write settings.gradle.kts
        File(projectDir, "settings.gradle.kts").writeText(settingsScript)

        // Write gradle.properties
        File(projectDir, "gradle.properties").writeText(gradleProperties)

        // Create source directories
        val srcDir = File(projectDir, "src/commonMain/kotlin/io/codenode/generated")
        srcDir.mkdirs()

        // Write generated source files
        generatedProject.writeTo(srcDir)
    }

    /**
     * Returns a summary of selected targets.
     */
    private fun getTargetSummary(config: BuildScriptGenerator.TargetConfig): String {
        val targets = mutableListOf<String>()
        if (config.android) targets.add("Android")
        if (config.ios) targets.add("iOS")
        if (config.desktop) targets.add("Desktop")
        if (config.js) targets.add("Web (JS)")
        if (config.wasm) targets.add("Web (Wasm)")
        return if (targets.isEmpty()) "JVM" else targets.joinToString(", ")
    }

    private fun notifyGenerationComplete(result: GenerationResult) {
        listeners.forEach { listener ->
            try {
                listener.onGenerationComplete(result)
            } catch (e: Exception) {
                logger.warn("Error notifying generation listener: ${e.message}")
            }
        }
    }

    private fun notifyGenerationFailed(result: GenerationResult) {
        listeners.forEach { listener ->
            try {
                listener.onGenerationFailed(result)
            } catch (e: Exception) {
                logger.warn("Error notifying generation listener: ${e.message}")
            }
        }
    }

    private fun invokeOnComplete(result: GenerationResult, callback: (GenerationResult) -> Unit) {
        ApplicationManager.getApplication().invokeLater {
            callback(result)
        }
    }

    override fun dispose() {
        generationCache.clear()
        listeners.clear()
    }

    companion object {
        /**
         * Gets the CodeGenerationService instance for a project.
         */
        @JvmStatic
        fun getInstance(project: Project): CodeGenerationService {
            return project.getService(CodeGenerationService::class.java)
        }
    }
}

/**
 * Result of a code generation operation.
 */
data class GenerationResult(
    val success: Boolean,
    val projectName: String?,
    val outputPath: String?,
    val fileCount: Int,
    val targetSummary: String?,
    val generatedFiles: List<String>,
    val errorMessage: String?,
    val errorType: GenerationErrorType?
) {
    companion object {
        fun success(
            projectName: String,
            outputPath: String,
            fileCount: Int,
            targetSummary: String,
            generatedFiles: List<String>
        ): GenerationResult {
            return GenerationResult(
                success = true,
                projectName = projectName,
                outputPath = outputPath,
                fileCount = fileCount,
                targetSummary = targetSummary,
                generatedFiles = generatedFiles,
                errorMessage = null,
                errorType = null
            )
        }

        fun failure(
            errorMessage: String,
            errorType: GenerationErrorType = GenerationErrorType.GENERAL
        ): GenerationResult {
            return GenerationResult(
                success = false,
                projectName = null,
                outputPath = null,
                fileCount = 0,
                targetSummary = null,
                generatedFiles = emptyList(),
                errorMessage = errorMessage,
                errorType = errorType
            )
        }
    }
}

/**
 * Types of generation errors.
 */
enum class GenerationErrorType {
    GENERAL,
    VALIDATION_FAILED,
    LICENSE_VIOLATION,
    IO_ERROR,
    PARSE_ERROR
}

/**
 * Listener for code generation events.
 */
interface CodeGenerationListener {
    /**
     * Called when code generation completes successfully.
     */
    fun onGenerationComplete(result: GenerationResult)

    /**
     * Called when code generation fails.
     */
    fun onGenerationFailed(result: GenerationResult)
}
