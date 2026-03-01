# Research: Any-Input Trigger Mode for Node Generator

**Feature**: 035-any-input-trigger
**Date**: 2026-03-01

## R1: Concurrent Channel Listening Pattern (Kotlin `select` Expression)

**Decision**: Use `kotlinx.coroutines.selects.select` expression for concurrent multi-channel listening.

**Rationale**: The existing all-input runtimes (e.g., `In2Out1Runtime`) use sequential `receive()` calls that block until ALL inputs have data. The any-input variant needs to fire as soon as ANY input delivers data. Kotlin's `select` expression is designed exactly for this — it suspends until one of several channel operations completes, then executes the corresponding block.

**Pattern**:
```kotlin
while (executionState != ExecutionState.IDLE) {
    // Pause check
    while (executionState == ExecutionState.PAUSED) { delay(10) }
    if (executionState == ExecutionState.IDLE) break

    select<Unit> {
        inChannel1.onReceive { value ->
            lastValue1 = value
            val result = process(lastValue1, lastValue2)
            outChannel.send(result)
        }
        inChannel2.onReceive { value ->
            lastValue2 = value
            val result = process(lastValue1, lastValue2)
            outChannel.send(result)
        }
    }
}
```

**Alternatives considered**:
- **Multiple coroutines per channel**: Launch separate jobs for each input. Rejected — harder to coordinate pause/resume/stop lifecycle, race conditions on shared cached values.
- **Flow.merge()**: Merge inputs into a single flow. Rejected — requires wrapping channels as flows, adds complexity, and loses the ability to identify which input triggered.

## R2: Existing Runtime Class Structure

**Decision**: Mirror the existing `In{A}Out{B}Runtime` class structure for `In{A}AnyOut{B}Runtime`.

**Rationale**: Each existing runtime class (e.g., `In2Out1Runtime`) extends `NodeRuntime`, has typed input/output channel properties, and overrides `start()` with a processing loop. The any-input variants follow the same pattern but add:
1. `lastValueN` cached fields (one per input, initialized to type-appropriate defaults)
2. `select` expression instead of sequential `receive()` calls
3. `reset()` method to clear cached values

**Key files**:
- `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2Out1Runtime.kt` — template for processors
- `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/In2SinkRuntime.kt` — template for sinks

**Variants needed** (8 total: 2-3 inputs x 0-3 outputs):
- `In2AnyOut0Runtime` (sink), `In2AnyOut1Runtime`, `In2AnyOut2Runtime`, `In2AnyOut3Runtime`
- `In3AnyOut0Runtime` (sink), `In3AnyOut1Runtime`, `In3AnyOut2Runtime`, `In3AnyOut3Runtime`

Note: Sink variants (0 outputs) follow `In2AnySinkRuntime` / `In3AnySinkRuntime` naming per spec.

## R3: Type Alias System

**Decision**: Add parallel type aliases for any-input process/tick blocks in `ContinuousTypes.kt`.

**Rationale**: The existing system defines type aliases for each port combination (e.g., `In2Out1ProcessBlock<A, B, R>`, `In2Out1TickBlock<A, B, R>`). The any-input variants use the SAME function signature since all input values are always provided (the difference is in the runtime behavior, not the block signature). However, separate type aliases improve clarity:
- `In2AnyOut1ProcessBlock<A, B, R>` — same as `suspend (A, B) -> R`
- `In2AnyOut1TickBlock<A, B, R>` — same as `suspend (A, B) -> R`
- `In2AnySinkBlock<A, B>` — same as `suspend (A, B) -> Unit`

These are technically identical signatures but provide semantic distinction for documentation and code generation.

## R4: Generic Type System Integration

**Decision**: Extend `_genericType` format from `"inXoutY"` to support `"inXanyoutY"` variant.

**Rationale**: The `GenericNodeTypeFactory.createGenericNodeType()` stores `_genericType` in `defaultConfiguration` (line 142: `put("_genericType", "in${numInputs}out${numOutputs}")`). The any-input variant uses `"in${numInputs}anyout${numOutputs}"` format. This propagates through:
1. `CustomNodeDefinition.genericType` field — stored in persistence
2. `NodeGeneratorPanelState.genericType` computed property — shown in UI preview
3. `RuntimeTypeResolver` — maps to correct factory method/runtime class names

## R5: RuntimeTypeResolver Integration

**Decision**: Add `anyInput: Boolean` parameter to `RuntimeTypeResolver` methods to select any-input variants.

**Rationale**: `RuntimeTypeResolver` currently maps `(inputCount, outputCount)` to factory method names, runtime type names, and tick param names via `when` blocks. Two approaches:
1. **Add anyInput parameter**: Clean, explicit. Each method gains an `anyInput: Boolean = false` parameter. When true, returns any-input variants.
2. **Read from CodeNode configuration**: Look up `_genericType` from node config. Rejected — couples resolver to model details.

The anyInput flag can be derived from the CodeNode's configuration `_genericType` field (contains "any") at the call site, keeping the resolver's API clean.

## R6: CustomNodeDefinition Persistence

**Decision**: Add `anyInput: Boolean = false` field to `CustomNodeDefinition`.

**Rationale**: The `CustomNodeDefinition` is a `@Serializable` data class persisted as JSON. Adding a field with a default value (`false`) maintains backward compatibility — existing serialized nodes without the field will deserialize with `anyInput = false`. The `genericType` computation in `create()` factory method changes from `"in${inputCount}out${outputCount}"` to conditionally include `"any"`.

## R7: Node Generator UI Pattern

**Decision**: Add a `Switch` (toggle) composable to `NodeGeneratorPanel` that is conditionally visible when `inputCount >= 2`.

**Rationale**: The existing panel uses `OutlinedButton` dropdowns for port counts and an `OutlinedTextField` for the name. A `Switch` composable from Compose Material fits naturally for a boolean toggle. The switch:
- Is only visible when `inputCount >= 2` (any-input is meaningless with < 2 inputs)
- Auto-disables (turns OFF) when `inputCount` is reduced below 2
- Updates the `genericType` preview in real-time

## R8: Code Generator Changes

**Decision**: Thread the `anyInput` flag through `RuntimeTypeResolver`, `RuntimeFlowGenerator`, and `ProcessingLogicStubGenerator`.

**Rationale**: The code generators need to produce correct class names, factory calls, and tick type aliases for any-input nodes:
- `RuntimeFlowGenerator.generate()` already uses `runtimeTypeResolver.getFactoryMethodName(node)`. The resolver needs the anyInput flag (from node's `_genericType` configuration).
- `ProcessingLogicStubGenerator.getTickTypeAlias()` maps port counts to tick type aliases. Needs parallel any-input variants.
- `RuntimeTypeResolver` methods all need the anyInput flag to return `createIn2AnyOut1Processor` vs `createIn2Out1Processor`.
