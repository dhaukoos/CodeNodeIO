# Quickstart: Channel-Based Connections

## Overview

This guide shows how to implement channel-based connections in CodeNodeIO using raw Kotlin `Channel<T>` for true FBP point-to-point semantics with backpressure.

## Step 1: Update ModuleGenerator Channel Creation

**File**: `kotlinCompiler/.../generator/ModuleGenerator.kt`

### Current (Line ~406)
```kotlin
flowGraph.connections.forEach { connection ->
    appendLine("    private val channel_${connection.id.sanitize()} = MutableSharedFlow<Any>(replay = 1)")
}
```

### After
```kotlin
flowGraph.connections.forEach { connection ->
    val capacity = connection.channelCapacity
    val capacityArg = when (capacity) {
        0 -> "Channel.RENDEZVOUS"
        -1 -> "Channel.UNLIMITED"
        else -> "$capacity"
    }
    appendLine("    private val channel_${connection.id.sanitize()} = Channel<Any>($capacityArg)")
}
```

## Step 2: Add Required Imports to Generated Code

Ensure generated code includes Channel imports:

```kotlin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.ReceiveChannel
```

## Step 3: Update Connection Wiring

**File**: `kotlinCompiler/.../generator/ModuleGenerator.kt`

### Current (Lines ~446-459)
```kotlin
private fun generateWireConnections(): String {
    return buildString {
        appendLine("    private fun wireConnections(scope: CoroutineScope) {")
        flowGraph.connections.forEach { connection ->
            val sourceNode = findNode(connection.sourceNodeId)
            val targetNode = findNode(connection.targetNodeId)
            appendLine("        scope.launch {")
            appendLine("            ${sourceNode.name.camelCase()}.output.collect { data ->")
            appendLine("                ${targetNode.name.camelCase()}.input.emit(data)")
            appendLine("            }")
            appendLine("        }")
        }
        appendLine("    }")
    }
}
```

### After
```kotlin
private fun generateWireConnections(): String {
    return buildString {
        appendLine("    private fun wireConnections() {")
        flowGraph.connections.forEach { connection ->
            val sourceNode = findNode(connection.sourceNodeId)
            val targetNode = findNode(connection.targetNodeId)
            val channelName = "channel_${connection.id.sanitize()}"
            val sourcePortName = findPortName(connection.sourcePortId)
            val targetPortName = findPortName(connection.targetPortId)

            // Wire channel to components
            appendLine("        ${sourceNode.name.camelCase()}.${sourcePortName}Channel = $channelName")
            appendLine("        ${targetNode.name.camelCase()}.${targetPortName}Channel = $channelName")
        }
        appendLine("    }")
    }
}
```

## Step 4: Update Generator Component

**File**: `StopWatch/.../usecases/TimerEmitterComponent.kt`

### Current
```kotlin
class TimerEmitterComponent(
    private val speedAttenuation: Long = 1000L
) : ProcessingLogic {
    val output = MutableSharedFlow<TimerOutput>(replay = 1)

    suspend fun start(scope: CoroutineScope) {
        timerJob = scope.launch {
            while (isActive && executionState == ExecutionState.RUNNING) {
                delay(speedAttenuation)
                // ... calculate time ...
                output.emit(TimerOutput(newSeconds, newMinutes))
            }
        }
    }
}
```

### After
```kotlin
class TimerEmitterComponent(
    private val speedAttenuation: Long = 1000L
) : ProcessingLogic {
    // Channel provided by flow orchestrator
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
        timerJob = null
        // Note: Channel is owned and closed by the flow orchestrator, not the component
    }
}
```

## Step 5: Update Sink Component

**File**: `StopWatch/.../usecases/DisplayReceiverComponent.kt`

### Current
```kotlin
class DisplayReceiverComponent : ProcessingLogic {
    val input = MutableSharedFlow<TimerOutput>(replay = 1)

    suspend fun start(scope: CoroutineScope) {
        collectionJob = scope.launch {
            input.collect { timerOutput ->
                _displayedSeconds.value = timerOutput.elapsedSeconds
                _displayedMinutes.value = timerOutput.elapsedMinutes
            }
        }
    }
}
```

