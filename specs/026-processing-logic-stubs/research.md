# Research: Redesign Processing Logic Stub Generator

**Feature**: 026-processing-logic-stubs
**Date**: 2026-02-20

## R1: Tick Stub Content Pattern

**Question**: What should the generated tick function stub file contain?

**Decision**: A top-level `val` property with the correct tick type alias, containing a TODO body with a compilable return value placeholder.

**Rationale**: The tick type aliases are simple suspend lambda signatures (e.g., `Out2TickBlock<Int, Int>` = `suspend () -> ProcessResult2<Int, Int>`). A top-level val is the simplest compilable form, matches Kotlin idioms for lambda constants, and can be imported directly by the generated factory code.

**Alternatives considered**:
- **Function returning a tick block**: `fun timerEmitterTick(): Out2TickBlock<Int, Int> = { ... }` — adds unnecessary indirection. The tick block is already a function; wrapping it in another function adds no value.
- **Object with tick property**: `object TimerEmitterProcessLogic { val tick: ... }` — adds namespace but complicates imports and doesn't align with how the StopWatch components define their tick blocks (simple val properties).
- **Class implementing an interface**: The old `ProcessingLogic` pattern — explicitly what we're replacing. Classes add ceremony without benefit for stateless tick functions.

**Example generated stub** (for a 0-input, 2-output generator with Int outputs):
```kotlin
package io.codenode.stopwatch.logicmethods

import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2

/**
 * Tick function for the TimerEmitter node.
 *
 * Node type: Generator (0 inputs, 2 outputs)
 *
 * Outputs:
 *   - elapsedSeconds: Int
 *   - elapsedMinutes: Int
 */
val timerEmitterTick: Out2TickBlock<Int, Int> = {
    // TODO: Implement TimerEmitter tick logic
    ProcessResult2.both(0, 0)
}
```

**Note**: The StopWatch `TimerEmitterComponent` is a full class with internal state (StateFlows) and lifecycle delegation. The tick stub is intentionally simpler — just the tick function. Developers who need observable state or lifecycle management can create a wrapper class around the tick, but the stub gives them the correctly-typed starting point.

---

## R2: How the Factory Generator References Tick Stubs

**Question**: How does generated factory code find and reference the tick function from the stub?

**Decision**: Convention-based import. The generator derives the tick function name from the node name using a fixed naming convention:
- Package: `{basePackage}.logicmethods`
- Val name: `{nodeName}Tick` (camelCase of the node name + "Tick")
- Import: `import {basePackage}.logicmethods.{nodeName}Tick`

**Rationale**: Convention over configuration. The `_useCaseClass` config key was necessary because ProcessingLogic classes could be anywhere. With generated stubs in a known location with a known naming pattern, no configuration is needed.

**Alternatives considered**:
- **Keep `_useCaseClass` config key**: Flexible but error-prone. Developers must keep the config in sync with the actual file location. Convention eliminates this coupling.
- **Generate import from file scan**: Too fragile. Depends on file system state at generation time.
- **Hardcoded tick function name in node config**: Redundant when node name already determines the tick name.

**Generated factory code example**:
```kotlin
import io.codenode.stopwatch.logicmethods.timerEmitterTick

val timerEmitter = CodeNodeFactory.createTimedOut2Generator<Int, Int>(
    name = "TimerEmitter",
    tickIntervalMs = 1000,
    tick = timerEmitterTick
)
```

---

## R3: Replacing _useCaseClass Configuration

**Question**: What happens to the `_useCaseClass` configuration key?

**Decision**: Remove it. Convention-based tick stub naming eliminates the need for explicit class references in node configuration.

**Rationale**: The `_useCaseClass` key served two purposes:
1. Tell the FlowGraphFactoryGenerator which class to instantiate
2. Tell the FlowKtGenerator which class to reference in the DSL

With convention-based tick stubs:
1. The factory generator derives the tick val name from the node name
2. The FlowKtGenerator no longer needs to emit processing logic references (tick stubs are code-generation-only, not serialized in the DSL)

**Impact**:
- `FlowGraphFactoryGenerator.kt`: Replace `_useCaseClass` lookup with convention-based naming
- `FlowKtGenerator.kt`: Remove the `processingLogic<T>()` generation code (lines 156-161)
- `PropertiesPanel.kt`: Remove the _useCaseClass property editor (lines 714-725)
- `FlowKtGeneratorTest.kt`: Remove tests for processingLogic<T>() generation (lines 391-483)

---

## R4: LogicMethods Folder Structure

**Question**: Where in the module should tick stubs be placed?

**Decision**: `src/commonMain/kotlin/{packagePath}/logicmethods/` — a peer of the `generated/` folder within the module.

**Rationale**: The user specified "LogicMethods" folder, separate from the "generated" folder. Using `logicmethods` (lowercase, no separator) follows Kotlin package naming conventions. Placing it as a peer to `generated/` makes the distinction clear: files in `generated/` are overwritten on regeneration; files in `logicmethods/` are preserved.

**Example directory layout**:
```
StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/
├── generated/
│   └── StopWatchFlow.kt           # Auto-generated, overwritten
└── logicmethods/
    ├── TimerEmitterProcessLogic.kt    # Generated once, then user-edited
    └── DisplayReceiverProcessLogic.kt # Generated once, then user-edited
```

---

## R5: Stub Overwrite Protection

**Question**: How to handle regeneration when a stub file already exists?

**Decision**: Check if the file exists before writing. If it exists, skip generation for that node. The generator should return a status indicating which stubs were created vs. preserved.

**Rationale**: The user explicitly stated the stubs are "a template that will be edited to contain the actual functionality." Overwriting would destroy user work. This is the same pattern used by many code generators (e.g., Android Room migration stubs, Rails scaffolding).

