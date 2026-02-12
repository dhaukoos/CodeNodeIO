/*
 * ModuleSaveService
 * Creates KMP module structure when saving a FlowGraph
 * License: Apache 2.0
 */

package io.codenode.grapheditor.save

import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.kotlincompiler.generator.FlowKtGenerator
import io.codenode.kotlincompiler.generator.ModuleGenerator
import java.io.File

/**
 * Result of a module save operation.
 *
 * @property success Whether the save succeeded
 * @property moduleDir The created module directory (if successful)
 * @property errorMessage Error message (if failed)
 * @property filesCreated List of files created during save
 */
data class ModuleSaveResult(
    val success: Boolean,
    val moduleDir: File? = null,
    val errorMessage: String? = null,
    val filesCreated: List<String> = emptyList()
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
        const val DEFAULT_PACKAGE_PREFIX = "io.codenode.generated"
    }

    private val moduleGenerator = ModuleGenerator()
    private val flowKtGenerator = FlowKtGenerator()

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

            // T005: Derive package name from module name if not provided
            val effectivePackageName = packageName
                ?: "$DEFAULT_PACKAGE_PREFIX.${effectiveModuleName.lowercase()}"

            // T001/T007: Create module directory
            val moduleDir = File(outputDir, effectiveModuleName)
            if (!moduleDir.exists()) {
                moduleDir.mkdirs()
            }

            val filesCreated = mutableListOf<String>()

            // T003/T007: Create source directory structure
            createDirectoryStructure(moduleDir, effectivePackageName, flowGraph)

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

            // T025: Generate and write .flow.kt file
            val packagePath = effectivePackageName.replace(".", "/")
            val flowKtContent = flowKtGenerator.generateFlowKt(flowGraph, effectivePackageName)
            val flowKtFileName = "${effectiveModuleName}.flow.kt"
            val flowKtFile = File(moduleDir, "src/commonMain/kotlin/$packagePath/$flowKtFileName")
            flowKtFile.writeText(flowKtContent)
            filesCreated.add("src/commonMain/kotlin/$packagePath/$flowKtFileName")

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
