/*
 * ModuleSaveService
 * Creates KMP module structure when saving a FlowGraph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.save

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.kotlincompiler.generator.FlowKtGenerator
import io.codenode.kotlincompiler.generator.ModuleGenerator
import io.codenode.kotlincompiler.generator.ProcessingLogicStubGenerator
import io.codenode.kotlincompiler.generator.StatePropertiesGenerator
import io.codenode.kotlincompiler.generator.RuntimeFlowGenerator
import io.codenode.kotlincompiler.generator.RuntimeControllerGenerator
import io.codenode.kotlincompiler.generator.RuntimeControllerInterfaceGenerator
import io.codenode.kotlincompiler.generator.RuntimeControllerAdapterGenerator
import io.codenode.kotlincompiler.generator.RuntimeViewModelGenerator
import java.io.File

/**
 * Result of a module save operation.
 *
 * @property success Whether the save succeeded
 * @property moduleDir The created module directory (if successful)
 * @property errorMessage Error message (if failed)
 * @property filesCreated List of new files created during save
 * @property filesOverwritten List of files overwritten during save
 * @property filesDeleted List of orphaned files deleted during save
 * @property warnings List of warnings (non-fatal issues)
 */
data class ModuleSaveResult(
    val success: Boolean,
    val moduleDir: File? = null,
    val errorMessage: String? = null,
    val filesCreated: List<String> = emptyList(),
    val filesOverwritten: List<String> = emptyList(),
    val filesDeleted: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Service for saving FlowGraphs as KMP module structures.
 *
 * The unified [saveModule] creates the full module in a single call:
 * module directory, gradle files, .flow.kt in the base package source set,
 * 5 runtime files under generated/, ProcessingLogic stubs under processingLogic/,
 * and StateProperties stubs under stateProperties/. Orphaned stubs are deleted.
 */
class ModuleSaveService {

    companion object {
        const val DEFAULT_PACKAGE_PREFIX = "io.codenode"
        const val GENERATED_SUBPACKAGE = "generated"
        const val PROCESSING_LOGIC_SUBPACKAGE = "processingLogic"
        const val STATE_PROPERTIES_SUBPACKAGE = "stateProperties"
    }

    private val moduleGenerator = ModuleGenerator()
    private val flowKtGenerator = FlowKtGenerator()
    private val stubGenerator = ProcessingLogicStubGenerator()
    private val statePropertiesGenerator = StatePropertiesGenerator()
    private val runtimeFlowGenerator = RuntimeFlowGenerator()
    private val runtimeControllerGenerator = RuntimeControllerGenerator()
    private val runtimeControllerInterfaceGenerator = RuntimeControllerInterfaceGenerator()
    private val runtimeControllerAdapterGenerator = RuntimeControllerAdapterGenerator()
    private val runtimeViewModelGenerator = RuntimeViewModelGenerator()

    /**
     * Saves a FlowGraph as a complete KMP module.
     *
     * Creates the full module in a single call: module directory, gradle files,
     * .flow.kt in the base package source set, 5 runtime files, ProcessingLogic stubs, and
     * StateProperties stubs. On re-save, .flow.kt and runtime files are always
     * overwritten, existing stubs are preserved, new stubs are created for added
     * nodes, and orphaned stubs are deleted for removed nodes.
     *
     * @param flowGraph The flow graph to save
     * @param outputDir Parent directory where the module will be created
     * @param packageName Package name for generated code (default: io.codenode.{modulename})
     * @param moduleName Module name (default: derived from FlowGraph name)
     * @return ModuleSaveResult with success status, module directory, and file tracking
     */
    fun saveModule(
        flowGraph: FlowGraph,
        outputDir: File,
        packageName: String? = null,
        moduleName: String? = null
    ): ModuleSaveResult {
        return try {
            val effectiveModuleName = moduleName ?: deriveModuleName(flowGraph.name)

            val basePackage = packageName ?: "$DEFAULT_PACKAGE_PREFIX.${effectiveModuleName.lowercase()}"
            val generatedPackage = "$basePackage.$GENERATED_SUBPACKAGE"
            val processingLogicPackage = "$basePackage.$PROCESSING_LOGIC_SUBPACKAGE"
            val statePropertiesPackage = "$basePackage.$STATE_PROPERTIES_SUBPACKAGE"

            // Create module directory
            val moduleDir = File(outputDir, effectiveModuleName)
            if (!moduleDir.exists()) {
                moduleDir.mkdirs()
            }

            val filesCreated = mutableListOf<String>()
            val filesOverwritten = mutableListOf<String>()
            val filesDeleted = mutableListOf<String>()

            // Create source directory structure
            createDirectoryStructure(moduleDir, basePackage, flowGraph)
            createDirectoryStructure(moduleDir, generatedPackage, flowGraph)
            createDirectoryStructure(moduleDir, processingLogicPackage, flowGraph)
            createDirectoryStructure(moduleDir, statePropertiesPackage, flowGraph)

            // Write gradle files (only if they don't exist)
            writeFileIfNew(
                File(moduleDir, "build.gradle.kts"),
                moduleGenerator.generateBuildGradle(flowGraph, effectiveModuleName),
                "build.gradle.kts",
                filesCreated
            )
            writeFileIfNew(
                File(moduleDir, "settings.gradle.kts"),
                generateSettingsGradle(effectiveModuleName),
                "settings.gradle.kts",
                filesCreated
            )

            // Write .flow.kt in source set (always overwrite)
            val basePackagePath = basePackage.replace(".", "/")
            val flowKtFileName = "${effectiveModuleName}.flow.kt"
            val flowKtRelativePath = "src/commonMain/kotlin/$basePackagePath/$flowKtFileName"
            writeFileAlways(
                File(moduleDir, flowKtRelativePath),
                flowKtGenerator.generateFlowKt(flowGraph, basePackage, processingLogicPackage),
                flowKtRelativePath,
                filesCreated,
                filesOverwritten
            )

            // Generate 5 runtime files (always overwrite)
            generateRuntimeFilesTracked(
                flowGraph, moduleDir, generatedPackage, processingLogicPackage,
                statePropertiesPackage, effectiveModuleName, filesCreated, filesOverwritten
            )

            // Generate processing logic stubs (don't overwrite existing)
            generateProcessingLogicStubs(flowGraph, moduleDir, processingLogicPackage, statePropertiesPackage, filesCreated)

            // Generate state properties files (don't overwrite existing)
            generateStatePropertiesFiles(flowGraph, moduleDir, statePropertiesPackage, filesCreated)

            // Delete orphaned stubs
            deleteOrphanedComponents(flowGraph, moduleDir, processingLogicPackage, filesDeleted)
            deleteOrphanedStateProperties(flowGraph, moduleDir, statePropertiesPackage, filesDeleted)

            ModuleSaveResult(
                success = true,
                moduleDir = moduleDir,
                filesCreated = filesCreated,
                filesOverwritten = filesOverwritten,
                filesDeleted = filesDeleted
            )
        } catch (e: Exception) {
            ModuleSaveResult(
                success = false,
                errorMessage = "Failed to save module: ${e.message}"
            )
        }
    }

    /**
     * Derives a valid module name from a FlowGraph name.
     *
     * Handles:
     * - Spaces (replaced with nothing, using PascalCase)
     * - Special characters (removed)
     *
     * @param flowGraphName The FlowGraph name
     * @return A valid module directory name
     */
    internal fun deriveModuleName(flowGraphName: String): String {
        // Split on spaces and capitalize each word (PascalCase)
        return flowGraphName
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString("") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    /**
     * Creates the source directory structure for the module.
     *
     * Creates:
     * - src/commonMain/kotlin/{package}
     * - src/commonTest/kotlin/{package}
     * - Platform-specific directories based on FlowGraph targets
     *
     * @param moduleDir The module root directory
     * @param packageName The package name (e.g., io.codenode.stopwatch.generated)
     * @param flowGraph The flow graph (for determining target platforms)
     */
    private fun createDirectoryStructure(
        moduleDir: File,
        packageName: String,
        flowGraph: FlowGraph
    ) {
        val packagePath = packageName.replace(".", "/")

        // Common source sets (always created)
        File(moduleDir, "src/commonMain/kotlin/$packagePath").mkdirs()
        File(moduleDir, "src/commonTest/kotlin/$packagePath").mkdirs()

        // JVM source sets (always included)
        File(moduleDir, "src/jvmMain/kotlin/$packagePath").mkdirs()
        File(moduleDir, "src/jvmTest/kotlin").mkdirs()

        // Platform-specific source sets
        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_ANDROID)) {
            File(moduleDir, "src/androidMain/kotlin/$packagePath").mkdirs()
            File(moduleDir, "src/androidTest/kotlin").mkdirs()
        }

        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_IOS)) {
            File(moduleDir, "src/iosMain/kotlin/$packagePath").mkdirs()
            File(moduleDir, "src/iosTest/kotlin").mkdirs()
        }

        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WEB)) {
            File(moduleDir, "src/jsMain/kotlin/$packagePath").mkdirs()
            File(moduleDir, "src/jsTest/kotlin").mkdirs()
        }

        if (flowGraph.targetsPlatform(FlowGraph.TargetPlatform.KMP_WASM)) {
            File(moduleDir, "src/wasmJsMain/kotlin/$packagePath").mkdirs()
        }
    }

    /**
     * Generates ProcessingLogic stub files for each CodeNode in the FlowGraph.
     *
     * Creates one stub file per CodeNode, only if the file doesn't already exist
     * (to preserve user implementations).
     *
     * @param flowGraph The flow graph containing CodeNodes
     * @param moduleDir The module root directory
     * @param packageName The package name for generated files
     * @param statePropertiesPackage The stateProperties package for import in stubs
     * @param filesCreated List to track created files
     */
    private fun generateProcessingLogicStubs(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String,
        statePropertiesPackage: String,
        filesCreated: MutableList<String>
    ) {
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        // Get all CodeNodes from the flow graph (including nested ones)
        val codeNodes = flowGraph.getAllCodeNodes()

        for (codeNode in codeNodes) {
            val stubFileName = stubGenerator.getStubFileName(codeNode)
            val stubFile = File(sourceDir, stubFileName)

            // Only create stub if file doesn't exist (preserve user implementations)
            if (!stubFile.exists()) {
                val stubContent = stubGenerator.generateStub(codeNode, packageName, statePropertiesPackage)
                stubFile.writeText(stubContent)
                filesCreated.add("src/commonMain/kotlin/$packagePath/$stubFileName")
            }
        }
    }

    /**
     * Generates state properties files for each CodeNode in the FlowGraph.
     *
     * Creates one state properties file per CodeNode that has ports,
     * only if the file doesn't already exist (to preserve user modifications).
     *
     * @param flowGraph The flow graph containing CodeNodes
     * @param moduleDir The module root directory
     * @param packageName The package name for state properties files
     * @param filesCreated List to track created files
     */
    private fun generateStatePropertiesFiles(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String,
        filesCreated: MutableList<String>
    ) {
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        val codeNodes = flowGraph.getAllCodeNodes()

        for (codeNode in codeNodes) {
            if (!statePropertiesGenerator.shouldGenerate(codeNode)) continue

            val fileName = statePropertiesGenerator.getStatePropertiesFileName(codeNode)
            val file = File(sourceDir, fileName)

            // Only create if file doesn't exist (preserve user modifications)
            if (!file.exists()) {
                val content = statePropertiesGenerator.generateStateProperties(codeNode, packageName)
                file.writeText(content)
                filesCreated.add("src/commonMain/kotlin/$packagePath/$fileName")
            }
        }
    }

    /**
     * Writes a file only if it doesn't already exist.
     * Tracks newly created files in [filesCreated].
     */
    private fun writeFileIfNew(
        file: File,
        content: String,
        relativePath: String,
        filesCreated: MutableList<String>
    ) {
        if (!file.exists()) {
            file.writeText(content)
            filesCreated.add(relativePath)
        }
    }

    /**
     * Writes a file, always overwriting existing content.
     * Tracks as [filesOverwritten] if file existed, or [filesCreated] if new.
     */
    private fun writeFileAlways(
        file: File,
        content: String,
        relativePath: String,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>
    ) {
        val existed = file.exists()
        file.writeText(content)
        if (existed) filesOverwritten.add(relativePath) else filesCreated.add(relativePath)
    }

    /**
     * Generates the 5 runtime files with proper overwrite tracking.
     * Files are always written; tracked as overwritten if they existed, created if new.
     */
    private fun generateRuntimeFilesTracked(
        flowGraph: FlowGraph,
        moduleDir: File,
        generatedPackage: String,
        processingLogicPackage: String,
        statePropertiesPackage: String,
        effectiveModuleName: String,
        filesCreated: MutableList<String>,
        filesOverwritten: MutableList<String>
    ) {
        val generatedPath = generatedPackage.replace(".", "/")
        val generatedDir = File(moduleDir, "src/commonMain/kotlin/$generatedPath")

        val runtimeFiles = listOf(
            "${effectiveModuleName}Flow.kt" to runtimeFlowGenerator.generate(flowGraph, generatedPackage, processingLogicPackage, statePropertiesPackage),
            "${effectiveModuleName}Controller.kt" to runtimeControllerGenerator.generate(flowGraph, generatedPackage, processingLogicPackage),
            "${effectiveModuleName}ControllerInterface.kt" to runtimeControllerInterfaceGenerator.generate(flowGraph, generatedPackage),
            "${effectiveModuleName}ControllerAdapter.kt" to runtimeControllerAdapterGenerator.generate(flowGraph, generatedPackage),
            "${effectiveModuleName}ViewModel.kt" to runtimeViewModelGenerator.generate(flowGraph, generatedPackage)
        )

        for ((fileName, content) in runtimeFiles) {
            val file = File(generatedDir, fileName)
            val relativePath = "src/commonMain/kotlin/$generatedPath/$fileName"
            writeFileAlways(file, content, relativePath, filesCreated, filesOverwritten)
        }
    }

    /**
     * Deletes orphaned ProcessingLogic files (stubs for nodes that no longer exist).
     * Tracks deleted files in [filesDeleted].
     */
    private fun deleteOrphanedComponents(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String,
        filesDeleted: MutableList<String>
    ) {
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        if (!sourceDir.exists()) return

        val codeNodes = flowGraph.getAllCodeNodes()
        val expectedFiles = codeNodes.map { stubGenerator.getStubFileName(it) }.toSet()

        val existingComponentFiles = sourceDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith("ProcessLogic.kt") }
            ?: emptyList()

        for (file in existingComponentFiles) {
            if (file.name !in expectedFiles) {
                val relativePath = "src/commonMain/kotlin/$packagePath/${file.name}"
                file.delete()
                filesDeleted.add(relativePath)
            }
        }
    }

    /**
     * Deletes orphaned StateProperties files (for nodes that no longer exist).
     * Tracks deleted files in [filesDeleted].
     */
    private fun deleteOrphanedStateProperties(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String,
        filesDeleted: MutableList<String>
    ) {
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        if (!sourceDir.exists()) return

        val codeNodes = flowGraph.getAllCodeNodes()
        val expectedFiles = codeNodes
            .filter { statePropertiesGenerator.shouldGenerate(it) }
            .map { statePropertiesGenerator.getStatePropertiesFileName(it) }
            .toSet()

        val existingFiles = sourceDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith("StateProperties.kt") }
            ?: emptyList()

        for (file in existingFiles) {
            if (file.name !in expectedFiles) {
                val relativePath = "src/commonMain/kotlin/$packagePath/${file.name}"
                file.delete()
                filesDeleted.add(relativePath)
            }
        }
    }

    /**
     * Generates settings.gradle.kts content.
     *
     * @param moduleName The module name
     * @return Generated settings.gradle.kts content
     */
    private fun generateSettingsGradle(moduleName: String): String {
        return buildString {
            appendLine("/*")
            appendLine(" * Settings for $moduleName")
            appendLine(" * Generated by CodeNodeIO ModuleSaveService")
            appendLine(" * License: Apache 2.0")
            appendLine(" */")
            appendLine()
            appendLine("pluginManagement {")
            appendLine("    repositories {")
            appendLine("        google()")
            appendLine("        mavenCentral()")
            appendLine("        gradlePluginPortal()")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("dependencyResolutionManagement {")
            appendLine("    repositories {")
            appendLine("        google()")
            appendLine("        mavenCentral()")
            appendLine("    }")
            appendLine("}")
            appendLine()
            appendLine("rootProject.name = \"$moduleName\"")
        }
    }
}
