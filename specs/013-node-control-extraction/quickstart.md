# Quickstart: Node Control Extraction

## Overview

This guide shows how to add lifecycle control (nodeControlJob, start, stop, pause, resume) to CodeNode and refactor components to use it.

## Step 1: Add nodeControlJob to CodeNode

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`

### Add Import

```kotlin
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
```

### Add Property (after line ~159, near processingLogic)

```kotlin
@Transient
var nodeControlJob: Job? = null
```

## Step 2: Add start() Method to CodeNode

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`

### Add After process() method (~line 345)

```kotlin
/**
 * Starts the node's processing loop.
 *
 * Manages the nodeControlJob lifecycle:
 * 1. Cancels any existing job (prevents duplicate jobs)
 * 2. Transitions executionState to RUNNING
 * 3. Launches new job in provided scope
 * 4. Executes processingBlock within the job
 *
 * @param scope CoroutineScope to launch the processing job in
 * @param processingBlock Custom processing logic to execute in the job loop
 */
suspend fun start(
    scope: CoroutineScope,
    processingBlock: suspend () -> Unit
) {
    // Cancel existing job if running (prevents duplicate jobs)
    nodeControlJob?.cancel()

    // Note: executionState must be set to RUNNING before launch
    // This is typically done by the caller before calling start()
    // or we could return a new CodeNode with RUNNING state

    // Launch the processing job
    nodeControlJob = scope.launch {
        processingBlock()
    }
}
```

## Step 3: Add stop() Method to CodeNode

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`

### Add After start() method

```kotlin
/**
 * Stops the node's processing loop.
 *
 * Manages graceful shutdown:
 * 1. Cancels the nodeControlJob
 * 2. Sets nodeControlJob to null
 *
 * Note: executionState transition to IDLE should be handled
 * by the caller or via withExecutionState() for immutability.
 */
fun stop() {
    nodeControlJob?.cancel()
    nodeControlJob = null
}
```

## Step 3b: Add pause() and resume() Methods to CodeNode

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`

### Add After stop() method

```kotlin
/**
 * Pauses the node's processing loop.
 *
 * Suspends job execution without cancelling:
 * 1. Only valid when state is RUNNING
 * 2. Transitions executionState to PAUSED
 * 3. Job remains active but processing should check isPaused()
 *
 * Note: The processing loop must check isPaused() to honor pause requests.
 */
fun pause() {
    if (executionState != ExecutionState.RUNNING) return
    // Note: For data class immutability, caller should use withExecutionState()
    // This method signals intent; actual state update depends on architecture choice
}

/**
 * Resumes the node's processing loop from paused state.
 *
 * Continues job execution:
 * 1. Only valid when state is PAUSED
 * 2. Transitions executionState to RUNNING
 *
 * Note: The processing loop should resume when isPaused() returns false.
 */
fun resume() {
    if (executionState != ExecutionState.PAUSED) return
    // Note: For data class immutability, caller should use withExecutionState()
    // This method signals intent; actual state update depends on architecture choice
}
```

### Processing Loop with Pause Support

Components should check for pause state in their processing loops:

```kotlin
suspend fun start(scope: CoroutineScope) {
    val node = codeNode ?: return
    node.start(scope) {
        while (isActive && !node.isIdle()) {
            // Honor pause state
            while (node.isPaused()) {
                delay(50) // Check periodically
                if (node.isIdle()) return@start // Stop was called while paused
            }

            // Processing logic here
            delay(speedAttenuation)
            // ... business logic ...
        }
    }
}
```

## Step 4: Refactor TimerEmitterComponent

**File**: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt`

### Current Implementation

```kotlin
class TimerEmitterComponent(...) : ProcessingLogic {
    var executionState: ExecutionState = ExecutionState.IDLE
    private var timerJob: Job? = null

