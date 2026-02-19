/*
 * PauseResumeTest - Tests for pause/resume behavior in runtime classes
 * Verifies that all runtime processing loops honor PAUSED state
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PauseResumeTest {

    private fun createTestCodeNode(id: String, type: CodeNodeType = CodeNodeType.GENERATOR): CodeNode {
        return CodeNode(
            id = id,
            name = "Test Node $id",
            codeNodeType = type,
            position = Node.Position.ORIGIN,
            controlConfig = ControlConfig()
        )
    }

    // ========== Basic State Transition Tests ==========

    @Test
    fun generatorRuntime_pauseChangesState() = runTest {
        val generator = GeneratorRuntime<Int>(
            codeNode = createTestCodeNode("gen1")
        ) { emit -> }

        generator.start(this) { }
        advanceUntilIdle()

        assertTrue(generator.isRunning())

        generator.pause()
        assertTrue(generator.isPaused())

        generator.resume()
        assertTrue(generator.isRunning())

        generator.stop()
        assertTrue(generator.isIdle())
    }

    @Test
    fun sinkRuntime_pauseChangesState() = runTest {
        val inputChannel = Channel<Int>(Channel.BUFFERED)
        val sink = SinkRuntime<Int>(
            codeNode = createTestCodeNode("sink1", CodeNodeType.SINK)
        ) { }
        sink.inputChannel = inputChannel

        sink.start(this) { }
        advanceUntilIdle()

        assertTrue(sink.isRunning())

        sink.pause()
        assertTrue(sink.isPaused())

        sink.resume()
        assertTrue(sink.isRunning())

        sink.stop()
        inputChannel.close()
        assertTrue(sink.isIdle())
    }

    @Test
    fun transformerRuntime_pauseChangesState() = runTest {
        val inputChannel = Channel<Int>(Channel.BUFFERED)
        val outputChannel = Channel<Int>(Channel.BUFFERED)
        val transformer = TransformerRuntime<Int, Int>(
            codeNode = createTestCodeNode("transformer1", CodeNodeType.TRANSFORMER)
        ) { it }
        transformer.inputChannel = inputChannel
        transformer.transformerOutputChannel = outputChannel

        transformer.start(this) { }
        advanceUntilIdle()

        assertTrue(transformer.isRunning())

        transformer.pause()
        assertTrue(transformer.isPaused())

        transformer.resume()
        assertTrue(transformer.isRunning())

        transformer.stop()
        inputChannel.close()
        assertTrue(transformer.isIdle())
    }

    @Test
    fun filterRuntime_pauseChangesState() = runTest {
        val inputChannel = Channel<Int>(Channel.BUFFERED)
        val outputChannel = Channel<Int>(Channel.BUFFERED)
        val filter = FilterRuntime<Int>(
            codeNode = createTestCodeNode("filter1", CodeNodeType.FILTER)
        ) { true }
        filter.inputChannel = inputChannel
        filter.outputChannel = outputChannel

        filter.start(this) { }
        advanceUntilIdle()

        assertTrue(filter.isRunning())

        filter.pause()
        assertTrue(filter.isPaused())

        filter.resume()
        assertTrue(filter.isRunning())

        filter.stop()
        inputChannel.close()
        assertTrue(filter.isIdle())
    }

    @Test
    fun out2GeneratorRuntime_pauseChangesState() = runTest {
        val generator = Out2GeneratorRuntime<Int, String>(
            codeNode = createTestCodeNode("gen2out"),
            generate = { emit -> }
        )

        generator.start(this) { }
        advanceUntilIdle()

        assertTrue(generator.isRunning())

        generator.pause()
        assertTrue(generator.isPaused())

        generator.resume()
        assertTrue(generator.isRunning())

        generator.stop()
        assertTrue(generator.isIdle())
    }

    @Test
    fun in2SinkRuntime_pauseChangesState() = runTest {
        val inputChannel1 = Channel<Int>(Channel.BUFFERED)
        val inputChannel2 = Channel<String>(Channel.BUFFERED)
        val sink = In2SinkRuntime<Int, String>(
            codeNode = createTestCodeNode("sink2in", CodeNodeType.SINK)
        ) { _, _ -> }
        sink.inputChannel = inputChannel1
        sink.inputChannel2 = inputChannel2

        sink.start(this) { }
        advanceUntilIdle()

        assertTrue(sink.isRunning())

        sink.pause()
        assertTrue(sink.isPaused())

        sink.resume()
        assertTrue(sink.isRunning())

        sink.stop()
        inputChannel1.close()
        inputChannel2.close()
        assertTrue(sink.isIdle())
    }

    // ========== Pause/Stop State Transitions ==========

    @Test
    fun pauseOnlyWorksWhenRunning() = runTest {
        val generator = GeneratorRuntime<Int>(
            codeNode = createTestCodeNode("gen-pause-test")
        ) { emit -> }

        // Can't pause when IDLE
        generator.pause()
        assertTrue(generator.isIdle(), "Pause should not change IDLE state")

        generator.start(this) { }
        advanceUntilIdle()
        assertTrue(generator.isRunning())

        // Can pause when RUNNING
        generator.pause()
        assertTrue(generator.isPaused())

        // Can't pause when already PAUSED
        generator.pause()
        assertTrue(generator.isPaused(), "State should remain PAUSED")

        generator.stop()
    }

    @Test
    fun resumeOnlyWorksWhenPaused() = runTest {
        val generator = GeneratorRuntime<Int>(
            codeNode = createTestCodeNode("gen-resume-test")
        ) { emit -> }

        // Can't resume when IDLE
        generator.resume()
        assertTrue(generator.isIdle(), "Resume should not change IDLE state")

        generator.start(this) { }
        advanceUntilIdle()

        // Can't resume when RUNNING
        generator.resume()
        assertTrue(generator.isRunning(), "Resume should not affect RUNNING state")

        generator.pause()
        assertTrue(generator.isPaused())

        // Can resume when PAUSED
        generator.resume()
        assertTrue(generator.isRunning())

        generator.stop()
    }

    @Test
    fun stopWhilePaused_changesStateToIdle() = runTest {
        val generator = GeneratorRuntime<Int>(
            codeNode = createTestCodeNode("gen-stop-paused")
        ) { emit -> }

        generator.start(this) { }
        advanceUntilIdle()
        assertTrue(generator.isRunning())

        generator.pause()
        assertTrue(generator.isPaused())

        // Stop while paused should work
        generator.stop()
        assertTrue(generator.isIdle())
    }

    // ========== Registry Integration Tests ==========

    @Test
    fun registry_pauseAll_pausesAllRuntimes() = runTest {
        val registry = RuntimeRegistry()

        val gen1 = GeneratorRuntime<Int>(
            codeNode = createTestCodeNode("gen1")
        ) { emit -> }
        gen1.registry = registry

        val gen2 = GeneratorRuntime<Int>(
            codeNode = createTestCodeNode("gen2")
        ) { emit -> }
        gen2.registry = registry

        gen1.start(this) { }
        gen2.start(this) { }
        advanceUntilIdle()

        assertTrue(gen1.isRunning())
        assertTrue(gen2.isRunning())
        assertEquals(2, registry.count)

        // Pause all through registry
        registry.pauseAll()
        assertTrue(gen1.isPaused())
        assertTrue(gen2.isPaused())

        // Resume all through registry
        registry.resumeAll()
        assertTrue(gen1.isRunning())
        assertTrue(gen2.isRunning())

        gen1.stop()
        gen2.stop()
    }

    @Test
    fun registry_stopAll_stopsAllRuntimes() = runTest {
        val registry = RuntimeRegistry()

        val gen1 = GeneratorRuntime<Int>(
            codeNode = createTestCodeNode("gen1")
        ) { emit -> }
        gen1.registry = registry

        val gen2 = GeneratorRuntime<Int>(
            codeNode = createTestCodeNode("gen2")
        ) { emit -> }
        gen2.registry = registry

        gen1.start(this) { }
        gen2.start(this) { }
        advanceUntilIdle()

        assertEquals(2, registry.count)

        // Stop all through registry
        registry.stopAll()
        assertTrue(gen1.isIdle())
        assertTrue(gen2.isIdle())
        assertEquals(0, registry.count)
    }

    @Test
    fun registry_independentControl_skippedByPauseAll() = runTest {
        val registry = RuntimeRegistry()

        val normalNode = createTestCodeNode("normal")
        val independentNode = CodeNode(
            id = "independent",
            name = "Independent Node",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position.ORIGIN,
            controlConfig = ControlConfig(independentControl = true)
        )

        val normalRuntime = GeneratorRuntime<Int>(
            codeNode = normalNode
        ) { emit -> }
        normalRuntime.registry = registry

        val independentRuntime = GeneratorRuntime<Int>(
            codeNode = independentNode
        ) { emit -> }
        independentRuntime.registry = registry

        normalRuntime.start(this) { }
        independentRuntime.start(this) { }
        advanceUntilIdle()

        assertTrue(normalRuntime.isRunning())
        assertTrue(independentRuntime.isRunning())

        // Pause all - should skip independent
        registry.pauseAll()
        assertTrue(normalRuntime.isPaused())
        assertTrue(independentRuntime.isRunning(), "Independent runtime should not be paused")

        // Resume all - should skip independent
        registry.resumeAll()
        assertTrue(normalRuntime.isRunning())
        assertTrue(independentRuntime.isRunning())

        normalRuntime.stop()
        independentRuntime.stop()
    }

    // ========== Multiple Pause/Resume Cycles ==========

    @Test
    fun multiplePauseResumeCycles() = runTest {
        val generator = GeneratorRuntime<Int>(
            codeNode = createTestCodeNode("gen-cycle")
        ) { emit -> }

        generator.start(this) { }
        advanceUntilIdle()

        // Cycle 1
        generator.pause()
        assertTrue(generator.isPaused())
        generator.resume()
        assertTrue(generator.isRunning())

        // Cycle 2
        generator.pause()
        assertTrue(generator.isPaused())
        generator.resume()
        assertTrue(generator.isRunning())

        // Cycle 3
        generator.pause()
        assertTrue(generator.isPaused())
        generator.resume()
        assertTrue(generator.isRunning())

        generator.stop()
        assertTrue(generator.isIdle())
    }
}
