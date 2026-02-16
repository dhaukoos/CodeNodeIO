# Data Model: Typed NodeRuntime Stubs

**Feature**: 015-typed-node-runtime
**Date**: 2026-02-15
**Status**: Complete

## Overview

This feature extends the existing runtime model to support nodes with 0-3 inputs and 0-3 outputs. The key additions are:

1. **ProcessResult types** for multi-output return values
2. **Multi-port runtime classes** for configurations beyond 1-in-1-out
3. **Type aliases** for process function signatures

## Entities

### ProcessResult2<U, V>

Multi-output result container for 2 outputs.

```kotlin
data class ProcessResult2<U, V>(
    val out1: U?,  // First output, nullable for selective sending
    val out2: V?   // Second output, nullable for selective sending
) {
    companion object {
        fun <U, V> of(out1: U?, out2: V?) = ProcessResult2(out1, out2)
        fun <U, V> first(value: U) = ProcessResult2<U, V>(value, null)
        fun <U, V> second(value: V) = ProcessResult2<U, V>(null, value)
        fun <U, V> both(first: U, second: V) = ProcessResult2(first, second)
    }
}
```

**Usage**:
```kotlin
val result = ProcessResult2(42, "hello")
val (num, str) = result  // Destructuring
result.out1?.let { sendToChannel1(it) }
result.out2?.let { sendToChannel2(it) }
```

---

### ProcessResult3<U, V, W>

Multi-output result container for 3 outputs.

```kotlin
data class ProcessResult3<U, V, W>(
    val out1: U?,  // First output
    val out2: V?,  // Second output
    val out3: W?   // Third output
) {
    companion object {
        fun <U, V, W> of(out1: U?, out2: V?, out3: W?) = ProcessResult3(out1, out2, out3)
        fun <U, V, W> all(first: U, second: V, third: W) = ProcessResult3(first, second, third)
    }
}
```

---

### In2Out1Runtime<A, B, R>

Runtime for nodes with 2 inputs and 1 output.

```kotlin
class In2Out1Runtime<A : Any, B : Any, R : Any>(
    codeNode: CodeNode,
    private val process: suspend (A, B) -> R
) : NodeRuntime<A>(codeNode) {

    // Second input channel (first is inherited from NodeRuntime)
    var inputChannel2: ReceiveChannel<B>? = null

    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit)
}
```

**Channel Configuration**:
- `inputChannel: ReceiveChannel<A>` - First input (inherited)
- `inputChannel2: ReceiveChannel<B>` - Second input
- `outputChannel: SendChannel<R>` - Output (inherited)

---

### In3Out1Runtime<A, B, C, R>

Runtime for nodes with 3 inputs and 1 output.

```kotlin
class In3Out1Runtime<A : Any, B : Any, C : Any, R : Any>(
    codeNode: CodeNode,
    private val process: suspend (A, B, C) -> R
) : NodeRuntime<A>(codeNode) {

    var inputChannel2: ReceiveChannel<B>? = null
    var inputChannel3: ReceiveChannel<C>? = null
}
```

---

### In1Out2Runtime<A, U, V>

Runtime for nodes with 1 input and 2 outputs.

```kotlin
class In1Out2Runtime<A : Any, U : Any, V : Any>(
    codeNode: CodeNode,
    private val process: suspend (A) -> ProcessResult2<U, V>
) : NodeRuntime<A>(codeNode) {

    // Output channels (2 separate typed channels)
    var outputChannel1: SendChannel<U>? = null
    var outputChannel2: SendChannel<V>? = null
}
```

---

### In2Out2Runtime<A, B, U, V>

Runtime for nodes with 2 inputs and 2 outputs.

```kotlin
class In2Out2Runtime<A : Any, B : Any, U : Any, V : Any>(
    codeNode: CodeNode,
    private val process: suspend (A, B) -> ProcessResult2<U, V>
) : NodeRuntime<A>(codeNode) {

    var inputChannel2: ReceiveChannel<B>? = null
    var outputChannel1: SendChannel<U>? = null
    var outputChannel2: SendChannel<V>? = null
}
```

---

### In2SinkRuntime<A, B>

Runtime for sink nodes with 2 inputs.

```kotlin
class In2SinkRuntime<A : Any, B : Any>(
    codeNode: CodeNode,
    private val consume: suspend (A, B) -> Unit
) : NodeRuntime<A>(codeNode) {

    var inputChannel2: ReceiveChannel<B>? = null
}
```

---

### Out2GeneratorRuntime<U, V>

Runtime for generator nodes with 2 outputs.

```kotlin
class Out2GeneratorRuntime<U : Any, V : Any>(
    codeNode: CodeNode,
    channelCapacity: Int = Channel.BUFFERED,
    private val generate: Out2GeneratorBlock<U, V>
) : NodeRuntime<Unit>(codeNode) {

    var outputChannel1: SendChannel<U>? = null
    var outputChannel2: SendChannel<V>? = null
}
```

---

## Type Aliases

