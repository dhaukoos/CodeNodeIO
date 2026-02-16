# Feature Specification: Typed NodeRuntime Stubs

**Feature Branch**: `015-typed-node-runtime`
**Created**: 2026-02-15
**Status**: Draft
**Input**: User description: "Generate typed NodeRuntime stubs for FBP nodes with 0-3 inputs and 0-3 outputs using template-driven code generation"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Typed Processor Node (Priority: P1)

As a flow-based programming developer, I want to create a node with a specific input/output signature (e.g., 2 inputs, 1 output) using a factory method that generates a typed runtime with matching channel configuration, so that I can focus on writing the processing logic rather than boilerplate channel wiring.

**Why this priority**: This is the core capability - generating typed node runtimes based on input/output count. Without this, no other features work.

**Independent Test**: Can be fully tested by creating a node with specific input/output types, wiring channels, and verifying data flows correctly through the processor.

**Acceptance Scenarios**:

1. **Given** a developer wants a node with 2 inputs (Int, String) and 1 output (Boolean), **When** they use the factory to create an In2Out1 processor with appropriate types, **Then** the system generates a runtime with two typed input channels and one typed output channel.

2. **Given** a generated In2Out1 runtime, **When** data arrives on both input channels and the process function is invoked, **Then** the output is sent to the output channel with the expected type.

3. **Given** a generated runtime, **When** the developer inspects the node, **Then** the underlying CodeNode reflects the correct number of input and output ports.

---

### User Story 2 - Lifecycle Control of Typed Nodes (Priority: P1)

As a flow orchestrator, I want typed node runtimes to provide standard lifecycle control (start, stop, pause, resume), so that I can manage node execution within the FBP graph consistently.

**Why this priority**: Lifecycle control is essential for managing node execution. Without start/stop capabilities, nodes cannot be run.

**Independent Test**: Can be tested by starting a typed node, verifying it processes data, pausing it, verifying it stops processing, resuming it, and finally stopping it.

**Acceptance Scenarios**:

1. **Given** a typed node runtime (e.g., In1Out1Node), **When** start() is called with a scope, **Then** the node begins processing input channel data and emitting to output channels.

2. **Given** a running typed node, **When** stop() is called, **Then** the processing loop exits gracefully and channels are closed appropriately.

3. **Given** a running typed node, **When** pause() is called, **Then** the node temporarily stops processing until resume() is called.

---

### User Story 3 - Generator and Sink Node Variants (Priority: P2)

As a developer, I want to create generator nodes (0 inputs, 1-3 outputs) and sink nodes (1-3 inputs, 0 outputs) using the same factory pattern, so that I have consistent APIs for all node types in my flow graph.

**Why this priority**: Generators and sinks are common node types. Supporting them extends the factory pattern to cover all use cases.

**Independent Test**: Can be tested by creating a generator that emits values, connecting it to a sink, and verifying end-to-end data flow.

**Acceptance Scenarios**:

1. **Given** a developer needs a generator node, **When** they create an Out2Node with a generation function, **Then** the node has no input channels and two typed output channels.

2. **Given** a developer needs a sink node, **When** they create an In3Node with a consumption function, **Then** the node has three typed input channels and no output channels.

3. **Given** a generator and sink connected via channels, **When** both are started, **Then** data flows from generator to sink correctly.

---

### User Story 4 - ProcessResult for Multi-Output Nodes (Priority: P2)

As a developer, I want multi-output nodes to use a structured ProcessResult return type, so that I can return nullable values for each output in a type-safe manner.

**Why this priority**: Multi-output nodes need a clean way to return values for multiple output channels. This builds on basic node functionality.

**Independent Test**: Can be tested by creating a node with 3 outputs, returning a ProcessResult with some null values, and verifying only non-null values are sent.

**Acceptance Scenarios**:

1. **Given** an In1Out3 node with a process function returning ProcessResult<A, B, C>, **When** the process returns (valueA, null, valueC), **Then** only non-null values are sent to their respective output channels.

2. **Given** a ProcessResult type, **When** destructuring is used (val (u, v, w) = result), **Then** all three values are accessible with correct types.

---

### User Story 5 - Named Node Objects (Priority: P3)

As a flow graph manager, I want typed node runtimes to be generated as uniquely named objects (e.g., "MyTimerNode" or "DataTransformerNode"), so that I can identify and reference specific nodes in my application.

**Why this priority**: Naming provides identity and debuggability. This is enhancement over basic functionality.

**Independent Test**: Can be tested by creating a node with a custom name and verifying the generated object/CodeNode has that name.

**Acceptance Scenarios**:

