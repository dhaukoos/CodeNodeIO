# Tasks: StopWatch Virtual Circuit Refactor

**Input**: Design documents from `/specs/011-stopwatch-refactor/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: TDD approach requested in constitution - tests included before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **graphEditor**: `graphEditor/src/jvmMain/kotlin/` and `graphEditor/src/jvmTest/kotlin/`
- **KMPMobileApp**: `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/`

---

## Phase 1: Setup

**Purpose**: Verify existing infrastructure and create test directory structure

- [x] T001 Verify graphEditor and KMPMobileApp modules compile successfully with `./gradlew :graphEditor:compileKotlinJvm :KMPMobileApp:compileCommonMainKotlinMetadata`
- [x] T002 Create test directory structure at `graphEditor/src/jvmTest/kotlin/compilation/` if not exists

**Checkpoint**: Build passes, ready to begin implementation

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create core data structures used by all user stories

**⚠️ CRITICAL**: User Story 1 requires these data structures to be in place

- [x] T003 Create PropertyValidationError data class in `graphEditor/src/jvmMain/kotlin/compilation/RequiredPropertyValidator.kt`
- [x] T004 Create PropertyValidationResult data class with toErrorMessage() method in `graphEditor/src/jvmMain/kotlin/compilation/RequiredPropertyValidator.kt`

**Checkpoint**: Foundation ready - data structures exist for validation logic

---

## Phase 3: User Story 1 - Compile Validation for Required Properties (Priority: P1) 🎯 MVP

**Goal**: Block compilation when GENERIC nodes are missing `_useCaseClass` or `_genericType` properties, with clear error messages.

**Independent Test**: Create a GENERIC node in graphEditor without `_useCaseClass`, click Compile, verify error message displays listing the missing property.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T005 [P] [US1] Write test `validate returns success when all required properties present` in `graphEditor/src/jvmTest/kotlin/compilation/RequiredPropertyValidatorTest.kt`
- [x] T006 [P] [US1] Write test `validate returns error for GENERIC node missing _useCaseClass` in `graphEditor/src/jvmTest/kotlin/compilation/RequiredPropertyValidatorTest.kt`
- [x] T007 [P] [US1] Write test `validate returns error for GENERIC node missing _genericType` in `graphEditor/src/jvmTest/kotlin/compilation/RequiredPropertyValidatorTest.kt`
- [x] T008 [P] [US1] Write test `validate returns multiple errors for multiple nodes with missing properties` in `graphEditor/src/jvmTest/kotlin/compilation/RequiredPropertyValidatorTest.kt`
- [x] T009 [P] [US1] Write test `validate ignores non-GENERIC node types` in `graphEditor/src/jvmTest/kotlin/compilation/RequiredPropertyValidatorTest.kt`
- [x] T010 [P] [US1] Write test `validate returns success for empty graph` in `graphEditor/src/jvmTest/kotlin/compilation/RequiredPropertyValidatorTest.kt`

### Implementation for User Story 1

- [x] T011 [US1] Implement RequiredPropertyValidator class with validate() and getRequiredProperties() methods in `graphEditor/src/jvmMain/kotlin/compilation/RequiredPropertyValidator.kt`
- [x] T012 [US1] Add requiredSpecs map with GENERIC node requirements (_useCaseClass, _genericType) in `graphEditor/src/jvmMain/kotlin/compilation/RequiredPropertyValidator.kt`
- [x] T013 [US1] Integrate RequiredPropertyValidator into CompilationService.compileToModule() after FlowGraph.validate() in `graphEditor/src/jvmMain/kotlin/compilation/CompilationService.kt`
- [x] T014 [US1] Update CompilationService to return error with PropertyValidationResult.toErrorMessage() when validation fails in `graphEditor/src/jvmMain/kotlin/compilation/CompilationService.kt`
- [x] T015 [US1] Run all tests to verify implementation with `./gradlew :graphEditor:jvmTest --tests "*.RequiredPropertyValidatorTest"`

**Checkpoint**: Compile validation works - clicking Compile on a graph with missing required properties shows error message

---

## Phase 4: User Story 2 - Properties Panel Shows Required Properties (Priority: P2)

**Goal**: Display `_useCaseClass` field with required indicator (*) in properties panel for GENERIC nodes.

**Independent Test**: Select a GENERIC node in graphEditor, verify properties panel shows "Use Case Class *" field. Enter a value, verify it persists.

### Implementation for User Story 2

- [ ] T016 [US2] Modify configProperties filter in PropertiesContent to show `_useCaseClass` for GENERIC nodes in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt` (around line 553)
- [ ] T017 [US2] Add PropertyEditorRow for `_useCaseClass` with required=true and description "Fully qualified class implementing ProcessingLogic" in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [ ] T018 [US2] Add validation error display for empty `_useCaseClass` ("Use Case Class is required") in `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- [ ] T019 [US2] Verify property changes persist to node configuration by testing in graphEditor manually

**Checkpoint**: Properties panel shows required fields - GENERIC nodes display _useCaseClass with required indicator

---

## Phase 5: User Story 3 - Remove Redundant FlowGraph Creation (Priority: P3)

**Goal**: Remove `createStopWatchFlowGraph()` from KMPMobileApp and use StopWatch module's `stopWatchFlowGraph` directly.

**Independent Test**: Build KMPMobileApp with `./gradlew :KMPMobileApp:build`, run mobile app and verify stopwatch functions correctly.

### Implementation for User Story 3

- [ ] T020 [US3] Update import statements in App.kt to add `import io.codenode.stopwatch.stopWatchFlowGraph` in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`
- [ ] T021 [US3] Update MainContent composable to use `stopWatchFlowGraph` directly instead of calling createStopWatchFlowGraph() in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`
- [ ] T022 [US3] Remove the entire `createStopWatchFlowGraph()` function (approximately 85 lines) from `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`
- [ ] T023 [US3] Remove unused fbpDsl model imports that were only needed for createStopWatchFlowGraph() in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt`
- [ ] T024 [US3] Verify KMPMobileApp builds successfully with `./gradlew :KMPMobileApp:build`
- [ ] T025 [US3] Verify no redundant FlowGraph definitions exist by searching codebase for `createStopWatchFlowGraph`

