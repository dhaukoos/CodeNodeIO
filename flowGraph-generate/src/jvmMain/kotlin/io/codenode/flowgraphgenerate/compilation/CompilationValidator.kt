/*
 * CompilationValidator
 * Validates module structure before compilation
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.compilation

import io.codenode.fbpdsl.model.FlowGraph
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
 * - Module directory exists
 * - Source directory structure is correct
 * - Build configuration is valid
 */
class CompilationValidator {

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

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