1. **Given** a factory method with name="PaymentProcessor", **When** the typed node is created, **Then** the runtime and underlying CodeNode have the specified name.

2. **Given** multiple nodes in a flow, **When** logging or debugging, **Then** each node can be identified by its unique name.

---

### Edge Cases

- What happens when an input channel is closed while the node is waiting to receive?
  - Node should handle ClosedReceiveChannelException gracefully and exit the processing loop
- What happens when an output channel is closed while the node tries to send?
  - Node should handle ClosedSendChannelException gracefully and exit the processing loop
- What happens when a node has 0 inputs and 0 outputs?
  - This represents an invalid node configuration; the factory should reject or warn about this
- What happens when the process function throws an exception?
  - The exception should be caught, the node should transition to ERROR state, and channels should be closed
- How does a node with multiple inputs handle different arrival rates?
  - The node waits for all required inputs before invoking the process function (synchronous receive pattern)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support node configurations with 0-3 input channels and 0-3 output channels
- **FR-002**: System MUST generate typed processor functions (e.g., in2out1Processor) for each valid input/output combination
- **FR-003**: System MUST provide lifecycle control methods (start, stop, pause, resume) on all generated node runtimes
- **FR-004**: System MUST use strongly-typed channels (ReceiveChannel<T>, SendChannel<T>) for type safety
- **FR-005**: System MUST provide ProcessResult<U, V, W> data classes for multi-output nodes to enable nullable output values
- **FR-006**: System MUST allow developers to provide a custom process function matching the node's type signature
- **FR-007**: System MUST maintain a nodeControlJob for coroutine lifecycle management
- **FR-008**: System MUST track executionState (IDLE, RUNNING, PAUSED, ERROR) for each node runtime
- **FR-009**: System MUST handle channel closure gracefully without throwing unhandled exceptions
- **FR-010**: System MUST reject or provide clear error for invalid configurations (e.g., 0 inputs AND 0 outputs)
- **FR-011**: System MUST support custom naming of node instances for identification and debugging

### Key Entities

- **TypedNodeRuntime**: A generated runtime object combining channel configuration, lifecycle control, and process function. Contains nodeControlJob, executionState, and references to typed input/output channels.

- **ProcessResult<...>**: A data class with nullable typed fields for each output, supporting destructuring. Variants: ProcessResult1<U>, ProcessResult2<U,V>, ProcessResult3<U,V,W>.

- **TypedUseCase**: A function type alias matching a node's signature, e.g., `(A, B) -> ProcessResult2<U, V>` for an In2Out2 node.

- **CodeNode**: The underlying serializable model containing name, type, port definitions, and configuration (unchanged from existing implementation).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can create any valid node configuration (0-3 inputs × 0-3 outputs = 15 combinations, excluding 0×0) with a single factory call
- **SC-002**: All generated node runtimes provide consistent lifecycle control with start(), stop(), pause(), resume() methods
- **SC-003**: Type errors in channel wiring or process function signatures are caught at compile time, not runtime
- **SC-004**: Node creation and wiring requires 50% less boilerplate code compared to manual NodeRuntime construction
- **SC-005**: All edge cases (channel closure, exceptions) are handled gracefully without crashing the application
- **SC-006**: Generated nodes integrate seamlessly with existing StopWatch and other FBP flow implementations

## Clarifications

### Session 2026-02-15

- Q: What is the processing loop behavior for multi-input nodes? → A: Continuous loop - nodes continuously wait for input tuples and process until stopped (consistent with feature 014's continuous mode default)

## Assumptions

- The existing NodeRuntime, ExecutionState, and CodeNode infrastructure from feature 014 will be leveraged
- **Continuous processing mode**: All typed node runtimes run in continuous loop mode by default, processing input tuples repeatedly until stop() is called (aligned with feature 014)
- The synchronous receive pattern (wait for all inputs before processing) is the default behavior for multi-input nodes
- Maximum of 3 inputs and 3 outputs is sufficient for the current use cases (down from 5)
- ProcessResult types are nullable to allow selective output (send to some channels but not others)
- The factory will generate inline suspend functions for optimal performance

## Scope Boundaries

### In Scope
- Factory methods for all valid input/output combinations (1-3 inputs × 0-3 outputs, 0 inputs × 1-3 outputs)
- ProcessResult data classes for multi-output scenarios
- Typed channel wiring with compile-time type safety
- Integration with existing NodeRuntime lifecycle control
- Named node instances

### Out of Scope
- Asynchronous/selective receive patterns (e.g., receive from whichever channel has data first)
- Dynamic reconfiguration of node input/output count at runtime
- Visual graph editor integration (separate feature)
- Persistence/serialization of typed node configurations
