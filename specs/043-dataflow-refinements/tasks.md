# Tasks: Module DataFlow Refinements

**Input**: Design documents from `/specs/043-dataflow-refinements/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: User Story 1 - UserProfiles Selective Channel Output (Priority: P1)

**Goal**: Only the triggered action's output channel emits data — pressing Add sends data on the save channel only, not on all three channels.

**Independent Test**: Run app, open UserProfiles module, set attenuation to 1000ms, enable "Animate Data Flow", press Start, press Add — only 1 connection shows an animated dot, not all 3.

### Implementation for User Story 1

- [x] T001 [US1] Replace `combine()` source with individual StateFlow collectors in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/UserProfilesFlow.kt` — each collector emits `ProcessResult3` with only the changed value (others null)
- [x] T002 [US1] Change processor from `In3Out2` to `In3AnyOut2` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/UserProfilesFlow.kt` — use `CodeNodeFactory.createIn3AnyOut2Processor()` with `Unit` initial values
- [x] T003 [US1] Update `start()` to match new flow pattern in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/UserProfilesController.kt` — wire connections for the new processor type
- [x] T004 [US1] Adapt process block for any-input semantics in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/processingLogic/UserProfileRepositoryProcessLogic.kt` — handle receiving data on any single input channel independently

**Checkpoint**: UserProfiles selective channel emission works — only the triggered action's channel emits data.

---

## Phase 2: User Story 2 - StopWatch Minutes-Only-On-Change Emission (Priority: P2)

**Goal**: The minutes output only emits when the minutes value actually changes (at 59→0 rollover), not on every tick.

**Independent Test**: Run app, open StopWatch module, set attenuation to 1000ms, enable "Animate Data Flow", press Start — seconds connection shows dots every tick; minutes connection shows dot only at 59→0 rollover.

### Implementation for User Story 2

- [x] T005 [US2] Use `ProcessResult2.first()` when minutes unchanged in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/processingLogic/TimeIncrementerProcessLogic.kt` — compare `newMinutes != elapsedMinutes` and use `ProcessResult2.both()` only when minutes changes, otherwise `ProcessResult2.first(newSeconds)`

**Checkpoint**: StopWatch minutes output only emits on actual minute changes.

---

## Phase 3: Polish & Cross-Cutting Concerns

- [x] T006 Run `./gradlew :StopWatch:compileKotlinJvm :UserProfiles:compileKotlinJvm` to verify both modules compile
- [x] T007 Run quickstart.md manual verification for both modules

---

## Dependencies & Execution Order

### Phase Dependencies

- **User Story 1 (Phase 1)**: No dependencies — can start immediately
- **User Story 2 (Phase 2)**: No dependencies — can start immediately, independent of US1
- **Polish (Phase 3)**: Depends on both user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: T001 → T002 → T003 → T004 (sequential — all modify related flow/controller files)
- **User Story 2 (P2)**: T005 (single task, no dependencies)
- **Cross-story**: US1 and US2 are fully independent (different modules)

### Parallel Opportunities

- US1 and US2 can be implemented in parallel (different modules: UserProfiles vs StopWatch)
- T006 compilation check covers both modules after all changes

---

## Parallel Example

```bash
# US1 and US2 can run in parallel (different modules):
Task: T001-T004 (UserProfiles module)
Task: T005 (StopWatch module)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: UserProfiles selective channel output (T001-T004)
2. **STOP and VALIDATE**: Test UserProfiles independently
3. Proceed to US2

### Incremental Delivery

1. Implement US1 (UserProfiles) → Test independently
2. Implement US2 (StopWatch) → Test independently
3. Polish: compile check + quickstart validation

---

## Notes

- No new files are created — all changes modify existing files
- The `In3AnyOut2Runtime` and `ProcessResult2` selective output infrastructure already exist in fbpDsl
- US1 tasks are sequential because T001-T002 modify the same file and T003-T004 depend on the new processor type
- US2 is a single 3-line change in one file
