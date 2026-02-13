# Contract: Component Channel Interface

## Overview

Components interact with connection channels through `SendChannel<T>` and `ReceiveChannel<T>` interfaces provided by the flow orchestrator. This uses raw Kotlin Channels for true FBP point-to-point semantics.

## Generator Component Contract

### Interface

```kotlin
interface ChannelOutputProvider {
    val outputChannels: MutableMap<String, SendChannel<Any>>

    fun setOutputChannel(portName: String, channel: SendChannel<Any>) {
        outputChannels[portName] = channel
    }

    fun closeOutputChannels() {
        outputChannels.values.forEach { it.close() }
    }
}
```

### Implementation Pattern

```kotlin
class GeneratorComponent : ProcessingLogic, ChannelOutputProvider {
    override val outputChannels = mutableMapOf<String, SendChannel<Any>>()

    private var producerJob: Job? = null

    suspend fun start(scope: CoroutineScope) {
        producerJob = scope.launch {
            while (isActive) {
                val data = produceData()
                // Send to all output channels - suspends if any is full (backpressure)
                outputChannels.values.forEach { channel ->
                    channel.send(data)
                }
            }
        }
    }

    fun stop() {
        producerJob?.cancel()
        closeOutputChannels()  // Signal no more data
    }
}
```

### Test Contract

```kotlin
@Test
fun `generator suspends when channel buffer full`() = runTest {
    val channel = Channel<Any>(capacity = 1)
    val component = GeneratorComponent()
    component.setOutputChannel("output", channel)

    // Fill buffer
    channel.send("data1")

    // Second send should suspend
    val sendJob = launch { channel.send("data2") }
    advanceUntilIdle()
    assertTrue(sendJob.isActive, "Send should be suspended")

    // Receive to unblock
    channel.receive()
    advanceUntilIdle()
    assertFalse(sendJob.isActive, "Send should complete after receive")

    channel.close()
}

@Test
fun `generator closes channel on stop`() = runTest {
    val channel = Channel<Any>(capacity = 1)
    val component = GeneratorComponent()
    component.setOutputChannel("output", channel)

    component.stop()

    assertTrue(channel.isClosedForSend, "Channel should be closed for send")
}
```

## Sink Component Contract

### Interface

```kotlin
interface ChannelInputProvider {
    val inputChannels: MutableMap<String, ReceiveChannel<Any>>

    fun setInputChannel(portName: String, channel: ReceiveChannel<Any>) {
        inputChannels[portName] = channel
    }

    fun cancelInputChannels() {
        inputChannels.values.forEach { it.cancel() }
    }
}
```

### Implementation Pattern

```kotlin
class SinkComponent : ProcessingLogic, ChannelInputProvider {
    override val inputChannels = mutableMapOf<String, ReceiveChannel<Any>>()

    private var consumerJob: Job? = null

    suspend fun start(scope: CoroutineScope) {
        val inputChannel = inputChannels["input"]
            ?: throw IllegalStateException("Input channel not connected")

        consumerJob = scope.launch {
            try {
                for (data in inputChannel) {  // Iterates until channel closed
                    process(data)
                }
            } catch (e: ClosedReceiveChannelException) {
                // Normal shutdown - channel was closed
            }
        }
    }

    fun stop() {
        consumerJob?.cancel()
    }

    private fun process(data: Any) {
        // Handle received data
    }
}
```

### Test Contract

```kotlin
@Test
fun `sink receives all buffered data before close`() = runTest {
    val channel = Channel<Any>(capacity = 3)
    val received = mutableListOf<Any>()
    val component = SinkComponent()
    component.setInputChannel("input", channel)

    // Buffer data then close
    channel.send("a")
    channel.send("b")
    channel.send("c")
    channel.close()

    // Start consuming
    val job = launch {
        for (data in channel) {
            received.add(data)
        }
    }

    advanceUntilIdle()

    assertEquals(listOf("a", "b", "c"), received)
    assertFalse(job.isActive, "Consumer should complete")
}

@Test
fun `sink handles channel closure gracefully`() = runTest {
    val channel = Channel<Any>(capacity = 1)
    val component = SinkComponent()
    component.setInputChannel("input", channel)

    channel.close()

    // Should not throw
    val job = launch {
        for (data in channel) {
            // No data, loop exits immediately
        }
    }

    advanceUntilIdle()
    assertFalse(job.isActive, "Consumer should exit cleanly")
}
```

## Transformer Component Contract

### Interface

```kotlin
interface ChannelTransformer : ChannelInputProvider, ChannelOutputProvider {
    // Combines both interfaces
}
```

### Implementation Pattern

```kotlin
class TransformerComponent : ProcessingLogic, ChannelTransformer {
    override val inputChannels = mutableMapOf<String, ReceiveChannel<Any>>()
    override val outputChannels = mutableMapOf<String, SendChannel<Any>>()

    private var transformJob: Job? = null

    suspend fun start(scope: CoroutineScope) {
        val inputChannel = inputChannels["input"]!!
        val outputChannel = outputChannels["output"]!!

        transformJob = scope.launch {
            try {
                for (input in inputChannel) {
                    val output = transform(input)
                    outputChannel.send(output)  // Backpressure propagates upstream
                }
            } finally {
                outputChannel.close()  // Propagate close downstream
            }
        }
    }

    fun stop() {
        transformJob?.cancel()
        closeOutputChannels()
    }

    private fun transform(input: Any): Any {
        // Transform logic
        return input
    }
}
```

### Test Contract

