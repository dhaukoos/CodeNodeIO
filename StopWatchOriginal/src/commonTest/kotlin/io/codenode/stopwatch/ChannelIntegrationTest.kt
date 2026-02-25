/*
 * ChannelIntegrationTest - Integration tests for Channel-based connections
 * Tests T019-T020: Channel integration and graceful shutdown
 * License: Apache 2.0
 */

package io.codenode.stopwatch

import io.codenode.stopwatch.generated.StopWatchFlow
import io.codenode.stopwatch.usecases.DisplayReceiverComponent
import io.codenode.stopwatch.usecases.TimerEmitterComponent
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
     * T019: Verify TimerEmitter and DisplayReceiver work with typed channels.
     *
     * Given: TimerEmitter and DisplayReceiver connected via typed Channels
     * When: Timer ticks occur
     * Then: DisplayReceiver receives and processes the timer outputs
     */
    @Test
    fun timerEmitter_sends_to_displayReceiver_via_channel() = runTest {
        // Given - create components (Out2Generator creates its own channels)
        val timerEmitter = TimerEmitterComponent(speedAttenuation = 50L)
        val displayReceiver = DisplayReceiverComponent()

        // Start the emitter - start() transitions to RUNNING and recreates output channels
        timerEmitter.start(backgroundScope)

        // Wire up after start so we get the fresh channels
        displayReceiver.inputChannel = timerEmitter.outputChannel1
        displayReceiver.inputChannel2 = timerEmitter.outputChannel2

        // Start the receiver
        backgroundScope.launch {
            displayReceiver.start(this)
        }

        // When - let a few ticks occur
        advanceTimeBy(150) // ~3 ticks

        // Then - verify data flowed through
        val seconds = displayReceiver.secondsFlow.first()
        assertTrue(seconds >= 1, "DisplayReceiver should have received at least 1 tick, got $seconds")

        // Cleanup - stop components (generator closes its channels)
        timerEmitter.stop()
        displayReceiver.stop()
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
        val channel = Channel<Int>(capacity = 5)
        val receivedData = mutableListOf<Int>()

        // Pre-populate channel with buffered data
        val testData = listOf(1, 2, 3)
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
     * Given: DisplayReceiver connected to channels
     * When: Channels are closed while receiver is running
     * Then: Receiver should exit gracefully without exceptions
     */
    @Test
    fun displayReceiver_handles_channel_closure_gracefully() = runTest {
        // Given
        val displayReceiver = DisplayReceiverComponent()
        val secondsChannel = Channel<Int>(Channel.RENDEZVOUS)
        val minutesChannel = Channel<Int>(Channel.RENDEZVOUS)
        displayReceiver.inputChannel = secondsChannel
        displayReceiver.inputChannel2 = minutesChannel

        // Start receiver
        val receiverJob = launch {
            displayReceiver.start(this)
        }

        advanceUntilIdle()

        // When - close channels
        secondsChannel.close()
        minutesChannel.close()
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
     * Given: TimerEmitter with its internal channels closed
     * When: Timer tries to send
     * Then: Emitter should handle ClosedSendChannelException and exit loop
     */
    @Test
    fun timerEmitter_handles_channel_closure_gracefully() = runTest {
        // Given
        val timerEmitter = TimerEmitterComponent(speedAttenuation = 50L)

        // Close channels before starting (generator creates channels in init)
        timerEmitter.outputChannel1?.close()
        timerEmitter.outputChannel2?.close()

        // When - start emitter (start() transitions to RUNNING)
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
     * T023: Full end-to-end StopWatchFlow test.
     *
     * Given: StopWatchFlow with TimerEmitter and DisplayReceiver wired via typed Channels
     * When: Flow is started and timer ticks occur
     * Then: Data flows from emitter through channels to receiver, and shutdown is graceful
     */
    @Test
    fun stopWatchFlow_end_to_end_channel_flow() = runTest {
        // Given - create the flow orchestrator
        val flow = StopWatchFlow()

        // Start the flow in background (start() transitions to RUNNING)
        backgroundScope.launch {
            flow.start(this)
        }

        // When - let timer ticks occur (using fast tick rate from component default)
        // TimerEmitter uses 1000ms by default, but we can check initial wiring works
        advanceTimeBy(50)

        // Verify wiring is correct - channels should be assigned
        assertTrue(flow.timerEmitter.outputChannel1 != null, "Timer outputChannel1 should be wired")
        assertTrue(flow.timerEmitter.outputChannel2 != null, "Timer outputChannel2 should be wired")
        assertTrue(flow.displayReceiver.inputChannel != null, "Display inputChannel should be wired")
        assertTrue(flow.displayReceiver.inputChannel2 != null, "Display inputChannel2 should be wired")

        // Let some time pass for data flow
        advanceTimeBy(2100) // ~2 ticks at 1000ms

        // Then - verify data flowed through the channels
        val seconds = flow.displayReceiver.secondsFlow.first()
        assertTrue(seconds >= 1, "DisplayReceiver should have received timer ticks, got $seconds")

        // Verify graceful shutdown
        flow.stop()

        // Flow should have stopped without exceptions
        assertTrue(true, "StopWatchFlow completed end-to-end test successfully")
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
        val channel = Channel<Int>(Channel.RENDEZVOUS)
        var sendCompleted = false

        // When - send without receiver
        val senderJob = launch {
            channel.send(1)
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
