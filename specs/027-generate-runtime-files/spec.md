# Feature Specification: Generate Runtime Files from FlowGraph Compilation

**Feature Branch**: `027-generate-runtime-files`
**Created**: 2026-02-20
**Status**: Draft
**Input**: User description: "StopWatch2 built from graphEditor. I just created a simple flowGraph in the graphEditor to match that of the StopWatch module, and named it StopWatch2. Then I created the StopWatch2 module from the compile option of the graphEditor. We need to update the compile option to also generate the equivalent files for StopWatchController, StopWatchControllerAdapter, StopWatchControllerInterface, StopWatchFlow, and StopWatchViewModel."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate Flow Orchestrator (Priority: P1)

When a user compiles a FlowGraph from the graphEditor, the system generates a `{FlowName}Flow` class that directly creates runtime instances (e.g., `Out2GeneratorRuntime<Int, Int>`) using `CodeNodeFactory` methods, passing the user's tick function vals from the stub files. Connections are wired via typed channel assignments between runtime instances. The Flow class also owns `MutableStateFlow` properties for sink node input ports (observable state). This eliminates the need for hand-written Component classes — the generated code replaces them entirely.

**Why this priority**: The Flow class is the core runtime orchestrator. Without it, the Controller, Adapter, Interface, and ViewModel have nothing to manage. It directly wires the FlowGraph model into executable channel-based code.

**Independent Test**: Can be fully tested by compiling any FlowGraph and verifying the generated `{FlowName}Flow.kt` file contains correct runtime instantiation via `CodeNodeFactory`, tick function imports from `usecases.logicmethods`, channel wiring matching the FlowGraph connections, MutableStateFlow properties for sink ports, and `start`/`stop`/`reset` methods.

**Acceptance Scenarios**:

1. **Given** a FlowGraph with 2 nodes and 2 connections, **When** the user compiles via the graphEditor, **Then** a `{FlowName}Flow.kt` file is generated in the `generated` package containing direct runtime instances created via `CodeNodeFactory`, tick function imports from `usecases.logicmethods`, and `wireConnections()` matching the FlowGraph connections.
2. **Given** a FlowGraph with nodes of varying types (generator, sink, transformer), **When** compiled, **Then** the Flow class creates the correct runtime type for each node (e.g., `Out2GeneratorRuntime`, `In2SinkRuntime`) and wires output channels to input channels respecting each runtime's channel property names.
3. **Given** a FlowGraph with sink nodes, **When** compiled, **Then** the Flow class creates `MutableStateFlow` properties for each sink input port and wraps the user's tick function to update them.
4. **Given** a FlowGraph with no connections, **When** compiled, **Then** the Flow class contains runtime instances but `wireConnections()` has no wiring code.

---

### User Story 2 - Generate Controller with Observable State (Priority: P2)

When a user compiles a FlowGraph, the system generates a `{FlowName}Controller` class that wraps the Flow orchestrator with execution control (start, stop, pause, resume, reset) and exposes observable state derived from sink node input ports as `StateFlow` properties.

**Why this priority**: The Controller provides the execution lifecycle and observable state that UI layers need. It bridges the Flow runtime to the rest of the MVVM stack.

**Independent Test**: Can be tested by compiling a FlowGraph and verifying the generated `{FlowName}Controller.kt` contains a `{FlowName}Flow` instance, `start()`/`stop()`/`pause()`/`resume()`/`reset()` methods, `executionState: StateFlow<ExecutionState>`, and one `StateFlow` property per sink node input port named after the port.

**Acceptance Scenarios**:

1. **Given** a FlowGraph with a sink node having 2 input ports ("seconds" of type Int, "minutes" of type Int), **When** compiled, **Then** the Controller exposes `val seconds: StateFlow<Int>` and `val minutes: StateFlow<Int>` derived from the sink's component state.
2. **Given** a FlowGraph, **When** compiled, **Then** the Controller contains `start()`, `stop()`, `pause()`, `resume()`, `reset()` methods that delegate to `RootControlNode` and `RuntimeRegistry`.
3. **Given** a FlowGraph, **When** compiled, **Then** the Controller exposes `val executionState: StateFlow<ExecutionState>`.

---

### User Story 3 - Generate Controller Interface (Priority: P3)

When a user compiles a FlowGraph, the system generates a `{FlowName}ControllerInterface` that abstracts the Controller's public API (control methods + observable state properties). This enables dependency injection and test fakes.

**Why this priority**: The interface enables testability of the ViewModel layer by allowing test doubles to replace the real controller.

**Independent Test**: Can be tested by verifying the generated interface file declares all the same control methods and StateFlow properties as the Controller, and that the Controller's adapter implements it.

**Acceptance Scenarios**:

1. **Given** a FlowGraph with observable state properties (sink input ports), **When** compiled, **Then** the generated interface declares each StateFlow property and all control methods (`start`, `stop`, `pause`, `resume`, `reset`).
2. **Given** any FlowGraph, **When** compiled, **Then** the generated interface includes `val executionState: StateFlow<ExecutionState>`.

---

### User Story 4 - Generate Controller Adapter (Priority: P4)

When a user compiles a FlowGraph, the system generates a `{FlowName}ControllerAdapter` that wraps the concrete Controller and implements the Controller Interface. This adapter pattern enables the ViewModel to depend on the interface abstraction.

**Why this priority**: The adapter completes the dependency inversion pattern, allowing the ViewModel to work with both real and test controllers.

**Independent Test**: Can be tested by verifying the generated adapter class takes the Controller as a constructor parameter, implements the Interface, and delegates all properties and methods.

**Acceptance Scenarios**:

