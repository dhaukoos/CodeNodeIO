/*
 * GenerationFileWriterTest - Tests for writing GenerationResult to disk
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.runner

import java.io.File
import kotlin.test.*

class GenerationFileWriterTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = kotlin.io.path.createTempDirectory("filewriter-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `writes FlowKtGenerator content to flow subdirectory`() {
        val result = GenerationResult(
            generatedFiles = mapOf("FlowKtGenerator" to "package io.test.flow\n// flow content")
        )
        val moduleDir = File(tempDir, "TestModule").apply { mkdirs() }
        val baseSrcDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/testmodule")
        baseSrcDir.mkdirs()

        val writer = GenerationFileWriter()
        val written = writer.write(result, moduleDir, "io.codenode.testmodule", "TestModule")

        assertEquals(1, written.size)
        assertTrue(written[0].contains("flow/TestModule.flow.kt"))
        val file = File(baseSrcDir, "flow/TestModule.flow.kt")
        assertTrue(file.exists(), "FlowKt file should exist in flow/")
        assertTrue(file.readText().contains("flow content"))
    }

    @Test
    fun `writes RuntimeControllerGenerator content to controller subdirectory`() {
        val result = GenerationResult(
            generatedFiles = mapOf("RuntimeControllerGenerator" to "package io.test.controller\n// controller content")
        )
        val moduleDir = File(tempDir, "TestModule").apply { mkdirs() }
        val baseSrcDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/testmodule")
        baseSrcDir.mkdirs()

        val writer = GenerationFileWriter()
        writer.write(result, moduleDir, "io.codenode.testmodule", "TestModule")

        val file = File(baseSrcDir, "controller/TestModuleController.kt")
        assertTrue(file.exists(), "Controller file should exist in controller/")
        assertTrue(file.readText().contains("controller content"))
    }

    @Test
    fun `writes multiple generator outputs to correct locations`() {
        val result = GenerationResult(
            generatedFiles = mapOf(
                "FlowKtGenerator" to "flow content",
                "RuntimeFlowGenerator" to "runtime flow content",
                "RuntimeControllerGenerator" to "controller content",
                "RuntimeViewModelGenerator" to "viewmodel content",
                "UserInterfaceStubGenerator" to "ui content"
            )
        )
        val moduleDir = File(tempDir, "MyModule").apply { mkdirs() }
        val baseSrcDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/mymodule")
        baseSrcDir.mkdirs()

        val writer = GenerationFileWriter()
        val written = writer.write(result, moduleDir, "io.codenode.mymodule", "MyModule")

        assertEquals(5, written.size)
        assertTrue(File(baseSrcDir, "flow/MyModule.flow.kt").exists())
        assertTrue(File(baseSrcDir, "flow/MyModuleFlow.kt").exists())
        assertTrue(File(baseSrcDir, "controller/MyModuleController.kt").exists())
        assertTrue(File(baseSrcDir, "viewmodel/MyModuleViewModel.kt").exists())
        assertTrue(File(baseSrcDir, "userInterface/MyModule.kt").exists())
    }

    @Test
    fun `creates parent directories if needed`() {
        val result = GenerationResult(
            generatedFiles = mapOf("FlowKtGenerator" to "content")
        )
        val moduleDir = File(tempDir, "NewModule").apply { mkdirs() }

        val writer = GenerationFileWriter()
        writer.write(result, moduleDir, "io.codenode.newmodule", "NewModule")

        val file = File(moduleDir, "src/commonMain/kotlin/io/codenode/newmodule/flow/NewModule.flow.kt")
        assertTrue(file.exists())
    }

    @Test
    fun `skips unknown generator IDs`() {
        val result = GenerationResult(
            generatedFiles = mapOf("UnknownGenerator" to "content")
        )
        val moduleDir = File(tempDir, "TestModule").apply { mkdirs() }

        val writer = GenerationFileWriter()
        val written = writer.write(result, moduleDir, "io.codenode.testmodule", "TestModule")

        assertEquals(0, written.size)
    }
}
