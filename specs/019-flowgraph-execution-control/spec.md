# Feature Specification: Unified FlowGraph Execution Control

**Feature Branch**: `019-flowgraph-execution-control`
**Created**: 2026-02-17
**Status**: Draft
**Input**: User description: "Unified FlowGraph Execution Control - centralized pause/resume control through RootControlNode"

## Problem Statement

The current StopWatch implementation bypasses the RootControlNode, directly manipulating component execution states. This creates:
1. Disconnection between model-level ExecutionState and runtime execution
2. No centralized pause/resume control
3. UI buttons don't control the FlowGraph through proper FBP architecture
4. CodeNodes don't inherit execution state from their parent (RootControlNode)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pause/Resume UI Control (Priority: P1)

As a StopWatch user, I want a Pause/Resume button between Start/Stop and Reset, so I can temporarily halt the timer without resetting.

**Why this priority**: Core user-facing functionality. Without pause/resume, users must stop and lose timing context or let the timer continue running.

**Independent Test**: Can be fully tested by starting the timer, pressing Pause, observing frozen display, pressing Resume, and confirming timer continues from paused value.

**Acceptance Scenarios**:

1. **Given** the timer is RUNNING, **When** user presses Pause, **Then** the timer display freezes at current value and Pause button changes to Resume
2. **Given** the timer is PAUSED, **When** user presses Resume, **Then** the timer continues counting from the paused value and Resume button changes to Pause
3. **Given** the timer is PAUSED, **When** user presses Reset, **Then** the timer resets to 00:00 and returns to IDLE state
4. **Given** the timer is IDLE, **When** user views controls, **Then** only Start and Reset buttons are visible (no Pause/Resume)

---

### User Story 2 - Centralized Execution Control (Priority: P1)

As a FlowGraph developer, I want UI controls to go through RootControlNode, so execution state is managed centrally following proper FBP architecture.

**Why this priority**: Architectural foundation. Without centralized control, state management becomes fragmented and difficult to maintain.

**Independent Test**: Can be tested by verifying that button presses trigger RootControlNode methods and state propagates to all nodes in the flow.

**Acceptance Scenarios**:

1. **Given** a FlowGraph in IDLE state, **When** Start is triggered, **Then** RootControlNode.startAll() sets all nodes to RUNNING
2. **Given** a FlowGraph in RUNNING state, **When** Pause is triggered, **Then** RootControlNode.pauseAll() sets all nodes to PAUSED
3. **Given** a FlowGraph in PAUSED state, **When** Resume is triggered, **Then** RootControlNode.resumeAll() sets all nodes back to RUNNING
4. **Given** a FlowGraph in any state, **When** Stop is triggered, **Then** RootControlNode.stopAll() sets all nodes to IDLE
5. **Given** a node with independentControl=true, **When** parent state changes, **Then** that node retains its own state

---

### User Story 3 - Runtime Registration (Priority: P1)

As a FlowGraph framework developer, I want a RuntimeRegistry that tracks active NodeRuntime instances, so RootControlNode can propagate state changes to actual running components.

**Why this priority**: Bridge between model and runtime. Without this, model state changes don't affect actual execution.

**Independent Test**: Can be tested by starting a flow, verifying runtimes register, calling pauseAll(), and confirming all registered runtimes receive pause() calls.

**Acceptance Scenarios**:

1. **Given** a NodeRuntime starts, **When** start() completes, **Then** the runtime is registered in RuntimeRegistry
2. **Given** a NodeRuntime stops, **When** stop() completes, **Then** the runtime is unregistered from RuntimeRegistry
3. **Given** multiple registered runtimes, **When** RootControlNode.pauseAll() is called, **Then** pause() is called on each registered runtime
4. **Given** multiple registered runtimes, **When** RootControlNode.resumeAll() is called, **Then** resume() is called on each registered runtime

---

### User Story 4 - Pause-Aware Processing Loops (Priority: P2)

As a FlowGraph framework developer, I want all runtime processing loops to check for PAUSED state, so they honor pause commands and wait for resume.

