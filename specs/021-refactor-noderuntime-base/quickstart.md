# Quickstart: Refactor Base NodeRuntime Class

**Feature**: 021-refactor-noderuntime-base
**Date**: 2026-02-19

## Overview

This refactoring simplifies the base `NodeRuntime` class by removing its generic type parameter and channel properties. The base class becomes a pure lifecycle manager, while subclasses own their typed channel properties.

## Before & After

### Base Class

**Before**:
```kotlin
open class NodeRuntime<T : Any>(
    val codeNode: CodeNode,
    var registry: RuntimeRegistry? = null
) {
    var executionState: ExecutionState = ExecutionState.IDLE
    var nodeControlJob: Job? = null
    var inputChannel: ReceiveChannel<T>? = null   // REMOVED
    var outputChannel: SendChannel<T>? = null      // REMOVED

    open fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        nodeControlJob?.cancel()
        registry?.register(this)
        executionState = ExecutionState.RUNNING
        nodeControlJob = scope.launch {
            try { processingBlock() }
            finally { outputChannel?.close() }     // REMOVED
        }
    }
    // stop(), pause(), resume() unchanged
}
```

**After**:
```kotlin
open class NodeRuntime(
    val codeNode: CodeNode,
    var registry: RuntimeRegistry? = null
) {
    var executionState: ExecutionState = ExecutionState.IDLE
    var nodeControlJob: Job? = null
    // No channel properties - subclasses own their channels

    open fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        nodeControlJob?.cancel()
        registry?.register(this)
        executionState = ExecutionState.RUNNING
        nodeControlJob = scope.launch {
            processingBlock()
            // No finally block needed - subclasses manage their own channels
        }
    }
    // stop(), pause(), resume() unchanged
}
```

### Subclass Example: SinkRuntime (single-input, keeps `inputChannel`)

**Before**:
```kotlin
class SinkRuntime<T : Any>(
    codeNode: CodeNode,
    private val consume: ContinuousSinkBlock<T>
) : NodeRuntime<T>(codeNode) {

    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        // Uses inherited inputChannel from base class
        val channel = inputChannel ?: return
        // ...
    }
}

// Test usage:
sink.inputChannel = channel  // Accesses base class property
```

**After**:
```kotlin
class SinkRuntime<T : Any>(
    codeNode: CodeNode,
    private val consume: ContinuousSinkBlock<T>
) : NodeRuntime(codeNode) {  // No generic on NodeRuntime

    var inputChannel: ReceiveChannel<T>? = null  // Own property, same name (single-input)

    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        val channel = inputChannel ?: return
        // ...
    }
}

// Test usage:
sink.inputChannel = channel  // Same property name, now owned by subclass
```

### Subclass Example: TransformerRuntime (single-input, output rename)

**Before**:
```kotlin
class TransformerRuntime<TIn : Any, TOut : Any>(
    codeNode: CodeNode,
    private val transform: ContinuousTransformBlock<TIn, TOut>
) : NodeRuntime<TIn>(codeNode) {
    // inputChannel inherited from NodeRuntime<TIn>
    var transformerOutputChannel: SendChannel<TOut>? = null  // Prefixed name
}
```

**After**:
```kotlin
class TransformerRuntime<TIn : Any, TOut : Any>(
    codeNode: CodeNode,
    private val transform: ContinuousTransformBlock<TIn, TOut>
) : NodeRuntime(codeNode) {  // No generic
    var inputChannel: ReceiveChannel<TIn>? = null   // Own property, same name (single-input)
    var outputChannel: SendChannel<TOut>? = null     // Renamed from transformerOutputChannel
}
```

### Multi-Input Example: In2SinkRuntime (multi-input, uses `inputChannel1`)

**Before**:
```kotlin
class In2SinkRuntime<A : Any, B : Any>(
    codeNode: CodeNode,
    private val consume: In2SinkBlock<A, B>
) : NodeRuntime<A>(codeNode) {
    // inputChannel inherited from NodeRuntime<A> (type A)
    var inputChannel2: ReceiveChannel<B>? = null
}
```

**After**:
```kotlin
class In2SinkRuntime<A : Any, B : Any>(
    codeNode: CodeNode,
    private val consume: In2SinkBlock<A, B>
) : NodeRuntime(codeNode) {  // No generic
    var inputChannel1: ReceiveChannel<A>? = null  // Explicit, numbered (multi-input)
    var inputChannel2: ReceiveChannel<B>? = null
}
```

