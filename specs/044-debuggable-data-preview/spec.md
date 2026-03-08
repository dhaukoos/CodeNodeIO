# Feature Specification: Debuggable Data Runtime Preview

**Feature Branch**: `044-debuggable-data-preview`
**Created**: 2026-03-07
**Status**: Draft
**Input**: User description: "Debuggable Data Runtime Preview — When an attenuated playback is paused in Runtime Preview with data flow animation enabled, selecting a connection displays the most recent data that passed through it."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Inspect Connection Data When Paused (Priority: P1)

When a developer is debugging a flow graph in Runtime Preview mode with data flow animation enabled, they pause the execution to examine the system state. They click on a connection (edge) between two nodes. In the Properties panel, below the existing connection properties, the most recent Information Packet (data value) that passed through that connection is displayed. This allows the developer to see what data was flowing through each connection at the moment they paused.

**Why this priority**: This is the core feature. Without the ability to see the data in a paused connection, the entire feature has no value. It directly enables runtime debugging — the primary user need.

**Independent Test**: Run any module in Runtime Preview with attenuation set to 1000ms and "Animate Data Flow" enabled. Press Start, wait for several ticks, then press Pause. Click on a connection in the flow graph. Verify the Properties panel shows the most recent data value that passed through that connection.

**Acceptance Scenarios**:

1. **Given** Runtime Preview is running with data flow animation enabled and attenuation > 0, **When** the user pauses execution and clicks on a connection, **Then** the Properties panel displays the connection's existing properties AND the most recent data value that passed through that connection
2. **Given** Runtime Preview is paused and a connection is selected, **When** the user clicks on a different connection, **Then** the Properties panel updates to show the data value for the newly selected connection
3. **Given** Runtime Preview is paused and a connection that has never carried data is selected, **When** the user views the Properties panel, **Then** the panel indicates no data has passed through this connection (e.g., "No data captured" or empty state)

---

### User Story 2 - Data Capture Tied to Animation Toggle (Priority: P2)

Data capture (snapshotting the most recent value on each connection) is only active when the "Animate Data Flow" toggle is enabled. When animation is disabled, no performance overhead is incurred from data capture. This ensures the debugging capability does not affect normal runtime performance.

**Why this priority**: This is important for ensuring the feature doesn't degrade performance during normal (non-debug) operation, but it builds on the core inspection capability from US1.

**Independent Test**: Run a module in Runtime Preview with "Animate Data Flow" disabled. Pause execution and click on a connection. Verify no snapshot data is displayed. Then enable "Animate Data Flow", run again, pause, and verify data is now captured and visible.

**Acceptance Scenarios**:

1. **Given** Runtime Preview is running with "Animate Data Flow" disabled, **When** the user pauses and selects a connection, **Then** no data snapshot is available for display
2. **Given** Runtime Preview is running with "Animate Data Flow" enabled, **When** data flows through a connection, **Then** the most recent value is captured and available for inspection when paused
3. **Given** "Animate Data Flow" is toggled off after data was previously captured, **When** the user pauses and selects a connection, **Then** previously captured data is cleared or unavailable

---

### Edge Cases

- What happens if the data value is very large (e.g., a list of hundreds of items)? The display should present a reasonable summary or truncated view rather than overwhelming the Properties panel.
- What happens if the same connection carries different data types over time? The display shows the most recent value regardless of type.
- What happens if the user selects a connection while execution is running (not paused)? The data snapshot feature is only available when paused; during running, the Properties panel shows only the standard connection properties.
- What happens after Stop is pressed? Captured data should be cleared since the runtime session has ended.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST capture the most recent data value that passes through each connection when data flow animation is enabled
- **FR-002**: The system MUST display the captured data value in the Properties panel when a connection is selected and execution is paused
- **FR-003**: The system MUST NOT capture data when data flow animation is disabled (no performance overhead)
- **FR-004**: The data display MUST update when the user selects a different connection while paused
- **FR-005**: The system MUST clear captured data when the runtime session is stopped
- **FR-006**: The system MUST show an appropriate empty state when a selected connection has no captured data
- **FR-007**: The captured data display MUST appear below the existing connection properties in the Properties panel, maintaining the current layout

### Key Entities

- **Transit Snapshot**: The most recent data value captured on a connection, associated with the specific connection (edge) in the flow graph
- **Connection**: An edge between two nodes in the flow graph, identified by source node/port and target node/port

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When paused with animation enabled, selecting any connection that has carried data displays the most recent value within 1 second
- **SC-002**: With data flow animation disabled, there is zero additional memory or processing overhead from the data capture mechanism
- **SC-003**: 100% of connections in a running flow graph correctly capture and display their most recent data value when inspected
- **SC-004**: The captured data display does not interfere with or obscure existing connection properties in the Properties panel

## Assumptions

- The data values flowing through connections can be meaningfully displayed as text (via toString() or similar). Complex objects will show their string representation.
- The Properties panel already supports displaying connection properties when a connection is selected — this feature adds to that existing display.
- Only the single most recent value per connection is captured (not a history). This keeps memory usage bounded.
- The "Animate Data Flow" toggle already exists and controls animation behavior. This feature piggybacks on that toggle for enabling/disabling data capture.
