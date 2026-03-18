# Tasks: CodeNode-Driven Flow Runtime

**Input**: Design documents from `/specs/053-codenode-flow-runtime/`
**Prerequisites**: plan.md (required), spec.md (required), research.md

**Tests**: Not requested in the spec. Existing test suite must continue passing (SC-005).

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: User Story 1 — Generated Flows Use CodeNode Runtimes (Priority: P1) MVP

**Goal**: Restructure the 4 generated Flow files to create node runtimes via `CodeNodeDefinition.createRuntime()` instead of importing processingLogic tick functions or CUD/Display stub functions. Observable state delegation and channel wiring remain unchanged.

**Independent Test**: Compile `graphEditor` and `KMPMobileApp`. Run StopWatch in KMPMobileApp — timer ticks, pause/resume works, stop resets. Run entity modules in graphEditor Runtime Preview — CRUD operations work.

### Implementation for User Story 1

- [X] T001 [P] [US1] Restructure `StopWatchFlow.kt` in `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/generated/StopWatchFlow.kt` — replace processingLogic import and inline CodeNodeFactory calls with direct CodeNode imports (`TimerEmitterCodeNode`, `TimeIncrementerCodeNode`, `DisplayReceiverCodeNode` from `io.codenode.stopwatch.nodes`) and `XxxCodeNode.createRuntime("NodeName")` calls. Cast return types to concrete runtime types for channel wiring. Keep observable state delegation, start/stop/reset lifecycle, and wireConnections unchanged.
- [X] T002 [P] [US1] Restructure `UserProfilesFlow.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/UserProfilesFlow.kt` — replace `userProfileRepositoryTick` import and `createUserProfileCUD()`/`createUserProfilesDisplay()` calls with CodeNode imports (`UserProfileCUDCodeNode`, `UserProfileRepositoryCodeNode`, `UserProfilesDisplayCodeNode` from `io.codenode.userprofiles.nodes`) and `XxxCodeNode.createRuntime("NodeName")` calls. Cast return types for channel wiring. Keep state delegation, lifecycle, and wireConnections unchanged.
- [X] T003 [P] [US1] Restructure `GeoLocationsFlow.kt` in `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/generated/GeoLocationsFlow.kt` — same pattern as T002 but with GeoLocations CodeNode imports (`GeoLocationCUDCodeNode`, `GeoLocationRepositoryCodeNode`, `GeoLocationsDisplayCodeNode` from `io.codenode.geolocations.nodes`).
- [X] T004 [P] [US1] Restructure `AddressesFlow.kt` in `Addresses/src/commonMain/kotlin/io/codenode/addresses/generated/AddressesFlow.kt` — same pattern as T002 but with Addresses CodeNode imports (`AddressCUDCodeNode`, `AddressRepositoryCodeNode`, `AddressesDisplayCodeNode` from `io.codenode.addresses.nodes`).
- [X] T005 [US1] Compile and verify — run `./gradlew :graphEditor:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest` to confirm no regressions. Verify StopWatch dynamic pipeline still works in graphEditor Runtime Preview.

**Checkpoint**: All 4 generated Flow files use CodeNode runtimes. ProcessingLogic and stub files are no longer imported but still exist on disk.

---

## Phase 2: User Story 2 — RuntimeFlowGenerator Produces CodeNode-Based Flows (Priority: P2)

**Goal**: Update the RuntimeFlowGenerator in kotlinCompiler so that newly generated Flow files use CodeNodeDefinition imports instead of processingLogic references, when a node's CodeNodeDefinition class is known. Fall back to existing pattern for nodes without CodeNodeDefinitions.

**Independent Test**: Inspect RuntimeFlowGenerator source — confirm CodeNode-aware code path exists alongside legacy path. Existing generator tests should pass.

### Implementation for User Story 2

