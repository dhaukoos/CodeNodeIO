# Data Model: Continuous Mode as Default

**Feature Branch**: `014-continuous-mode-default`
**Date**: 2026-02-14
**Spec**: [spec.md](./spec.md)

## Entity Changes

### NodeRuntime (New)

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`

Runtime execution wrapper that owns lifecycle control and channel wiring. This class holds all runtime state that should NOT be in the serializable CodeNode model.

```kotlin
/**
 * Runtime execution wrapper for a CodeNode.
 *
 * Owns all runtime state including:
 * - Execution state (IDLE, RUNNING, PAUSED, ERROR)
 * - Coroutine job for lifecycle control
 * - Input/output channels for continuous processing
 *
 * This separation keeps CodeNode as a pure serializable model.
 *
 * @param T The primary data type flowing through this node
 * @property codeNode The underlying CodeNode model (immutable definition)
 */
class NodeRuntime<T : Any>(
    val codeNode: CodeNode
) {
    /**
     * Current execution state of this node.
     * Mutable at runtime, not persisted with CodeNode.
     */
    var executionState: ExecutionState = ExecutionState.IDLE

    /**
     * Runtime job reference for lifecycle control.
     * Tracks the active coroutine when the node is running.
     */
    var nodeControlJob: Job? = null

    /**
     * Input channel for receiving data (sink/transformer nodes).
     */
    var inputChannel: ReceiveChannel<T>? = null

    /**
     * Output channel for emitting data (generator/transformer nodes).
     */
    var outputChannel: SendChannel<T>? = null

    /**
     * Starts the node's processing loop.
     *
     * Manages the nodeControlJob lifecycle:
     * 1. Cancels any existing job (prevents duplicate jobs)
     * 2. Sets executionState to RUNNING
     * 3. Launches new job in provided scope
     * 4. Executes processingBlock within the job
     *
     * @param scope CoroutineScope to launch the processing job in
     * @param processingBlock Custom processing logic to execute in the job loop
     */
    fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Cancel existing job if running (prevents duplicate jobs)
        nodeControlJob?.cancel()

        // Transition to RUNNING state
        executionState = ExecutionState.RUNNING

        // Launch the processing job
        nodeControlJob = scope.launch {
            try {
                processingBlock()
            } finally {
                // Close output channel when loop exits
                outputChannel?.close()
            }
        }
    }

    /**
     * Stops the node's processing loop.
     *
     * Manages graceful shutdown:
     * 1. Sets executionState to IDLE
     * 2. Cancels the nodeControlJob
     * 3. Sets nodeControlJob to null
     */
    fun stop() {
        executionState = ExecutionState.IDLE
        nodeControlJob?.cancel()
        nodeControlJob = null
    }

    /**
     * Pauses the node's processing loop.
     *
     * Sets executionState to PAUSED. The processing loop must check
     * isPaused() to honor pause requests.
     *
     * Only valid when state is RUNNING.
     */
    fun pause() {
        if (executionState != ExecutionState.RUNNING) return
        executionState = ExecutionState.PAUSED
    }

    /**
     * Resumes the node's processing loop from paused state.
     *
     * Sets executionState back to RUNNING.
     *
     * Only valid when state is PAUSED.
     */
    fun resume() {
        if (executionState != ExecutionState.PAUSED) return
        executionState = ExecutionState.RUNNING
    }

    /**
     * Checks if this node is currently executing.
     */
    fun isRunning(): Boolean = executionState == ExecutionState.RUNNING

    /**
     * Checks if this node is paused.
     */
    fun isPaused(): Boolean = executionState == ExecutionState.PAUSED

    /**
     * Checks if this node is idle (not processing).
     */
    fun isIdle(): Boolean = executionState == ExecutionState.IDLE
}
```

### CodeNode (Modified - Remove Runtime Concerns)

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`

**Removed** (moved to NodeRuntime):
- `@Transient var nodeControlJob: Job?`
- `fun start(scope, processingBlock)`
- `fun stop()`
- `fun pause()`
- `fun resume()`

