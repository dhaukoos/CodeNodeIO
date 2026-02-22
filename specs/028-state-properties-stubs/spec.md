# Feature Specification: State Properties Stubs for Node Runtime Generation

**Feature Branch**: `028-state-properties-stubs`
**Created**: 2026-02-21
**Status**: Draft
**Input**: User description: "StopWatch4 to replicate StopWatch - Generate state property stubs for nodes so processing logic can access per-node state"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - State Property Stubs Generated per Node (Priority: P1)

When the graphEditor compiles a FlowGraph, each node should receive a generated state properties file containing MutableStateFlow properties derived from the node's input and output ports. These properties provide per-node state that processing logic tick functions can read and update, enabling stateful behavior (e.g., a timer emitter that tracks elapsed seconds/minutes across ticks).

**Why this priority**: Without per-node state properties, processing logic stubs reference variables that don't exist in their scope. This is the fundamental blocker preventing generated code from being functional.

**Independent Test**: Can be tested by compiling a FlowGraph with a generator node that has two output ports, verifying a state properties file is generated with matching MutableStateFlow properties, and confirming the processing logic stub can reference those properties.

**Acceptance Scenarios**:

1. **Given** a FlowGraph with a generator node having outputs `elapsedSeconds: Int` and `elapsedMinutes: Int`, **When** the user compiles the module, **Then** a state properties file is generated in the `stateProperties/` directory containing `_elapsedSeconds: MutableStateFlow<Int>` and `_elapsedMinutes: MutableStateFlow<Int>` with public StateFlow accessors.
2. **Given** a FlowGraph with a sink node having inputs `seconds: Int` and `minutes: Int`, **When** the user compiles the module, **Then** a state properties file is generated containing `_seconds: MutableStateFlow<Int>` and `_minutes: MutableStateFlow<Int>`.
3. **Given** a node with no ports (0 inputs, 0 outputs), **When** the user compiles the module, **Then** no state properties file is generated for that node.

---

### User Story 2 - Processing Logic Stubs Import State Properties (Priority: P1)

Processing logic stub files must import and reference the state properties from their corresponding node's state properties file. The tick function body should use the state properties by default (e.g., `_elapsedSeconds.value + 1`) so the generated stubs compile and demonstrate correct usage patterns.

**Why this priority**: The processing logic stubs currently reference undefined variables, making the generated code non-compilable. This is a co-requisite with User Story 1.

**Independent Test**: Can be tested by generating stubs for a two-output generator node and verifying the processing logic file imports the state properties and the tick body references them correctly.

**Acceptance Scenarios**:

1. **Given** a generator node with two Int outputs (`elapsedSeconds`, `elapsedMinutes`), **When** the processing logic stub is generated, **Then** the stub imports the corresponding state properties and the tick body reads/updates `_elapsedSeconds.value` and `_elapsedMinutes.value`.
2. **Given** a sink node with two Int inputs (`seconds`, `minutes`), **When** the processing logic stub is generated, **Then** the stub imports the state properties and the tick body writes received values to `_seconds.value` and `_minutes.value`.
3. **Given** a transformer node with one String input and one Int output, **When** the processing logic stub is generated, **Then** the stub imports both input and output state properties.

---

### User Story 3 - State Properties Preserved on Re-compile (Priority: P2)

State property files are user-editable (developers may add custom state beyond the defaults). When a module is re-compiled, existing state property files must not be overwritten, matching the behavior of processing logic stubs.

**Why this priority**: Protects developer customizations during iterative development, but only matters once the initial generation works.

**Independent Test**: Can be tested by compiling, modifying a state properties file, re-compiling, and verifying the modifications are preserved.

**Acceptance Scenarios**:

1. **Given** a compiled module with a state properties file that the developer has modified, **When** the module is re-compiled, **Then** the modified state properties file is not overwritten.
2. **Given** a FlowGraph where a new node was added since the last compile, **When** the module is re-compiled, **Then** only the new node gets a fresh state properties file; existing ones are preserved.

---

