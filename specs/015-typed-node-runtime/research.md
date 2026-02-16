# Research: Typed NodeRuntime Stubs

**Feature**: 015-typed-node-runtime
**Date**: 2026-02-15
**Status**: Complete

## Research Questions

### Q1: How should multi-input nodes synchronize data arrival?

**Decision**: Synchronous receive pattern (wait for all inputs)

**Rationale**: The spec explicitly states "The node waits for all required inputs before invoking the process function (synchronous receive pattern)". This is the simplest model and matches classical FBP semantics.

**Implementation**:
```kotlin
// In2Out1Runtime.start()
nodeControlJob = scope.launch {
    val ch1 = inputChannel1!!
    val ch2 = inputChannel2!!

    while (isActive) {
        val val1 = ch1.receive()  // Blocks until value
        val val2 = ch2.receive()  // Blocks until value
        val result = process(val1, val2)
        outputChannel?.send(result)
    }
}
```

**Alternative Considered**: Async/selective receive (receive from whichever channel has data first) - marked as out of scope in spec.

---

### Q2: How should ProcessResult handle nullable outputs?

**Decision**: Nullable fields with conditional send

**Rationale**: Multi-output nodes may not always produce values for all outputs. A nullable field in ProcessResult allows selective output.

**Implementation**:
```kotlin
data class ProcessResult2<U, V>(val out1: U?, val out2: V?)
data class ProcessResult3<U, V, W>(val out1: U?, val out2: V?, val out3: W?)

// In dispatch logic:
val result = process(input)
result.out1?.let { outputChannel1?.send(it) }
result.out2?.let { outputChannel2?.send(it) }
result.out3?.let { outputChannel3?.send(it) }
```

**Destructuring Support**:
```kotlin
val (u, v, w) = result  // All three accessible with correct types
```

---

### Q3: How should runtimes handle multiple output channels with different types?

**Decision**: Named output channel properties per type

**Rationale**: Since Kotlin doesn't support variance in the way that would allow a `List<SendChannel<*>>`, and we need type safety, each output channel must be a separate property with its own type.

**Implementation**:
```kotlin
class In1Out2Runtime<A, U, V>(
    codeNode: CodeNode,
    private val process: suspend (A) -> ProcessResult2<U, V>
) : NodeRuntime<A>(codeNode) {

    var outputChannel1: SendChannel<U>? = null
    var outputChannel2: SendChannel<V>? = null

    override fun start(scope: CoroutineScope, processingBlock: suspend () -> Unit) {
        nodeControlJob = scope.launch {
            val inChannel = inputChannel ?: return@launch
            for (value in inChannel) {
                val result = process(value)
                result.out1?.let { outputChannel1?.send(it) }
                result.out2?.let { outputChannel2?.send(it) }
            }
        }
    }
}
```

---

### Q4: Should we create separate runtime classes for each configuration or use a generic approach?

**Decision**: Separate runtime classes per configuration

**Rationale**:
1. Type safety at compile time requires explicit type parameters
2. Each configuration has a unique signature for its process function
3. IntelliJ/IDE autocomplete works better with explicit classes
4. Documentation is clearer per-class

**Tradeoff**: More code (15 runtime classes), but each is small (~30-50 lines)

**Alternative Considered**: Generic `MultiPortRuntime<T1, T2, T3, R1, R2, R3>` with Optional types - rejected due to complexity and nullable type handling.

---

### Q5: How should we handle channel closure in multi-input scenarios?

**Decision**: First channel closure triggers graceful shutdown

**Rationale**: If any input channel closes, the node cannot produce complete tuples. It should stop processing and close output channels.

**Implementation**:
```kotlin
nodeControlJob = scope.launch {
    try {
        while (isActive) {
            val val1 = inputChannel1!!.receive()
            val val2 = inputChannel2!!.receive()
            // process...
        }
    } catch (e: ClosedReceiveChannelException) {
        // Any input closed - graceful shutdown
    } finally {
        outputChannel1?.close()
        outputChannel2?.close()
    }
}
```

---

### Q6: What naming convention should factory methods use?

**Decision**: `createIn{N}Out{M}Processor` pattern

