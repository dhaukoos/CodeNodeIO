# Tasks: Migrate Module Runtimes

**Input**: Design documents from `/specs/052-migrate-module-runtimes/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested in the spec. Tests omitted. Existing test suite must continue passing (SC-005).

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: User Story 1 — StopWatch Runs via Dynamic Pipeline (Priority: P1) 🎯 MVP

**Goal**: Create self-contained CodeNodeDefinitions for all 3 StopWatch nodes, register them in the node registry via ServiceLoader, and update ModuleSessionFactory so StopWatch runs via DynamicPipelineController instead of the generated StopWatchController/StopWatchFlow.

**Independent Test**: Load StopWatch → Start → verify timer ticks (seconds increment, minutes roll at 60) → Pause/Resume → Stop/Start resets → all via dynamic pipeline.

### Implementation for User Story 1

- [ ] T001 [P] [US1] Create `TimerEmitterCodeNode` object implementing `CodeNodeDefinition` in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/TimerEmitterCodeNode.kt` — category SOURCE, 0 inputs, 2 outputs (elapsedSeconds: Int, elapsedMinutes: Int), `createRuntime()` returns `CodeNodeFactory.createSourceOut2<Int, Int>` that combines `StopWatchState._elapsedSeconds` and `_elapsedMinutes` flows with `.drop(1)` and emits `ProcessResult2.both()` (same logic as `StopWatchFlow.timerEmitter`)
- [ ] T002 [P] [US1] Create `TimeIncrementerCodeNode` object implementing `CodeNodeDefinition` in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/TimeIncrementerCodeNode.kt` — category PROCESSOR, 2 inputs (elapsedSeconds: Int, elapsedMinutes: Int), 2 outputs (seconds: Int, minutes: Int), `createRuntime()` returns `CodeNodeFactory.createIn2Out2Processor<Int, Int, Int, Int>` embedding the `timeIncrementerTick` logic directly (increment seconds, roll at 60, update StopWatchState)
- [ ] T003 [P] [US1] Create `DisplayReceiverCodeNode` object implementing `CodeNodeDefinition` in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/nodes/DisplayReceiverCodeNode.kt` — category SINK, 2 inputs (seconds: Int, minutes: Int), 0 outputs, `createRuntime()` returns `CodeNodeFactory.createSinkIn2Any<Int, Int>` with initialValue1=0, initialValue2=0 that updates `StopWatchState._seconds` and `_minutes` (same logic as `StopWatchFlow.displayReceiver`)
- [ ] T004 [US1] Create ServiceLoader registration file at `StopWatch/src/jvmMain/resources/META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition` listing all 3 StopWatch CodeNodeDefinition fully-qualified class names (one per line)
- [ ] T005 [US1] Update `ModuleSessionFactory.kt` in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` to create a dynamic session for StopWatch when `canBuildDynamic()` returns true — create `DynamicPipelineController` with flowGraphProvider and lookup, create adapter implementing `StopWatchControllerInterface`, create `StopWatchViewModel` with adapter, return `RuntimeSession` with controller, viewModel, flowGraph, flowGraphProvider
- [ ] T006 [US1] Verify StopWatch migration per quickstart.md Step 2 — load StopWatch, Start, verify ticking, Pause/Resume, Stop/Start reset, enable attenuation + animation, confirm dynamic pipeline path is used

**Checkpoint**: StopWatch runs via dynamic pipeline — identical behavior to generated controller. MVP delivered.

---

## Phase 2: User Story 2 — Entity Modules Run via Dynamic Pipeline (Priority: P2)

**Goal**: Create self-contained CodeNodeDefinitions for all 9 entity module nodes (3 each for UserProfiles, GeoLocations, Addresses), register via ServiceLoader, and update ModuleSessionFactory so all entity modules run via DynamicPipelineController.

**Independent Test**: Load UserProfiles (or GeoLocations or Addresses) → Start → add/update/remove entity → verify persistence and display → all via dynamic pipeline.

### UserProfiles CodeNodes

- [ ] T007 [P] [US2] Create `UserProfileCUDCodeNode` object implementing `CodeNodeDefinition` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/nodes/UserProfileCUDCodeNode.kt` — category SOURCE, 0 inputs, 3 outputs (save: Any, update: Any, remove: Any), `createRuntime()` returns `CodeNodeFactory.createSourceOut3<Any, Any, Any>` that collects from `UserProfilesState._save`, `_update`, `_remove` with `.drop(1)`, emits `ProcessResult3` selectively, resets state to null after emission (same logic as `createUserProfileCUD()`)
- [ ] T008 [P] [US2] Create `UserProfileRepositoryCodeNode` object implementing `CodeNodeDefinition` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/nodes/UserProfileRepositoryCodeNode.kt` — category PROCESSOR, 3 inputs (save: Any, update: Any, remove: Any), 2 outputs (result: Any, error: Any), `createRuntime()` returns `CodeNodeFactory.createIn3AnyOut2Processor<Any, Any, Any, Any, Any>` with initialValues=Unit, embedding the `userProfileRepositoryTick` logic with identity tracking vars (`lastSaveRef`, `lastUpdateRef`, `lastRemoveRef`) as closure-scoped locals, accessing DAO via `UserProfilesPersistence.dao`
- [ ] T009 [P] [US2] Create `UserProfilesDisplayCodeNode` object implementing `CodeNodeDefinition` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/nodes/UserProfilesDisplayCodeNode.kt` — category SINK, 2 inputs (result: Any, error: Any), 0 outputs, `createRuntime()` returns `CodeNodeFactory.createSinkIn2<Any, Any>` that updates `UserProfilesState._result` and `_error` (same logic as `createUserProfilesDisplay()`)
- [ ] T010 [US2] Create ServiceLoader registration file at `UserProfiles/src/jvmMain/resources/META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition` listing all 3 UserProfiles CodeNodeDefinition fully-qualified class names