### User Story 4 - Runtime Flow Imports State Properties (Priority: P1)

The generated Flow class (e.g., `StopWatch4Flow.kt`) must import and instantiate the state properties objects so that the tick functions passed to the runtime factory methods can access node state. The Flow class wires the state properties into the runtime lifecycle.

**Why this priority**: Even if state property files exist, they must be integrated into the runtime flow for the system to function.

**Independent Test**: Can be tested by verifying the generated Flow class imports state property objects and that the runtime instances reference them correctly.

**Acceptance Scenarios**:

1. **Given** a FlowGraph with a generator node and a sink node, **When** the Flow class is generated, **Then** it imports the state properties objects for each node and makes them accessible to the tick functions.
2. **Given** a FlowGraph with a sink node that has observable state, **When** the Flow class is generated, **Then** the sink's observable StateFlow properties are delegated from the state properties object (not duplicated in the Flow class).

---

### Edge Cases

- What happens when a node has ports with non-primitive types (e.g., custom data classes)? State properties should use a reasonable default value or `TODO()`.
- What happens when a node has ports whose types don't have obvious defaults (e.g., `Any`)? The default should be `TODO("Provide initial value")`.
- How does the system handle orphaned state property files when a node is removed? Same behavior as processing logic stubs: warn but don't delete.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The compile operation MUST generate a state properties file for each CodeNode that has at least one input or output port, placed in a `stateProperties/` sub-package directory.
- **FR-002**: Each state properties file MUST contain a Kotlin object with a private `MutableStateFlow` property and a public `StateFlow` accessor for each port on the node.
- **FR-003**: MutableStateFlow properties MUST be initialized with type-appropriate defaults (0 for Int, "" for String, false for Boolean, 0L for Long, 0.0 for Double, 0.0f for Float, `TODO()` for unknown types).
- **FR-004**: State properties naming MUST follow the pattern: `_{portName}` for the mutable property and `{portName}Flow` for the public accessor.
- **FR-005**: Processing logic stub files MUST import their corresponding state properties object so tick functions can reference `_portName.value`.
- **FR-006**: The generated Flow class MUST import and reference state properties objects so tick functions have access to node state at runtime.
- **FR-007**: State properties files MUST NOT be overwritten on re-compile if they already exist, preserving developer modifications.
- **FR-008**: The compile operation MUST detect orphaned state properties files (for removed nodes) and include them in the warnings list.
- **FR-009**: State properties files MUST include a `reset()` method that resets all MutableStateFlow properties to their initial defaults.

### Key Entities

- **StateProperties object**: A Kotlin object per node containing MutableStateFlow/StateFlow pairs for each port, with a reset() method. Lives in the `stateProperties/` sub-package.
- **ProcessingLogic stub**: Existing tick function stub that will now import and reference the corresponding StateProperties object.
- **Flow class**: The generated runtime orchestrator that instantiates and wires state properties into the runtime lifecycle.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A compiled FlowGraph matching the StopWatch topology (2-output generator + 2-input sink with connections) produces state properties files, processing logic stubs, and a Flow class that together form compilable code with no undefined variable references.
- **SC-002**: All generated state properties files contain the correct number of MutableStateFlow/StateFlow pairs matching the node's port count and types.
- **SC-003**: Re-compiling a module preserves 100% of developer-modified state properties files while generating files for any newly added nodes.
- **SC-004**: The generated code for a StopWatch-equivalent FlowGraph can be compiled without errors, closing the gap between graphEditor output and the original hand-crafted StopWatch module.

## Assumptions

- State properties are per-node (not per-flow), matching the original StopWatch architecture where each component owned its own state.
- The `stateProperties/` directory follows the same pattern as `processingLogic/` — files are generated once and then owned by the developer.
- The tick interval for timed generators (e.g., 1000ms) is already handled by the existing `tickIntervalMs` parameter in the runtime factory; this feature does not address making it configurable from DSL config (that can be a separate feature).
- Port names from the FlowGraph DSL are used directly as property names in state properties files (converted to camelCase).
