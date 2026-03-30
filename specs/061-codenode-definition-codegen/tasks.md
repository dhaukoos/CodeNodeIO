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

- [ ] T001 Create `EntityCUDCodeNodeGenerator` class at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityCUDCodeNodeGenerator.kt` — generates `{Entity}CUDCodeNode.kt` implementing `CodeNodeDefinition` with SOURCE category, 3 typed output PortSpecs (save, update, remove using IP type), and `createSourceOut3<{Entity}, {Entity}, {Entity}>` runtime with coroutineScope + 3 launch blocks collecting from `{PluralName}State._save/_update/_remove` StateFlows (per contracts/generated-node-contract.md).
- [ ] T002 Create `EntityRepositoryCodeNodeGenerator` class at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityRepositoryCodeNodeGenerator.kt` — generates `{Entity}RepositoryCodeNode.kt` implementing `CodeNodeDefinition` with TRANSFORMER category, 3 typed input PortSpecs (IP type), 2 output PortSpecs (String for result/error), `anyInput = true`, identity tracking vars, `createIn3AnyOut2Processor` runtime with `toEntity()` conversion at DAO boundary (per contracts/generated-node-contract.md).
- [ ] T003 Create `EntityDisplayCodeNodeGenerator` class at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityDisplayCodeNodeGenerator.kt` — generates `{PluralName}DisplayCodeNode.kt` implementing `CodeNodeDefinition` with SINK category, 2 typed input PortSpecs (String for result/error), `createSinkIn2<String, String>` runtime updating `{PluralName}State._result/_error` (per contracts/generated-node-contract.md).

**Checkpoint**: Three new generator classes exist and produce valid Kotlin source strings

---

## Phase 2: Foundational — Wire Generators into EntityModuleGenerator

**Purpose**: Connect the new generators to the orchestrator and update the FlowGraph builder. MUST complete before user stories can be validated.

- [ ] T004 Modify `EntityModuleGenerator` at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityModuleGenerator.kt` — replace `cudGenerator` (EntityCUDGenerator) and `displayGenerator` (EntityDisplayGenerator) with the new `EntityCUDCodeNodeGenerator`, `EntityRepositoryCodeNodeGenerator`, and `EntityDisplayCodeNodeGenerator`. Output the 3 generated node files to `src/commonMain/kotlin/$basePackagePath/nodes/` instead of the base package path.
- [ ] T005 Modify `EntityFlowGraphBuilder` at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityFlowGraphBuilder.kt` — add `config("_codeNodeClass", "io.codenode.{module}.nodes.{NodeName}CodeNode")` and `config("_genericType", "{inXoutY}")` to all three nodes (CUD: in0out3, Repository: in3anyout2, Display: in2out0). Remove `_codeNodeDefinition` config if present.
- [ ] T006 Verify kotlinCompiler builds: `./gradlew :kotlinCompiler:compileCommonMainKotlinMetadata`

**Checkpoint**: EntityModuleGenerator uses new generators, FlowGraph builder tags all nodes with _codeNodeClass

---

## Phase 3: User Story 1 — Generated Modules Compile and Run (Priority: P1) MVP

**Goal**: A developer creates a repository module and it compiles on first attempt with zero manual fixes.

**Independent Test**: Create "TestItem" IP type, click "Create Repository Module", compile with `./gradlew :TestItems:jvmJar`.

### Implementation for User Story 1

- [ ] T007 [US1] Update `RuntimeFlowGenerator` at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGenerator.kt` — remove the `legacyNodes` filter and all code that handles legacy tick functions (tick function imports, factory function calls with tick params, legacy source/sink/processor handling). All nodes now go through the `codeNodeClassNodes` path: import by FQCN, instantiate via `createRuntime()`.
- [ ] T008 [US1] Update `ModuleGenerator` at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ModuleGenerator.kt` — ensure generated `build.gradle.kts` includes `implementation("io.codenode:preview-api")` in jvmMain source set (already started in feature 060).
- [ ] T009 [US1] Update `ModuleSaveService` at `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt` — ensure `unwireGraphEditorIntegration` cleans up the `nodes/` subdirectory when removing a module (delete `{Module}/src/commonMain/kotlin/.../nodes/` directory contents).
- [ ] T010 [US1] Verify build: `./gradlew :kotlinCompiler:jvmTest` — ensure all existing generator tests pass with the updated RuntimeFlowGenerator.
- [ ] T011 [US1] End-to-end test: Create a "TestItem" IP type in the graphEditor, click "Create Repository Module", compile the generated module with `./gradlew :TestItems:jvmJar` from the DemoProject. Verify zero compilation errors.

**Checkpoint**: Generated modules compile out of the box. No tick function errors. No type mismatches.

---

## Phase 4: User Story 2 — Generated Nodes Match Existing Pattern (Priority: P2)

**Goal**: Generated code follows the same architectural pattern as hand-written nodes.

**Independent Test**: Compare generated `*RepositoryCodeNode.kt` with hand-written `UserProfileRepositoryCodeNode.kt`.

### Implementation for User Story 2

- [ ] T012 [US2] Verify generated `{Entity}CUDCodeNode.kt` uses typed output ports (`{Entity}::class` not `Any::class`), imports from `iptypes` package, and uses `createSourceOut3<{Entity}, {Entity}, {Entity}>`.
- [ ] T013 [US2] Verify generated `{Entity}RepositoryCodeNode.kt` uses typed input ports (`{Entity}::class`), String output ports, identity tracking pattern (`lastSaveRef`, etc.), `toEntity()` conversion at DAO boundary, and proper error handling with `ProcessResult2`.
- [ ] T014 [US2] Verify generated `{PluralName}DisplayCodeNode.kt` uses `String::class` input ports and `createSinkIn2<String, String>`.
- [ ] T015 [US2] Verify the generated `.flow.kt` file shows concrete IP type names in port declarations (not `Any::class`) by loading the module in the graphEditor and switching to Textual view.

**Checkpoint**: Generated nodes are structurally identical to hand-written nodes. Types are concrete.

---

## Phase 5: User Story 3 — Legacy Code Paths Eliminated (Priority: P3)

**Goal**: No legacy tick functions or factory functions in generated code. RuntimeFlowGenerator simplified.

**Independent Test**: Inspect generated `*Flow.kt` for absence of tick function references. Verify existing modules still work.

### Implementation for User Story 3

- [ ] T016 [US3] Delete `EntityCUDGenerator` at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityCUDGenerator.kt` and its test file. Replaced by `EntityCUDCodeNodeGenerator`.
- [ ] T017 [US3] Delete `EntityDisplayGenerator` at `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityDisplayGenerator.kt` and its test file. Replaced by `EntityDisplayCodeNodeGenerator`.
- [ ] T018 [US3] Verify generated `{PluralName}Flow.kt` contains NO tick function imports (no `import ... xxxTick`) and NO legacy factory calls (no `createXxxCUD()`, no `createXxxDisplay()`).
- [ ] T019 [US3] Regression test: Verify existing modules compile — run from DemoProject: `./gradlew :UserProfiles:jvmJar :GeoLocations:jvmJar :Addresses:jvmJar :StopWatch:jvmJar :EdgeArtFilter:jvmJar :WeatherForecast:jvmJar`.

**Checkpoint**: Legacy code paths deprecated. No regressions in existing modules.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Test cleanup, documentation, and final validation

- [ ] T020 [P] Update existing generator tests at `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/` — update `EntityModuleGeneratorTest` expectations for new file paths (nodes/ subdirectory) and CodeNodeDefinition output.
- [ ] T021 [P] Update `RuntimeFlowGeneratorTest` at `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeFlowGeneratorTest.kt` — remove tests for legacy tick function generation, add tests verifying CodeNodeDefinition-only path.
- [ ] T022 Run full test suite: `./gradlew :kotlinCompiler:jvmTest` — verify all tests pass.
- [ ] T023 Run quickstart.md scenarios 1-5 to validate end-to-end functionality.
- [ ] T024 Clean up test artifacts (remove any TestItem module created during testing).

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
