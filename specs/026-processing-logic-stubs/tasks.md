# Tasks: Redesign Processing Logic Stub Generator

**Input**: Design documents from `/specs/026-processing-logic-stubs/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/stub-generator-contract.md

**Tests**: No dedicated test-first tasks. Existing test files (ProcessingLogicStubGeneratorTest.kt, FlowGraphFactoryGeneratorTest.kt, FlowKtGeneratorTest.kt) are updated inline with each user story's implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Stub generator**: `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt`
- **Factory generator**: `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt`
- **DSL generator**: `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt`
- **CodeNode model**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt`
- **CodeNode factory**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt`
- **Properties panel**: `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt`
- **Stub generator tests**: `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGeneratorTest.kt`
- **Factory generator tests**: `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt`
- **DSL generator tests**: `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowKtGeneratorTest.kt`

---

## Phase 1: Setup

**Purpose**: No project initialization needed — all infrastructure exists. This is a modification feature affecting existing files across the `kotlinCompiler`, `fbpDsl`, and `graphEditor` modules.

No setup tasks required.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No blocking prerequisites needed. The tick type aliases (Feature 025) and timed factory methods (Feature 025) are already in place. All changes are to existing generator and model files.

No foundational tasks required.

---

## Phase 3: User Story 1 - Generate Tick Function Stubs (Priority: P1) MVP

**Goal**: Rewrite the ProcessingLogicStubGenerator to generate typed tick function stubs (top-level `val` with the correct tick type alias) instead of ProcessingLogic class stubs.

**Independent Test**: After implementing, `./gradlew :kotlinCompiler:jvmTest` passes and generated stubs contain the correct tick type alias signatures for all node configurations (generators, sinks, processors).

### Implementation for User Story 1

- [X] T001 [US1] Add `getTickTypeAlias(codeNode)` method to `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt` — Implement the mapping from (inputCount, outputCount) to the correct tick type alias name. Use the node's input/output ports to determine count, and port dataType to resolve type parameters. Return the fully parameterized type alias string (e.g., `"Out2TickBlock<Int, Int>"`). Handle the transformer vs filter distinction: when inputs=1 and outputs=1, if input type equals output type use `FilterTickBlock<T>`, otherwise use `TransformerTickBlock<TIn, TOut>`. See `contracts/stub-generator-contract.md` for the complete mapping table.

- [X] T002 [US1] Add `getTickValName(codeNode)` and `shouldGenerateStub(codeNode)` methods to `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt` — `getTickValName` returns `{nodeName}Tick` (camelCase of node name + "Tick"). `shouldGenerateStub` returns false if node has 0 inputs AND 0 outputs, or if input count > 3 or output count > 3, otherwise returns true. Update the existing `getStubFileName` to return `{NodeName}ProcessLogic.kt` (PascalCase + "ProcessLogic") instead of the current `{NodeName}Component.kt`.

- [X] T003 [US1] Rewrite `generateStub(codeNode, packageName)` method in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt` — Replace the current ProcessingLogic class stub generation with tick val stub generation. The generated file should contain: (1) package declaration with `.logicmethods` suffix, (2) imports for the tick type alias and ProcessResult2/3 if needed, (3) KDoc with node type description and port listings, (4) a top-level `val {nodeName}Tick: {TickTypeAlias}<{TypeParams}> = { ... }` with a TODO body and compilable default return value. Use `getTickTypeAlias`, `getTickValName`, and `shouldGenerateStub` from T001-T002. Return empty string if `shouldGenerateStub` is false. See `quickstart.md` for example generated output and `contracts/stub-generator-contract.md` for the default return value table.

- [X] T004 [US1] Add helper method for default return values in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGenerator.kt` — Add a private method `getDefaultReturnValue(codeNode)` that returns the compilable default for the stub body. For sinks: empty (no return). For filters: `true`. For single-output nodes: `0`, `0L`, `0.0`, `""`, `false`, or `TODO("Provide default value")` based on port type. For multi-output: `ProcessResult2.both(d1, d2)` or `ProcessResult3(d1, d2, d3)` with per-port defaults. See `contracts/stub-generator-contract.md` Default Values by Type table.

- [X] T005 [US1] Rewrite tests in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/ProcessingLogicStubGeneratorTest.kt` — Replace the existing ProcessingLogic-based tests with tests for tick stub generation. Test cases: (1) generator node (0 in, 1 out) → `GeneratorTickBlock<T>`, (2) generator node (0 in, 2 out) → `Out2TickBlock<U, V>`, (3) sink node (1 in, 0 out) → `SinkTickBlock<T>`, (4) sink node (2 in, 0 out) → `In2SinkTickBlock<A, B>`, (5) transformer node (1 in, 1 out, different types) → `TransformerTickBlock<TIn, TOut>`, (6) filter node (1 in, 1 out, same type) → `FilterTickBlock<T>`, (7) processor node (2 in, 2 out) → `In2Out2TickBlock<A, B, U, V>`, (8) file naming: `{NodeName}ProcessLogic.kt`, (9) val naming: `{nodeName}Tick`, (10) edge case: 0 in + 0 out → empty string, (11) edge case: >3 inputs → should not generate. Verify generated code includes correct package, imports, KDoc, and default return values.

