# Naming Convention Contract: Source and Sink Nodes

**Feature Branch**: `037-source-sink-refactor`
**Date**: 2026-03-04

## Runtime Class Naming

Pattern: `{Role}{Direction}{Count}Runtime`

| Inputs | Outputs | Old Name | New Name |
|--------|---------|----------|----------|
| 0 | 1 | `GeneratorRuntime<T>` | `SourceRuntime<T>` |
| 0 | 2 | `Out2GeneratorRuntime<U, V>` | `SourceOut2Runtime<U, V>` |
| 0 | 3 | `Out3GeneratorRuntime<U, V, W>` | `SourceOut3Runtime<U, V, W>` |
| 1 | 0 | `SinkRuntime<T>` | `SinkRuntime<T>` *(unchanged)* |
| 2 | 0 | `In2SinkRuntime<A, B>` | `SinkIn2Runtime<A, B>` |
| 3 | 0 | `In3SinkRuntime<A, B, C>` | `SinkIn3Runtime<A, B, C>` |
| 2 (any) | 0 | `In2AnySinkRuntime<A, B>` | `SinkIn2AnyRuntime<A, B>` |
| 3 (any) | 0 | `In3AnySinkRuntime<A, B, C>` | `SinkIn3AnyRuntime<A, B, C>` |

## Factory Method Naming

Pattern: `create{Modifier}{Role}{Direction}{Count}`

| Old Method | New Method | Returns |
|-----------|-----------|---------|
| `createContinuousGenerator<T>` | `createContinuousSource<T>` | `SourceRuntime<T>` |
| `createOut2Generator<U, V>` | `createSourceOut2<U, V>` | `SourceOut2Runtime<U, V>` |
| `createOut3Generator<U, V, W>` | `createSourceOut3<U, V, W>` | `SourceOut3Runtime<U, V, W>` |
| `createContinuousSink<T>` | `createContinuousSink<T>` *(unchanged)* | `SinkRuntime<T>` |
| `createIn2Sink<A, B>` | `createSinkIn2<A, B>` | `SinkIn2Runtime<A, B>` |
| `createIn3Sink<A, B, C>` | `createSinkIn3<A, B, C>` | `SinkIn3Runtime<A, B, C>` |
| `createIn2AnySink<A, B>` | `createSinkIn2Any<A, B>` | `SinkIn2AnyRuntime<A, B>` |
| `createIn3AnySink<A, B, C>` | `createSinkIn3Any<A, B, C>` | `SinkIn3AnyRuntime<A, B, C>` |

### Removed Factory Methods (timed variants)

| Removed Method | Reason |
|---------------|--------|
| `createTimedGenerator<T>` | Source nodes no longer use timed tick loops |
| `createTimedOut2Generator<U, V>` | Same |
| `createTimedOut3Generator<U, V, W>` | Same |
| `createTimedSink<T>` | Sink nodes are pure state bridges, no ticks |
| `createTimedIn2Sink<A, B>` | Same |
| `createTimedIn3Sink<A, B, C>` | Same |

## Type Alias Naming

Pattern: `{Role}{Direction}{Count}Block` or `Continuous{Role}Block`

| Old Alias | New Alias |
|----------|----------|
| `ContinuousGeneratorBlock<T>` | `ContinuousSourceBlock<T>` |
| `Out2GeneratorBlock<U, V>` | `SourceOut2Block<U, V>` |
| `Out3GeneratorBlock<U, V, W>` | `SourceOut3Block<U, V, W>` |
| `ContinuousSinkBlock<T>` | `ContinuousSinkBlock<T>` *(unchanged)* |
| `In2SinkBlock<A, B>` | `SinkIn2Block<A, B>` |
| `In3SinkBlock<A, B, C>` | `SinkIn3Block<A, B, C>` |
| `In2AnySinkBlock<A, B>` | `SinkIn2AnyBlock<A, B>` |
| `In3AnySinkBlock<A, B, C>` | `SinkIn3AnyBlock<A, B, C>` |

### Removed Type Aliases (tick variants)

| Removed Alias | Reason |
|--------------|--------|
| `GeneratorTickBlock<T>` | No timed source nodes |
| `Out2TickBlock<U, V>` | No timed source nodes |
| `Out3TickBlock<U, V, W>` | No timed source nodes |
| `SinkTickBlock<T>` | No timed sink nodes |
| `In2SinkTickBlock<A, B>` | No timed sink nodes |
| `In3SinkTickBlock<A, B, C>` | No timed sink nodes |
| `In2AnySinkTickBlock<A, B>` | No timed sink nodes |
| `In3AnySinkTickBlock<A, B, C>` | No timed sink nodes |

## Enum Value

| Old | New |
|-----|-----|
| `CodeNodeType.GENERATOR` | `CodeNodeType.SOURCE` |

## Serialization Compatibility

- **Write**: Always serialize as `"SOURCE"`
- **Read**: Accept both `"SOURCE"` (new) and `"GENERATOR"` (legacy) — map both to `CodeNodeType.SOURCE`
