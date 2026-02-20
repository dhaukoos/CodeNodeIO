# Contract: Tick Stub Generator

**Feature**: 026-processing-logic-stubs
**Date**: 2026-02-20

## ProcessingLogicStubGenerator (Redesigned)

### Public API

```
generateStub(codeNode, packageName) → String
  - Generates the complete Kotlin source for a tick function stub file
  - Returns empty string if node has 0 inputs AND 0 outputs

getStubFileName(codeNode) → String
  - Returns "{NodeName}ProcessLogic.kt"

getTickValName(codeNode) → String
  - Returns "{nodeName}Tick" (camelCase)

getTickTypeAlias(codeNode) → String
  - Returns the tick type alias name (e.g., "Out2TickBlock<Int, Int>")

shouldGenerateStub(codeNode) → Boolean
  - Returns false if node has 0 inputs and 0 outputs
  - Returns false if input count > 3 or output count > 3
  - Returns true otherwise
```

### Generated File Structure

```
// Package declaration
package {packageName}.logicmethods

// Imports (tick type alias + supporting types)
import io.codenode.fbpdsl.runtime.{TickTypeAlias}
import io.codenode.fbpdsl.runtime.ProcessResult2  // only if multi-output
import io.codenode.fbpdsl.runtime.ProcessResult3  // only if 3-output

// KDoc with node description and port listing
/**
 * Tick function for the {NodeName} node.
 *
 * Node type: {Generator|Transformer|Filter|Processor|Sink} ({N} inputs, {M} outputs)
 *
 * [Inputs section if inputs > 0]
 * Inputs:
 *   - {portName}: {PortType}
 *
 * [Outputs section if outputs > 0]
 * Outputs:
 *   - {portName}: {PortType}
 */
val {nodeName}Tick: {TickTypeAlias}<{TypeParams}> = { [params ->]
    // TODO: Implement {NodeName} tick logic
    {defaultReturnValue}
}
```

### Default Return Values by Category

| Category | Return Value |
|----------|-------------|
| Generator (1 output) | Default value for type (e.g., `0`, `""`, `false`) |
| Generator (2 outputs) | `ProcessResult2.both(default1, default2)` |
| Generator (3 outputs) | `ProcessResult3(default1, default2, default3)` |
| Transformer | Default value for output type |
| Filter | `true` |
| Processor (1 output) | Default value for output type |
| Processor (2 outputs) | `ProcessResult2.both(default1, default2)` |
| Processor (3 outputs) | `ProcessResult3(default1, default2, default3)` |
| Sink (any inputs) | (no return value — Unit) |

### Default Values by Type

| Type | Default |
|------|---------|
| `Int` | `0` |
| `Long` | `0L` |
| `Double` | `0.0` |
| `Float` | `0.0f` |
| `String` | `""` |
| `Boolean` | `false` |
| `Any` / unknown | `TODO("Provide default value")` |

---

## FlowGraphFactoryGenerator (Updated)

### Changes to Generated Factory Code

**Before** (ProcessingLogic pattern):
```
val {nodeName}Logic = {ComponentClassName}()
val {nodeName} = CodeNode(
    processingLogic = {nodeName}Logic,
    ...
)
```

**After** (Tick stub pattern):
```
import {packageName}.logicmethods.{nodeName}Tick

val {nodeName} = CodeNodeFactory.createTimed{NodeType}<{TypeParams}>(
    name = "{NodeName}",
    tickIntervalMs = {intervalFromConfig},
    tick = {nodeName}Tick
)
```

### Factory Method Selection

The generator selects the factory method based on (inputCount, outputCount):

| Inputs | Outputs | Factory Method |
|--------|---------|---------------|
| 0 | 1 | `createTimedGenerator<T>` |
| 0 | 2 | `createTimedOut2Generator<U, V>` |
| 0 | 3 | `createTimedOut3Generator<U, V, W>` |
| 1 | 1 (filter) | `createTimedFilter<T>` |
| 1 | 1 (transform) | `createTimedTransformer<TIn, TOut>` |
| 2 | 1 | `createTimedIn2Out1Processor<A, B, R>` |
| 3 | 1 | `createTimedIn3Out1Processor<A, B, C, R>` |
| 1 | 2 | `createTimedIn1Out2Processor<A, U, V>` |
| 1 | 3 | `createTimedIn1Out3Processor<A, U, V, W>` |
| 2 | 2 | `createTimedIn2Out2Processor<A, B, U, V>` |
| 2 | 3 | `createTimedIn2Out3Processor<A, B, U, V, W>` |
| 3 | 2 | `createTimedIn3Out2Processor<A, B, C, U, V>` |
| 3 | 3 | `createTimedIn3Out3Processor<A, B, C, U, V, W>` |
| 1 | 0 | `createTimedSink<T>` |
| 2 | 0 | `createTimedIn2Sink<A, B>` |
| 3 | 0 | `createTimedIn3Sink<A, B, C>` |

---

## Removed Symbols

### CodeNode.kt Removals

| Symbol | Type | Lines |
|--------|------|-------|
| `ProcessingLogic` | fun interface | 43-51 |
| `processingLogic` | property | 159 |
| `hasProcessingLogic()` | method | 321 |
| `withProcessingLogic()` | method | 329-331 |
| `process()` | method | 340-345 |

### CodeNodeFactory.kt Removals

| Symbol | Type | Lines |
|--------|------|-------|
| `processingLogic` parameter | parameter in `create()` | 45 |

### FlowKtGenerator.kt Removals

| Symbol | Type | Lines |
|--------|------|-------|
| `processingLogic<T>()` generation | code block | 156-161 |

### Configuration Key Removals

| Key | Used In |
|-----|---------|
| `_useCaseClass` | FlowGraphFactoryGenerator, FlowKtGenerator, PropertiesPanel |
