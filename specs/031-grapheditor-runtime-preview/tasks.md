# Tasks: GraphEditor Runtime Preview

**Input**: Design documents from `/specs/031-grapheditor-runtime-preview/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Dependency Wiring)

**Purpose**: Restructure module dependencies so graphEditor → circuitSimulator → fbpDsl + StopWatch

- [X] T001 Remove graphEditor dependency from circuitSimulator in `circuitSimulator/build.gradle.kts` (per R2: reverse the dependency direction)
- [X] T002 Add StopWatch module dependency to circuitSimulator in `circuitSimulator/build.gradle.kts`: `implementation(project(":StopWatch"))`
- [X] T003 Add Compose Desktop dependencies to circuitSimulator in `circuitSimulator/build.gradle.kts`: compose.runtime, compose.foundation, compose.material3, compose.ui (explicit 1.7.3 versions matching StopWatch)
- [X] T004 Add lifecycle-viewmodel-compose 2.8.0 dependency to circuitSimulator in `circuitSimulator/build.gradle.kts`
- [X] T005 Add circuitSimulator dependency to graphEditor in `graphEditor/build.gradle.kts`: `implementation(project(":circuitSimulator"))`
- [X] T006 Add StopWatch dependency to graphEditor in `graphEditor/build.gradle.kts`: `implementation(project(":StopWatch"))`
- [X] T007 Verify compilation: `./gradlew :circuitSimulator:compileKotlinJvm :graphEditor:compileKotlinJvm`

---

## Phase 2: Foundational (Attenuation Support in fbpDsl)

**Purpose**: Add `attenuationDelayMs` to NodeRuntime and wire it into timed generator delay loops. MUST complete before user stories.

- [X] T008 Add `var attenuationDelayMs: Long? = null` property to `NodeRuntime` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`
- [X] T009 Modify `createTimedGenerator` factory method in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` to use `delay(attenuationDelayMs ?: tickIntervalMs)` in the timed loop (the closure must capture `this` reference to read the mutable property from the runtime instance)
- [X] T010 Modify `createTimedOut2Generator` factory method in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` to use `delay(attenuationDelayMs ?: tickIntervalMs)` in the timed loop
- [X] T011 Modify `createTimedOut3Generator` factory method in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` to use `delay(attenuationDelayMs ?: tickIntervalMs)` in the timed loop
- [X] T012 Verify existing fbpDsl tests still pass: `./gradlew :fbpDsl:allTests`

**Checkpoint**: attenuationDelayMs property exists on NodeRuntime and is used by all timed generator factories. Existing behavior unchanged (null defaults to tickIntervalMs).

---

## Phase 3: User Story 1 - Runtime Execution Controls (Priority: P1)

**Goal**: Create RuntimeSession orchestrator and runtime controls UI. User can start/stop/pause/resume execution and adjust attenuation delay.

**Independent Test**: Open graphEditor, expand preview panel, verify Start/Stop/Pause/Resume buttons work with execution state transitions. Adjust attenuation slider and observe delay changes.

### Implementation for User Story 1

- [X] T013 [US1] Create `RuntimeSession` class in `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt` with: executionState (StateFlow<ExecutionState>), attenuationDelayMs (StateFlow<Long>), start(), stop(), pause(), resume(), setAttenuation(ms: Long), and CoroutineScope management. RuntimeSession instantiates StopWatchController with stopWatchFlowGraph, creates StopWatchViewModel, and propagates attenuationDelayMs to generator runtimes on start and on attenuation change.
- [X] T014 [US1] Create `RuntimePreviewPanel` composable in `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` with: collapsible panel (toggle button), execution state indicator text, Start/Stop/Pause/Resume buttons with contextual enable/disable (Start only when Idle, Pause only when Running, Resume only when Paused, Stop when Running or Paused), and attenuation slider (0ms to 5000ms range, labeled in ms). The panel calls RuntimeSession methods and observes its StateFlows.
- [X] T015 [US1] Integrate `RuntimePreviewPanel` into graphEditor layout in `graphEditor/src/jvmMain/kotlin/Main.kt`: Add to the right side of the main Row layout (after Properties panel), with a collapsible toggle. When collapsed, panel takes zero width.
- [X] T016 [US1] Verify compilation: `./gradlew :circuitSimulator:compileKotlinJvm :graphEditor:compileKotlinJvm`
- [X] T017 [US1] Manual verification: Launch graphEditor (`./gradlew :graphEditor:run`), expand preview panel, verify Start/Stop/Pause/Resume buttons transition execution state correctly, verify attenuation slider adjusts delay value

**Checkpoint**: Runtime controls pane is visible and functional. Execution state transitions work. Attenuation slider adjusts delay. No preview rendering yet.

---

## Phase 4: User Story 2 - Live UI Preview (Priority: P2)

**Goal**: Render StopWatch UI composables in the preview pane, driven by the running flow graph state.

**Independent Test**: Start the StopWatch via the controls, observe the analog clock face and digital timer updating in real time in the preview pane.

### Implementation for User Story 2

- [X] T018 [US2] Create `StopWatchPreviewProvider` in `graphEditor/src/jvmMain/kotlin/ui/StopWatchPreviewProvider.kt` that provides a `@Composable` function rendering StopWatchFace + digital time from the RuntimeSession's ViewModel state. Placed in graphEditor (not circuitSimulator) because Compose compiler plugin is required.
- [X] T019 [US2] Update `RuntimePreviewPanel` in `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` to add a preview area below the controls section that calls `StopWatchPreviewProvider`'s composable function. The preview area should fill remaining vertical space in the panel.
- [X] T020 [US2] Verify compilation: `./gradlew :circuitSimulator:compileKotlinJvm :graphEditor:compileKotlinJvm`
- [X] T021 [US2] Manual verification: Launch graphEditor, expand preview panel, press Start, verify the StopWatch face renders with moving seconds hand and incrementing digital timer. Verify Pause freezes the display, Resume continues, Stop resets to 00:00.

**Checkpoint**: Live preview pane renders StopWatch UI composables in real time, driven by flow graph execution.

---

## Phase 5: User Story 3 - StopWatch Proof of Concept (Priority: P3)

**Goal**: Validate full end-to-end StopWatch lifecycle in the preview, including auto-stop on graph edit.

**Independent Test**: Run StopWatch for >60 seconds to verify minute rollover, test all lifecycle transitions, test attenuation slider, test auto-stop on graph edit.

### Implementation for User Story 3

- [X] T022 [US3] Implement auto-stop on graph edit: In `graphEditor/src/jvmMain/kotlin/Main.kt` (or `RuntimePreviewPanel.kt`), observe the flowGraph state and call `RuntimeSession.stop()` when the graph is modified while execution is running (FR-007)
- [X] T023 [US3] Clamp attenuation slider range: In `RuntimeSession` or `RuntimePreviewPanel`, ensure attenuationDelayMs is clamped to [0, 5000] range (FR-005)
- [X] T024 [US3] Manual verification - full lifecycle: Launch graphEditor, run quickstart.md Scenario 1 (start/stop), Scenario 2 (pause/resume), Scenario 3 (speed attenuation at 0ms, 1000ms, 3000ms), Scenario 4 (minute rollover), Scenario 5 (edit while running auto-stops)
- [X] T025 [US3] Manual verification - panel toggle: Run quickstart.md Scenario 6 (collapse/expand preview panel without losing state)

**Checkpoint**: All StopWatch lifecycle scenarios pass. Auto-stop on edit works. Attenuation range is clamped. Full PoC validated.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup, edge cases, and final validation

- [X] T026 Handle "no UI composables" edge case: When a module lacks userInterface composables, show "No preview available" in preview pane while controls still function (quickstart Scenario 7)
- [X] T027 Run all automated tests to verify no regressions: `./gradlew :fbpDsl:allTests :circuitSimulator:jvmTest :graphEditor:jvmTest`
- [X] T028 Run full quickstart.md validation (all 7 scenarios)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - dependency wiring first
- **Foundational (Phase 2)**: Depends on Phase 1 - attenuationDelayMs in fbpDsl
- **User Story 1 (Phase 3)**: Depends on Phase 2 - needs attenuationDelayMs on NodeRuntime
- **User Story 2 (Phase 4)**: Depends on Phase 3 - needs RuntimeSession and controls panel to exist
- **User Story 3 (Phase 5)**: Depends on Phase 4 - needs live preview to validate full PoC
- **Polish (Phase 6)**: Depends on Phase 5

### Within Each Phase

- Setup tasks T001-T006 are sequential (dependency chain in build files)
- Foundational tasks T009-T011 are parallel [P] (different factory methods in same file, but logically independent patterns)
- US1 tasks are sequential (RuntimeSession before panel before layout integration)
- US2 tasks are sequential (provider before panel update)
- US3 tasks T022-T023 are parallel [P] (different concerns)

### Parallel Opportunities

- T009, T010, T011 can be applied in parallel (same file but independent method modifications)
- T022 and T023 can run in parallel (auto-stop vs clamping are independent concerns)

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (dependency wiring)
2. Complete Phase 2: Foundational (attenuationDelayMs in fbpDsl)
3. Complete Phase 3: User Story 1 (RuntimeSession + controls panel)
4. **STOP and VALIDATE**: Verify execution controls work without preview rendering

### Incremental Delivery

1. Setup + Foundational → Attenuation support ready
2. Add US1 → Controls work → Demo execution state transitions
3. Add US2 → Live preview → Demo StopWatch face rendering
4. Add US3 → Full PoC validated → Demo all quickstart scenarios

---

## Notes

- T009-T011 modify the same file (CodeNodeFactory.kt) but different methods — apply sequentially to avoid merge conflicts
- The closure in timed generator factories must capture a reference that allows reading the runtime's `attenuationDelayMs` at delay time, not at creation time
- circuitSimulator's existing `CircuitSimulator.kt` stub may need updating after removing the graphEditor dependency (T001)
- Compose dependencies in circuitSimulator should use explicit 1.7.3 versions to match StopWatch module, avoiding compileSdk 35 issues discovered in feature 030
