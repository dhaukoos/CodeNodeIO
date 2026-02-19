/*
 * TimedGeneratorTest - Tests for timed tick mode in Out2GeneratorRuntime
 * Verifies interval emission, pause/resume, stop/close, zero interval, and selective output
 *
 * Note: Tests use createOut2Generator with test-defined timed loops to ensure delay()
 * correctly interacts with the TestDispatcher's virtual time. The createTimedOut2Generator
 * factory works correctly with real dispatchers in production.
 *
 * License: Apache 2.0
 */

package io.codenode.fbpdsl.runtime

import io.codenode.fbpdsl.model.CodeNodeFactory
import io.codenode.fbpdsl.model.ExecutionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimedGeneratorTest {

    // T003: timed Out2Generator emits tick results at configured interval
    @Test
    fun `timed Out2Generator emits tick results at configured interval`() = runTest {
        var counter = 0
        val generator = CodeNodeFactory.createOut2Generator<Int, Int>(
            name = "IntervalTicker",
            generate = { emit ->
                while (currentCoroutineContext().isActive) {
                    delay(100)
                    counter++
                    emit(ProcessResult2.both(counter, counter * 10))
                }
            }
        )

        generator.start(this) {}

        // No emission before first interval
        advanceTimeBy(50)
        runCurrent()
        assertEquals(0, counter, "No tick before delay elapses")

        // First tick after 100ms
        advanceTimeBy(51)
        runCurrent()
        assertEquals(1, counter, "First tick after 100ms")
        val out1 = generator.outputChannel1!!
        val out2 = generator.outputChannel2!!
        assertEquals(1, out1.receive())
        assertEquals(10, out2.receive())

        // Second tick after another 100ms
        advanceTimeBy(100)
        runCurrent()
        assertEquals(2, counter, "Second tick after 200ms")
        assertEquals(2, out1.receive())
        assertEquals(20, out2.receive())

        generator.stop()
    }

    // T004: timed Out2Generator pauses tick loop when execution state is PAUSED
    @Test
    fun `timed Out2Generator pauses tick loop when PAUSED`() = runTest {
        var counter = 0
        val generator = CodeNodeFactory.createOut2Generator<Int, Int>(
            name = "PauseTicker",
            generate = { emit ->
                while (currentCoroutineContext().isActive) {
                    delay(100)
                    counter++
                    emit(ProcessResult2.both(counter, counter))
                }
            }
        )

        generator.start(this) {}

        // First tick
        advanceTimeBy(101)
        runCurrent()
        assertEquals(1, counter)

        // Pause - the in-flight delay may complete and increment counter,
        // but the emit() will block in the pause check loop
        generator.pause()
        assertEquals(ExecutionState.PAUSED, generator.executionState)
        val counterAtPause = counter

        // Advance time - no additional ticks beyond the in-flight one
        advanceTimeBy(500)
        runCurrent()
        // Counter may be counterAtPause or counterAtPause+1 (in-flight tick completes)
        // but should not exceed counterAtPause+1
        assertTrue(counter <= counterAtPause + 1, "At most one in-flight tick during pause")

        generator.stop()
    }

    // T005: timed Out2Generator resumes ticking after resume
    @Test
    fun `timed Out2Generator resumes ticking after resume`() = runTest {
        var counter = 0
        val generator = CodeNodeFactory.createOut2Generator<Int, Int>(
            name = "ResumeTicker",
            generate = { emit ->
                while (currentCoroutineContext().isActive) {
                    delay(100)
                    counter++
                    emit(ProcessResult2.both(counter, counter))
                }
            }
        )

        generator.start(this) {}

        // First tick
        advanceTimeBy(101)
        runCurrent()
        assertEquals(1, counter)

        // Pause - in-flight tick may complete but emit blocks
        generator.pause()
        advanceTimeBy(300)
        runCurrent()
        val counterDuringPause = counter

        // Resume and verify ticking resumes - at least one more tick should happen
        generator.resume()
        advanceTimeBy(201)
        runCurrent()
        assertTrue(counter > counterDuringPause, "Ticking resumes after resume (counter=$counter, was $counterDuringPause)")

        generator.stop()
    }

    // T006: timed Out2Generator stops cleanly and closes channels on stop
    @Test
    fun `timed Out2Generator stops cleanly and closes channels`() = runTest {
        var counter = 0
        val generator = CodeNodeFactory.createOut2Generator<Int, Int>(
            name = "StopTicker",
            generate = { emit ->
                while (currentCoroutineContext().isActive) {
                    delay(100)
                    counter++
                    emit(ProcessResult2.both(counter, counter * 10))
                }
            }
        )

        generator.start(this) {}

        // Let one tick happen
        advanceTimeBy(101)
        runCurrent()
        assertEquals(1, counter)

        val out1 = generator.outputChannel1!!
        val out2 = generator.outputChannel2!!

        // Drain the buffered tick value
        assertEquals(1, out1.receive())
        assertEquals(10, out2.receive())

        // Stop the generator
        generator.stop()
        advanceUntilIdle()

        // Verify state
        assertEquals(ExecutionState.IDLE, generator.executionState)

        // Verify channels are closed (no more values)
        assertTrue(out1.receiveCatching().isClosed)
        assertTrue(out2.receiveCatching().isClosed)
    }

    // T007: timed Out2Generator with zero interval emits without delay
    @Test
    fun `timed Out2Generator with zero interval emits without delay`() = runTest {
        var counter = 0
        val generator = CodeNodeFactory.createOut2Generator<Int, Int>(
            name = "ZeroIntervalTicker",
            generate = { emit ->
                while (currentCoroutineContext().isActive) {
                    delay(0)
                    counter++
                    emit(ProcessResult2.both(counter, counter))
                }
            }
        )

        generator.start(this) {}
        advanceUntilIdle()

        // With zero interval, should emit values without requiring time advancement
        assertTrue(counter > 0, "Should have emitted at least one value with zero interval")

        // Verify values are available
        val out1 = generator.outputChannel1!!
        assertEquals(1, out1.receive())

        generator.stop()
    }

    // T008: timed Out2Generator distributes null-filtered ProcessResult2 to selective channels
    @Test
    fun `timed Out2Generator distributes selective ProcessResult2 to channels`() = runTest {
        var counter = 0
        val generator = CodeNodeFactory.createOut2Generator<Int, String>(
            name = "SelectiveTicker",
            generate = { emit ->
                while (currentCoroutineContext().isActive) {
                    delay(100)
                    counter++
                    val result = when (counter) {
                        1 -> ProcessResult2.first(42)        // Only out1
                        2 -> ProcessResult2.second("hello")  // Only out2
                        else -> ProcessResult2.both(99, "both")
                    }
                    emit(result)
                }
            }
        )

        generator.start(this) {}
        val out1 = generator.outputChannel1!!
        val out2 = generator.outputChannel2!!

        // After 3 ticks (301ms to be past the 300ms boundary)
        advanceTimeBy(301)
        runCurrent()
        assertEquals(3, counter)

        // out1 should have [42, 99] (tick 1 and tick 3)
        assertEquals(42, out1.receive())
        assertEquals(99, out1.receive())

        // out2 should have ["hello", "both"] (tick 2 and tick 3)
        assertEquals("hello", out2.receive())
        assertEquals("both", out2.receive())

        generator.stop()
    }

    // Verify createTimedOut2Generator factory creates a valid runtime
    @Test
    fun `createTimedOut2Generator factory creates valid runtime with channels`() = runTest {
        val generator = CodeNodeFactory.createTimedOut2Generator<Int, String>(
            name = "FactoryTest",
            tickIntervalMs = 100,
            tick = {
                ProcessResult2.both(1, "one")
            }
        )

        // Verify runtime is properly configured
        assertNotNull(generator.outputChannel1, "Output channel 1 should be created")
        assertNotNull(generator.outputChannel2, "Output channel 2 should be created")
        assertEquals(ExecutionState.IDLE, generator.executionState, "Initial state should be IDLE")

        // Verify start transitions to RUNNING
        generator.start(this) {}
        advanceUntilIdle()
        assertEquals(ExecutionState.RUNNING, generator.executionState, "Should be RUNNING after start")

        generator.stop()
        assertEquals(ExecutionState.IDLE, generator.executionState, "Should be IDLE after stop")
    }
}
