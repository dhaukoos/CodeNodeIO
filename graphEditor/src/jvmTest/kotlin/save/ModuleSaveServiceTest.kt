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

    // ========== T045: Preserve Existing ProcessingLogic Files on Re-save ==========

    @Test
    fun `T045 - re-save preserves user implementation in ProcessingLogic files`() {
        // Given
        val flowGraph = createTestFlowGraph("IncrementalTest")
        val saveService = ModuleSaveService()

        // First save - creates stub
        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        // User implements the ProcessingLogic
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/generated/incrementaltest")
        val stubFile = File(packageDir, "ProcessorComponent.kt")
        val userImplementation = """
            package io.codenode.generated.incrementaltest

            import io.codenode.fbpdsl.model.ProcessingLogic
            import io.codenode.fbpdsl.model.InformationPacket

            class ProcessorComponent : ProcessingLogic {
                // USER IMPLEMENTED CODE - MUST BE PRESERVED
                private val customField = "user data"

                override suspend operator fun invoke(
                    inputs: Map<String, InformationPacket<*>>
                ): Map<String, InformationPacket<*>> {
                    // Custom implementation by user
                    val processed = inputs["input"]?.payload.toString().uppercase()
                    return mapOf("output" to InformationPacket(processed))
                }
            }
        """.trimIndent()
        stubFile.writeText(userImplementation)

        // Re-save the same FlowGraph
        val result2 = saveService.saveModule(flowGraph, tempDir)

        // Then - user implementation should be preserved
        assertTrue(result2.success)
        val content = stubFile.readText()
        assertTrue(content.contains("USER IMPLEMENTED CODE - MUST BE PRESERVED"),
            "User implementation should be preserved on re-save")
        assertTrue(content.contains("customField"),
            "User's custom fields should be preserved")
        assertTrue(content.contains("uppercase()"),
            "User's custom logic should be preserved")
    }

    @Test
    fun `T045 - re-save preserves multiple ProcessingLogic files`() {
        // Given
        val node1 = createTestCodeNode("node1", "Generator", CodeNodeType.GENERATOR)
        val node2 = createTestCodeNode("node2", "Processor", CodeNodeType.TRANSFORMER)
        val flowGraph = createTestFlowGraph("MultiNode", listOf(node1, node2))
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        // User implements both files
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/generated/multinode")
        val file1 = File(packageDir, "GeneratorComponent.kt")
        val file2 = File(packageDir, "ProcessorComponent.kt")

        val impl1 = "// GENERATOR IMPL v1.0\n" + file1.readText()
        val impl2 = "// PROCESSOR IMPL v2.0\n" + file2.readText()
        file1.writeText(impl1)
        file2.writeText(impl2)

        // Re-save
        val result2 = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result2.success)
        assertTrue(file1.readText().contains("GENERATOR IMPL v1.0"),
            "GeneratorComponent should be preserved")
        assertTrue(file2.readText().contains("PROCESSOR IMPL v2.0"),
            "ProcessorComponent should be preserved")
    }

    // ========== T046: Generate Stubs Only for NEW Nodes ==========

    @Test
    fun `T046 - re-save generates stub only for new node`() {
        // Given - initial flow with one node
        val node1 = createTestCodeNode("node1", "ExistingNode", CodeNodeType.TRANSFORMER)
        val flowGraph1 = createTestFlowGraph("ExpandingFlow", listOf(node1))
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        // User implements the first node
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/generated/expandingflow")
        val existingStub = File(packageDir, "ExistingNodeComponent.kt")
        val userImpl = "// USER IMPLEMENTED\n" + existingStub.readText()
        existingStub.writeText(userImpl)

        // Add a new node to the flow
        val node2 = createTestCodeNode("node2", "NewNode", CodeNodeType.SINK)
        val flowGraph2 = createTestFlowGraph("ExpandingFlow", listOf(node1, node2))

        // Re-save with new node
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        // Then
        assertTrue(result2.success)

        // Existing node's implementation preserved
        assertTrue(existingStub.readText().contains("USER IMPLEMENTED"),
            "Existing node implementation should be preserved")

        // New node gets a stub
        val newStub = File(packageDir, "NewNodeComponent.kt")
        assertTrue(newStub.exists(), "New node should get a stub file")
        assertTrue(newStub.readText().contains("TODO"),
            "New node stub should have TODO placeholder")
    }

    @Test
    fun `T046 - filesCreated only includes newly created files on re-save`() {
        // Given
        val node1 = createTestCodeNode("node1", "FirstNode", CodeNodeType.GENERATOR)
        val flowGraph1 = createTestFlowGraph("TrackFiles", listOf(node1))
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)
        assertTrue(result1.filesCreated.any { it.contains("FirstNodeComponent.kt") },
            "First save should report FirstNodeComponent as created")

        // Add second node
        val node2 = createTestCodeNode("node2", "SecondNode", CodeNodeType.SINK)
        val flowGraph2 = createTestFlowGraph("TrackFiles", listOf(node1, node2))

        // Re-save
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        // Then - only new file should be in filesCreated
        assertTrue(result2.success)
        assertFalse(result2.filesCreated.any { it.contains("FirstNodeComponent.kt") },
            "Existing FirstNodeComponent should not be in filesCreated")
        assertTrue(result2.filesCreated.any { it.contains("SecondNodeComponent.kt") },
            "New SecondNodeComponent should be in filesCreated")
    }

    // ========== T047: Warning When Node Removed (Orphaned ProcessingLogic) ==========

    @Test
    fun `T047 - re-save warns about orphaned ProcessingLogic when node removed`() {
        // Given - initial flow with two nodes
        val node1 = createTestCodeNode("node1", "KeptNode", CodeNodeType.GENERATOR)
        val node2 = createTestCodeNode("node2", "RemovedNode", CodeNodeType.SINK)
        val flowGraph1 = createTestFlowGraph("ShrinkingFlow", listOf(node1, node2))
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        // User implements both
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/generated/shrinkingflow")
        val keptFile = File(packageDir, "KeptNodeComponent.kt")
        val removedFile = File(packageDir, "RemovedNodeComponent.kt")
        keptFile.writeText("// User impl kept\n" + keptFile.readText())
        removedFile.writeText("// User impl removed\n" + removedFile.readText())

        // Remove node2 from flow
        val flowGraph2 = createTestFlowGraph("ShrinkingFlow", listOf(node1))

        // Re-save without node2
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        // Then
        assertTrue(result2.success, "Save should still succeed")
        assertTrue(result2.warnings.isNotEmpty(), "Should have warnings about orphaned file")
        assertTrue(result2.warnings.any { it.contains("RemovedNodeComponent") },
            "Warning should mention the orphaned component")
    }

    @Test
    fun `T047 - orphaned ProcessingLogic file is not deleted`() {
        // Given
        val node1 = createTestCodeNode("node1", "Active", CodeNodeType.TRANSFORMER)
        val node2 = createTestCodeNode("node2", "ToRemove", CodeNodeType.SINK)
        val flowGraph1 = createTestFlowGraph("SafeDelete", listOf(node1, node2))
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        // Implement the file that will become orphaned
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/generated/safedelete")
        val orphanedFile = File(packageDir, "ToRemoveComponent.kt")
        orphanedFile.writeText("// IMPORTANT USER CODE - DO NOT DELETE\n" + orphanedFile.readText())

        // Remove node from flow
        val flowGraph2 = createTestFlowGraph("SafeDelete", listOf(node1))

        // Re-save
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        // Then - orphaned file should NOT be deleted
        assertTrue(result2.success)
        assertTrue(orphanedFile.exists(), "Orphaned file should not be automatically deleted")
        assertTrue(orphanedFile.readText().contains("IMPORTANT USER CODE"),
            "Orphaned file content should be preserved")
    }

    // ========== T048: Update .flow.kt While Preserving Structure ==========

    @Test
    fun `T048 - re-save updates flow kt file with new node`() {
        // Given
        val node1 = createTestCodeNode("node1", "Original", CodeNodeType.GENERATOR)
        val flowGraph1 = createTestFlowGraph("UpdateFlow", listOf(node1))
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/generated/updateflow")
        val flowKtFile = File(packageDir, "UpdateFlow.flow.kt")
        val originalContent = flowKtFile.readText()
        assertTrue(originalContent.contains("Original"),
            "Original flow.kt should contain Original node")

        // Add new node
        val node2 = createTestCodeNode("node2", "Added", CodeNodeType.SINK)
        val flowGraph2 = createTestFlowGraph("UpdateFlow", listOf(node1, node2))

        // Re-save
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        // Then
        assertTrue(result2.success)
        val updatedContent = flowKtFile.readText()
        assertTrue(updatedContent.contains("Original"),
            "Updated flow.kt should still contain Original node")
        assertTrue(updatedContent.contains("Added"),
            "Updated flow.kt should contain new Added node")
    }

    @Test
    fun `T048 - re-save updates flow kt when node position changes`() {
        // Given
        val node1 = createTestCodeNode("node1", "Movable")
        val flowGraph1 = createTestFlowGraph("PositionTest", listOf(node1))
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        // Move the node
        val movedNode = node1.copy(position = Node.Position(500.0, 600.0))
        val flowGraph2 = createTestFlowGraph("PositionTest", listOf(movedNode))

        // Re-save
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        // Then
        assertTrue(result2.success)
        val packageDir = File(result2.moduleDir, "src/commonMain/kotlin/io/codenode/generated/positiontest")
        val flowKtFile = File(packageDir, "PositionTest.flow.kt")
        val content = flowKtFile.readText()
        assertTrue(content.contains("500.0") && content.contains("600.0"),
            "flow.kt should reflect new node position")
    }

    @Test
    fun `T048 - re-save updates flow kt when connection added`() {
        // Given - two nodes, no connection initially
        val node1 = createTestCodeNode("node1", "Source", CodeNodeType.GENERATOR)
        val node2 = createTestCodeNode("node2", "Target", CodeNodeType.SINK)
        val flowGraph1 = FlowGraph(
            id = "flow_connect",
            name = "ConnectTest",
            version = "1.0.0",
            rootNodes = listOf(node1, node2),
            connections = emptyList()
        )
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        // Add connection
        val connection = Connection(
            id = "conn_1",
            sourceNodeId = "node1",
            sourcePortId = "node1_output",
            targetNodeId = "node2",
            targetPortId = "node2_input"
        )
        val flowGraph2 = flowGraph1.copy(connections = listOf(connection))

        // Re-save
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        // Then
        assertTrue(result2.success)
        val packageDir = File(result2.moduleDir, "src/commonMain/kotlin/io/codenode/generated/connecttest")
        val flowKtFile = File(packageDir, "ConnectTest.flow.kt")
        val content = flowKtFile.readText()
        assertTrue(content.contains("connect"),
            "flow.kt should contain the new connection")
    }
}
