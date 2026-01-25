/*
 * License Validator
 * Validates dependencies against constitution licensing requirements
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.validator

/**
 * Validates that dependencies comply with the project constitution's licensing requirements.
 *
 * Per constitution: NO GPL/LGPL/AGPL dependencies allowed.
 *
 * This validator checks:
 * - Import statements in generated code
 * - Dependency declarations in build scripts
 * - Known restricted packages from various ecosystems
 */
class LicenseValidator {

    /**
     * Result of a license validation check.
     */
    data class ValidationResult(
        val isValid: Boolean,
        val violations: List<LicenseViolation> = emptyList(),
        val warnings: List<String> = emptyList()
    ) {
        fun hasViolations(): Boolean = violations.isNotEmpty()
        fun hasWarnings(): Boolean = warnings.isNotEmpty()
    }

    /**
     * Represents a single license violation.
     */
    data class LicenseViolation(
        val packageName: String,
        val licenseType: LicenseType,
        val reason: String,
        val suggestion: String? = null
    )

    /**
     * License types that we track.
     */
    enum class LicenseType {
        GPL,
        LGPL,
        AGPL,
        UNKNOWN_RESTRICTIVE,
        SAFE
    }

    /**
     * Known restricted packages organized by license type.
     */
    private val restrictedPackages: Map<String, LicenseType> = mapOf(
        // GPL packages
        "gnu.classpath" to LicenseType.GPL,
        "org.gnu" to LicenseType.GPL,
        "com.mysql.jdbc" to LicenseType.GPL, // MySQL Connector/J (GPL with FOSS exception)
        "org.netbeans" to LicenseType.GPL, // CDDL+GPL

        // LGPL packages
        "org.hibernate.ogm" to LicenseType.LGPL,
        "ch.qos.logback" to LicenseType.LGPL, // Dual licensed, but LGPL by default
        "com.github.spotbugs" to LicenseType.LGPL,
        "org.jboss.logging" to LicenseType.LGPL,
        "org.aspectj" to LicenseType.LGPL, // Some components

        // AGPL packages
        "mongodb.driver" to LicenseType.AGPL, // MongoDB drivers are Apache, but server is AGPL
        "com.neo4j" to LicenseType.AGPL, // Neo4j Community is GPL

        // Other restrictive
        "oracle.jdbc" to LicenseType.UNKNOWN_RESTRICTIVE, // Oracle proprietary
        "com.ibm.db2" to LicenseType.UNKNOWN_RESTRICTIVE // IBM proprietary
    )

    /**
     * Known safe packages (Apache 2.0, MIT, BSD, etc.)
     */
    private val safePackages: Set<String> = setOf(
        "org.jetbrains.kotlin",
        "org.jetbrains.kotlinx",
        "org.jetbrains.compose",
        "com.squareup.kotlinpoet",
        "com.squareup.okhttp3",
        "com.squareup.retrofit2",
        "com.squareup.moshi",
        "io.ktor",
        "com.google.code.gson",
        "com.google.guava",
        "com.fasterxml.jackson",
        "org.apache",
        "org.slf4j", // MIT
        "ch.qos.cal10n", // LGPL but often used with SLF4J
        "io.netty", // Apache 2.0
        "com.github.ben-manes.caffeine" // Apache 2.0
    )

    /**
     * Validates import statements in generated code.
     *
     * @param code The generated code to validate
     * @return ValidationResult with any violations found
     */
    fun validateCode(code: String): ValidationResult {
        val violations = mutableListOf<LicenseViolation>()
        val warnings = mutableListOf<String>()

        // Extract all import statements
        val imports = extractImports(code)

        imports.forEach { importStatement ->
            val (licenseType, packagePrefix) = checkPackageLicense(importStatement)

            when (licenseType) {
                LicenseType.GPL -> violations.add(
                    LicenseViolation(
                        packageName = importStatement,
                        licenseType = LicenseType.GPL,
                        reason = "GPL-licensed package detected: $packagePrefix",
                        suggestion = "Replace with Apache 2.0 or MIT licensed alternative"
                    )
                )
                LicenseType.LGPL -> violations.add(
                    LicenseViolation(
                        packageName = importStatement,
                        licenseType = LicenseType.LGPL,
                        reason = "LGPL-licensed package detected: $packagePrefix",
                        suggestion = "Consider using a permissively licensed alternative"
                    )
                )
                LicenseType.AGPL -> violations.add(
                    LicenseViolation(
                        packageName = importStatement,
                        licenseType = LicenseType.AGPL,
                        reason = "AGPL-licensed package detected: $packagePrefix",
                        suggestion = "AGPL is not permitted. Use a permissive alternative"
                    )
                )
                LicenseType.UNKNOWN_RESTRICTIVE -> warnings.add(
                    "Unknown or potentially restrictive license: $importStatement"
                )
                LicenseType.SAFE -> { /* No action needed */ }
            }
        }

        return ValidationResult(
            isValid = violations.isEmpty(),
            violations = violations,
            warnings = warnings
        )
    }

