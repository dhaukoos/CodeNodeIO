# Tasks: Persist CodeNode Metadata Through Save Pipeline

**Input**: Design documents from `/specs/054-persist-codenode-metadata/`
**Prerequisites**: plan.md, spec.md, research.md, quickstart.md

**Tests**: Not requested in spec. Existing tests must pass (SC-003).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Inject `_codeNodeClass` into CodeNodeDefinition's defaultConfiguration — all user stories depend on this metadata being present.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T001 Add `_codeNodeClass` to `defaultConfiguration` in `CodeNodeDefinition.toNodeTypeDefinition()` using `this::class.qualifiedName` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/CodeNodeDefinition.kt` (research decision R1: add `"_codeNodeClass" to (this::class.qualifiedName ?: "")` alongside existing `_genericType` and `_codeNodeDefinition` entries at ~line 139-142)

**Checkpoint**: Foundation ready — `_codeNodeClass` is now present in every CodeNodeDefinition's defaultConfiguration. User story implementation can begin.

---

## Phase 2: User Story 1 — Saved Modules Regenerate CodeNode-Driven Flows (Priority: P1)

**Goal**: When a module is saved, the regenerated Flow file uses CodeNode `createRuntime()` pattern. On re-save, metadata survives the round-trip.

**Independent Test**: Save StopWatch module, open regenerated `StopWatchFlow.kt`, confirm it uses `CodeNodeDefinition.createRuntime()` calls. Close, re-open, re-save — confirm it still uses CodeNode pattern.

### Implementation for User Story 1

