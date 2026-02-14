# Feature Specification: Node Control Extraction

**Feature Branch**: `013-node-control-extraction`
**Created**: 2026-02-13
**Status**: Draft
**Input**: User description: "Extract Node Control functionality from Components into CodeNode. Extract the node control functionality from TimerEmitterComponent and add it to the CodeNode class, e.g. timerJob (to be renamed nodeControlJob), executionState, start(..), stop(). Then also refactor DisplayReceiverComponent to utilize this change."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Unified Node Lifecycle Control (Priority: P1)

As a flow developer, I want CodeNode to provide standardized lifecycle control (start, stop) so that all node components have consistent execution management without duplicating control logic in each component.

**Why this priority**: This is the core extraction - moving the common node control pattern (job management, execution state, start/stop) from individual components into the base CodeNode class. All other stories depend on this foundation.

**Independent Test**: Can be tested by creating a CodeNode with processing logic, calling start(), verifying the node enters RUNNING state with an active job, then calling stop() and verifying graceful shutdown.

**Acceptance Scenarios**:

1. **Given** a CodeNode with processing logic assigned, **When** start(scope) is called, **Then** the node's executionState transitions to RUNNING and a nodeControlJob is created.

2. **Given** a running CodeNode (executionState == RUNNING), **When** stop() is called, **Then** the nodeControlJob is cancelled, and executionState transitions to IDLE.

3. **Given** a running CodeNode, **When** the coroutine scope is cancelled, **Then** the node gracefully shuts down and executionState reflects IDLE.

4. **Given** start() is called on an already running CodeNode, **When** the new start completes, **Then** the previous job is cancelled before the new job begins (no duplicate jobs).

---

### User Story 2 - TimerEmitterComponent Refactoring (Priority: P2)

As a flow developer, I want TimerEmitterComponent to delegate lifecycle management to CodeNode so that it only contains timer-specific business logic, reducing code duplication.

**Why this priority**: TimerEmitterComponent is the source of the extraction and serves as the validation that the extraction works correctly. This refactoring proves the pattern.

**Independent Test**: Can be tested by running the existing TimerEmitterComponentTest suite - all tests should pass after refactoring without changing test expectations.

**Acceptance Scenarios**:

1. **Given** a refactored TimerEmitterComponent, **When** compared to the original, **Then** timerJob, executionState, start(), and stop() are delegated to CodeNode.

2. **Given** the StopWatch flow with refactored TimerEmitter, **When** the flow is started, **Then** the timer emits elapsed time through the output channel exactly as before.

3. **Given** the StopWatch flow is running, **When** stop() is called, **Then** the timer stops gracefully with no data loss or exceptions.

---

### User Story 3 - DisplayReceiverComponent Refactoring (Priority: P3)

As a flow developer, I want DisplayReceiverComponent to use CodeNode's lifecycle management so that sink components follow the same pattern as generator components.

**Why this priority**: Validates that the extraction pattern works for different node types (sinks vs generators), ensuring the abstraction is general-purpose.

**Independent Test**: Can be tested by running the existing DisplayReceiverComponentTest suite - all tests should pass after refactoring.

**Acceptance Scenarios**:

1. **Given** a refactored DisplayReceiverComponent, **When** compared to the original, **Then** collectionJob management is delegated to CodeNode's nodeControlJob.

2. **Given** the StopWatch flow with both refactored components, **When** data flows from TimerEmitter to DisplayReceiver, **Then** the display updates correctly with received timer values.

3. **Given** the channel is closed, **When** DisplayReceiver is running, **Then** it gracefully exits without exceptions.

---

### Edge Cases

- What happens when start() is called without a valid CoroutineScope? The system should throw a clear error.
- How does the node handle rapid start/stop cycles? Jobs should be properly cancelled before new ones are created.
- What happens if stop() is called on an already stopped node? It should be a no-op without errors.
- How does node control interact with pause/resume from ControlConfig? Pause should suspend job execution, resume should continue.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: CodeNode MUST have a nodeControlJob property to track the active coroutine job.
- **FR-002**: CodeNode MUST provide a start(scope: CoroutineScope) method that launches the processing loop.
- **FR-003**: CodeNode MUST provide a stop() method that cancels the nodeControlJob and sets executionState to IDLE.
- **FR-004**: CodeNode MUST manage executionState transitions (IDLE -> RUNNING, RUNNING -> IDLE) during start/stop.
- **FR-005**: Components MUST be able to override or customize the processing loop while delegating job management to CodeNode.
- **FR-006**: The existing ProcessingLogic interface MUST remain unchanged for backward compatibility.
- **FR-007**: TimerEmitterComponent MUST be refactored to use CodeNode's lifecycle control.
- **FR-008**: DisplayReceiverComponent MUST be refactored to use CodeNode's lifecycle control.
- **FR-009**: All existing StopWatch tests MUST continue to pass after refactoring.
- **FR-010**: CodeNode MUST handle channel exceptions gracefully during shutdown.

### Key Entities

- **CodeNode**: Extended with nodeControlJob, start(), stop() methods for lifecycle management. Represents the base node abstraction in the FBP graph.
- **ProcessingLogic**: Functional interface for node processing. Unchanged - components implement this for their business logic.
- **ExecutionState**: Enum tracking node state (IDLE, RUNNING, PAUSED, ERROR). Already exists, now actively managed by CodeNode.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All existing StopWatch tests (currently 12+ tests) pass without modification to test expectations.
- **SC-002**: TimerEmitterComponent and DisplayReceiverComponent have reduced duplication - job/state management code consolidated into CodeNode.
- **SC-003**: New CodeNode lifecycle tests achieve full coverage of start/stop/state transitions.
- **SC-004**: The StopWatch end-to-end flow test continues to pass, demonstrating no regression in data flow.
- **SC-005**: Code review confirms single source of truth for job management logic in CodeNode.

## Assumptions

- The existing ExecutionState enum is sufficient for lifecycle management and does not need extension.
- CodeNode can be extended with mutable state for nodeControlJob while maintaining data class semantics where possible.
- Components will use composition or delegation to leverage CodeNode's lifecycle control.
- The refactoring is internal and does not change any public API contracts for flow orchestration.
