/*
 * CompilationValidator
 * Validates module structure before compilation
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compilation

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.FlowGraph
import io.codenode.kotlincompiler.generator.ComponentValidationResult
import io.codenode.kotlincompiler.generator.FlowGraphFactoryGenerator
import java.io.File

/**
 * Result of module validation.
 *
 * @property isValid Whether the module is valid for compilation
 * @property errors List of validation errors
 * @property warnings List of validation warnings
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Validates module structure before compilation.
 *
 * Performs validation checks to ensure:
 * - All required ProcessingLogic classes exist
 * - Source directory structure is correct
 * - Build configuration is valid
 *
 * T043: Update compile validation to check ProcessingLogic classes exist in module
 */
class CompilationValidator {

    private val factoryGenerator = FlowGraphFactoryGenerator()

    /**
     * Validates a module is ready for compilation.
     *
     * @param flowGraph The flow graph to validate
     * @param moduleDir The module root directory
     * @param packageName The package name for generated code
     * @return ValidationResult indicating validity and any errors/warnings
     */
    fun validateModule(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Check module directory exists
        if (!moduleDir.exists()) {
            errors.add("Module directory does not exist: ${moduleDir.absolutePath}")
            return ValidationResult(false, errors, warnings)
        }

        // Check build.gradle.kts exists
        val buildFile = File(moduleDir, "build.gradle.kts")
        if (!buildFile.exists()) {
            errors.add("build.gradle.kts not found in module directory")
        }

        // Check source directory structure
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")
        if (!sourceDir.exists()) {
            errors.add("Source directory not found: ${sourceDir.absolutePath}")
        }

        // T043: Validate ProcessingLogic classes exist
        val componentValidation = validateProcessingLogicClasses(flowGraph, sourceDir)
        if (!componentValidation.isValid) {
            componentValidation.missingComponents.forEach { missing ->
                errors.add("Missing ProcessingLogic implementation: $missing.kt")
            }
        }

        // Check for orphaned ProcessingLogic files (warning only)
        val orphanedFiles = findOrphanedComponents(flowGraph, sourceDir)
        orphanedFiles.forEach { orphaned ->
            warnings.add("Orphaned ProcessingLogic file found: $orphaned (node may have been removed)")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validates that all required ProcessingLogic classes exist.
     *
     * @param flowGraph The flow graph to validate
     * @param sourceDir The source directory to check
     * @return ComponentValidationResult indicating validity and missing components
     */
    fun validateProcessingLogicClasses(
        flowGraph: FlowGraph,
        sourceDir: File
    ): ComponentValidationResult {
        if (!sourceDir.exists()) {
            val required = factoryGenerator.getRequiredComponents(flowGraph)
            return ComponentValidationResult(
                isValid = false,
                missingComponents = required
            )
        }

        val existingFiles = sourceDir.listFiles()
            ?.filter { it.extension == "kt" }
            ?.map { it.name }
            ?.toSet() ?: emptySet()

        return factoryGenerator.validateComponents(flowGraph, existingFiles)
    }

    /**
     * Finds ProcessingLogic files that don't correspond to any node.
     *
     * @param flowGraph The flow graph to check against
     * @param sourceDir The source directory to scan
     * @return List of orphaned file names
     */
    fun findOrphanedComponents(
        flowGraph: FlowGraph,
        sourceDir: File
    ): List<String> {
        if (!sourceDir.exists()) return emptyList()

        val requiredComponents = factoryGenerator.getRequiredComponents(flowGraph).toSet()
        val expectedFiles = requiredComponents.map { "$it.kt" }.toSet()

        // Also include the .flow.kt file and factory file as expected
        val flowKtFile = "${flowGraph.name}Factory.kt"
        val flowDefFile = "${flowGraph.name}.flow.kt"

        val existingFiles = sourceDir.listFiles()
            ?.filter { it.extension == "kt" }
            ?.map { it.name }
            ?: emptyList()

        return existingFiles.filter { fileName ->
            fileName !in expectedFiles &&
                fileName != flowKtFile &&
                fileName != flowDefFile &&
                fileName.endsWith("Component.kt") // Only flag Component files
        }
    }

    /**
     * Quick validation check for ProcessingLogic class existence.
     *
     * @param flowGraph The flow graph to validate
     * @param moduleDir The module root directory
     * @param packageName The package name
     * @return True if all ProcessingLogic classes exist
     */
    fun hasAllProcessingLogicClasses(
        flowGraph: FlowGraph,
        moduleDir: File,
        packageName: String
    ): Boolean {
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")
        return validateProcessingLogicClasses(flowGraph, sourceDir).isValid
    }
}
