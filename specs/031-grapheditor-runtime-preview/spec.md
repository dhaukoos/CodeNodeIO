# Feature Specification: GraphEditor Runtime Preview

**Feature Branch**: `031-grapheditor-runtime-preview`
**Created**: 2026-02-25
**Status**: Draft
**Input**: User description: "Support runtime preview execution of a module in the graphEditor. Add a pane with runtime controls (start, stop, pause, speedAttenuation) for the executionState of the flowGraph. Below that controls pane, add a pane that contains rendering of the UI composables defined in the module. When the Start button is pressed, the flowgraph executes as it would in the app. Use the StopWatch module to prove out this design feature. Use the circuitSimulator module to contain the new code to support this plan."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Runtime Execution Controls (Priority: P1)

As a flow designer, I want to start, stop, and pause the execution of my flow graph directly within the graphEditor so that I can test my flow logic without switching to a separate application.

The graphEditor displays a runtime controls pane that provides Start, Stop, Pause, and Resume buttons. The controls reflect the current execution state of the flow graph (Idle, Running, Paused). A speed attenuation control allows me to slow down execution by adding delay (in milliseconds) to each tick cycle, making it easier to observe behavior step by step.

**Why this priority**: Without execution controls, there is no way to run the flow graph in the editor. This is the foundational capability that all other runtime preview features depend on.

**Independent Test**: Can be fully tested by opening a saved module (e.g., StopWatch), pressing Start, observing the execution state changes (Idle to Running), pressing Pause (Running to Paused), pressing Resume (Paused to Running), and pressing Stop (Running to Idle). Speed attenuation can be verified by increasing the added delay and observing the tick rate slow down.

**Acceptance Scenarios**:

1. **Given** a module is loaded in the graphEditor, **When** the user presses Start, **Then** the flow graph begins executing and the execution state indicator shows "Running"
2. **Given** the flow graph is running, **When** the user presses Pause, **Then** execution suspends and the state indicator shows "Paused"
3. **Given** the flow graph is paused, **When** the user presses Resume, **Then** execution resumes and the state indicator shows "Running"
4. **Given** the flow graph is running or paused, **When** the user presses Stop, **Then** execution halts, all state resets, and the indicator shows "Idle"
5. **Given** the flow graph is running with attenuation at 0ms, **When** the user increases attenuation to 500ms, **Then** each tick cycle has 500ms of additional delay added, visibly slowing execution
6. **Given** the flow graph is running with attenuation at 500ms, **When** the user decreases attenuation back to 0ms, **Then** execution returns to nominal speed (no added delay)

---

### User Story 2 - Live UI Preview (Priority: P2)

As a flow designer, I want to see the module's UI composables rendered live within the graphEditor so that I can observe how the running flow graph drives the visual output without deploying to a device.

Below the runtime controls pane, a preview pane renders the module's UI composables. When the flow graph is executing, the preview updates in real time as the flow produces new state values. For the StopWatch module, this means the analog clock face animates and the digital time display counts up.

**Why this priority**: The live UI preview is what makes runtime execution visible and useful. Without it, the designer can only observe execution state changes in the controls pane with no visual feedback of the module's actual behavior.

**Independent Test**: Can be tested by loading the StopWatch module, pressing Start, and observing the StopWatch face rendering in the preview pane with the seconds hand moving and the digital timer counting up. Pressing Pause should freeze the display, and Resume should continue from where it left off.

**Acceptance Scenarios**:

1. **Given** a module with UI composables is loaded, **When** the preview pane is visible, **Then** the module's UI composables render in the preview pane area
2. **Given** the StopWatch module is loaded and running, **When** the timer ticks, **Then** the analog clock face updates (seconds hand moves) and the digital display increments
3. **Given** the flow graph is paused, **When** the user views the preview pane, **Then** the UI displays the frozen state at the moment of pause
4. **Given** the flow graph is stopped, **When** the user views the preview pane, **Then** the UI displays the initial/reset state (00:00 for StopWatch)

---

### User Story 3 - StopWatch Module Proof of Concept (Priority: P3)

As a developer, I want the StopWatch module to serve as the reference implementation for the runtime preview feature so that the design pattern is proven before extending to other modules.

The StopWatch module is used end-to-end to validate the runtime preview architecture. When loaded in the graphEditor, the StopWatch's controller, view model, and UI composables are instantiated within the circuitSimulator module infrastructure and rendered in the preview pane. The full lifecycle (start, count, pause, resume, stop, reset) works identically to how it behaves in the mobile app.

**Why this priority**: This validates the entire architecture. Once StopWatch works correctly in the preview, the pattern can be generalized for other modules.

**Independent Test**: Can be tested by loading the StopWatch module in the graphEditor, starting execution, letting it run for over 60 seconds to verify minute rollover, pausing and resuming, adjusting speed attenuation, and stopping with reset. All behaviors should match the mobile app's StopWatch behavior.