**Retained** (model properties):
- `executionState: ExecutionState` - kept as initial/default state in model
- `isRunning()`, `isPaused()`, `isIdle()`, `isError()` - convenience checks on model state
- `withExecutionState()` - for creating copies with different state

**Note**: The `executionState` property remains in CodeNode as the *persisted/initial* state, but runtime state changes happen on NodeRuntime. When a flow is loaded, CodeNode.executionState provides the initial value for NodeRuntime.executionState.

### ContinuousTypes (New Type Aliases)

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`

```kotlin
/**
 * Type alias for continuous generator processing block.
 * Receives an emit function to send values to the output channel.
 */
typealias ContinuousGeneratorBlock<T> = suspend (emit: suspend (T) -> Unit) -> Unit

/**
 * Type alias for continuous sink processing block.
 * Receives values from the input channel.
 */
typealias ContinuousSinkBlock<T> = suspend (T) -> Unit

/**
 * Type alias for continuous transformer processing block.
 * Transforms input values to output values.
 */
typealias ContinuousTransformBlock<TIn, TOut> = suspend (TIn) -> TOut

/**
 * Type alias for continuous filter predicate.
 * Returns true to pass the value through, false to drop it.
 */
typealias ContinuousFilterPredicate<T> = suspend (T) -> Boolean
```

### CodeNodeFactory (Modified)

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`

**Added Methods** (return NodeRuntime instead of CodeNode):

```kotlin
/**
 * Creates a continuous generator node that emits values in a loop.
 *
 * @param T Type of values emitted
 * @param name Human-readable name
 * @param channelCapacity Buffer capacity for output channel (default: BUFFERED)
 * @param position Canvas position
 * @param description Optional documentation
 * @param generate Processing block that receives an emit function
 * @return NodeRuntime configured for continuous generation
 */
inline fun <reified T : Any> createContinuousGenerator(
    name: String,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline generate: ContinuousGeneratorBlock<T>
): NodeRuntime<T>

/**
 * Creates a continuous sink node that consumes values in a loop.
 *
 * @param T Type of values consumed
 * @param name Human-readable name
 * @param position Canvas position
 * @param description Optional documentation
 * @param consume Processing block for each received value
 * @return NodeRuntime configured for continuous consumption
 */
inline fun <reified T : Any> createContinuousSink(
    name: String,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline consume: ContinuousSinkBlock<T>
): NodeRuntime<T>

/**
 * Creates a continuous transformer node that processes values in a loop.
 *
 * @param TIn Type of input values
 * @param TOut Type of output values
 * @param name Human-readable name
 * @param channelCapacity Buffer capacity for output channel (default: BUFFERED)
 * @param position Canvas position
 * @param description Optional documentation
 * @param transform Transformation function
 * @return NodeRuntime configured for continuous transformation
 */
inline fun <reified TIn : Any, reified TOut : Any> createContinuousTransformer(
    name: String,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline transform: ContinuousTransformBlock<TIn, TOut>
): NodeRuntime<TOut>

/**
 * Creates a continuous filter node that selectively passes values.
 *
 * @param T Type of values
 * @param name Human-readable name
 * @param channelCapacity Buffer capacity for output channel (default: BUFFERED)
 * @param position Canvas position
 * @param description Optional documentation
 * @param predicate Filter predicate returning true to pass value
 * @return NodeRuntime configured for continuous filtering
 */
inline fun <reified T : Any> createContinuousFilter(
    name: String,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline predicate: ContinuousFilterPredicate<T>
): NodeRuntime<T>
```