- [X] T006 [US1] Build verification for US1 — Run `./gradlew :kotlinCompiler:compileKotlinJvm` and `./gradlew :kotlinCompiler:jvmTest`. Verify compilation succeeds and all stub generator tests pass.

**Checkpoint**: ProcessingLogicStubGenerator generates typed tick function stubs for all node configurations. Tests pass.

---

## Phase 4: User Story 2 - Update Code Generator to Reference Tick Stubs (Priority: P2)

**Goal**: Update FlowGraphFactoryGenerator to create NodeRuntime instances via timed factory methods using tick stubs, and remove processingLogic<T>() generation from FlowKtGenerator.

**Independent Test**: After implementing, `./gradlew :kotlinCompiler:jvmTest` passes and generated factory code references tick stub functions and creates correct NodeRuntime types.

### Implementation for User Story 2

- [X] T007 [US2] Add tick stub reference helpers to `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt` — Replace `getComponentClassName`, `getComponentInstantiation`, and `getExternalImports` with tick-stub-aware methods. Add `getTickValName(node)` (returns `{nodeName}Tick`), `getTickImport(node, packageName)` (returns `import {packageName}.logicmethods.{nodeName}Tick`), and `getFactoryMethodName(node)` (returns the correct `createTimed{X}` factory method name based on input/output count, using the same mapping as the stub generator from T001). The factory method selection should match `contracts/stub-generator-contract.md` Factory Method Selection table.

- [X] T008 [US2] Rewrite `generateFactory` in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGenerator.kt` — Replace the ProcessingLogic instantiation and CodeNode creation pattern with the tick stub reference pattern. The generated code should: (1) import tick functions from `{packageName}.logicmethods`, (2) for each node, call the correct `CodeNodeFactory.createTimed{X}<TypeParams>(name, tickIntervalMs, tick = {nodeName}Tick)` instead of creating a CodeNode with `processingLogic = ...`. The `tickIntervalMs` value should come from a node configuration key (e.g., `"tickIntervalMs"` or fallback to a default). Remove all references to `_useCaseClass`, `ProcessingLogic`, and `getComponentClassName`/`getComponentInstantiation`/`getExternalImports`. See `quickstart.md` After section for the target generated code pattern.

- [X] T009 [US2] Remove `processingLogic<T>()` generation from `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/FlowKtGenerator.kt` — Remove lines 156-161 that read `_useCaseClass` from node configuration and generate `processingLogic<$className>()` DSL calls. The FlowKtGenerator should no longer emit any processing logic references — tick stubs are a code-generation-only concern, not serialized in the DSL.

- [X] T010 [US2] Update tests in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowGraphFactoryGeneratorTest.kt` — Replace ProcessingLogic-based test assertions with tick stub pattern assertions. Verify generated factory code: (1) imports tick functions from `logicmethods` package, (2) calls the correct `createTimed{X}` factory method for each node type, (3) passes tick function reference as `tick` parameter, (4) no longer instantiates ProcessingLogic components or assigns `processingLogic` to CodeNode. Update test fixtures to remove `_useCaseClass` config.

- [X] T011 [US2] Update tests in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/FlowKtGeneratorTest.kt` — Remove the T015 test suite (lines ~391-483) that tested `processingLogic<T>()` generation with `_useCaseClass`. Update remaining tests to verify no processingLogic references appear in generated .flow.kt output.

- [X] T012 [US2] Build verification for US2 — Run `./gradlew :kotlinCompiler:compileKotlinJvm` and `./gradlew :kotlinCompiler:jvmTest`. Verify compilation succeeds and all generator tests pass.

**Checkpoint**: Code generators produce tick-stub-based factory code. No ProcessingLogic references in generated output. Tests pass.

---

## Phase 5: User Story 3 - Remove ProcessingLogic Interface and References (Priority: P3)

**Goal**: Remove the ProcessingLogic fun interface, processingLogic property, all helper methods, and all remaining references from the codebase.

**Independent Test**: After removing, `./gradlew :fbpDsl:compileKotlinJvm :kotlinCompiler:compileKotlinJvm :graphEditor:compileKotlinJvm :StopWatch:compileKotlinJvm` succeeds and `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest` passes. Grep for `ProcessingLogic` returns zero source file matches.

### Implementation for User Story 3

- [X] T013 [US3] Remove `ProcessingLogic` fun interface from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt` — Delete the `ProcessingLogic` fun interface definition (lines 43-51) and its KDoc. This interface contains the `suspend operator fun invoke(inputs: Map<String, InformationPacket<*>>): Map<String, InformationPacket<*>>` method.

- [X] T014 [US3] Remove `processingLogic` property and helper methods from `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNode.kt` — Remove the `@Transient val processingLogic: ProcessingLogic? = null` property (line 159) from the CodeNode data class. Remove the helper methods: `hasProcessingLogic()` (line 321), `withProcessingLogic(logic: ProcessingLogic)` (lines 329-331), and `process(inputs)` (lines 340-345). Remove any imports related to InformationPacket if no longer used by remaining code.

