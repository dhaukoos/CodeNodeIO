# Data Model: Any-Input Trigger Mode

**Feature**: 035-any-input-trigger
**Date**: 2026-03-01

## Entity Changes

### CustomNodeDefinition (modified)

**File**: `graphEditor/src/jvmMain/kotlin/repository/CustomNodeDefinition.kt`

| Field | Type | Default | Change | Description |
|-------|------|---------|--------|-------------|
| id | String | auto-generated | existing | Unique identifier |
| name | String | — | existing | User-provided display name |
| inputCount | Int | — | existing | Number of input ports (0-3) |
| outputCount | Int | — | existing | Number of output ports (0-3) |
| genericType | String | computed | existing | Type string, now supports "inXanyoutY" |
| anyInput | Boolean | false | **NEW** | Whether to use any-input trigger mode |
| createdAt | Long | auto-generated | existing | Creation timestamp |

**Behavior change**: `create()` factory method computes `genericType` as:
- `anyInput = false`: `"in${inputCount}out${outputCount}"` (unchanged)
- `anyInput = true`: `"in${inputCount}anyout${outputCount}"`

**Serialization**: New field has default value `false`, maintaining backward compatibility.

### NodeGeneratorPanelState (modified)

**File**: `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt`

| Field | Type | Default | Change | Description |
|-------|------|---------|--------|-------------|
| anyInput | Boolean | false | **NEW** | Whether "Any Input" toggle is ON |

**Computed properties affected**:
- `genericType`: Changes from `"in${inputCount}out${outputCount}"` to include "any" when `anyInput = true`
- `showAnyInputToggle` (new): `inputCount >= 2` — controls UI visibility

## New Runtime Classes (8 total)

All in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/`

### In2AnyOut1Runtime<A, B, R>

| Property | Type | Description |
|----------|------|-------------|
| inputChannel1 | ReceiveChannel<A>? | First input channel |
| inputChannel2 | ReceiveChannel<B>? | Second input channel |
| outputChannel | SendChannel<R>? | Output channel |
| lastValue1 | A | Cached last value from input 1 (initialized to type default) |
| lastValue2 | B | Cached last value from input 2 (initialized to type default) |

**Behavior**: Uses `select` expression. Fires process block on ANY input receive. Provides cached values for non-triggered inputs.

### Naming Pattern for All 8 Variants

| Inputs | Outputs | Class Name | Process Block Type |
|--------|---------|-----------|-------------------|
| 2 | 0 | In2AnySinkRuntime<A, B> | In2AnySinkBlock<A, B> |
| 2 | 1 | In2AnyOut1Runtime<A, B, R> | In2AnyOut1ProcessBlock<A, B, R> |
| 2 | 2 | In2AnyOut2Runtime<A, B, U, V> | In2AnyOut2ProcessBlock<A, B, U, V> |
| 2 | 3 | In2AnyOut3Runtime<A, B, U, V, W> | In2AnyOut3ProcessBlock<A, B, U, V, W> |
| 3 | 0 | In3AnySinkRuntime<A, B, C> | In3AnySinkBlock<A, B, C> |
| 3 | 1 | In3AnyOut1Runtime<A, B, C, R> | In3AnyOut1ProcessBlock<A, B, C, R> |
| 3 | 2 | In3AnyOut2Runtime<A, B, C, U, V> | In3AnyOut2ProcessBlock<A, B, C, U, V> |
| 3 | 3 | In3AnyOut3Runtime<A, B, C, U, V, W> | In3AnyOut3ProcessBlock<A, B, C, U, V, W> |

## New Type Aliases

All in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/ContinuousTypes.kt`

### Process Block Type Aliases (8)

Same function signatures as all-input variants but semantically distinct for code generation.

### Tick Block Type Aliases (8)

| Alias | Signature |
|-------|-----------|
| In2AnyOut1TickBlock<A, B, R> | suspend (A, B) -> R |
| In2AnyOut2TickBlock<A, B, U, V> | suspend (A, B) -> ProcessResult2<U, V> |
| In2AnyOut3TickBlock<A, B, U, V, W> | suspend (A, B) -> ProcessResult3<U, V, W> |
| In2AnySinkTickBlock<A, B> | suspend (A, B) -> Unit |
| In3AnyOut1TickBlock<A, B, C, R> | suspend (A, B, C) -> R |
| In3AnyOut2TickBlock<A, B, C, U, V> | suspend (A, B, C) -> ProcessResult2<U, V> |
| In3AnyOut3TickBlock<A, B, C, U, V, W> | suspend (A, B, C) -> ProcessResult3<U, V, W> |
| In3AnySinkTickBlock<A, B, C> | suspend (A, B, C) -> Unit |

## Relationships

```
CustomNodeDefinition --[anyInput]--> genericType computation
                                         |
NodeGeneratorPanelState --[anyInput]--> genericType preview in UI
                                         |
GenericNodeTypeFactory --[_genericType]--> NodeTypeDefinition.defaultConfiguration
                                         |
RuntimeTypeResolver --[anyInput flag]--> factory method name, runtime type, tick param
                                         |
                            ┌────────────┴────────────┐
                    RuntimeFlowGenerator    ProcessingLogicStubGenerator
                    (factory calls)         (tick type aliases)
```
