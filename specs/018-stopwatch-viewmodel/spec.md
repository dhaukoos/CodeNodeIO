# Feature Specification: StopWatch ViewModel Pattern

**Feature Branch**: `018-stopwatch-viewmodel`
**Created**: 2026-02-16
**Status**: Draft
**Input**: User description: "Use ViewModel Pattern to interface between the FlowGraph and Compose UI of the StopWatch module - Investigate how the Stopwatch module could use the multiplatform ViewModel implementation to interface between the FlowGraph's domain logic and the Compose UI of the Stopwatch Face of the KMPMobileApp."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Clean Separation Between FlowGraph and UI (Priority: P1)

A developer working on the KMPMobileApp wants the StopWatch UI to observe FlowGraph state changes through a dedicated ViewModel layer, rather than having the composables directly access the StopWatchController.

**Why this priority**: This is the core architectural improvement - establishing a clear boundary between the FlowGraph domain logic and the Compose UI layer. Without this separation, all other improvements are impossible.

**Independent Test**: Can be tested by creating a StopWatchViewModel that wraps StopWatchController, verifying that the ViewModel exposes StateFlow properties for elapsed time and execution state, and confirming that the StopWatchFace composable can render correctly using only ViewModel state.

**Acceptance Scenarios**:

1. **Given** a StopWatchViewModel wrapping StopWatchController, **When** the developer creates a StopWatch composable, **Then** the composable only accesses state through the ViewModel's StateFlow properties.
2. **Given** the FlowGraph updates elapsed seconds internally, **When** the ViewModel observes this change, **Then** the UI automatically updates to reflect the new seconds value.
3. **Given** a StopWatchViewModel, **When** a unit test instantiates it with a mock controller, **Then** the test can verify state transitions without any Compose UI dependencies.

---

### User Story 2 - Consistent Action Handling (Priority: P2)

A developer wants all user interactions (start, stop, reset) to be handled through ViewModel actions, providing a single point of control for business logic and state management.

**Why this priority**: Once the state observation layer is in place (US1), centralizing actions ensures predictable behavior and enables comprehensive testing of all user interactions.

**Independent Test**: Can be tested by calling ViewModel action methods (start(), stop(), reset()) and verifying that the corresponding StateFlow values update correctly without involving any UI components.

**Acceptance Scenarios**:

1. **Given** the stopwatch is idle, **When** the developer calls viewModel.start(), **Then** the execution state transitions to RUNNING and the UI observes this change.
2. **Given** the stopwatch is running, **When** the developer calls viewModel.stop(), **Then** the execution state transitions to IDLE and elapsed time stops incrementing.
3. **Given** the stopwatch shows elapsed time, **When** the developer calls viewModel.reset(), **Then** elapsed seconds and minutes reset to 0 and execution state becomes IDLE.

---

### User Story 3 - Platform-Agnostic ViewModel (Priority: P3)

A developer building for multiple platforms (Android, iOS, Desktop) wants the StopWatch ViewModel to work identically across all Kotlin Multiplatform targets without platform-specific code.

**Why this priority**: Multiplatform consistency ensures the same behavior on all targets, but this is only valuable after the core ViewModel architecture (US1, US2) is established.

**Independent Test**: Can be tested by running the same ViewModel unit tests on JVM and verifying they pass, confirming platform-agnostic behavior.

**Acceptance Scenarios**:

1. **Given** a StopWatchViewModel implementation, **When** compiled for Android, iOS, and Desktop targets, **Then** the ViewModel class is available in commonMain without platform-specific implementations.
2. **Given** the ViewModel uses JetBrains lifecycle-viewmodel-compose, **When** used in composables on any platform, **Then** the ViewModel lifecycle is managed correctly.

---

### Edge Cases

- What happens when the ViewModel is created but the FlowGraph is not yet initialized?
- How does the ViewModel handle rapid start/stop/reset sequences?
- What happens when the UI subscribes to ViewModel state after the stopwatch has already been running?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a StopWatchViewModel class that exposes stopwatch state as StateFlow properties (elapsed seconds, elapsed minutes, execution state).
- **FR-002**: System MUST expose start(), stop(), and reset() actions as public methods on the ViewModel.
- **FR-003**: ViewModel MUST delegate execution control to the underlying StopWatchController without exposing controller internals.
- **FR-004**: UI composables (StopWatch, StopWatchFace) MUST observe ViewModel state using collectAsState() rather than accessing controller directly.
- **FR-005**: ViewModel MUST be instantiable and testable without Compose UI dependencies.
- **FR-006**: System MUST support late subscription - UI that subscribes after the stopwatch started MUST receive the current state immediately.
- **FR-007**: ViewModel MUST extend the JetBrains multiplatform ViewModel class for proper lifecycle management.

### Key Entities

- **StopWatchViewModel**: The ViewModel class that bridges FlowGraph domain logic and Compose UI. Exposes observable state (elapsedSeconds, elapsedMinutes, executionState) and actions (start, stop, reset).
- **StopWatchState**: Data class representing the current stopwatch state including execution state and elapsed time values.
- **StopWatchController**: Existing generated controller that manages the FlowGraph execution (already exists, not modified).
- **StopWatchFace**: Existing UI composable that renders the analog clock face (already exists, receives state from ViewModel).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of StopWatch UI state access goes through the ViewModel's StateFlow properties (no direct controller access in composables).
- **SC-002**: ViewModel unit tests pass without any Compose UI imports or dependencies.
- **SC-003**: Existing stopwatch functionality works identically after migration (start, stop, reset, time display).
- **SC-004**: The same ViewModel code compiles and runs on Android, iOS, and Desktop targets.
- **SC-005**: All ViewModel actions (start, stop, reset) correctly delegate to and update state from the StopWatchController.
- **SC-006**: UI latency from FlowGraph state change to visible update remains imperceptible to users (under 100 milliseconds).

## Scope

### In Scope

- Creating StopWatchViewModel to wrap StopWatchController
- Refactoring StopWatch.kt composable to use ViewModel
- Adding ViewModel dependency to KMPMobileApp module
- Writing unit tests for StopWatchViewModel
- Documenting the ViewModel pattern for FlowGraph integration

### Out of Scope

- Modifying the StopWatchController generated code
- Modifying the underlying FlowGraph or FBP runtime
- Changing the StopWatchFace composable rendering logic
- Adding new stopwatch features (lap times, countdown mode, etc.)

## Assumptions

- The JetBrains lifecycle-viewmodel-compose library (used in graphEditor) is compatible with KMPMobileApp's multiplatform setup
- StopWatchController's StateFlow properties (elapsedSeconds, elapsedMinutes, executionState) are sufficient for ViewModel state without transformation
- The existing StopWatch.kt composable can be refactored to accept a ViewModel parameter without breaking existing usage
