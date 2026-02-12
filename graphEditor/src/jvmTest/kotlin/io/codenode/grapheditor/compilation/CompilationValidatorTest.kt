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
 *
 * T043: Update compile validation to check ProcessingLogic classes exist in module
 */
class CompilationValidatorTest {

    // ========== Test Fixtures ==========

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = createTempDir("compilation-validator-test")
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createTestCodeNode(
        id: String,
        name: String,
        type: CodeNodeType = CodeNodeType.TRANSFORMER,
        processingLogicClass: String? = null
    ): CodeNode {
        val config = mutableMapOf<String, String>()
        if (processingLogicClass != null) {
            config["_useCaseClass"] = processingLogicClass
        }
        return CodeNode(
            id = id,
            name = name,
            codeNodeType = type,
            position = Node.Position(100.0, 200.0),
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
            ),
            configuration = config
        )
    }

    private fun createTestFlowGraph(
        name: String = "TestFlow",
        nodes: List<Node> = listOf(
            createTestCodeNode("timer", "TimerEmitter", CodeNodeType.GENERATOR, "TimerEmitterComponent"),
            createTestCodeNode("display", "DisplayReceiver", CodeNodeType.SINK, "DisplayReceiverComponent")
        )
    ): FlowGraph {
        return FlowGraph(
            id = "flow_${name.lowercase()}",
            name = name,
            version = "1.0.0",
            rootNodes = nodes
        )
    }

    private fun setupModuleStructure(
        moduleDir: File,
        packageName: String,
        componentFiles: List<String> = emptyList()
    ) {
        // Create directory structure
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")
        sourceDir.mkdirs()

        // Create build.gradle.kts
        File(moduleDir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("multiplatform")
            }
        """.trimIndent())

        // Create component files
        componentFiles.forEach { fileName ->
            File(sourceDir, fileName).writeText("// Stub")
        }
    }

    // ========== T043: Validate ProcessingLogic Classes Exist ==========

    @Test
    fun `T043 - validateModule returns valid when all components exist`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.generated.stopwatch"

        setupModuleStructure(
            moduleDir,
            packageName,
            componentFiles = listOf("TimerEmitterComponent.kt", "DisplayReceiverComponent.kt")
        )

        val validator = CompilationValidator()

        // When
        val result = validator.validateModule(flowGraph, moduleDir, packageName)

        // Then
        assertTrue(result.isValid, "Validation should pass when all components exist")
        assertTrue(result.errors.isEmpty(), "No errors expected")
    }

    @Test
    fun `T043 - validateModule returns invalid when components missing`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.generated.stopwatch"

        setupModuleStructure(
            moduleDir,
            packageName,
            componentFiles = listOf("TimerEmitterComponent.kt")
            // DisplayReceiverComponent.kt is missing
        )

        val validator = CompilationValidator()

        // When
        val result = validator.validateModule(flowGraph, moduleDir, packageName)

        // Then
        assertFalse(result.isValid, "Validation should fail when components are missing")
        assertTrue(result.errors.any { it.contains("DisplayReceiverComponent") },
            "Should report DisplayReceiverComponent as missing")
    }

    @Test
    fun `T043 - validateModule returns invalid when module directory missing`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "NonExistent")
        val packageName = "io.codenode.generated.stopwatch"
        // Don't create the module directory

        val validator = CompilationValidator()

        // When
        val result = validator.validateModule(flowGraph, moduleDir, packageName)

        // Then
        assertFalse(result.isValid, "Validation should fail when module directory missing")
        assertTrue(result.errors.any { it.contains("does not exist") },
            "Should report module directory as missing")
    }

    @Test
    fun `T043 - validateModule returns invalid when build gradle missing`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.generated.stopwatch"

        // Create directory without build.gradle.kts
        val packagePath = packageName.replace(".", "/")
        File(moduleDir, "src/commonMain/kotlin/$packagePath").mkdirs()
        File(moduleDir, "src/commonMain/kotlin/$packagePath/TimerEmitterComponent.kt").writeText("// Stub")
        File(moduleDir, "src/commonMain/kotlin/$packagePath/DisplayReceiverComponent.kt").writeText("// Stub")

        val validator = CompilationValidator()

        // When
        val result = validator.validateModule(flowGraph, moduleDir, packageName)

        // Then
        assertFalse(result.isValid, "Validation should fail when build.gradle.kts missing")
        assertTrue(result.errors.any { it.contains("build.gradle.kts") },
            "Should report build.gradle.kts as missing")
    }

    @Test
    fun `T043 - hasAllProcessingLogicClasses returns true when all exist`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.generated.stopwatch"

        setupModuleStructure(
            moduleDir,
            packageName,
            componentFiles = listOf("TimerEmitterComponent.kt", "DisplayReceiverComponent.kt")
        )

        val validator = CompilationValidator()

        // When
        val result = validator.hasAllProcessingLogicClasses(flowGraph, moduleDir, packageName)

        // Then
        assertTrue(result, "Should return true when all components exist")
    }

    @Test
    fun `T043 - hasAllProcessingLogicClasses returns false when some missing`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.generated.stopwatch"

        setupModuleStructure(
            moduleDir,
            packageName,
            componentFiles = listOf("TimerEmitterComponent.kt")
        )

        val validator = CompilationValidator()

        // When
        val result = validator.hasAllProcessingLogicClasses(flowGraph, moduleDir, packageName)

        // Then
        assertFalse(result, "Should return false when components are missing")
    }

    // ========== Orphaned Component Detection ==========

    @Test
    fun `findOrphanedComponents returns empty when all components used`() {
        // Given
        val flowGraph = createTestFlowGraph("StopWatch")
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.generated.stopwatch"

        setupModuleStructure(
            moduleDir,
            packageName,
            componentFiles = listOf("TimerEmitterComponent.kt", "DisplayReceiverComponent.kt")
        )

        val validator = CompilationValidator()
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        // When
        val orphaned = validator.findOrphanedComponents(flowGraph, sourceDir)

        // Then
        assertTrue(orphaned.isEmpty(), "No orphaned files expected")
    }

    @Test
    fun `findOrphanedComponents detects removed node components`() {
        // Given
        val flowGraph = createTestFlowGraph(
            "StopWatch",
            nodes = listOf(createTestCodeNode("timer", "TimerEmitter", processingLogicClass = "TimerEmitterComponent"))
        )
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.generated.stopwatch"

        // Include an extra component file that's no longer needed
        setupModuleStructure(
            moduleDir,
            packageName,
            componentFiles = listOf(
                "TimerEmitterComponent.kt",
                "OldRemovedComponent.kt" // This node was removed
            )
        )

        val validator = CompilationValidator()
        val packagePath = packageName.replace(".", "/")
        val sourceDir = File(moduleDir, "src/commonMain/kotlin/$packagePath")

        // When
        val orphaned = validator.findOrphanedComponents(flowGraph, sourceDir)

        // Then
        assertTrue(orphaned.contains("OldRemovedComponent.kt"),
            "Should detect orphaned component file")
    }

    @Test
    fun `validateModule includes warnings for orphaned components`() {
        // Given
        val flowGraph = createTestFlowGraph(
            "StopWatch",
            nodes = listOf(createTestCodeNode("timer", "TimerEmitter", processingLogicClass = "TimerEmitterComponent"))
        )
        val moduleDir = File(tempDir, "StopWatch")
        val packageName = "io.codenode.generated.stopwatch"

        setupModuleStructure(
            moduleDir,
            packageName,
            componentFiles = listOf("TimerEmitterComponent.kt", "OldComponent.kt")
        )

        val validator = CompilationValidator()

        // When
        val result = validator.validateModule(flowGraph, moduleDir, packageName)

        // Then
        assertTrue(result.isValid, "Should be valid (orphaned files are warnings, not errors)")
        assertTrue(result.warnings.any { it.contains("OldComponent") },
            "Should warn about orphaned component")
    }
}
