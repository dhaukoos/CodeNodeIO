# Data Model: Modify Source and Sink Nodes

**Feature Branch**: `037-source-sink-refactor`
**Date**: 2026-03-04

## Entity Changes

### CodeNodeType Enum

**Before:**
```
TRANSFORMER, FILTER, SPLITTER, MERGER, VALIDATOR, GENERATOR, SINK, API_ENDPOINT, DATABASE, CUSTOM, GENERIC
```

**After:**
```
TRANSFORMER, FILTER, SPLITTER, MERGER, VALIDATOR, SOURCE, SINK, API_ENDPOINT, DATABASE, CUSTOM, GENERIC
```

Change: `GENERATOR` → `SOURCE`

### Runtime Class Hierarchy

**Before:**
```
NodeRuntime (abstract)
├── GeneratorRuntime<T>         (0 inputs, 1 output)
├── Out2GeneratorRuntime<U, V>  (0 inputs, 2 outputs)
├── Out3GeneratorRuntime<U, V, W> (0 inputs, 3 outputs)
├── SinkRuntime<T>              (1 input, 0 outputs)
├── In2SinkRuntime<A, B>        (2 inputs, 0 outputs)
├── In3SinkRuntime<A, B, C>     (3 inputs, 0 outputs)
├── In2AnySinkRuntime<A, B>     (2 inputs any-trigger, 0 outputs)
├── In3AnySinkRuntime<A, B, C>  (3 inputs any-trigger, 0 outputs)
├── TransformerRuntime<TIn, TOut>
├── FilterRuntime<T>
├── In2Out1Runtime<A, B, R>
├── In3Out1Runtime<A, B, C, R>
├── In1Out2Runtime<A, U, V>
├── In1Out3Runtime<A, U, V, W>
├── In2Out2Runtime<A, B, U, V>
├── In2Out3Runtime<A, B, U, V, W>
├── In3Out2Runtime<A, B, C, U, V>
└── In3Out3Runtime<A, B, C, U, V, W>
```

**After:**
```
NodeRuntime (abstract)
├── SourceRuntime<T>            (0 inputs, 1 output)
├── SourceOut2Runtime<U, V>     (0 inputs, 2 outputs)
├── SourceOut3Runtime<U, V, W>  (0 inputs, 3 outputs)
├── SinkRuntime<T>              (1 input, 0 outputs) [unchanged]
├── SinkIn2Runtime<A, B>        (2 inputs, 0 outputs)
├── SinkIn3Runtime<A, B, C>     (3 inputs, 0 outputs)
├── SinkIn2AnyRuntime<A, B>     (2 inputs any-trigger, 0 outputs)
├── SinkIn3AnyRuntime<A, B, C>  (3 inputs any-trigger, 0 outputs)
├── TransformerRuntime<TIn, TOut> [unchanged]
├── FilterRuntime<T>            [unchanged]
├── In2Out1Runtime<A, B, R>     [unchanged]
├── ...remaining InXOutY unchanged
```

### Type Aliases

**Removed (timed tick types no longer needed):**
- `GeneratorTickBlock<T>`
- `Out2TickBlock<U, V>`
- `Out3TickBlock<U, V, W>`
- `SinkTickBlock<T>`
- `In2SinkTickBlock<A, B>`
- `In3SinkTickBlock<A, B, C>`
- `In2AnySinkTickBlock<A, B>`
- `In3AnySinkTickBlock<A, B, C>`

**Renamed (continuous block types):**

| Old | New |
|-----|-----|
| `ContinuousGeneratorBlock<T>` | `ContinuousSourceBlock<T>` |
| `Out2GeneratorBlock<U, V>` | `SourceOut2Block<U, V>` |
| `Out3GeneratorBlock<U, V, W>` | `SourceOut3Block<U, V, W>` |
| `In2SinkBlock<A, B>` | `SinkIn2Block<A, B>` |
| `In3SinkBlock<A, B, C>` | `SinkIn3Block<A, B, C>` |
| `In2AnySinkBlock<A, B>` | `SinkIn2AnyBlock<A, B>` |
| `In3AnySinkBlock<A, B, C>` | `SinkIn3AnyBlock<A, B, C>` |

**Unchanged:**
- `ContinuousSinkBlock<T>` — already consistent naming

### Factory Methods

**Removed (timed variants no longer needed):**
- `createTimedGenerator<T>`
- `createTimedOut2Generator<U, V>`
- `createTimedOut3Generator<U, V, W>`
- `createTimedSink<T>`
- `createTimedIn2Sink<A, B>`
- `createTimedIn3Sink<A, B, C>`

**Renamed:**

| Old | New |
|-----|-----|
| `createContinuousGenerator<T>` | `createContinuousSource<T>` |
| `createOut2Generator<U, V>` | `createSourceOut2<U, V>` |
| `createOut3Generator<U, V, W>` | `createSourceOut3<U, V, W>` |
| `createIn2Sink<A, B>` | `createSinkIn2<A, B>` |
| `createIn3Sink<A, B, C>` | `createSinkIn3<A, B, C>` |
| `createIn2AnySink<A, B>` | `createSinkIn2Any<A, B>` |
| `createIn3AnySink<A, B, C>` | `createSinkIn3Any<A, B, C>` |

**Unchanged:**
- `createContinuousSink<T>` — already consistent naming

### ProcessingLogic Stub Generation

**Before:** Stubs generated for all node types with >0 inputs or >0 outputs (including generators and sinks)

**After:** Stubs NOT generated for:
- Source nodes (`CodeNodeType.SOURCE`) — data comes from UI via ViewModel
- Sink nodes (`CodeNodeType.SINK`) — data bridges to ViewModel observable state

Stubs still generated for: `TRANSFORMER`, `FILTER`, `SPLITTER`, `MERGER`, and multi-input/multi-output processors (`In{X}Out{Y}`)

### Generated Runtime Code Pattern

**Source Node Before (timed generator):**
```kotlin
val timer = CodeNodeFactory.createTimedGenerator<Int>(
    name = "Timer",
    tickIntervalMs = 1000L,
    tick = timerTick  // from ProcessingLogic stub
)
```

**Source Node After (ViewModel-driven):**
```kotlin
val timer = CodeNodeFactory.createContinuousSource<Int>(
    name = "Timer",
    generate = { emit ->
        // ViewModel/Controller drives emission externally
    }
)
```

**Sink Node Before (with tick call):**
```kotlin
val display = CodeNodeFactory.createContinuousSink<String>(
    name = "Display",
    consume = { value ->
        StopWatchState._displayValue.value = value
        displayTick(value)  // from ProcessingLogic stub
    }
)
```

**Sink Node After (state bridge only):**
```kotlin
val display = CodeNodeFactory.createContinuousSink<String>(
    name = "Display",
    consume = { value ->
        StopWatchState._displayValue.value = value
    }
)
```

## Serialization Impact

The `.flow.kt` DSL files serialize `CodeNodeType.name` as a string. Changing `GENERATOR` to `SOURCE` affects:
- `FlowKtGenerator` — generates `nodeType = "SOURCE"` instead of `nodeType = "GENERATOR"`
- `FlowGraphSerializer` — must parse `"SOURCE"` when deserializing (and handle `"GENERATOR"` for backward compatibility with existing `.flow.kt` files)
