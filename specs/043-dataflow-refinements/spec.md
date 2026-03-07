# Feature Specification: Module DataFlow Refinements

**Feature Branch**: `043-dataflow-refinements`
**Created**: 2026-03-07
**Status**: Draft
**Input**: User description: "Module DataFlow refinements. For UserProfiles, only send data on the output channel whose action button was pressed (not all three). For StopWatch, elapsedMinutes should only emit when the value changes."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - UserProfiles Selective Channel Output (Priority: P1)

When a user presses the "Add" button in UserProfiles, only the add/save output channel should carry data to the downstream processor. Currently, all three output channels (add, update, remove) send data whenever any single action is triggered, causing unnecessary data flow on unrelated channels.

**Why this priority**: This is the more impactful change. Sending data on all three channels when only one action was taken creates misleading data flow animations and potentially redundant processing in the downstream node. Fixing this makes the data flow accurate and efficient.

**Independent Test**: Can be fully tested by pressing each action button (Add, Update, Remove) individually and observing that only the corresponding output channel carries data, while the other two channels remain silent.

**Acceptance Scenarios**:

1. **Given** the UserProfiles runtime is running, **When** the user presses the "Add" button, **Then** only the add/save output channel sends data to the downstream processor; the update and remove channels do not emit
2. **Given** the UserProfiles runtime is running, **When** the user presses the "Update" button, **Then** only the update output channel sends data; the add and remove channels do not emit
3. **Given** the UserProfiles runtime is running, **When** the user presses the "Remove" button, **Then** only the remove output channel sends data; the add and update channels do not emit
4. **Given** data flow animation is enabled, **When** a single action button is pressed, **Then** animated dots only appear on the connection for the triggered channel, not on all three connections

---

### User Story 2 - StopWatch Minutes-Only-On-Change Emission (Priority: P2)

When the StopWatch is running, the elapsedMinutes output should only emit a new value when the minutes counter actually changes (i.e., when seconds rolls over from 59 to 0). Currently, elapsedMinutes emits on every second tick, even when the value hasn't changed.

**Why this priority**: This is a straightforward optimization that reduces unnecessary data flow. The minutes value only changes once every 60 seconds, so emitting it every second creates 59 redundant emissions per minute cycle.

**Independent Test**: Can be fully tested by running the StopWatch with data flow animation enabled and observing that the minutes output connection only shows an animated dot when the minutes value increments (every 60 seconds), not on every tick.

**Acceptance Scenarios**:

1. **Given** the StopWatch is running and the current time is 0:58, **When** the timer ticks to 0:59, **Then** the seconds output emits but the minutes output does not emit (minutes is still 0)
2. **Given** the StopWatch is running and the current time is 0:59, **When** the timer ticks to 1:00, **Then** both the seconds output and the minutes output emit (minutes changed from 0 to 1)
3. **Given** data flow animation is enabled, **When** the timer ticks without a minutes change, **Then** only the seconds connection shows an animated dot; the minutes connection does not

---

### Edge Cases

- What happens if two UserProfiles actions are triggered in rapid succession? Each should send data only on its respective channel, processed in order.
- What happens at the StopWatch boundary of 59:59 → 0:00 (if the timer wraps)? Minutes should emit because the value changed.
- What happens if the UserProfiles downstream processor expects all three inputs simultaneously? The processor must handle receiving data on individual channels independently rather than waiting for all three.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The UserProfiles source node MUST send data only on the output channel corresponding to the action that was triggered (add, update, or remove)
- **FR-002**: The UserProfiles source node MUST NOT send data on output channels for actions that were not triggered
- **FR-003**: The UserProfiles downstream processor MUST be able to process data received on any single input channel independently, without waiting for all three inputs to arrive
- **FR-004**: The StopWatch processor MUST only emit a minutes value on the minutes output channel when the minutes value has actually changed compared to the previous tick
- **FR-005**: The StopWatch processor MUST continue to emit the seconds value on every tick regardless of whether minutes changed

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Pressing a single UserProfiles action button results in exactly 1 output channel emission (not 3)
- **SC-002**: With data flow animation enabled, pressing Add in UserProfiles shows an animated dot on only 1 connection path (not 3)
- **SC-003**: Over a 60-second StopWatch cycle, the minutes output emits exactly once (at the 59→0 seconds rollover), not 60 times
- **SC-004**: With data flow animation enabled, the StopWatch minutes connection shows a dot only when minutes changes

## Assumptions

- The UserProfiles downstream processor (UserProfileRepository) can be modified to accept input on any single channel independently, using an "any-input" trigger pattern rather than requiring all inputs simultaneously
- The StopWatch seconds output behavior remains unchanged — it emits every tick as before
- These changes are backward-compatible and do not affect the module's external API or ViewModel behavior
