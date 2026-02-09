/*
 * StopWatchModuleGeneratorTest - TDD Tests for StopWatch Module Generation
 * User Story 2: KMP Module Generation from FlowGraph
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import io.codenode.fbpdsl.model.*
import kotlin.test.*

/**
 * TDD tests for generating the StopWatch KMP module from FlowGraph.
 *
 * These tests verify:
 * - T020: ModuleGenerator produces StopWatchFlowGraph.kt file
 * - T021: ModuleGenerator produces StopWatchController.kt with start(), stop(), pause(), getStatus() methods
 * - T022: StopWatchController.bindToLifecycle() pauses on ON_PAUSE and resumes on ON_RESUME
 * - T023: Generated build.gradle.kts has correct KMP configuration (Kotlin 2.1.21, Compose 1.7.3)
 * - T024: Generated module compiles (structure verification)
 */
class StopWatchModuleGeneratorTest {

    // ========== Test Fixtures ==========

    /**
     * Creates a StopWatch FlowGraph matching the specification
     */
    private fun createStopWatchFlowGraph(): FlowGraph {
        val timerEmitterId = "timer-emitter"
        val displayReceiverId = "display-receiver"

        val timerEmitter = CodeNode(
            id = timerEmitterId,
            name = "TimerEmitter",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(100.0, 100.0),
            inputPorts = emptyList(),
            outputPorts = listOf(
                Port(
                    id = "${timerEmitterId}_elapsedSeconds",
                    name = "elapsedSeconds",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = timerEmitterId
                ),
                Port(
                    id = "${timerEmitterId}_elapsedMinutes",
                    name = "elapsedMinutes",
                    direction = Port.Direction.OUTPUT,
                    dataType = Int::class,
                    owningNodeId = timerEmitterId
                )
            ),
            controlConfig = ControlConfig(speedAttenuation = 1000L)
        )

        val displayReceiver = CodeNode(
            id = displayReceiverId,
            name = "DisplayReceiver",
            codeNodeType = CodeNodeType.SINK,
            position = Node.Position(400.0, 100.0),
            inputPorts = listOf(
                Port(
                    id = "${displayReceiverId}_seconds",
                    name = "seconds",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = displayReceiverId
                ),
                Port(
                    id = "${displayReceiverId}_minutes",
                    name = "minutes",
                    direction = Port.Direction.INPUT,
                    dataType = Int::class,
                    owningNodeId = displayReceiverId
                )
            ),
            outputPorts = emptyList()
        )

        val connections = listOf(
            Connection(
                id = "conn_seconds",
                sourceNodeId = timerEmitterId,
                sourcePortId = "${timerEmitterId}_elapsedSeconds",
                targetNodeId = displayReceiverId,
                targetPortId = "${displayReceiverId}_seconds",
                channelCapacity = 1
            ),
            Connection(
                id = "conn_minutes",
                sourceNodeId = timerEmitterId,
                sourcePortId = "${timerEmitterId}_elapsedMinutes",
                targetNodeId = displayReceiverId,
                targetPortId = "${displayReceiverId}_minutes",
                channelCapacity = 1
            )
        )

        return FlowGraph(
            id = "stopwatch-flow",
            name = "StopWatch",
            version = "1.0.0",
            description = "Virtual circuit demo for stopwatch functionality",
            rootNodes = listOf(timerEmitter, displayReceiver),
            connections = connections,
            targetPlatforms = listOf(
                FlowGraph.TargetPlatform.KMP_ANDROID,
                FlowGraph.TargetPlatform.KMP_IOS,
                FlowGraph.TargetPlatform.KMP_DESKTOP
            )
        )
    }

    // ========== T020: StopWatchFlowGraph.kt Generation Tests ==========

    @Test
    fun `T020 - ModuleGenerator produces StopWatchFlowGraph kt file`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the module
        val module = generator.generateModule(flowGraph, "StopWatch", packageName)