**Checkpoint**: Redundant code removed - KMPMobileApp uses StopWatch module directly

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [ ] T026 Run full test suite with `./gradlew :graphEditor:jvmTest`
- [ ] T027 Run graphEditor and manually test complete flow: create GENERIC node, set _useCaseClass, compile successfully
- [ ] T028 Verify success criteria SC-001 through SC-005 from spec.md are met
- [ ] T029 [P] Update any documentation that references createStopWatchFlowGraph()

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup - creates data structures needed by US1
- **User Story 1 (Phase 3)**: Depends on Foundational - implements validation logic
- **User Story 2 (Phase 4)**: Depends on Foundational - can run in parallel with US1
- **User Story 3 (Phase 5)**: No dependencies on US1/US2 - can run in parallel
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational (T003-T004) for data structures
- **User Story 2 (P2)**: No dependencies on US1 - independent properties panel work
- **User Story 3 (P3)**: No dependencies on US1/US2 - independent refactoring work

### Within Each User Story

- Tests (T005-T010) MUST be written and FAIL before implementation (T011-T015)
- All tests for a user story can run in parallel
- Implementation tasks within a story are sequential

### Parallel Opportunities

Within Phase 3 (User Story 1):
- T005, T006, T007, T008, T009, T010 can all run in parallel (different test methods)

Across User Stories:
- US2 (T016-T019) and US3 (T020-T025) can run in parallel with US1 after Foundational completes

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all User Story 1 tests together:
Task: "Write test validate returns success when all required properties present"
Task: "Write test validate returns error for GENERIC node missing _useCaseClass"
Task: "Write test validate returns error for GENERIC node missing _genericType"
Task: "Write test validate returns multiple errors for multiple nodes"
Task: "Write test validate ignores non-GENERIC node types"
Task: "Write test validate returns success for empty graph"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T004)
3. Complete Phase 3: User Story 1 (T005-T015)
4. **STOP and VALIDATE**: Test compile validation independently
5. Deploy/demo if ready - users can now see validation errors

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Compile validation works (MVP!)
3. Add User Story 2 → Properties panel shows required fields
4. Add User Story 3 → Redundant code removed
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers after Foundational completes:
- Developer A: User Story 1 (compile validation)
- Developer B: User Story 2 (properties panel)
- Developer C: User Story 3 (code removal)

---

## Notes

- [P] tasks = different files or test methods, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- TDD: Tests must fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
