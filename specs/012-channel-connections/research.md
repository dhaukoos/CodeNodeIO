# Research: Channel-Based Connections

## Decision Summary

**Decision**: Use raw `Channel<T>` from kotlinx.coroutines.channels for implementing FBP connections.

**Rationale**:
- Channels are the canonical FBP primitive - bounded buffers between processes
- Explicit send/receive semantics match FBP's message-passing model
- Single-consumer guarantee aligns with FBP connection semantics (one source, one target)
- Built-in backpressure via suspending send() when buffer full
- Clean close() semantics for graceful shutdown
- Direct mapping: Connection.channelCapacity → Channel(capacity)

**Alternatives Considered**:
1. ~~MutableSharedFlow~~: Designed for broadcast (multi-consumer), not point-to-point connections
2. ~~BroadcastChannel~~: Deprecated in favor of SharedFlow
3. ~~StateFlow~~: Single-value only, not suitable for packet streams

## Why Channel<T> Over MutableSharedFlow

| Aspect | Channel<T> | MutableSharedFlow |
|--------|-----------|-------------------|
| **Semantics** | Point-to-point (FBP) | Broadcast (pub-sub) |
| **Consumers** | Single consumer | Multiple collectors |
| **Buffering** | `Channel(capacity)` | `extraBufferCapacity` |
| **Close** | Explicit `close()` | No close concept |
| **Receive** | `receive()` consumes | `collect()` observes |
| **FBP Alignment** | Direct match | Adapted usage |

## Current Architecture Analysis

### MutableSharedFlow Usage (To Be Replaced)

**TimerEmitterComponent** (`StopWatch/.../TimerEmitterComponent.kt:52`):
```kotlin
val output = MutableSharedFlow<TimerOutput>(replay = 1)
```

**DisplayReceiverComponent** (`StopWatch/.../DisplayReceiverComponent.kt:30`):
```kotlin
val input = MutableSharedFlow<TimerOutput>(replay = 1)
```

### Code Generation (Current - To Be Changed)

**ModuleGenerator.kt:406** generates:
```kotlin
private val channel_conn_XXX = MutableSharedFlow<Any>(replay = 1)
```

**ModuleGenerator.kt:446-459** wires:
```kotlin
scope.launch {
    sourceComponent.output.collect { data ->
        targetComponent.input.emit(data)
    }
}
```

## Channel Capacity Mapping

### Channel Factory Functions

```kotlin
import kotlinx.coroutines.channels.Channel

// Capacity mapping
fun createChannel(capacity: Int): Channel<Any> = when (capacity) {
    0 -> Channel(Channel.RENDEZVOUS)      // Sender blocks until receiver ready
    -1 -> Channel(Channel.UNLIMITED)       // No backpressure
    else -> Channel(capacity)              // Buffered with backpressure
}
```

### Mapping Table

| Connection.channelCapacity | Channel Configuration | Behavior |
|---------------------------|----------------------|----------|
| 0 | `Channel(Channel.RENDEZVOUS)` | Sender suspends until receiver calls receive() |
| 1-N | `Channel(N)` | N packets buffered, then sender suspends |
| -1 | `Channel(Channel.UNLIMITED)` | No backpressure (use with caution) |

### Channel Constants

```kotlin
object Channel {
    const val RENDEZVOUS = 0      // No buffer
    const val UNLIMITED = Int.MAX_VALUE  // Unlimited buffer
    const val CONFLATED = -1      // Keep only latest (drop older)
    const val BUFFERED = 64       // Default buffer size
}
```

## Implementation Pattern

### Generated Channel Declaration

**Before** (current):
```kotlin
private val channel_conn_XXX = MutableSharedFlow<Any>(replay = 1)
```

**After** (proposed):
```kotlin
private val channel_conn_XXX = Channel<Any>(capacity = 1)
```

### Generated Wiring Code

**Before** (current):
```kotlin
scope.launch {
    sourceComponent.output.collect { data ->
        targetComponent.input.emit(data)
    }
}
```

**After** (proposed):
```kotlin
// Wire source output to channel
scope.launch {
    for (data in sourceComponent.outputChannel) {
        channel_conn_XXX.send(data)
    }
}

// Wire channel to target input
scope.launch {
    for (data in channel_conn_XXX) {
        targetComponent.receive(data)
    }
}
```

Or more directly - components receive channel references:
```kotlin
// In Flow.start():
sourceComponent.outputChannel = channel_conn_XXX
targetComponent.inputChannel = channel_conn_XXX
```

## Component Interface Pattern

### Generator Component (outputs only)