    suspend fun start(scope: CoroutineScope) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && executionState == ExecutionState.RUNNING) {
                delay(speedAttenuation)
                if (executionState != ExecutionState.RUNNING) break
                // ... timer logic ...
            }
        }
    }

    fun stop() {
        executionState = ExecutionState.IDLE
        timerJob?.cancel()
        timerJob = null
    }
}
```

### Refactored Implementation (Option A: Composition)

```kotlin
class TimerEmitterComponent(
    private val speedAttenuation: Long = 1000L,
    initialSeconds: Int = 0,
    initialMinutes: Int = 0
) : ProcessingLogic {

    // CodeNode reference for lifecycle delegation
    var codeNode: CodeNode? = null

    var outputChannel: SendChannel<TimerOutput>? = null

    // State flows remain for observable state
    private val _elapsedSeconds = MutableStateFlow(initialSeconds)
    val elapsedSecondsFlow: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _elapsedMinutes = MutableStateFlow(initialMinutes)
    val elapsedMinutesFlow: StateFlow<Int> = _elapsedMinutes.asStateFlow()

    // Convenience accessors
    var executionState: ExecutionState
        get() = codeNode?.executionState ?: ExecutionState.IDLE
        set(value) { /* Update via codeNode.withExecutionState() */ }

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        return mapOf(
            "elapsedSeconds" to InformationPacketFactory.create(_elapsedSeconds.value),
            "elapsedMinutes" to InformationPacketFactory.create(_elapsedMinutes.value)
        )
    }

    suspend fun start(scope: CoroutineScope) {
        val node = codeNode ?: return
        node.start(scope) {
            while (isActive && node.isRunning()) {
                delay(speedAttenuation)
                if (!node.isRunning()) break

                var newSeconds = _elapsedSeconds.value + 1
                var newMinutes = _elapsedMinutes.value

                if (newSeconds >= 60) {
                    newSeconds = 0
                    newMinutes += 1
                }

                _elapsedSeconds.value = newSeconds
                _elapsedMinutes.value = newMinutes

                val timerOutput = TimerOutput(newSeconds, newMinutes)
                try {
                    outputChannel?.send(timerOutput)
                } catch (e: ClosedSendChannelException) {
                    break
                }
            }
        }
    }

    fun stop() {
        codeNode?.stop()
    }

    fun reset() {
        stop()
        _elapsedSeconds.value = 0
        _elapsedMinutes.value = 0
    }
}
```

## Step 5: Refactor DisplayReceiverComponent

**File**: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt`

### Refactored Implementation

```kotlin
class DisplayReceiverComponent : ProcessingLogic {

    // CodeNode reference for lifecycle delegation
    var codeNode: CodeNode? = null

    var inputChannel: ReceiveChannel<TimerOutput>? = null

    private val _displayedSeconds = MutableStateFlow(0)
    val displayedSecondsFlow: StateFlow<Int> = _displayedSeconds.asStateFlow()

    private val _displayedMinutes = MutableStateFlow(0)
    val displayedMinutesFlow: StateFlow<Int> = _displayedMinutes.asStateFlow()

    override suspend fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>> {
        inputs["seconds"]?.let { packet ->
            @Suppress("UNCHECKED_CAST")
            (packet as? InformationPacket<Int>)?.payload?.let { receiveSeconds(it) }
        }
        inputs["minutes"]?.let { packet ->
            @Suppress("UNCHECKED_CAST")
            (packet as? InformationPacket<Int>)?.payload?.let { receiveMinutes(it) }
        }
        return emptyMap()
    }

    suspend fun start(scope: CoroutineScope) {
        val node = codeNode ?: return
        val channel = inputChannel ?: return

        node.start(scope) {
            try {
                for (timerOutput in channel) {
                    receiveSeconds(timerOutput.elapsedSeconds)
                    receiveMinutes(timerOutput.elapsedMinutes)
                }
            } catch (e: ClosedReceiveChannelException) {
                // Graceful shutdown
            }
        }
    }

    fun stop() {
        codeNode?.stop()
    }

    fun receiveSeconds(seconds: Int) {
        _displayedSeconds.value = seconds
    }

    fun receiveMinutes(minutes: Int) {
        _displayedMinutes.value = minutes
    }
}
```

## Step 6: Write Unit Tests for CodeNode Lifecycle

