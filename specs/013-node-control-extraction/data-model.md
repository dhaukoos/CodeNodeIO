# Data Model: Node Control Extraction

**Feature Branch**: `013-node-control-extraction`
**Date**: 2026-02-13
**Spec**: [spec.md](./spec.md)

## Entity Changes

### CodeNode (Modified)

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`

**Added Properties**:

```kotlin
/**
 * Runtime job reference for node lifecycle control.
 * Tracks the active coroutine job when the node is running.
 * Marked @Transient as Job cannot be serialized.
 */
@Transient
var nodeControlJob: Job? = null
```

**Added Methods**:

```kotlin
/**
 * Starts the node's processing loop.
 *
 * @param scope CoroutineScope to launch the processing job in
 * @param processingBlock Custom processing logic executed in the job
 * @throws IllegalStateException if processingLogic is null and no processingBlock provided
 */
suspend fun start(
    scope: CoroutineScope,
    processingBlock: (suspend () -> Unit)? = null
)

/**
 * Stops the node's processing loop.
 * Cancels nodeControlJob and transitions to IDLE state.
 */
fun stop()

/**
 * Pauses the node's processing loop.
 * Suspends job execution without cancelling; transitions to PAUSED state.
 * The job remains active but processing is suspended.
 */
fun pause()

/**
 * Resumes the node's processing loop from paused state.
 * Continues job execution; transitions back to RUNNING state.
 */
fun resume()
```

**State Transition Diagram**:

```
                         start()
    ┌─────────────────────────────────────────────┐
    │                                             ▼
┌───────┐                                    ┌─────────┐
│ IDLE  │                                    │ RUNNING │
└───────┘                                    └─────────┘
    ▲                                          │    ▲
    │                                 pause()  │    │  resume()
    │                                          ▼    │
    │               stop()               ┌──────────┐
    └────────────────────────────────────│  PAUSED  │
                                         └──────────┘

Note: stop() transitions to IDLE from both RUNNING and PAUSED states.
```

### ExecutionState (Unchanged)

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/ExecutionState.kt`

No changes required. Existing states are sufficient:
- `IDLE` - Node not processing
- `RUNNING` - Node actively processing
- `PAUSED` - Node paused (future use)
- `ERROR` - Node in error state

### TimerEmitterComponent (Modified)

**File**: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/TimerEmitterComponent.kt`

**Removed**:
- `var executionState: ExecutionState` (delegated to CodeNode)
- `private var timerJob: Job?` (delegated to CodeNode)

**Modified**:
- `start()`: Delegates job management to CodeNode
- `stop()`: Delegates to CodeNode

**Retained**:
- Timer tick logic (business logic)
- StateFlows for elapsed time
- `outputChannel: SendChannel<TimerOutput>?`

### DisplayReceiverComponent (Modified)

**File**: `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/usecases/DisplayReceiverComponent.kt`

**Removed**:
- `private var collectionJob: Job?` (delegated to CodeNode)

**Modified**:
- `start()`: Delegates job management to CodeNode
- `stop()`: Delegates to CodeNode

**Retained**:
- Channel iteration logic (business logic)
- StateFlows for displayed time
- `inputChannel: ReceiveChannel<TimerOutput>?`

## Relationship Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      CodeNode (base)                         │
│─────────────────────────────────────────────────────────────│
│  + executionState: ExecutionState                           │
│  + nodeControlJob: Job?  [NEW]                              │
│  + processingLogic: ProcessingLogic?                        │
│─────────────────────────────────────────────────────────────│
│  + start(scope, processingBlock?)  [NEW]                    │
│  + stop()  [NEW]                                            │
│  + pause()  [NEW]                                           │
│  + resume()  [NEW]                                          │
│  + process(inputs)                                          │
│  + withExecutionState(state, propagate)                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ delegates to
                              ▼
┌─────────────────────────────────────────────────────────────┐
│               Component (implements ProcessingLogic)         │
│─────────────────────────────────────────────────────────────│
│  TimerEmitterComponent                                       │
│    - Timer tick loop (business logic)                        │
│    - outputChannel: SendChannel<TimerOutput>                 │
│                                                              │
│  DisplayReceiverComponent                                    │
│    - Channel iteration loop (business logic)                 │
│    - inputChannel: ReceiveChannel<TimerOutput>               │
└─────────────────────────────────────────────────────────────┘
```

## Migration Notes

### Before (TimerEmitterComponent)

```kotlin
class TimerEmitterComponent(...) : ProcessingLogic {
    var executionState: ExecutionState = ExecutionState.IDLE
    private var timerJob: Job? = null

    suspend fun start(scope: CoroutineScope) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && executionState == ExecutionState.RUNNING) {
                // timer logic
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

### After (TimerEmitterComponent with CodeNode delegation)

```kotlin
class TimerEmitterComponent(
    private val codeNode: CodeNode  // or uses composition
) : ProcessingLogic {

    suspend fun start(scope: CoroutineScope) {
        codeNode.start(scope) {
            while (codeNode.isRunning()) {
                // timer logic
            }
        }
    }

    fun stop() {
        codeNode.stop()
    }
}
```

## Validation Rules

1. `start()` on already-running node MUST cancel existing job first
2. `stop()` is valid when state is RUNNING or PAUSED
3. `stop()` on IDLE node MUST be a no-op (no errors)
4. `nodeControlJob` MUST be set to null after stop
5. `executionState` MUST transition to RUNNING on start
6. `executionState` MUST transition to IDLE on stop (from RUNNING or PAUSED)
7. `pause()` MUST only be valid when state is RUNNING
8. `pause()` on non-RUNNING node MUST be a no-op (no errors)
9. `executionState` MUST transition to PAUSED on pause
10. `resume()` MUST only be valid when state is PAUSED
11. `resume()` on non-PAUSED node MUST be a no-op (no errors)
12. `executionState` MUST transition to RUNNING on resume
