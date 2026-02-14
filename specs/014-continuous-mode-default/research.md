# Research: Continuous Mode as Default

**Feature**: 014-continuous-mode-default
**Date**: 2026-02-13
**Status**: Complete

## Research Questions

### RQ1: Should continuous mode completely replace single-invocation, or coexist?

**Decision**: Coexist with gradual deprecation path

**Rationale**:
- Existing code uses single-invocation ProcessingLogic interface
- TypedUseCases.kt provides base classes (TransformerUseCase, etc.) that use single invocation
- Breaking existing API would violate backward compatibility requirement
- Continuous mode is more powerful but not always needed (simple transforms)

**Alternatives Considered**:
1. **Replace completely**: Would break existing code, requires migration
2. **Coexist indefinitely**: Confusing API surface, maintenance burden
3. **Coexist with deprecation** ✅: Gradual migration, clear path forward

**Implementation**:
- Add `createContinuousGenerator`, `createContinuousSink`, `createContinuousTransformer`
- Mark existing `createGenerator`, `createSink` as `@Deprecated` with migration message
- Document migration path in quickstart.md

---

### RQ2: What should the default channel buffer size be?

**Decision**: `Channel.BUFFERED` (64 elements)

**Rationale**:
- Kotlin default for buffered channels is 64 elements
- Provides reasonable throughput without excessive memory
- Prevents producer/consumer coupling (some buffering allows async)
- Can be overridden per-channel via configuration

**Alternatives Considered**:
1. **Channel.RENDEZVOUS (0)**: Too tight coupling, producer blocks immediately
2. **Channel.UNLIMITED**: Memory exhaustion risk under load
3. **Channel.BUFFERED (64)** ✅: Balanced default, matches Kotlin convention
4. **Channel.CONFLATED (1)**: Only keeps latest, loses intermediate values

**Configuration**:
```kotlin
// Factory method with configurable buffer
createContinuousGenerator<T>(
    name = "MyGenerator",
    channelCapacity = Channel.BUFFERED  // default, can override
) { emit -> ... }
```

---

### RQ3: How should errors in continuous loops be handled?

**Decision**: Fail-fast with structured error propagation

**Rationale**:
- FBP principle: errors should be visible, not swallowed
- Coroutine cancellation propagates through job hierarchy
- Silent failures lead to hard-to-debug issues
- Recovery strategies can be added via decorators/wrappers

**Error Handling Strategy**:

1. **Processing errors**: Exception in user code → log, propagate via channel exception handler
2. **Channel errors**: ClosedSendChannelException → graceful loop exit
3. **Cancellation**: CancellationException → normal shutdown, don't log as error
4. **System errors**: OutOfMemoryError, etc. → propagate immediately

**Implementation**:
```kotlin
node.start(scope) {
    try {
        // Processing loop
        while (isActive) {
            try {
                val item = inputChannel.receive()
                val result = process(item)
                outputChannel.send(result)
            } catch (e: ClosedReceiveChannelException) {
                break  // Graceful shutdown
            } catch (e: CancellationException) {
                throw e  // Don't catch cancellation
            } catch (e: Exception) {
                // Log and optionally propagate
                errorHandler?.invoke(e) ?: throw e
            }
        }
    } finally {
        outputChannel.close()  // Propagate closure
    }
}
```

**Future Enhancement**: Add retry decorator, circuit breaker pattern as optional wrappers.

---

### RQ4: Should channels be part of CodeNode data class or separate wiring?

**Decision**: Separate `NodeRuntime` class (renamed from NodeWiring)

**Rationale**:
- CodeNode is a `@Serializable` data class
- Channels (`SendChannel`, `ReceiveChannel`) are not serializable
- Runtime wiring should not pollute persistent model
- Separation of concerns: model vs runtime
- "Runtime" better describes the class purpose than "Wiring"

**Architecture**:

```
┌─────────────────────────────────────────────────────────┐
│                    FlowGraph (Model)                     │
│  ┌───────────┐    ┌───────────┐    ┌───────────┐        │
│  │ CodeNode  │───▶│ Connection│───▶│ CodeNode  │        │
│  │ (source)  │    │ (edge)    │    │ (target)  │        │
│  └───────────┘    └───────────┘    └───────────┘        │
└─────────────────────────────────────────────────────────┘
                         │
                         ▼ Runtime instantiation
┌─────────────────────────────────────────────────────────┐
│                  FlowRuntime (Execution)                 │
│  ┌─────────────┐    ┌───────────┐    ┌─────────────┐    │
│  │ NodeRuntime │───▶│  Channel  │───▶│ NodeRuntime │    │
│  │  (source)   │    │ (runtime) │    │  (target)   │    │
│  └─────────────┘    └───────────┘    └─────────────┘    │
│       │                                     │            │
│       ▼                                     ▼            │
│  runtime.start(scope)               runtime.start(scope) │
└─────────────────────────────────────────────────────────┘
```

**NodeRuntime Structure**:
```kotlin
class NodeRuntime<T : Any>(
    val codeNode: CodeNode,
    var executionState: ExecutionState = ExecutionState.IDLE,
    var nodeControlJob: Job? = null,
    var inputChannel: ReceiveChannel<T>? = null,
    var outputChannel: SendChannel<T>? = null
) {
    fun start(scope, processingBlock) { ... }
    fun stop() { ... }
    fun pause() { ... }
    fun resume() { ... }
}
```

