/*
 * StopWatchModuleGenerationTest - Generates the StopWatch KMP module
 * T025-T028: Implementation tasks for User Story 2
 * License: Apache 2.0
 */

package io.codenode.grapheditor.compilation

import io.codenode.grapheditor.serialization.FlowGraphDeserializer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for generating the actual StopWatch module from StopWatch.flow
 *
 * T025: Generate StopWatch/ module directory from demos/stopwatch/StopWatch.flow
 * T026: Verify lifecycle-runtime-compose dependency is included
 * T027: Verify StopWatchFlowGraph.kt instantiates TimerEmitter and DisplayReceiver
 * T028: Verify StopWatchController.kt has start(), stop(), pause(), getStatus() methods
 */
class StopWatchModuleGenerationTest {

    companion object {
        /**
         * Find the project root directory by looking for settings.gradle.kts
         */
        private fun findProjectRoot(): File {
            var dir = File(System.getProperty("user.dir"))
            while (dir.parentFile != null) {
                if (File(dir, "settings.gradle.kts").exists()) {
                    return dir
                }
                dir = dir.parentFile
            }
            return File(System.getProperty("user.dir"))
        }

        private val PROJECT_ROOT = findProjectRoot()
        private val STOPWATCH_FLOW_FILE = File(PROJECT_ROOT, "demos/stopwatch/StopWatch.flow")
        private val OUTPUT_DIR = PROJECT_ROOT
        private const val PACKAGE_NAME = "io.codenode.generated.stopwatch"
    }

    @Test
    fun `T025 - Generate StopWatch module from StopWatch flow file`() {
        // Given: The StopWatch.flow file exists
        assertTrue(STOPWATCH_FLOW_FILE.exists(),
            "StopWatch.flow should exist at ${STOPWATCH_FLOW_FILE.absolutePath}")

        // When: Loading and compiling the FlowGraph
        val dslContent = STOPWATCH_FLOW_FILE.readText()
        val deserializeResult = FlowGraphDeserializer.deserialize(dslContent)

        assertTrue(deserializeResult.isSuccess,
            "Failed to deserialize StopWatch.flow: ${deserializeResult.errorMessage}")
        assertNotNull(deserializeResult.graph)

        val flowGraph = deserializeResult.graph!!
        assertEquals("StopWatch", flowGraph.name, "FlowGraph name should be StopWatch")

        // Compile to module
        val compilationService = CompilationService()
        val result = compilationService.compileToModule(
            flowGraph = flowGraph,
            outputDir = OUTPUT_DIR,
            moduleName = "StopWatch",
            packageName = PACKAGE_NAME
        )

        // Then: Compilation should succeed
        assertTrue(result.success, "Compilation failed: ${result.errorMessage}")
        assertNotNull(result.outputPath)
        assertTrue(result.fileCount > 0, "Should generate at least one file")

        // Verify module directory was created
        val moduleDir = File(OUTPUT_DIR, "StopWatch")
        assertTrue(moduleDir.exists(), "StopWatch module directory should exist")
        assertTrue(moduleDir.isDirectory, "StopWatch should be a directory")

        println("T025: Generated StopWatch module at ${result.outputPath} with ${result.fileCount} files")
    }

    @Test
    fun `T026 - Generated build gradle includes lifecycle-runtime-compose`() {
        // Given: A generated StopWatch module
        val moduleDir = generateStopWatchModule()

        // When: Reading the generated build.gradle.kts
        val buildGradleFile = File(moduleDir, "build.gradle.kts")
        assertTrue(buildGradleFile.exists(), "build.gradle.kts should exist")

        val buildGradleContent = buildGradleFile.readText()

        // Then: Should include lifecycle-runtime-compose dependency
        assertTrue(buildGradleContent.contains("lifecycle-runtime-compose"),
            "build.gradle.kts should include lifecycle-runtime-compose dependency")

        // And: Should include Compose plugin
        assertTrue(buildGradleContent.contains("org.jetbrains.compose"),
            "build.gradle.kts should include Compose plugin")

        println("T026: Verified lifecycle-runtime-compose dependency in build.gradle.kts")
    }

