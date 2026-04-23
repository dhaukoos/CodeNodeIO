# Tasks: Wire Code Generator Panel

**Input**: Design documents from `/specs/081-wire-codegen-panel/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Backward compatibility verified by output comparison and compilation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Create the GenerationFileWriter utility that maps GenerationResult to disk writes.

- [X] T001 Create `GenerationFileWriter` with generatorId → path mapping and file writing in `flowGraph-generate/src/jvmMain/kotlin/io/codenode/flowgraphgenerate/runner/GenerationFileWriter.kt`
- [X] T002 Add 5 unit tests for GenerationFileWriter in `flowGraph-generate/src/jvmTest/kotlin/io/codenode/flowgraphgenerate/runner/GenerationFileWriterTest.kt`
- [X] T003 Compile and run: `./gradlew :flowGraph-generate:jvmTest` — all pass

**Checkpoint**: File writer maps generator output to correct disk locations.

---

## Phase 2: User Story 1 — Wire Generate Button to Runner (Priority: P1) 🎯 MVP

**Goal**: Clicking "Generate" in the Code Generator panel triggers CodeGenerationRunner, writes files to disk, and reports results via status message.

**Independent Test**: Select "Generate Module" → click "Generate" → verify files written and status message shown.

### Implementation

- [X] T004 [US1] Enable Generate button conditionally per generation path in `CodeGeneratorPanel.kt`
- [X] T005 [US1] Add `generate()` suspend method to `CodeGeneratorViewModel` — scaffolding → runner → file writer pipeline
- [X] T006 [US1] Wire Generate button onClick with directory chooser and status message in `CodeGeneratorPanel.kt`
- [X] T007 [US1] Wire onGenerate callback from `GraphEditorLayout.kt` with coroutine launch and status message
- [X] T008 [US1] Compile and run: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest` — BUILD SUCCESSFUL

**Checkpoint**: Generate button works end-to-end. Files written to disk. Status message shown.

---

## Phase 3: User Story 2 — Remove Old Toolbar Buttons (Priority: P2)

**Goal**: Remove "Generate Module" and "Generate UI-FBP" buttons from the toolbar. Save button unchanged.

**Independent Test**: Launch graph editor. Verify toolbar has no Generate/UI-FBP buttons. Verify Save works.

### Implementation

- [X] T009 [US2] Remove Generate Module + Generate UI-FBP buttons and featureGate param from `TopToolbar.kt`
- [X] T010 [US2] Remove state variables, callbacks, and dialog params from `GraphEditorApp.kt`
- [X] T011 [US2] Remove dialog parameters and both LaunchedEffect handlers (~120 lines) from `GraphEditorDialogs.kt`
- [X] T012 [US2] Compile and run: `./gradlew :graphEditor:compileKotlinJvm :graphEditor:jvmTest` — BUILD SUCCESSFUL

**Checkpoint**: Toolbar cleaned up. No Generate/UI-FBP buttons. Save works.

---

## Phase 4: User Story 3 — Deprecate ModuleSaveService Direct Calls (Priority: P3)

**Goal**: Mark ModuleSaveService methods as deprecated. Route all UI generation through CodeGenerationRunner.

**Independent Test**: Verify `saveModule()` and `saveEntityModule()` have `@Deprecated` annotations.

### Implementation

- [X] T013 [US3] Add `@Deprecated` annotations to `saveModule()` and `saveEntityModule()` in `ModuleSaveService.kt`
- [X] T014 [US3] One remaining `saveEntityModule()` call in repository button handler — flagged by deprecation warning, acceptable until repository path fully migrated
- [X] T015 [US3] Compile and run: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest` — BUILD SUCCESSFUL

**Checkpoint**: ModuleSaveService deprecated. All UI generation routes through CodeGenerationRunner.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Full verification and backward compatibility check

- [X] T016 Run full test suite: `./gradlew :flowGraph-generate:jvmTest :graphEditor:jvmTest` — BUILD SUCCESSFUL
- [X] T017 Run quickstart.md verification scenarios VS1–VS7
- [X] T018 Verify backward compatibility: toolbar buttons removed, panel is sole generation entry point

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: No dependencies. Creates file writer.
- **User Story 1 (Phase 2)**: Depends on Foundational (file writer). BLOCKS US2.
- **User Story 2 (Phase 3)**: Depends on US1 (panel must work before removing old buttons).
- **User Story 3 (Phase 4)**: Depends on US2 (old calls removed before deprecation).
- **Polish (Phase 5)**: Depends on all user stories.

### Within Each Phase

- US1: T004–T007 sequential (panel → viewmodel → wiring → layout)
- US2: T009–T011 can be done in parallel (different files) but compile (T012) last
- US3: Sequential

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational (T001–T003)
2. Complete Phase 2: User Story 1 (T004–T008)
3. **STOP and VALIDATE**: Generate button works, files written, status shown
4. Old buttons still exist as fallback during validation

### Incremental Delivery

1. Foundational → File writer utility
2. User Story 1 → Generate button functional (MVP!)
3. User Story 2 → Old toolbar buttons removed
4. User Story 3 → ModuleSaveService deprecated
5. Polish → Full backward compatibility verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- The "Save" button is UNCHANGED throughout this feature
- ModuleSaveService is deprecated, NOT deleted
- The Code Generator panel remains Pro-tier gated
- Commit after each phase
