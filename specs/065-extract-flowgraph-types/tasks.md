# Tasks: Extract flowGraph-types Module

**Input**: Design documents from `/specs/065-extract-flowgraph-types/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Included — US4 explicitly requires TDD for the CodeNode.

**Organization**: Tasks follow the Strangler Fig execution sequence from plan.md. User stories are interleaved in the order required by the migration pattern (extract → TDD tests → implement → update call sites → remove originals → wire architecture).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Prerequisite Move + Module Creation)

**Purpose**: Move PlacementLevel to fbpDsl and create the flowGraph-types module skeleton

- [X] T001 Move `PlacementLevel.kt` from `graphEditor/src/jvmMain/kotlin/model/PlacementLevel.kt` to `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/PlacementLevel.kt` with package update to `io.codenode.fbpdsl.model`
- [X] T002 Update all imports of `io.codenode.grapheditor.model.PlacementLevel` to `io.codenode.fbpdsl.model.PlacementLevel` across the codebase
- [X] T003 Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` to verify no regressions after PlacementLevel move
- [X] T004 Add `include("flowGraph-types")` to `settings.gradle.kts`
- [X] T005 Create `flowGraph-types/build.gradle.kts` with KMP configuration depending only on `:fbpDsl`
- [X] T006 Create directory structure: `flowGraph-types/src/commonMain/kotlin/io/codenode/flowgraphtypes/model/`, `flowGraph-types/src/commonMain/kotlin/io/codenode/flowgraphtypes/registry/`, `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/discovery/`, `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/repository/`, `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/generator/`, `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/node/`, `flowGraph-types/src/jvmTest/kotlin/io/codenode/flowgraphtypes/node/`
- [X] T007 Run `./gradlew :flowGraph-types:build` to verify empty module compiles

**Checkpoint**: PlacementLevel lives in fbpDsl. flowGraph-types module exists and compiles. All existing tests pass.

---

## Phase 2: File Extraction (US1 — Extract Files to Module)

**Purpose**: Copy all 9 IP type files to flowGraph-types with updated packages (keep originals for Strangler Fig safety)

**Goal**: All 9 files exist in flowGraph-types with correct packages; originals remain in graphEditor temporarily

**Independent Test**: `./gradlew :flowGraph-types:build` succeeds; `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest` still passes

### commonMain files (5 files)

- [ ] T008 [P] [US1] Copy `IPProperty.kt` from `graphEditor/src/jvmMain/kotlin/model/IPProperty.kt` to `flowGraph-types/src/commonMain/kotlin/io/codenode/flowgraphtypes/model/IPProperty.kt` with package update to `io.codenode.flowgraphtypes.model`
- [ ] T009 [P] [US1] Copy `IPPropertyMeta.kt` from `graphEditor/src/jvmMain/kotlin/model/IPPropertyMeta.kt` to `flowGraph-types/src/commonMain/kotlin/io/codenode/flowgraphtypes/model/IPPropertyMeta.kt` with package update to `io.codenode.flowgraphtypes.model`
- [ ] T010 [P] [US1] Copy `IPTypeFileMeta.kt` from `graphEditor/src/jvmMain/kotlin/model/IPTypeFileMeta.kt` to `flowGraph-types/src/commonMain/kotlin/io/codenode/flowgraphtypes/model/IPTypeFileMeta.kt` with package update to `io.codenode.flowgraphtypes.model`
- [ ] T011 [P] [US1] Copy `SerializableIPType.kt` from `graphEditor/src/jvmMain/kotlin/model/SerializableIPType.kt` to `flowGraph-types/src/commonMain/kotlin/io/codenode/flowgraphtypes/model/SerializableIPType.kt` with package update to `io.codenode.flowgraphtypes.model`
- [ ] T012 [P] [US1] Copy `IPTypeRegistry.kt` from `graphEditor/src/jvmMain/kotlin/state/IPTypeRegistry.kt` to `flowGraph-types/src/commonMain/kotlin/io/codenode/flowgraphtypes/registry/IPTypeRegistry.kt` with package update to `io.codenode.flowgraphtypes.registry`

### jvmMain files (4 files)