---

### RQ5: Should lifecycle methods remain on CodeNode or move to NodeRuntime?

**Decision**: Move lifecycle methods from CodeNode to NodeRuntime

**Rationale**:
- Feature 013 added `nodeControlJob`, `start()`, `stop()`, `pause()`, `resume()` to CodeNode
- These are runtime concerns that don't belong on a serializable data class
- CodeNode required `@Transient var nodeControlJob` which is awkward for a data class
- NodeRuntime already exists to hold runtime state - lifecycle belongs there
- Clean separation: CodeNode = "what the node IS", NodeRuntime = "how the node RUNS"

**Before (Feature 013)**:
```kotlin
// CodeNode has mixed concerns
@Serializable
data class CodeNode(...) {
    @Transient var nodeControlJob: Job? = null  // Awkward!

    fun start(scope, block) { ... }  // Runtime method on data class
    fun stop() { ... }
    fun pause() { ... }
    fun resume() { ... }
}
```

**After (Feature 014)**:
```kotlin
// CodeNode is pure model
@Serializable
data class CodeNode(...) {
    // No @Transient properties
    // No lifecycle methods
    // Just model data
}

// NodeRuntime owns all runtime concerns
class NodeRuntime<T>(val codeNode: CodeNode) {
    var executionState: ExecutionState = ExecutionState.IDLE
    var nodeControlJob: Job? = null
    var inputChannel: ReceiveChannel<T>? = null
    var outputChannel: SendChannel<T>? = null

    fun start(scope, block) { ... }
    fun stop() { ... }
    fun pause() { ... }
    fun resume() { ... }
}
```

**Migration Impact**:
- CodeNodeLifecycleTest.kt → NodeRuntimeTest.kt (test the same behaviors, different class)
- TimerEmitterComponent: `codeNode.start()` → `nodeRuntime.start()`
- DisplayReceiverComponent: `codeNode.stop()` → `nodeRuntime.stop()`

---

## Existing Patterns Analysis

### Current CodeNodeFactory Methods

| Method | Execution Model | Input | Output |
|--------|-----------------|-------|--------|
| `createGenerator<T>` | Single invocation | None | Map<String, IP> |
| `createSink<T>` | Single invocation | Map<String, IP> | None |
| `createTransformer<TIn, TOut>` | Single invocation | Map<String, IP> | Map<String, IP> |
| `createFilter<T>` | Single invocation | Map<String, IP> | Map<String, IP> or empty |
| `createValidator<T>` | Single invocation | Map<String, IP> | Route to valid/invalid |
| `createSplitter<T>` | Single invocation | Map<String, IP> | Route to multiple outputs |
| `createMerger<T>` | Single invocation | Map<String, IP> | Single output |

### Proposed Continuous Factory Methods

| Method | Execution Model | Input | Output |
|--------|-----------------|-------|--------|
| `createContinuousGenerator<T>` | Loop until stopped | None | SendChannel<T> |
| `createContinuousSink<T>` | Loop over channel | ReceiveChannel<T> | None |
| `createContinuousTransformer<TIn, TOut>` | Loop: receive → transform → send | ReceiveChannel<TIn> | SendChannel<TOut> |
| `createContinuousFilter<T>` | Loop: receive → predicate → maybe send | ReceiveChannel<T> | SendChannel<T> |

---

## Performance Considerations

### Channel Overhead Benchmark (Target)

| Operation | Target Latency | Notes |
|-----------|----------------|-------|
| Channel.send() | < 100ns | Unbuffered hot path |
| Channel.receive() | < 100ns | Unbuffered hot path |
| Buffered send | < 500ns | With 64-element buffer |
| Backpressure (full) | ~1ms | Suspend until space available |

### Memory Footprint

| Component | Memory | Notes |
|-----------|--------|-------|
| Channel (buffered) | ~8KB | 64 elements × ~128 bytes average |
| NodeWiring | ~200 bytes | References only |
| Running coroutine | ~2KB | Stack + locals |

---

## Dependencies Review

### From Feature 013 (Node Control Extraction) - TO BE RELOCATED

Feature 013 added these to CodeNode, but feature 014 moves them to NodeRuntime:

- `CodeNode.nodeControlJob: Job?` → `NodeRuntime.nodeControlJob`
- `CodeNode.start(scope, processingBlock)` → `NodeRuntime.start()`
- `CodeNode.stop()` → `NodeRuntime.stop()`
- `CodeNode.pause()` / `resume()` → `NodeRuntime.pause()` / `resume()`

The CodeNodeLifecycleTest.kt tests remain valid - they just test NodeRuntime instead.

### From Feature 012 (Channel Connections)

- Typed channel infrastructure already exists
- TimerEmitterComponent uses `SendChannel<TimerOutput>`
- DisplayReceiverComponent uses `ReceiveChannel<TimerOutput>`

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Breaking existing code | Additive API, deprecation warnings |
| Resource leaks | Structured concurrency, finally blocks close channels |
| Testing complexity | Virtual time support via runTest |
| Performance regression | Benchmark suite, profiling during implementation |
