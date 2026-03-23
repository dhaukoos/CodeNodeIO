/*
 * CompilationValidator Test
 * Tests for module validation before compilation
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compilation

import io.codenode.fbpdsl.model.*
import java.io.File
import kotlin.test.*

/**
 * Tests for CompilationValidator - validates module structure before compilation.
 */
class CompilationValidatorTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = createTempDir("compilation-validator-test")
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createTestFlowGraph(name: String = "TestFlow"): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            rootNodes = emptyList()
        )
    }

    private fun setupModuleStructure(
        moduleDir: File,
        packageName: String
    ) {
        val packagePath = packageName.replace(".", "/")
        File(moduleDir, "src/commonMain/kotlin/$packagePath").mkdirs()
        File(moduleDir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("multiplatform")
            }
        """.trimIndent())
    }

    @Test
    fun `validateModule returns valid when structure is correct`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.stopwatch"

        setupModuleStructure(moduleDir, packageName)

        val validator = CompilationValidator()
        val result = validator.validateModule(flowGraph, moduleDir, packageName)

        assertTrue(result.isValid, "Validation should pass when module structure is correct")
        assertTrue(result.errors.isEmpty(), "No errors expected")
    }

    @Test
    fun `validateModule returns invalid when module directory missing`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "NonExistent")
        val packageName = "io.codenode.stopwatch"

        val validator = CompilationValidator()
        val result = validator.validateModule(flowGraph, moduleDir, packageName)

        assertFalse(result.isValid, "Validation should fail when module directory missing")
        assertTrue(result.errors.any { it.contains("does not exist") })
    }

    @Test
    fun `validateModule returns invalid when build gradle missing`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.stopwatch"

        val packagePath = packageName.replace(".", "/")
        File(moduleDir, "src/commonMain/kotlin/$packagePath").mkdirs()

        val validator = CompilationValidator()
        val result = validator.validateModule(flowGraph, moduleDir, packageName)

        assertFalse(result.isValid, "Validation should fail when build.gradle.kts missing")
        assertTrue(result.errors.any { it.contains("build.gradle.kts") })
    }

    @Test
    fun `validateModule returns invalid when source directory missing`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.stopwatch"

        moduleDir.mkdirs()
        File(moduleDir, "build.gradle.kts").writeText("plugins { }")

        val validator = CompilationValidator()
        val result = validator.validateModule(flowGraph, moduleDir, packageName)

        assertFalse(result.isValid, "Validation should fail when source directory missing")
        assertTrue(result.errors.any { it.contains("Source directory not found") })
    }
}
