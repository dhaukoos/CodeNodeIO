# Data Model: Redesign Processing Logic Stub Generator

**Feature**: 026-processing-logic-stubs
**Date**: 2026-02-20

## Entities

### Tick Function Stub (Generated File)

A generated template file containing a top-level `val` property with the correct tick type alias signature.

| Attribute | Description |
|-----------|-------------|
| File name | `{NodeName}ProcessLogic.kt` (PascalCase node name + "ProcessLogic") |
| Package | `{basePackage}.logicmethods` |
| Val name | `{nodeName}Tick` (camelCase node name + "Tick") |
| Type alias | One of 16 tick type aliases, determined by input/output count |
| Type parameters | Resolved from CodeNode port data types |
| Body | TODO placeholder with compilable return value |
| KDoc | Node type, port descriptions |

**Lifecycle**: Created once by the stub generator. Never overwritten. Edited by the developer with actual business logic.

### Node-to-TickType Mapping

Maps a CodeNode's port configuration to the correct tick type alias and factory method.

| Input Count | Output Count | Category | Tick Type Alias | Factory Method (Timed) |
|-------------|-------------|----------|-----------------|----------------------|
| 0 | 1 | Generator | `GeneratorTickBlock<T>` | `createTimedGenerator` |
| 0 | 2 | Generator | `Out2TickBlock<U, V>` | `createTimedOut2Generator` |
| 0 | 3 | Generator | `Out3TickBlock<U, V, W>` | `createTimedOut3Generator` |
| 1 | 1 (same) | Filter | `FilterTickBlock<T>` | `createTimedFilter` |
| 1 | 1 (diff) | Transformer | `TransformerTickBlock<TIn, TOut>` | `createTimedTransformer` |
| 2 | 1 | Processor | `In2Out1TickBlock<A, B, R>` | `createTimedIn2Out1Processor` |
| 3 | 1 | Processor | `In3Out1TickBlock<A, B, C, R>` | `createTimedIn3Out1Processor` |
| 1 | 2 | Processor | `In1Out2TickBlock<A, U, V>` | `createTimedIn1Out2Processor` |
| 1 | 3 | Processor | `In1Out3TickBlock<A, U, V, W>` | `createTimedIn1Out3Processor` |
| 2 | 2 | Processor | `In2Out2TickBlock<A, B, U, V>` | `createTimedIn2Out2Processor` |
| 2 | 3 | Processor | `In2Out3TickBlock<A, B, U, V, W>` | `createTimedIn2Out3Processor` |
| 3 | 2 | Processor | `In3Out2TickBlock<A, B, C, U, V>` | `createTimedIn3Out2Processor` |
| 3 | 3 | Processor | `In3Out3TickBlock<A, B, C, U, V, W>` | `createTimedIn3Out3Processor` |
| 1 | 0 | Sink | `SinkTickBlock<T>` | `createTimedSink` |
| 2 | 0 | Sink | `In2SinkTickBlock<A, B>` | `createTimedIn2Sink` |
| 3 | 0 | Sink | `In3SinkTickBlock<A, B, C>` | `createTimedIn3Sink` |

### Removed Entities

| Entity | Location | Reason |
|--------|----------|--------|
| `ProcessingLogic` fun interface | CodeNode.kt lines 43-51 | Replaced by typed tick type aliases |
| `processingLogic` property | CodeNode.kt line 159 | Replaced by convention-based tick stub reference |
| `hasProcessingLogic()` | CodeNode.kt line 321 | No longer needed |
| `withProcessingLogic()` | CodeNode.kt line 329 | No longer needed |
| `process()` | CodeNode.kt line 340 | No longer needed |
| `processingLogic` parameter | CodeNodeFactory.create() line 45 | No longer needed |
| `_useCaseClass` config key | Various generators + PropertiesPanel | Replaced by convention-based naming |

## Naming Conventions

### Node Name to Identifier Mapping

| Node Name | File Name | Val Name | Type |
|-----------|-----------|----------|------|
| `TimerEmitter` | `TimerEmitterProcessLogic.kt` | `timerEmitterTick` | `Out2TickBlock<Int, Int>` |
| `DisplayReceiver` | `DisplayReceiverProcessLogic.kt` | `displayReceiverTick` | `In2SinkTickBlock<Int, Int>` |
| `DataTransformer` | `DataTransformerProcessLogic.kt` | `dataTransformerTick` | `TransformerTickBlock<String, Int>` |

### Type Parameter Resolution

Type parameters for the tick type alias are resolved from the CodeNode's port data types:

- **Inputs**: Ordered by port declaration order → type parameters A, B, C
- **Outputs**: Ordered by port declaration order → type parameters T/U/V/W (or R for single-output processors)
- **Default**: `Any` if port data type is not specified

## Module Directory Structure

```text
{ModuleName}/src/commonMain/kotlin/{packagePath}/
├── generated/                        # Auto-generated, overwritten on regeneration
│   ├── {GraphName}Flow.kt
│   ├── {GraphName}Controller.kt
│   └── ...
└── logicmethods/                     # Generated once, preserved on regeneration
    ├── {Node1Name}ProcessLogic.kt    # User-editable tick stub
    ├── {Node2Name}ProcessLogic.kt
    └── ...
```
