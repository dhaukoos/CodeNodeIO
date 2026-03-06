# Feature Specification: Reactive Feedback Loop for Source Nodes

**Feature Branch**: `038-reactive-source-loop`
**Created**: 2026-03-05
**Status**: Draft
**Input**: User description: "Reactive Feedback Loop for Source Nodes. Source nodes reactively re-emit when their corresponding observable StateFlows change, creating a self-perpetuating pipeline."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Processor Delay Controls Cycle Rate (Priority: P1)

When a flow graph contains processor nodes (nodes with both inputs and outputs), the processing cycle rate is governed by a configurable delay. This prevents feedback loops from spinning at maximum speed and allows developers to control timing (e.g., a 1-second tick rate for a stopwatch).

**Why this priority**: Without a rate-limiting mechanism, the feedback loop is either too fast (infinite spin) or impossible (no mechanism exists). This is the foundational enabler for all other stories.

**Independent Test**: Can be tested by creating a processor runtime, setting a delay value, sending input, and verifying that output appears only after the configured delay has elapsed.

**Acceptance Scenarios**:

1. **Given** a processor node with a delay value of 1000ms configured, **When** a value is received on its input channels, **Then** the processor waits 1000ms before processing the value and producing output.
2. **Given** a processor node with no delay configured (null), **When** a value is received, **Then** the processor processes immediately without any delay.
3. **Given** a processor node with a delay of 0ms, **When** a value is received, **Then** the processor processes immediately without any delay.
4. **Given** a running processor with a delay configured, **When** the controller changes the delay value, **Then** subsequent processing cycles use the new delay value.

---

### User Story 2 - Source Nodes Reactively Re-emit on State Changes (Priority: P2)

Source nodes (nodes with outputs but no inputs) observe their corresponding observable state and automatically re-emit to their output channels whenever that state changes. This eliminates the need for internal tick loops or timed generators.

**Why this priority**: This is the mechanism that closes the feedback loop. Without reactive re-emission, the pipeline runs exactly once after priming and then stops.

**Independent Test**: Can be tested by creating a source node with a reactive generate block, modifying its corresponding state externally, and verifying that the source emits the updated values to its output channels.

**Acceptance Scenarios**:

1. **Given** a started source node observing its state properties, **When** the state properties are modified externally, **Then** the source emits the updated values to its output channels.
2. **Given** a started source node, **When** the state properties are set to the same values (no actual change), **Then** the source does not re-emit (no duplicate emissions).
3. **Given** a source node with 2 output ports, **When** either corresponding state value changes, **Then** the source emits a single combined result containing both state values.
4. **Given** a source node with 1 output port, **When** its state value changes, **Then** the source emits the single updated value.

---

### User Story 3 - Controller Primes Source Nodes on Start (Priority: P3)

When the controller starts a flow graph, it sends the initial state values to each source node's output channels. This "primes the pump" and triggers the first processing cycle, after which the reactive feedback loop sustains itself.

**Why this priority**: Without priming, the reactive source block waits for state changes that never occur (since the initial state was already set before the source started observing). Priming is what kicks off the cycle.

**Independent Test**: Can be tested by starting a controller with a source-processor-sink flow, and verifying that the processor receives the initial values from the source without any external trigger.

**Acceptance Scenarios**:

1. **Given** a stopped flow graph with state values at defaults (e.g., 0), **When** the controller starts the flow, **Then** the source node's output channels receive the initial default values.
2. **Given** a flow graph that was previously run and reset, **When** the controller starts again, **Then** the source node's output channels receive the reset state values (defaults).
3. **Given** a flow graph with multiple source nodes, **When** the controller starts, **Then** each source node's output channels are primed with their respective state values.

---

### User Story 4 - End-to-End Feedback Loop (Priority: P4)

A complete flow graph with source, processor, and sink nodes operates as a self-perpetuating pipeline. After the controller primes the source, the processor processes values at the configured rate, updates state, and the source reactively re-emits, creating a continuous cycle that runs until paused or stopped.

**Why this priority**: This is the integration story that validates all previous stories working together. It depends on stories 1-3 being complete.

**Independent Test**: Can be tested with the StopWatch flow: start the controller, verify that seconds increment at 1-second intervals, pause stops the counter, resume continues it, and reset zeros all values.

**Acceptance Scenarios**:

1. **Given** a StopWatch flow graph with a 1-second delay, **When** the controller starts, **Then** the seconds counter increments by 1 every second.
2. **Given** a running StopWatch, **When** the controller pauses, **Then** the seconds counter stops incrementing.
3. **Given** a paused StopWatch, **When** the controller resumes, **Then** the seconds counter continues incrementing from where it left off.
4. **Given** a running StopWatch, **When** the controller resets, **Then** the seconds and minutes counters return to 0 and the flow stops.
5. **Given** a StopWatch running for 59 seconds, **When** the 60th second arrives, **Then** the seconds counter resets to 0 and the minutes counter increments to 1.