    @Test
    fun `T027 - Generated StopWatchFlow kt instantiates TimerEmitter and DisplayReceiver`() {
        // Given: A generated StopWatch module
        val moduleDir = generateStopWatchModule()

        // When: Reading the generated StopWatchFlow.kt
        val flowFile = File(moduleDir, "src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchFlow.kt")
        assertTrue(flowFile.exists(), "StopWatchFlow.kt should exist at ${flowFile.absolutePath}")

        val flowContent = flowFile.readText()

        // Then: Should instantiate both nodes
        assertTrue(flowContent.contains("timerEmitter") || flowContent.contains("TimerEmitter"),
            "StopWatchFlow.kt should instantiate TimerEmitter")
        assertTrue(flowContent.contains("displayReceiver") || flowContent.contains("DisplayReceiver"),
            "StopWatchFlow.kt should instantiate DisplayReceiver")

        // And: Should have component classes
        assertTrue(flowContent.contains("TimerEmitterComponent"),
            "Should generate TimerEmitterComponent class")
        assertTrue(flowContent.contains("DisplayReceiverComponent"),
            "Should generate DisplayReceiverComponent class")

        // And: Should have correct package
        assertTrue(flowContent.contains("package $PACKAGE_NAME"),
            "Should have correct package declaration")

        println("T027: Verified StopWatchFlow.kt instantiates TimerEmitter and DisplayReceiver")
    }

    @Test
    fun `T028 - Generated StopWatchController kt has required methods`() {
        // Given: A generated StopWatch module
        val moduleDir = generateStopWatchModule()

        // When: Reading the generated StopWatchController.kt
        val controllerFile = File(moduleDir, "src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchController.kt")
        assertTrue(controllerFile.exists(), "StopWatchController.kt should exist at ${controllerFile.absolutePath}")

        val controllerContent = controllerFile.readText()

        // Then: Should have start() method
        assertTrue(controllerContent.contains("fun start()") || controllerContent.contains("fun start("),
            "StopWatchController.kt should have start() method")

        // And: Should have stop() method
        assertTrue(controllerContent.contains("fun stop()") || controllerContent.contains("fun stop("),
            "StopWatchController.kt should have stop() method")

        // And: Should have pause() method
        assertTrue(controllerContent.contains("fun pause()") || controllerContent.contains("fun pause("),
            "StopWatchController.kt should have pause() method")

        // And: Should have getStatus() method
        assertTrue(controllerContent.contains("fun getStatus()") || controllerContent.contains("getStatus"),
            "StopWatchController.kt should have getStatus() method")

        // And: Should wrap RootControlNode
        assertTrue(controllerContent.contains("RootControlNode"),
            "StopWatchController.kt should reference RootControlNode")

        // And: Should have bindToLifecycle method
        assertTrue(controllerContent.contains("bindToLifecycle"),
            "StopWatchController.kt should have bindToLifecycle() method")

        println("T028: Verified StopWatchController.kt has start(), stop(), pause(), getStatus(), bindToLifecycle()")
    }

    @Test
    fun `T028 - Generated StopWatchController kt wraps RootControlNode correctly`() {
        // Given: A generated StopWatch module
        val moduleDir = generateStopWatchModule()

        // When: Reading the generated StopWatchController.kt
        val controllerFile = File(moduleDir, "src/commonMain/kotlin/io/codenode/generated/stopwatch/StopWatchController.kt")
        val controllerContent = controllerFile.readText()

        // Then: Should import RootControlNode
        assertTrue(controllerContent.contains("import io.codenode.fbpdsl.model.RootControlNode"),
            "Should import RootControlNode")

        // And: Should create RootControlNode instance
        assertTrue(controllerContent.contains("RootControlNode.createFor"),
            "Should create RootControlNode using createFor factory")

        // And: Should delegate to RootControlNode methods
        assertTrue(controllerContent.contains("controller.startAll()") || controllerContent.contains("startAll"),
            "Should delegate start to RootControlNode.startAll()")
        assertTrue(controllerContent.contains("controller.stopAll()") || controllerContent.contains("stopAll"),
            "Should delegate stop to RootControlNode.stopAll()")
        assertTrue(controllerContent.contains("controller.pauseAll()") || controllerContent.contains("pauseAll"),
            "Should delegate pause to RootControlNode.pauseAll()")

        println("T028: Verified StopWatchController.kt properly wraps RootControlNode")
    }

    /**
     * Helper function to generate the StopWatch module and return the module directory
     */
    private fun generateStopWatchModule(): File {
        assertTrue(STOPWATCH_FLOW_FILE.exists(),
            "StopWatch.flow should exist at ${STOPWATCH_FLOW_FILE.absolutePath}")

        val dslContent = STOPWATCH_FLOW_FILE.readText()
        val deserializeResult = FlowGraphDeserializer.deserialize(dslContent)

        assertTrue(deserializeResult.isSuccess,
            "Failed to deserialize StopWatch.flow: ${deserializeResult.errorMessage}")

        val flowGraph = deserializeResult.graph!!

        val compilationService = CompilationService()
        val result = compilationService.compileToModule(
            flowGraph = flowGraph,
            outputDir = OUTPUT_DIR,
            moduleName = "StopWatch",
            packageName = PACKAGE_NAME
        )

        assertTrue(result.success, "Compilation failed: ${result.errorMessage}")

        return File(OUTPUT_DIR, "StopWatch")
    }
}