**File**: `fbpDsl/src/commonTest/kotlin/io/codenode/fbpdsl/model/CodeNodeLifecycleTest.kt`

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class CodeNodeLifecycleTest {

    @Test
    fun `start creates nodeControlJob and runs processingBlock`() = runTest {
        val codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0)
        )

        var blockExecuted = false
        codeNode.start(this) {
            blockExecuted = true
        }

        advanceUntilIdle()

        assertTrue(blockExecuted, "Processing block should execute")
        assertNotNull(codeNode.nodeControlJob, "Job should be created")
    }

    @Test
    fun `stop cancels nodeControlJob`() = runTest {
        val codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0)
        )

        codeNode.start(this) {
            while (true) { delay(100) }
        }

        codeNode.stop()

        assertNull(codeNode.nodeControlJob, "Job should be null after stop")
    }

    @Test
    fun `start cancels existing job before creating new one`() = runTest {
        val codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0)
        )

        var firstJobStarted = false
        var secondJobStarted = false

        codeNode.start(this) {
            firstJobStarted = true
            while (true) { delay(100) }
        }
        advanceTimeBy(50)

        val firstJob = codeNode.nodeControlJob

        codeNode.start(this) {
            secondJobStarted = true
        }
        advanceUntilIdle()

        assertTrue(firstJob?.isCancelled == true, "First job should be cancelled")
        assertTrue(secondJobStarted, "Second job should start")
    }

    @Test
    fun `stop on idle node is no-op`() = runTest {
        val codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0),
            executionState = ExecutionState.IDLE
        )

        // Should not throw
        codeNode.stop()

        assertNull(codeNode.nodeControlJob, "Job should remain null")
        assertTrue(codeNode.isIdle(), "State should remain IDLE")
    }

    @Test
    fun `stop on paused node transitions to IDLE`() = runTest {
        var codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0),
            executionState = ExecutionState.RUNNING
        )

        codeNode.start(this) {
            while (true) { delay(100) }
        }

        // Pause the node
        codeNode = codeNode.withExecutionState(ExecutionState.PAUSED)

        // Stop from paused state should work
        codeNode.stop()

        assertNull(codeNode.nodeControlJob, "Job should be cancelled and null")
    }

    @Test
    fun `pause on running node transitions to PAUSED`() = runTest {
        var codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0),
            executionState = ExecutionState.RUNNING
        )

        codeNode.start(this) {
            while (true) { delay(100) }
        }

        codeNode = codeNode.withExecutionState(ExecutionState.PAUSED)
        codeNode.pause()

        assertTrue(codeNode.isPaused(), "Node should be in PAUSED state")
        assertNotNull(codeNode.nodeControlJob, "Job should still exist (not cancelled)")
    }

    @Test
    fun `resume from paused transitions back to RUNNING`() = runTest {
        var codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0),
            executionState = ExecutionState.PAUSED
        )

        codeNode = codeNode.withExecutionState(ExecutionState.RUNNING)
        codeNode.resume()

        assertTrue(codeNode.isRunning(), "Node should be in RUNNING state")
    }

    @Test
    fun `pause on non-running node is no-op`() = runTest {
        val codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0),
            executionState = ExecutionState.IDLE
        )

        // Should not throw
        codeNode.pause()

        assertTrue(codeNode.isIdle(), "State should remain IDLE")
    }

    @Test
    fun `resume on non-paused node is no-op`() = runTest {
        val codeNode = CodeNode(
            id = "test-node",
            name = "TestNode",
            codeNodeType = CodeNodeType.GENERATOR,
            position = Node.Position(0.0, 0.0),
            executionState = ExecutionState.RUNNING
        )

        // Should not throw
        codeNode.resume()

        assertTrue(codeNode.isRunning(), "State should remain RUNNING")
    }
}
```

## Verification Checklist

- [ ] CodeNode has `nodeControlJob: Job?` property
- [ ] CodeNode has `start(scope, processingBlock)` method
- [ ] CodeNode has `stop()` method
- [ ] CodeNode has `pause()` method
- [ ] CodeNode has `resume()` method
- [ ] `start()` cancels existing job before creating new one
- [ ] `stop()` on RUNNING transitions to IDLE (job cancelled)
- [ ] `stop()` on PAUSED transitions to IDLE (job cancelled)
- [ ] `stop()` on IDLE is a no-op
- [ ] `pause()` on RUNNING transitions to PAUSED (job not cancelled)
- [ ] `resume()` on PAUSED transitions to RUNNING
- [ ] `pause()` on non-RUNNING is a no-op
- [ ] `resume()` on non-PAUSED is a no-op
- [ ] TimerEmitterComponent delegates to CodeNode
- [ ] DisplayReceiverComponent delegates to CodeNode
- [ ] All existing TimerEmitterComponentTest tests pass
- [ ] All existing DisplayReceiverComponentTest tests pass
- [ ] All existing ChannelIntegrationTest tests pass
- [ ] Run: `./gradlew :fbpDsl:jvmTest :StopWatch:jvmTest`

## Common Issues

### Issue: "Job is cancelled" during stop

**Cause**: Calling stop() while processing block is still running.

**Solution**: Use `isActive` check in processing loops:
```kotlin
while (isActive && codeNode.isRunning()) {
    // processing
}
```

### Issue: Duplicate jobs running

**Cause**: start() called without cancelling existing job.

**Solution**: Always cancel existing job at start of start():
```kotlin
suspend fun start(scope: CoroutineScope, block: suspend () -> Unit) {
    nodeControlJob?.cancel()  // Always cancel first
    nodeControlJob = scope.launch { block() }
}
```

### Issue: executionState not updating

**Cause**: CodeNode is a data class - need to handle state separately.

**Solution**: Components set executionState before calling start():
```kotlin
// In component
executionState = ExecutionState.RUNNING
codeNode.start(scope) { ... }

// Or use withExecutionState
codeNode = codeNode.withExecutionState(ExecutionState.RUNNING)
```