**Alternatives considered**:
- **Always overwrite with backup**: Creates clutter, users may not notice their edits were backed up.
- **Merge generated and user code**: Too complex, unreliable for arbitrary user edits.
- **Overwrite with diff prompt**: Interactive approach doesn't work for automated code generation.

---

## R6: Tick Type Alias Selection Logic

**Question**: How does the stub generator determine which tick type alias to use for a given node?

**Decision**: Map from (inputCount, outputCount) to the correct tick type alias name. The mapping is deterministic based on the node's port configuration.

**Mapping table**:

| Inputs | Outputs | Node Type | Tick Type Alias | Continuous Block Used |
|--------|---------|-----------|-----------------|----------------------|
| 0 | 1 | Generator | `GeneratorTickBlock<T>` | `ContinuousGeneratorBlock<T>` |
| 0 | 2 | Generator | `Out2TickBlock<U, V>` | `Out2GeneratorBlock<U, V>` |
| 0 | 3 | Generator | `Out3TickBlock<U, V, W>` | `Out3GeneratorBlock<U, V, W>` |
| 1 | 1 (same type) | Filter | `FilterTickBlock<T>` | `ContinuousFilterPredicate<T>` |
| 1 | 1 (diff type) | Transformer | `TransformerTickBlock<TIn, TOut>` | `ContinuousTransformBlock<TIn, TOut>` |
| 2 | 1 | Processor | `In2Out1TickBlock<A, B, R>` | `In2Out1ProcessBlock<A, B, R>` |
| 3 | 1 | Processor | `In3Out1TickBlock<A, B, C, R>` | `In3Out1ProcessBlock<A, B, C, R>` |
| 1 | 2 | Processor | `In1Out2TickBlock<A, U, V>` | `In1Out2ProcessBlock<A, U, V>` |
| 1 | 3 | Processor | `In1Out3TickBlock<A, U, V, W>` | `In1Out3ProcessBlock<A, U, V, W>` |
| 2 | 2 | Processor | `In2Out2TickBlock<A, B, U, V>` | `In2Out2ProcessBlock<A, B, U, V>` |
| 2 | 3 | Processor | `In2Out3TickBlock<A, B, U, V, W>` | `In2Out3ProcessBlock<A, B, U, V, W>` |
| 3 | 2 | Processor | `In3Out2TickBlock<A, B, C, U, V>` | `In3Out2ProcessBlock<A, B, C, U, V>` |
| 3 | 3 | Processor | `In3Out3TickBlock<A, B, C, U, V, W>` | `In3Out3ProcessBlock<A, B, C, U, V, W>` |
| 1 | 0 | Sink | `SinkTickBlock<T>` | `ContinuousSinkBlock<T>` |
| 2 | 0 | Sink | `In2SinkTickBlock<A, B>` | `In2SinkBlock<A, B>` |
| 3 | 0 | Sink | `In3SinkTickBlock<A, B, C>` | `In3SinkBlock<A, B, C>` |

**Transformer vs Filter distinction**: When inputs=1 and outputs=1, if the input type equals the output type, it's a filter (predicate returns Boolean). If types differ, it's a transformer. This can be determined from the port data types on the CodeNode.

**Note**: The `_genericType` config key (e.g., `"in0out2"`) already encodes the input/output count. The stub generator can use either port counting or this config value.

---

## R7: Factory Method Selection Logic

**Question**: Which factory method should the generated code call for each node type?

**Decision**: Use timed factory methods when a `tickIntervalMs` configuration exists on the node; otherwise use continuous factory methods.

**Mapping**: Each tick type alias maps to exactly one timed factory method (Feature 025) and one continuous factory method (Feature 014/015):

| Tick Type Alias | Timed Factory | Continuous Factory |
|-----------------|---------------|-------------------|
| `GeneratorTickBlock<T>` | `createTimedGenerator` | `createContinuousGenerator` |
| `Out2TickBlock<U, V>` | `createTimedOut2Generator` | `createOut2Generator` |
| `Out3TickBlock<U, V, W>` | `createTimedOut3Generator` | `createOut3Generator` |
| `TransformerTickBlock<TIn, TOut>` | `createTimedTransformer` | `createContinuousTransformer` |
| `FilterTickBlock<T>` | `createTimedFilter` | `createContinuousFilter` |
| `In2Out1TickBlock<A, B, R>` | `createTimedIn2Out1Processor` | `createIn2Out1Processor` |
| ... (and so on for all 16 combinations) | ... | ... |
| `SinkTickBlock<T>` | `createTimedSink` | `createContinuousSink` |
| `In2SinkTickBlock<A, B>` | `createTimedIn2Sink` | `createIn2Sink` |
| `In3SinkTickBlock<A, B, C>` | `createTimedIn3Sink` | `createIn3Sink` |

**Rationale**: The StopWatch example uses `createTimedOut2Generator` with a `speedAttenuation` config value as the tick interval. The generator reads `tickIntervalMs` (or a similar config key) to decide between timed vs. continuous.

---

## R8: Impact on Existing StopWatch Module

**Question**: Does this change break the existing StopWatch module?

**Decision**: The StopWatch module's manually-written components (`TimerEmitterComponent`, `DisplayReceiverComponent`) already use the tick block pattern and do NOT use the ProcessingLogic interface. They will not be affected by removing ProcessingLogic.

However, the StopWatch's `_useCaseClass` references in `StopWatch.flow.kt` and `RegenerateStopWatch.kt` will need updating to align with the new convention. This is a cleanup task, not a breaking change — the StopWatch module compiles independently of the code generator.

**Rationale**: The StopWatch components are the reference implementation. They already follow the target pattern. The generated code should produce something structurally equivalent (though simpler — just the tick function, not the full component wrapper).
