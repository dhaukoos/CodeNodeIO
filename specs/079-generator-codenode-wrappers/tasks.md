# Tasks: Generator CodeNode Wrappers

**Input**: Design documents from `/specs/079-generator-codenode-wrappers/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Unit tests included — spec FR-007 requires each wrapper to be independently testable.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: User Story 1 — Define GeneratorCodeNode Interface (Priority: P1) 🎯 MVP

**Goal**: Define the common interface/pattern for generator CodeNode wrappers.

**Independent Test**: The interface compiles. A test wrapper can be instantiated and produces output.

### Implementation

- [X] T001 [US1] Create `nodes/` directory in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/`
- [X] T002 [US1] Create a reference wrapper `FlowKtGeneratorNode` + `GenerationConfig` data class in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/`
- [X] T003 [US1] Add unit test: `FlowKtGeneratorNode` properties and output verified in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/nodes/GeneratorCodeNodeTest.kt`
- [X] T004 [US1] Compile and run: `./gradlew :flowGraph-generate:jvmTest --tests "*GeneratorCodeNode*"`

**Checkpoint**: Reference wrapper works. Pattern established for remaining 14 wrappers.

---

## Phase 2: User Story 2 — Wrap Module-Level Generators (Priority: P2)

**Goal**: Wrap the remaining 6 module-level generators as CodeNodes (FlowKt already done in US1).

**Independent Test**: All 7 module-level wrappers produce correct output. All appear in palette.

### Implementation

- [X] T005 [P] [US2] Create `RuntimeFlowGeneratorNode` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/RuntimeFlowGeneratorNode.kt`
- [X] T006 [P] [US2] Create `RuntimeControllerGeneratorNode` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/RuntimeControllerGeneratorNode.kt`
- [X] T007 [P] [US2] Create `RuntimeControllerInterfaceGeneratorNode` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/RuntimeControllerInterfaceGeneratorNode.kt`
- [X] T008 [P] [US2] Create `RuntimeControllerAdapterGeneratorNode` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/RuntimeControllerAdapterGeneratorNode.kt`
- [X] T009 [P] [US2] Create `RuntimeViewModelGeneratorNode` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/RuntimeViewModelGeneratorNode.kt`
- [X] T010 [P] [US2] Create `UserInterfaceStubGeneratorNode` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/UserInterfaceStubGeneratorNode.kt`
- [X] T011 [US2] Add unit tests for all 7 module-level wrappers in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/nodes/GeneratorCodeNodeTest.kt`
- [X] T012 [US2] Compile and run: `./gradlew :flowGraph-generate:jvmTest` — all pass

**Checkpoint**: All 7 module-level generator CodeNodes work and tested.

---

## Phase 3: User Story 3 — Wrap Entity and UI-FBP Generators (Priority: P3)

**Goal**: Wrap the remaining 8 generators (4 entity + 4 UI-FBP) as CodeNodes.

**Independent Test**: All 15 wrappers produce correct output. All discoverable.

### Implementation

- [ ] T013 [P] [US3] Create `EntityCUDGeneratorNode` — delegates to `EntityCUDCodeNodeGenerator().generate()`, input "entitySpec" (Any::class) in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/EntityCUDGeneratorNode.kt`
- [ ] T014 [P] [US3] Create `EntityRepositoryGeneratorNode` — delegates to `EntityRepositoryCodeNodeGenerator().generate()` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/EntityRepositoryGeneratorNode.kt`
- [ ] T015 [P] [US3] Create `EntityDisplayGeneratorNode` — delegates to `EntityDisplayCodeNodeGenerator().generate()` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/EntityDisplayGeneratorNode.kt`
- [ ] T016 [P] [US3] Create `EntityPersistenceGeneratorNode` — delegates to `EntityPersistenceGenerator().generate()` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/EntityPersistenceGeneratorNode.kt`
- [ ] T017 [P] [US3] Create `UIFBPStateGeneratorNode` — delegates to `UIFBPStateGenerator().generate()`, input "uiFBPSpec" (Any::class) in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/UIFBPStateGeneratorNode.kt`
- [ ] T018 [P] [US3] Create `UIFBPViewModelGeneratorNode` — delegates to `UIFBPViewModelGenerator().generate()` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/UIFBPViewModelGeneratorNode.kt`
- [ ] T019 [P] [US3] Create `UIFBPSourceGeneratorNode` — delegates to `UIFBPSourceCodeNodeGenerator().generate()` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/UIFBPSourceGeneratorNode.kt`
- [ ] T020 [P] [US3] Create `UIFBPSinkGeneratorNode` — delegates to `UIFBPSinkCodeNodeGenerator().generate()` in `flowGraph-generate/src/commonMain/kotlin/io/codenode/flowgraphgenerate/nodes/UIFBPSinkGeneratorNode.kt`
- [ ] T021 [US3] Add unit tests for all 8 entity/UI-FBP wrappers — each produces non-empty output for appropriate spec input in `flowGraph-generate/src/commonTest/kotlin/io/codenode/flowgraphgenerate/nodes/GeneratorCodeNodeTest.kt`
- [ ] T022 [US3] Compile and run all tests: `./gradlew :flowGraph-generate:jvmTest`

**Checkpoint**: All 15 generator CodeNodes work, tested, and ready for palette discovery.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Full verification and palette discovery

- [ ] T023 Run full test suite: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest`
- [ ] T024 Verify all 15 generator CodeNodes are discoverable by node palette (launch graph editor, search for "Generator")
- [ ] T025 Verify existing generators are unchanged: `git diff` shows zero modifications to files in `generator/` directory

---

## Dependencies & Execution Order

### Phase Dependencies

- **User Story 1 (Phase 1)**: No dependencies — creates reference wrapper. BLOCKS US2/US3.
- **User Story 2 (Phase 2)**: Depends on US1 — follows established pattern.
- **User Story 3 (Phase 3)**: Depends on US1 — follows established pattern. Can run in parallel with US2.
- **Polish (Phase 4)**: Depends on all user stories.

### Within Each Phase

- US2: All 6 wrappers (T005–T010) in parallel — different files. Then tests (T011) and compile (T012).
- US3: All 8 wrappers (T013–T020) in parallel — different files. Then tests (T021) and compile (T022).

### Parallel Opportunities

```text
# US2 — 6 module-level wrappers (all different files):
T005, T006, T007, T008, T009, T010

# US3 — 8 entity/UI-FBP wrappers (all different files):
T013, T014, T015, T016, T017, T018, T019, T020

# US2 and US3 can also run in parallel (both depend only on US1)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: User Story 1 (T001–T004) — reference FlowKtGeneratorNode
2. **STOP and VALIDATE**: Wrapper pattern works, test passes
3. This establishes the template for all remaining wrappers

### Incremental Delivery

1. User Story 1 → Reference wrapper + test (MVP!)
2. User Story 2 → 6 more module-level wrappers (7 total)
3. User Story 3 → 8 entity/UI-FBP wrappers (15 total)
4. Polish → Full verification + palette discovery

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- All 15 wrappers follow the same pattern — only the delegate generator and input type differ
- Wrappers are in `nodes/` following the "eat our own dogfood" principle
- Existing `generator/` directory is UNCHANGED — wrappers are purely additive
- Commit after each phase
