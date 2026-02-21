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

## R2: Direct Runtime Instantiation (No Component Classes)

**Decision**: The generated `{FlowName}Flow` class directly creates runtime instances (e.g., `Out2GeneratorRuntime<Int, Int>`, `In2SinkRuntime<Int, Int>`) using `CodeNodeFactory` methods, passing the user's tick function vals from the `usecases.logicmethods` package. Hand-written Component classes (like `TimerEmitterComponent`, `DisplayReceiverComponent`) are eliminated — the generated code replaces them entirely.

**Rationale**: The existing StopWatch Component classes are essentially boilerplate wrappers around runtime instances. Their responsibilities can be fully automated:
1. Runtime creation → generated via `CodeNodeFactory.create*()` with tick function reference
2. Channel exposure → runtime instances already expose channel properties directly
3. Observable StateFlows → generated Flow class creates `MutableStateFlow` properties for sink input ports
4. Lifecycle delegation → runtime `start()`/`stop()` called directly

**How it works**:

For generators (e.g., TimerEmitter with 0 inputs, 2 outputs):
```kotlin
// Flow imports the user's tick val from usecases.logicmethods
import io.codenode.stopwatch2.usecases.logicmethods.timerEmitterTick

// Flow creates runtime directly with the tick function
internal val timerEmitter = CodeNodeFactory.createTimedOut2Generator<Int, Int>(
    name = "TimerEmitter",
    tickIntervalMs = 1000L,
    tick = timerEmitterTick
)
```

For sinks with observable state (e.g., DisplayReceiver with 2 inputs, 0 outputs):
```kotlin
import io.codenode.stopwatch2.usecases.logicmethods.displayReceiverTick

// Flow owns the observable state
private val _seconds = MutableStateFlow(0)
val secondsFlow: StateFlow<Int> = _seconds.asStateFlow()
private val _minutes = MutableStateFlow(0)
val minutesFlow: StateFlow<Int> = _minutes.asStateFlow()

// Sink's consume block wraps user tick + updates StateFlows
internal val displayReceiver = CodeNodeFactory.createIn2Sink<Int, Int>(
    name = "DisplayReceiver",
    consume = { seconds, minutes ->
        _seconds.value = seconds
        _minutes.value = minutes
        displayReceiverTick(seconds, minutes)
    }
)
```

**Key implications**:
- No `{NodeName}Component` classes needed — the generated Flow class handles everything
- User-written tick stubs remain in `usecases.logicmethods/` as the only user-editable code
- Observable state (MutableStateFlows) is owned by the Flow class for sink input ports
- The Controller reads StateFlows from `flow.secondsFlow`, `flow.minutesFlow` etc.
- `reset()` on the Controller zeroes the Flow's MutableStateFlows

**Factory method selection**:
- Generators: `createTimedOut2Generator` (tick-based, default 1000ms interval)
- Sinks: `createIn2Sink` (event-driven, receives from channels)
- Transformers/Processors: `createContinuousTransformer`, `createIn2Out1Processor`, etc.

Note: The tick interval for generators defaults to 1000ms. This could later be made configurable via node configuration properties.

## R3: Channel Property Naming Convention

**Decision**: Follow the established runtime convention for channel property names in generated wiring code.

| Channels | Property Names |
|----------|---------------|
| 1 input | `inputChannel` |
| 2 inputs | `inputChannel1`, `inputChannel2` |
| 3 inputs | `inputChannel1`, `inputChannel2`, `inputChannel3` |
| 1 output | `outputChannel` |
| 2 outputs | `outputChannel1`, `outputChannel2` |
| 3 outputs | `outputChannel1`, `outputChannel2`, `outputChannel3` |

**Rationale**: These names are established by the runtime class hierarchy (`Out2GeneratorRuntime`, `In2SinkRuntime`, etc.). Single-input sinks use `inputChannel`; multi-input sinks use numbered properties (`inputChannel1`, `inputChannel2`). Single-output generators use `outputChannel`; multi-output generators use numbered properties (`outputChannel1`, `outputChannel2`).

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

**Decision**: The generated Flow class wires connections by mapping FlowGraph connections to channel property assignments on the runtime instances directly. For each connection:
1. Resolve source node → source runtime instance variable name (camelCase of node name)
2. Resolve source port → determine output channel property name based on port index within the runtime
3. Resolve target node → target runtime instance variable name
4. Resolve target port → determine input channel property name based on port index within the runtime
5. Generate: `{targetRuntime}.inputChannel{N} = {sourceRuntime}.outputChannel{N}`

**Rationale**: Runtime instances (e.g., `Out2GeneratorRuntime`, `In2SinkRuntime`) expose channel properties directly:
```kotlin
// Generated wiring - runtime-to-runtime, no Component wrapper
displayReceiver.inputChannel1 = timerEmitter.outputChannel1
displayReceiver.inputChannel2 = timerEmitter.outputChannel2
```

**Channel property names on runtime classes**:
- `GeneratorRuntime<T>`: `outputChannel`
- `Out2GeneratorRuntime<U, V>`: `outputChannel1`, `outputChannel2`
- `Out3GeneratorRuntime<U, V, W>`: `outputChannel1`, `outputChannel2`, `outputChannel3`
- `SinkRuntime<T>`: `inputChannel`
- `In2SinkRuntime<A, B>`: `inputChannel1`, `inputChannel2`
- `In3SinkRuntime<A, B, C>`: `inputChannel1`, `inputChannel2`, `inputChannel3`
