# Feature Specification: StopWatch Virtual Circuit Demo

**Feature Branch**: `008-stopwatch-virtual-circuit`
**Created**: 2026-02-08
**Status**: Draft
**Input**: User description: "Create a simple Virtual Circuit for the KMPMobileApp demo - refactoring the StopWatch composable as a KMP virtual circuit using FlowGraph and RootControlNode."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - StopWatch FlowGraph Creation (Priority: P1)

A developer opens the graphEditor and creates a new FlowGraph representing the StopWatch virtual circuit. The graph contains two CodeNodes connected by two Integer connections (elapsedSeconds, elapsedMinutes).

**Why this priority**: The FlowGraph is the foundation of the virtual circuit. Without it, the KMP module cannot be generated.

**Independent Test**: Can be fully tested by creating the FlowGraph in the graphEditor, saving it as a .flow.kts file, and verifying the structure contains the correct nodes and connections.

**Acceptance Scenarios**:

1. **Given** an empty graphEditor canvas, **When** the developer creates a new FlowGraph named "StopWatch", **Then** a blank graph with metadata is initialized.
2. **Given** a StopWatch FlowGraph, **When** the developer adds a CodeNode "TimerEmitter" with 0 inputs and 2 outputs (elapsedSeconds: Int, elapsedMinutes: Int), **Then** the node appears with two output ports.
3. **Given** a StopWatch FlowGraph with TimerEmitter, **When** the developer adds a CodeNode "DisplayReceiver" with 2 inputs (seconds: Int, minutes: Int) and 0 outputs, **Then** the node appears with two input ports.
4. **Given** both nodes exist, **When** the developer connects TimerEmitter.elapsedSeconds to DisplayReceiver.seconds AND TimerEmitter.elapsedMinutes to DisplayReceiver.minutes, **Then** two connections are visible on the canvas.
5. **Given** the complete FlowGraph, **When** the developer saves the graph, **Then** a valid .flow.kts file is produced that can be reloaded.

---

### User Story 2 - KMP Module Generation from FlowGraph (Priority: P1)

A developer compiles the StopWatch FlowGraph into a KMP module that can be integrated with the existing KMPMobileApp project.

**Why this priority**: Module generation is essential to demonstrate the virtual circuit concept—transforming a visual flow into executable code.

**Independent Test**: Can be fully tested by using the Compile button to generate a module, then verifying the output directory contains a valid KMP module structure with build.gradle.kts and source files.

**Acceptance Scenarios**:

1. **Given** a saved StopWatch FlowGraph, **When** the developer clicks the Compile button and selects an output directory, **Then** a KMP module named "StopWatch" is generated.
2. **Given** the generated module, **When** examining the directory structure, **Then** it contains: build.gradle.kts, src/commonMain/kotlin/[package]/StopWatchFlowGraph.kt, and src/commonMain/kotlin/[package]/StopWatchController.kt.
3. **Given** the generated StopWatchController, **When** examining the code, **Then** it wraps RootControlNode and exposes start(), pause(), stop(), and getStatus() methods.
4. **Given** the generated module, **When** added as a dependency to KMPMobileApp, **Then** it compiles without errors.

---

### User Story 3 - StopWatch Composable Integration (Priority: P1)

A developer refactors the existing StopWatch composable to use the generated StopWatch module, replacing the LaunchedEffect timing logic with RootControlNode execution control.

**Why this priority**: This is the final demonstration of the virtual circuit concept—the composable UI controls the flow execution and receives data from it.

**Independent Test**: Can be fully tested by running the refactored KMPMobileApp on Android/iOS and verifying the stopwatch starts, stops, and displays elapsed time correctly.

**Acceptance Scenarios**:

1. **Given** the refactored StopWatch composable, **When** the user taps the Start button, **Then** the RootControlNode transitions to RUNNING state and time begins counting.
2. **Given** a running stopwatch, **When** the user taps the Stop button, **Then** the RootControlNode transitions to IDLE state and time stops counting.
3. **Given** a running stopwatch, **When** 60 seconds have elapsed, **Then** elapsedMinutes increments and elapsedSeconds resets to 0.
4. **Given** a stopped stopwatch with elapsed time, **When** the user taps Reset, **Then** both elapsedSeconds and elapsedMinutes reset to 0.
5. **Given** the RootControlNode with speedAttenuation=1000, **When** running, **Then** elapsed time updates at 1-second intervals (matching the original 1000ms delay).

---

### User Story 4 - UseCase Mapping (Priority: P2)

The generated CodeNode UseCases correctly map to the original composable logic: TimerEmitter replaces LaunchedEffect, and DisplayReceiver hosts StopWatchFace.

