/*
 * RuntimeRegistryTest - Unit tests for RuntimeRegistry
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.ControlConfig
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.Node
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RuntimeRegistryTest {

    private fun createTestCodeNode(
        id: String,
        independentControl: Boolean = false
    ): CodeNode {
        return CodeNode(
            id = id,
            name = "Test Node $id",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position.ORIGIN,
            controlConfig = ControlConfig(independentControl = independentControl)
        )
    }

    private fun createTestRuntime(
        id: String,
        independentControl: Boolean = false
    ): NodeRuntime {
        return NodeRuntime(createTestCodeNode(id, independentControl))
    }

    // ========== Registration Tests ==========

    @Test
    fun register_addsRuntimeToRegistry() {
        val registry = RuntimeRegistry()
        val runtime = createTestRuntime("node1")

        registry.register(runtime)

        assertEquals(1, registry.count)
        assertTrue(registry.isRegistered("node1"))
    }

    @Test
    fun register_multipleRuntimes_allTracked() {
        val registry = RuntimeRegistry()
        val runtime1 = createTestRuntime("node1")
        val runtime2 = createTestRuntime("node2")
        val runtime3 = createTestRuntime("node3")

        registry.register(runtime1)
        registry.register(runtime2)
        registry.register(runtime3)

        assertEquals(3, registry.count)
        assertTrue(registry.isRegistered("node1"))
        assertTrue(registry.isRegistered("node2"))
        assertTrue(registry.isRegistered("node3"))
    }

    @Test
    fun register_sameIdTwice_replacesExisting() {
        val registry = RuntimeRegistry()
        val runtime1 = createTestRuntime("node1")
        val runtime2 = createTestRuntime("node1") // Same ID

        registry.register(runtime1)
        registry.register(runtime2)

        assertEquals(1, registry.count)
        // Should be the second runtime
        assertEquals(runtime2, registry.get("node1"))
    }

    @Test
    fun unregister_removesRuntimeFromRegistry() {
        val registry = RuntimeRegistry()
        val runtime = createTestRuntime("node1")

        registry.register(runtime)
        assertEquals(1, registry.count)

        registry.unregister(runtime)

        assertEquals(0, registry.count)
        assertFalse(registry.isRegistered("node1"))
    }

    @Test
    fun unregister_nonexistentRuntime_noError() {
        val registry = RuntimeRegistry()
        val runtime = createTestRuntime("node1")

        // Should not throw
        registry.unregister(runtime)

        assertEquals(0, registry.count)
    }

    // ========== Lookup Tests ==========

    @Test
    fun get_existingRuntime_returnsRuntime() {
        val registry = RuntimeRegistry()
        val runtime = createTestRuntime("node1")

        registry.register(runtime)

        val retrieved = registry.get("node1")
        assertNotNull(retrieved)
        assertEquals(runtime, retrieved)
    }

    @Test
    fun get_nonexistentRuntime_returnsNull() {
        val registry = RuntimeRegistry()

        val retrieved = registry.get("nonexistent")
        assertNull(retrieved)
    }

    @Test
    fun isRegistered_existingRuntime_returnsTrue() {
        val registry = RuntimeRegistry()
        val runtime = createTestRuntime("node1")

        registry.register(runtime)

        assertTrue(registry.isRegistered("node1"))
    }

    @Test
    fun isRegistered_nonexistentRuntime_returnsFalse() {
        val registry = RuntimeRegistry()

        assertFalse(registry.isRegistered("nonexistent"))
    }

    // ========== PauseAll Tests ==========

    @Test
    fun pauseAll_pausesAllRunningRuntimes() {
        val registry = RuntimeRegistry()
        val runtime1 = createTestRuntime("node1")
        val runtime2 = createTestRuntime("node2")

        // Set to RUNNING state (pause only works when RUNNING)
        runtime1.executionState = ExecutionState.RUNNING
        runtime2.executionState = ExecutionState.RUNNING

        registry.register(runtime1)
        registry.register(runtime2)

        registry.pauseAll()

        assertEquals(ExecutionState.PAUSED, runtime1.executionState)
        assertEquals(ExecutionState.PAUSED, runtime2.executionState)
    }

    @Test
    fun pauseAll_skipsIndependentControlRuntimes() {
        val registry = RuntimeRegistry()
        val normalRuntime = createTestRuntime("normal", independentControl = false)
        val independentRuntime = createTestRuntime("independent", independentControl = true)

        normalRuntime.executionState = ExecutionState.RUNNING
        independentRuntime.executionState = ExecutionState.RUNNING

        registry.register(normalRuntime)
        registry.register(independentRuntime)

        registry.pauseAll()

        // Normal runtime should be paused
        assertEquals(ExecutionState.PAUSED, normalRuntime.executionState)
        // Independent runtime should still be running
        assertEquals(ExecutionState.RUNNING, independentRuntime.executionState)
    }

    @Test
    fun pauseAll_emptyRegistry_noError() {
        val registry = RuntimeRegistry()

        // Should not throw
        registry.pauseAll()
    }

    // ========== ResumeAll Tests ==========

    @Test
    fun resumeAll_resumesAllPausedRuntimes() {
        val registry = RuntimeRegistry()
        val runtime1 = createTestRuntime("node1")
        val runtime2 = createTestRuntime("node2")

        // Set to PAUSED state (resume only works when PAUSED)
        runtime1.executionState = ExecutionState.PAUSED
        runtime2.executionState = ExecutionState.PAUSED

        registry.register(runtime1)
        registry.register(runtime2)

        registry.resumeAll()

        assertEquals(ExecutionState.RUNNING, runtime1.executionState)
        assertEquals(ExecutionState.RUNNING, runtime2.executionState)
    }

    @Test
    fun resumeAll_skipsIndependentControlRuntimes() {
        val registry = RuntimeRegistry()
        val normalRuntime = createTestRuntime("normal", independentControl = false)
        val independentRuntime = createTestRuntime("independent", independentControl = true)

        normalRuntime.executionState = ExecutionState.PAUSED
        independentRuntime.executionState = ExecutionState.PAUSED

        registry.register(normalRuntime)
        registry.register(independentRuntime)

        registry.resumeAll()

        // Normal runtime should be resumed
        assertEquals(ExecutionState.RUNNING, normalRuntime.executionState)
        // Independent runtime should still be paused
        assertEquals(ExecutionState.PAUSED, independentRuntime.executionState)
    }

    @Test
    fun resumeAll_emptyRegistry_noError() {
        val registry = RuntimeRegistry()

        // Should not throw
        registry.resumeAll()
    }

    // ========== StopAll Tests ==========

    @Test
    fun stopAll_stopsAllRuntimesAndClearsRegistry() {
        val registry = RuntimeRegistry()
        val runtime1 = createTestRuntime("node1")
        val runtime2 = createTestRuntime("node2")

        runtime1.executionState = ExecutionState.RUNNING
        runtime2.executionState = ExecutionState.RUNNING

        registry.register(runtime1)
        registry.register(runtime2)

        registry.stopAll()

        // Runtimes should be stopped
        assertEquals(ExecutionState.IDLE, runtime1.executionState)
        assertEquals(ExecutionState.IDLE, runtime2.executionState)

        // Registry should be cleared
        assertEquals(0, registry.count)
    }

    @Test
    fun stopAll_emptyRegistry_noError() {
        val registry = RuntimeRegistry()

        // Should not throw
        registry.stopAll()
    }

    // ========== Clear Tests ==========

    @Test
    fun clear_removesAllRegistrationsWithoutStoppingRuntimes() {
        val registry = RuntimeRegistry()
        val runtime1 = createTestRuntime("node1")
        val runtime2 = createTestRuntime("node2")

        runtime1.executionState = ExecutionState.RUNNING
        runtime2.executionState = ExecutionState.RUNNING

        registry.register(runtime1)
        registry.register(runtime2)

        registry.clear()

        // Registry should be cleared
        assertEquals(0, registry.count)

        // But runtimes should NOT be stopped
        assertEquals(ExecutionState.RUNNING, runtime1.executionState)
        assertEquals(ExecutionState.RUNNING, runtime2.executionState)
    }

    // ========== Count Tests ==========

    @Test
    fun count_emptyRegistry_returnsZero() {
        val registry = RuntimeRegistry()

        assertEquals(0, registry.count)
    }

    @Test
    fun count_afterRegistrations_returnsCorrectCount() {
        val registry = RuntimeRegistry()

        registry.register(createTestRuntime("node1"))
        assertEquals(1, registry.count)

        registry.register(createTestRuntime("node2"))
        assertEquals(2, registry.count)

        registry.register(createTestRuntime("node3"))
        assertEquals(3, registry.count)
    }

    @Test
    fun count_afterUnregister_decreases() {
        val registry = RuntimeRegistry()
        val runtime1 = createTestRuntime("node1")
        val runtime2 = createTestRuntime("node2")

        registry.register(runtime1)
        registry.register(runtime2)
        assertEquals(2, registry.count)

        registry.unregister(runtime1)
        assertEquals(1, registry.count)
    }

    // ========== Thread-Safety / Concurrency Tests ==========

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun concurrentRegisterUnregister_noExceptions() = runTest {
        val registry = RuntimeRegistry()
        val runtimes = (1..100).map { createTestRuntime("node$it") }

        // Launch many concurrent register operations
        val registerJobs = runtimes.map { runtime ->
            launch {
                registry.register(runtime)
            }
        }

        // Wait for all registrations
        registerJobs.forEach { it.join() }

        // All should be registered
        assertEquals(100, registry.count)

        // Launch many concurrent unregister operations
        val unregisterJobs = runtimes.map { runtime ->
            launch {
                registry.unregister(runtime)
            }
        }

        // Wait for all unregistrations
        unregisterJobs.forEach { it.join() }

        // All should be unregistered
        assertEquals(0, registry.count)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun concurrentPauseAllWhileRegistering_noExceptions() = runTest {
        val registry = RuntimeRegistry()
        val runtimes = (1..50).map { createTestRuntime("node$it") }

        // Set all to RUNNING so pause will work
        runtimes.forEach { it.executionState = ExecutionState.RUNNING }

        // Launch concurrent registrations and pauseAll calls
        val jobs = mutableListOf<kotlinx.coroutines.Job>()

        runtimes.forEachIndexed { index, runtime ->
            jobs += launch {
                registry.register(runtime)
            }
            // Intersperse pauseAll calls
            if (index % 10 == 5) {
                jobs += launch {
                    registry.pauseAll()
                }
            }
        }

        // Wait for all operations
        jobs.forEach { it.join() }

        // Should complete without exceptions - count may vary due to timing
        assertTrue(registry.count <= 50)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun concurrentResumeAllWhileUnregistering_noExceptions() = runTest {
        val registry = RuntimeRegistry()
        val runtimes = (1..50).map { createTestRuntime("node$it") }

        // Register all and set to PAUSED
        runtimes.forEach { runtime ->
            runtime.executionState = ExecutionState.PAUSED
            registry.register(runtime)
        }

        assertEquals(50, registry.count)

        // Launch concurrent unregistrations and resumeAll calls
        val jobs = mutableListOf<kotlinx.coroutines.Job>()

        runtimes.forEachIndexed { index, runtime ->
            jobs += launch {
                registry.unregister(runtime)
            }
            // Intersperse resumeAll calls
            if (index % 10 == 5) {
                jobs += launch {
                    registry.resumeAll()
                }
            }
        }

        // Wait for all operations
        jobs.forEach { it.join() }

        // Should complete without exceptions
        assertEquals(0, registry.count)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun concurrentStopAllWhileOperating_noExceptions() = runTest {
        val registry = RuntimeRegistry()
        val runtimes = (1..30).map { createTestRuntime("node$it") }

        // Register all and set to RUNNING
        runtimes.forEach { runtime ->
            runtime.executionState = ExecutionState.RUNNING
            registry.register(runtime)
        }

        // Launch concurrent operations including stopAll
        val jobs = mutableListOf<kotlinx.coroutines.Job>()

        jobs += launch { registry.pauseAll() }
        jobs += launch { registry.resumeAll() }
        jobs += launch { registry.stopAll() }
        jobs += launch { registry.pauseAll() }
        jobs += launch { registry.resumeAll() }

        // Wait for all operations
        jobs.forEach { it.join() }

        // stopAll should have cleared the registry
        assertEquals(0, registry.count)
    }
}