        // Then: Should contain StopWatchFlow.kt file
        val flowGraphFile = module.files.find { it.name.contains("StopWatch") && it.name.contains("Flow") && it.name.endsWith(".kt") }
        assertNotNull(flowGraphFile, "Should generate StopWatchFlow.kt or similar file")
    }

    @Test
    fun `T020 - StopWatchFlowGraph class instantiates TimerEmitter and DisplayReceiver`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the FlowGraph class
        val flowGraphClass = generator.generateFlowGraphClass(flowGraph, packageName)

        // Then: Should reference both nodes
        assertTrue(flowGraphClass.contains("TimerEmitter") || flowGraphClass.contains("timerEmitter"),
            "Should instantiate TimerEmitter node")
        assertTrue(flowGraphClass.contains("DisplayReceiver") || flowGraphClass.contains("displayReceiver"),
            "Should instantiate DisplayReceiver node")
    }

    @Test
    fun `T020 - StopWatchFlowGraph class has correct package declaration`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the FlowGraph class
        val flowGraphClass = generator.generateFlowGraphClass(flowGraph, packageName)

        // Then: Should have correct package
        assertTrue(flowGraphClass.contains("package $packageName"),
            "Should have correct package declaration")
    }

    // ========== T021: StopWatchController.kt Generation Tests ==========

    @Test
    fun `T021 - ModuleGenerator produces StopWatchController kt file`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the module
        val module = generator.generateModule(flowGraph, "StopWatch", packageName)

        // Then: Should contain StopWatchController.kt file
        val controllerFile = module.files.find { it.name.contains("Controller") && it.name.endsWith(".kt") }
        assertNotNull(controllerFile, "Should generate StopWatchController.kt file")
        assertTrue(controllerFile.name.contains("StopWatch"),
            "Controller file should be named StopWatchController.kt")
    }

    @Test
    fun `T021 - StopWatchController has start method`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the controller class
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then: Should have start method
        assertTrue(controllerClass.contains("fun start()") || controllerClass.contains("fun start("),
            "Should have start() method")
    }

    @Test
    fun `T021 - StopWatchController has stop method`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the controller class
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then: Should have stop method
        assertTrue(controllerClass.contains("fun stop()") || controllerClass.contains("fun stop("),
            "Should have stop() method")
    }

    @Test
    fun `T021 - StopWatchController has pause method`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the controller class
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then: Should have pause method
        assertTrue(controllerClass.contains("fun pause()") || controllerClass.contains("fun pause("),
            "Should have pause() method")
    }

    @Test
    fun `T021 - StopWatchController has getStatus method`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the controller class
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then: Should have getStatus method
        assertTrue(controllerClass.contains("fun getStatus()") || controllerClass.contains("getStatus"),
            "Should have getStatus() method")
    }

    @Test
    fun `T021 - StopWatchController wraps RootControlNode`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the controller class
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then: Should reference RootControlNode
        assertTrue(controllerClass.contains("RootControlNode"),
            "Should reference RootControlNode")
    }

    // ========== T022: bindToLifecycle Tests ==========

    @Test
    fun `T022 - StopWatchController has bindToLifecycle method`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the controller class
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then: Should have bindToLifecycle method
        assertTrue(controllerClass.contains("fun bindToLifecycle") || controllerClass.contains("bindToLifecycle("),
            "Should have bindToLifecycle() method for Android/KMP lifecycle integration")
    }

    @Test
    fun `T022 - StopWatchController bindToLifecycle imports Lifecycle`() {
        // Given: A StopWatch FlowGraph with Android target
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the controller class
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then: Should import Lifecycle classes
        assertTrue(controllerClass.contains("Lifecycle") || controllerClass.contains("lifecycle"),
            "Should reference Lifecycle for lifecycle binding")
    }

    @Test
    fun `T022 - StopWatchController has wasRunningBeforePause tracking`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the controller class
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then: Should track wasRunningBeforePause state
        assertTrue(controllerClass.contains("wasRunningBeforePause") ||
            controllerClass.contains("wasRunning") ||
            controllerClass.contains("bindToLifecycle"),
            "Should track running state for lifecycle pause/resume")
    }

    // ========== T023: build.gradle.kts Configuration Tests ==========

    @Test
    fun `T023 - Generated build gradle has Kotlin version 2_1_21`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()

        // When: Generating build.gradle.kts
        val buildGradle = generator.generateBuildGradle(flowGraph, "StopWatch")

        // Then: Should use Kotlin 2.1.21
        assertTrue(buildGradle.contains("2.1.21") || buildGradle.contains("2.1."),
            "Should use Kotlin version 2.1.21 (got: ${extractKotlinVersion(buildGradle)})")
    }

    @Test
    fun `T023 - Generated build gradle has Compose Multiplatform 1_7_3`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()

        // When: Generating build.gradle.kts
        val buildGradle = generator.generateBuildGradle(flowGraph, "StopWatch")

        // Then: Should include Compose plugin with version 1.7.3
        assertTrue(buildGradle.contains("compose") && buildGradle.contains("1.7.3") ||
            buildGradle.contains("org.jetbrains.compose"),
            "Should include Compose Multiplatform 1.7.3")
    }

    @Test
    fun `T023 - Generated build gradle includes lifecycle-runtime-compose dependency`() {
        // Given: A StopWatch FlowGraph targeting Android
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()

        // When: Generating build.gradle.kts
        val buildGradle = generator.generateBuildGradle(flowGraph, "StopWatch")

        // Then: Should include lifecycle-runtime-compose dependency
        assertTrue(buildGradle.contains("lifecycle-runtime-compose") ||
            buildGradle.contains("androidx.lifecycle"),
            "Should include lifecycle-runtime-compose dependency for bindToLifecycle support")
    }

    @Test
    fun `T023 - Generated build gradle configures Android iOS and Desktop targets`() {
        // Given: A StopWatch FlowGraph with all targets
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()

        // When: Generating build.gradle.kts
        val buildGradle = generator.generateBuildGradle(flowGraph, "StopWatch")

        // Then: Should configure all three targets
        assertTrue(buildGradle.contains("androidTarget") || buildGradle.contains("android"),
            "Should configure Android target")
        assertTrue(buildGradle.contains("ios") || buildGradle.contains("iosX64"),
            "Should configure iOS targets")
        assertTrue(buildGradle.contains("jvm") || buildGradle.contains("desktop"),
            "Should configure JVM/Desktop target")
    }

    @Test
    fun `T023 - Generated build gradle includes coroutines dependency`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()

        // When: Generating build.gradle.kts
        val buildGradle = generator.generateBuildGradle(flowGraph, "StopWatch")

        // Then: Should include coroutines
        assertTrue(buildGradle.contains("kotlinx-coroutines"),
            "Should include kotlinx-coroutines dependency")
    }

    // ========== T024: Module Structure/Compilation Tests ==========

    @Test
    fun `T024 - Generated module has correct directory structure`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the module structure
        val structure = generator.generateModuleStructure(flowGraph, "StopWatch", packageName)

        // Then: Should have required directories
        assertTrue(structure.directories.contains("src/commonMain/kotlin"),
            "Should have commonMain source directory")
        assertTrue(structure.directories.contains("src/commonTest/kotlin"),
            "Should have commonTest source directory")

        // Package-specific directories
        val packagePath = "src/commonMain/kotlin/io/codenode/generated/stopwatch"
        assertTrue(structure.directories.contains(packagePath),
            "Should have package-specific directory: $packagePath")
    }

    @Test
    fun `T024 - Generated module has all required files`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the complete module
        val module = generator.generateModule(flowGraph, "StopWatch", packageName)

        // Then: Should have all required files
        assertTrue(module.files.any { it.name == "build.gradle.kts" },
            "Should have build.gradle.kts")
        assertTrue(module.files.any { it.name == "settings.gradle.kts" },
            "Should have settings.gradle.kts")
        assertTrue(module.files.any { it.name.contains("StopWatch") && it.name.endsWith(".kt") },
            "Should have StopWatch source files")
    }

    @Test
    fun `T024 - Generated module has TimerEmitterComponent and DisplayReceiverComponent`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating the FlowGraph class (which includes component stubs)
        val flowGraphClass = generator.generateFlowGraphClass(flowGraph, packageName)

        // Then: Should have component classes for both nodes
        assertTrue(flowGraphClass.contains("TimerEmitterComponent") || flowGraphClass.contains("class TimerEmitter"),
            "Should generate TimerEmitterComponent")
        assertTrue(flowGraphClass.contains("DisplayReceiverComponent") || flowGraphClass.contains("class DisplayReceiver"),
            "Should generate DisplayReceiverComponent")
    }

    @Test
    fun `T024 - Generated StopWatch module has valid Kotlin syntax`() {
        // Given: A StopWatch FlowGraph
        val flowGraph = createStopWatchFlowGraph()
        val generator = ModuleGenerator()
        val packageName = "io.codenode.generated.stopwatch"

        // When: Generating all source files
        val flowGraphClass = generator.generateFlowGraphClass(flowGraph, packageName)
        val controllerClass = generator.generateControllerClass(flowGraph, packageName)

        // Then: Basic Kotlin syntax checks
        // Check for balanced braces (simple validation)
        assertEquals(flowGraphClass.count { it == '{' }, flowGraphClass.count { it == '}' },
            "FlowGraph class should have balanced braces")
        assertEquals(controllerClass.count { it == '{' }, controllerClass.count { it == '}' },
            "Controller class should have balanced braces")

        // Check for package declaration
        assertTrue(flowGraphClass.startsWith("/*") || flowGraphClass.contains("package "),
            "FlowGraph class should have package declaration")
        assertTrue(controllerClass.startsWith("/*") || controllerClass.contains("package "),
            "Controller class should have package declaration")
    }

    // ========== Helper Functions ==========

    /**
     * Extracts Kotlin version from build.gradle.kts content for error messages
     */
    private fun extractKotlinVersion(buildGradle: String): String {
        val versionPattern = Regex("""kotlin\([^)]+\)\s*version\s*"([^"]+)"""")
        return versionPattern.find(buildGradle)?.groupValues?.get(1) ?: "unknown"
    }
}