### After
```kotlin
class DisplayReceiverComponent : ProcessingLogic {
    // Channel provided by flow orchestrator
    var inputChannel: ReceiveChannel<TimerOutput>? = null

    private var receiveJob: Job? = null
    private val _displayedSeconds = MutableStateFlow(0)
    private val _displayedMinutes = MutableStateFlow(0)

    val displayedSeconds: StateFlow<Int> = _displayedSeconds.asStateFlow()
    val displayedMinutes: StateFlow<Int> = _displayedMinutes.asStateFlow()

    suspend fun start(scope: CoroutineScope) {
        receiveJob = scope.launch {
            try {
                for (timerOutput in inputChannel!!) {
                    _displayedSeconds.value = timerOutput.elapsedSeconds
                    _displayedMinutes.value = timerOutput.elapsedMinutes
                }
            } catch (e: ClosedReceiveChannelException) {
                // Normal shutdown
            }
        }
    }

    fun stop() {
        receiveJob?.cancel()
    }
}
```

## Step 6: Update Flow Class Stop Method

### Current
```kotlin
fun stop() {
    timerEmitter.stop()
    displayReceiver.stop()
}
```

### After
```kotlin
fun stop() {
    // Close channels first (graceful shutdown)
    channel_conn_seconds.close()
    channel_conn_minutes.close()

    // Then stop components
    timerEmitter.stop()
    displayReceiver.stop()
}
```

## Step 7: Regenerate StopWatch Module

After updating the generators:

```bash
./gradlew :kotlinCompiler:jvmTest --tests "*.ModuleGeneratorTest"
```

Then regenerate:

```bash
./gradlew :StopWatch:regenerate  # If task exists
# Or manually trigger regeneration
```

## Step 8: Verify Backpressure

Create a test to verify backpressure works:

```kotlin
@Test
fun `channel respects capacity from Connection`() = runTest {
    // Create connection with capacity 2
    val connection = Connection(
        id = "test_conn",
        sourceNodeId = "source",
        sourcePortId = "out",
        targetNodeId = "target",
        targetPortId = "in",
        channelCapacity = 2
    )

    // Create channel with connection's capacity
    val channel = Channel<Int>(connection.channelCapacity)

    // Fill buffer
    channel.send(1)
    channel.send(2)

    // Third send should suspend (backpressure)
    val sendJob = launch { channel.send(3) }
    advanceTimeBy(100)
    assertTrue(sendJob.isActive, "Backpressure should suspend sender")

    // Receive one
    channel.receive()
    advanceUntilIdle()

    assertFalse(sendJob.isActive, "Sender should complete after receive")

    channel.close()
}
```

## Verification Checklist

- [x] ModuleGenerator creates `Channel<Any>(capacity)` instead of `MutableSharedFlow`
- [x] Generated code includes channel imports
- [x] Components use `SendChannel` / `ReceiveChannel` interfaces
- [x] Generated code compiles without errors
- [x] StopWatch flow functions correctly
- [x] Backpressure test passes (ChannelBackpressureTest.kt)
- [x] Graceful shutdown test passes (ChannelIntegrationTest.kt)
- [x] All existing tests pass

**Verified**: 2026-02-12 - All tests pass with `./gradlew :kotlinCompiler:jvmTest :StopWatch:jvmTest`

## Common Issues

### Issue: "Unresolved reference: Channel"

**Solution**: Add import:
```kotlin
import kotlinx.coroutines.channels.Channel
```

### Issue: "Unresolved reference: SendChannel"

**Solution**: Add imports:
```kotlin
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.ReceiveChannel
```

### Issue: ClosedSendChannelException on send

**Cause**: Calling `send()` after channel is closed.

**Solution**: Check channel state or catch exception:
```kotlin
try {
    channel.send(data)
} catch (e: ClosedSendChannelException) {
    // Handle gracefully - channel was closed
}
```

Or use `trySend`:
```kotlin
val result = channel.trySend(data)
if (result.isFailure) {
    // Channel closed or full
}
```

### Issue: Data loss on shutdown

**Solution**: Close channels before cancelling scope to allow buffered data to drain:
```kotlin
fun stop() {
    // 1. Close channels (graceful)
    channels.forEach { it.close() }

    // 2. Give consumers time to drain
    // (Or await completion)

    // 3. Cancel scope
    flowScope?.cancel()
}
```

### Issue: Consumer doesn't exit after channel close

**Solution**: Use `for` loop instead of `while(true) { receive() }`:
```kotlin
// Good - exits when channel closes
for (data in channel) {
    process(data)
}

// Bad - throws exception when channel closes
while (true) {
    val data = channel.receive()  // Throws ClosedReceiveChannelException
    process(data)
}
```
