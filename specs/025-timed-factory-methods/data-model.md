# Data Model: Timed Factory Methods

**Feature**: 025-timed-factory-methods
**Date**: 2026-02-20

## Entities

This feature adds no new runtime classes or data model entities. It adds type aliases (compile-time only) and factory methods (convenience wrappers).

### Tick Block Type Aliases (15 new + 1 existing)

All type aliases are `suspend` lambda functions. They exist for type safety and API clarity only.

#### Generator Tick Blocks

| Alias | Signature | Status |
|-------|-----------|--------|
| `GeneratorTickBlock<T>` | `suspend () -> T` | NEW |
| `Out2TickBlock<U, V>` | `suspend () -> ProcessResult2<U, V>` | EXISTS |
| `Out3TickBlock<U, V, W>` | `suspend () -> ProcessResult3<U, V, W>` | NEW |

#### Processor Tick Blocks

| Alias | Signature | Status |
|-------|-----------|--------|
| `TransformerTickBlock<TIn, TOut>` | `suspend (TIn) -> TOut` | NEW |
| `FilterTickBlock<T>` | `suspend (T) -> Boolean` | NEW |
| `In2Out1TickBlock<A, B, R>` | `suspend (A, B) -> R` | NEW |
| `In3Out1TickBlock<A, B, C, R>` | `suspend (A, B, C) -> R` | NEW |
| `In1Out2TickBlock<A, U, V>` | `suspend (A) -> ProcessResult2<U, V>` | NEW |
| `In1Out3TickBlock<A, U, V, W>` | `suspend (A) -> ProcessResult3<U, V, W>` | NEW |
| `In2Out2TickBlock<A, B, U, V>` | `suspend (A, B) -> ProcessResult2<U, V>` | NEW |
| `In2Out3TickBlock<A, B, U, V, W>` | `suspend (A, B) -> ProcessResult3<U, V, W>` | NEW |
| `In3Out2TickBlock<A, B, C, U, V>` | `suspend (A, B, C) -> ProcessResult2<U, V>` | NEW |
| `In3Out3TickBlock<A, B, C, U, V, W>` | `suspend (A, B, C) -> ProcessResult3<U, V, W>` | NEW |

#### Sink Tick Blocks

| Alias | Signature | Status |
|-------|-----------|--------|
| `SinkTickBlock<T>` | `suspend (T) -> Unit` | NEW |
| `In2SinkTickBlock<A, B>` | `suspend (A, B) -> Unit` | NEW |
| `In3SinkTickBlock<A, B, C>` | `suspend (A, B, C) -> Unit` | NEW |

### Timed Factory Methods (15 new + 1 existing)

Each timed method wraps a tick block + interval into its continuous counterpart.

#### Generator Methods

| Method | Delegates To | Tick Alias | Status |
|--------|-------------|------------|--------|
| `createTimedGenerator<T>` | `createContinuousGenerator<T>` | `GeneratorTickBlock<T>` | NEW |
| `createTimedOut2Generator<U, V>` | `createOut2Generator<U, V>` | `Out2TickBlock<U, V>` | EXISTS |
| `createTimedOut3Generator<U, V, W>` | `createOut3Generator<U, V, W>` | `Out3TickBlock<U, V, W>` | NEW |

#### Processor Methods

| Method | Delegates To | Tick Alias | Status |
|--------|-------------|------------|--------|
| `createTimedTransformer<TIn, TOut>` | `createContinuousTransformer` | `TransformerTickBlock` | NEW |
| `createTimedFilter<T>` | `createContinuousFilter<T>` | `FilterTickBlock<T>` | NEW |
| `createTimedIn2Out1Processor<A, B, R>` | `createIn2Out1Processor` | `In2Out1TickBlock` | NEW |
| `createTimedIn3Out1Processor<A, B, C, R>` | `createIn3Out1Processor` | `In3Out1TickBlock` | NEW |
| `createTimedIn1Out2Processor<A, U, V>` | `createIn1Out2Processor` | `In1Out2TickBlock` | NEW |
| `createTimedIn1Out3Processor<A, U, V, W>` | `createIn1Out3Processor` | `In1Out3TickBlock` | NEW |
| `createTimedIn2Out2Processor<A, B, U, V>` | `createIn2Out2Processor` | `In2Out2TickBlock` | NEW |
| `createTimedIn2Out3Processor<A, B, U, V, W>` | `createIn2Out3Processor` | `In2Out3TickBlock` | NEW |
| `createTimedIn3Out2Processor<A, B, C, U, V>` | `createIn3Out2Processor` | `In3Out2TickBlock` | NEW |
| `createTimedIn3Out3Processor<A, B, C, U, V, W>` | `createIn3Out3Processor` | `In3Out3TickBlock` | NEW |

#### Sink Methods

| Method | Delegates To | Tick Alias | Status |
|--------|-------------|------------|--------|
| `createTimedSink<T>` | `createContinuousSink<T>` | `SinkTickBlock<T>` | NEW |
| `createTimedIn2Sink<A, B>` | `createIn2Sink<A, B>` | `In2SinkTickBlock<A, B>` | NEW |
| `createTimedIn3Sink<A, B, C>` | `createIn3Sink<A, B, C>` | `In3SinkTickBlock<A, B, C>` | NEW |