- [X] T006 [US2] Update `RuntimeFlowGenerator.kt` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt` — add a CodeNode-aware generation path: when a node's configuration contains a `_codeNodeClass` key (fully-qualified CodeNodeDefinition class name), generate `import {class}` and `val nodeName = {ClassName}.createRuntime("NodeName")` instead of processingLogic/stub imports and factory calls. For nodes without `_codeNodeClass`, preserve the existing generation logic (FR-003).
- [X] T007 [US2] Verify generator changes — run `./gradlew :kotlinCompiler:jvmTest` to confirm existing generator tests pass (they test the legacy path which must still work).

**Checkpoint**: RuntimeFlowGenerator supports both CodeNode-driven and legacy processingLogic generation paths.

---

## Phase 3: User Story 3 — Dead ProcessingLogic Files and Imports Removed (Priority: P3)

**Goal**: Delete the processingLogic directories, CUD/Display stub files, and unused processingLogic imports from .flow.kt files for the 4 migrated modules.

**Independent Test**: Search all 4 module directories for any remaining processingLogic references or CUD/Display stub files. None should exist. Project compiles and tests pass.

### Implementation for User Story 3

- [ ] T008 [P] [US3] Delete `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/processingLogic/TimeIncrementerProcessLogic.kt` and remove the empty `processingLogic/` directory.
- [ ] T009 [P] [US3] Delete `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfileCUD.kt` and `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesDisplay.kt`.
- [ ] T010 [P] [US3] Delete `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/GeoLocationCUD.kt` and `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/GeoLocationsDisplay.kt`.
- [ ] T011 [P] [US3] Delete `Addresses/src/commonMain/kotlin/io/codenode/addresses/AddressCUD.kt` and `Addresses/src/commonMain/kotlin/io/codenode/addresses/AddressesDisplay.kt`.
- [ ] T012 [P] [US3] Remove `import ...processingLogic.*` from `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/StopWatch.flow.kt`.
- [ ] T013 [P] [US3] Remove `import ...processingLogic.*` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfiles.flow.kt`.
- [ ] T014 [P] [US3] Remove `import ...processingLogic.*` from `GeoLocations/src/commonMain/kotlin/io/codenode/geolocations/GeoLocations.flow.kt`.
- [ ] T015 [P] [US3] Remove `import ...processingLogic.*` from `Addresses/src/commonMain/kotlin/io/codenode/addresses/Addresses.flow.kt`.
- [ ] T016 [US3] Compile and verify — run `./gradlew :graphEditor:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest` to confirm no regressions after deletions.

**Checkpoint**: Zero processingLogic files, zero CUD/Display stubs, zero processingLogic imports remain in the 4 migrated modules.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Final build verification and full quickstart validation

- [ ] T017 Build verification — run `./gradlew :graphEditor:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest` to confirm no regressions (SC-005)
- [ ] T018 Run full quickstart.md validation (Steps 1-6) — verify generated Flow files, KMPMobileApp compilation, graphEditor dynamic pipeline regression, dead code removal

---

## Dependencies & Execution Order

### Phase Dependencies

- **US1 (Phase 1)**: No dependencies — can start immediately. This is the MVP.
- **US2 (Phase 2)**: Independent of US1 — modifies a different file (RuntimeFlowGenerator.kt). Can run in parallel with US1.
- **US3 (Phase 3)**: Depends on US1 — files can only be deleted after the generated Flows no longer import them.
- **Polish (Phase 4)**: Depends on all user stories being complete.

### Within Each Phase

- T001, T002, T003, T004 can ALL run in parallel (4 different generated Flow files)
- T005 depends on T001-T004 (compilation verification)
- T006 is independent (different file in kotlinCompiler)
- T007 depends on T006
- T008-T015 can ALL run in parallel (file deletions and import removals across different modules)
- T016 depends on T008-T015

### Parallel Opportunities

```bash
# Phase 1 — all 4 Flow restructures in parallel:
Task T001: StopWatchFlow.kt
Task T002: UserProfilesFlow.kt
Task T003: GeoLocationsFlow.kt
Task T004: AddressesFlow.kt

# Phase 3 — all deletions and import removals in parallel:
Task T008: Delete StopWatch processingLogic
Task T009: Delete UserProfiles CUD/Display stubs
Task T010: Delete GeoLocations CUD/Display stubs
Task T011: Delete Addresses CUD/Display stubs
Task T012: Remove StopWatch .flow.kt import
Task T013: Remove UserProfiles .flow.kt import
Task T014: Remove GeoLocations .flow.kt import
Task T015: Remove Addresses .flow.kt import
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Restructure all 4 generated Flow files (T001-T004, parallel)
2. Compile and verify (T005)
3. **STOP and VALIDATE**: Run graphEditor Runtime Preview for StopWatch, verify ticking
4. If all modules work via restructured Flows, MVP is delivered

### Incremental Delivery

1. Restructure generated Flows → Verify (MVP!)
2. Update RuntimeFlowGenerator → Verify generator tests
3. Delete dead files and imports → Verify clean build
4. Full quickstart validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Channel wiring code in generated Flows remains unchanged — only runtime creation changes
- Cast `createRuntime()` return to concrete runtime type for channel property access (research.md R3)
- Observable state delegation in generated Flows remains unchanged (research.md R7)
- KMPMobileApp requires zero code changes — only Flow internals change (research.md R6)
- 7 total files deleted: 1 processingLogic + 6 CUD/Display stubs (research.md R5)