**Examples**:
- `createIn2Out1Processor<A, B, R>`
- `createIn1Out3Processor<A, U, V, W>`
- `createOut2Generator<U, V>` (0 inputs)
- `createIn3Sink<A, B, C>` (0 outputs)

**Aliases for existing methods**:
- `createIn1Out1Processor` → alias for `createContinuousTransformer`
- `createIn1Sink` → alias for `createContinuousSink`
- `createOut1Generator` → alias for `createContinuousGenerator`

**Rationale**: Naming is self-documenting and consistent. Reading `In2Out1` immediately conveys the port configuration.

---

### Q7: How should type aliases be structured for process functions?

**Decision**: Explicit type aliases in ContinuousTypes.kt

**Examples**:
```kotlin
// Single output
typealias In2Out1ProcessBlock<A, B, R> = suspend (A, B) -> R

// Multi-output with ProcessResult
typealias In1Out2ProcessBlock<A, U, V> = suspend (A) -> ProcessResult2<U, V>
typealias In2Out3ProcessBlock<A, B, U, V, W> = suspend (A, B) -> ProcessResult3<U, V, W>

// Generator multi-output
typealias Out2GeneratorBlock<U, V> = suspend (emit: suspend (ProcessResult2<U, V>) -> Unit) -> Unit
```

---

### Q8: Should ProcessResult support a single-output variant?

**Decision**: No, use direct return for single output

**Rationale**:
- `ProcessResult1<U>` adds unnecessary wrapper
- Single output cases can return `U` directly
- Consistency with existing `TransformerRuntime` which returns `TOut` directly

---

## Existing Code Analysis

### Current Runtime Hierarchy

```
NodeRuntime<T> (base)
├── GeneratorRuntime<T>      - 0 inputs, 1 output
├── SinkRuntime<T>           - 1 input, 0 outputs
├── TransformerRuntime<TIn, TOut>  - 1 input, 1 output
└── FilterRuntime<T>         - 1 input, 1 output (predicate-based)
```

### Patterns to Reuse

1. **Lifecycle management**: `nodeControlJob`, `executionState` from `NodeRuntime`
2. **Channel iteration**: `for (value in channel)` pattern from `SinkRuntime`
3. **Exception handling**: `ClosedReceiveChannelException` / `ClosedSendChannelException`
4. **Factory pattern**: `CodeNodeFactory.createContinuous*` methods

### New Runtime Classes Needed

```
NodeRuntime<T> (base)
├── [existing...]
├── In2SinkRuntime<A, B>           - 2 inputs, 0 outputs
├── In3SinkRuntime<A, B, C>        - 3 inputs, 0 outputs
├── In2Out1Runtime<A, B, R>        - 2 inputs, 1 output
├── In3Out1Runtime<A, B, C, R>     - 3 inputs, 1 output
├── In1Out2Runtime<A, U, V>        - 1 input, 2 outputs
├── In1Out3Runtime<A, U, V, W>     - 1 input, 3 outputs
├── In2Out2Runtime<A, B, U, V>     - 2 inputs, 2 outputs
├── In2Out3Runtime<A, B, U, V, W>  - 2 inputs, 3 outputs
├── In3Out2Runtime<A, B, C, U, V>  - 3 inputs, 2 outputs
├── In3Out3Runtime<A, B, C, U, V, W> - 3 inputs, 3 outputs
├── Out2GeneratorRuntime<U, V>     - 0 inputs, 2 outputs
└── Out3GeneratorRuntime<U, V, W>  - 0 inputs, 3 outputs
```

**Note**: Some configurations like In1Out1 are aliases to existing runtimes (TransformerRuntime).

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Type explosion (too many classes) | Medium | Low | Code generation or templates if needed |
| Channel deadlock with sync receive | Low | High | Document requirement for balanced data rates |
| Memory issues with buffered channels | Low | Medium | Configurable channel capacity |
| Breaking changes to existing runtimes | Low | High | Keep existing runtimes unchanged, add new ones |

---

## Recommendations

1. **Start with ProcessResult**: Foundation for multi-output support
2. **Implement In2Out1 first**: Most common multi-input case, validates pattern
3. **Add factory methods incrementally**: Don't implement all 15 at once
4. **Use virtual time testing**: Continue pattern from feature 014
5. **Consider code generation**: If boilerplate becomes excessive, use KotlinPoet