- [X] T002 [P] [US1] Whitelist `_codeNodeClass`, `_genericType`, and `_codeNodeDefinition` in the `_`-prefix filter in `FlowKtGenerator` at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt` (~line 166-169). Change filter from `!it.key.startsWith("_")` to `!it.key.startsWith("_") || it.key in preservedInternalKeys` where `preservedInternalKeys = setOf("_codeNodeClass", "_genericType", "_codeNodeDefinition")` (research decision R2)
- [X] T003 [P] [US1] Skip processingLogic stub generation for nodes with `_codeNodeClass` in their configuration in `ModuleSaveService` at `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt` (~line 1025-1051). Since all nodes will have `_codeNodeClass` after T001, the `ProcessingLogicStubGenerator` invocation becomes a no-op — remove it entirely (research decision R5)

**Checkpoint**: Saving a module now preserves `_codeNodeClass` in the `.flow.kt` file and skips legacy processingLogic stub generation. US1 acceptance scenarios 1-3 should pass.

---

## Phase 3: User Story 2 — Drag-and-Drop Nodes Retain CodeNode Identity (Priority: P2)

**Goal**: When a user drags a node from the palette onto the canvas, the node retains its CodeNode class identity in the graph configuration.

**Independent Test**: Drag a CodeNodeDefinition-backed node onto the canvas, inspect the node's configuration — confirm `_codeNodeClass` is present.

### Implementation for User Story 2

- [X] T004 [US2] Pass `nodeType.defaultConfiguration` instead of `emptyMap()` in `DragAndDropHandler.createNodeFromType()` at `graphEditor/src/jvmMain/kotlin/ui/DragAndDropHandler.kt` (~line 267). Change `configuration = emptyMap()` to `configuration = nodeType.defaultConfiguration` (research decision R3)

**Checkpoint**: Both click-to-place and drag-and-drop paths now preserve CodeNode identity. US2 acceptance scenarios 1-2 should pass.

---

## Phase 4: User Story 3 — Remove Legacy CustomNodeDefinition Infrastructure (Priority: P3)

**Goal**: Remove all legacy CustomNodeDefinition infrastructure — JSON repository, FileCustomNodeRepository, legacy palette discovery, and the legacy "Create" button. All node creation flows exclusively through CodeNodeDefinitions.

**Independent Test**: Launch graphEditor, verify palette populates from CodeNodeDefinitions only, verify no "Create" button exists, verify zero references to CustomNodeDefinition or FileCustomNodeRepository in active code.

### Implementation for User Story 3

- [ ] T005 [P] [US3] **DEFERRED** — Delete legacy repository files cannot be done yet: `FileCustomNodeRepository.kt`, `CustomNodeDefinition.kt`, `CustomNodeRepository.kt` are still actively used by the entity module generator (feature 047) for entity module creation, removal, moduleExists checks, and IP type property mapping. Requires entity module generator refactoring first.
- [X] T006 [P] [US3] Remove `customNodeRepository` constructor parameter, `legacyNodes` property, `discoverLegacyNodes()` method, and the legacy merge block in `getAllForPalette()` from `NodeDefinitionRegistry` at `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodeDefinitionRegistry.kt` (~lines 45-46, 55, 106-113, 251-255) (research decision R4)
- [X] T007 [P] [US3] Remove `customNodeRepository` constructor parameter and `createNode()` method from `NodeGeneratorViewModel` at `graphEditor/src/jvmMain/kotlin/viewmodel/NodeGeneratorViewModel.kt` (~lines 167-180) (research decision R6)
- [X] T008 [P] [US3] Remove the legacy "Create" button and `onCreate` callback from `NodeGeneratorPanel` at `graphEditor/src/jvmMain/kotlin/ui/NodeGeneratorPanel.kt` (~lines 60-67) (research decision R6)
- [X] T009 [US3] Clean up Main.kt: removed `customNodeRepository` from `NodeDefinitionRegistry` constructor, removed `customNodeRepository` from `NodeGeneratorViewModel` constructor, removed `onNodeCreated` callback from `NodeGeneratorPanel`, removed `customNodes` dependency from `nodeTypes` remember key. Note: `customNodeRepository` and `customNodes` state kept for entity module generator (feature 047) which still uses them.

**Checkpoint**: All legacy CustomNodeDefinition infrastructure removed. US3 acceptance scenarios 1-3 should pass.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validate the full pipeline end-to-end and ensure all success criteria pass.

- [X] T010 Run `./gradlew :graphEditor:compileKotlinJvm` — BUILD SUCCESSFUL (SC-003)
- [X] T011 Run `./gradlew :kotlinCompiler:jvmTest` — BUILD SUCCESSFUL (SC-003)
- [X] T012 Run `./gradlew :fbpDsl:jvmTest` — 7 pre-existing ContinuousFactoryTest failures only, no new failures (SC-003)
- [X] T013 Verify legacy references removed from active code paths — legacy palette discovery, "Create" button, createNode(), and onNodeCreated callback all removed. Remaining CustomNodeDefinition references are entity module generator infrastructure (feature 047), not legacy. SC-005 partially met (legacy paths removed; entity module generator references remain as active functionality).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies — start immediately. BLOCKS all user stories.
- **US1 (Phase 2)**: Depends on Phase 1 completion (T001). T002 and T003 can run in parallel.
- **US2 (Phase 3)**: Depends on Phase 1 completion (T001). Independent of US1.
- **US3 (Phase 4)**: Depends on Phase 1 completion (T001). Independent of US1 and US2, but best done after US1/US2 to avoid compilation errors during intermediate states.
- **Polish (Phase 5)**: Depends on all user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Depends only on T001. Modifies kotlinCompiler and graphEditor save pipeline.
- **User Story 2 (P2)**: Depends only on T001. Modifies graphEditor DragAndDropHandler.
- **User Story 3 (P3)**: Depends only on T001. Modifies/deletes graphEditor repository and UI files. Should be done last because T009 (Main.kt cleanup) removes parameters that T005-T008 make unused.

### Within Each User Story

- US1: T002 and T003 are parallel (different files)
- US2: Single task (T004)
- US3: T005-T008 are parallel (different files). T009 depends on T005-T008 completing first (removes references to deleted/modified code)

### Parallel Opportunities

- T002 and T003 can run in parallel (US1 — different files)
- T002/T003 and T004 can run in parallel (US1 + US2 — different modules)
- T005, T006, T007, T008 can run in parallel (US3 — different files)

---

## Parallel Example: User Story 1

```bash
# Launch US1 tasks together (different files, no dependencies):
Task: "T002 - Whitelist _-prefixed keys in FlowKtGenerator.kt"
Task: "T003 - Remove ProcessingLogicStubGenerator from ModuleSaveService.kt"
```

## Parallel Example: User Story 3

```bash
# Launch US3 file deletions and cleanups together:
Task: "T005 - Delete legacy repository files"
Task: "T006 - Clean up NodeDefinitionRegistry.kt"
Task: "T007 - Clean up NodeGeneratorViewModel.kt"
Task: "T008 - Clean up NodeGeneratorPanel.kt"
# Then sequentially:
Task: "T009 - Clean up Main.kt (depends on T005-T008)"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001)
2. Complete Phase 2: User Story 1 (T002-T003)
3. **STOP and VALIDATE**: Save StopWatch, verify CodeNode-driven Flow file
4. Re-save and verify metadata round-trip

### Incremental Delivery

1. T001 → Foundation ready (metadata injected)
2. T002-T003 → US1 complete → Save produces CodeNode Flows
3. T004 → US2 complete → Drag-and-drop retains identity
4. T005-T009 → US3 complete → Legacy infrastructure removed
5. T010-T013 → Polish → Full validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently testable after Phase 1 foundation
- No new tests requested — existing tests must pass (SC-003)
- Commit after each phase completion
- The 3 legacy files deleted in T005 are the only files removed; all other tasks modify existing files