- [ ] T013 [P] [US1] Copy `IPTypeDiscovery.kt` from `graphEditor/src/jvmMain/kotlin/state/IPTypeDiscovery.kt` to `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/discovery/IPTypeDiscovery.kt` with package update to `io.codenode.flowgraphtypes.discovery`
- [ ] T014 [P] [US1] Copy `FileIPTypeRepository.kt` from `graphEditor/src/jvmMain/kotlin/repository/FileIPTypeRepository.kt` to `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/repository/FileIPTypeRepository.kt` with package update to `io.codenode.flowgraphtypes.repository`
- [ ] T015 [P] [US1] Copy `IPTypeMigration.kt` from `graphEditor/src/jvmMain/kotlin/repository/IPTypeMigration.kt` to `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/repository/IPTypeMigration.kt` with package update to `io.codenode.flowgraphtypes.repository`
- [ ] T016 [P] [US1] Copy `IPTypeFileGenerator.kt` from `graphEditor/src/jvmMain/kotlin/state/IPTypeFileGenerator.kt` to `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/generator/IPTypeFileGenerator.kt` with package update to `io.codenode.flowgraphtypes.generator`

### Verification

- [ ] T017 [US1] Update internal imports within copied files to reference new `io.codenode.flowgraphtypes.*` packages
- [ ] T018 [US1] Run `./gradlew :flowGraph-types:build` to verify new module compiles with all 9 files
- [ ] T019 [US1] Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` to verify no regressions (originals still in graphEditor)

**Checkpoint**: 9 files exist in flowGraph-types with correct packages. Module compiles. All existing tests pass. Originals remain in graphEditor.

---

## Phase 3: TDD CodeNode Tests (US4 — Test CodeNode Contract)

**Purpose**: Write CodeNode tests BEFORE implementation (TDD). Tests must FAIL initially.

**Goal**: Complete test suite for FlowGraphTypesCodeNode covering port contract, data flow, command processing, and boundary conditions

**Independent Test**: Tests compile but FAIL (no implementation yet)

- [ ] T020 [US4] Create `FlowGraphTypesCodeNodeTest.kt` at `flowGraph-types/src/jvmTest/kotlin/io/codenode/flowgraphtypes/node/FlowGraphTypesCodeNodeTest.kt` with test verifying the CodeNode has exactly 3 input ports (`filesystemPaths`, `classpathEntries`, `ipTypeCommands`) and 1 output port (`ipTypeMetadata`)
- [ ] T021 [US4] Add test verifying the CodeNode uses `anyInput` mode — re-emits on any input change
- [ ] T022 [US4] Add test verifying data flows through channels: providing filesystem paths and classpath entries produces IP type metadata output
- [ ] T023 [US4] Add test verifying mutation commands (register, unregister) through `ipTypeCommands` produce updated metadata output
- [ ] T024 [US4] Add tests verifying boundary conditions: empty inputs, invalid paths, malformed commands are handled gracefully
- [ ] T025 [US4] Run `./gradlew :flowGraph-types:jvmTest` to confirm tests compile but FAIL (no CodeNode implementation)

**Checkpoint**: TDD tests written and confirmed failing. Git history shows tests before implementation.

---

## Phase 4: CodeNode Implementation (US3 — Wrap as CodeNode)

**Purpose**: Implement the FlowGraphTypesCodeNode as In3AnyOut1Runtime wrapper

**Goal**: CodeNode wraps IPTypeDiscovery, IPTypeRegistry, FileIPTypeRepository, and IPTypeFileGenerator behind 3 input ports and 1 output port

**Independent Test**: `./gradlew :flowGraph-types:jvmTest` — all CodeNode tests pass

- [ ] T026 [US3] Create `FlowGraphTypesCodeNode.kt` at `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/node/FlowGraphTypesCodeNode.kt` implementing `CodeNodeDefinition` with `In3AnyOut1Runtime<String, String, String, String>` and `anyInput = true`
- [ ] T027 [US3] Implement processing logic: filesystemPaths/classpathEntries changes trigger IPTypeDiscovery re-scan; ipTypeCommands changes deserialize and apply mutations (register, unregister, generate, update color); all cases serialize current registry state and emit as ipTypeMetadata
- [ ] T028 [US3] Run `./gradlew :flowGraph-types:jvmTest` to verify all CodeNode tests pass

**Checkpoint**: CodeNode implemented. All TDD tests pass. Module builds independently.

---

## Phase 5: Data Flow Contract Migration (US2 — Update Call Sites)

**Purpose**: Migrate 6 call sites from direct IPTypeRegistry access to data flow consumption via CodeNode ports

**Goal**: Read-only consumers hold local `ipTypeMetadata`; mutating consumers send `ipTypeCommands`

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all tests pass

- [ ] T029 [US2] Add dependency on `:flowGraph-types` in `graphEditor/build.gradle.kts`
- [ ] T030 [US2] Update `graphEditor/src/jvmMain/kotlin/viewmodel/SharedStateProvider.kt` to hold current `ipTypeMetadata` data (received from CodeNode output channel) instead of `IPTypeRegistry` instance
- [ ] T031 [US2] Update `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` to query locally-held `ipTypeMetadata` instead of `ipTypeRegistry.getByTypeName(name)`
- [ ] T032 [US2] Update `graphEditor/src/jvmMain/kotlin/serialization/GraphNodeTemplateSerializer.kt` to receive `ipTypeMetadata` as parameter and query locally instead of `ipTypeRegistry.getByTypeName(name)`
- [ ] T033 [US2] Update `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt` to query locally-held `ipTypeMetadata` instead of `ipTypeRegistry.getAllTypes()` / `.getById(id)`
- [ ] T034 [US2] Update `graphEditor/src/jvmMain/kotlin/viewmodel/IPGeneratorViewModel.kt` to send `ipTypeCommands` (register/generate) to CodeNode input port and receive updated `ipTypeMetadata` from output, replacing direct `ipTypeRegistry.register(...)`, `discovery.parseIPTypeFile(...)`, `repository.add(...)` calls
- [ ] T035 [US2] Update `graphEditor/src/jvmMain/kotlin/viewmodel/IPPaletteViewModel.kt` to send `ipTypeCommands` (unregister/remove) to CodeNode input port and receive updated `ipTypeMetadata` from output, replacing direct `ipTypeRegistry.unregister(id)`, `repository.remove(id)` calls
- [ ] T036 [US2] Wire FlowGraphTypesCodeNode in composition root: construct, start, send filesystemPaths/classpathEntries/ipTypeCommands to input ports, distribute ipTypeMetadata output to ViewModels and UI state
- [ ] T037 [US2] Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` to verify no regressions after call site migration

