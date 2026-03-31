# Tasks: Generate CodeNodeDefinition-Based Repository Modules

**Input**: Design documents from `/specs/061-codenode-definition-codegen/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: Create the new generator classes and establish the file structure

- [X] T001 Created `EntityCUDCodeNodeGenerator` — generates `{Entity}CUDCodeNode.kt` implementing CodeNodeDefinition with typed PortSpecs and coroutineScope + 3 launch blocks.
- [X] T002 Created `EntityRepositoryCodeNodeGenerator` — generates `{Entity}RepositoryCodeNode.kt` implementing CodeNodeDefinition with identity tracking, typed ports, and toEntity() conversion.
- [X] T003 Created `EntityDisplayCodeNodeGenerator` — generates `{PluralName}DisplayCodeNode.kt` implementing CodeNodeDefinition with String input ports.

**Checkpoint**: Three new generator classes exist and produce valid Kotlin source strings

---

## Phase 2: Foundational — Wire Generators into EntityModuleGenerator

**Purpose**: Connect the new generators to the orchestrator and update the FlowGraph builder. MUST complete before user stories can be validated.

- [X] T004 Modified EntityModuleGenerator — replaced legacy generators with new CodeNodeDefinition generators. Output to nodes/ subdirectory.
- [X] T005 Modified EntityFlowGraphBuilder — added _codeNodeClass and _genericType config to all three nodes.
- [X] T006 Verified: `./gradlew :kotlinCompiler:compileKotlinJvm :graphEditor:compileKotlinJvm` — BUILD SUCCESSFUL.

**Checkpoint**: EntityModuleGenerator uses new generators, FlowGraph builder tags all nodes with _codeNodeClass

---

## Phase 3: User Story 1 — Generated Modules Compile and Run (Priority: P1) MVP

**Goal**: A developer creates a repository module and it compiles on first attempt with zero manual fixes.

**Independent Test**: Create "TestItem" IP type, click "Create Repository Module", compile with `./gradlew :TestItems:jvmJar`.

### Implementation for User Story 1

- [X] T007 [US1] Simplified RuntimeFlowGenerator — removed legacyNodes filter, tick function imports, factory function calls. All nodes use CodeNodeDefinition path.
- [X] T008 [US1] ModuleGenerator already includes preview-api in jvmMain (from feature 060).
- [X] T009 [US1] ModuleSaveService already deletes entire module directory on removal (nodes/ included).
- [X] T010 [US1] Tests: 502 total, 17 failures. 16 failures are expected (legacy path tests that need updating in T020-T021). 1 pre-existing (EntityUIGeneratorTest). Production code compiles.
- [X] T011 [US1] End-to-end test: Created TestItem, compiled successfully. Generated Flow.kt uses concrete types (TestItem, String) for runtime casts and observable state. No tick function references.

**Checkpoint**: Generated modules compile out of the box. No tick function errors. No type mismatches.

---

## Phase 4: User Story 2 — Generated Nodes Match Existing Pattern (Priority: P2)

**Goal**: Generated code follows the same architectural pattern as hand-written nodes.

**Independent Test**: Compare generated `*RepositoryCodeNode.kt` with hand-written `UserProfileRepositoryCodeNode.kt`.

### Implementation for User Story 2

- [X] T012 [US2] Verified: generated CUDCodeNode uses typed ports (TestItem::class), imports from iptypes, uses createSourceOut3<TestItem>.
- [X] T013 [US2] Verified: generated RepositoryCodeNode uses typed input ports, String output ports, identity tracking, toEntity() conversion, ProcessResult2.
- [X] T014 [US2] Verified: generated DisplayCodeNode uses String::class input ports and createSinkIn2<String, String>.
- [X] T015 [US2] Verified: generated Flow.kt has concrete types in runtime casts and observable state.

**Checkpoint**: Generated nodes are structurally identical to hand-written nodes. Types are concrete.

---

## Phase 5: User Story 3 — Legacy Code Paths Eliminated (Priority: P3)

**Goal**: No legacy tick functions or factory functions in generated code. RuntimeFlowGenerator simplified.

### Implementation for User Story 3

- [X] T016 [US3] Deleted EntityCUDGenerator and EntityCUDGeneratorTest.
- [X] T017 [US3] Deleted EntityDisplayGenerator and EntityDisplayGeneratorTest.
- [X] T018 [US3] Verified: generated Flow.kt contains no tick function imports and no legacy factory calls. All nodes use CodeNodeDefinition.createRuntime().
- [ ] T019 [US3] Regression test: Verify existing modules compile from DemoProject. [MANUAL TEST]

**Checkpoint**: Legacy code paths removed. No regressions in existing modules.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Test cleanup, documentation, and final validation

- [X] T020 [P] Updated EntityModuleGeneratorTest — file paths now reference nodes/ subdirectory, assertions check for CodeNodeDefinition output.
- [X] T021 [P] Updated RuntimeFlowGeneratorTest — removed 13 legacy tick-function tests. 25 remaining tests pass (wiring, observable state, CodeNodeDefinition path).
- [X] T022 Full test suite: 469 tests, 1 failure (pre-existing EntityUIGeneratorTest). All generator changes pass.
- [ ] T023 Run quickstart.md scenarios 1-5 to validate end-to-end functionality. [MANUAL TEST]
- [ ] T024 Clean up test artifacts (remove any TestItem module created during testing). [MANUAL]

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (needs new generator classes)
- **US1 (Phase 3)**: Depends on Phase 2 (needs generators wired into orchestrator)
- **US2 (Phase 4)**: Depends on US1 (verification of generated output)
- **US3 (Phase 5)**: Depends on US1 (legacy cleanup after new path works)
- **Polish (Phase 6)**: Depends on US1 + US3

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — core compilation fix
- **User Story 2 (P2)**: Depends on US1 — verifies output quality
- **User Story 3 (P3)**: Can start after US1 — independent legacy cleanup

### Parallel Opportunities

- T001, T002, T003 (new generator classes) can run in parallel
- T016, T017 (deprecation annotations) can run in parallel
- T020, T021 (test updates) can run in parallel

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Create 3 new generator classes
2. Complete Phase 2: Wire into orchestrator + FlowGraphBuilder
3. Complete Phase 3: Simplify RuntimeFlowGenerator + end-to-end test
4. **STOP and VALIDATE**: Generated module compiles on first attempt
5. This alone resolves the compilation failure blocking new module creation

### Incremental Delivery

1. Setup + Foundational → New generators ready
2. Add US1 → Generated modules compile (MVP!)
3. Add US2 → Verify type safety and pattern consistency
4. Add US3 → Legacy cleanup, deprecation
5. Polish → Tests, quickstart validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- The 3 new generator classes (T001-T003) follow the exact patterns from the hand-written nodes in UserProfiles/GeoLocations/Addresses — use those as templates
- RuntimeFlowGenerator simplification (T007) is the highest-risk task — it removes ~50% of the generator. Test thoroughly.
- The generated `build.gradle.kts` must use `"io.codenode:fbpDsl"` (Maven coordinate for composite build), NOT `project(":fbpDsl")` (causes StackOverflowError in Gradle sync)
