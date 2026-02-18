# Research: Unified FlowGraph Execution Control

**Feature**: 019-flowgraph-execution-control
**Date**: 2026-02-17

## Research Questions

### 1. RuntimeRegistry Design Pattern

**Decision**: Use a simple registry with thread-safe ConcurrentHashMap and flow-scoped lifecycle

**Rationale**:
- RuntimeRegistry needs to track active NodeRuntime instances for a specific flow
- Thread safety required since runtimes start/stop from coroutines
- Flow-scoped ensures cleanup when flow stops
- Simple interface: register(runtime), unregister(runtime), pauseAll(), resumeAll()

**Alternatives Considered**:
- Global singleton registry - Rejected: would mix runtimes from different flows
- WeakReference-based registry - Rejected: complicates lifecycle, unpredictable cleanup
- Hierarchical registry matching GraphNode structure - Rejected: over-engineering for current needs

**Code Pattern**:
```kotlin
class RuntimeRegistry {
    private val runtimes = ConcurrentHashMap<String, NodeRuntime<*>>()

    fun register(runtime: NodeRuntime<*>) {
        runtimes[runtime.codeNode.id] = runtime
    }

    fun unregister(runtime: NodeRuntime<*>) {
        runtimes.remove(runtime.codeNode.id)
    }

    fun pauseAll() {
        runtimes.values.forEach { it.pause() }
    }

    fun resumeAll() {
        runtimes.values.forEach { it.resume() }
    }

    fun stopAll() {
        runtimes.values.forEach { it.stop() }
        runtimes.clear()
    }
}
```

### 2. Pause Hook Implementation Pattern

**Decision**: Use a consistent pause check pattern with delay loop in all runtime processing blocks

**Rationale**:
- TransformerRuntime already implements this pattern successfully
- 10ms delay provides responsive resume while minimizing CPU usage
- Check for IDLE after pause loop to handle stop-while-paused scenario
- Pattern must be applied consistently across all runtime classes

**Code Pattern**:
```kotlin
// Standard pause hook - insert at start of processing loop iteration
while (executionState == ExecutionState.PAUSED) {
    delay(10)
}
if (executionState == ExecutionState.IDLE) break  // Exit if stopped during pause
```

**Affected Classes**:
- GeneratorRuntime (currently missing pause hook)
- SinkRuntime (currently missing pause hook)
- Out2GeneratorRuntime (currently missing pause hook)
- In2SinkRuntime (currently missing pause hook)
- In2Out1Runtime (already has pause hook - verify)
- In3SinkRuntime, Out2GeneratorRuntime, etc. (all multi-I/O runtimes)

### 3. RootControlNode Integration with RuntimeRegistry

**Decision**: Pass RuntimeRegistry to RootControlNode; pauseAll()/resumeAll() update both model state AND call registry methods

**Rationale**:
- RootControlNode already manages model-level state propagation
- Adding registry integration creates unified control point
- Both model state and runtime state updated in single operation
- Maintains immutable FlowGraph pattern (returns new FlowGraph)

**Code Pattern**:
```kotlin
class RootControlNode private constructor(
    private val flowGraph: FlowGraph,
    private val name: String,
    private val registry: RuntimeRegistry? = null  // Optional for backward compatibility
) {
    fun pauseAll(): FlowGraph {
        // Update model state
        val updatedGraph = flowGraph.withRootNodes(
            flowGraph.rootNodes.map { it.withExecutionState(ExecutionState.PAUSED, propagate = true) }
        )
        // Update runtime state
        registry?.pauseAll()
        return updatedGraph
    }

    fun resumeAll(): FlowGraph {
        val updatedGraph = flowGraph.withRootNodes(
            flowGraph.rootNodes.map { it.withExecutionState(ExecutionState.RUNNING, propagate = true) }
        )
        registry?.resumeAll()
        return updatedGraph
    }
}
```

### 4. Channel Restart on Stop/Start Cycle

**Decision**: Recreate channels in start() method, not just in init

**Rationale**:
- Current issue: channels created in init, closed in stop(), not recreated on restart
- Fix: Check if channels are closed and recreate them at start of start()
- Ensures fresh channels for each start cycle

**Code Pattern**:
```kotlin
override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
    nodeControlJob?.cancel()

    // Recreate channels if closed or null
    if (outputChannel1?.isClosedForSend != false) {
        outputChannel1 = Channel(channelCapacity)
    }
    if (outputChannel2?.isClosedForSend != false) {
        outputChannel2 = Channel(channelCapacity)
    }

    executionState = ExecutionState.RUNNING
    // ... rest of start logic
}
```

### 5. UI Button State Management

**Decision**: Derive button visibility from executionState in ViewModel

**Rationale**:
- executionState already has IDLE, RUNNING, PAUSED values
- Button visibility is pure function of state - no additional tracking needed
- Consistent with existing Start/Stop button pattern

**UI Logic**:
```kotlin
// In StopWatch.kt composable
val executionState by viewModel.executionState.collectAsState()

// Button visibility
val showStart = executionState == ExecutionState.IDLE
val showPause = executionState == ExecutionState.RUNNING
val showResume = executionState == ExecutionState.PAUSED
val enableReset = executionState != ExecutionState.RUNNING
```

## Technical Decisions Summary

| Decision | Choice | Confidence |
|----------|--------|------------|
| Registry Pattern | Flow-scoped ConcurrentHashMap | High |
| Pause Hook | 10ms delay loop with IDLE check | High |
| RootControlNode Integration | Pass registry, dual state update | High |
| Channel Restart | Recreate in start() | High |
| Button State | Derive from executionState | High |

## Runtime Classes Requiring Pause Hooks

| Class | Current Status | Action |
|-------|----------------|--------|
| NodeRuntime | Has pause()/resume() | No change needed |
| GeneratorRuntime | NO pause hook | Add pause check in emit loop |
| SinkRuntime | NO pause hook | Add pause check in receive loop |
| TransformerRuntime | HAS pause hook | Verify, no change needed |
| FilterRuntime | NEEDS CHECK | Verify, add if missing |
| Out2GeneratorRuntime | NO pause hook | Add pause check in emit loop |
| Out3GeneratorRuntime | NO pause hook | Add pause check in emit loop |
| In2SinkRuntime | NO pause hook | Add pause check in receive loop |
| In3SinkRuntime | NO pause hook | Add pause check in receive loop |
| In2Out1Runtime | HAS pause hook | Verify, no change needed |
| In3Out1Runtime | HAS pause hook | Verify, no change needed |
| In1Out2Runtime | NEEDS CHECK | Verify, add if missing |
| In1Out3Runtime | NEEDS CHECK | Verify, add if missing |
| In2Out2Runtime | NEEDS CHECK | Verify, add if missing |
| In2Out3Runtime | NEEDS CHECK | Verify, add if missing |
| In3Out2Runtime | NEEDS CHECK | Verify, add if missing |
| In3Out3Runtime | NEEDS CHECK | Verify, add if missing |

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Race condition in registry | Low | Medium | Use ConcurrentHashMap; test concurrent register/unregister |
| Pause not honored in mid-operation | Low | Low | Document: current operation completes before pause |
| Channel deadlock on restart | Medium | High | Recreate channels in start(); test stop/start cycles |
| UI lag on pause/resume | Low | Medium | 10ms delay is responsive; test on mobile devices |
