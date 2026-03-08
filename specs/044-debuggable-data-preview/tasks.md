# Tasks: Debuggable Data Runtime Preview

**Input**: Design documents from `/specs/044-debuggable-data-preview/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Not requested — no test tasks included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No project initialization needed — all target modules already exist.

(No tasks — existing project structure is sufficient.)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before user story work begins.

- [ ] T001 Create `DataFlowDebugger` class with per-connection snapshot storage (`Map<String, MutableStateFlow<Any?>>`) in `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/DataFlowDebugger.kt`
- [ ] T002 Add `onEmitValue: ((String, Int, Any?) -> Unit)?` callback to `NodeRuntime` base class in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeRuntime.kt`
- [ ] T003 Update all runtime emission sites to pass the emitted value to `onEmitValue` callback alongside existing `onEmit` calls — files in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/` (SourceRuntime.kt, SourceOut2Runtime.kt, SourceOut3Runtime.kt, TransformerRuntime.kt, FilterRuntime.kt, In1Out2Runtime.kt, In1Out3Runtime.kt, In2Out1Runtime.kt, In2Out2Runtime.kt, In2Out3Runtime.kt, In3Out1Runtime.kt, In3Out2Runtime.kt, In3Out3Runtime.kt, In2AnyOut1Runtime.kt, In2AnyOut2Runtime.kt, In2AnyOut3Runtime.kt, In3AnyOut1Runtime.kt, In3AnyOut2Runtime.kt, In3AnyOut3Runtime.kt)

**Checkpoint**: DataFlowDebugger exists and runtimes can report emitted values via callback.

---

## Phase 3: User Story 1 - Inspect Connection Data When Paused (Priority: P1) MVP

**Goal**: When paused with animation enabled, selecting a connection shows the most recent data value in the Properties panel.

**Independent Test**: Run StopWatch module with attenuation 1000ms and "Animate Data Flow" enabled. Start, wait several ticks, Pause. Click a connection between TimerEmitter and TimeIncrementer. Verify the Properties panel shows the most recent seconds value below connection properties.

### Implementation for User Story 1

- [ ] T004 [US1] Wire `DataFlowDebugger` into `RuntimeSession` — instantiate debugger, create emission value observer using `nodePortToConnections` mapping pattern from `DataFlowAnimationController`, and assign `onEmitValue` callbacks on runtimes during start. File: `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt`
- [ ] T005 [US1] Expose debugger snapshot state from `RuntimeSession` so it can be passed to the UI composable chain. File: `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt`
- [ ] T006 [US1] Pass debugger snapshot state through composable parameters to `PropertiesPanel` in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [ ] T007 [US1] Display captured transit snapshot value in the Properties panel below existing connection properties when a connection is selected and execution is paused. Show value via `toString()` in a read-only text area. Show "No data captured" when no snapshot exists. File: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`

**Checkpoint**: US1 complete — pausing and selecting a connection shows the most recent data value.

---

## Phase 4: User Story 2 - Data Capture Tied to Animation Toggle (Priority: P2)

**Goal**: Data capture is only active when "Animate Data Flow" is enabled. No overhead when disabled. Snapshots cleared on stop or toggle-off.

**Independent Test**: Run a module with "Animate Data Flow" disabled. Pause and click a connection — no snapshot data shown. Enable animation, run again, pause — data is captured and visible.

### Implementation for User Story 2

- [ ] T008 [US2] Ensure `DataFlowDebugger` only captures values when `animateDataFlow` is true — guard the `onEmitValue` callback assignment so it is only wired when animation is enabled. File: `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt`
- [ ] T009 [US2] Clear all snapshots in `DataFlowDebugger` when `stop()` is called or when `animateDataFlow` is toggled off. File: `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/RuntimeSession.kt` and `circuitSimulator/src/commonMain/kotlin/io/codenode/circuitsimulator/DataFlowDebugger.kt`

**Checkpoint**: US2 complete — no capture overhead when animation disabled; snapshots cleared on stop/toggle-off.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validation and edge case handling.

- [ ] T010 Handle large data values gracefully — truncate `toString()` output if it exceeds a reasonable length (e.g., 500 characters) in the Properties panel display. File: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [ ] T011 Run quickstart.md verification scenarios (StopWatch, UserProfiles, No Debug Mode) to validate end-to-end behavior

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No external dependencies — can start immediately
- **US1 (Phase 3)**: Depends on T001, T002, T003 (Foundational)
- **US2 (Phase 4)**: Depends on T004, T005 (US1 RuntimeSession wiring)
- **Polish (Phase 5)**: Depends on all user stories complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational phase only
- **User Story 2 (P2)**: Depends on US1 (shares RuntimeSession lifecycle wiring)

### Within Each User Story

- RuntimeSession wiring before UI display
- Core implementation before integration

### Parallel Opportunities

- T002 and T001 can run in parallel (different modules: fbpDsl vs circuitSimulator)
- T006 and T007 can run in parallel (different files: Main.kt vs PropertiesPanel.kt)
- T008 and T009 touch the same files as T004/T005, so must be sequential after US1

---

## Parallel Example: Foundational Phase

```bash
# These can run in parallel (different modules):
Task T001: "Create DataFlowDebugger in circuitSimulator/.../DataFlowDebugger.kt"
Task T002: "Add onEmitValue callback to NodeRuntime in fbpDsl/.../NodeRuntime.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (T001-T003)
2. Complete Phase 3: User Story 1 (T004-T007)
3. **STOP and VALIDATE**: Test with StopWatch module — pause and inspect connection data
4. Deploy/demo if ready

### Incremental Delivery

1. Foundational → DataFlowDebugger + onEmitValue callback ready
2. Add User Story 1 → Inspect connection data when paused (MVP!)
3. Add User Story 2 → Conditional capture, lifecycle cleanup
4. Polish → Edge cases, validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- T003 is the largest task (touches ~19 runtime files) but follows a consistent 1-line pattern per emission site
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
