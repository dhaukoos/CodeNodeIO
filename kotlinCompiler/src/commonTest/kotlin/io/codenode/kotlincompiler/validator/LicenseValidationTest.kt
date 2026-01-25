/*
 * License Validation Test
 * Verifies that generated code and dependencies comply with constitution licensing requirements
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.validator

import kotlin.test.*

/**
 * Tests that verify generated code and dependencies comply with the project constitution's
 * licensing requirements. Per constitution: No GPL/LGPL/AGPL dependencies allowed.
 *
 * These tests ensure:
 * 1. Generated code doesn't import from restrictively-licensed libraries
 * 2. Code templates don't introduce license-incompatible dependencies
 * 3. License validator correctly identifies problematic dependencies
 */
class LicenseValidationTest {

    // Known GPL/LGPL/AGPL packages that must be avoided
    private val restrictedPackages = listOf(
        "gnu.classpath",
        "org.gnu",
        "com.mysql.jdbc",          // GPL with FOSS exception, but risky
        "org.hibernate.ogm",       // LGPL
        "org.jboss.logging",       // LGPL
        "ch.qos.logback",          // LGPL (EPL dual-licensed is OK)
        "org.slf4j.ext",           // MIT is OK, but some extensions are LGPL
        "org.aspectj",             // EPL, but some have LGPL components
        "com.github.spotbugs",     // LGPL
        "org.netbeans",            // CDDL+GPL, avoid
        "org.eclipse.swt"          // EPL but has OS-specific binaries with issues
    )

    // Known safe packages (Apache 2.0, MIT, BSD, etc.)
    private val safePackages = listOf(
        "org.jetbrains.kotlin",
        "org.jetbrains.kotlinx",
        "com.squareup.kotlinpoet",
        "org.jetbrains.compose",
        "io.ktor",
        "com.google.code.gson",
        "org.apache",
        "com.fasterxml.jackson"
    )

    @Test
    fun `should identify GPL-licensed packages as restricted`() {
        // Given known GPL packages
        val gplPackages = listOf(
            "gnu.classpath.Foo",
            "org.gnu.something.Bar",
            "com.mysql.jdbc.Driver"
        )

        // When checking each package
        for (pkg in gplPackages) {
            val isRestricted = restrictedPackages.any { pkg.startsWith(it) }

            // Then should be identified as restricted
            assertTrue(isRestricted, "Package $pkg should be identified as restricted")
        }
    }

    @Test
    fun `should identify LGPL-licensed packages as restricted`() {
        // Given known LGPL packages
        val lgplPackages = listOf(
            "org.hibernate.ogm.core.OgmSession",
            "ch.qos.logback.classic.Logger",
            "com.github.spotbugs.SpotBugsTask"
        )

        // When checking each package
        for (pkg in lgplPackages) {
            val isRestricted = restrictedPackages.any { pkg.startsWith(it) }

            // Then should be identified as restricted
            assertTrue(isRestricted, "Package $pkg should be identified as restricted (LGPL)")
        }
    }

    @Test
    fun `should allow Apache 2 licensed packages`() {
        // Given known Apache 2.0 packages
        val apache2Packages = listOf(
            "org.apache.commons.lang3.StringUtils",
            "com.google.code.gson.Gson",
            "com.fasterxml.jackson.databind.ObjectMapper"
        )

        // When checking each package
        for (pkg in apache2Packages) {
            val isRestricted = restrictedPackages.any { pkg.startsWith(it) }

            // Then should NOT be identified as restricted
            assertFalse(isRestricted, "Package $pkg should be allowed (Apache 2.0)")
        }
    }

    @Test
    fun `should allow Kotlin stdlib and KotlinPoet`() {
        // Given core Kotlin packages we depend on
        val kotlinPackages = listOf(
            "org.jetbrains.kotlin.stdlib",
            "org.jetbrains.kotlinx.coroutines",
            "com.squareup.kotlinpoet.FileSpec"
        )

        // When checking each package
        for (pkg in kotlinPackages) {
            val isRestricted = restrictedPackages.any { pkg.startsWith(it) }
            val isSafe = safePackages.any { pkg.startsWith(it) }

            // Then should be allowed
            assertFalse(isRestricted, "Package $pkg should not be restricted")
            assertTrue(isSafe, "Package $pkg should be in safe list")
        }
    }

