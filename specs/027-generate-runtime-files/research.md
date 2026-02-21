# Research: Generate Runtime Files from FlowGraph Compilation

**Feature**: 027-generate-runtime-files
**Date**: 2026-02-20

## R1: Observable State Discovery — Sink Node Input Ports

**Decision**: Derive observable state properties from sink node input ports. The port name becomes the `StateFlow` property name; the port's data type becomes the `StateFlow` generic type parameter.

**Rationale**: Sink nodes are the terminal consumers in a FlowGraph — they receive processed data that represents the flow's output state. Their input ports directly correspond to observable values that a UI would want to display. This matches the StopWatch reference where `DisplayReceiver`'s input ports ("seconds", "minutes") become `StateFlow<Int>` properties on the Controller.

**Alternatives considered**:
1. Hand-written interface properties — rejected because it requires manual specification and doesn't scale
2. Derive from all node output ports — rejected because intermediate outputs are implementation details, not observable state
3. Configuration-based approach — rejected as over-engineered for the current need

## R2: Component Class Reference Strategy

**Decision**: The generated `{FlowName}Flow` class instantiates `{NodeName}Component` classes from the `usecases` package. These component classes are the hand-written wrappers around runtime instances that users create to add business logic beyond the tick function stubs.

**Rationale**: The ProcessingLogicStubGenerator generates `{NodeName}ProcessLogic.kt` files containing tick val stubs (e.g., `timerEmitterTick`), but the actual component classes (`TimerEmitterComponent`, `DisplayReceiverComponent`) are hand-written by users. The generated Flow class will reference these component classes, following the same pattern as the StopWatch module.

**Important distinction**: The generated code currently does NOT generate component classes. Component classes are user-written wrappers that:
- Extend or compose a runtime (e.g., `Out2GeneratorRuntime<Int, Int>`)
- Expose channel properties for wiring
- Add domain-specific logic (StateFlows, reset behavior, etc.)

**For the initial implementation**: The Flow generator will assume component classes exist in the `usecases` package with the naming convention `{NodeName}Component`. If they don't exist yet, the generated code will have compilation errors that guide the user to create them.

## R3: Channel Property Naming Convention

**Decision**: Follow the established runtime convention for channel property names in generated wiring code.

| Channels | Property Names |
|----------|---------------|
| 1 input | `inputChannel` |
| 2 inputs | `inputChannel`, `inputChannel2` |
| 3 inputs | `inputChannel`, `inputChannel2`, `inputChannel3` |
| 1 output | `outputChannel1` (multi-output generators) or `outputChannel` (single) |
| 2 outputs | `outputChannel1`, `outputChannel2` |
| 3 outputs | `outputChannel1`, `outputChannel2`, `outputChannel3` |

**Rationale**: These names are established by the runtime class hierarchy (`Out2GeneratorRuntime`, `In2SinkRuntime`, etc.) and are used consistently in the StopWatch reference implementation.

## R4: Runtime Type Resolution — Port Count Mapping

**Decision**: Map `(inputPortCount, outputPortCount)` to runtime classes for determining which channels exist on each component.

