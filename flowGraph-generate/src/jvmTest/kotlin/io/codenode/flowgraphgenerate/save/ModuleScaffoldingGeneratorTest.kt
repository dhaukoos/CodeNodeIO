/*
 * ModuleScaffoldingGeneratorTest - Tests for standalone module scaffolding
 * License: Apache 2.0
 */

package io.codenode.flowgraphgenerate.save

import io.codenode.fbpdsl.model.FlowGraph
import java.io.File
import kotlin.test.*

class ModuleScaffoldingGeneratorTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = kotlin.io.path.createTempDirectory("scaffolding-test").toFile()
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // T001: Module directory creation
    @Test
    fun `generate creates module directory with correct name`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("StopWatch", tempDir)

        assertTrue(result.moduleDir.exists())
        assertTrue(result.moduleDir.isDirectory)
        assertEquals("StopWatch", result.moduleDir.name)
    }

    @Test
    fun `generate creates module in output directory`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("MyModule", tempDir)

        assertEquals(File(tempDir, "MyModule").absolutePath, result.moduleDir.absolutePath)
    }

    // T002: Source set directories
    @Test
    fun `generate creates commonMain kotlin directory with package path`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val commonMainDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule")
        assertTrue(commonMainDir.exists(), "commonMain/kotlin package dir should exist")
    }

    @Test
    fun `generate creates jvmMain kotlin directory`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val jvmMainDir = File(result.moduleDir, "src/jvmMain/kotlin/io/codenode/testmodule")
        assertTrue(jvmMainDir.exists(), "jvmMain/kotlin package dir should exist")
    }

    @Test
    fun `generate creates commonTest kotlin directory`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val commonTestDir = File(result.moduleDir, "src/commonTest/kotlin/io/codenode/testmodule")
        assertTrue(commonTestDir.exists(), "commonTest/kotlin package dir should exist")
    }

    // T003: Subdirectories
    @Test
    fun `generate creates flow subdirectory`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val flowDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/flow")
        assertTrue(flowDir.exists(), "flow/ subdirectory should exist")
    }

    @Test
    fun `generate creates controller subdirectory`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val controllerDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/controller")
        assertTrue(controllerDir.exists(), "controller/ subdirectory should exist")
    }

    @Test
    fun `generate creates viewmodel subdirectory`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val viewmodelDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/viewmodel")
        assertTrue(viewmodelDir.exists(), "viewmodel/ subdirectory should exist")
    }

    @Test
    fun `generate creates userInterface subdirectory`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val uiDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/userInterface")
        assertTrue(uiDir.exists(), "userInterface/ subdirectory should exist")
    }

    @Test
    fun `generate creates nodes subdirectory`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val nodesDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/nodes")
        assertTrue(nodesDir.exists(), "nodes/ subdirectory should exist")
    }

    @Test
    fun `generate creates iptypes subdirectory`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val iptypesDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/iptypes")
        assertTrue(iptypesDir.exists(), "iptypes/ subdirectory should exist")
    }

    // T004: Platform-specific source sets
    @Test
    fun `generate creates androidMain when Android target specified`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir,
            targetPlatforms = listOf(FlowGraph.TargetPlatform.KMP_ANDROID))

        val androidDir = File(result.moduleDir, "src/androidMain/kotlin/io/codenode/testmodule")
        assertTrue(androidDir.exists(), "androidMain should exist when Android targeted")
    }

    @Test
    fun `generate creates iosMain when iOS target specified`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir,
            targetPlatforms = listOf(FlowGraph.TargetPlatform.KMP_IOS))

        val iosDir = File(result.moduleDir, "src/iosMain/kotlin/io/codenode/testmodule")
        assertTrue(iosDir.exists(), "iosMain should exist when iOS targeted")
    }

    @Test
    fun `generate does not create androidMain when Android not targeted`() {
        val generator = ModuleScaffoldingGenerator()
        generator.generate("TestModule", tempDir, targetPlatforms = emptyList())

        val androidDir = File(tempDir, "TestModule/src/androidMain")
        assertFalse(androidDir.exists(), "androidMain should not exist when Android not targeted")
    }

    // T005: Gradle files with write-once semantics
    @Test
    fun `generate creates build gradle kts`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val buildFile = File(result.moduleDir, "build.gradle.kts")
        assertTrue(buildFile.exists(), "build.gradle.kts should be created")
        assertTrue(buildFile.readText().isNotBlank())
    }

    @Test
    fun `generate creates settings gradle kts`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        val settingsFile = File(result.moduleDir, "settings.gradle.kts")
        assertTrue(settingsFile.exists(), "settings.gradle.kts should be created")
        assertTrue(settingsFile.readText().contains("rootProject.name"))
    }

    @Test
    fun `generate does not overwrite existing build gradle kts`() {
        val generator = ModuleScaffoldingGenerator()

        val result1 = generator.generate("TestModule", tempDir)
        val buildFile = File(result1.moduleDir, "build.gradle.kts")
        buildFile.writeText("// custom content")

        val result2 = generator.generate("TestModule", tempDir)
        assertEquals("// custom content", File(result2.moduleDir, "build.gradle.kts").readText(),
            "Existing build.gradle.kts should not be overwritten")
    }

    @Test
    fun `generate does not overwrite existing settings gradle kts`() {
        val generator = ModuleScaffoldingGenerator()

        val result1 = generator.generate("TestModule", tempDir)
        val settingsFile = File(result1.moduleDir, "settings.gradle.kts")
        settingsFile.writeText("// custom settings")

        val result2 = generator.generate("TestModule", tempDir)
        assertEquals("// custom settings", File(result2.moduleDir, "settings.gradle.kts").readText(),
            "Existing settings.gradle.kts should not be overwritten")
    }

    // T006: ScaffoldingResult
    @Test
    fun `generate returns correct basePackage`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("StopWatch", tempDir)

        assertEquals("io.codenode.stopwatch", result.basePackage)
    }

    @Test
    fun `generate returns correct subpackage paths`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("StopWatch", tempDir)

        assertEquals("io.codenode.stopwatch.flow", result.flowPackage)
        assertEquals("io.codenode.stopwatch.controller", result.controllerPackage)
        assertEquals("io.codenode.stopwatch.viewmodel", result.viewModelPackage)
        assertEquals("io.codenode.stopwatch.userInterface", result.userInterfacePackage)
    }

    @Test
    fun `generate reports filesCreated on first run`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("TestModule", tempDir)

        assertTrue(result.filesCreated.contains("build.gradle.kts"))
        assertTrue(result.filesCreated.contains("settings.gradle.kts"))
        assertEquals(2, result.filesCreated.size)
    }

    @Test
    fun `generate reports empty filesCreated when files already exist`() {
        val generator = ModuleScaffoldingGenerator()
        generator.generate("TestModule", tempDir)

        val result2 = generator.generate("TestModule", tempDir)
        assertTrue(result2.filesCreated.isEmpty(), "No files should be created on re-run")
    }

    @Test
    fun `generate accepts custom package prefix`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("MyApp", tempDir, packagePrefix = "com.example")

        assertEquals("com.example.myapp", result.basePackage)
        val dir = File(result.moduleDir, "src/commonMain/kotlin/com/example/myapp")
        assertTrue(dir.exists(), "Custom package directory should exist")
    }

    @Test
    fun `generate works without FlowGraph dependency`() {
        val generator = ModuleScaffoldingGenerator()
        val result = generator.generate("Standalone", tempDir,
            targetPlatforms = listOf(FlowGraph.TargetPlatform.KMP_ANDROID, FlowGraph.TargetPlatform.KMP_IOS))

        assertTrue(result.moduleDir.exists())
        assertTrue(File(result.moduleDir, "build.gradle.kts").exists())
        assertTrue(File(result.moduleDir, "src/androidMain").exists())
        assertTrue(File(result.moduleDir, "src/iosMain").exists())
    }
}