### GeoLocations CodeNodes

- [ ] T011 [P] [US2] Create `GeoLocationCUDCodeNode` object implementing `CodeNodeDefinition` in `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/nodes/GeoLocationCUDCodeNode.kt` — category SOURCE, 0 inputs, 3 outputs (save: Any, update: Any, remove: Any), `createRuntime()` returns `CodeNodeFactory.createSourceOut3<Any, Any, Any>` collecting from `GeoLocationsState._save`, `_update`, `_remove` (same pattern as UserProfileCUDCodeNode)
- [ ] T012 [P] [US2] Create `GeoLocationRepositoryCodeNode` object implementing `CodeNodeDefinition` in `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/nodes/GeoLocationRepositoryCodeNode.kt` — category PROCESSOR, 3 inputs, 2 outputs, `createRuntime()` returns `CodeNodeFactory.createIn3AnyOut2Processor<Any, Any, Any, Any, Any>` embedding `geoLocationRepositoryTick` logic with closure-scoped identity tracking, accessing DAO via `GeoLocationsPersistence.dao`
- [ ] T013 [P] [US2] Create `GeoLocationsDisplayCodeNode` object implementing `CodeNodeDefinition` in `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/nodes/GeoLocationsDisplayCodeNode.kt` — category SINK, 2 inputs, 0 outputs, `createRuntime()` returns `CodeNodeFactory.createSinkIn2<Any, Any>` updating `GeoLocationsState._result` and `_error`
- [ ] T014 [US2] Create ServiceLoader registration file at `GeoLocations/src/jvmMain/resources/META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition` listing all 3 GeoLocations CodeNodeDefinition fully-qualified class names

### Addresses CodeNodes

- [ ] T015 [P] [US2] Create `AddressCUDCodeNode` object implementing `CodeNodeDefinition` in `Addresses/src/commonMain/kotlin/io/codenode/addresses/nodes/AddressCUDCodeNode.kt` — category SOURCE, 0 inputs, 3 outputs (save: Any, update: Any, remove: Any), `createRuntime()` returns `CodeNodeFactory.createSourceOut3<Any, Any, Any>` collecting from `AddressesState._save`, `_update`, `_remove` (same pattern as UserProfileCUDCodeNode)
- [ ] T016 [P] [US2] Create `AddressRepositoryCodeNode` object implementing `CodeNodeDefinition` in `Addresses/src/commonMain/kotlin/io/codenode/addresses/nodes/AddressRepositoryCodeNode.kt` — category PROCESSOR, 3 inputs, 2 outputs, `createRuntime()` returns `CodeNodeFactory.createIn3AnyOut2Processor<Any, Any, Any, Any, Any>` embedding `addressRepositoryTick` logic with closure-scoped identity tracking, accessing DAO via `AddressesPersistence.dao`
- [ ] T017 [P] [US2] Create `AddressesDisplayCodeNode` object implementing `CodeNodeDefinition` in `Addresses/src/commonMain/kotlin/io/codenode/addresses/nodes/AddressesDisplayCodeNode.kt` — category SINK, 2 inputs, 0 outputs, `createRuntime()` returns `CodeNodeFactory.createSinkIn2<Any, Any>` updating `AddressesState._result` and `_error`
- [ ] T018 [US2] Create ServiceLoader registration file at `Addresses/src/jvmMain/resources/META-INF/services/io.codenode.fbpdsl.runtime.CodeNodeDefinition` listing all 3 Addresses CodeNodeDefinition fully-qualified class names

### Factory Updates for Entity Modules