```kotlin
@Test
fun `transformer propagates backpressure upstream`() = runTest {
    val inputChannel = Channel<Int>(capacity = 1)
    val outputChannel = Channel<Int>(capacity = 1)

    val component = TransformerComponent()
    component.setInputChannel("input", inputChannel)
    component.setOutputChannel("output", outputChannel)

    // Fill output buffer
    outputChannel.send(0)

    // Start transformer
    component.start(this)

    // Send to input - should eventually block due to output full
    inputChannel.send(1)

    // Transformer should be blocked trying to send to output
    val sendJob = launch { inputChannel.send(2) }
    advanceTimeBy(100)

    // Input buffer is full because transformer is blocked
    assertTrue(sendJob.isActive, "Upstream should be blocked")

    // Consume output to unblock
    outputChannel.receive()  // Gets 0
    outputChannel.receive()  // Gets transformed 1
    advanceUntilIdle()

    assertFalse(sendJob.isActive, "Upstream should unblock")

    inputChannel.close()
    component.stop()
}
```

## Flow Orchestrator Contract

### Channel Creation

```kotlin
class FlowOrchestrator {
    private val channels = mutableMapOf<String, Channel<Any>>()

    fun createChannel(connectionId: String, capacity: Int): Channel<Any> {
        val channel = when (capacity) {
            0 -> Channel(Channel.RENDEZVOUS)
            -1 -> Channel(Channel.UNLIMITED)
            else -> Channel(capacity)
        }
        channels[connectionId] = channel
        return channel
    }

    fun wireConnection(
        connection: Connection,
        sourceComponent: ChannelOutputProvider,
        targetComponent: ChannelInputProvider,
        sourcePortName: String,
        targetPortName: String
    ) {
        val channel = createChannel(connection.id, connection.channelCapacity)

        // Same channel, different views
        sourceComponent.setOutputChannel(sourcePortName, channel)
        targetComponent.setInputChannel(targetPortName, channel)
    }

    fun closeAllChannels() {
        channels.values.forEach { it.close() }
    }

    fun cancelAllChannels() {
        channels.values.forEach { it.cancel() }
    }
}
```

### Test Contract

```kotlin
@Test
fun `orchestrator creates channel with correct capacity`() = runTest {
    val orchestrator = FlowOrchestrator()

    val rendezvousChannel = orchestrator.createChannel("conn1", 0)
    val bufferedChannel = orchestrator.createChannel("conn2", 5)
    val unlimitedChannel = orchestrator.createChannel("conn3", -1)

    // Rendezvous: send blocks immediately
    val rendezvousSend = launch { rendezvousChannel.send("data") }
    advanceTimeBy(100)
    assertTrue(rendezvousSend.isActive, "Rendezvous should block")
    rendezvousSend.cancel()

    // Buffered: can send up to capacity
    repeat(5) { bufferedChannel.send(it) }
    val bufferedSend = launch { bufferedChannel.send(5) }
    advanceTimeBy(100)
    assertTrue(bufferedSend.isActive, "Should block at capacity")
    bufferedSend.cancel()

    // Unlimited: never blocks
    repeat(1000) { unlimitedChannel.send(it) }
    // No blocking

    orchestrator.closeAllChannels()
}
```

## Backpressure Verification

### Test: Bounded Buffer Backpressure

```kotlin
@Test
fun `connection applies backpressure at capacity`() = runTest {
    val channel = Channel<Int>(capacity = 2)
    val sendCount = atomic(0)

    // Producer tries to send 5 items
    val producer = launch {
        repeat(5) { i ->
            channel.send(i)
            sendCount.incrementAndGet()
        }
    }

    // Let producer run but don't consume
    advanceTimeBy(100)

    // Should be blocked after 2 (buffer size)
    assertEquals(2, sendCount.value, "Producer should block at buffer capacity")

    // Consumer starts
    var consumed = 0
    val consumer = launch {
        repeat(5) {
            channel.receive()
            consumed++
        }
    }

    advanceUntilIdle()

    assertEquals(5, consumed, "All items should be consumed")
    assertFalse(producer.isActive, "Producer should complete")

    channel.close()
}
```

### Test: Rendezvous Channel

```kotlin
@Test
fun `rendezvous channel blocks until receiver ready`() = runTest {
    val channel = Channel<Int>(Channel.RENDEZVOUS)
    var sent = false

    val producer = launch {
        channel.send(1)
        sent = true
    }

    advanceTimeBy(100)
    assertFalse(sent, "Send should block without receiver")

    launch { channel.receive() }
    advanceUntilIdle()

    assertTrue(sent, "Send should complete after receiver ready")

    channel.close()
}
```

## Graceful Shutdown

### Shutdown Sequence

```kotlin
fun shutdown() {
    // 1. Stop producers first (no more sends)
    producers.forEach { it.stop() }

    // 2. Close all channels (signals end of stream)
    channels.values.forEach { it.close() }

    // 3. Wait for consumers to drain
    runBlocking {
        withTimeout(5000) {
            consumers.forEach { it.awaitCompletion() }
        }
    }

    // 4. Cancel scope
    flowScope?.cancel()
}
```

### Test: Graceful Shutdown

```kotlin
@Test
fun `buffered data is consumed on graceful shutdown`() = runTest {
    val channel = Channel<Int>(capacity = 5)
    val received = mutableListOf<Int>()

    // Buffer some data
    repeat(3) { channel.send(it) }

    // Start consumer
    val consumer = launch {
        for (data in channel) {
            received.add(data)
        }
    }

    // Close channel (graceful)
    channel.close()

    advanceUntilIdle()

    assertEquals(listOf(0, 1, 2), received, "All buffered data should be received")
    assertFalse(consumer.isActive, "Consumer should complete")
}
```