**Acceptance Scenarios**:

1. **Given** the StopWatch module is loaded in the graphEditor, **When** the user presses Start and waits, **Then** the seconds count from 0 to 59 and minutes increment at 60-second rollover, matching mobile app behavior
2. **Given** the StopWatch is running at 0ms attenuation, **When** the user increases attenuation to 2000ms, **Then** the clock visibly slows down (each tick takes approximately 3 seconds instead of 1 second)
3. **Given** the StopWatch is paused at 01:23, **When** the user presses Resume, **Then** the clock continues from 01:23
4. **Given** the StopWatch is stopped, **When** the user presses Start again, **Then** the clock starts from 00:00

---

### Edge Cases

- What happens when the user edits the flow graph while it is running? Execution should stop automatically and the state should reset to Idle.
- What happens when a module has no UI composables? The preview pane should display a message indicating no preview is available, while runtime controls still function.
- What happens when the user switches to a different module while execution is running? The running execution should stop before loading the new module.
- What happens when speed attenuation is set to 0ms? This is the maximum speed (nominal execution), which is the default and valid state.
- What happens when speed attenuation is set extremely high (e.g., 10000ms)? The maximum added delay should be clamped to a reasonable upper bound (e.g., 5000ms) to prevent the system from appearing frozen.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The graphEditor MUST display a runtime controls pane containing Start, Stop, Pause, and Resume buttons
- **FR-002**: The runtime controls MUST reflect the current execution state (Idle, Running, Paused) and only enable contextually valid buttons (e.g., Pause is only enabled when Running)
- **FR-003**: The graphEditor MUST display a preview pane below the controls pane that renders the module's UI composables
- **FR-004**: The system MUST execute the flow graph's runtime (controller, flow, node runtimes) when Start is pressed, producing the same behavior as the mobile app
- **FR-005**: The system MUST provide a speed attenuation control specified in milliseconds of added delay per tick cycle, with a range of 0ms (nominal speed, the maximum) to 5000ms, defaulting to 0ms
- **FR-006**: The preview pane MUST update in real time as the flow graph produces new state values during execution
- **FR-007**: The system MUST stop execution and reset state when the user edits the flow graph while it is running
- **FR-008**: The circuitSimulator module MUST contain the runtime orchestration logic that connects the graphEditor controls to the module's generated controller and UI composables
- **FR-009**: The system MUST support the StopWatch module as the initial proof-of-concept, with full lifecycle behavior (start, stop, pause, resume, reset, speed control)
- **FR-010**: The runtime controls and preview pane MUST be positioned within the graphEditor layout without obscuring the main canvas or properties panel

### Key Entities

- **Runtime Session**: Represents an active execution of a module's flow graph within the graphEditor. Has an execution state (Idle, Running, Paused), a speed attenuation value, and a reference to the module's controller and UI composables.
- **Speed Attenuation**: An added delay in milliseconds injected into each tick cycle of timed generators. A value of 0ms means nominal speed (the maximum, no added delay). Higher values slow execution proportionally (e.g., 1000ms adds one full second of delay per tick). Range: 0ms to 5000ms.
- **Preview Pane**: A designated area in the graphEditor that hosts the rendered output of a module's UI composables, driven by the running flow graph's state.

## Assumptions

- The graphEditor already has a saved/compiled module available to load (i.e., the module's generated files exist, including the controller, flow, view model, and UI composables).
- The circuitSimulator module already exists in the project structure and can be extended with new functionality.
- The StopWatch module's generated code (StopWatchController, StopWatchFlow, StopWatchViewModel) and UI composables (StopWatch, StopWatchFace) are fully functional and compile correctly.
- Speed attenuation adds delay to timed generators' tick cycles. A value of 0ms means no added delay (nominal/maximum speed). There is no way to speed up execution beyond nominal speed because the nominal rate depends on the logic in the processing loop. Non-timed nodes process messages as fast as they arrive regardless of attenuation setting.
- The runtime preview pane renders composables using the same desktop rendering capabilities already used by the graphEditor itself.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A flow designer can start, pause, resume, and stop a StopWatch module execution within the graphEditor in under 5 seconds per action
- **SC-002**: The live preview displays the StopWatch face with seconds hand movement that matches the expected tick rate within 100ms accuracy at 0ms attenuation (nominal speed)
- **SC-003**: Speed attenuation changes take effect within 1 second of adjustment
- **SC-004**: 100% of StopWatch lifecycle scenarios (start, count, minute rollover, pause, resume, stop, reset) produce identical behavior to the mobile app
- **SC-005**: Editing the flow graph while running automatically stops execution within 1 second
- **SC-006**: The runtime preview pane renders without causing the graphEditor canvas to lose responsiveness or resize unexpectedly