- [ ] T019 [US2] Update `ModuleSessionFactory.kt` in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` to create dynamic sessions for UserProfiles, GeoLocations, and Addresses when `canBuildDynamic()` returns true — for each: create `DynamicPipelineController` with flowGraphProvider and lookup, create adapter implementing the module's `ControllerInterface`, create module's ViewModel with adapter (and DAO for entity modules), return `RuntimeSession` with flowGraphProvider. Entity modules no longer need pre-starting (research.md R5)
- [ ] T020 [US2] Verify entity module migrations per quickstart.md Steps 3-5 — load each entity module, Start, perform CRUD operations, verify persistence and display, enable attenuation + animation, confirm dynamic pipeline path is used

**Checkpoint**: All 4 modules (StopWatch + 3 entity modules) run via dynamic pipeline with identical behavior.

---

## Phase 3: User Story 3 — All Modules Interoperable on Canvas (Priority: P3)

**Goal**: Verify that all 5 modules (including EdgeArtFilter) consistently use the dynamic pipeline, with speed attenuation, data flow animation, pause/resume, and module switching all working uniformly.

**Independent Test**: Cycle through all 5 modules — start each, verify dynamic pipeline, enable animation with attenuation, stop and switch — no regressions.

### Implementation for User Story 3

- [ ] T021 [US3] Verify cross-module switching per quickstart.md Step 6 — load StopWatch → Start → switch to UserProfiles → Start → verify clean transition → cycle through GeoLocations, Addresses, EdgeArtFilter → no crashes, no stale state
- [ ] T022 [US3] Verify speed attenuation and data flow animation work for all 4 migrated modules — for each module: set attenuation slider > 200ms, enable Animate Data Flow, verify dot animations appear on connections, verify pause/resume preserves animation state

**Checkpoint**: All modules interoperable — uniform dynamic pipeline behavior across the system.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Build verification and full quickstart validation

- [ ] T023 Build verification — run `./gradlew :fbpDsl:jvmTest` to confirm no test regressions (SC-005)
- [ ] T024 Run full quickstart.md validation (Steps 1-7) — EdgeArtFilter baseline, StopWatch, all entity modules, cross-module switching, build verification

---

## Dependencies & Execution Order

### Phase Dependencies

- **US1 (Phase 1)**: No dependencies beyond 050/051 infrastructure — can start immediately
- **US2 (Phase 2)**: Independent of US1 — CodeNode files are in different modules. However, T019 modifies the same file as T005 (ModuleSessionFactory.kt), so US2 factory work should follow US1 factory work
- **US3 (Phase 3)**: Depends on US1 and US2 being complete — integration validation
- **Polish (Phase 4)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: No dependencies on other stories — can start immediately
- **User Story 2 (P2)**: CodeNode files (T007-T018) are independent of US1. Factory update (T019) depends on T005 (same file)
- **User Story 3 (P3)**: Depends on US1 and US2 — pure verification

### Within Each Phase

- T001, T002, T003 can run in parallel (different files in StopWatch/nodes/)
- T004 depends on T001-T003 (lists their class names)
- T005 depends on T004 (needs nodes registered for canBuildDynamic)
- T007-T009, T011-T013, T015-T017 can ALL run in parallel (9 different files across 3 modules)
- T010, T014, T018 depend on their module's CodeNode files
- T019 depends on T010, T014, T018 AND T005 (same file)

### Parallel Opportunities

```bash
# Phase 1 — all 3 StopWatch CodeNodes in parallel:
Task T001: TimerEmitterCodeNode.kt
Task T002: TimeIncrementerCodeNode.kt
Task T003: DisplayReceiverCodeNode.kt

# Phase 2 — all 9 entity CodeNodes in parallel:
Task T007: UserProfileCUDCodeNode.kt
Task T008: UserProfileRepositoryCodeNode.kt
Task T009: UserProfilesDisplayCodeNode.kt
Task T011: GeoLocationCUDCodeNode.kt
Task T012: GeoLocationRepositoryCodeNode.kt
Task T013: GeoLocationsDisplayCodeNode.kt
Task T015: AddressCUDCodeNode.kt
Task T016: AddressRepositoryCodeNode.kt
Task T017: AddressesDisplayCodeNode.kt

# Phase 2 — all 3 ServiceLoader files in parallel (after CodeNodes):
Task T010: UserProfiles ServiceLoader
Task T014: GeoLocations ServiceLoader
Task T018: Addresses ServiceLoader
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: StopWatch CodeNodes (T001-T003, parallel)
2. ServiceLoader + Factory (T004-T005, sequential)
3. **STOP and VALIDATE**: Load StopWatch → Start → verify ticking, pause/resume, stop/restart
4. If StopWatch works dynamically, MVP is delivered

### Incremental Delivery

1. StopWatch CodeNodes → Factory → Verify (MVP!)
2. Entity CodeNodes (all 9, parallel) → ServiceLoaders → Factory → Verify
3. Cross-module integration verification
4. Build verification and full quickstart validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each module's `nodes/` directory follows the EdgeArtFilter pattern from feature 050
- Processing logic is EMBEDDED in `createRuntime()` — no external tick function references
- Identity tracking vars in repository CodeNodes use closure scope (research.md R8)
- Entity modules no longer need pre-starting in factory (research.md R5)
- Generated Controller/Flow files remain as fallback (FR-008) — not deleted