**Checkpoint**: All 6 call sites migrated to data flow consumption. No direct IPTypeRegistry/IPTypeDiscovery/FileIPTypeRepository imports remain in call site files. All tests pass.

---

## Phase 6: Remove Originals (US1 — Complete Extraction)

**Purpose**: Remove the original 9 files from graphEditor (Strangler Fig completion)

**Goal**: No copies of the 9 extracted files remain in graphEditor

**Independent Test**: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` — all tests pass

- [ ] T038 [P] [US1] Remove `graphEditor/src/jvmMain/kotlin/model/IPProperty.kt`
- [ ] T039 [P] [US1] Remove `graphEditor/src/jvmMain/kotlin/model/IPPropertyMeta.kt`
- [ ] T040 [P] [US1] Remove `graphEditor/src/jvmMain/kotlin/model/IPTypeFileMeta.kt`
- [ ] T041 [P] [US1] Remove `graphEditor/src/jvmMain/kotlin/model/SerializableIPType.kt`
- [ ] T042 [P] [US1] Remove `graphEditor/src/jvmMain/kotlin/state/IPTypeRegistry.kt`
- [ ] T043 [P] [US1] Remove `graphEditor/src/jvmMain/kotlin/state/IPTypeDiscovery.kt`
- [ ] T044 [P] [US1] Remove `graphEditor/src/jvmMain/kotlin/repository/FileIPTypeRepository.kt`
- [ ] T045 [P] [US1] Remove `graphEditor/src/jvmMain/kotlin/repository/IPTypeMigration.kt`
- [ ] T046 [P] [US1] Remove `graphEditor/src/jvmMain/kotlin/state/IPTypeFileGenerator.kt`
- [ ] T047 [US1] Update remaining graphEditor imports to use `io.codenode.flowgraphtypes.*` packages for any files that still reference the extracted types
- [ ] T048 [US1] Run `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest` to verify no regressions after original file removal

**Checkpoint**: Original files removed. graphEditor depends on flowGraph-types for all IP type functionality. All tests pass.

---

## Phase 7: Architecture FlowGraph Wiring (US5 — Wire into architecture.flow.kt)

**Purpose**: Add `ipTypeCommands` port, connection from graphEditor-source, and populate flowGraph-types GraphNode container with the CodeNode

**Goal**: architecture.flow.kt reflects the live flowGraph-types CodeNode with 3 inputs, 1 output, 20 total connections

**Independent Test**: `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"` — all tests pass

- [ ] T049 [US5] Add `ipTypeCommands` input port to the flowGraph-types GraphNode container in `graphEditor/architecture.flow.kt`
- [ ] T050 [US5] Add connection from `graphEditor-source` to `flowGraph-types.ipTypeCommands` in `graphEditor/architecture.flow.kt`
- [ ] T051 [US5] Populate the flowGraph-types GraphNode container with `FlowGraphTypesCodeNode` in `graphEditor/architecture.flow.kt`
- [ ] T052 [US5] Update `ArchitectureFlowKtsTest` for 20 connections (from 19) and verify: `architecture flow kt has exactly 20 connections`, `graphEditor-source has only command outputs` includes ipTypeCommands, all existing test assertions remain valid
- [ ] T053 [US5] Run `./gradlew :graphEditor:jvmTest --tests "characterization.ArchitectureFlowKtsTest"` to verify all architecture tests pass