| Inputs | Outputs | Runtime Type | Tick Type |
|--------|---------|-------------|-----------|
| 0 | 1 | `GeneratorRuntime<T>` | `GeneratorTickBlock<T>` |
| 0 | 2 | `Out2GeneratorRuntime<U, V>` | `Out2TickBlock<U, V>` |
| 0 | 3 | `Out3GeneratorRuntime<U, V, W>` | `Out3TickBlock<U, V, W>` |
| 1 | 0 | `SinkRuntime<T>` | `SinkTickBlock<T>` |
| 2 | 0 | `In2SinkRuntime<A, B>` | `In2SinkTickBlock<A, B>` |
| 3 | 0 | `In3SinkRuntime<A, B, C>` | `In3SinkTickBlock<A, B, C>` |
| 1 | 1 | `TransformerRuntime<TIn, TOut>` | `TransformerTickBlock<TIn, TOut>` |
| 2 | 1 | `In2Out1Runtime<A, B, R>` | `In2Out1TickBlock<A, B, R>` |
| 3 | 1 | `In3Out1Runtime<A, B, C, R>` | `In3Out1TickBlock<A, B, C, R>` |
| 1 | 2 | `In1Out2Runtime<A, U, V>` | `In1Out2TickBlock<A, U, V>` |
| 1 | 3 | `In1Out3Runtime<A, U, V, W>` | `In1Out3TickBlock<A, U, V, W>` |
| 2 | 2 | `In2Out2Runtime<A, B, U, V>` | `In2Out2TickBlock<A, B, U, V>` |
| 2 | 3 | `In2Out3Runtime<A, B, U, V, W>` | `In2Out3TickBlock<A, B, U, V, W>` |
| 3 | 2 | `In3Out2Runtime<A, B, C, U, V>` | `In3Out2TickBlock<A, B, C, U, V>` |
| 3 | 3 | `In3Out3Runtime<A, B, C, U, V, W>` | `In3Out3TickBlock<A, B, C, U, V, W>` |

**Rationale**: This mapping is already implemented in `ProcessingLogicStubGenerator.getTickTypeAlias()` and is the canonical source of truth for the node type system.

## R5: Port Name Disambiguation

**Decision**: When multiple sink nodes have input ports with the same name, disambiguate by prefixing with the node name in camelCase (e.g., `displayReceiverSeconds`, `loggerReceiverSeconds`). When port names are unique across all sink nodes, use the bare port name (e.g., `seconds`).

**Rationale**: Simple port names are more ergonomic for the common case (most flows have a single sink node). Disambiguation only kicks in for the edge case of name collisions.

## R6: Generator Class Architecture

**Decision**: Create 5 new generator classes in the `kotlinCompiler` module:

1. `RuntimeFlowGenerator` — generates `{Name}Flow.kt`
2. `RuntimeControllerGenerator` — generates `{Name}Controller.kt`
3. `RuntimeControllerInterfaceGenerator` — generates `{Name}ControllerInterface.kt`
4. `RuntimeControllerAdapterGenerator` — generates `{Name}ControllerAdapter.kt`
5. `RuntimeViewModelGenerator` — generates `{Name}ViewModel.kt`

Each follows the same pattern as `FlowKtGenerator`:
```kotlin
fun generate(flowGraph: FlowGraph, generatedPackage: String, usecasesPackage: String): String
```

**Rationale**: Follows existing generator separation pattern. Each file has distinct concerns and templates. String-based generation (like FlowKtGenerator) rather than KotlinPoet (like FlowGenerator) — simpler and more readable for template-style code.

## R7: ModuleSaveService Integration Point

**Decision**: Add runtime file generation calls in `ModuleSaveService.saveModule()` after the existing `.flow.kt` generation step. All 5 files are written to the `generated` package directory and are always overwritten.

**Rationale**: The ModuleSaveService already orchestrates all code generation. Adding 5 more generator calls follows the established pattern. The `generated` package is always overwritten (per FR-007), so no file-exists checks needed.

## R8: Connection Wiring Strategy

**Decision**: The generated Flow class wires connections by mapping FlowGraph connections to channel property assignments. For each connection:
1. Resolve source node → source component instance name
2. Resolve source port → determine output channel property name based on port index
3. Resolve target node → target component instance name
4. Resolve target port → determine input channel property name based on port index
5. Generate: `targetComponent.inputChannelN = sourceComponent.outputChannelN`

**Rationale**: This matches exactly how the StopWatch `StopWatchFlow.wireConnections()` works:
```kotlin
displayReceiver.inputChannel = timerEmitter.outputChannel1
displayReceiver.inputChannel2 = timerEmitter.outputChannel2
```