**Why this priority**: UseCase mapping demonstrates how existing composable logic decomposes into FBP nodes, but the core circuit works without complete UseCase implementation.

**Independent Test**: Can be tested by verifying the generated UseCase stubs have correct signatures matching the original composable functions.

**Acceptance Scenarios**:

1. **Given** the TimerEmitter CodeNode UseCase, **When** the node is RUNNING, **Then** it emits elapsedSeconds and elapsedMinutes values on each tick (respecting speedAttenuation).
2. **Given** the DisplayReceiver CodeNode UseCase, **When** it receives seconds and minutes inputs, **Then** it renders the StopWatchFace composable with those values.
3. **Given** the RootControlNode executionState is RUNNING, **When** mapped to isRunning, **Then** the StopWatchFace displays the running indicator.
4. **Given** the RootControlNode executionState is IDLE, **When** mapped to isRunning, **Then** the StopWatchFace displays the stopped indicator.

---

### Edge Cases

- What happens when the user rapidly toggles Start/Stop?
  - The RootControlNode state transitions immediately; no buffering delays.
- What happens if module generation fails due to invalid FlowGraph?
  - Validation errors are displayed before compilation proceeds.
- What happens when speedAttenuation is 0?
  - Timer ticks as fast as possible (no artificial delay).
- How does the stopwatch handle hour overflow (elapsedMinutes >= 60)?
  - For this demo, hours are not tracked; minutes continue incrementing indefinitely.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow creation of a FlowGraph named "StopWatch" containing two connected CodeNodes.
- **FR-002**: System MUST support a CodeNode with 0 inputs and 2 Integer outputs (elapsedSeconds, elapsedMinutes).
- **FR-003**: System MUST support a CodeNode with 2 Integer inputs (seconds, minutes) and 0 outputs.
- **FR-004**: System MUST generate connections between output and input ports of matching Integer type.
- **FR-005**: System MUST compile a FlowGraph into a KMP module with Controller and FlowGraph classes.
- **FR-006**: System MUST map RootControlNode.executionState to isRunning Boolean (RUNNING = true, else false).
- **FR-007**: System MUST map RootControlNode.controlConfig.speedAttenuation to the timer tick interval in milliseconds.
- **FR-008**: Start button MUST call RootControlNode.startAll() to set executionState to RUNNING.
- **FR-009**: Stop button MUST call RootControlNode.stopAll() to set executionState to IDLE.
- **FR-010**: Reset button MUST reset elapsedSeconds and elapsedMinutes to 0 in addition to stopping.
- **FR-011**: Generated module MUST compile as a KMP module compatible with the existing KMPMobileApp project.
- **FR-012**: TimerEmitter UseCase MUST emit incrementing elapsedSeconds and roll over to elapsedMinutes at 60.

### Key Entities

- **StopWatch FlowGraph**: The visual graph containing TimerEmitter and DisplayReceiver nodes with their connections.
- **TimerEmitter CodeNode**: A node with 0 inputs, 2 outputs (elapsedSeconds: Int, elapsedMinutes: Int). Its UseCase contains the timing logic from the original LaunchedEffect.
- **DisplayReceiver CodeNode**: A node with 2 inputs (seconds: Int, minutes: Int), 0 outputs. Its UseCase wraps StopWatchFace composable.
- **StopWatchController**: Generated wrapper around RootControlNode exposing execution control methods (start, stop, pause, getStatus).
- **elapsedSeconds Connection**: Integer connection from TimerEmitter to DisplayReceiver carrying the current second value.
- **elapsedMinutes Connection**: Integer connection from TimerEmitter to DisplayReceiver carrying the current minute value.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developer can create, save, and reload the StopWatch FlowGraph within 5 minutes.
- **SC-002**: Generated KMP module compiles successfully when added to KMPMobileApp.
- **SC-003**: Refactored StopWatch composable displays correct elapsed time (matching original behavior within 1 second accuracy).
- **SC-004**: Start/Stop button correctly controls timer execution (visually verified on device/emulator).
- **SC-005**: Timer increments elapsedMinutes when elapsedSeconds reaches 60.
- **SC-006**: speedAttenuation of 1000 results in 1-second tick intervals (±100ms tolerance).

## Assumptions

- The existing graphEditor, fbpDsl, and kotlinCompiler modules are stable and provide the necessary infrastructure for FlowGraph creation and module generation.
- The KMPMobileApp project is configured to accept additional KMP module dependencies.
- The ModuleGenerator produces valid Kotlin code that compiles with the current Kotlin/Compose versions.
- Integer connection types are already supported by the connection model.
- The StopWatchFace composable remains unchanged; only the parent StopWatch composable is refactored.