    /**
     * Validates a list of dependency declarations.
     *
     * @param dependencies List of dependency strings (e.g., "org.example:library:1.0.0")
     * @return ValidationResult with any violations found
     */
    fun validateDependencies(dependencies: List<String>): ValidationResult {
        val violations = mutableListOf<LicenseViolation>()
        val warnings = mutableListOf<String>()

        dependencies.forEach { dependency ->
            // Extract group ID from dependency notation
            val groupId = dependency.split(":").firstOrNull() ?: dependency

            val (licenseType, packagePrefix) = checkPackageLicense(groupId)

            when (licenseType) {
                LicenseType.GPL, LicenseType.LGPL, LicenseType.AGPL -> {
                    violations.add(
                        LicenseViolation(
                            packageName = dependency,
                            licenseType = licenseType,
                            reason = "${licenseType.name}-licensed dependency detected",
                            suggestion = "Remove this dependency and use a permissively licensed alternative"
                        )
                    )
                }
                LicenseType.UNKNOWN_RESTRICTIVE -> {
                    warnings.add("Review license for dependency: $dependency")
                }
                LicenseType.SAFE -> { /* No action needed */ }
            }
        }

        return ValidationResult(
            isValid = violations.isEmpty(),
            violations = violations,
            warnings = warnings
        )
    }

    /**
     * Validates a build.gradle.kts file content.
     *
     * @param buildScript The build script content
     * @return ValidationResult with any violations found
     */
    fun validateBuildScript(buildScript: String): ValidationResult {
        // Extract dependencies from build script
        val dependencyPattern = Regex("""implementation\s*\(\s*["']([^"']+)["']\s*\)""")
        val dependencies = dependencyPattern.findAll(buildScript)
            .map { it.groupValues[1] }
            .toList()

        return validateDependencies(dependencies)
    }

    /**
     * Checks if a package name is safe to use.
     *
     * @param packageName The package name to check
     * @return true if the package is known to be safe
     */
    fun isPackageSafe(packageName: String): Boolean {
        return safePackages.any { packageName.startsWith(it) }
    }

    /**
     * Checks if a package name is restricted.
     *
     * @param packageName The package name to check
     * @return Pair of license type and the matching prefix (if restricted)
     */
    fun checkPackageLicense(packageName: String): Pair<LicenseType, String?> {
        // Check against restricted packages
        for ((prefix, licenseType) in restrictedPackages) {
            if (packageName.startsWith(prefix)) {
                return Pair(licenseType, prefix)
            }
        }

        // Check if it's a known safe package
        if (isPackageSafe(packageName)) {
            return Pair(LicenseType.SAFE, null)
        }

        // Unknown - assume safe but could add warning
        return Pair(LicenseType.SAFE, null)
    }

    /**
     * Extracts import statements from code.
     */
    private fun extractImports(code: String): List<String> {
        val importPattern = Regex("""import\s+([\w.]+)""")
        return importPattern.findAll(code)
            .map { it.groupValues[1] }
            .toList()
    }

    /**
     * Gets a list of safe alternatives for common restricted packages.
     *
     * @param restrictedPackage The restricted package name
     * @return List of suggested safe alternatives
     */
    fun getSafeAlternatives(restrictedPackage: String): List<String> {
        return when {
            restrictedPackage.contains("mysql") -> listOf(
                "org.postgresql:postgresql (PostgreSQL - BSD)",
                "com.h2database:h2 (H2 - MPL 2.0)",
                "org.xerial:sqlite-jdbc (SQLite - Apache 2.0)"
            )
            restrictedPackage.contains("hibernate.ogm") -> listOf(
                "org.jetbrains.exposed:exposed (Exposed - Apache 2.0)",
                "com.squareup.sqldelight (SQLDelight - Apache 2.0)"
            )
            restrictedPackage.contains("logback") -> listOf(
                "org.slf4j:slf4j-simple (SLF4J Simple - MIT)",
                "io.github.microutils:kotlin-logging (kotlin-logging - Apache 2.0)"
            )
            restrictedPackage.contains("neo4j") -> listOf(
                "Consider using Neo4j via API (Neo4j AuraDB)",
                "org.apache.tinkerpop:gremlin (Apache 2.0)"
            )
            else -> listOf(
                "Search for alternatives at https://mvnrepository.com with Apache 2.0 or MIT license"
            )
        }
    }

    /**
     * Generates a report of all license checks.
     *
     * @param result The validation result
     * @return Formatted report string
     */
    fun generateReport(result: ValidationResult): String {
        return buildString {
            appendLine("=== License Validation Report ===")
            appendLine()

            if (result.isValid) {
                appendLine("✓ All dependencies are compliant with constitution requirements")
            } else {
                appendLine("✗ License violations detected!")
                appendLine()
                appendLine("Violations:")
                result.violations.forEach { violation ->
                    appendLine("  - ${violation.packageName}")
                    appendLine("    License: ${violation.licenseType}")
                    appendLine("    Reason: ${violation.reason}")
                    violation.suggestion?.let {
                        appendLine("    Suggestion: $it")
                    }
                    val alternatives = getSafeAlternatives(violation.packageName)
                    if (alternatives.isNotEmpty()) {
                        appendLine("    Alternatives:")
                        alternatives.forEach { alt ->
                            appendLine("      • $alt")
                        }
                    }
                    appendLine()
                }
            }

            if (result.warnings.isNotEmpty()) {
                appendLine("Warnings:")
                result.warnings.forEach { warning ->
                    appendLine("  ⚠ $warning")
                }
            }

            appendLine()
            appendLine("Constitution requirement: NO GPL/LGPL/AGPL dependencies allowed")
        }
    }
}