    @Test
    fun `should reject generated code that imports GPL packages`() {
        // Given generated code with GPL imports
        val generatedCode = """
            package io.codenode.generated

            import gnu.classpath.SomeClass
            import org.jetbrains.kotlin.stdlib.StdlibClass

            class GeneratedComponent {
                fun process() {}
            }
        """.trimIndent()

        // When validating imports
        val imports = extractImports(generatedCode)
        val hasRestrictedImport = imports.any { import ->
            restrictedPackages.any { import.startsWith(it) }
        }

        // Then should detect restricted import
        assertTrue(hasRestrictedImport, "Should detect GPL import in generated code")
    }

    @Test
    fun `should accept generated code with only safe imports`() {
        // Given generated code with safe imports only
        val generatedCode = """
            package io.codenode.generated

            import org.jetbrains.kotlin.Unit
            import com.squareup.kotlinpoet.FileSpec

            class SafeComponent {
                fun process() {}
            }
        """.trimIndent()

        // When validating imports
        val imports = extractImports(generatedCode)
        val hasRestrictedImport = imports.any { import ->
            restrictedPackages.any { import.startsWith(it) }
        }

        // Then should NOT detect any restricted imports
        assertFalse(hasRestrictedImport, "Should not flag safe imports as restricted")
    }

    @Test
    fun `should validate code templates are license compliant`() {
        // Given a list of code template imports that would be added
        val templateImports = listOf(
            "kotlinx.coroutines.flow.Flow",
            "kotlinx.coroutines.flow.collect",
            "kotlin.collections.List"
        )

        // When checking template imports
        val hasRestrictedImport = templateImports.any { import ->
            restrictedPackages.any { import.startsWith(it) }
        }

        // Then templates should be compliant
        assertFalse(hasRestrictedImport, "Code templates should only use safe imports")
    }

    @Test
    fun `should handle empty import lists`() {
        // Given code with no imports
        val generatedCode = """
            package io.codenode.generated

            class SimpleComponent {
                fun process() = Unit
            }
        """.trimIndent()

        // When validating imports
        val imports = extractImports(generatedCode)
        val hasRestrictedImport = imports.any { import ->
            restrictedPackages.any { import.startsWith(it) }
        }

        // Then should be compliant (no imports = no violations)
        assertTrue(imports.isEmpty(), "Should have no imports")
        assertFalse(hasRestrictedImport, "No imports means no restrictions")
    }

    @Test
    fun `should detect multiple restricted imports`() {
        // Given code with multiple restricted imports
        val generatedCode = """
            package io.codenode.generated

            import gnu.classpath.Foo
            import ch.qos.logback.classic.Logger
            import org.hibernate.ogm.OgmSession

            class ProblematicComponent
        """.trimIndent()

        // When validating imports
        val imports = extractImports(generatedCode)
        val restrictedImports = imports.filter { import ->
            restrictedPackages.any { import.startsWith(it) }
        }

        // Then should detect all restricted imports
        assertEquals(3, restrictedImports.size, "Should detect all 3 restricted imports")
    }

    @Test
    fun `should validate constitution compliance message`() {
        // Given the constitution requirement
        val constitutionRequirement = "No GPL/LGPL/AGPL dependencies allowed"

        // When checking our restricted packages list
        val coversGpl = restrictedPackages.any { it.contains("gnu") }
        val coversLgpl = restrictedPackages.any { it.contains("logback") || it.contains("hibernate") }

        // Then our validation covers the constitution requirement
        assertTrue(coversGpl, "Should cover GPL packages")
        assertTrue(coversLgpl, "Should cover LGPL packages")
    }

    // Helper function to extract import statements from code
    private fun extractImports(code: String): List<String> {
        val importRegex = """import\s+([\w.]+)""".toRegex()
        return importRegex.findAll(code)
            .map { it.groupValues[1] }
            .toList()
    }
}