**Why this priority**: Enables actual pause behavior. Without this, runtimes would ignore pause state and continue processing.

**Independent Test**: Can be tested by starting a generator, pausing it, verifying no new emissions occur, resuming, and confirming emissions continue.

**Acceptance Scenarios**:

1. **Given** a GeneratorRuntime is RUNNING, **When** executionState changes to PAUSED, **Then** emit loop waits without emitting
2. **Given** a SinkRuntime is RUNNING, **When** executionState changes to PAUSED, **Then** receive loop waits without processing
3. **Given** any paused runtime, **When** executionState changes to RUNNING, **Then** processing resumes from where it paused
4. **Given** any paused runtime, **When** executionState changes to IDLE, **Then** processing loop exits cleanly

---

### User Story 5 - Execution State Inheritance (Priority: P2)

As a FlowGraph developer, I want CodeNodes to inherit execution state from RootControlNode by default, so I don't need to manage each node individually.

**Why this priority**: Simplifies flow management. Developers only control the root; children follow automatically.

**Independent Test**: Can be tested by creating a flow with multiple nodes, changing root state, and verifying all child nodes (without independentControl) reflect the new state.

**Acceptance Scenarios**:

1. **Given** a node with independentControl=false (default), **When** parent transitions to PAUSED, **Then** node inherits PAUSED state
2. **Given** a node with independentControl=true, **When** parent transitions to PAUSED, **Then** node retains its current state
3. **Given** a nested GraphNode hierarchy, **When** root state changes, **Then** state propagates through all levels respecting independentControl flags

---

### Edge Cases

- What happens when pause is called during channel send/receive? (Should complete current operation, then pause)
- How does the system handle pause during the speedAttenuation delay? (Should check state after delay returns)
- What happens if a runtime is registered but its node has independentControl=true? (Runtime should only respond to direct pause() calls, not propagated ones)
- What happens when channels close during pause? (Should transition to IDLE, not remain PAUSED)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a Pause button that appears when FlowGraph is in RUNNING state
- **FR-002**: System MUST provide a Resume button that appears when FlowGraph is in PAUSED state
- **FR-003**: System MUST route all execution control through RootControlNode (start, pause, resume, stop, reset)
- **FR-004**: System MUST maintain a RuntimeRegistry that tracks all active NodeRuntime instances
- **FR-005**: System MUST call pause() on all registered runtimes when RootControlNode.pauseAll() is invoked
- **FR-006**: System MUST call resume() on all registered runtimes when RootControlNode.resumeAll() is invoked
- **FR-007**: All runtime processing loops MUST check executionState and wait when PAUSED
- **FR-008**: Runtimes MUST register themselves on start() and unregister on stop()
- **FR-009**: State propagation MUST respect the independentControl flag on each node
- **FR-010**: Paused runtimes MUST exit cleanly when state transitions to IDLE

### Key Entities

- **RuntimeRegistry**: Tracks active NodeRuntime instances for a flow; enables RootControlNode to propagate runtime commands
- **RootControlNode**: Extended to support resumeAll() and integration with RuntimeRegistry
- **NodeRuntime**: Base class extended to register/unregister with RuntimeRegistry on lifecycle events
- **ExecutionState**: Existing enum (IDLE, RUNNING, PAUSED, ERROR) - no changes needed

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can pause and resume the StopWatch timer with state preserved accurately
- **SC-002**: All UI control actions route through RootControlNode (no direct component manipulation)
- **SC-003**: 100% of runtime classes honor PAUSED state in their processing loops
- **SC-004**: State propagation respects independentControl flag in all scenarios
- **SC-005**: Pause/resume cycle completes without data loss or timing drift greater than the speedAttenuation interval
- **SC-006**: RuntimeRegistry correctly tracks all active runtimes (verified by unit tests)

## Assumptions

- The existing pause()/resume() methods on NodeRuntime provide the correct foundation and only need integration
- Channel operations (send/receive) will complete their current operation before honoring pause
- The 10ms delay in pause wait loops is acceptable for responsiveness
- ControlConfig.pauseBufferSize implementation is out of scope for this feature (future enhancement)