1. **Given** a compiled FlowGraph, **When** the adapter is generated, **Then** it takes `{FlowName}Controller` as a constructor parameter and implements `{FlowName}ControllerInterface`.
2. **Given** a compiled FlowGraph, **When** the adapter is generated, **Then** every interface property and method delegates to the underlying controller.

---

### User Story 5 - Generate ViewModel (Priority: P5)

When a user compiles a FlowGraph, the system generates a `{FlowName}ViewModel` class extending `androidx.lifecycle.ViewModel` that takes the Controller Interface as a constructor parameter and delegates all state and actions.

**Why this priority**: The ViewModel is the final layer connecting runtime state to UI. It depends on all other generated files being correct.

**Independent Test**: Can be tested by verifying the generated ViewModel extends `ViewModel`, takes the interface as parameter, and delegates all properties and methods.

**Acceptance Scenarios**:

1. **Given** a compiled FlowGraph, **When** the ViewModel is generated, **Then** it extends `androidx.lifecycle.ViewModel`, takes `{FlowName}ControllerInterface` as constructor parameter, and delegates all StateFlow properties and control methods.
2. **Given** a FlowGraph with sink node ports "seconds" (Int) and "minutes" (Int), **When** compiled, **Then** the ViewModel exposes `val seconds: StateFlow<Int>` and `val minutes: StateFlow<Int>` delegated from the controller interface.

---

### Edge Cases

- What happens when a FlowGraph has no sink nodes? The Controller, Interface, Adapter, and ViewModel are generated with only `executionState` as observable state (no port-derived StateFlow properties).
- What happens when a FlowGraph has multiple sink nodes? Observable state properties are derived from all sink nodes' input ports, with port names used as property names. If two sink nodes have ports with the same name, the names are disambiguated by prefixing with the node name (e.g., `displayReceiverSeconds`).
- What happens when a sink node input port has a type that cannot be mapped to a Kotlin type? The port type defaults to `Any`.
- What happens when the generated file already exists in the output directory? Generated files in the `generated` package are always overwritten (they are fully generated, not user-editable).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST generate a `{FlowName}Flow.kt` file in the `generated` package that directly creates runtime instances via `CodeNodeFactory`, imports tick function vals from stub files, wires connections via typed channels, and owns `MutableStateFlow` properties for sink node input ports.
- **FR-002**: System MUST generate a `{FlowName}Controller.kt` file in the `generated` package with execution control methods (`start`, `stop`, `pause`, `resume`, `reset`) and observable `StateFlow` properties.
- **FR-003**: System MUST derive observable state properties from sink node input ports — using port names as property names and port IP types as StateFlow generic types.
- **FR-004**: System MUST generate a `{FlowName}ControllerInterface.kt` file declaring the same control methods and StateFlow properties as the Controller.
- **FR-005**: System MUST generate a `{FlowName}ControllerAdapter.kt` file that wraps the Controller and implements the Interface via delegation.
- **FR-006**: System MUST generate a `{FlowName}ViewModel.kt` file extending `ViewModel` that takes the Interface as constructor parameter and delegates all state and actions.
- **FR-007**: System MUST always overwrite files in the `generated` package on each compile (generated code is not user-editable).
- **FR-008**: System MUST include the `executionState: StateFlow<ExecutionState>` property in the Controller, Interface, Adapter, and ViewModel regardless of FlowGraph structure.
- **FR-009**: System MUST use the FlowGraph name (PascalCase) as the prefix for all generated class names.
- **FR-010**: System MUST generate all five runtime files as part of the existing compile/save action (no separate menu item needed).

### Key Entities

- **Flow Orchestrator**: Instantiates components, wires connections, provides start/stop lifecycle. One per FlowGraph.
- **Controller**: Wraps Flow with execution control and observable state. Uses RootControlNode and RuntimeRegistry.
- **Controller Interface**: Abstraction defining control methods and observable state properties for dependency injection.
- **Controller Adapter**: Bridges concrete Controller to Interface via delegation pattern.
- **ViewModel**: UI-facing facade extending AndroidX ViewModel, delegates to Controller Interface.

## Assumptions

- The existing `ModuleSaveService` compile pipeline will be extended to call the new generators alongside the existing `FlowKtGenerator` and `ProcessingLogicStubGenerator`.
- Generator classes will be added to the `kotlinCompiler` module following the pattern of `FlowKtGenerator` and `ProcessingLogicStubGenerator`.
- The `generated` package is always overwritten; only the `usecases` package preserves user files.
- Observable state discovery uses sink node input ports: the port name becomes the StateFlow property name, and the port's IP type becomes the generic type parameter.
- All generated files go into the `generated` subpackage within the module's base package.
- The Controller's `bindToLifecycle()` method is included as a standard part of the generated controller (matching the StopWatch reference implementation).
- The generated Flow class directly creates runtime instances via `CodeNodeFactory` — no hand-written Component classes are needed. The user's only editable code is the tick function vals in `usecases.logicmethods/`.
- Generator nodes use `createTimedOut2Generator` (etc.) with a default tick interval of 1000ms. This can be made configurable in a future iteration.
- The sink consume block wraps the user's tick function to also update MutableStateFlows for observable state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After compiling any FlowGraph from the graphEditor, all five runtime files (`Flow`, `Controller`, `ControllerInterface`, `ControllerAdapter`, `ViewModel`) are present in the generated package directory.
- **SC-002**: Generated files compile without errors when the module is built.
- **SC-003**: The generated Controller exposes one `StateFlow` property per sink node input port, plus `executionState`.
- **SC-004**: The generated ViewModel, Interface, and Adapter expose the same set of properties and control methods as the Controller.
- **SC-005**: Re-compiling the same FlowGraph overwrites all generated files without errors or stale artifacts.
