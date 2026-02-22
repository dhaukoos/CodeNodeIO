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
 * TDD tests for ModuleSaveService - verifies module creation on save and compile.
 *
 * Save creates: module directory, gradle files, directory structure, .flow.kt at module root.
 * Compile creates: 5 runtime files under generated/, ProcessingLogic stubs under processingLogic/.
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
        val packageName = "io.codenode.testmodule"  // Base package
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir, packageName)

        // Then
        assertTrue(result.success)
        // ProcessingLogic package for user components
        val processingLogicDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/processingLogic")
        assertTrue(processingLogicDir.exists(), "ProcessingLogic package directory should exist")
        assertTrue(processingLogicDir.isDirectory, "ProcessingLogic path should be a directory")
        // Generated package for generated code
        val generatedDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/generated")
        assertTrue(generatedDir.exists(), "Generated package directory should exist")
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
        // Default base package is io.codenode.{lowercase module name}
        // ProcessingLogic package is io.codenode.{modulename}.processingLogic
        val expectedPackageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch/processingLogic")
        assertTrue(expectedPackageDir.exists(),
            "Default processingLogic package directory should be created based on module name")
    }

    // ========== Save writes .flow.kt at module root ==========

    @Test
    fun `saveModule writes flow kt at module root`() {
        // Given
        val node1 = createTestCodeNode("node1", "Original", CodeNodeType.GENERATOR)
        val flowGraph = createTestFlowGraph("UpdateFlow", listOf(node1))
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val flowKtFile = File(result.moduleDir, "UpdateFlow.flow.kt")
        assertTrue(flowKtFile.exists(), ".flow.kt should be at module root")
        val content = flowKtFile.readText()
        assertTrue(content.contains("Original"),
            "flow.kt should contain the node")
    }

    @Test
    fun `re-save updates flow kt at module root`() {
        // Given
        val node1 = createTestCodeNode("node1", "Original", CodeNodeType.GENERATOR)
        val flowGraph1 = createTestFlowGraph("UpdateFlow", listOf(node1))
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val flowKtFile = File(result1.moduleDir, "UpdateFlow.flow.kt")
        assertTrue(flowKtFile.exists(), "First save should create .flow.kt at module root")
        assertTrue(flowKtFile.readText().contains("Original"))

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
    fun `re-save updates flow kt when node position changes`() {
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
        val flowKtFile = File(result2.moduleDir, "PositionTest.flow.kt")
        val content = flowKtFile.readText()
        assertTrue(content.contains("500.0") && content.contains("600.0"),
            "flow.kt should reflect new node position")
    }

    @Test
    fun `re-save updates flow kt when connection added`() {
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
        val flowKtFile = File(result2.moduleDir, "ConnectTest.flow.kt")
        val content = flowKtFile.readText()
        assertTrue(content.contains("connect"),
            "flow.kt should contain the new connection")
    }

    @Test
    fun `saveModule does not generate runtime files`() {
        // Given
        val flowGraph = createTestFlowGraph("SaveOnly")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val generatedDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/saveonly/generated")
        // Generated directory exists (created by directory structure) but should be empty
        val generatedFiles = generatedDir.listFiles()?.filter { it.isFile } ?: emptyList()
        assertTrue(generatedFiles.isEmpty(),
            "saveModule should not generate runtime files")
    }

    @Test
    fun `saveModule does not generate ProcessingLogic stubs`() {
        // Given
        val flowGraph = createTestFlowGraph("SaveOnly")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.saveModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val processingLogicDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/saveonly/processingLogic")
        val stubFiles = processingLogicDir.listFiles()?.filter { it.isFile } ?: emptyList()
        assertTrue(stubFiles.isEmpty(),
            "saveModule should not generate ProcessingLogic stubs")
    }

    // ========== T036/T037: ProcessingLogic Stub Generation (via compileModule) ==========

    @Test
    fun `T036 - compileModule generates ProcessingLogic stub for CodeNode`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.compileModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val packageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/processingLogic")
        val stubFile = File(packageDir, "ProcessorProcessLogic.kt")
        assertTrue(stubFile.exists(), "ProcessingLogic stub should be created for CodeNode")
    }

    @Test
    fun `T036 - stub file contains ProcessingLogic implementation`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // When
        val result = saveService.compileModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val packageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/processingLogic")
        val stubFile = File(packageDir, "ProcessorProcessLogic.kt")
        val content = stubFile.readText()
        assertTrue(content.contains("Tick"), "Stub should contain tick type alias reference")
        assertTrue(content.contains("TODO"), "Stub should have TODO placeholder")
    }

    @Test
    fun `T037 - compileModule generates stub for each CodeNode`() {
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
        val result = saveService.compileModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val packageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/multinodetest/processingLogic")
        val stub1 = File(packageDir, "FirstNodeProcessLogic.kt")
        val stub2 = File(packageDir, "SecondNodeProcessLogic.kt")
        assertTrue(stub1.exists(), "Stub for FirstNode should be created")
        assertTrue(stub2.exists(), "Stub for SecondNode should be created")
    }

    @Test
    fun `T037 - compileModule does not overwrite existing stub files`() {
        // Given
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // First compile
        val result1 = saveService.compileModule(flowGraph, tempDir)
        assertTrue(result1.success)

        // Modify the stub file
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/processingLogic")
        val stubFile = File(packageDir, "ProcessorProcessLogic.kt")
        val userImplementation = "// USER IMPLEMENTATION - DO NOT OVERWRITE\n" + stubFile.readText()
        stubFile.writeText(userImplementation)

        // Second compile
        val result2 = saveService.compileModule(flowGraph, tempDir)

        // Then
        assertTrue(result2.success)
        val content = stubFile.readText()
        assertTrue(content.startsWith("// USER IMPLEMENTATION"),
            "Existing stub file should not be overwritten")
    }

    // ========== T045: Preserve Existing ProcessingLogic Files on Re-compile ==========

    @Test
    fun `T045 - re-compile preserves user implementation in ProcessingLogic files`() {
        // Given
        val flowGraph = createTestFlowGraph("IncrementalTest")
        val saveService = ModuleSaveService()

        // First compile - creates stub
        val result1 = saveService.compileModule(flowGraph, tempDir)
        assertTrue(result1.success)

        // User implements the ProcessingLogic
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/incrementaltest/processingLogic")
        val stubFile = File(packageDir, "ProcessorProcessLogic.kt")
        val userImplementation = """
            package io.codenode.incrementaltest.processingLogic

            import io.codenode.fbpdsl.runtime.TransformerTickBlock

            class ProcessorComponent {
                // USER IMPLEMENTED CODE - MUST BE PRESERVED
                private val customField = "user data"
            }
        """.trimIndent()
        stubFile.writeText(userImplementation)

        // Re-compile the same FlowGraph
        val result2 = saveService.compileModule(flowGraph, tempDir)

        // Then - user implementation should be preserved
        assertTrue(result2.success)
        val content = stubFile.readText()
        assertTrue(content.contains("USER IMPLEMENTED CODE - MUST BE PRESERVED"),
            "User implementation should be preserved on re-compile")
        assertTrue(content.contains("customField"),
            "User's custom fields should be preserved")
    }

    @Test
    fun `T045 - re-compile preserves multiple ProcessingLogic files`() {
        // Given
        val node1 = createTestCodeNode("node1", "Generator", CodeNodeType.GENERATOR)
        val node2 = createTestCodeNode("node2", "Processor", CodeNodeType.TRANSFORMER)
        val flowGraph = createTestFlowGraph("MultiNode", listOf(node1, node2))
        val saveService = ModuleSaveService()

        // First compile
        val result1 = saveService.compileModule(flowGraph, tempDir)
        assertTrue(result1.success)

        // User implements both files
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/multinode/processingLogic")
        val file1 = File(packageDir, "GeneratorProcessLogic.kt")
        val file2 = File(packageDir, "ProcessorProcessLogic.kt")

        val impl1 = "// GENERATOR IMPL v1.0\n" + file1.readText()
        val impl2 = "// PROCESSOR IMPL v2.0\n" + file2.readText()
        file1.writeText(impl1)
        file2.writeText(impl2)

        // Re-compile
        val result2 = saveService.compileModule(flowGraph, tempDir)

        // Then
        assertTrue(result2.success)
        assertTrue(file1.readText().contains("GENERATOR IMPL v1.0"),
            "GeneratorProcessLogic should be preserved")
        assertTrue(file2.readText().contains("PROCESSOR IMPL v2.0"),
            "ProcessorProcessLogic should be preserved")
    }

    // ========== T046: Generate Stubs Only for NEW Nodes ==========

    @Test
    fun `T046 - re-compile generates stub only for new node`() {
        // Given - initial flow with one node
        val node1 = createTestCodeNode("node1", "ExistingNode", CodeNodeType.TRANSFORMER)
        val flowGraph1 = createTestFlowGraph("ExpandingFlow", listOf(node1))
        val saveService = ModuleSaveService()

        // First compile
        val result1 = saveService.compileModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        // User implements the first node
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/expandingflow/processingLogic")
        val existingStub = File(packageDir, "ExistingNodeProcessLogic.kt")
        val userImpl = "// USER IMPLEMENTED\n" + existingStub.readText()
        existingStub.writeText(userImpl)

        // Add a new node to the flow
        val node2 = createTestCodeNode("node2", "NewNode", CodeNodeType.SINK)
        val flowGraph2 = createTestFlowGraph("ExpandingFlow", listOf(node1, node2))

        // Re-compile with new node
        val result2 = saveService.compileModule(flowGraph2, tempDir)

        // Then
        assertTrue(result2.success)

        // Existing node's implementation preserved
        assertTrue(existingStub.readText().contains("USER IMPLEMENTED"),
            "Existing node implementation should be preserved")

        // New node gets a stub
        val newStub = File(packageDir, "NewNodeProcessLogic.kt")
        assertTrue(newStub.exists(), "New node should get a stub file")
        assertTrue(newStub.readText().contains("TODO"),
            "New node stub should have TODO placeholder")
    }

    @Test
    fun `T046 - filesCreated only includes newly created files on re-compile`() {
        // Given
        val node1 = createTestCodeNode("node1", "FirstNode", CodeNodeType.GENERATOR)
        val flowGraph1 = createTestFlowGraph("TrackFiles", listOf(node1))
        val saveService = ModuleSaveService()

        // First compile
        val result1 = saveService.compileModule(flowGraph1, tempDir)
        assertTrue(result1.success)
        assertTrue(result1.filesCreated.any { it.contains("FirstNodeProcessLogic.kt") },
            "First compile should report FirstNodeProcessLogic as created")

        // Add second node
        val node2 = createTestCodeNode("node2", "SecondNode", CodeNodeType.SINK)
        val flowGraph2 = createTestFlowGraph("TrackFiles", listOf(node1, node2))

        // Re-compile
        val result2 = saveService.compileModule(flowGraph2, tempDir)

        // Then - only new file should be in filesCreated
        assertTrue(result2.success)
        assertFalse(result2.filesCreated.any { it.contains("FirstNodeProcessLogic.kt") },
            "Existing FirstNodeProcessLogic should not be in filesCreated")
        assertTrue(result2.filesCreated.any { it.contains("SecondNodeProcessLogic.kt") },
            "New SecondNodeProcessLogic should be in filesCreated")
    }

    // ========== T047: Warning When Node Removed (Orphaned ProcessingLogic) ==========

    @Test
    fun `T047 - re-compile warns about orphaned ProcessingLogic when node removed`() {
        // Given - initial flow with two nodes
        val node1 = createTestCodeNode("node1", "KeptNode", CodeNodeType.GENERATOR)
        val node2 = createTestCodeNode("node2", "RemovedNode", CodeNodeType.SINK)
        val flowGraph1 = createTestFlowGraph("ShrinkingFlow", listOf(node1, node2))
        val saveService = ModuleSaveService()

        // First compile
        val result1 = saveService.compileModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        // User implements both
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/shrinkingflow/processingLogic")
        val keptFile = File(packageDir, "KeptNodeProcessLogic.kt")
        val removedFile = File(packageDir, "RemovedNodeProcessLogic.kt")
        keptFile.writeText("// User impl kept\n" + keptFile.readText())
        removedFile.writeText("// User impl removed\n" + removedFile.readText())

        // Remove node2 from flow
        val flowGraph2 = createTestFlowGraph("ShrinkingFlow", listOf(node1))

        // Re-compile without node2
        val result2 = saveService.compileModule(flowGraph2, tempDir)

        // Then
        assertTrue(result2.success, "Compile should still succeed")
        assertTrue(result2.warnings.isNotEmpty(), "Should have warnings about orphaned file")
        assertTrue(result2.warnings.any { it.contains("RemovedNodeProcessLogic") },
            "Warning should mention the orphaned component")
    }

    @Test
    fun `T047 - orphaned ProcessingLogic file is not deleted`() {
        // Given
        val node1 = createTestCodeNode("node1", "Active", CodeNodeType.TRANSFORMER)
        val node2 = createTestCodeNode("node2", "ToRemove", CodeNodeType.SINK)
        val flowGraph1 = createTestFlowGraph("SafeDelete", listOf(node1, node2))
        val saveService = ModuleSaveService()

        // First compile
        val result1 = saveService.compileModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        // Implement the file that will become orphaned
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/safedelete/processingLogic")
        val orphanedFile = File(packageDir, "ToRemoveProcessLogic.kt")
        orphanedFile.writeText("// IMPORTANT USER CODE - DO NOT DELETE\n" + orphanedFile.readText())

        // Remove node from flow
        val flowGraph2 = createTestFlowGraph("SafeDelete", listOf(node1))

        // Re-compile
        val result2 = saveService.compileModule(flowGraph2, tempDir)

        // Then - orphaned file should NOT be deleted
        assertTrue(result2.success)
        assertTrue(orphanedFile.exists(), "Orphaned file should not be automatically deleted")
        assertTrue(orphanedFile.readText().contains("IMPORTANT USER CODE"),
            "Orphaned file content should be preserved")
    }

    // ========== Runtime File Generation (via compileModule) ==========

    @Test
    fun `compileModule creates all 5 runtime files in generated directory`() {
        // Given
        val node1 = CodeNode(
            id = "gen1",
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "gen1_out", name = "value", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "gen1")
            )
        )
        val node2 = CodeNode(
            id = "sink1",
            name = "Display",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(300.0, 100.0),
            inputPorts = listOf(
                Port(id = "sink1_in", name = "data", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "sink1")
            ),
            outputPorts = emptyList()
        )
        val flowGraph = FlowGraph(
            id = "flow_runtime",
            name = "RuntimeTest",
            version = "1.0.0",
            rootNodes = listOf(node1, node2),
            connections = listOf(
                Connection("c1", "gen1", "gen1_out", "sink1", "sink1_in")
            )
        )
        val saveService = ModuleSaveService()

        // When
        val result = saveService.compileModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val generatedDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/runtimetest/generated")

        val expectedFiles = listOf(
            "RuntimeTestFlow.kt",
            "RuntimeTestController.kt",
            "RuntimeTestControllerInterface.kt",
            "RuntimeTestControllerAdapter.kt",
            "RuntimeTestViewModel.kt"
        )
        for (fileName in expectedFiles) {
            val file = File(generatedDir, fileName)
            assertTrue(file.exists(), "$fileName should exist in generated directory")
            assertTrue(file.readText().isNotBlank(), "$fileName should not be empty")
        }
    }

    @Test
    fun `re-compile overwrites existing runtime files`() {
        // Given
        val node1 = CodeNode(
            id = "gen1",
            name = "Source",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "gen1_out", name = "value", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "gen1")
            )
        )
        val flowGraph1 = FlowGraph(
            id = "flow_overwrite",
            name = "OverwriteTest",
            version = "1.0.0",
            rootNodes = listOf(node1)
        )
        val saveService = ModuleSaveService()

        // First compile
        val result1 = saveService.compileModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val generatedDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/overwritetest/generated")
        val flowFile = File(generatedDir, "OverwriteTestFlow.kt")
        val originalContent = flowFile.readText()
        assertTrue(originalContent.contains("Source"), "First compile should reference Source node")

        // Change the flow (rename node)
        val node2 = node1.copy(name = "UpdatedSource")
        val flowGraph2 = flowGraph1.copy(rootNodes = listOf(node2))

        // Re-compile
        val result2 = saveService.compileModule(flowGraph2, tempDir)

        // Then - runtime files should be overwritten with new content
        assertTrue(result2.success)
        val updatedContent = flowFile.readText()
        assertTrue(updatedContent.contains("UpdatedSource"),
            "Runtime file should be overwritten with new content")
    }

    @Test
    fun `filesCreated includes all 5 runtime file paths`() {
        // Given
        val node1 = CodeNode(
            id = "gen1",
            name = "Emitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "gen1_out", name = "value", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "gen1")
            )
        )
        val flowGraph = FlowGraph(
            id = "flow_files",
            name = "FilesTest",
            version = "1.0.0",
            rootNodes = listOf(node1)
        )
        val saveService = ModuleSaveService()

        // When
        val result = saveService.compileModule(flowGraph, tempDir)

        // Then
        assertTrue(result.success)
        val runtimeFileNames = listOf(
            "FilesTestFlow.kt",
            "FilesTestController.kt",
            "FilesTestControllerInterface.kt",
            "FilesTestControllerAdapter.kt",
            "FilesTestViewModel.kt"
        )
        for (fileName in runtimeFileNames) {
            assertTrue(result.filesCreated.any { it.contains(fileName) },
                "filesCreated should include $fileName")
        }
    }
}
