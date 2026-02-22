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
 * @property filesCreated List of files created during save
 * @property warnings List of warnings (e.g., orphaned ProcessingLogic files)
 */
data class ModuleSaveResult(
    val success: Boolean,
    val moduleDir: File? = null,
    val errorMessage: String? = null,
    val filesCreated: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Service for saving FlowGraphs as KMP module structures.
 *
 * Two operations:
 * - **Save**: Creates module directory structure (gradle files) + writes the .flow.kt
 *   DSL file at the module root (alongside build.gradle.kts).
 * - **Compile**: Generates the 5 runtime files under generated/ and ProcessingLogic
 *   stubs under processingLogic/.
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
     * Saves a FlowGraph as a KMP module structure.
     *
     * Creates the module directory, gradle files, directory structure, and
     * writes the .flow.kt DSL file at the module root.
     *
     * @param flowGraph The flow graph to save
     * @param outputDir Parent directory where the module will be created
     * @param packageName Package name for generated code (default: io.codenode.{modulename})
     * @param moduleName Module name (default: derived from FlowGraph name)
     * @return ModuleSaveResult with success status and module directory
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

            // Create module directory
            val moduleDir = File(outputDir, effectiveModuleName)
            if (!moduleDir.exists()) {
                moduleDir.mkdirs()
            }

            val filesCreated = mutableListOf<String>()

            // Create source directory structure (both generated and processingLogic)
            createDirectoryStructure(moduleDir, generatedPackage, flowGraph)
            createDirectoryStructure(moduleDir, processingLogicPackage, flowGraph)

            // Generate and write build.gradle.kts
            val buildGradleContent = moduleGenerator.generateBuildGradle(flowGraph, effectiveModuleName)
            val buildGradleFile = File(moduleDir, "build.gradle.kts")
            buildGradleFile.writeText(buildGradleContent)
            filesCreated.add("build.gradle.kts")

            // Generate and write settings.gradle.kts
            val settingsGradleContent = generateSettingsGradle(effectiveModuleName)
            val settingsGradleFile = File(moduleDir, "settings.gradle.kts")
            settingsGradleFile.writeText(settingsGradleContent)
            filesCreated.add("settings.gradle.kts")

            // Generate and write .flow.kt file at module root
            val flowKtContent = flowKtGenerator.generateFlowKt(flowGraph, basePackage, processingLogicPackage)
            val flowKtFileName = "${effectiveModuleName}.flow.kt"
            val flowKtFile = File(moduleDir, flowKtFileName)
            flowKtFile.writeText(flowKtContent)
            filesCreated.add(flowKtFileName)

            ModuleSaveResult(
                success = true,
                moduleDir = moduleDir,
                filesCreated = filesCreated
            )
        } catch (e: Exception) {
            ModuleSaveResult(
                success = false,
                errorMessage = "Failed to save module: ${e.message}"
            )
        }
    }

    /**
     * Compiles a FlowGraph by generating runtime files and ProcessingLogic stubs.
     *
     * Generates the 5 runtime files under the generated/ package and
     * ProcessingLogic stubs under the processingLogic/ package.
     *
     * @param flowGraph The flow graph to compile
     * @param outputDir Parent directory where the module exists
     * @param packageName Package name for generated code (default: io.codenode.{modulename})
     * @param moduleName Module name (default: derived from FlowGraph name)
     * @return ModuleSaveResult with success status and files created
     */
    fun compileModule(
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

            val moduleDir = File(outputDir, effectiveModuleName)

            // Ensure directory structure exists (in case compile is run without prior save)
            createDirectoryStructure(moduleDir, generatedPackage, flowGraph)
            createDirectoryStructure(moduleDir, processingLogicPackage, flowGraph)
            createDirectoryStructure(moduleDir, statePropertiesPackage, flowGraph)

            val filesCreated = mutableListOf<String>()

            // Generate runtime files (Flow, Controller, Interface, Adapter, ViewModel)
            generateRuntimeFiles(flowGraph, moduleDir, generatedPackage, processingLogicPackage, statePropertiesPackage, effectiveModuleName, filesCreated)

            // Generate ProcessingLogic stub files (in processingLogic package)
            generateProcessingLogicStubs(flowGraph, moduleDir, processingLogicPackage, statePropertiesPackage, filesCreated)

            // Generate state properties files (in stateProperties package)
            generateStatePropertiesFiles(flowGraph, moduleDir, statePropertiesPackage, filesCreated)

            // Detect orphaned files
            val warnings = detectOrphanedComponents(flowGraph, moduleDir, processingLogicPackage) +
                detectOrphanedStateProperties(flowGraph, moduleDir, statePropertiesPackage)

            ModuleSaveResult(
                success = true,
                moduleDir = moduleDir,
                filesCreated = filesCreated,
                warnings = warnings
            )
        } catch (e: Exception) {
            ModuleSaveResult(
                success = false,
                errorMessage = "Failed to compile module: ${e.message}"
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
     * Generates the 5 runtime files (Flow, Controller, ControllerInterface,
     * ControllerAdapter, ViewModel) in the generated package directory.
     *
     * These files are always overwritten since they are fully generated.
     */
    private fun generateRuntimeFiles(
        flowGraph: FlowGraph,
        moduleDir: File,
        generatedPackage: String,
        processingLogicPackage: String,
        statePropertiesPackage: String,
        effectiveModuleName: String,
        filesCreated: MutableList<String>
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
            File(generatedDir, fileName).writeText(content)
            filesCreated.add("src/commonMain/kotlin/$generatedPath/$fileName")
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
     * Detects orphaned ProcessingLogic files (components for nodes that no longer exist).
     *
     * Scans the source directory for *ProcessLogic.kt files and compares against
     * the current FlowGraph's CodeNodes. Files that don't correspond to any
     * current node are considered orphaned.
     *
     * @param flowGraph The current flow graph
     * @param moduleDir The module root directory
     * @param packageName The package name
     * @return List of warning messages for orphaned files
     */
    private fun detectOrphanedComponents(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String
    ): List<String> {
        val warnings = mutableListOf<String>()
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        if (!sourceDir.exists()) return warnings

        // Get expected component file names from current FlowGraph
        val codeNodes = flowGraph.getAllCodeNodes()
        val expectedFiles = codeNodes.map { stubGenerator.getStubFileName(it) }.toSet()

        // Also include known non-component files to ignore
        val moduleName = moduleDir.name
        val ignoredFiles = setOf(
            "$moduleName.flow.kt",
            "${moduleName}Factory.kt"
        )

        // Find all *ProcessLogic.kt files in source directory
        val existingComponentFiles = sourceDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith("ProcessLogic.kt") }
            ?.map { it.name }
            ?: emptyList()

        // Detect orphaned files
        for (fileName in existingComponentFiles) {
            if (fileName !in expectedFiles && fileName !in ignoredFiles) {
                val componentName = fileName.removeSuffix(".kt")
                warnings.add("Orphaned ProcessingLogic file: $componentName (node may have been removed from flow)")
            }
        }

        return warnings
    }

    /**
     * Detects orphaned state properties files (for nodes that no longer exist).
     *
     * Scans the stateProperties directory for *StateProperties.kt files and compares
     * against the current FlowGraph's CodeNodes. Files that don't correspond to any
     * current node are considered orphaned.
     *
     * @param flowGraph The current flow graph
     * @param moduleDir The module root directory
     * @param packageName The stateProperties package name
     * @return List of warning messages for orphaned files
     */
    private fun detectOrphanedStateProperties(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String
    ): List<String> {
        val warnings = mutableListOf<String>()
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        if (!sourceDir.exists()) return warnings

        val codeNodes = flowGraph.getAllCodeNodes()
        val expectedFiles = codeNodes
            .filter { statePropertiesGenerator.shouldGenerate(it) }
            .map { statePropertiesGenerator.getStatePropertiesFileName(it) }
            .toSet()

        val existingFiles = sourceDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith("StateProperties.kt") }
            ?.map { it.name }
            ?: emptyList()

        for (fileName in existingFiles) {
            if (fileName !in expectedFiles) {
                val componentName = fileName.removeSuffix(".kt")
                warnings.add("Orphaned StateProperties file: $componentName (node may have been removed from flow)")
            }
        }

        return warnings
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
