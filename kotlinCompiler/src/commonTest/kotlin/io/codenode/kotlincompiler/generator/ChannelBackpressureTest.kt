/*
 * Channel Backpressure Test
 * Verifies backpressure behavior with Kotlin Channels
 * License: Apache 2.0
 */

package io.codenode.kotlincompiler.generator

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.*

/**
 * Tests for verifying backpressure behavior with Kotlin Channels.
 *
 * These tests verify the core FBP semantics that:
 * - Bounded channels apply backpressure when buffer is full
 * - Rendezvous channels block sender until receiver is ready
 * - Channel close propagates gracefully
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChannelBackpressureTest {

    // ========== Backpressure Tests ==========

    @Test
    fun `buffered channel applies backpressure when full`() = runTest {
        // Given: A channel with capacity 2
        val channel = Channel<Int>(capacity = 2)
        var sendCount = 0

        // When: Producer tries to send 5 items without consumer
        val producer = launch {
            repeat(5) { i ->
                channel.send(i)
                sendCount++
            }
        }

        // Let producer run but don't consume
        advanceTimeBy(100)

        // Then: Should be blocked after 2 (buffer size)
        assertEquals(2, sendCount, "Producer should block at buffer capacity")
        assertTrue(producer.isActive, "Producer should still be active (blocked)")

        // When: Consumer receives items
        repeat(5) {
            channel.receive()
        }

        advanceUntilIdle()

        // Then: Producer should complete
        assertEquals(5, sendCount, "All items should eventually be sent")
        assertFalse(producer.isActive, "Producer should complete")

        channel.close()
    }

    @Test
    fun `rendezvous channel blocks sender until receiver ready`() = runTest {
        // Given: A rendezvous channel (capacity 0)
        val channel = Channel<Int>(Channel.RENDEZVOUS)
        var sent = false

        // When: Producer tries to send without receiver
        val producer = launch {
            channel.send(1)
            sent = true
        }

        advanceTimeBy(100)

        // Then: Send should be blocked
        assertFalse(sent, "Send should block without receiver")
        assertTrue(producer.isActive, "Producer should be blocked")

        // When: Receiver starts
        launch { channel.receive() }
        advanceUntilIdle()

        // Then: Send should complete
        assertTrue(sent, "Send should complete after receiver ready")
        assertFalse(producer.isActive, "Producer should complete")

        channel.close()
    }

    @Test
    fun `channel close allows buffered data to be consumed`() = runTest {
        // Given: A channel with buffered data
        val channel = Channel<Int>(capacity = 5)
        val received = mutableListOf<Int>()

        // Buffer some data
        repeat(3) { channel.send(it) }

        // Close channel (no more sends allowed)
        channel.close()

        // When: Consumer drains the channel
        val consumer = launch {
            for (data in channel) {
                received.add(data)
            }
        }

        advanceUntilIdle()

        // Then: All buffered data should be received
        assertEquals(listOf(0, 1, 2), received, "All buffered data should be received")
        assertFalse(consumer.isActive, "Consumer should complete after channel drained")
    }

    @Test
    fun `channel iteration exits gracefully when closed`() = runTest {
        // Given: A channel
        val channel = Channel<String>(capacity = 1)
        var iterationCompleted = false

        // When: Consumer starts iterating and channel is closed
        val consumer = launch {
            for (data in channel) {
                // Process data
            }
            iterationCompleted = true
        }

        // Close immediately (no data sent)
        channel.close()

        advanceUntilIdle()

        // Then: Iteration should complete gracefully
        assertTrue(iterationCompleted, "Iteration should complete when channel closed")
        assertFalse(consumer.isActive, "Consumer should complete")
    }

    @Test
    fun `unlimited channel never blocks sender`() = runTest {
        // Given: An unlimited channel
        val channel = Channel<Int>(Channel.UNLIMITED)
        var sendCount = 0

        // When: Producer sends many items without consumer
        val producer = launch {
            repeat(1000) { i ->
                channel.send(i)
                sendCount++
            }
        }

        advanceUntilIdle()

        // Then: All sends should complete immediately (no blocking)
        assertEquals(1000, sendCount, "Unlimited channel should never block sender")
        assertFalse(producer.isActive, "Producer should complete immediately")

        channel.close()
    }

    // ========== Capacity Mapping Verification ==========

    @Test
    fun `capacity 0 creates rendezvous channel`() = runTest {
        // Capacity 0 maps to Channel.RENDEZVOUS
        val capacity = ModuleGenerator.channelCapacityArg(0)
        assertEquals("Channel.RENDEZVOUS", capacity)

        // Verify rendezvous behavior
        val channel = Channel<Int>(Channel.RENDEZVOUS)
        var sent = false

        val producer = launch {
            channel.send(1)
            sent = true
        }

        advanceTimeBy(100)
        assertFalse(sent, "Rendezvous channel should block without receiver")

        launch { channel.receive() }
        advanceUntilIdle()
        assertTrue(sent, "Rendezvous channel should unblock when receiver ready")

        channel.close()
    }

    @Test
    fun `positive capacity creates buffered channel`() = runTest {
        // Capacity 3 maps to "3"
        val capacity = ModuleGenerator.channelCapacityArg(3)
        assertEquals("3", capacity)

        // Verify buffered behavior
        val channel = Channel<Int>(3)
        var sendCount = 0

        val producer = launch {
            repeat(5) { i ->
                channel.send(i)
                sendCount++
            }
        }

        advanceTimeBy(100)
        assertEquals(3, sendCount, "Buffered channel should accept up to capacity")

        // Receive to unblock
        repeat(3) { channel.receive() }
        advanceUntilIdle()

        assertEquals(5, sendCount, "All items should be sent after consuming")

        channel.close()
    }
}
