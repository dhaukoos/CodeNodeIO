/*
 * UserInterfaceStubGenerator Test
 * Tests for generating Composable stub files for userInterface/ directory
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * Tests for UserInterfaceStubGenerator - generates a @Composable stub file
 * with a basic Column + Text placeholder and ViewModel import.
 */
class UserInterfaceStubGeneratorTest {

    private val generator = UserInterfaceStubGenerator()
    private val uiPackage = "io.codenode.testapp.userInterface"
    private val generatedPackage = "io.codenode.testapp.generated"

    private fun createFlowGraph(name: String = "TestFlow"): FlowGraph {
        return FlowGraph(
            id = "test-flow",
            name = name,
            version = "1.0.0",
            description = "Test flow",
            rootNodes = emptyList(),
            connections = emptyList()
        )
    }

    // ========== Package Declaration ==========

    @Test
    fun `generates package declaration with userInterface subpackage`() {
        val result = generator.generate(createFlowGraph(), uiPackage, generatedPackage)
        assertTrue(result.contains("package io.codenode.testapp.userInterface"))
    }

    // ========== Composable Annotation ==========

    @Test
    fun `generates Composable annotation`() {
        val result = generator.generate(createFlowGraph(), uiPackage, generatedPackage)
        assertTrue(result.contains("@Composable"))
    }

    // ========== Function Name ==========

    @Test
    fun `function name matches module name`() {
        val result = generator.generate(createFlowGraph("StopWatch"), uiPackage, generatedPackage)
        assertTrue(result.contains("fun StopWatch("))
    }

    @Test
    fun `function name uses pascalCase for multi-word names`() {
        val result = generator.generate(createFlowGraph("my cool app"), uiPackage, generatedPackage)
        assertTrue(result.contains("fun Mycoolapp("))
    }

    // ========== ViewModel Import ==========

    @Test
    fun `imports ViewModel from generated package`() {
        val result = generator.generate(createFlowGraph("TestFlow"), uiPackage, generatedPackage)
        assertTrue(result.contains("import io.codenode.testapp.generated.TestFlowViewModel"))
    }

    // ========== Compose Imports ==========

    @Test
    fun `imports Compose foundation layout`() {
        val result = generator.generate(createFlowGraph(), uiPackage, generatedPackage)
        assertTrue(result.contains("import androidx.compose.foundation.layout.*"))
    }

    @Test
    fun `imports Compose material3 Text`() {
        val result = generator.generate(createFlowGraph(), uiPackage, generatedPackage)
        assertTrue(result.contains("import androidx.compose.material3.Text"))
    }

    @Test
    fun `imports Compose runtime`() {
        val result = generator.generate(createFlowGraph(), uiPackage, generatedPackage)
        assertTrue(result.contains("import androidx.compose.runtime.*"))
    }

    @Test
    fun `imports Compose ui Alignment`() {
        val result = generator.generate(createFlowGraph(), uiPackage, generatedPackage)
        assertTrue(result.contains("import androidx.compose.ui.Alignment"))
    }

    @Test
    fun `imports Compose ui Modifier`() {
        val result = generator.generate(createFlowGraph(), uiPackage, generatedPackage)
        assertTrue(result.contains("import androidx.compose.ui.Modifier"))
    }

    // ========== Function Signature ==========

    @Test
    fun `function takes viewModel parameter`() {
        val result = generator.generate(createFlowGraph("TestFlow"), uiPackage, generatedPackage)
        assertTrue(result.contains("viewModel: TestFlowViewModel"))
    }

    @Test
    fun `function takes modifier parameter with default`() {
        val result = generator.generate(createFlowGraph(), uiPackage, generatedPackage)
        assertTrue(result.contains("modifier: Modifier = Modifier"))
    }

    // ========== Placeholder Content ==========

    @Test
    fun `contains Column with horizontalAlignment`() {
        val result = generator.generate(createFlowGraph(), uiPackage, generatedPackage)
        assertTrue(result.contains("Column("))
        assertTrue(result.contains("horizontalAlignment = Alignment.CenterHorizontally"))
    }

    @Test
    fun `contains Text placeholder with module name`() {
        val result = generator.generate(createFlowGraph("StopWatch"), uiPackage, generatedPackage)
        assertTrue(result.contains("Text(\"StopWatch\")"))
    }

    @Test
    fun `contains TODO comment`() {
        val result = generator.generate(createFlowGraph("StopWatch"), uiPackage, generatedPackage)
        assertTrue(result.contains("// TODO: Implement StopWatch UI"))
    }

    // ========== File Name ==========

    @Test
    fun `stub file name matches module name`() {
        val fileName = generator.getStubFileName(createFlowGraph("StopWatch"))
        assertEquals("StopWatch.kt", fileName)
    }

    @Test
    fun `stub file name uses pascalCase`() {
        val fileName = generator.getStubFileName(createFlowGraph("my cool app"))
        assertEquals("Mycoolapp.kt", fileName)
    }

    // ========== Header ==========

    @Test
    fun `generates header with module name`() {
        val result = generator.generate(createFlowGraph("StopWatch"), uiPackage, generatedPackage)
        assertTrue(result.contains("StopWatch Composable"))
        assertTrue(result.contains("Generated by CodeNodeIO"))
    }
}
