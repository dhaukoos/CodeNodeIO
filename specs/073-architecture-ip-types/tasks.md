# Tasks: Architecture IP Types

**Input**: Design documents from `/specs/073-architecture-ip-types/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Unit tests are included for the IPTypeDiscovery extension (foundational infrastructure change).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Setup

**Purpose**: Create the `iptypes` Gradle module and register it in the build system

- [X] T001 Create `iptypes/build.gradle.kts` with KMP configuration and dependencies on `:fbpDsl`, `:flowGraph-persist` (commonMain), `:flowGraph-persist`, `:flowGraph-execute` (jvmMain)
- [X] T002 Add `include(":iptypes")` to `settings.gradle.kts`
- [X] T003 Create source directories: `iptypes/src/commonMain/kotlin/io/codenode/iptypes/` and `iptypes/src/jvmMain/kotlin/io/codenode/iptypes/`
- [X] T004 Verify module compiles: `./gradlew :iptypes:compileKotlinJvm`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Extend IPTypeDiscovery to parse `typealias` declarations — MUST be complete before IP type files can be discovered

**⚠️ CRITICAL**: US1 IP type files using `typealias` cannot be discovered until this phase is complete

### Tests

- [X] T005 Add test: parse typealias IP type file returns correct IPTypeFileMeta (typeName, typeId, color, empty properties, className) in `flowGraph-types/src/jvmTest/kotlin/.../discovery/IPTypeDiscoveryTest.kt`
- [X] T006 Add test: parse data class IP type file returns unchanged behavior (backward compatibility) in `flowGraph-types/src/jvmTest/kotlin/.../discovery/IPTypeDiscoveryTest.kt`
- [X] T007 Add test: parse file with `@IPType` but no data class or typealias skips gracefully in `flowGraph-types/src/jvmTest/kotlin/.../discovery/IPTypeDiscoveryTest.kt`

### Implementation

- [X] T008 Add `typealiasPattern` regex (`typealias\s+(\w+)\s*=\s*(.+)`) to `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/discovery/IPTypeDiscovery.kt`
- [X] T009 Extend parsing logic in IPTypeDiscovery to try `dataClassPattern` first, then fall back to `typealiasPattern`, creating `IPTypeFileMeta` with empty properties for typealias matches in `flowGraph-types/src/jvmMain/kotlin/io/codenode/flowgraphtypes/discovery/IPTypeDiscovery.kt`
- [X] T010 Run `./gradlew :flowGraph-types:jvmTest` to verify all discovery tests pass (new + existing)

**Checkpoint**: IPTypeDiscovery can now parse both `data class` and `typealias` IP type files

---

## Phase 3: User Story 1 — Register Architecture IP Types (Priority: P1) 🎯 MVP

**Goal**: Create 14 IP type files so all architecture-specific types appear in the IP type palette with distinct names, colors, and descriptions

**Independent Test**: Launch graph editor, open IP type palette, verify all 14 architecture IP types are listed with distinct colors. No changes to architecture.flow.kt needed yet.

### commonMain Typealias Types (5 files)

- [X] T011 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/NodeDescriptors.kt` — typealias `NodeDescriptors = List<NodeTypeDefinition>`, @TypeId ip_nodedescriptors, @Color rgb(233, 30, 99)
- [X] T012 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/IPTypeMetadata.kt` — typealias `IPTypeMetadata = List<InformationPacketType>`, @TypeId ip_iptypemetadata, @Color rgb(0, 188, 212)
- [X] T013 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/FlowGraphModel.kt` — typealias `FlowGraphModel = FlowGraph`, @TypeId ip_flowgraphmodel, @Color rgb(121, 85, 72)
- [X] T014 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/GraphNodeTemplates.kt` — typealias `GraphNodeTemplates = List<GraphNodeTemplateMeta>`, @TypeId ip_graphnodetemplates, @Color rgb(63, 81, 181)
- [X] T015 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/RuntimeExecutionState.kt` — typealias `RuntimeExecutionState = ExecutionState`, @TypeId ip_runtimeexecutionstate, @Color rgb(0, 150, 136)

### commonMain Data Class Types (5 files)

- [X] T016 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/FilesystemPath.kt` — data class with `val path: String`, @TypeId ip_filesystempath, @Color rgb(96, 125, 139)
- [X] T017 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/ClasspathEntry.kt` — data class with `val entry: String`, @TypeId ip_classpathentry, @Color rgb(139, 195, 74)
- [X] T018 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/IPTypeCommand.kt` — data class with `val command: String, val targetTypeId: String, val payload: String`, @TypeId ip_iptypecommand, @Color rgb(244, 67, 54)
- [X] T019 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/GeneratedOutput.kt` — data class with `val files: String, val status: String, val errorMessage: String?`, @TypeId ip_generatedoutput, @Color rgb(255, 193, 7)
- [X] T020 [P] [US1] Create `iptypes/src/commonMain/kotlin/io/codenode/iptypes/GenerationContext.kt` — data class with `val flowGraphModel: String, val serializedOutput: String`, @TypeId ip_generationcontext, @Color rgb(3, 169, 244)

### jvmMain Typealias Types (2 files)

