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
 * When a FlowGraph is saved, this service creates:
 * - Module directory with build.gradle.kts and settings.gradle.kts
 * - Source directory structure (src/commonMain/kotlin/{package})
 * - ProcessingLogic stub files for each CodeNode (Phase 3)
 * - The .flow.kt file defining the FlowGraph (Phase 2)
 *
 * T006: Create ModuleSaveService class with saveModule method
 * T007: Implement module directory creation with proper structure
 * T008: Implement build.gradle.kts generation
 * T009: Implement settings.gradle.kts generation
 * T010: Integrate into graphEditor Save action
 */
class ModuleSaveService {

    companion object {
        const val DEFAULT_PACKAGE_PREFIX = "io.codenode"
        const val GENERATED_SUBPACKAGE = "generated"
        const val USECASES_SUBPACKAGE = "usecases"
    }

    private val moduleGenerator = ModuleGenerator()
    private val flowKtGenerator = FlowKtGenerator()
    private val stubGenerator = ProcessingLogicStubGenerator()

    /**
     * Saves a FlowGraph as a KMP module.
     *
     * @param flowGraph The flow graph to save
     * @param outputDir Parent directory where the module will be created
     * @param packageName Package name for generated code (default: io.codenode.generated.{modulename})
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
            // T005: Derive module name from FlowGraph name
            val effectiveModuleName = moduleName ?: deriveModuleName(flowGraph.name)

            // Package structure:
            // - basePackage: io.codenode.{modulename} (e.g., io.codenode.stopwatch)
            // - generatedPackage: io.codenode.{modulename}.generated (for generated code)
            // - usecasesPackage: io.codenode.{modulename}.usecases (for user-implemented components)
            val basePackage = packageName ?: "$DEFAULT_PACKAGE_PREFIX.${effectiveModuleName.lowercase()}"
            val generatedPackage = "$basePackage.$GENERATED_SUBPACKAGE"
            val usecasesPackage = "$basePackage.$USECASES_SUBPACKAGE"

            // T001/T007: Create module directory
            val moduleDir = File(outputDir, effectiveModuleName)
            if (!moduleDir.exists()) {
                moduleDir.mkdirs()
            }

            val filesCreated = mutableListOf<String>()

            // T003/T007: Create source directory structure (both generated and usecases)
            createDirectoryStructure(moduleDir, generatedPackage, flowGraph)
            createDirectoryStructure(moduleDir, usecasesPackage, flowGraph)

            // T008: Generate and write build.gradle.kts
            val buildGradleContent = moduleGenerator.generateBuildGradle(flowGraph, effectiveModuleName)
            val buildGradleFile = File(moduleDir, "build.gradle.kts")
            buildGradleFile.writeText(buildGradleContent)
            filesCreated.add("build.gradle.kts")

            // T009: Generate and write settings.gradle.kts
            val settingsGradleContent = generateSettingsGradle(effectiveModuleName)
            val settingsGradleFile = File(moduleDir, "settings.gradle.kts")
            settingsGradleFile.writeText(settingsGradleContent)
            filesCreated.add("settings.gradle.kts")

            // T025: Generate and write .flow.kt file (in generated package)
            val generatedPath = generatedPackage.replace(".", "/")
            val flowKtContent = flowKtGenerator.generateFlowKt(flowGraph, generatedPackage, usecasesPackage)
            val flowKtFileName = "${effectiveModuleName}.flow.kt"
            val flowKtFile = File(moduleDir, "src/commonMain/kotlin/$generatedPath/$flowKtFileName")
            flowKtFile.writeText(flowKtContent)
            filesCreated.add("src/commonMain/kotlin/$generatedPath/$flowKtFileName")

            // T036/T037: Generate ProcessingLogic stub files (in usecases package)
            generateProcessingLogicStubs(flowGraph, moduleDir, usecasesPackage, filesCreated)

            // T047: Detect orphaned ProcessingLogic files (in usecases package)
            val warnings = detectOrphanedComponents(flowGraph, moduleDir, usecasesPackage)

            ModuleSaveResult(
                success = true,
                moduleDir = moduleDir,
                filesCreated = filesCreated,
                warnings = warnings
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
    private fun deriveModuleName(flowGraphName: String): String {
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
     * @param packageName The package name (e.g., io.codenode.generated.stopwatch)
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
     * T036/T037: Generates ProcessingLogic stub files for each CodeNode in the FlowGraph.
     *
     * Creates one stub file per CodeNode, only if the file doesn't already exist
     * (to preserve user implementations).
     *
     * @param flowGraph The flow graph containing CodeNodes
     * @param moduleDir The module root directory
     * @param packageName The package name for generated files
     * @param filesCreated List to track created files
     */
    private fun generateProcessingLogicStubs(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String,
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
                val stubContent = stubGenerator.generateStub(codeNode, packageName)
                stubFile.writeText(stubContent)
                filesCreated.add("src/commonMain/kotlin/$packagePath/$stubFileName")
            }
        }
    }

    /**
     * T047: Detects orphaned ProcessingLogic files (components for nodes that no longer exist).
     *
     * Scans the source directory for *Component.kt files and compares against
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

        // Find all *Component.kt files in source directory
        val existingComponentFiles = sourceDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith("Component.kt") }
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
