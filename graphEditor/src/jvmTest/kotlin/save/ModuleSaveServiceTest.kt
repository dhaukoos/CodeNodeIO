/*
 * ModuleSaveService Test
 * TDD tests for creating KMP module structure on FlowGraph save
 * License: Apache 2.0
 */

package io.codenode.grapheditor.save

import io.codenode.fbpdsl.model.*
import java.io.File
import kotlin.test.*

/**
 * TDD tests for ModuleSaveService - verifies module creation on save.
 *
 * These tests are written FIRST and should FAIL until ModuleSaveService is implemented.
 *
 * T001: Test for creating module directory on save
 * T002: Test for generating build.gradle.kts with KMP configuration
 * T003: Test for creating src/commonMain/kotlin/{package} directory structure
 * T004: Test for generating settings.gradle.kts
 * T005: Test for deriving module name from FlowGraph name
 */
class ModuleSaveServiceTest {

    // ========== Test Fixtures ==========

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = createTempDir("module-save-test")
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createTestCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER
    ): CodeNode {
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(0.0, 0.0),
            inputPorts = listOf(
                Port(
                    id = "${id}_input",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            ),
            outputPorts = listOf(
                Port(
                    id = "${id}_output",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = id
                )
            )
        )
    }

    private fun createTestFlowGraph(
        name: String = "TestFlow",
        nodes: List<Node> = listOf(createTestCodeNode("node1", "Processor"))
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            description = "Test flow for module save",
            rootNodes = nodes,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }

    // ========== T001: Module Directory Creation ==========

    @Test
    fun `T001 - saveModule creates module directory`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success, "Save should succeed")
        assertNotNull(result.moduleDir, "Module directory should be returned")
        assertTrue(result.moduleDir!!.exists(), "Module directory should exist")
        assertTrue(result.moduleDir!!.isDirectory, "Module path should be a directory")
    }

    @Test
    fun `T001 - saveModule creates module directory with correct name`() {
        // Given
        val flowGraph = createTestFlowGraph("MyCustomFlow")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        assertEquals("MyCustomFlow", result.moduleDir?.name,
            "Module directory should be named after FlowGraph")
    }

    // ========== T002: build.gradle.kts Generation ==========

    @Test
    fun `T002 - saveModule generates build gradle kts`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val buildFile = File(result.moduleDir, "build.gradle.kts")
        assertTrue(buildFile.exists(), "build.gradle.kts should exist")
    }

    @Test
    fun `T002 - build gradle kts contains KMP multiplatform plugin`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        val buildFile = File(result.moduleDir, "build.gradle.kts")
        val content = buildFile.readText()
        assertTrue(content.contains("kotlin(\"multiplatform\")"),
            "build.gradle.kts should contain KMP plugin")
    }

    @Test
    fun `T002 - build gradle kts contains target platforms from FlowGraph`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule").copy(
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS
            )
        )
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        val buildFile = File(result.moduleDir, "build.gradle.kts")
        val content = buildFile.readText()
        assertTrue(content.contains("androidTarget"),
            "build.gradle.kts should configure Android target")
        assertTrue(content.contains("ios") || content.contains("iosX64"),
            "build.gradle.kts should configure iOS target")
    }

    // ========== T003: Source Directory Structure ==========

    @Test
    fun `T003 - saveModule creates commonMain kotlin directory`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val commonMainDir = File(result.moduleDir, "src/commonMain/kotlin")
        assertTrue(commonMainDir.exists(), "src/commonMain/kotlin should exist")
        assertTrue(commonMainDir.isDirectory, "src/commonMain/kotlin should be a directory")
    }

    @Test
    fun `T003 - saveModule creates package directory structure`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val packageName = "io.codenode.generated.testmodule"
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir, packageName)

        // Then
        assertTrue(result.success)
        val packageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/generated/testmodule")
        assertTrue(packageDir.exists(), "Package directory should exist")
        assertTrue(packageDir.isDirectory, "Package path should be a directory")
    }

    @Test
    fun `T003 - saveModule creates commonTest kotlin directory`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val commonTestDir = File(result.moduleDir, "src/commonTest/kotlin")
        assertTrue(commonTestDir.exists(), "src/commonTest/kotlin should exist")
    }

    // ========== T004: settings.gradle.kts Generation ==========

    @Test
    fun `T004 - saveModule generates settings gradle kts`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val settingsFile = File(result.moduleDir, "settings.gradle.kts")
        assertTrue(settingsFile.exists(), "settings.gradle.kts should exist")
    }

    @Test
    fun `T004 - settings gradle kts contains rootProject name`() {
        // Given
        val flowGraph = createTestFlowGraph("MyModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        val settingsFile = File(result.moduleDir, "settings.gradle.kts")
        val content = settingsFile.readText()
        assertTrue(content.contains("rootProject.name"),
            "settings.gradle.kts should set rootProject.name")
        assertTrue(content.contains("MyModule"),
            "settings.gradle.kts should reference module name")
    }

    @Test
    fun `T004 - settings gradle kts contains plugin repositories`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        val settingsFile = File(result.moduleDir, "settings.gradle.kts")
        val content = settingsFile.readText()
        assertTrue(content.contains("pluginManagement"),
            "settings.gradle.kts should have pluginManagement block")
        assertTrue(content.contains("mavenCentral()"),
            "settings.gradle.kts should include mavenCentral repository")
    }

    // ========== T005: Module Name Derivation ==========

    @Test
    fun `T005 - module name derived from FlowGraph name`() {
        // Given
        val flowGraph = createTestFlowGraph("UserAuthentication")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        assertEquals("UserAuthentication", result.moduleDir?.name)
    }

    @Test
    fun `T005 - module name handles spaces in FlowGraph name`() {
        // Given
        val flowGraph = createTestFlowGraph("User Authentication Flow")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        // Module name should not contain spaces
        assertFalse(result.moduleDir?.name?.contains(" ") ?: true,
            "Module name should not contain spaces")
    }

    @Test
    fun `T005 - module name can be overridden`() {
        // Given
        val flowGraph = createTestFlowGraph("SomeFlow")
        val customModuleName = "custom-module-name"
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(
            flowGraph = flowGraph,
            outputDir = tempDir,
            moduleName = customModuleName
        )

        // Then
        assertTrue(result.success)
        assertEquals(customModuleName, result.moduleDir?.name,
            "Module name should match custom override")
    }

    @Test
    fun `T005 - default package name derived from module name`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        // Default package should be io.codenode.generated.{lowercase module name}
        val expectedPackageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/generated/stopwatch")
        assertTrue(expectedPackageDir.exists(),
            "Default package directory should be created based on module name")
    }

    // ========== T036/T037: ProcessingLogic Stub Generation ==========

    @Test
    fun `T036 - saveModule generates ProcessingLogic stub for CodeNode`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val packageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/generated/testmodule")
        val stubFile = File(packageDir, "ProcessorComponent.kt")
        assertTrue(stubFile.exists(), "ProcessingLogic stub should be created for CodeNode")
    }

    @Test
    fun `T036 - stub file contains ProcessingLogic implementation`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val packageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/generated/testmodule")
        val stubFile = File(packageDir, "ProcessorComponent.kt")
        val content = stubFile.readText()
        assertTrue(content.contains(": ProcessingLogic"), "Stub should implement ProcessingLogic")
        assertTrue(content.contains("override suspend operator fun invoke"), "Stub should have invoke method")
    }

    @Test
    fun `T037 - saveModule generates stub for each CodeNode`() {
        // Given
        val node1 = CodeNode(
            id = "node1",
            name = "FirstNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "node1_out",
                    name = "output",
                    direction = Port.Direction.OUTPUT,
                    dataType = String::class,
                    owningNodeId = "node1"
                )
            )
        )
        val node2 = CodeNode(
            id = "node2",
            name = "SecondNode",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(300.0, 100.0),
            inputPorts = listOf(
                Port(
                    id = "node2_in",
                    name = "input",
                    direction = Port.Direction.INPUT,
                    dataType = String::class,
                    owningNodeId = "node2"
                )
            ),
            outputPorts = emptyList()
        )
        val flowGraph = FlowGraph(
            id = "flow_multi",
            name = "MultiNodeTest",
            version = "1.0.0",
            rootNodes = listOf(node1, node2)
        )
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val packageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/generated/multinodetest")
        val stub1 = File(packageDir, "FirstNodeComponent.kt")
        val stub2 = File(packageDir, "SecondNodeComponent.kt")
        assertTrue(stub1.exists(), "Stub for FirstNode should be created")
        assertTrue(stub2.exists(), "Stub for SecondNode should be created")
    }

    @Test
    fun `T037 - saveModule does not overwrite existing stub files`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        // Modify the stub file
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/generated/testmodule")
        val stubFile = File(packageDir, "ProcessorComponent.kt")
        val userImplementation = "// USER IMPLEMENTATION - DO NOT OVERWRITE\n" + stubFile.readText()
        stubFile.writeText(userImplementation)

        // Second save
        val result2 = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result2.success)
        val content = stubFile.readText()
        assertTrue(content.startsWith("// USER IMPLEMENTATION"),
            "Existing stub file should not be overwritten")
    }
}
