# Research: State Properties Stubs

## R1: MutableStateFlow Visibility for Cross-File Access

**Decision**: Use `internal` visibility for `_portName` MutableStateFlow properties in state properties objects.

**Rationale**: The spec says "private MutableStateFlow property" (FR-002), but processing logic stubs are in separate files and need to reference `_portName.value`. In the hand-crafted StopWatch, the tick lambda is defined *within* the component class, so `private` works. In the generated architecture, ticks are top-level vals in separate files. Using `internal` allows same-module access while keeping the property hidden from external consumers.

**Alternatives considered**:
- `private` with nested tick definition inside object: Would break the current top-level val pattern for processing logic stubs
- `public`: Over-exposes mutable state to external modules
- `internal` (chosen): Standard Kotlin visibility for same-module access, matches how KMP modules scope their internals

## R2: State Properties File Naming Convention

**Decision**: `{NodeName}StateProperties.kt` containing `object {NodeName}StateProperties`.

**Rationale**: Follows the parallel naming convention with `{NodeName}ProcessLogic.kt`. Using a Kotlin `object` (singleton) ensures a single instance of state per node, matching the hand-crafted StopWatch architecture where each component owns one set of state flows.

**Alternatives considered**:
- Class with companion object: Unnecessary complexity; state properties are inherently singletons
- Top-level properties (no wrapper): Would pollute the package namespace and make imports ambiguous
- Object (chosen): Clean namespace, clear ownership, supports `reset()` method naturally

## R3: Which Ports Generate State Properties

**Decision**: ALL ports (both input and output) on each node generate state properties.

**Rationale**: In the hand-crafted StopWatch:
- TimerEmitterComponent (generator) owns `_elapsedSeconds` and `_elapsedMinutes` for its OUTPUT ports — the tick function reads/updates these to track elapsed time across ticks
- DisplayReceiverComponent (sink) owns `_seconds` and `_minutes` for its INPUT ports — the consume block writes received values to these for UI observation

Both input and output ports need state. A generator's tick needs to read previous output state. A sink's consume block stores received values.

**Alternatives considered**:
- Output ports only: Would miss sink state (needed for UI observation)
- Input ports only: Would miss generator state (needed for stateful tick logic)
- All ports (chosen): Matches hand-crafted architecture, covers all use cases

## R4: Flow Class Observable State Delegation

**Decision**: Flow class delegates `StateFlow` properties from sink node state properties objects instead of owning `MutableStateFlow` directly. Flow's `reset()` calls each state properties object's `reset()`.

**Rationale**: Currently `RuntimeFlowGenerator` creates `MutableStateFlow` properties in the Flow class and updates them in the sink consume block. With state properties objects, this state moves to `{SinkNodeName}StateProperties` and the Flow class delegates:
```kotlin
// Before (current):
private val _seconds = MutableStateFlow(0)
val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()

// After (with state properties):
val secondsFlow: StateFlow<Int> = DisplayReceiverStateProperties.secondsFlow
```

The sink consume block in the Flow class still updates the state, but via the state properties object:
```kotlin
consume = { seconds, minutes ->
    DisplayReceiverStateProperties._seconds.value = seconds
    DisplayReceiverStateProperties._minutes.value = minutes
    displayReceiverTick(seconds, minutes)
}
```

**Alternatives considered**:
- Keep MutableStateFlow in Flow, duplicate in state properties: Violates spec FR-006 ("not duplicated")
- Move all state management to state properties, no Flow references: Would break Controller/ViewModel delegation chain
- Delegation (chosen): Clean separation; state lives in one place, is accessible from multiple consumers

## R5: Default Value Strategy for MutableStateFlow Initialization

**Decision**: Use the existing `defaultForType()` pattern from `ProcessingLogicStubGenerator` and `ObservableStateResolver`, extended to handle unknown types with `TODO("Provide initial value for {TypeName}")`.

**Rationale**: Consistent with FR-003. The existing `defaultForType()` already handles Int→0, String→"", Boolean→false, Long→0L, Double→0.0, Float→0.0f. Unknown types use `TODO()` which fails at runtime with a clear message, prompting the developer to provide a proper initial value.

| Type | Default |
|------|---------|
| Int | `0` |
| Long | `0L` |
| Double | `0.0` |
| Float | `0.0f` |
| String | `""` |
| Boolean | `false` |
| Other | `TODO("Provide initial value for {TypeName}")` |

## R6: Processing Logic Stub Import Pattern

**Decision**: Processing logic stubs import the state properties object. The tick body can then reference properties via the object name (e.g., `TimerEmitterStateProperties._elapsedSeconds.value`), or the developer can add direct property imports.

**Rationale**: The generated stub imports the object so properties are accessible. The TODO body doesn't try to demonstrate usage of state properties — that's left to the developer. This keeps the stub simple and avoids generating incorrect logic.

**Generated stub example**:
```kotlin
package io.codenode.stopwatch4.processingLogic

import io.codenode.fbpdsl.runtime.Out2TickBlock
import io.codenode.fbpdsl.runtime.ProcessResult2
import io.codenode.stopwatch4.stateProperties.TimerEmitterStateProperties

val timerEmitterTick: Out2TickBlock<Int, Int> = {
    // TODO: Implement TimerEmitter tick logic
    ProcessResult2.both(0, 0)
}
```

## R7: Orphan Detection for stateProperties Directory

**Decision**: Follow the same pattern as `detectOrphanedComponents()` in ModuleSaveService. Scan `stateProperties/` for `*StateProperties.kt` files, compare against current FlowGraph nodes, warn about orphans.

**Rationale**: Matches existing behavior for processing logic stubs. Developers may have customized state property files, so orphans are warned but never deleted.