```kotlin
class TimerEmitterComponent : ProcessingLogic {
    // Output channel provided by orchestrator
    var outputChannel: SendChannel<TimerOutput>? = null

    private var timerJob: Job? = null
    var executionState: ExecutionState = ExecutionState.IDLE

    suspend fun start(scope: CoroutineScope) {
        executionState = ExecutionState.RUNNING
        timerJob = scope.launch {
            var seconds = 0
            var minutes = 0
            while (isActive && executionState == ExecutionState.RUNNING) {
                delay(speedAttenuation)
                seconds++
                if (seconds >= 60) {
                    seconds = 0
                    minutes++
                }
                // Send to channel - suspends if buffer full (backpressure)
                outputChannel?.send(TimerOutput(seconds, minutes))
            }
        }
    }

    fun stop() {
        executionState = ExecutionState.IDLE
        timerJob?.cancel()
        outputChannel?.close()  // Signal no more data
    }
}
```

### Sink Component (inputs only)

```kotlin
class DisplayReceiverComponent : ProcessingLogic {
    // Input channel provided by orchestrator
    var inputChannel: ReceiveChannel<TimerOutput>? = null

    private var receiveJob: Job? = null
    private val _displayedSeconds = MutableStateFlow(0)
    private val _displayedMinutes = MutableStateFlow(0)

    suspend fun start(scope: CoroutineScope) {
        receiveJob = scope.launch {
            try {
                for (timerOutput in inputChannel!!) {  // Iterate until closed
                    _displayedSeconds.value = timerOutput.elapsedSeconds
                    _displayedMinutes.value = timerOutput.elapsedMinutes
                }
            } catch (e: ClosedReceiveChannelException) {
                // Channel closed, normal shutdown
            }
        }
    }

    fun stop() {
        receiveJob?.cancel()
    }
}
```

### Transformer Component (inputs and outputs)

```kotlin
class TransformerComponent : ProcessingLogic {
    var inputChannel: ReceiveChannel<Input>? = null
    var outputChannel: SendChannel<Output>? = null

    suspend fun start(scope: CoroutineScope) {
        scope.launch {
            try {
                for (input in inputChannel!!) {
                    val output = transform(input)
                    outputChannel?.send(output)
                }
            } finally {
                outputChannel?.close()  // Propagate close downstream
            }
        }
    }
}
```

## Backpressure Behavior

When buffer is full and sender calls `send()`:
1. Sender coroutine suspends
2. Sender remains suspended until receiver calls `receive()`
3. This is true backpressure - no data loss, no configuration needed

Testing backpressure:
```kotlin
@Test
fun `channel applies backpressure when buffer full`() = runTest {
    val channel = Channel<Int>(capacity = 2)

    // Fill buffer
    channel.send(1)  // Buffered
    channel.send(2)  // Buffered

    // This should suspend
    val sendJob = launch { channel.send(3) }
    advanceUntilIdle()
    assertTrue(sendJob.isActive, "Send should be suspended")

    // Receive one, allowing send to complete
    val received = channel.receive()
    assertEquals(1, received)
    advanceUntilIdle()
    assertFalse(sendJob.isActive, "Send should complete after receive")

    channel.close()
}
```

## Channel Lifecycle

### Creation
- Channels created in Flow class initialization
- Capacity set from Connection.channelCapacity
- `Channel<T>(capacity)` constructor

### Wiring
- SendChannel side given to producer component
- ReceiveChannel side given to consumer component
- Same channel instance, different interfaces

### Operation
- Producer calls `send()` - suspends if buffer full
- Consumer calls `receive()` or iterates with `for (item in channel)`
- Backpressure automatic

### Shutdown
1. **Graceful**: Producer calls `channel.close()`
   - No more sends allowed (throws ClosedSendChannelException)
   - Buffered data still receivable
   - After buffer empty, receivers see channel closed

2. **Abrupt**: Call `channel.cancel()`
   - All pending operations throw CancellationException
   - Buffered data discarded

```kotlin
fun stop() {
    // Graceful: close channels, let data drain
    channels.values.forEach { it.close() }

    // Then cancel scope
    flowScope?.cancel()
}
```

## Files to Modify

1. **ModuleGenerator.kt** - Channel creation with capacity
2. **FlowGenerator.kt** - Connection wiring with send/receive
3. **TimerEmitterComponent.kt** - SendChannel pattern
4. **DisplayReceiverComponent.kt** - ReceiveChannel pattern
5. **StopWatchFlow.kt** - Regenerate after generator changes

## Required Imports

```kotlin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
```

## Test Strategy

1. **Unit Tests**: Channel capacity mapping in generator
2. **Integration Tests**: Backpressure behavior verification
3. **Regression Tests**: Existing StopWatch functionality
4. **Shutdown Tests**: Graceful close with data drain