- [X] T015 [US3] Remove `processingLogic` parameter from `create()` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/CodeNodeFactory.kt` — Remove the `processingLogic: ProcessingLogic? = null` parameter from the `create()` factory method and the corresponding `processingLogic = processingLogic` assignment in the CodeNode constructor call.

- [X] T016 [US3] Remove `_useCaseClass` property editor from `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt` — Remove the _useCaseClass property editor in the Required Properties section (lines ~714-726) that shows "Use Case Class" with description "Fully qualified class implementing ProcessingLogic". This field is obsolete since _useCaseClass configuration is no longer used.

- [X] T017 [US3] Scan for and remove any remaining `ProcessingLogic` references across the codebase — Search for `ProcessingLogic`, `processingLogic`, `_useCaseClass`, `withProcessingLogic`, `hasProcessingLogic` across all source files (excluding specs/ and docs/). Remove any remaining references found in imports, configuration handling, test fixtures, or generated code templates. Check modules: fbpDsl, kotlinCompiler, graphEditor, StopWatch.

- [X] T018 [US3] Build verification for US3 — Run `./gradlew :fbpDsl:compileKotlinJvm :kotlinCompiler:compileKotlinJvm :graphEditor:compileKotlinJvm :StopWatch:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest`. Verify all modules compile and all tests pass. Grep for `ProcessingLogic` in source files to confirm zero matches remain.

**Checkpoint**: ProcessingLogic completely removed from the codebase. All modules compile and tests pass.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all user stories.

- [X] T019 Full cross-module build verification — Run `./gradlew :fbpDsl:compileKotlinJvm :graphEditor:compileKotlinJvm :kotlinCompiler:compileKotlinJvm :StopWatch:compileKotlinJvm` and `./gradlew :fbpDsl:jvmTest :kotlinCompiler:jvmTest`. Verify all modules compile and all tests pass.

- [X] T020 Verify completeness — Confirm: (1) ProcessingLogicStubGenerator generates tick stubs with correct type aliases for all 16 node configurations, (2) FlowGraphFactoryGenerator produces tick-stub-based factory code, (3) FlowKtGenerator does not emit processingLogic references, (4) Zero `ProcessingLogic` references remain in source files, (5) Zero `_useCaseClass` references remain in source files (excluding specs/).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No tasks needed
- **Foundational (Phase 2)**: No tasks needed — tick type aliases and factory methods from Feature 025 are the prerequisite (already complete)
- **US1 (Phase 3)**: Can start immediately — modifies ProcessingLogicStubGenerator and its tests
- **US2 (Phase 4)**: Depends on US1 (needs `getTickTypeAlias` mapping logic; also depends on understanding the new stub pattern)
- **US3 (Phase 5)**: Depends on US2 (generators must stop producing ProcessingLogic references before the interface can be removed)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: No dependencies — modifies ProcessingLogicStubGenerator only
- **User Story 2 (P2)**: Depends on US1 (tick type alias mapping logic shared between stub generator and factory generator)
- **User Story 3 (P3)**: Depends on US2 (cannot remove ProcessingLogic until generators stop referencing it)

### Within Each User Story

- US1: T001 → T002 → T003 → T004 → T005 → T006 (all modify same file sequentially)
- US2: T007 → T008 (same file), T009 (different file, can parallel with T007-T008 after US1), T010-T011 (test files, after T007-T009), T012
- US3: T013 → T014 (same file), T015 (different file, [P] with T013-T014), T016 ([P] different file), T017 (after all removals), T018

### Parallel Opportunities

```bash
# Within US2: FlowKtGenerator change is independent of FlowGraphFactoryGenerator
T009: Remove processingLogic<T>() from FlowKtGenerator  # [P] after US1 complete

# Within US3: Removals in different files/modules
T013-T014: CodeNode.kt (fbpDsl)
T015: CodeNodeFactory.kt (fbpDsl)           # [P] with T013-T014
T016: PropertiesPanel.kt (graphEditor)      # [P] with T013-T015

# Cross-story: US1 and parts of US2 test updates are in different test files
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 3: User Story 1 (T001-T006)
2. **STOP and VALIDATE**: Tick stubs generate correctly for all node types, tests pass
3. This alone delivers value — developers can see the new stub pattern

### Incremental Delivery

1. US1 (T001-T006) → Tick stub generation works → MVP
2. US2 (T007-T012) → Factory code references tick stubs → Full code generation pipeline
3. US3 (T013-T018) → ProcessingLogic removed → Clean codebase
4. Polish (T019-T020) → Final verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Total: 20 tasks across ~10 files (all modified, no new source files)
- US1 and US2 changes are in the kotlinCompiler module only
- US3 changes span fbpDsl, kotlinCompiler, and graphEditor modules
- The StopWatch module is NOT modified — its components already use the tick block pattern
- All user stories must be executed sequentially (US1 → US2 → US3) due to dependencies
