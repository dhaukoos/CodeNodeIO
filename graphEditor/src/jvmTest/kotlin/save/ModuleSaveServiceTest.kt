/*
 * ModuleSaveService Test
 * TDD tests for creating KMP module structure on FlowGraph save
 * License: Apache 2.0
 */

package io.codenode.grapheditor.save

import io.codenode.fbpdsl.model.*
import io.codenode.flowgraphgenerate.generator.RuntimeViewModelGenerator
import java.io.File
import kotlin.test.*

/**
 * TDD tests for ModuleSaveService - verifies unified saveModule() creates the
 * full module: directory structure, gradle files, .flow.kt, 4 runtime files,
 * and a ViewModel stub in the base package.
 * Handles selective ViewModel regeneration.
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
            codeNodeType = CodeNodeType.SOURCE,
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
        val generatedDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/testmodule/generated")
        assertTrue(generatedDir.exists(), "Generated package directory should exist")
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
        val expectedPackageDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch/generated")
        assertTrue(expectedPackageDir.exists(),
            "Default generated package directory should be created based on module name")
    }

    // ========== .flow.kt in source set ==========

    @Test
    fun `saveModule writes flow kt in source set`() {
        val node1 = createTestCodeNode("node1", "Original", CodeNodeType.SOURCE)
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
        val node1 = createTestCodeNode("node1", "Original", CodeNodeType.SOURCE)
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
        val node1 = createTestCodeNode("node1", "Source", CodeNodeType.SOURCE)
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

    // ========== Unified saveModule generates runtime files ==========

    @Test
    fun `saveModule generates all 4 runtime files in generated and ViewModel in base package`() {
        val flowGraph = createTestFlowGraph("FullSave")
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val generatedDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/fullsave/generated")
        val expectedRuntimeFiles = listOf(
            "FullSaveFlow.kt",
            "FullSaveController.kt",
            "FullSaveControllerInterface.kt",
            "FullSaveControllerAdapter.kt"
        )
        for (fileName in expectedRuntimeFiles) {
            val file = File(generatedDir, fileName)
            assertTrue(file.exists(), "$fileName should exist in generated directory")
            assertTrue(file.readText().isNotBlank(), "$fileName should not be empty")
        }

        // ViewModel stub in base package (not generated/)
        val viewModelFile = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/fullsave/FullSaveViewModel.kt")
        assertTrue(viewModelFile.exists(), "FullSaveViewModel.kt should exist in base package")
        assertTrue(viewModelFile.readText().isNotBlank(), "FullSaveViewModel.kt should not be empty")
        assertFalse(File(generatedDir, "FullSaveViewModel.kt").exists(),
            "FullSaveViewModel.kt should NOT exist in generated directory")
    }

    // ========== ViewModel Stub Generation ==========

    @Test
    fun `saveModule generates ViewModel stub with Module Properties markers`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val viewModelFile = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4/StopWatch4ViewModel.kt")
        assertTrue(viewModelFile.exists(), "ViewModel stub should be in base package")
        val content = viewModelFile.readText()
        assertTrue(content.contains(RuntimeViewModelGenerator.MODULE_PROPERTIES_START),
            "ViewModel stub should contain MODULE PROPERTIES START marker")
        assertTrue(content.contains(RuntimeViewModelGenerator.MODULE_PROPERTIES_END),
            "ViewModel stub should contain MODULE PROPERTIES END marker")
    }

    @Test
    fun `saveModule generates ViewModel stub with State object for sink ports`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result.success)
        val viewModelFile = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4/StopWatch4ViewModel.kt")
        val content = viewModelFile.readText()
        assertTrue(content.contains("object StopWatch4State"),
            "ViewModel stub should contain State object")
        assertTrue(content.contains("internal val _seconds = MutableStateFlow(0)"),
            "State object should contain MutableStateFlow for sink input port 'seconds'")
        assertTrue(content.contains("val secondsFlow: StateFlow<Int>"),
            "State object should contain StateFlow accessor for 'seconds'")
        assertTrue(content.contains("internal val _minutes = MutableStateFlow(0)"),
            "State object should contain MutableStateFlow for sink input port 'minutes'")
        assertTrue(content.contains("val minutesFlow: StateFlow<Int>"),
            "State object should contain StateFlow accessor for 'minutes'")
    }

    @Test
    fun `re-save selectively regenerates ViewModel module properties section`() {
        // First save with original sink ports
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.SOURCE,
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
        val flowGraph1 = FlowGraph(
            id = "flow_vm", name = "VmRegenTest", version = "1.0.0",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = listOf(Connection("c1", "timer", "timer_sec", "display", "display_sec"))
        )
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph1, tempDir)
        assertTrue(result1.success)

        val viewModelFile = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/vmregentest/VmRegenTestViewModel.kt")
        assertTrue(viewModelFile.readText().contains("_seconds"),
            "First save should have 'seconds' property")

        // Add a new sink port (minutes) and re-save
        val updatedDisplay = displayReceiver.copy(
            inputPorts = listOf(
                Port(id = "display_sec", name = "seconds", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display"),
                Port(id = "display_min", name = "minutes", direction = Port.Direction.INPUT, dataType = Int::class, owningNodeId = "display")
            )
        )
        val flowGraph2 = flowGraph1.copy(rootNodes = listOf(timerEmitter, updatedDisplay))

        val result2 = saveService.saveModule(flowGraph2, tempDir)

        assertTrue(result2.success)
        val updatedContent = viewModelFile.readText()
        assertTrue(updatedContent.contains("_seconds"),
            "Updated ViewModel should still have 'seconds' property")
        assertTrue(updatedContent.contains("_minutes"),
            "Updated ViewModel should have new 'minutes' property")
        assertTrue(result2.filesOverwritten.any { it.contains("VmRegenTestViewModel.kt") },
            "ViewModel should be in filesOverwritten")
    }

    @Test
    fun `re-save preserves user code outside ViewModel markers`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        val viewModelFile = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4/StopWatch4ViewModel.kt")
        val originalContent = viewModelFile.readText()

        // Simulate user adding code after MODULE PROPERTIES END marker
        val endMarker = RuntimeViewModelGenerator.MODULE_PROPERTIES_END
        val endIndex = originalContent.indexOf(endMarker)
        assertTrue(endIndex >= 0, "END marker should exist")
        val endOfMarkerLine = originalContent.indexOf('\n', endIndex)
        val userCode = "\n// USER CUSTOM CODE - MUST BE PRESERVED\nval customField = 42\n"
        val modifiedContent = originalContent.substring(0, endOfMarkerLine + 1) +
            userCode +
            originalContent.substring(endOfMarkerLine + 1)
        viewModelFile.writeText(modifiedContent)

        // Re-save
        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        val updatedContent = viewModelFile.readText()
        assertTrue(updatedContent.contains("// USER CUSTOM CODE - MUST BE PRESERVED"),
            "User code after END marker should be preserved on re-save")
        assertTrue(updatedContent.contains("val customField = 42"),
            "User's custom field should be preserved")
    }

    @Test
    fun `re-save with missing ViewModel markers regenerates from scratch`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()

        val result1 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result1.success)

        // Remove markers from ViewModel file
        val viewModelFile = File(result1.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4/StopWatch4ViewModel.kt")
        viewModelFile.writeText("// Corrupted file with no markers\nclass StopWatch4ViewModel\n")

        // Re-save
        val result2 = saveService.saveModule(flowGraph, tempDir)

        assertTrue(result2.success)
        val content = viewModelFile.readText()
        assertTrue(content.contains(RuntimeViewModelGenerator.MODULE_PROPERTIES_START),
            "Missing markers should trigger fresh generation with START marker")
        assertTrue(content.contains(RuntimeViewModelGenerator.MODULE_PROPERTIES_END),
            "Missing markers should trigger fresh generation with END marker")
        assertTrue(content.contains("object StopWatch4State"),
            "Fresh generation should include State object")
    }

    // ========== Runtime File Generation ==========

    @Test
    fun `saveModule creates all 4 runtime files in generated directory and ViewModel in base package`() {
        val node1 = CodeNode(
            id = "gen1",
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.SOURCE,
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

        val expectedRuntimeFiles = listOf(
            "RuntimeTestFlow.kt",
            "RuntimeTestController.kt",
            "RuntimeTestControllerInterface.kt",
            "RuntimeTestControllerAdapter.kt"
        )
        for (fileName in expectedRuntimeFiles) {
            val file = File(generatedDir, fileName)
            assertTrue(file.exists(), "$fileName should exist in generated directory")
            assertTrue(file.readText().isNotBlank(), "$fileName should not be empty")
        }

        // ViewModel in base package
        val viewModelFile = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/runtimetest/RuntimeTestViewModel.kt")
        assertTrue(viewModelFile.exists(), "RuntimeTestViewModel.kt should exist in base package")
        assertFalse(File(generatedDir, "RuntimeTestViewModel.kt").exists(),
            "ViewModel should NOT be in generated/")
    }

    @Test
    fun `re-save overwrites existing runtime files`() {
        val node1 = CodeNode(
            id = "gen1",
            name = "Source",
            codeNodeType = CodeNodeType.SOURCE,
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
    fun `re-save reports flow kt runtime files and ViewModel as overwritten`() {
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
            "Should have exactly 6 overwritten files (flow.kt + 4 runtime + 1 ViewModel)")
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
        // 4 runtime files in generated/
        assertTrue(result.filesCreated.any { it.contains("StopWatch4Flow.kt") })
        assertTrue(result.filesCreated.any { it.contains("StopWatch4Controller.kt") })
        assertTrue(result.filesCreated.any { it.contains("StopWatch4ControllerInterface.kt") })
        assertTrue(result.filesCreated.any { it.contains("StopWatch4ControllerAdapter.kt") })
        // ViewModel stub in base package
        assertTrue(result.filesCreated.any { it.contains("StopWatch4ViewModel.kt") })
        // UI stub
        assertTrue(result.filesCreated.any { it.contains("StopWatch4.kt") && it.contains("userInterface") })
        // Total: 2 gradle + 1 flow.kt + 4 runtime + 1 ViewModel + 1 UI stub = 9
        assertEquals(9, result.filesCreated.size, "First save should create 9 files")
    }

    // ========== End-to-End Quickstart Validation ==========

    @Test
    fun `end-to-end StopWatch4 save validates all integration`() {
        val flowGraph = createStopWatchFlowGraph()
        val saveService = ModuleSaveService()
        val result = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result.success, "Save should succeed")

        val baseDir = File(result.moduleDir, "src/commonMain/kotlin/io/codenode/stopwatch4")
        val generatedDir = File(baseDir, "generated")

        // Verify ViewModel stub in base package with State object
        val viewModelFile = File(baseDir, "StopWatch4ViewModel.kt")
        assertTrue(viewModelFile.exists(), "ViewModel stub should exist in base package")
        val viewModelContent = viewModelFile.readText()
        assertTrue(viewModelContent.contains("object StopWatch4State"),
            "ViewModel should contain State object")
        assertTrue(viewModelContent.contains("internal val _seconds = MutableStateFlow(0)"),
            "State object should have seconds MutableStateFlow")
        assertTrue(viewModelContent.contains("val secondsFlow: StateFlow<Int>"),
            "State object should have seconds StateFlow accessor")
        assertTrue(viewModelContent.contains("internal val _minutes = MutableStateFlow(0)"),
            "State object should have minutes MutableStateFlow")
        assertTrue(viewModelContent.contains("val minutesFlow: StateFlow<Int>"),
            "State object should have minutes StateFlow accessor")

        // Verify Flow class delegates from State object
        val flowFile = File(generatedDir, "StopWatch4Flow.kt")
        assertTrue(flowFile.exists(), "StopWatch4Flow.kt should exist")
        val flowContent = flowFile.readText()
        assertTrue(flowContent.contains("val secondsFlow: StateFlow<Int> = StopWatch4State.secondsFlow"),
            "Flow should delegate secondsFlow from StopWatch4State")
        assertTrue(flowContent.contains("val minutesFlow: StateFlow<Int> = StopWatch4State.minutesFlow"),
            "Flow should delegate minutesFlow from StopWatch4State")
        assertFalse(flowContent.contains("MutableStateFlow"),
            "Flow should not own MutableStateFlow when delegating to State object")

        // Verify Flow's reset() calls State.reset()
        assertTrue(flowContent.contains("StopWatch4State.reset()"),
            "Flow reset() should call StopWatch4State.reset()")

        // Verify re-save selective regeneration: Module Properties section updated, user code preserved
        val endMarker = RuntimeViewModelGenerator.MODULE_PROPERTIES_END
        val endIdx = viewModelContent.indexOf(endMarker)
        val endOfLine = viewModelContent.indexOf('\n', endIdx)
        val modifiedContent = viewModelContent.substring(0, endOfLine + 1) +
            "\n// USER CUSTOMIZED CODE\n" +
            viewModelContent.substring(endOfLine + 1)
        viewModelFile.writeText(modifiedContent)

        val result2 = saveService.saveModule(flowGraph, tempDir)
        assertTrue(result2.success, "Re-save should succeed")
        val updatedContent = viewModelFile.readText()
        assertTrue(updatedContent.contains("// USER CUSTOMIZED CODE"),
            "User code outside markers should be preserved on re-save")
        assertTrue(updatedContent.contains("object StopWatch4State"),
            "State object should still be present after re-save")
    }

    @Test
    fun `filesCreated includes all 4 runtime file paths and ViewModel on first save`() {
        val node1 = CodeNode(
            id = "gen1",
            name = "Emitter",
            codeNodeType = CodeNodeType.SOURCE,
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
            "FilesTestControllerAdapter.kt"
        )
        for (fileName in runtimeFileNames) {
            assertTrue(result.filesCreated.any { it.contains(fileName) },
                "filesCreated should include $fileName")
        }
        assertTrue(result.filesCreated.any { it.contains("FilesTestViewModel.kt") },
            "filesCreated should include FilesTestViewModel.kt")
    }

    // ========== T006: First Save Integration Test (US1) ==========

    @Test
    fun `T006 - first save creates complete module with all file categories`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.SOURCE,
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

        // 2. All 4 runtime files in generated/
        val generatedDir = File(moduleDir, "$basePackagePath/generated")
        assertTrue(File(generatedDir, "StopWatch3Flow.kt").exists(), "StopWatch3Flow.kt should exist")
        assertTrue(File(generatedDir, "StopWatch3Controller.kt").exists(), "StopWatch3Controller.kt should exist")
        assertTrue(File(generatedDir, "StopWatch3ControllerInterface.kt").exists(), "StopWatch3ControllerInterface.kt should exist")
        assertTrue(File(generatedDir, "StopWatch3ControllerAdapter.kt").exists(), "StopWatch3ControllerAdapter.kt should exist")

        // 3. ViewModel stub in base package (not generated/)
        val viewModelFile = File(moduleDir, "$basePackagePath/StopWatch3ViewModel.kt")
        assertTrue(viewModelFile.exists(), "StopWatch3ViewModel.kt should exist in base package")
        assertFalse(File(generatedDir, "StopWatch3ViewModel.kt").exists(),
            "StopWatch3ViewModel.kt should NOT be in generated/")

        // 4. filesCreated includes all 9 files
        assertEquals(9, result.filesCreated.size,
            "First save should report 9 files created (2 gradle + 1 flow.kt + 4 runtime + 1 ViewModel + 1 UI stub)")

        // 5. No overwrites or deletions on first save
        assertTrue(result.filesOverwritten.isEmpty(), "First save should have 0 overwritten")
        assertTrue(result.filesDeleted.isEmpty(), "First save should have 0 deleted")
    }

    // ========== T008: Re-Save Integration Tests (US2) ==========

    @Test
    fun `T008 - re-save overwrites flow kt with updated content`() {
        // First save with 2 nodes
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.SOURCE,
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
    fun `T008 - re-save overwrites all 4 runtime files and ViewModel`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.SOURCE,
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
            "ReSaveTestControllerInterface.kt", "ReSaveTestControllerAdapter.kt"
        )
        for (fileName in runtimeFiles) {
            assertTrue(result2.filesOverwritten.any { it.contains(fileName) },
                "$fileName should be in filesOverwritten on re-save")
        }
        assertTrue(result2.filesOverwritten.any { it.contains("ReSaveTestViewModel.kt") },
            "ReSaveTestViewModel.kt should be in filesOverwritten on re-save")
    }

    @Test
    fun `T008 - re-save result matches quickstart Step 2 counts`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.SOURCE,
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
        assertEquals(9, result1.filesCreated.size,
            "Step 1: 9 files created (2 gradle + 1 flow.kt + 4 runtime + 1 ViewModel + 1 UI stub)")

        // Step 2: Add Logger node (SINK), re-save
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
        // Step 2: 0 files created, 6 files overwritten, 0 deleted
        assertEquals(0, result2.filesCreated.size,
            "Step 2: 0 new files created")
        assertEquals(6, result2.filesOverwritten.size,
            "Step 2: 6 files overwritten (flow.kt + 4 runtime + 1 ViewModel)")
        assertEquals(0, result2.filesDeleted.size,
            "Step 2: 0 files deleted")

        // Verify the 6 overwritten files are flow.kt + 4 runtime + ViewModel
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3.flow.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3Flow.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3Controller.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3ControllerInterface.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3ControllerAdapter.kt") })
        assertTrue(result2.filesOverwritten.any { it.contains("StopWatch3ViewModel.kt") })
    }

    // ========== T010: Name Change Integration (Quickstart Step 4) ==========

    @Test
    fun `T010 - save under new name creates new module and preserves original`() {
        val timerEmitter = CodeNode(
            id = "timer", name = "TimerEmitter", codeNodeType = CodeNodeType.SOURCE,
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
        assertTrue(File(betaDir, "src/commonMain/kotlin/io/codenode/beta/BetaViewModel.kt").exists(),
            "Beta ViewModel stub should exist in base package")

        // Alpha module is untouched
        assertTrue(alphaDir.exists(), "Alpha module directory should still exist")
        assertTrue(alphaFlowKt.exists(), "Alpha .flow.kt should still exist")
        assertEquals(alphaFlowKtContent, alphaFlowKt.readText(),
            "Alpha .flow.kt content should be unchanged")
        assertTrue(alphaGeneratedDir.exists(), "Alpha generated dir should still exist")
    }
}