### Single-Output Processor Example: In2Out1Runtime (output rename)

**Before**:
```kotlin
class In2Out1Runtime<A : Any, B : Any, R : Any>(
    codeNode: CodeNode,
    private val process: In2Out1ProcessBlock<A, B, R>
) : NodeRuntime<A>(codeNode) {
    // inputChannel inherited from NodeRuntime<A>
    var inputChannel2: ReceiveChannel<B>? = null
    var processorOutputChannel: SendChannel<R>? = null  // Prefixed name
}
```

**After**:
```kotlin
class In2Out1Runtime<A : Any, B : Any, R : Any>(
    codeNode: CodeNode,
    private val process: In2Out1ProcessBlock<A, B, R>
) : NodeRuntime(codeNode) {  // No generic
    var inputChannel1: ReceiveChannel<A>? = null   // Numbered (multi-input)
    var inputChannel2: ReceiveChannel<B>? = null
    var outputChannel: SendChannel<R>? = null       // Renamed from processorOutputChannel
}
```

### RuntimeRegistry

**Before**:
```kotlin
private val runtimes = mutableMapOf<String, NodeRuntime<*>>()
fun register(runtime: NodeRuntime<*>) { ... }
```

**After**:
```kotlin
private val runtimes = mutableMapOf<String, NodeRuntime>()
fun register(runtime: NodeRuntime) { ... }
```

## Files Changed

| File | Change Type | Description |
| ---- | ----------- | ----------- |
| `fbpDsl/.../runtime/NodeRuntime.kt` | Modify | Remove `<T: Any>`, `inputChannel`, `outputChannel`, `finally` block |
| `fbpDsl/.../runtime/GeneratorRuntime.kt` | Modify | Inherit `NodeRuntime`, define own `outputChannel` |
| `fbpDsl/.../runtime/SinkRuntime.kt` | Modify | Inherit `NodeRuntime`, add own `inputChannel` (same name) |
| `fbpDsl/.../runtime/TransformerRuntime.kt` | Modify | Inherit `NodeRuntime`, add own `inputChannel`, rename `transformerOutputChannel` → `outputChannel` |
| `fbpDsl/.../runtime/FilterRuntime.kt` | Modify | Inherit `NodeRuntime`, add own `inputChannel` + `outputChannel` |
| `fbpDsl/.../runtime/Out2GeneratorRuntime.kt` | Modify | Inherit `NodeRuntime` (channels already own) |
| `fbpDsl/.../runtime/Out3GeneratorRuntime.kt` | Modify | Inherit `NodeRuntime` (channels already own) |
| `fbpDsl/.../runtime/In2SinkRuntime.kt`, `In3SinkRuntime.kt` | Modify | Inherit `NodeRuntime`, rename `inputChannel` → `inputChannel1` (multi-input) |
| `fbpDsl/.../runtime/In1Out2Runtime.kt`, `In1Out3Runtime.kt` | Modify | Inherit `NodeRuntime`, add own `inputChannel` (single-input) |
| `fbpDsl/.../runtime/In2Out1Runtime.kt`, `In3Out1Runtime.kt` | Modify | Inherit `NodeRuntime`, rename `inputChannel` → `inputChannel1`, rename `processorOutputChannel` → `outputChannel` |
| `fbpDsl/.../runtime/In2Out2Runtime.kt`, `In2Out3Runtime.kt`, `In3Out2Runtime.kt`, `In3Out3Runtime.kt` | Modify | Inherit `NodeRuntime`, rename `inputChannel` → `inputChannel1` (multi-input) |
| `fbpDsl/.../runtime/RuntimeRegistry.kt` | Modify | `NodeRuntime<*>` → `NodeRuntime` |
| Test files (7 files) | Modify | Update multi-input `inputChannel` → `inputChannel1`, `transformerOutputChannel` → `outputChannel`, `processorOutputChannel` → `outputChannel`, `NodeRuntime<*>` → `NodeRuntime` |
| `StopWatch/.../DisplayReceiverComponent.kt` | Modify | Update internal delegation to `sinkRuntime.inputChannel1` (component keeps external `inputChannel` name) |
| `StopWatch/.../StopWatchFlow.kt` | Verify | No changes needed (uses component's external `inputChannel` property) |
| `StopWatch/.../ChannelIntegrationTest.kt` | Verify | No changes needed (uses component's external `inputChannel` property) |
