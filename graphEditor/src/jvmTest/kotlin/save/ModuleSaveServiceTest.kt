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
 * TDD tests for ModuleSaveService - verifies unified saveModule() creates the
 * full module: directory structure, gradle files, .flow.kt, 5 runtime files,
 * ProcessingLogic stubs, StateProperties stubs, and handles orphan deletion.
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

    private fun createStopWatchFlowGraph(): FlowGraph {
        val timerEmitter = CodeNode(
            id = "timer",
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer"),
                Port(id = "timer_min", name = "elapsedMinutes", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val displayReceiver = CodeNode(
            id = "display",
            name = "DisplayReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display"),
                Port(id = "display_min", name = "minutes", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )
        return FlowGraph(
            id = "flow_stopwatch",
            name = "StopWatch4",
            version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = listOf(
                Connection("c1", "timer", "timer_sec", "display", "display_sec"),
                Connection("c2", "timer", "timer_min", "display", "display_min")
            )
        )
    }

    // ========== T001: Module Directory Creation ==========

    @Test
    fun `T001 - saveModule creates module directory`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success, "Save should succeed")
        assertNotNull(result.moduleDir, "Module directory should be returned")
        assertTrue(result.moduleDir!!.exists(), "Module directory should exist")
        assertTrue(result.moduleDir!!.isDirectory, "Module path should be a directory")
    }

    @Test
    fun `T001 - saveModule creates module directory with correct name`() {
        val flowGraph = createTestFlowGraph("MyCustomFlow")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        assertEquals("MyCustomFlow", result.moduleDir?.name,
            "Module directory should be named after FlowGraph")
    }

    // ========== T002: build.gradle.kts Generation ==========

    @Test
    fun `T002 - saveModule generates build gradle kts`() {
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val buildFile = File(result.moduleDir, "build.gradle.kts")
        assertTrue(buildFile.exists(), "build.gradle.kts should exist")
    }

    @Test
    fun `T002 - build gradle kts contains KMP multiplatform plugin`() {
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        val buildFile = File(result.moduleDir, "build.gradle.kts")
        val content = buildFile.readText()
        assertTrue(content.contains("kotlin(\"multiplatform\")"),
            "build.gradle.kts should contain KMP plugin")
    }

    @Test
    fun `T002 - build gradle kts contains target platforms from FlowGraph`() {
        val flowGraph = createTestFlowGraph("TestModule").copy(
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS
            )
        )
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

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
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val commonMainDir = File(result.moduleDir, "src/commonMain/kotlin")
        assertTrue(commonMainDir.exists(), "src/commonMain/kotlin should exist")
        assertTrue(commonMainDir.isDirectory, "src/commonMain/kotlin should be a directory")
    }

    @Test
    fun `T003 - saveModule creates package directory structure`() {
        val flowGraph = createTestFlowGraph("TestModule")
        val packageName = "io.codenode.testmodule"
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir, packageName)

        assertTrue(result.success)
        val processingLogicDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/processingLogic")
        assertTrue(processingLogicDir.exists(), "ProcessingLogic package directory should exist")
        val generatedDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/generated")
        assertTrue(generatedDir.exists(), "Generated package directory should exist")
        val statePropsDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/stateProperties")
        assertTrue(statePropsDir.exists(), "StateProperties package directory should exist")
    }

    @Test
    fun `T003 - saveModule creates commonTest kotlin directory`() {
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val commonTestDir = File(result.moduleDir, "src/commonTest/kotlin")
        assertTrue(commonTestDir.exists(), "src/commonTest/kotlin should exist")
    }

    // ========== T004: settings.gradle.kts Generation ==========

    @Test
    fun `T004 - saveModule generates settings gradle kts`() {
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val settingsFile = File(result.moduleDir, "settings.gradle.kts")
        assertTrue(settingsFile.exists(), "settings.gradle.kts should exist")
    }

    @Test
    fun `T004 - settings gradle kts contains rootProject name`() {
        val flowGraph = createTestFlowGraph("MyModule")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        val settingsFile = File(result.moduleDir, "settings.gradle.kts")
        val content = settingsFile.readText()
        assertTrue(content.contains("rootProject.name"),
            "settings.gradle.kts should set rootProject.name")
        assertTrue(content.contains("MyModule"),
            "settings.gradle.kts should reference module name")
    }

    @Test
    fun `T004 - settings gradle kts contains plugin repositories`() {
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

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
        val flowGraph = createTestFlowGraph("UserAuthentication")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        assertEquals("UserAuthentication", result.moduleDir?.name)
    }

    @Test
    fun `T005 - module name handles spaces in FlowGraph name`() {
        val flowGraph = createTestFlowGraph("User Authentication Flow")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        assertFalse(result.moduleDir?.name?.contains(" ") ?: true,
            "Module name should not contain spaces")
    }

    @Test
    fun `T005 - module name can be overridden`() {
        val flowGraph = createTestFlowGraph("SomeFlow")
        val customModuleName = "custom-module-name"
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(
            flowGraph = flowGraph,
            outputDir = tempDir,
            moduleName = customModuleName
        )

        assertTrue(result.success)
        assertEquals(customModuleName, result.moduleDir?.name,
            "Module name should match custom override")
    }

    @Test
    fun `T005 - default package name derived from module name`() {
        val flowGraph = createTestFlowGraph("StopWatch")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val expectedPackageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch/processingLogic")
        assertTrue(expectedPackageDir.exists(),
            "Default processingLogic package directory should be created based on module name")
    }

    // ========== .flow.kt in source set ==========

    @Test
    fun `saveModule writes flow kt in source set`() {
        val node1 = createTestCodeNode("node1", "Original", CodeNodeType.GENERATOR)
        val flowGraph = createTestFlowGraph("UpdateFlow", listOf(node1))
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val flowKtFile = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/updateflow/UpdateFlow.flow.kt")
        assertTrue(flowKtFile.exists(), ".flow.kt should be in source set")
        val content = flowKtFile.readText()
        assertTrue(content.contains("Original"),
            "flow.kt should contain the node")
    }

    @Test
    fun `re-save updates flow kt in source set`() {
        val node1 = createTestCodeNode("node1", "Original", CodeNodeType.GENERATOR)
        val flowGraph1 = createTestFlowGraph("UpdateFlow", listOf(node1))
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val flowKtFile = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/updateflow/UpdateFlow.flow.kt")
        assertTrue(flowKtFile.exists(), "First save should create .flow.kt in source set")
        assertTrue(flowKtFile.readText().contains("Original"))

        // Add new node
        val node2 = createTestCodeNode("node2", "Added", CodeNodeType.SINK)
        val flowGraph2 = createTestFlowGraph("UpdateFlow", listOf(node1, node2))

        // Re-save
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        val updatedContent = flowKtFile.readText()
        assertTrue(updatedContent.contains("Original"),
            "Updated flow.kt should still contain Original node")
        assertTrue(updatedContent.contains("Added"),
            "Updated flow.kt should contain new Added node")
    }

    @Test
    fun `re-save updates flow kt when node position changes`() {
        val node1 = createTestCodeNode("node1", "Movable")
        val flowGraph1 = createTestFlowGraph("PositionTest", listOf(node1))
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val movedNode = node1.copy(position = Node.Position(500.0, 600.0))
        val flowGraph2 = createTestFlowGraph("PositionTest", listOf(movedNode))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        val flowKtFile = File(result2.moduleDir, "src/commonMain/kotlin/io/codenode/positiontest/PositionTest.flow.kt")
        val content = flowKtFile.readText()
        assertTrue(content.contains("500.0") && content.contains("600.0"),
            "flow.kt should reflect new node position")
    }

    @Test
    fun `re-save updates flow kt when connection added`() {
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

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val connection = Connection(
            id = "conn_1",
            sourceNodeId = "node1",
            sourcePortId = "node1_output",
            targetNodeId = "node2",
            targetPortId = "node2_input"
        )
        val flowGraph2 = flowGraph1.copy(connections = listOf(connection))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        val flowKtFile = File(result2.moduleDir, "src/commonMain/kotlin/io/codenode/connecttest/ConnectTest.flow.kt")
        val content = flowKtFile.readText()
        assertTrue(content.contains("connect"),
            "flow.kt should contain the new connection")
    }

    // ========== Unified saveModule generates runtime files and stubs ==========

    @Test
    fun `saveModule generates all 5 runtime files`() {
        val flowGraph = createTestFlowGraph("FullSave")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val generatedDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/fullsave/generated")
        val expectedFiles = listOf(
            "FullSaveFlow.kt",
            "FullSaveController.kt",
            "FullSaveControllerInterface.kt",
            "FullSaveControllerAdapter.kt",
            "FullSaveViewModel.kt"
        )
        for (fileName in expectedFiles) {
            val file = File(generatedDir, fileName)
            assertTrue(file.exists(), "$fileName should exist in generated directory")
            assertTrue(file.readText().isNotBlank(), "$fileName should not be empty")
        }
    }

    @Test
    fun `saveModule generates ProcessingLogic stubs`() {
        val flowGraph = createTestFlowGraph("StubTest")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val packageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stubtest/processingLogic")
        val stubFile = File(packageDir, "ProcessorProcessLogic.kt")
        assertTrue(stubFile.exists(), "ProcessingLogic stub should be created for CodeNode")
        val content = stubFile.readText()
        assertTrue(content.contains("Tick"), "Stub should contain tick type alias reference")
        assertTrue(content.contains("TODO"), "Stub should have TODO placeholder")
    }

    @Test
    fun `saveModule generates state properties files`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val statePropsDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4/stateProperties")
        assertTrue(File(statePropsDir, "TimerEmitterStateProperties.kt").exists(),
            "TimerEmitterStateProperties.kt should be generated")
        assertTrue(File(statePropsDir, "DisplayReceiverStateProperties.kt").exists(),
            "DisplayReceiverStateProperties.kt should be generated")
    }

    // ========== ProcessingLogic Stub Generation ==========

    @Test
    fun `saveModule generates stub for each CodeNode`() {
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

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val packageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/multinodetest/processingLogic")
        assertTrue(File(packageDir, "FirstNodeProcessLogic.kt").exists(), "Stub for FirstNode should be created")
        assertTrue(File(packageDir, "SecondNodeProcessLogic.kt").exists(), "Stub for SecondNode should be created")
    }

    @Test
    fun `saveModule does not overwrite existing stub files`() {
        val flowGraph = createTestFlowGraph("TestModule")
        val saveService = ModuleSaveService()

        // First save
        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        // Modify the stub file
        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/processingLogic")
        val stubFile = File(packageDir, "ProcessorProcessLogic.kt")
        val userImplementation = "// USER IMPLEMENTATION - DO NOT OVERWRITE\n" + stubFile.readText()
        stubFile.writeText(userImplementation)

        // Re-save
        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        val content = stubFile.readText()
        assertTrue(content.startsWith("// USER IMPLEMENTATION"),
            "Existing stub file should not be overwritten")
    }

    // ========== Preserve Existing Files on Re-save ==========

    @Test
    fun `re-save preserves user implementation in ProcessingLogic files`() {
        val flowGraph = createTestFlowGraph("IncrementalTest")
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

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

        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        val content = stubFile.readText()
        assertTrue(content.contains("USER IMPLEMENTED CODE - MUST BE PRESERVED"),
            "User implementation should be preserved on re-save")
        assertTrue(content.contains("customField"),
            "User's custom fields should be preserved")
    }

    @Test
    fun `re-save preserves multiple ProcessingLogic files`() {
        val node1 = createTestCodeNode("node1", "Generator", CodeNodeType.GENERATOR)
        val node2 = createTestCodeNode("node2", "Processor", CodeNodeType.TRANSFORMER)
        val flowGraph = createTestFlowGraph("MultiNode", listOf(node1, node2))
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/multinode/processingLogic")
        val file1 = File(packageDir, "GeneratorProcessLogic.kt")
        val file2 = File(packageDir, "ProcessorProcessLogic.kt")

        val impl1 = "// GENERATOR IMPL v1.0\n" + file1.readText()
        val impl2 = "// PROCESSOR IMPL v2.0\n" + file2.readText()
        file1.writeText(impl1)
        file2.writeText(impl2)

        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        assertTrue(file1.readText().contains("GENERATOR IMPL v1.0"),
            "GeneratorProcessLogic should be preserved")
        assertTrue(file2.readText().contains("PROCESSOR IMPL v2.0"),
            "ProcessorProcessLogic should be preserved")
    }

    // ========== Generate Stubs Only for NEW Nodes ==========

    @Test
    fun `re-save generates stub only for new node`() {
        val node1 = createTestCodeNode("node1", "ExistingNode", CodeNodeType.TRANSFORMER)
        val flowGraph1 = createTestFlowGraph("ExpandingFlow", listOf(node1))
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/expandingflow/processingLogic")
        val existingStub = File(packageDir, "ExistingNodeProcessLogic.kt")
        val userImpl = "// USER IMPLEMENTED\n" + existingStub.readText()
        existingStub.writeText(userImpl)

        val node2 = createTestCodeNode("node2", "NewNode", CodeNodeType.SINK)
        val flowGraph2 = createTestFlowGraph("ExpandingFlow", listOf(node1, node2))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        assertTrue(existingStub.readText().contains("USER IMPLEMENTED"),
            "Existing node implementation should be preserved")

        val newStub = File(packageDir, "NewNodeProcessLogic.kt")
        assertTrue(newStub.exists(), "New node should get a stub file")
        assertTrue(newStub.readText().contains("TODO"),
            "New node stub should have TODO placeholder")
    }

    @Test
    fun `filesCreated only includes newly created files on re-save`() {
        val node1 = createTestCodeNode("node1", "FirstNode", CodeNodeType.GENERATOR)
        val flowGraph1 = createTestFlowGraph("TrackFiles", listOf(node1))
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)
        assertTrue(result1.filesCreated.any { it.contains("FirstNodeProcessLogic.kt") },
            "First save should report FirstNodeProcessLogic as created")

        val node2 = createTestCodeNode("node2", "SecondNode", CodeNodeType.SINK)
        val flowGraph2 = createTestFlowGraph("TrackFiles", listOf(node1, node2))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        assertFalse(result2.filesCreated.any { it.contains("FirstNodeProcessLogic.kt") },
            "Existing FirstNodeProcessLogic should not be in filesCreated")
        assertTrue(result2.filesCreated.any { it.contains("SecondNodeProcessLogic.kt") },
            "New SecondNodeProcessLogic should be in filesCreated")
    }

    // ========== Orphan Deletion (replaces warn-only behavior) ==========

    @Test
    fun `re-save deletes orphaned ProcessingLogic when node removed`() {
        val node1 = createTestCodeNode("node1", "KeptNode", CodeNodeType.GENERATOR)
        val node2 = createTestCodeNode("node2", "RemovedNode", CodeNodeType.SINK)
        val flowGraph1 = createTestFlowGraph("ShrinkingFlow", listOf(node1, node2))
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val packageDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/shrinkingflow/processingLogic")
        val keptFile = File(packageDir, "KeptNodeProcessLogic.kt")
        val removedFile = File(packageDir, "RemovedNodeProcessLogic.kt")
        assertTrue(keptFile.exists(), "KeptNode stub should exist after first save")
        assertTrue(removedFile.exists(), "RemovedNode stub should exist after first save")

        // Remove node2 from flow
        val flowGraph2 = createTestFlowGraph("ShrinkingFlow", listOf(node1))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        assertTrue(keptFile.exists(), "KeptNode stub should still exist")
        assertFalse(removedFile.exists(), "Orphaned RemovedNode stub should be deleted")
        assertTrue(result2.filesDeleted.any { it.contains("RemovedNodeProcessLogic.kt") },
            "filesDeleted should include the orphaned file")
    }

    @Test
    fun `re-save deletes orphaned StateProperties when node removed`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val displayReceiver = CodeNode(
            id = "display", name = "DisplayReceiver", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )
        val flowGraph1 = FlowGraph(id = "flow_1", name = "ShrinkFlow", version = "1.0.0", rootNodes = listOf(timerEmitter, displayReceiver))
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val statePropsDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/shrinkflow/stateProperties")
        val timerFile = File(statePropsDir, "TimerEmitterStateProperties.kt")
        val displayFile = File(statePropsDir, "DisplayReceiverStateProperties.kt")
        assertTrue(timerFile.exists())
        assertTrue(displayFile.exists())

        // Remove displayReceiver from flow
        val flowGraph2 = FlowGraph(id = "flow_1", name = "ShrinkFlow", version = "1.0.0", rootNodes = listOf(timerEmitter))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        assertTrue(timerFile.exists(), "Kept node state properties should still exist")
        assertFalse(displayFile.exists(), "Orphaned state properties should be deleted")
        assertTrue(result2.filesDeleted.any { it.contains("DisplayReceiverStateProperties.kt") },
            "filesDeleted should include the orphaned state properties file")
    }

    // ========== State Properties Content ==========

    @Test
    fun `state properties file contains MutableStateFlow for output ports`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val statePropsDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4/stateProperties")
        val timerContent = File(statePropsDir, "TimerEmitterStateProperties.kt").readText()

        assertTrue(timerContent.contains("object TimerEmitterStateProperties"),
            "Should contain object declaration")
        assertTrue(timerContent.contains("internal val _elapsedSeconds = MutableStateFlow(0)"),
            "Should contain MutableStateFlow for elapsedSeconds")
        assertTrue(timerContent.contains("val elapsedSecondsFlow: StateFlow<Int>"),
            "Should contain StateFlow accessor for elapsedSeconds")
        assertTrue(timerContent.contains("internal val _elapsedMinutes = MutableStateFlow(0)"),
            "Should contain MutableStateFlow for elapsedMinutes")
    }

    @Test
    fun `state properties file contains MutableStateFlow for input ports`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val statePropsDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4/stateProperties")
        val displayContent = File(statePropsDir, "DisplayReceiverStateProperties.kt").readText()

        assertTrue(displayContent.contains("object DisplayReceiverStateProperties"),
            "Should contain object declaration")
        assertTrue(displayContent.contains("internal val _seconds = MutableStateFlow(0)"),
            "Should contain MutableStateFlow for seconds input")
        assertTrue(displayContent.contains("val secondsFlow: StateFlow<Int>"),
            "Should contain StateFlow accessor for seconds")
        assertTrue(displayContent.contains("internal val _minutes = MutableStateFlow(0)"),
            "Should contain MutableStateFlow for minutes input")
    }

    @Test
    fun `state properties file contains reset method`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val statePropsDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4/stateProperties")
        val timerContent = File(statePropsDir, "TimerEmitterStateProperties.kt").readText()

        assertTrue(timerContent.contains("fun reset()"), "Should contain reset method")
        assertTrue(timerContent.contains("_elapsedSeconds.value = 0"), "Should reset elapsedSeconds")
        assertTrue(timerContent.contains("_elapsedMinutes.value = 0"), "Should reset elapsedMinutes")
    }

    @Test
    fun `saveModule does not generate state properties for portless nodes`() {
        val portlessNode = CodeNode(
            id = "empty",
            name = "EmptyNode",
            codeNodeType = CodeNodeType.TRANSFORMER,
            position = Node.Position(0.0, 0.0),
            inputPorts = emptyList(),
            outputPorts = emptyList()
        )
        val nodeWithPorts = createTestCodeNode("node1", "HasPorts")
        val flowGraph = FlowGraph(
            id = "flow_portless",
            name = "PortlessTest",
            version = "1.0.0",
            rootNodes = listOf(portlessNode, nodeWithPorts)
        )
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val statePropsDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/portlesstest/stateProperties")

        assertFalse(File(statePropsDir, "EmptyNodeStateProperties.kt").exists(),
            "No state properties file for portless node")
        assertTrue(File(statePropsDir, "HasPortsStateProperties.kt").exists(),
            "State properties file should exist for node with ports")
    }

    @Test
    fun `saveModule does not overwrite existing state properties files`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        val statePropsDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4/stateProperties")
        val timerFile = File(statePropsDir, "TimerEmitterStateProperties.kt")
        val userModification = "// USER MODIFIED\n" + timerFile.readText()
        timerFile.writeText(userModification)

        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        val content = timerFile.readText()
        assertTrue(content.startsWith("// USER MODIFIED"),
            "State properties file should not be overwritten")
    }

    @Test
    fun `re-save generates state properties only for new nodes`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val flowGraph1 = FlowGraph(id = "flow_1", name = "ExpandFlow", version = "1.0.0", rootNodes = listOf(timerEmitter))
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val statePropsDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/expandflow/stateProperties")
        val timerFile = File(statePropsDir, "TimerEmitterStateProperties.kt")
        val userModified = "// USER MODIFIED\n" + timerFile.readText()
        timerFile.writeText(userModified)

        val displayReceiver = CodeNode(
            id = "display", name = "DisplayReceiver", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )
        val flowGraph2 = FlowGraph(id = "flow_1", name = "ExpandFlow", version = "1.0.0", rootNodes = listOf(timerEmitter, displayReceiver))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        assertTrue(timerFile.readText().startsWith("// USER MODIFIED"),
            "Existing state properties should not be overwritten")
        val displayFile = File(statePropsDir, "DisplayReceiverStateProperties.kt")
        assertTrue(displayFile.exists(), "New node should get a state properties file")
        assertTrue(displayFile.readText().contains("object DisplayReceiverStateProperties"),
            "New state properties file should contain object declaration")
    }

    // ========== Runtime File Generation ==========

    @Test
    fun `saveModule creates all 5 runtime files in generated directory`() {
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

        val result = saveService.saveModule(flowGraph, tempDir)

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
    fun `re-save overwrites existing runtime files`() {
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

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val generatedDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/overwritetest/generated")
        val flowFile = File(generatedDir, "OverwriteTestFlow.kt")
        val originalContent = flowFile.readText()
        assertTrue(originalContent.contains("Source"), "First save should reference Source node")

        val node2 = node1.copy(name = "UpdatedSource")
        val flowGraph2 = flowGraph1.copy(rootNodes = listOf(node2))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        val updatedContent = flowFile.readText()
        assertTrue(updatedContent.contains("UpdatedSource"),
            "Runtime file should be overwritten with new content")
    }

    // ========== filesOverwritten Tracking ==========

    @Test
    fun `first save reports all files as created, none overwritten`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        assertTrue(result.filesCreated.isNotEmpty(), "First save should create files")
        assertTrue(result.filesOverwritten.isEmpty(), "First save should not overwrite any files")
        assertTrue(result.filesDeleted.isEmpty(), "First save should not delete any files")
    }

    @Test
    fun `re-save reports flow kt and runtime files as overwritten`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        // First save
        saveService.saveModule(flowGraph, tempDir)

        // Re-save
        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch4.flow.kt") },
            "flow.kt should be in filesOverwritten")
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch4Flow.kt") },
            "StopWatch4Flow.kt should be in filesOverwritten")
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch4Controller.kt") },
            "StopWatch4Controller.kt should be in filesOverwritten")
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch4ControllerInterface.kt") },
            "StopWatch4ControllerInterface.kt should be in filesOverwritten")
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch4ControllerAdapter.kt") },
            "StopWatch4ControllerAdapter.kt should be in filesOverwritten")
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch4ViewModel.kt") },
            "StopWatch4ViewModel.kt should be in filesOverwritten")
        assertEquals(6, result2.filesOverwritten.size,
            "Should have exactly 6 overwritten files (flow.kt + 5 runtime)")
    }

    @Test
    fun `re-save does not report gradle files as overwritten`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        saveService.saveModule(flowGraph, tempDir)
        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        assertFalse(result2.filesOverwritten.any { it.contains("build.gradle.kts") },
            "build.gradle.kts should not be overwritten on re-save")
        assertFalse(result2.filesOverwritten.any { it.contains("settings.gradle.kts") },
            "settings.gradle.kts should not be overwritten on re-save")
    }

    @Test
    fun `filesCreated includes all file categories on first save`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        // Gradle files
        assertTrue(result.filesCreated.any { it.contains("build.gradle.kts") })
        assertTrue(result.filesCreated.any { it.contains("settings.gradle.kts") })
        // .flow.kt
        assertTrue(result.filesCreated.any { it.contains("StopWatch4.flow.kt") })
        // 5 runtime files
        assertTrue(result.filesCreated.any { it.contains("StopWatch4Flow.kt") })
        assertTrue(result.filesCreated.any { it.contains("StopWatch4Controller.kt") })
        assertTrue(result.filesCreated.any { it.contains("StopWatch4ControllerInterface.kt") })
        assertTrue(result.filesCreated.any { it.contains("StopWatch4ControllerAdapter.kt") })
        assertTrue(result.filesCreated.any { it.contains("StopWatch4ViewModel.kt") })
        // Processing logic stubs
        assertTrue(result.filesCreated.any { it.contains("TimerEmitterProcessLogic.kt") })
        assertTrue(result.filesCreated.any { it.contains("DisplayReceiverProcessLogic.kt") })
        // State properties stubs
        assertTrue(result.filesCreated.any { it.contains("TimerEmitterStateProperties.kt") })
        assertTrue(result.filesCreated.any { it.contains("DisplayReceiverStateProperties.kt") })
        // Total: 2 gradle + 1 flow.kt + 5 runtime + 2 stubs + 2 state props = 12
        assertEquals(12, result.filesCreated.size, "First save should create 12 files")
    }

    @Test
    fun `filesCreated excludes existing state properties files on re-save`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)
        assertTrue(result1.filesCreated.any { it.contains("TimerEmitterStateProperties.kt") },
            "First save should report TimerEmitterStateProperties as created")

        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        assertFalse(result2.filesCreated.any { it.contains("TimerEmitterStateProperties.kt") },
            "Existing state properties should not be in filesCreated")
        assertFalse(result2.filesCreated.any { it.contains("DisplayReceiverStateProperties.kt") },
            "Existing state properties should not be in filesCreated")
    }

    // ========== End-to-End Quickstart Validation ==========

    @Test
    fun `end-to-end StopWatch4 save validates all integration`() {
        // Quickstart checklist item 1: Save a StopWatch4-like FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()
        val result = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result.success, "Save should succeed")

        val baseDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4")
        val statePropsDir = File(baseDir, "stateProperties")
        val processingLogicDir = File(baseDir, "processingLogic")
        val generatedDir = File(baseDir, "generated")

        // Quickstart checklist item 2: Verify stateProperties/ directory with 2 files
        assertTrue(statePropsDir.exists(), "stateProperties/ directory should exist")
        val stateFiles = statePropsDir.listFiles()?.filter { it.isFile && it.name.endsWith("StateProperties.kt") } ?: emptyList()
        assertEquals(2, stateFiles.size, "Should have exactly 2 state properties files")
        assertTrue(stateFiles.any { it.name == "TimerEmitterStateProperties.kt" })
        assertTrue(stateFiles.any { it.name == "DisplayReceiverStateProperties.kt" })

        // Quickstart checklist item 3: Verify MutableStateFlow/StateFlow pairs match port names/types
        val timerContent = File(statePropsDir, "TimerEmitterStateProperties.kt").readText()
        assertTrue(timerContent.contains("internal val _elapsedSeconds = MutableStateFlow(0)"))
        assertTrue(timerContent.contains("val elapsedSecondsFlow: StateFlow<Int>"))
        assertTrue(timerContent.contains("internal val _elapsedMinutes = MutableStateFlow(0)"))
        assertTrue(timerContent.contains("val elapsedMinutesFlow: StateFlow<Int>"))

        val displayContent = File(statePropsDir, "DisplayReceiverStateProperties.kt").readText()
        assertTrue(displayContent.contains("internal val _seconds = MutableStateFlow(0)"))
        assertTrue(displayContent.contains("val secondsFlow: StateFlow<Int>"))
        assertTrue(displayContent.contains("internal val _minutes = MutableStateFlow(0)"))
        assertTrue(displayContent.contains("val minutesFlow: StateFlow<Int>"))

        // Quickstart checklist item 4: Verify processing logic stubs import state properties
        val timerStub = File(processingLogicDir, "TimerEmitterProcessLogic.kt")
        assertTrue(timerStub.exists(), "TimerEmitter stub should exist")
        assertTrue(timerStub.readText().contains("import io.codenode.stopwatch4.stateProperties.TimerEmitterStateProperties"),
            "TimerEmitter stub should import TimerEmitterStateProperties")

        val displayStub = File(processingLogicDir, "DisplayReceiverProcessLogic.kt")
        assertTrue(displayStub.exists(), "DisplayReceiver stub should exist")
        assertTrue(displayStub.readText().contains("import io.codenode.stopwatch4.stateProperties.DisplayReceiverStateProperties"),
            "DisplayReceiver stub should import DisplayReceiverStateProperties")

        // Quickstart checklist item 5: Verify Flow class delegates from StateProperties
        val flowFile = File(generatedDir, "StopWatch4Flow.kt")
        assertTrue(flowFile.exists(), "StopWatch4Flow.kt should exist")
        val flowContent = flowFile.readText()
        assertTrue(flowContent.contains("val secondsFlow: StateFlow<Int> = DisplayReceiverStateProperties.secondsFlow"),
            "Flow should delegate secondsFlow from DisplayReceiverStateProperties")
        assertTrue(flowContent.contains("val minutesFlow: StateFlow<Int> = DisplayReceiverStateProperties.minutesFlow"),
            "Flow should delegate minutesFlow from DisplayReceiverStateProperties")
        assertFalse(flowContent.contains("MutableStateFlow"),
            "Flow should not own MutableStateFlow when delegating to state properties")

        // Quickstart checklist item 6: Verify Flow's reset() calls state properties reset()
        assertTrue(flowContent.contains("TimerEmitterStateProperties.reset()"),
            "Flow reset() should call TimerEmitterStateProperties.reset()")
        assertTrue(flowContent.contains("DisplayReceiverStateProperties.reset()"),
            "Flow reset() should call DisplayReceiverStateProperties.reset()")

        // Quickstart checklist item 7: Re-save preserves existing state property files
        val timerFile = File(statePropsDir, "TimerEmitterStateProperties.kt")
        val displayFile = File(statePropsDir, "DisplayReceiverStateProperties.kt")
        timerFile.writeText("// USER CUSTOMIZED TIMER\n" + timerFile.readText())
        displayFile.writeText("// USER CUSTOMIZED DISPLAY\n" + displayFile.readText())

        val result2 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result2.success, "Re-save should succeed")
        assertTrue(timerFile.readText().startsWith("// USER CUSTOMIZED TIMER"),
            "TimerEmitter state properties should be preserved on re-save")
        assertTrue(displayFile.readText().startsWith("// USER CUSTOMIZED DISPLAY"),
            "DisplayReceiver state properties should be preserved on re-save")
    }

    @Test
    fun `filesCreated includes all 5 runtime file paths on first save`() {
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

        val result = saveService.saveModule(flowGraph, tempDir)

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

    // ========== T006: First Save Integration Test (US1) ==========

    @Test
    fun `T006 - first save creates complete module with all file categories`() {
        // Matches quickstart.md Step 1: StopWatch3-like FlowGraph with 2-output generator + 2-input sink
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer"),
                Port(id = "timer_min", name = "elapsedMinutes", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val displayReceiver = CodeNode(
            id = "display", name = "DisplayReceiver", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 200.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display"),
                Port(id = "display_min", name = "minutes", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )
        val flowGraph = FlowGraph(
            id = "flow_stopwatch3",
            name = "StopWatch3",
            version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = listOf(
                Connection("c1", "timer", "timer_sec", "display", "display_sec"),
                Connection("c2", "timer", "timer_min", "display", "display_min")
            )
        )
        val saveService = ModuleSaveService()

        // First save — single saveModule() call
        val result = saveService.saveModule(flowGraph, tempDir)

        // Verify success
        assertTrue(result.success, "First save should succeed")
        assertNotNull(result.moduleDir, "Module directory should be returned")

        val moduleDir = result.moduleDir!!
        val basePackagePath = "src/commonMain/kotlin/io/codenode/stopwatch3"

        // 1. Module directory structure
        assertTrue(File(moduleDir, "build.gradle.kts").exists(), "build.gradle.kts should exist")
        assertTrue(File(moduleDir, "settings.gradle.kts").exists(), "settings.gradle.kts should exist")
        assertTrue(File(moduleDir, "$basePackagePath/StopWatch3.flow.kt").exists(), ".flow.kt should be in source set")

        // 2. All 5 runtime files in generated/
        val generatedDir = File(moduleDir, "$basePackagePath/generated")
        assertTrue(File(generatedDir, "StopWatch3Flow.kt").exists(), "StopWatch3Flow.kt should exist")
        assertTrue(File(generatedDir, "StopWatch3Controller.kt").exists(), "StopWatch3Controller.kt should exist")
        assertTrue(File(generatedDir, "StopWatch3ControllerInterface.kt").exists(), "StopWatch3ControllerInterface.kt should exist")
        assertTrue(File(generatedDir, "StopWatch3ControllerAdapter.kt").exists(), "StopWatch3ControllerAdapter.kt should exist")
        assertTrue(File(generatedDir, "StopWatch3ViewModel.kt").exists(), "StopWatch3ViewModel.kt should exist")

        // 3. Processing logic stubs in processingLogic/
        val processingLogicDir = File(moduleDir, "$basePackagePath/processingLogic")
        assertTrue(File(processingLogicDir, "TimerEmitterProcessLogic.kt").exists(), "TimerEmitter stub should exist")
        assertTrue(File(processingLogicDir, "DisplayReceiverProcessLogic.kt").exists(), "DisplayReceiver stub should exist")

        // 4. State properties stubs in stateProperties/
        val statePropsDir = File(moduleDir, "$basePackagePath/stateProperties")
        assertTrue(File(statePropsDir, "TimerEmitterStateProperties.kt").exists(), "TimerEmitter state props should exist")
        assertTrue(File(statePropsDir, "DisplayReceiverStateProperties.kt").exists(), "DisplayReceiver state props should exist")

        // 5. filesCreated includes all 12 files
        assertEquals(12, result.filesCreated.size,
            "First save should report 12 files created (2 gradle + 1 flow.kt + 5 runtime + 2 stubs + 2 state props)")

        // 6. No overwrites or deletions on first save
        assertTrue(result.filesOverwritten.isEmpty(), "First save should have 0 overwritten")
        assertTrue(result.filesDeleted.isEmpty(), "First save should have 0 deleted")
    }

    // ========== T008: Re-Save Integration Tests (US2) ==========

    @Test
    fun `T008 - re-save overwrites flow kt with updated content`() {
        // First save with 2 nodes
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val displayReceiver = CodeNode(
            id = "display", name = "DisplayReceiver", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 200.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )
        val flowGraph1 = FlowGraph(
            id = "flow_resave", name = "ReSaveTest", version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = listOf(Connection("c1", "timer", "timer_sec", "display", "display_sec"))
        )
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        // Add a Logger node
        val logger = CodeNode(
            id = "logger", name = "Logger", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 400.0),
            inputPorts = listOf(
                Port(id = "logger_in", name = "message", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "logger")
            ),
            outputPorts = emptyList()
        )
        val flowGraph2 = flowGraph1.copy(rootNodes = listOf(timerEmitter, displayReceiver, logger))

        // Re-save
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        val flowKtFile = File(result2.moduleDir, "src/commonMain/kotlin/io/codenode/resavetest/ReSaveTest.flow.kt")
        val content = flowKtFile.readText()
        assertTrue(content.contains("Logger"), ".flow.kt should contain the new Logger node after re-save")
        assertTrue(content.contains("TimerEmitter"), ".flow.kt should still contain TimerEmitter")
    }

    @Test
    fun `T008 - re-save overwrites all 5 runtime files`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val displayReceiver = CodeNode(
            id = "display", name = "DisplayReceiver", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 200.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )
        val flowGraph1 = FlowGraph(
            id = "flow_resave", name = "ReSaveTest", version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver)
        )
        val saveService = ModuleSaveService()

        saveService.saveModule(flowGraph1, tempDir)

        // Re-save same flow
        val result2 = saveService.saveModule(flowGraph1, tempDir)

        assertTrue(result2.success)
        val runtimeFiles = listOf(
            "ReSaveTestFlow.kt", "ReSaveTestController.kt",
            "ReSaveTestControllerInterface.kt", "ReSaveTestControllerAdapter.kt",
            "ReSaveTestViewModel.kt"
        )
        for (fileName in runtimeFiles) {
            assertTrue(result2.filesOverwritten.any { it.contains(fileName) },
                "$fileName should be in filesOverwritten on re-save")
        }
    }

    @Test
    fun `T008 - re-save preserves existing processing logic stubs`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val displayReceiver = CodeNode(
            id = "display", name = "DisplayReceiver", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 200.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )
        val flowGraph = FlowGraph(
            id = "flow_resave", name = "ReSaveTest", version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver)
        )
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        // Simulate user editing the stubs
        val procDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/resavetest/processingLogic")
        val timerStub = File(procDir, "TimerEmitterProcessLogic.kt")
        val displayStub = File(procDir, "DisplayReceiverProcessLogic.kt")
        timerStub.writeText("// USER CODE: TimerEmitter\n" + timerStub.readText())
        displayStub.writeText("// USER CODE: DisplayReceiver\n" + displayStub.readText())

        // Re-save
        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        assertTrue(timerStub.readText().startsWith("// USER CODE: TimerEmitter"),
            "TimerEmitter processing logic should be preserved")
        assertTrue(displayStub.readText().startsWith("// USER CODE: DisplayReceiver"),
            "DisplayReceiver processing logic should be preserved")
        assertFalse(result2.filesCreated.any { it.contains("TimerEmitterProcessLogic.kt") },
            "Existing stub should not be in filesCreated")
    }

    @Test
    fun `T008 - re-save preserves existing state properties stubs`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val flowGraph = FlowGraph(
            id = "flow_resave", name = "ReSaveTest", version = "1.0.0",
            rootNodes = listOf(timerEmitter)
        )
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        val stateDir = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/resavetest/stateProperties")
        val timerProps = File(stateDir, "TimerEmitterStateProperties.kt")
        timerProps.writeText("// USER MODIFIED STATE\n" + timerProps.readText())

        // Re-save
        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        assertTrue(timerProps.readText().startsWith("// USER MODIFIED STATE"),
            "State properties should be preserved on re-save")
        assertFalse(result2.filesCreated.any { it.contains("TimerEmitterStateProperties.kt") },
            "Existing state properties should not be in filesCreated")
    }

    @Test
    fun `T008 - re-save creates new stubs for added nodes`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val flowGraph1 = FlowGraph(
            id = "flow_resave", name = "ReSaveTest", version = "1.0.0",
            rootNodes = listOf(timerEmitter)
        )
        val saveService = ModuleSaveService()

        saveService.saveModule(flowGraph1, tempDir)

        // Add a Logger node
        val logger = CodeNode(
            id = "logger", name = "Logger", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 400.0),
            inputPorts = listOf(
                Port(id = "logger_in", name = "message", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "logger")
            ),
            outputPorts = emptyList()
        )
        val flowGraph2 = flowGraph1.copy(rootNodes = listOf(timerEmitter, logger))

        // Re-save with new node
        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        // New stubs created
        assertTrue(result2.filesCreated.any { it.contains("LoggerProcessLogic.kt") },
            "New LoggerProcessLogic stub should be in filesCreated")
        assertTrue(result2.filesCreated.any { it.contains("LoggerStateProperties.kt") },
            "New LoggerStateProperties stub should be in filesCreated")
        // Existing stubs NOT in filesCreated
        assertFalse(result2.filesCreated.any { it.contains("TimerEmitterProcessLogic.kt") },
            "Existing TimerEmitter stub should not be in filesCreated")
    }

    @Test
    fun `T008 - re-save result matches quickstart Step 2 counts`() {
        // Matches quickstart.md Step 2: re-save after adding a Logger node
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer"),
                Port(id = "timer_min", name = "elapsedMinutes", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val displayReceiver = CodeNode(
            id = "display", name = "DisplayReceiver", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 200.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display"),
                Port(id = "display_min", name = "minutes", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )
        val flowGraph1 = FlowGraph(
            id = "flow_qs2", name = "StopWatch3", version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = listOf(
                Connection("c1", "timer", "timer_sec", "display", "display_sec"),
                Connection("c2", "timer", "timer_min", "display", "display_min")
            )
        )
        val saveService = ModuleSaveService()

        // Step 1: First save
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)
        assertEquals(12, result1.filesCreated.size, "Step 1: 12 files created")

        // Step 2: Add Logger node, re-save
        val logger = CodeNode(
            id = "logger", name = "Logger", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 400.0),
            inputPorts = listOf(
                Port(id = "logger_in", name = "message", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "logger")
            ),
            outputPorts = emptyList()
        )
        val flowGraph2 = flowGraph1.copy(rootNodes = listOf(timerEmitter, displayReceiver, logger))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        // Quickstart Step 2: 2 files created, 6 files overwritten, 0 deleted
        assertEquals(2, result2.filesCreated.size,
            "Step 2: 2 new files created (LoggerProcessLogic + LoggerStateProperties)")
        assertEquals(6, result2.filesOverwritten.size,
            "Step 2: 6 files overwritten (flow.kt + 5 runtime)")
        assertEquals(0, result2.filesDeleted.size,
            "Step 2: 0 files deleted")

        // Verify the 2 created files are Logger stubs
        assertTrue(result2.filesCreated.any { it.contains("LoggerProcessLogic.kt") })
        assertTrue(result2.filesCreated.any { it.contains("LoggerStateProperties.kt") })

        // Verify the 6 overwritten files are flow.kt + 5 runtime
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3.flow.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3Flow.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3Controller.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3ControllerInterface.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3ControllerAdapter.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3ViewModel.kt") })
    }

    // ========== T009: Orphan Deletion Integration (Quickstart Step 3) ==========

    @Test
    fun `T009 - re-save after removing node deletes orphaned stubs and preserves remaining`() {
        // Matches quickstart.md Step 3: remove Logger node after Step 2
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer"),
                Port(id = "timer_min", name = "elapsedMinutes", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val displayReceiver = CodeNode(
            id = "display", name = "DisplayReceiver", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 200.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display"),
                Port(id = "display_min", name = "minutes", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )
        val logger = CodeNode(
            id = "logger", name = "Logger", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 400.0),
            inputPorts = listOf(
                Port(id = "logger_in", name = "message", direction = Port.Direction.INPUT, dataType = String::class, owningNodeId = "logger")
            ),
            outputPorts = emptyList()
        )
        val flowGraph1 = FlowGraph(
            id = "flow_qs3", name = "StopWatch3", version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver, logger),
            connections = listOf(
                Connection("c1", "timer", "timer_sec", "display", "display_sec"),
                Connection("c2", "timer", "timer_min", "display", "display_min")
            )
        )
        val saveService = ModuleSaveService()

        // Step 1: First save with 3 nodes
        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val moduleDir = result1.moduleDir!!
        val processingLogicDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch3/processingLogic")
        val statePropsDir = File(moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch3/stateProperties")

        // Verify all 3 nodes have stubs
        assertTrue(File(processingLogicDir, "TimerEmitterProcessLogic.kt").exists())
        assertTrue(File(processingLogicDir, "DisplayReceiverProcessLogic.kt").exists())
        assertTrue(File(processingLogicDir, "LoggerProcessLogic.kt").exists())
        assertTrue(File(statePropsDir, "TimerEmitterStateProperties.kt").exists())
        assertTrue(File(statePropsDir, "DisplayReceiverStateProperties.kt").exists())
        assertTrue(File(statePropsDir, "LoggerStateProperties.kt").exists())

        // Step 3: Remove Logger node, re-save
        val flowGraph3 = flowGraph1.copy(rootNodes = listOf(timerEmitter, displayReceiver))

        val result3 = saveService.saveModule(flowGraph3, tempDir)

        assertTrue(result3.success)

        // Logger stubs deleted
        assertFalse(File(processingLogicDir, "LoggerProcessLogic.kt").exists(),
            "Logger processing logic stub should be deleted")
        assertFalse(File(statePropsDir, "LoggerStateProperties.kt").exists(),
            "Logger state properties stub should be deleted")

        // TimerEmitter and DisplayReceiver stubs preserved
        assertTrue(File(processingLogicDir, "TimerEmitterProcessLogic.kt").exists(),
            "TimerEmitter processing logic stub should be preserved")
        assertTrue(File(processingLogicDir, "DisplayReceiverProcessLogic.kt").exists(),
            "DisplayReceiver processing logic stub should be preserved")
        assertTrue(File(statePropsDir, "TimerEmitterStateProperties.kt").exists(),
            "TimerEmitter state properties stub should be preserved")
        assertTrue(File(statePropsDir, "DisplayReceiverStateProperties.kt").exists(),
            "DisplayReceiver state properties stub should be preserved")

        // filesDeleted contains the deleted file paths
        assertTrue(result3.filesDeleted.any { it.contains("LoggerProcessLogic.kt") },
            "filesDeleted should include LoggerProcessLogic.kt")
        assertTrue(result3.filesDeleted.any { it.contains("LoggerStateProperties.kt") },
            "filesDeleted should include LoggerStateProperties.kt")

        // Quickstart Step 3 counts: 0 created, 6 overwritten, 2 deleted
        assertEquals(0, result3.filesCreated.size,
            "Step 3: 0 new files created")
        assertEquals(6, result3.filesOverwritten.size,
            "Step 3: 6 files overwritten (flow.kt + 5 runtime)")
        assertEquals(2, result3.filesDeleted.size,
            "Step 3: 2 files deleted (Logger processing logic + state properties)")
    }

    // ========== T010: Name Change Integration (Quickstart Step 4) ==========

    @Test
    fun `T010 - save under new name creates new module and preserves original`() {
        // Matches quickstart.md Step 4: rename and save as new module
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 200.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(id = "timer_sec", name = "elapsedSeconds", direction = Port.Direction.OUTPUT, dataType = Int::class, owningNodeId = "timer")
            )
        )
        val displayReceiver = CodeNode(
            id = "display", name = "DisplayReceiver", codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 200.0),
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            ),
            outputPorts = emptyList()
        )

        // Save as "Alpha"
        val alphaFlow = FlowGraph(
            id = "flow_alpha", name = "Alpha", version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = listOf(
                Connection("c1", "timer", "timer_sec", "display", "display_sec")
            )
        )
        val saveService = ModuleSaveService()

        val alphaResult = saveService.saveModule(alphaFlow, tempDir)
        assertTrue(alphaResult.success)
        val alphaDir = alphaResult.moduleDir!!
        assertTrue(alphaDir.exists(), "Alpha module directory should exist")

        // Record Alpha's files for later comparison
        val alphaFlowKt = File(alphaDir, "src/commonMain/kotlin/io/codenode/alpha/Alpha.flow.kt")
        assertTrue(alphaFlowKt.exists(), "Alpha .flow.kt should exist")
        val alphaFlowKtContent = alphaFlowKt.readText()
        val alphaGeneratedDir = File(alphaDir, "src/commonMain/kotlin/io/codenode/alpha/generated")
        assertTrue(alphaGeneratedDir.exists(), "Alpha generated dir should exist")

        // Rename to "Beta" and save to same output dir
        val betaFlow = FlowGraph(
            id = "flow_beta", name = "Beta", version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = listOf(
                Connection("c1", "timer", "timer_sec", "display", "display_sec")
            )
        )

        val betaResult = saveService.saveModule(betaFlow, tempDir)
        assertTrue(betaResult.success)
        val betaDir = betaResult.moduleDir!!

        // Beta is a complete new module
        assertTrue(betaDir.exists(), "Beta module directory should exist")
        assertNotEquals(alphaDir.absolutePath, betaDir.absolutePath,
            "Beta module should be in a different directory than Alpha")
        assertTrue(File(betaDir, "src/commonMain/kotlin/io/codenode/beta/Beta.flow.kt").exists(), "Beta .flow.kt should exist")
        assertTrue(File(betaDir, "build.gradle.kts").exists(), "Beta build.gradle.kts should exist")
        assertTrue(File(betaDir, "settings.gradle.kts").exists(), "Beta settings.gradle.kts should exist")
        assertTrue(File(betaDir, "src/commonMain/kotlin/io/codenode/beta/generated/BetaFlow.kt").exists(),
            "Beta runtime files should exist")
        assertTrue(File(betaDir, "src/commonMain/kotlin/io/codenode/beta/processingLogic/TimerEmitterProcessLogic.kt").exists(),
            "Beta processing logic stubs should exist")
        assertTrue(File(betaDir, "src/commonMain/kotlin/io/codenode/beta/stateProperties/TimerEmitterStateProperties.kt").exists(),
            "Beta state properties stubs should exist")

        // Alpha module is untouched
        assertTrue(alphaDir.exists(), "Alpha module directory should still exist")
        assertTrue(alphaFlowKt.exists(), "Alpha .flow.kt should still exist")
        assertEquals(alphaFlowKtContent, alphaFlowKt.readText(),
            "Alpha .flow.kt content should be unchanged")
        assertTrue(alphaGeneratedDir.exists(), "Alpha generated dir should still exist")
    }
}