---

### Edge Cases

- What happens when a source node has no corresponding observable state properties? The source falls back to a passive await pattern (no reactive emission).
- How does the system handle rapid successive state changes during a processing delay? The source emits the latest combined state when its observation fires; buffered channels absorb any intermediate values.
- What happens if the delay value is changed while a processor is mid-delay? The current delay completes with the old value; the new value applies on the next cycle.
- What happens to pending reactive emissions during pause? The source's emit function blocks in the pause loop; when resumed, the latest pending emission goes through.
- What happens when stop is called while a processor is mid-delay? The processing loop exits because the execution state changes to IDLE, and the delay is interrupted by coroutine cancellation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Processor runtime nodes MUST support a configurable delay that is applied after receiving input values and before invoking the processing function.
- **FR-002**: The configurable delay MUST default to null (no delay), meaning existing processor behavior is unchanged unless explicitly configured.
- **FR-003**: Source nodes MUST observe their corresponding observable state properties and re-emit to output channels when those properties change.
- **FR-004**: Source nodes MUST skip their initial state observation emission so that the controller's priming is the sole initial trigger.
- **FR-005**: The controller MUST send initial state values to all source node output channels immediately after starting the flow.
- **FR-006**: The controller's delay propagation method MUST set the delay value on all runtime nodes in the flow (not just source nodes).
- **FR-007**: The code generators MUST produce the reactive source observation pattern for source nodes based on their output port count (1, 2, or 3 outputs).
- **FR-008**: The code generators MUST produce the controller priming logic for each source node's output channels using the corresponding state property values.
- **FR-009**: Processor processing logic MUST receive input values through its function parameters (not read directly from global state) to maintain proper data flow through the pipeline.
- **FR-010**: Processor processing logic MUST write updated values back to observable state to trigger the source node's reactive re-emission.
- **FR-011**: The generated delay propagation method MUST update the delay on all nodes in the flow graph, not only source or generator nodes.
- **FR-012**: The controller code generator MUST accept the state object package path to generate correct state references for priming.

### Key Entities

- **Source Node**: A node with output ports but no input ports. Emits data into the pipeline by observing state changes and forwarding current values to output channels.
- **Processor Node**: A node with both input and output ports. Receives values, applies a configurable delay, processes them, and emits results. Updates shared observable state as a side effect.
- **Sink Node**: A node with input ports but no output ports. Receives processed values and updates observable state for UI binding.
- **Observable State**: A shared state object containing flow properties for each boundary port (source outputs and sink inputs). State changes trigger reactive source re-emission.
- **Attenuation Delay**: A configurable time value (in milliseconds) that controls the processing cycle rate. Applied in processor nodes between receiving input and invoking the processing function.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The StopWatch application's seconds counter increments exactly once per second (within 50ms tolerance) when running.
- **SC-002**: Starting a flow graph with the feedback loop pattern produces continuous output without manual re-triggering; the cycle sustains itself after the initial prime.
- **SC-003**: Pausing a running flow stops all processing within one cycle (within the configured delay tolerance), and resuming continues from the paused state.
- **SC-004**: All existing processor, transformer, and filter runtime tests continue to pass with no delay configured (null default preserves backward compatibility).
- **SC-005**: The generated code for source nodes, controllers, and flows compiles and runs correctly for 1-output, 2-output, and 3-output source node configurations.
- **SC-006**: Resetting a running flow returns all observable state to default values and stops the feedback cycle completely.

## Assumptions

- The feedback loop is driven by state changes propagated through observable StateFlow properties. The source node observes its own output port state flows and re-emits when they change.
- Attenuation delay defaults to null (no delay). When null or 0, processors behave identically to the current implementation with no backward compatibility concerns.
- The delay is applied uniformly to all processor runtimes. If different nodes need different delays, that would be a separate future enhancement.
- Channel buffer capacity (64 elements) is sufficient for the feedback loop. At a 1-second cycle rate, there is no risk of buffer overflow.
- The controller primes all source nodes simultaneously on start. There is no ordered priming sequence.

## Scope

### In Scope

- Adding delay support to all processor, transformer, and filter runtime classes
- Generating reactive source blocks in the flow code generator
- Generating controller priming logic in the controller code generator
- Propagating attenuation delay to all nodes (not just sources)
- Updating the StopWatch processing logic to use input values
- Adding state object package parameter to the controller code generator

### Out of Scope

- Per-node configurable delay values (all nodes share the same delay)
- Dynamic delay adjustment based on pipeline throughput
- Multi-source feedback loops with cross-source dependencies
- UI controls for adjusting the delay at runtime (existing API is sufficient)
- Changes to sink runtime classes (sinks are terminal and do not need delay)
