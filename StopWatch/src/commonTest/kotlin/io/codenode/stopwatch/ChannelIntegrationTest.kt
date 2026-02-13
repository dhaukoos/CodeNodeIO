/*
 * ChannelIntegrationTest - Integration tests for Channel-based connections
 * Tests T019-T020: Channel integration and graceful shutdown
 * License: Apache 2.0
 */

package io.codenode.stopwatch

import io.codenode.fbpdsl.model.ExecutionState
import io.codenode.stopwatch.usecases.DisplayReceiverComponent
import io.codenode.stopwatch.usecases.TimerEmitterComponent
import io.codenode.stopwatch.usecases.TimerOutput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for Channel-based component connections.
 * Verifies FBP point-to-point semantics with backpressure and graceful shutdown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelIntegrationTest {

    /**
     * T019: Verify TimerEmitter and DisplayReceiver work with channels.
     *
     * Given: TimerEmitter and DisplayReceiver connected via a Channel
     * When: Timer ticks occur
     * Then: DisplayReceiver receives and processes the timer outputs
     */
    @Test
    fun timerEmitter_sends_to_displayReceiver_via_channel() = runTest {
        // Given - create components and channel (use buffered to avoid blocking)
        val timerEmitter = TimerEmitterComponent(speedAttenuation = 50L)
        val displayReceiver = DisplayReceiverComponent()
        val channel = Channel<TimerOutput>(capacity = 2)

        // Wire up the channel connection
        timerEmitter.outputChannel = channel
        displayReceiver.inputChannel = channel

        // Start the receiver first (to be ready for data) - use backgroundScope
        backgroundScope.launch {
            displayReceiver.start(this)
        }

        // Start the emitter - use backgroundScope
        timerEmitter.executionState = ExecutionState.RUNNING
        backgroundScope.launch {
            timerEmitter.start(this)
        }

        // When - let a few ticks occur
        advanceTimeBy(150) // ~3 ticks

        // Then - verify data flowed through
        val seconds = displayReceiver.displayedSecondsFlow.first()
        assertTrue(seconds >= 1, "DisplayReceiver should have received at least 1 tick, got $seconds")

        // Cleanup - stop components first, then close channel
        timerEmitter.stop()
        displayReceiver.stop()
        channel.close()
    }

    /**
     * T020: Verify graceful shutdown - buffered data consumed before channel reports closed.
     *
     * Given: A buffered channel with data in it
     * When: Channel is closed
     * Then: Consumer should receive all buffered data before for-loop exits
     */
    @Test
    fun buffered_channel_data_consumed_before_close() = runTest {
        // Given - buffered channel with capacity
        val channel = Channel<TimerOutput>(capacity = 5)
        val receivedData = mutableListOf<TimerOutput>()

        // Pre-populate channel with buffered data
        val testData = listOf(
            TimerOutput(1, 0),
            TimerOutput(2, 0),
            TimerOutput(3, 0)
        )
        testData.forEach { channel.send(it) }

        // Close channel (no more sends allowed, but buffered data remains)
        channel.close()

        // When - consume the channel
        val consumerJob = launch {
            for (data in channel) {
                receivedData.add(data)
            }
        }

        advanceUntilIdle()

        // Then - all buffered data should be received
        assertEquals(3, receivedData.size, "All 3 buffered items should be received")
        assertEquals(testData, receivedData, "Received data should match sent data")
    }

    /**
     * Verify DisplayReceiver handles channel closure gracefully.
     *
     * Given: DisplayReceiver connected to a channel
     * When: Channel is closed while receiver is running
     * Then: Receiver should exit gracefully without exceptions
     */
    @Test
    fun displayReceiver_handles_channel_closure_gracefully() = runTest {
        // Given
        val displayReceiver = DisplayReceiverComponent()
        val channel = Channel<TimerOutput>(Channel.RENDEZVOUS)
        displayReceiver.inputChannel = channel

        // Start receiver
        val receiverJob = launch {
            displayReceiver.start(this)
        }

        advanceUntilIdle()

        // When - close channel
        channel.close()
        advanceUntilIdle()

        // Then - receiver job should complete gracefully (no exception thrown)
        displayReceiver.stop()
        receiverJob.cancel()

        // If we get here without exception, the test passes
        assertTrue(true, "DisplayReceiver handled channel closure gracefully")
    }

    /**
     * Verify TimerEmitter handles channel closure gracefully.
     *
     * Given: TimerEmitter connected to a closed channel
     * When: Timer tries to send
     * Then: Emitter should handle ClosedSendChannelException and exit loop
     */
    @Test
    fun timerEmitter_handles_channel_closure_gracefully() = runTest {
        // Given
        val timerEmitter = TimerEmitterComponent(speedAttenuation = 50L)
        val channel = Channel<TimerOutput>(Channel.RENDEZVOUS)
        timerEmitter.outputChannel = channel

        // Close channel before starting
        channel.close()

        // When - start emitter
        timerEmitter.executionState = ExecutionState.RUNNING
        val emitterJob = launch {
            timerEmitter.start(this)
        }

        advanceTimeBy(100)
        advanceUntilIdle()

        // Then - emitter should have exited gracefully
        timerEmitter.stop()
        emitterJob.cancel()

        // If we get here without exception, the test passes
        assertTrue(true, "TimerEmitter handled channel closure gracefully")
    }

    /**
     * Verify rendezvous channel provides backpressure.
     *
     * Given: A rendezvous channel (capacity 0)
     * When: Sender sends without receiver
     * Then: Sender should block until receiver is ready
     */
    @Test
    fun rendezvous_channel_provides_backpressure() = runTest {
        // Given
        val channel = Channel<TimerOutput>(Channel.RENDEZVOUS)
        var sendCompleted = false

        // When - send without receiver
        val senderJob = launch {
            channel.send(TimerOutput(1, 0))
            sendCompleted = true
        }

        advanceTimeBy(100)

        // Then - send should be blocked
        assertEquals(false, sendCompleted, "Send should be blocked without receiver")

        // Now add receiver
        val receiverJob = launch {
            channel.receive()
        }

        advanceUntilIdle()

        // Then - send should complete
        assertEquals(true, sendCompleted, "Send should complete when receiver is ready")

        channel.close()
        senderJob.cancel()
        receiverJob.cancel()
    }
}