- [X] T021 [P] [US1] Create `iptypes/src/jvmMain/kotlin/io/codenode/iptypes/LoadedFlowGraph.kt` — typealias `LoadedFlowGraph = ParseResult`, @TypeId ip_loadedflowgraph, @Color rgb(255, 87, 34)
- [X] T022 [P] [US1] Create `iptypes/src/jvmMain/kotlin/io/codenode/iptypes/DataFlowAnimations.kt` — typealias `DataFlowAnimations = List<ConnectionAnimation>`, @TypeId ip_dataflowanimations, @Color rgb(205, 220, 57)

### jvmMain Data Class Types (2 files)

- [X] T023 [P] [US1] Create `iptypes/src/jvmMain/kotlin/io/codenode/iptypes/DebugSnapshots.kt` — data class with `val connectionId: String, val timestamp: String, val value: String`, @TypeId ip_debugsnapshots, @Color rgb(158, 158, 158)
- [X] T024 [P] [US1] Create `iptypes/src/jvmMain/kotlin/io/codenode/iptypes/EditorGraphState.kt` — data class with `val flowGraph: String, val selection: String, val panOffset: String, val scale: String, val isDirty: Boolean`, @TypeId ip_editorgraphstate, @Color rgb(103, 58, 183)

### Verification

- [X] T025 [US1] Compile iptypes module: `./gradlew :iptypes:compileKotlinJvm`
- [X] T026 [US1] Validate no duplicate TypeId values across all IP type files in `iptypes/src/`
- [X] T027 [US1] Run existing test suites to verify no regressions: `./gradlew :flowGraph-types:jvmTest :graphEditor:jvmTest`

**Checkpoint**: All 14 IP types are discoverable and visible in the palette. User Story 1 is independently testable.

---

## Phase 4: User Story 2 — Update Architecture Flow Graph Ports (Priority: P2)

**Goal**: Replace all `String::class` port types in `architecture.flow.kt` with the corresponding domain-specific IP types, making connections color-coded and self-documenting

**Independent Test**: Open `architecture.flow.kt` in the graph editor. Every port displays its domain-specific IP type name and color. The `serializedOutput` port remains typed as `String`. Save and re-open to confirm persistence.

### Implementation

- [ ] T028 [US2] Update all port declarations in `graphEditor/architecture.flow.kt` to replace `String::class` with the appropriate IP type class references per the Port-to-Type Mapping in plan.md (14 ports changed, `serializedOutput` remains `String::class`)
- [ ] T029 [US2] Add necessary imports to `graphEditor/architecture.flow.kt` for the new IP type classes from `io.codenode.iptypes`
- [ ] T030 [US2] Add `implementation(project(":iptypes"))` dependency to `graphEditor/build.gradle.kts` if needed for architecture.flow.kt compilation
- [ ] T031 [US2] Verify architecture.flow.kt compiles: `./gradlew :graphEditor:compileKotlinJvm`
- [ ] T032 [US2] Run graph editor tests: `./gradlew :graphEditor:jvmTest`

**Checkpoint**: All ports in architecture.flow.kt reference domain-specific IP types. Connections are color-coded. User Story 2 is independently testable.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Full verification across all quickstart scenarios

- [ ] T033 Run full test suite to verify no regressions: `./gradlew :flowGraph-types:jvmTest :graphEditor:jvmTest :iptypes:compileKotlinJvm`
- [ ] T034 Validate file format: verify typealias files contain `typealias` keyword, data class files contain `data class` keyword, all files have `@IPType` header
- [ ] T035 Run quickstart.md verification scenarios VS1–VS8

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories (typealias parsing must work before IP type files are discoverable)
- **User Story 1 (Phase 3)**: Depends on Foundational phase completion
- **User Story 2 (Phase 4)**: Depends on User Story 1 completion (IP types must exist before ports can reference them)
- **Polish (Phase 5)**: Depends on all user stories being complete

### Within Each Phase

- **Phase 2**: Tests (T005–T007) before implementation (T008–T009), then verification (T010)
- **Phase 3**: All 14 IP type files (T011–T024) can be created in parallel, then verification (T025–T027)
- **Phase 4**: Sequential — imports (T029) and deps (T030) before port updates (T028), then verification (T031–T032)

### Parallel Opportunities

```text
# Phase 3: All 14 IP type files can be created in parallel:
T011, T012, T013, T014, T015  (commonMain typealias)
T016, T017, T018, T019, T020  (commonMain data class)
T021, T022                     (jvmMain typealias)
T023, T024                     (jvmMain data class)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T004)
2. Complete Phase 2: Foundational (T005–T010)
3. Complete Phase 3: User Story 1 (T011–T027)
4. **STOP and VALIDATE**: All 14 IP types visible in palette with distinct colors

### Incremental Delivery

1. Setup + Foundational → IPTypeDiscovery supports typealias
2. User Story 1 → 14 IP types registered and visible (MVP!)
3. User Story 2 → architecture.flow.kt ports updated, connections color-coded
4. Polish → Full verification pass

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- `serializedOutput` port intentionally remains `String::class` (FR-004)
- EditorGraphState uses `data class` (not typealias) to avoid circular dependency with graphEditor
- jvmMain types: LoadedFlowGraph, DataFlowAnimations, DebugSnapshots, EditorGraphState
- Commit after each phase completion