New type aliases in ContinuousTypes.kt:

```kotlin
// Multi-input, single output
typealias In2Out1ProcessBlock<A, B, R> = suspend (A, B) -> R
typealias In3Out1ProcessBlock<A, B, C, R> = suspend (A, B, C) -> R

// Single input, multi-output
typealias In1Out2ProcessBlock<A, U, V> = suspend (A) -> ProcessResult2<U, V>
typealias In1Out3ProcessBlock<A, U, V, W> = suspend (A) -> ProcessResult3<U, V, W>

// Multi-input, multi-output
typealias In2Out2ProcessBlock<A, B, U, V> = suspend (A, B) -> ProcessResult2<U, V>
typealias In2Out3ProcessBlock<A, B, U, V, W> = suspend (A, B) -> ProcessResult3<U, V, W>
typealias In3Out2ProcessBlock<A, B, C, U, V> = suspend (A, B, C) -> ProcessResult2<U, V>
typealias In3Out3ProcessBlock<A, B, C, U, V, W> = suspend (A, B, C) -> ProcessResult3<U, V, W>

// Multi-input sink
typealias In2SinkBlock<A, B> = suspend (A, B) -> Unit
typealias In3SinkBlock<A, B, C> = suspend (A, B, C) -> Unit

// Multi-output generator
typealias Out2GeneratorBlock<U, V> = suspend (emit: suspend (ProcessResult2<U, V>) -> Unit) -> Unit
typealias Out3GeneratorBlock<U, V, W> = suspend (emit: suspend (ProcessResult3<U, V, W>) -> Unit) -> Unit
```

---

## Factory Methods

All factory methods in CodeNodeFactory:

### Generator Factory Methods (0 inputs)

```kotlin
// Existing (alias: createOut1Generator)
fun <T> createContinuousGenerator(...): GeneratorRuntime<T>

// New
fun <U, V> createOut2Generator(...): Out2GeneratorRuntime<U, V>
fun <U, V, W> createOut3Generator(...): Out3GeneratorRuntime<U, V, W>
```

### Sink Factory Methods (0 outputs)

```kotlin
// Existing (alias: createIn1Sink)
fun <T> createContinuousSink(...): SinkRuntime<T>

// New
fun <A, B> createIn2Sink(...): In2SinkRuntime<A, B>
fun <A, B, C> createIn3Sink(...): In3SinkRuntime<A, B, C>
```

### Processor Factory Methods (N inputs, M outputs)

```kotlin
// Existing (alias: createIn1Out1Processor)
fun <TIn, TOut> createContinuousTransformer(...): TransformerRuntime<TIn, TOut>

// New single-output
fun <A, B, R> createIn2Out1Processor(...): In2Out1Runtime<A, B, R>
fun <A, B, C, R> createIn3Out1Processor(...): In3Out1Runtime<A, B, C, R>

// New multi-output
fun <A, U, V> createIn1Out2Processor(...): In1Out2Runtime<A, U, V>
fun <A, U, V, W> createIn1Out3Processor(...): In1Out3Runtime<A, U, V, W>
fun <A, B, U, V> createIn2Out2Processor(...): In2Out2Runtime<A, B, U, V>
fun <A, B, U, V, W> createIn2Out3Processor(...): In2Out3Runtime<A, B, U, V, W>
fun <A, B, C, U, V> createIn3Out2Processor(...): In3Out2Runtime<A, B, C, U, V>
fun <A, B, C, U, V, W> createIn3Out3Processor(...): In3Out3Runtime<A, B, C, U, V, W>
```

---

## Relationships

```
┌─────────────┐
│  CodeNode   │  (immutable model, no runtime state)
└──────┬──────┘
       │ wraps
       ▼
┌─────────────────────────────────────────┐
│           NodeRuntime<T>                │
│  - executionState: ExecutionState       │
│  - nodeControlJob: Job?                 │
│  - inputChannel: ReceiveChannel<T>?     │
│  - outputChannel: SendChannel<T>?       │
│  + start(), stop(), pause(), resume()   │
└─────────────────────────────────────────┘
       │
       │ extends
       ├──────────────────┬──────────────────┬──────────────────┐
       ▼                  ▼                  ▼                  ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│ Generator   │   │ Sink        │   │ Transformer │   │ Multi-Port  │
│ Runtime<T>  │   │ Runtime<T>  │   │ Runtime     │   │ Runtimes    │
│             │   │             │   │ <TIn,TOut>  │   │ (new)       │
└─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘
```

---

## Invariants

1. **Valid Configuration**: Nodes must have at least 1 input OR 1 output (0×0 is invalid)
2. **Channel Requirement**: All channels must be wired before start()
3. **State Transitions**: IDLE → RUNNING → (PAUSED ↔ RUNNING) → IDLE
4. **Synchronous Receive**: Multi-input nodes block on each input in order
5. **Graceful Shutdown**: Channel closure triggers orderly shutdown of processing loop
6. **Type Safety**: All channel types must match at compile time
