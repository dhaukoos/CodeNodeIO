/*
 * IndependentControlTest
 * Tests for independentControl flag behavior in RuntimeRegistry
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNode
import io.codenode.fbpdsl.model.CodeNodeType
import io.codenode.fbpdsl.model.ControlConfig
import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.fbpdsl.model.Node
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the independentControl flag behavior.
 *
 * When independentControl=false (default):
 * - Runtime responds to pauseAll()/resumeAll() from RuntimeRegistry
 * - State changes propagate from RootControlNode
 *
 * When independentControl=true:
 * - Runtime is skipped by pauseAll()/resumeAll()
 * - Must be controlled directly via its own pause()/resume() methods
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IndependentControlTest {

    private fun createCodeNode(
        id: String,
        type: CodeNodeType = CodeNodeType.GENERATOR,
        independentControl: Boolean = false
    ): CodeNode {
        return CodeNode(
            id = id,
            name = "Test Node $id",
            codeNodeType = type,
            position = Node.Position.ORIGIN,
            controlConfig = ControlConfig(independentControl = independentControl)
        )
    }

    // ========================================
    // T054: independentControl=true behavior
    // ========================================

    @Test
    fun generator_with_independentControl_true_is_skipped_by_pauseAll() = runTest {
        val registry = RuntimeRegistry()

        // Create generator with independentControl=true
        val independentGenerator = GeneratorRuntime<Int>(
            codeNode = createCodeNode("independent", independentControl = true)
        ) { emit ->
            while (true) {
                emit(1)
                delay(100)
            }
        }
        independentGenerator.outputChannel = Channel(Channel.BUFFERED)
        independentGenerator.registry = registry

        // Create normal generator (independentControl=false)
        val normalGenerator = GeneratorRuntime<Int>(
            codeNode = createCodeNode("normal", independentControl = false)
        ) { emit ->
            while (true) {
                emit(1)
                delay(100)
            }
        }
        normalGenerator.outputChannel = Channel(Channel.BUFFERED)
        normalGenerator.registry = registry

        // Start both
        independentGenerator.start(this) { }
        normalGenerator.start(this) { }
        advanceUntilIdle()

        // Verify both registered
        assertEquals(2, registry.count)
        assertTrue(independentGenerator.isRunning())
        assertTrue(normalGenerator.isRunning())

        // Pause all via registry
        registry.pauseAll()

        // Independent generator should still be RUNNING
        assertTrue(independentGenerator.isRunning())

        // Normal generator should be PAUSED
        assertTrue(normalGenerator.isPaused())

        // Cleanup
        independentGenerator.stop()
        normalGenerator.stop()
    }

    @Test
    fun generator_with_independentControl_true_is_skipped_by_resumeAll() = runTest {
        val registry = RuntimeRegistry()

        // Create generator with independentControl=true
        val generator = GeneratorRuntime<Int>(
            codeNode = createCodeNode("independent", independentControl = true)
        ) { emit ->
            while (true) {
                emit(1)
                delay(100)
            }
        }
        generator.outputChannel = Channel(Channel.BUFFERED)
        generator.registry = registry

        // Start and then pause directly
        generator.start(this) { }
        advanceUntilIdle()
        generator.pause()
        assertTrue(generator.isPaused())

        // resumeAll should NOT resume the independent generator
        registry.resumeAll()
        assertTrue(generator.isPaused())

        // Must resume directly
        generator.resume()
        assertTrue(generator.isRunning())

        // Cleanup
        generator.stop()
    }

    @Test
    fun sink_with_independentControl_true_is_skipped_by_pauseAll() = runTest {
        val registry = RuntimeRegistry()

        // Create sink with independentControl=true
        val sink = SinkRuntime<Int>(
            codeNode = createCodeNode("independent-sink", CodeNodeType.SINK, independentControl = true)
        ) { }
        sink.inputChannel = Channel(Channel.BUFFERED)
        sink.registry = registry

        // Start
        sink.start(this) { }
        advanceUntilIdle()
        assertEquals(1, registry.count)
        assertTrue(sink.isRunning())

        // Pause all via registry
        registry.pauseAll()

        // Independent sink should still be RUNNING
        assertTrue(sink.isRunning())

        // Cleanup
        sink.stop()
    }

    @Test
    fun independentControl_true_generator_can_be_controlled_directly() = runTest {
        val registry = RuntimeRegistry()

        // Create generator with independentControl=true
        val generator = GeneratorRuntime<Int>(
            codeNode = createCodeNode("independent", independentControl = true)
        ) { emit ->
            while (true) {
                emit(1)
                delay(100)
            }
        }
        generator.outputChannel = Channel(Channel.BUFFERED)
        generator.registry = registry

        // Start
        generator.start(this) { }
        advanceUntilIdle()
        assertTrue(generator.isRunning())

        // Pause directly (not through registry)
        generator.pause()
        assertTrue(generator.isPaused())

        // Resume directly
        generator.resume()
        assertTrue(generator.isRunning())

        // Stop
        generator.stop()
        assertTrue(generator.isIdle())
    }

    @Test
    fun default_independentControl_is_false() {
        val codeNode = createCodeNode("test")
        assertFalse(codeNode.controlConfig.independentControl)
    }

    @Test
    fun independentControl_can_be_set_to_true_via_ControlConfig() {
        val codeNode = createCodeNode("test", independentControl = true)
        assertTrue(codeNode.controlConfig.independentControl)
    }

    // ========================================
    // T055: State propagation tests
    // ========================================

    @Test
    fun mixed_independent_and_normal_runtimes_are_handled_correctly() = runTest {
        val registry = RuntimeRegistry()

        // Create 3 generators: 1 independent, 2 normal
        val independent = GeneratorRuntime<Int>(
            codeNode = createCodeNode("independent", independentControl = true)
        ) { emit ->
            while (true) { emit(1); delay(100) }
        }
        independent.outputChannel = Channel(Channel.BUFFERED)
        independent.registry = registry

        val normal1 = GeneratorRuntime<Int>(
            codeNode = createCodeNode("normal1")
        ) { emit ->
            while (true) { emit(1); delay(100) }
        }
        normal1.outputChannel = Channel(Channel.BUFFERED)
        normal1.registry = registry

        val normal2 = GeneratorRuntime<Int>(
            codeNode = createCodeNode("normal2")
        ) { emit ->
            while (true) { emit(1); delay(100) }
        }
        normal2.outputChannel = Channel(Channel.BUFFERED)
        normal2.registry = registry

        // Start all
        independent.start(this) { }
        normal1.start(this) { }
        normal2.start(this) { }
        advanceUntilIdle()

        assertEquals(3, registry.count)

        // All should be running
        assertTrue(independent.isRunning())
        assertTrue(normal1.isRunning())
        assertTrue(normal2.isRunning())

        // Pause all via registry
        registry.pauseAll()

        // Independent stays RUNNING, others are PAUSED
        assertTrue(independent.isRunning())
        assertTrue(normal1.isPaused())
        assertTrue(normal2.isPaused())

        // Resume all via registry
        registry.resumeAll()

        // Independent still RUNNING (was never paused), others resume
        assertTrue(independent.isRunning())
        assertTrue(normal1.isRunning())
        assertTrue(normal2.isRunning())

        // Cleanup
        independent.stop()
        normal1.stop()
        normal2.stop()
    }

    @Test
    fun stopAll_affects_all_runtimes_regardless_of_independentControl() = runTest {
        val registry = RuntimeRegistry()

        // Create both independent and normal generators
        val independent = GeneratorRuntime<Int>(
            codeNode = createCodeNode("independent", independentControl = true)
        ) { emit ->
            while (true) { emit(1); delay(100) }
        }
        independent.outputChannel = Channel(Channel.BUFFERED)
        independent.registry = registry

        val normal = GeneratorRuntime<Int>(
            codeNode = createCodeNode("normal")
        ) { emit ->
            while (true) { emit(1); delay(100) }
        }
        normal.outputChannel = Channel(Channel.BUFFERED)
        normal.registry = registry

        // Start both
        independent.start(this) { }
        normal.start(this) { }
        advanceUntilIdle()

        assertEquals(2, registry.count)

        // stopAll should stop ALL runtimes, including independent ones
        registry.stopAll()

        // Both should be stopped and registry cleared
        assertTrue(independent.isIdle())
        assertTrue(normal.isIdle())
        assertEquals(0, registry.count)
    }

    @Test
    fun pauseAll_on_empty_registry_does_not_throw() = runTest {
        val registry = RuntimeRegistry()

        // Should not throw
        registry.pauseAll()
        registry.resumeAll()
        registry.stopAll()

        assertEquals(0, registry.count)
    }

    @Test
    fun multiple_pause_and_resume_cycles_work_correctly() = runTest {
        val registry = RuntimeRegistry()

        val generator = GeneratorRuntime<Int>(
            codeNode = createCodeNode("cycle-test")
        ) { emit ->
            while (true) {
                emit(1)
                delay(100)
            }
        }
        generator.outputChannel = Channel(Channel.BUFFERED)
        generator.registry = registry

        generator.start(this) { }
        advanceUntilIdle()
        assertTrue(generator.isRunning())

        // Multiple pause/resume cycles
        for (i in 1..3) {
            registry.pauseAll()
            assertTrue(generator.isPaused())

            registry.resumeAll()
            assertTrue(generator.isRunning())
        }

        generator.stop()
        assertTrue(generator.isIdle())
    }

    @Test
    fun transformer_with_independentControl_true_is_skipped_by_pauseAll() = runTest {
        val registry = RuntimeRegistry()

        // Create transformer with independentControl=true
        val transformer = TransformerRuntime<Int, String>(
            codeNode = createCodeNode("independent-transformer", CodeNodeType.TRANSFORMER, independentControl = true)
        ) { value -> "Value: $value" }
        transformer.inputChannel = Channel(Channel.BUFFERED)
        transformer.transformerOutputChannel = Channel(Channel.BUFFERED)
        transformer.registry = registry

        // Start
        transformer.start(this) { }
        advanceUntilIdle()
        assertTrue(transformer.isRunning())

        // Pause all via registry
        registry.pauseAll()

        // Independent transformer should still be RUNNING
        assertTrue(transformer.isRunning())

        // Cleanup
        transformer.stop()
    }

    @Test
    fun filter_with_independentControl_true_is_skipped_by_pauseAll() = runTest {
        val registry = RuntimeRegistry()

        // Create filter with independentControl=true
        val filter = FilterRuntime<Int>(
            codeNode = createCodeNode("independent-filter", CodeNodeType.FILTER, independentControl = true)
        ) { value -> value > 0 }
        filter.inputChannel = Channel(Channel.BUFFERED)
        filter.outputChannel = Channel(Channel.BUFFERED)
        filter.registry = registry

        // Start
        filter.start(this) { }
        advanceUntilIdle()
        assertTrue(filter.isRunning())

        // Pause all via registry
        registry.pauseAll()

        // Independent filter should still be RUNNING
        assertTrue(filter.isRunning())

        // Cleanup
        filter.stop()
    }
}
