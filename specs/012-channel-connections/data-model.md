# Data Model: Channel-Based Connections

## Channel Capacity Mapping

### Connection Model (Existing)

```kotlin
// fbpDsl/.../Connection.kt
data class Connection(
    val id: String,
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String,
    val channelCapacity: Int = 0,  // ← Maps directly to Channel(capacity)
    // ...
)
```

### Capacity Values

| Value | Name | Channel Constructor | Behavior |
|-------|------|---------------------|----------|
| 0 | Rendezvous | `Channel(Channel.RENDEZVOUS)` | Sender suspends until receiver ready |
| 1-N | Buffered | `Channel(N)` | N packets can be buffered before backpressure |
| -1 | Unlimited | `Channel(Channel.UNLIMITED)` | No backpressure (use with caution) |

### Channel Factory Function

```kotlin
import kotlinx.coroutines.channels.Channel

fun createChannelForConnection(connection: Connection): Channel<Any> {
    return when (connection.channelCapacity) {
        0 -> Channel(Channel.RENDEZVOUS)
        -1 -> Channel(Channel.UNLIMITED)
        else -> Channel(connection.channelCapacity)
    }
}
```

## Channel Interfaces

### SendChannel (Producer Side)

```kotlin
interface SendChannel<in E> : AutoCloseable {
    // Core operations
    suspend fun send(element: E)           // Suspends if buffer full
    fun close(cause: Throwable? = null): Boolean  // Signal no more data

    // Non-suspending alternatives
    fun trySend(element: E): ChannelResult<Unit>  // Returns immediately
    val isClosedForSend: Boolean           // Check if closed
}
```

Usage in generator components:
```kotlin
class GeneratorComponent {
    var outputChannel: SendChannel<Output>? = null

    suspend fun emit(data: Output) {
        outputChannel?.send(data)  // Backpressure applied here
    }

    fun stop() {
        outputChannel?.close()  // Signal end of stream
    }
}
```

### ReceiveChannel (Consumer Side)

```kotlin
interface ReceiveChannel<out E> {
    // Core operations
    suspend fun receive(): E               // Suspends if empty
    fun cancel(cause: CancellationException? = null)  // Abort receiving

    // Non-suspending alternatives
    fun tryReceive(): ChannelResult<E>     // Returns immediately
    val isClosedForReceive: Boolean        // Check if closed and empty

    // Iteration support
    operator fun iterator(): ChannelIterator<E>
}
```

Usage in sink components:
```kotlin
class SinkComponent {
    var inputChannel: ReceiveChannel<Input>? = null

    suspend fun startReceiving(scope: CoroutineScope) {
        scope.launch {
            for (data in inputChannel!!) {  // Iterates until channel closed
                process(data)
            }
        }
    }
}
```

### Channel (Both Sides)

```kotlin
interface Channel<E> : SendChannel<E>, ReceiveChannel<E> {
    // Combines both interfaces
    // Used for channel creation, then split into Send/Receive
}
```

## Generated Code Structure

### Flow Class Channels

```kotlin
class GeneratedFlow {
    // Channels created from Connection.channelCapacity
    private val channel_conn_seconds = Channel<Any>(capacity = 1)
    private val channel_conn_minutes = Channel<Any>(capacity = 1)

    // Components
    internal val timerEmitter = TimerEmitterComponent()
    internal val displayReceiver = DisplayReceiverComponent()

    suspend fun start(scope: CoroutineScope) {
        // Wire channels to components (SendChannel/ReceiveChannel views)
        timerEmitter.secondsOutput = channel_conn_seconds
        timerEmitter.minutesOutput = channel_conn_minutes
        displayReceiver.secondsInput = channel_conn_seconds
        displayReceiver.minutesInput = channel_conn_minutes

        // Start components
        timerEmitter.start(scope)
        displayReceiver.start(scope)
    }

    fun stop() {
        // Close channels gracefully (allows buffered data to drain)
        channel_conn_seconds.close()
        channel_conn_minutes.close()

        // Stop components
        timerEmitter.stop()
        displayReceiver.stop()
    }
}
```

## State Transitions

### Channel Lifecycle States

```
CREATED → OPEN → CLOSED → CANCELLED
                    ↓
           (buffered data still receivable)
```

1. **CREATED**: Channel instantiated via `Channel(capacity)`
2. **OPEN**: Active send/receive operations
3. **CLOSED**: `close()` called - no more sends, buffered data receivable
4. **CANCELLED**: `cancel()` called - all operations abort

### Channel State Checks

```kotlin
// Producer side
channel.isClosedForSend  // true after close() or cancel()

// Consumer side
channel.isClosedForReceive  // true after close() AND buffer empty
```

### ExecutionState Mapping

| Flow ExecutionState | Channel Operations |
|--------------------|-------------------|
| IDLE | Channels not yet wired |
| RUNNING | send()/receive() active |
| PAUSED | Components paused, channels still open |
| ERROR | May close channels with cause |

## Error Handling

### ClosedSendChannelException

Thrown when calling `send()` on a closed channel:

```kotlin
try {
    channel.send(data)
} catch (e: ClosedSendChannelException) {
    // Channel was closed, handle gracefully
}
```

### ClosedReceiveChannelException

Thrown when calling `receive()` on a closed and empty channel:

```kotlin
try {
    val data = channel.receive()
} catch (e: ClosedReceiveChannelException) {
    // No more data, channel closed
}
```

### Iteration Pattern (Recommended)

Using `for` loop handles closure automatically:

```kotlin
for (data in channel) {
    process(data)
}
// Loop exits when channel is closed and empty
```

## Type Safety

### Generic Channel Types

```kotlin
// Type-safe channel
val timerChannel: Channel<TimerOutput> = Channel(capacity = 1)

// SendChannel view (producer can only send)
val sendSide: SendChannel<TimerOutput> = timerChannel

// ReceiveChannel view (consumer can only receive)
val receiveSide: ReceiveChannel<TimerOutput> = timerChannel
```

### InformationPacket Wrapper

For FBP metadata support:

```kotlin
data class InformationPacket<T>(
    val data: T,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

// Channel carrying packets
val channel: Channel<InformationPacket<TimerOutput>> = Channel(1)
```

## Validation Rules

### Capacity Validation

```kotlin
init {
    require(channelCapacity >= -1) {
        "Channel capacity must be >= -1 (unlimited), got $channelCapacity"
    }
}
```

### Connection Constraints

- Each Connection creates exactly one Channel
- Channel has one SendChannel side (producer) and one ReceiveChannel side (consumer)
- FBP semantics: single producer, single consumer per channel
- Fan-out requires multiple connections (multiple channels)
- Fan-in requires multiple connections (multiple channels to same target)