## Relationship Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     CodeNodeFactory                          │
│─────────────────────────────────────────────────────────────│
│  createContinuousGenerator<T>() → NodeRuntime<T>            │
│  createContinuousSink<T>() → NodeRuntime<T>                 │
│  createContinuousTransformer<TIn,TOut>() → NodeRuntime<TOut>│
│  createContinuousFilter<T>() → NodeRuntime<T>               │
│─────────────────────────────────────────────────────────────│
│  @Deprecated createGenerator<T>() → CodeNode                │
│  @Deprecated createSink<T>() → CodeNode                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ creates
┌─────────────────────────────────────────────────────────────┐
│                     NodeRuntime<T>                           │
│              (Runtime Execution Wrapper)                     │
│─────────────────────────────────────────────────────────────│
│  + codeNode: CodeNode           # Immutable model reference  │
│  + executionState: ExecutionState  # Mutable runtime state   │
│  + nodeControlJob: Job?         # Active coroutine job       │
│  + inputChannel: ReceiveChannel<T>?                          │
│  + outputChannel: SendChannel<T>?                            │
│─────────────────────────────────────────────────────────────│
│  + start(scope, processingBlock)                             │
│  + stop()                                                    │
│  + pause()                                                   │
│  + resume()                                                  │
│  + isRunning() / isPaused() / isIdle()                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ references (immutable)
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       CodeNode                               │
│                (Serializable Model/Schema)                   │
│─────────────────────────────────────────────────────────────│
│  + id: String                                                │
│  + name: String                                              │
│  + codeNodeType: CodeNodeType                                │
│  + position: Position                                        │
│  + inputPorts: List<Port<*>>                                 │
│  + outputPorts: List<Port<*>>                                │
│  + configuration: Map<String, String>                        │
│  + executionState: ExecutionState  # Initial/persisted state │
│  + processingLogic: ProcessingLogic?                         │
│─────────────────────────────────────────────────────────────│
│  + process(inputs)              # Single invocation          │
│  + withExecutionState(state)    # Create copy                │
│  + validate()                                                │
└─────────────────────────────────────────────────────────────┘
```

## Separation of Concerns

| Concern | CodeNode (Model) | NodeRuntime (Execution) |
|---------|------------------|-------------------------|
| Serializable | ✅ Yes | ❌ No |
| Persisted | ✅ Yes | ❌ No |
| executionState | Initial/default value | Mutable runtime value |
| nodeControlJob | ❌ None | ✅ Owns Job |
| Channels | ❌ None | ✅ Owns channels |
| start/stop/pause/resume | ❌ None | ✅ Owns lifecycle |
| Immutable | ✅ Data class | ❌ Mutable properties |

## State Transitions

Managed by NodeRuntime:

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
```

## Migration from Feature 013

### Components Before (013 style)

```kotlin
class TimerEmitterComponent : ProcessingLogic {
    var codeNode: CodeNode? = CodeNode(...)

    var executionState: ExecutionState
        get() = codeNode?.executionState ?: ExecutionState.IDLE
        set(value) { codeNode = codeNode?.withExecutionState(value) }

    suspend fun start(scope: CoroutineScope) {
        val node = codeNode ?: return
        node.start(scope) { ... }  // Lifecycle on CodeNode
    }

    fun stop() {
        codeNode?.stop()  // Lifecycle on CodeNode
    }
}
```

### Components After (014 style)

```kotlin
class TimerEmitterComponent : ProcessingLogic {
    var nodeRuntime: NodeRuntime<TimerOutput>? = null

    val codeNode: CodeNode?
        get() = nodeRuntime?.codeNode

    var executionState: ExecutionState
        get() = nodeRuntime?.executionState ?: ExecutionState.IDLE
        set(value) { nodeRuntime?.executionState = value }

    suspend fun start(scope: CoroutineScope) {
        val runtime = nodeRuntime ?: return
        runtime.start(scope) { ... }  // Lifecycle on NodeRuntime
    }

    fun stop() {
        nodeRuntime?.stop()  // Lifecycle on NodeRuntime
    }
}
```

## Validation Rules

1. `NodeRuntime.start()` MUST set executionState to RUNNING before launching job
2. `NodeRuntime.stop()` MUST set executionState to IDLE and cancel job
3. `NodeRuntime.pause()` MUST only transition from RUNNING to PAUSED
4. `NodeRuntime.resume()` MUST only transition from PAUSED to RUNNING
5. `outputChannel` MUST be closed in finally block when loop exits
6. `inputChannel` closure MUST cause graceful loop exit
7. CodeNode MUST NOT have any `@Transient` properties after refactoring
8. CodeNode MUST NOT have lifecycle methods after refactoring
9. Factory continuous methods MUST return NodeRuntime, not CodeNode