**Checkpoint**: architecture.flow.kt updated. flowGraph-types node has 3 inputs, 1 output. 20 connections. All architecture tests pass.

---

## Phase 8: Verification & Cross-Cutting Concerns (US6, US7)

**Purpose**: Verify cyclic dependency elimination and Strangler Fig pattern compliance

- [ ] T054 [US6] Verify no dependency on `:graphEditor` in `flowGraph-types/build.gradle.kts` — dependency direction is graphEditor → flowGraph-types → fbpDsl
- [ ] T055 [US6] Verify no circular dependency exists by running `./gradlew :flowGraph-types:dependencies` and confirming only fbpDsl appears
- [ ] T056 [US7] Verify Strangler Fig sequence in git history: prerequisite move → module creation → file copy → TDD tests → CodeNode implementation → call site update → old file removal → architecture wiring
- [ ] T057 Run full test suite: `./gradlew :graphEditor:jvmTest :kotlinCompiler:jvmTest :circuitSimulator:jvmTest :flowGraph-types:jvmTest` to verify zero regressions
- [ ] T058 Run quickstart.md validation scenarios (Scenarios 1-4, 6-10)

**Checkpoint**: All verification complete. No cycles. Strangler Fig pattern followed. Zero regressions.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — starts immediately
- **Phase 2 (Extract Files)**: Depends on Phase 1 completion (module must exist)
- **Phase 3 (TDD Tests)**: Depends on Phase 2 completion (files must be in module for CodeNode to reference)
- **Phase 4 (CodeNode Implementation)**: Depends on Phase 3 completion (TDD — tests must exist and fail first)
- **Phase 5 (Call Site Migration)**: Depends on Phase 4 completion (CodeNode must exist for call sites to consume)
- **Phase 6 (Remove Originals)**: Depends on Phase 5 completion (all consumers must use new module)
- **Phase 7 (Architecture Wiring)**: Depends on Phase 4 completion (CodeNode must exist); can run in parallel with Phases 5-6 if architecture tests are independent
- **Phase 8 (Verification)**: Depends on all previous phases

### User Story Dependencies

- **US1 (Extract)**: Phase 1 → Phase 2 → Phase 6 (spans multiple phases due to Strangler Fig)
- **US2 (Data Flow Contracts)**: Depends on US3 (CodeNode must exist before call sites can consume its ports)
- **US3 (Wrap as CodeNode)**: Depends on US4 (TDD — tests first)
- **US4 (TDD Tests)**: Depends on US1 Phase 2 (files must be in module)
- **US5 (Architecture Wiring)**: Depends on US3 (CodeNode must exist to populate container)
- **US6 (Cycle Elimination)**: Verification only — depends on US1 completion
- **US7 (Strangler Fig Validation)**: Verification only — depends on all other stories

### Within Each Phase

- Tasks marked [P] can run in parallel
- File copies (T008-T016) are all independent [P] tasks
- File removals (T038-T046) are all independent [P] tasks
- Call site updates (T030-T035) are mostly independent but T030 (SharedStateProvider) should run first as other call sites may depend on its data distribution

### Parallel Opportunities

```text
# Phase 2: All 9 file copies can run in parallel
T008, T009, T010, T011, T012, T013, T014, T015, T016

# Phase 6: All 9 file removals can run in parallel
T038, T039, T040, T041, T042, T043, T044, T045, T046
```

---

## Implementation Strategy

### MVP First (US1 + US4 + US3)

1. Complete Phase 1: Setup (PlacementLevel move + module creation)
2. Complete Phase 2: Extract files to module
3. Complete Phase 3: TDD tests for CodeNode
4. Complete Phase 4: CodeNode implementation
5. **STOP and VALIDATE**: Module builds, CodeNode tests pass, existing tests pass

### Incremental Delivery

1. Setup + Extract → Module exists with all files (US1 partial)
2. TDD + CodeNode → Module has working CodeNode (US4 + US3)
3. Call site migration → graphEditor consumes via data flow (US2)
4. Remove originals → Extraction complete (US1 complete)
5. Architecture wiring → FlowGraph reflects reality (US5)
6. Verification → Cycles eliminated, Strangler Fig validated (US6 + US7)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- The Strangler Fig pattern dictates phase ordering — files are copied first, originals removed later
- TDD is mandatory for US4: tests MUST be committed before CodeNode implementation
- Commit after each phase to maintain safe intermediate states
- No service interfaces — all module boundaries are data flow through CodeNode ports
- `ipTypeCommands` is a NEW port not in current architecture.flow.kt
