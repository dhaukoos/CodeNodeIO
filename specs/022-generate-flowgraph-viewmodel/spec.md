# Feature Specification: Generate FlowGraph ViewModel

**Feature Branch**: `022-generate-flowgraph-viewmodel`
**Created**: 2026-02-19
**Status**: Draft
**Input**: User description: "Derive viewmodel classes from flowgraph module. Move the viewmodel classes for KMPMobileApp into the Stopwatch module. Then determine what is needed to generate these classes as part of the kotlinCompiler from the flowgraph definition, and identify and call out any needed yet undefined inputs to that process. The viewmodel will define the interface between the flowgraph file and the compose ui elements associated with it."

## Clarifications

### Session 2026-02-19

- Q: Observable state discovery strategy? → A: Use Alternative #2 - derive from sink node input ports. Use port names as interface property names. Port types will be updated to be selectable from IP types available in the graphEditor.

## User Scenarios & Testing

### User Story 1 - Move ViewModel to StopWatch Module (Priority: P1)

As a developer, I want the ViewModel classes (ViewModel, ControllerInterface, ControllerAdapter) to live inside the StopWatch module alongside the generated Controller and Flow classes, so that each flow module is self-contained and the consuming app (KMPMobileApp) only needs a dependency on the flow module to get both the domain logic and its UI bridge.

**Why this priority**: The ViewModel classes currently live in KMPMobileApp but are tightly coupled to the StopWatch module's generated Controller. Moving them into StopWatch is a prerequisite for code generation - the generator must know where to emit these files.

**Independent Test**: Can be verified by building the StopWatch module in isolation (the ViewModel, ControllerInterface, and ControllerAdapter compile within the module) and by building KMPMobileApp to confirm it can still reference these classes via the StopWatch dependency.

**Acceptance Scenarios**:

1. **Given** the StopWatch module, **When** it is compiled, **Then** it contains the ViewModel class, the ControllerInterface, and the ControllerAdapter in its output.
2. **Given** the KMPMobileApp module, **When** it references the StopWatch ViewModel, **Then** it resolves the class through its dependency on the StopWatch module without duplicating the ViewModel source.
3. **Given** the moved ViewModel files, **When** all existing tests are run, **Then** they pass with zero failures (moved test files compile and execute in the StopWatch test source set).

---

### User Story 2 - Generate ControllerInterface from FlowGraph (Priority: P2)

As a developer, I want the code generator to automatically produce a ControllerInterface from a FlowGraph definition, so that every flow module ships a testable interface contract without hand-writing boilerplate.

The generated ControllerInterface must expose:
- Observable state properties derived from the input ports of the FlowGraph's sink nodes. Each sink input port becomes a StateFlow property, using the port name as the property name and the port's data type (selected from IP types in the graphEditor) as the StateFlow type parameter.
- Standard lifecycle control methods (start, stop, pause, resume, reset) that are common to every flow.

**Why this priority**: The ControllerInterface is the contract that both the real Controller and test fakes implement. Generating it first establishes the shape of the API that the ViewModel and Adapter depend on.

**Independent Test**: Can be verified by running the code generator on the StopWatch FlowGraph and comparing the output to the hand-written ControllerInterface. The generated interface must compile and expose the same set of StateFlow properties and control methods.

**Acceptance Scenarios**:

1. **Given** a FlowGraph definition with sink nodes that have input ports, **When** the code generator runs, **Then** it produces a ControllerInterface file with one StateFlow property per input port on the sink nodes, using the port name as the property name and the port's data type as the StateFlow type parameter.
2. **Given** any FlowGraph definition, **When** the code generator runs, **Then** the generated ControllerInterface always includes the standard lifecycle methods: start, stop, pause, resume, reset.
3. **Given** the generated ControllerInterface, **When** it is compiled against the existing ViewModel and test fakes, **Then** the code compiles successfully with no manual edits.

---

### User Story 3 - Generate ControllerAdapter from FlowGraph (Priority: P3)

As a developer, I want the code generator to automatically produce a ControllerAdapter that wraps the generated Controller to implement the generated ControllerInterface, so that the wiring between generated code and the ViewModel is fully automated.

**Why this priority**: The Adapter is pure boilerplate - it delegates every interface member to the corresponding Controller member. Once the Interface (US2) and the Controller already exist, generating the Adapter eliminates the last piece of manual glue code.

**Independent Test**: Can be verified by compiling the generated Adapter against the generated ControllerInterface and the generated Controller. All interface members must be satisfied by delegation.

**Acceptance Scenarios**:

1. **Given** a generated ControllerInterface and an existing generated Controller, **When** the code generator runs, **Then** it produces a ControllerAdapter that implements every member of the ControllerInterface by delegating to the Controller.
2. **Given** the generated ControllerAdapter, **When** it is substituted for the hand-written adapter in KMPMobileApp, **Then** all existing ViewModel tests pass without modification.

---

### User Story 4 - Generate ViewModel from FlowGraph (Priority: P4)

As a developer, I want the code generator to automatically produce a ViewModel class from a FlowGraph definition, so that each flow module ships a ready-to-use Compose UI bridge with zero hand-written boilerplate.

The generated ViewModel must:
- Accept the ControllerInterface as a constructor dependency (enabling test fakes).
- Expose the same observable state properties as the ControllerInterface.
- Delegate all lifecycle actions to the ControllerInterface.

**Why this priority**: The ViewModel is the outermost layer consumed by Compose UI. It depends on all the previous stories being complete.

**Independent Test**: Can be verified by running the code generator on the StopWatch FlowGraph, compiling the generated ViewModel, and running the existing StopWatchViewModelTest against it.

**Acceptance Scenarios**:

1. **Given** a FlowGraph definition, **When** the code generator runs, **Then** it produces a ViewModel class that extends the platform ViewModel base class and accepts the ControllerInterface as a constructor parameter.
2. **Given** the generated ViewModel, **When** it replaces the hand-written StopWatchViewModel, **Then** all existing StopWatchViewModelTest tests pass without modification.

---

### User Story 5 - Identify Undefined Inputs for Generation (Priority: P5)

As a developer, I want the code generator to clearly report when a FlowGraph definition lacks the metadata needed to generate the ViewModel layer, so that I know exactly what additional information to provide in the flow definition.

Currently, the FlowGraph definition does not explicitly declare which node properties should be surfaced as observable state in the ViewModel. The generator needs a way to discover these. This story identifies and documents what metadata is missing and proposes how it should be supplied.

**Why this priority**: This is a design/analysis story that documents the gap between the current FlowGraph schema and what the generator needs. It informs future FlowGraph DSL enhancements but does not block the initial generation (US2-US4 can use reasonable conventions and existing node metadata).

**Independent Test**: Can be verified by reviewing the generated documentation listing all undefined inputs, along with the conventions or heuristics the generator uses as defaults.

**Acceptance Scenarios**:

1. **Given** the current FlowGraph schema, **When** the analysis is performed, **Then** a document is produced listing every piece of information the generator needs but cannot currently derive from the FlowGraph definition.
2. **Given** each identified gap, **When** the document is reviewed, **Then** it includes a recommended convention or DSL extension to fill the gap.

---

### Edge Cases

- What happens when a FlowGraph has zero sink nodes with observable state? The generator should produce a ViewModel with only lifecycle control methods and no state properties.
- What happens when a sink node has input ports that are not relevant to the UI (internal metrics, debug counters)? All sink input ports generate observable state properties by default. Filtering is a future enhancement identified by US5.
- What happens when the generated file already exists in the output directory? The generator should overwrite it (generated files are not hand-edited).
- What happens when the FlowGraph has multiple sink nodes, each with observable state? The generator should surface all observable state from all sink nodes, prefixed by the node name to avoid collisions.

## Requirements

### Functional Requirements

- **FR-001**: The StopWatch module MUST contain the ViewModel, ControllerInterface, and ControllerAdapter classes after the move, with no copies remaining in KMPMobileApp.
- **FR-002**: The code generator MUST produce a ControllerInterface with one StateFlow property per input port on the FlowGraph's sink nodes, using the port name as the property name and the port's data type as the StateFlow type parameter.
- **FR-003**: The code generator MUST include the standard lifecycle methods (start, stop, pause, resume, reset) on every generated ControllerInterface, regardless of the FlowGraph content.
- **FR-004**: The code generator MUST produce a ControllerAdapter that implements the generated ControllerInterface by delegating to the generated Controller.
- **FR-005**: The code generator MUST produce a ViewModel that accepts the ControllerInterface as a constructor dependency and delegates all state observation and actions to it.
- **FR-006**: The generated ViewModel MUST extend the platform ViewModel base class to integrate with the UI lifecycle.
- **FR-007**: All generated files MUST be placed in the module's generated package directory alongside the existing generated Controller and Flow files.
- **FR-008**: The code generator MUST produce a report listing any FlowGraph metadata it could not derive and the convention it used as a fallback.
- **FR-009**: When a FlowGraph has multiple sink nodes with observable state, the generator MUST prefix property names with the node name to avoid naming collisions.

### Key Entities

- **ControllerInterface**: The contract defining observable state properties and lifecycle control methods for a flow. Enables dependency injection and test fakes.
- **ControllerAdapter**: Wrapper that implements ControllerInterface by delegating to the generated Controller.
- **ViewModel**: The UI bridge that accepts a ControllerInterface and exposes observable state for UI binding. Extends the platform ViewModel base class.
- **Observable State Property**: A property derived from a sink node's input port. The port name becomes the property name (e.g., `seconds`, `minutes`), and the port's data type (selected from IP types in the graphEditor) becomes the StateFlow type parameter.

## Success Criteria

### Measurable Outcomes

- **SC-001**: After the move (US1), the StopWatch module compiles independently with zero errors and the KMPMobileApp module compiles with zero errors referencing the moved classes.
- **SC-002**: The generated ControllerInterface has one StateFlow property per sink input port (names and types match the port definitions) plus all standard lifecycle methods. The hand-written StopWatchControllerInterface and tests are updated to use port-derived names.
- **SC-003**: The generated ControllerAdapter, when substituted for the hand-written one, causes zero test failures in the existing test suite.
- **SC-004**: The generated ViewModel, when substituted for the hand-written one, causes zero test failures in the existing StopWatchViewModelTest.
- **SC-005**: The undefined-inputs report identifies all metadata gaps and provides a recommended convention for each, with zero gaps left undocumented.

## Assumptions

- The StopWatch module's build configuration already includes (or can easily add) the ViewModel base class dependency needed for the generated ViewModel. If not, this will be added as part of US1.
- The code generator already produces a Controller class with observable state properties and lifecycle methods. The new generation builds on this existing output.
- Observable state properties are derived from the input ports of sink nodes in the FlowGraph. Each sink input port's name becomes the ControllerInterface property name, and the port's data type (selectable from IP types in the graphEditor) provides the StateFlow type parameter. No source-file inspection is required.
- The standard lifecycle methods (start, stop, pause, resume, reset) are fixed and do not vary per FlowGraph. Every generated ControllerInterface includes them.
- Generated files are placed in the generated subdirectory and are expected to be overwritten on each generation run.
- The KMPMobileApp module will continue to depend on the StopWatch module, so the moved classes remain accessible.
