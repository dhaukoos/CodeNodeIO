# Feature Specification: Refactor TimerEmitterComponent

**Feature Branch**: `020-refactor-timer-emitter`
**Created**: 2026-02-18
**Status**: Draft
**Input**: User description: "Refactor TimerEmitterComponent to cleanly separate the common CodeNode nodeControl and flowgraph execution control functionality (which should become part of the fbpDsl module) from the unique processing logic."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Simplified Component Authoring (Priority: P1)

As a developer creating a new CodeNode component, I want the runtime classes to handle all execution lifecycle concerns (pause/resume loops, execution state checks, delay management) so that my component only needs to provide the unique business logic (e.g., a processing function and any stateful outputs).

**Why this priority**: This is the core value of the refactoring. If the runtime handles lifecycle boilerplate, every future component becomes simpler to write and less error-prone. The TimerEmitterComponent serves as the proof case.

**Independent Test**: Can be verified by refactoring TimerEmitterComponent to provide only its incrementer logic and StateFlow updates, while the runtime handles the execution loop, pause hooks, and state transitions - and the StopWatch app continues to function identically.

**Acceptance Scenarios**:

1. **Given** a refactored TimerEmitterComponent, **When** the StopWatch is started, **Then** the timer ticks at the correct interval and the display updates as before.
2. **Given** a refactored TimerEmitterComponent, **When** the StopWatch is paused and then resumed, **Then** the timer pauses and resumes correctly without the component containing any pause/resume logic.
3. **Given** a refactored TimerEmitterComponent, **When** the StopWatch is reset and restarted, **Then** the timer restarts from zero and ticks correctly.
4. **Given** a refactored TimerEmitterComponent, **When** the component code is reviewed, **Then** it contains no references to ExecutionState, no pause loops, and no coroutine context checks - only the incrementer function and StateFlow declarations.

---

### User Story 2 - Runtime Handles Timed Generation Loop (Priority: P2)

As a developer building a time-based generator component, I want the runtime to support a configurable tick interval so that my component only needs to supply a "what happens each tick" function rather than implementing the entire timing loop with pause/resume/stop handling.

**Why this priority**: TimerEmitterComponent's generator block is mostly boilerplate (while loop, pause check, delay, state check). Moving this into the runtime as a "timed generator" pattern makes it reusable for any periodic-emission component.

**Independent Test**: Can be verified by creating a timed generator using the new runtime capability with only a tick function, and confirming it emits at the specified interval with correct pause/resume behavior.

**Acceptance Scenarios**:

1. **Given** a runtime that supports timed generation, **When** a generator is created with a tick interval and a processing function, **Then** the runtime calls the processing function at the specified interval.
2. **Given** a running timed generator, **When** execution is paused, **Then** the runtime suspends the tick loop without the component needing any pause logic.
3. **Given** a paused timed generator, **When** execution is resumed, **Then** ticking resumes at the configured interval.
4. **Given** a running timed generator, **When** execution is stopped, **Then** the tick loop exits cleanly and channels are closed.

---

### User Story 3 - DisplayReceiverComponent Follows Same Pattern (Priority: P3)

As a developer maintaining the StopWatch components, I want the DisplayReceiverComponent to follow the same clean separation pattern as the refactored TimerEmitterComponent so that both components serve as reference examples of the intended architecture.

**Why this priority**: DisplayReceiverComponent is already relatively clean (its consumer block is minimal), but ensuring it follows the identical pattern validates the approach and provides a second reference implementation.

**Independent Test**: Can be verified by confirming DisplayReceiverComponent contains only its StateFlow update logic and delegates all lifecycle concerns to the runtime, while the StopWatch display continues to update correctly.

**Acceptance Scenarios**:

1. **Given** a refactored DisplayReceiverComponent, **When** time values flow through the channels, **Then** the display updates correctly as before.
2. **Given** a refactored DisplayReceiverComponent, **When** its code is reviewed, **Then** it contains no execution lifecycle boilerplate - only the consumer function and StateFlow declarations.

---

### Edge Cases

- What happens when the tick interval is zero? The runtime should treat zero as "emit as fast as possible" (no delay between ticks).
- What happens if the processing function throws an exception during a tick? The runtime should catch it and transition to an error state rather than silently exiting.
- What happens if the component's observable state is read before the first tick? It should contain the initial values provided at construction (e.g., 0 for seconds and minutes).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The runtime module MUST provide a way for generator components to supply only a tick-processing function without needing to implement the execution loop, pause hooks, or state management.
- **FR-002**: The runtime MUST handle the complete execution lifecycle for timed generators: starting the tick loop, pausing (suspending ticks), resuming, and stopping (clean exit with channel closure).
- **FR-003**: The runtime MUST support a configurable tick interval for timed generators.
- **FR-004**: The refactored TimerEmitterComponent MUST contain only its unique business logic: the incrementer function (seconds/minutes rollover), observable state declarations, and state updates.
- **FR-005**: The refactored TimerEmitterComponent MUST NOT contain any references to execution state management, pause loops, coroutine context checks, or execution lifecycle management.
- **FR-006**: The refactored TimerEmitterComponent MUST produce identical observable behavior to the current implementation: same tick timing, same pause/resume behavior, same reset behavior.
- **FR-007**: The refactored DisplayReceiverComponent MUST follow the same clean separation pattern, containing only its consumer function and observable state declarations.
- **FR-008**: All existing tests for the StopWatch (unit tests, integration tests, channel tests) MUST continue to pass after refactoring.

### Key Entities

- **Timed Generator Pattern**: A runtime-level abstraction that combines a tick interval with a user-supplied processing function, handling the execution loop internally. The processing function receives the ability to emit values and is called once per tick interval.
- **Processing Function**: The component author's unique business logic, distilled to its simplest form. For TimerEmitterComponent, this is the incrementer that advances seconds/minutes. It has no knowledge of execution state or lifecycle.
- **Component**: The user-facing class that wires together a runtime instance with a processing function and exposes domain-specific observable state (e.g., observable flows for elapsed time).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The refactored TimerEmitterComponent's unique processing logic (incrementer + state management) is expressible in fewer than 20 lines of code, down from the current ~50 lines of mixed business/lifecycle logic.
- **SC-002**: The StopWatch application passes all existing automated tests without modification to test assertions (test setup may change to reflect new wiring patterns).
- **SC-003**: The StopWatch application's user-visible behavior (start, pause, resume, reset) is identical before and after refactoring.
- **SC-004**: A new timed generator component can be created by providing only a tick function and interval, with zero boilerplate for execution lifecycle management.

## Assumptions

- The existing runtime class hierarchy (NodeRuntime, Out2GeneratorRuntime, etc.) is the appropriate place to add timed generation support, keeping the component layer thin.
- The generator block type can be extended or supplemented with a simpler "tick block" variant that removes the need for the component to manage its own while-loop.
- The DisplayReceiverComponent is already close to the target pattern and will require minimal changes.
- The ProcessingLogic interface implementation (the invoke method) in both components is outside the scope of this refactoring, as it serves a different execution mode (single-invocation vs. continuous).
