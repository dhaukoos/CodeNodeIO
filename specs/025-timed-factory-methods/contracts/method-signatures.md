# Contract: Timed Factory Method Signatures

**Feature**: 025-timed-factory-methods
**Date**: 2026-02-20

## Verification Protocol

Each timed factory method must:
1. Be an `inline fun` with `reified` type parameters matching its continuous counterpart
2. Accept `tickIntervalMs: Long` as the second parameter (after `name`)
3. Accept `noinline tick:` with the corresponding tick type alias as the last parameter
4. Forward all other parameters (`channelCapacity`, `position`, `description`) to the continuous method
5. Return the exact same runtime type as the continuous method

## Method Signatures

### Generators

```kotlin
// NEW
inline fun <reified T : Any> createTimedGenerator(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: GeneratorTickBlock<T>
): GeneratorRuntime<T>

// EXISTS — no changes
inline fun <reified U : Any, reified V : Any> createTimedOut2Generator(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: Out2TickBlock<U, V>
): Out2GeneratorRuntime<U, V>

// NEW
inline fun <reified U : Any, reified V : Any, reified W : Any> createTimedOut3Generator(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: Out3TickBlock<U, V, W>
): Out3GeneratorRuntime<U, V, W>
```

### Processors

```kotlin
// NEW — Transformer (In1Out1)
inline fun <reified TIn : Any, reified TOut : Any> createTimedTransformer(
    name: String,
    tickIntervalMs: Long,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: TransformerTickBlock<TIn, TOut>
): TransformerRuntime<TIn, TOut>

// NEW — Filter (In1Out1, same type)
inline fun <reified T : Any> createTimedFilter(
    name: String,
    tickIntervalMs: Long,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: FilterTickBlock<T>
): FilterRuntime<T>

// NEW — In2Out1
inline fun <reified A : Any, reified B : Any, reified R : Any> createTimedIn2Out1Processor(
    name: String,
    tickIntervalMs: Long,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In2Out1TickBlock<A, B, R>
): In2Out1Runtime<A, B, R>

// NEW — In3Out1
inline fun <reified A : Any, reified B : Any, reified C : Any, reified R : Any> createTimedIn3Out1Processor(
    name: String,
    tickIntervalMs: Long,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In3Out1TickBlock<A, B, C, R>
): In3Out1Runtime<A, B, C, R>

// NEW — In1Out2
inline fun <reified A : Any, reified U : Any, reified V : Any> createTimedIn1Out2Processor(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In1Out2TickBlock<A, U, V>
): In1Out2Runtime<A, U, V>

// NEW — In1Out3
inline fun <reified A : Any, reified U : Any, reified V : Any, reified W : Any> createTimedIn1Out3Processor(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In1Out3TickBlock<A, U, V, W>
): In1Out3Runtime<A, U, V, W>

// NEW — In2Out2
inline fun <reified A : Any, reified B : Any, reified U : Any, reified V : Any> createTimedIn2Out2Processor(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In2Out2TickBlock<A, B, U, V>
): In2Out2Runtime<A, B, U, V>

// NEW — In2Out3
inline fun <reified A : Any, reified B : Any, reified U : Any, reified V : Any, reified W : Any> createTimedIn2Out3Processor(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In2Out3TickBlock<A, B, U, V, W>
): In2Out3Runtime<A, B, U, V, W>

// NEW — In3Out2
inline fun <reified A : Any, reified B : Any, reified C : Any, reified U : Any, reified V : Any> createTimedIn3Out2Processor(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In3Out2TickBlock<A, B, C, U, V>
): In3Out2Runtime<A, B, C, U, V>

// NEW — In3Out3
inline fun <reified A : Any, reified B : Any, reified C : Any, reified U : Any, reified V : Any, reified W : Any> createTimedIn3Out3Processor(
    name: String,
    tickIntervalMs: Long,
    channelCapacity: Int = Channel.BUFFERED,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In3Out3TickBlock<A, B, C, U, V, W>
): In3Out3Runtime<A, B, C, U, V, W>
```

### Sinks

```kotlin
// NEW — Single-input
inline fun <reified T : Any> createTimedSink(
    name: String,
    tickIntervalMs: Long,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: SinkTickBlock<T>
): SinkRuntime<T>

// NEW — 2-input
inline fun <reified A : Any, reified B : Any> createTimedIn2Sink(
    name: String,
    tickIntervalMs: Long,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In2SinkTickBlock<A, B>
): In2SinkRuntime<A, B>

// NEW — 3-input
inline fun <reified A : Any, reified B : Any, reified C : Any> createTimedIn3Sink(
    name: String,
    tickIntervalMs: Long,
    position: Node.Position = Node.Position.ORIGIN,
    description: String? = null,
    noinline tick: In3SinkTickBlock<A, B, C>
): In3SinkRuntime<A, B, C>
```

## Parameter Forwarding Rules

- **Generators** have `channelCapacity` (forwarded to continuous method)
- **Multi-output processors** (In1Out2+, In2Out2+, In3Out2+) have `channelCapacity` (forwarded)
- **Single-output processors** (Transformer, Filter, In2Out1, In3Out1) do NOT have `channelCapacity`
- **Sinks** do NOT have `channelCapacity`
- All methods forward `name`, `position`, `description`
