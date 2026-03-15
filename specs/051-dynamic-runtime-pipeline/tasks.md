# Tasks: Dynamic Runtime Pipeline

**Input**: Design documents from `/specs/051-dynamic-runtime-pipeline/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested in the spec. Tests omitted.

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup

**Purpose**: Ensure fbpDsl module has the interfaces needed for the dynamic pipeline builder to resolve node definitions without depending on graphEditor.

- [X] T001 Create `NodeDefinitionLookup` functional interface (a `(String) -> CodeNodeDefinition?` typealias or interface) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/NodeDefinitionLookup.kt` so DynamicPipelineBuilder can resolve node names without importing graphEditor's `NodeDefinitionRegistry`
- [X] T002 Create `PipelineValidation.kt` with `PipelineValidationResult` and `ValidationError` data classes per data-model.md in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/PipelineValidation.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core pipeline builder and controller classes — MUST be complete before user story integration

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Implement `DynamicPipelineBuilder.validate()` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineBuilder.kt` — iterate `FlowGraph.getAllCodeNodes()`, resolve each node name via `NodeDefinitionLookup`, check connections reference valid ports, detect cycles; return `PipelineValidationResult` per contracts/dynamic-pipeline-builder.md
- [X] T004 Implement `DynamicPipelineBuilder.build()` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineBuilder.kt` — call `createRuntime()` on each resolved `CodeNodeDefinition`, create `Channel<Any>(Channel.BUFFERED)` per connection, wire channels to runtime input/output properties using port index mapping from data-model.md (handle `TransformerRuntime`, `SinkRuntime`, `SourceOut2Runtime`, `In2Out1Runtime`, etc. naming inconsistencies), return `DynamicPipeline`
- [X] T005 Implement `DynamicPipelineBuilder.canBuildDynamic()` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineBuilder.kt` — return true only if every `CodeNode` name in the FlowGraph has a `CodeNodeDefinition` in the lookup
- [X] T006 Implement `DynamicPipeline` data class in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineBuilder.kt` — holds `flowGraph`, `runtimes: Map<String, NodeRuntime>`, `channels: List<Channel<Any>>`, `scope: CoroutineScope`; provides `start(scope)`, `stop()`, `pause()`, `resume()` operations per data-model.md
- [X] T007 Implement `DynamicPipelineController` in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt` — implement `ModuleController` interface; delegates to `DynamicPipelineBuilder.validate()` + `build()` on `start()`, manages `RuntimeRegistry` for pause/resume propagation, supports `setAttenuationDelay()`, `setEmissionObserver()`, `setValueObserver()` per contracts/dynamic-pipeline-builder.md

**Checkpoint**: Foundation ready — pipeline builder can construct and control dynamic pipelines from any FlowGraph

---

## Phase 3: User Story 1 — Run Pipeline from Canvas (Priority: P1) 🎯 MVP

**Goal**: Load a module with fully-registered CodeNodeDefinitions (EdgeArtFilter), press Start, and the system builds and runs the pipeline dynamically from the canvas FlowGraph — no hardcoded Flow class needed.

**Independent Test**: Load EdgeArtFilter → Start → Pick Image → verify processed image appears, all via DynamicPipelineController instead of EdgeArtFilterController.

### Implementation for User Story 1

- [X] T008 [US1] Modify `ModuleSessionFactory.kt` to check `DynamicPipelineBuilder.canBuildDynamic()` before creating a session in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` — if all canvas nodes are resolvable, create a `DynamicPipelineController` + existing ViewModel; otherwise fall back to existing per-module factory method (per research.md R4 fallback detection)
- [X] T009 [US1] Wire `NodeDefinitionRegistry` as the `NodeDefinitionLookup` for `DynamicPipelineBuilder` in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` — adapt `registry.getByName(name)` to satisfy the lookup interface
- [X] T010 [US1] Pass the canvas `FlowGraph` to `DynamicPipelineController` on construction in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` — the controller reads node/connection topology from this FlowGraph on each `start()` call
- [X] T011 [US1] Surface validation errors in `DynamicPipelineController.start()` — when `validate()` returns errors, set execution state back to IDLE and expose error messages via a `StateFlow<String?>` or callback that `RuntimePreviewPanel` can display in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt`
- [X] T012 [US1] Display validation error messages in `RuntimePreviewPanel` when `DynamicPipelineController` reports errors in `graphEditor/src/jvmMain/kotlin/ui/RuntimePreviewPanel.kt` — show red text identifying unresolvable nodes or invalid connections

**Checkpoint**: EdgeArtFilter runs dynamically from canvas — identical output to hardcoded pipeline

---

## Phase 4: User Story 2 — Swap a Node and Re-Run (Priority: P2)

**Goal**: Stop a running pipeline, swap a node on the canvas (e.g., replace GrayscaleTransformer with SepiaTransformer), press Start, and the new pipeline uses the replacement node's processing logic.

**Independent Test**: Run EdgeArtFilter → Stop → replace GrayscaleTransformer with SepiaTransformer on canvas → re-wire → Start → Pick Image → verify sepia-toned output.

### Implementation for User Story 2

- [ ] T013 [US2] Ensure `DynamicPipelineController.start()` re-reads the current canvas FlowGraph on every invocation (not cached from construction) — verify `build()` creates a fresh pipeline reflecting any canvas changes since the last run, in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt`
- [ ] T014 [US2] Ensure `DynamicPipelineController.stop()` fully cleans up previous pipeline (close all channels, cancel scope, clear runtime map) so the next `start()` begins from a clean state in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt`
- [ ] T015 [US2] Verify `DynamicPipelineBuilder.build()` handles port-compatible node substitutions — when a TRANSFORMER (1-in, 1-out) is replaced with another TRANSFORMER (1-in, 1-out), wiring succeeds identically in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineBuilder.kt`
- [ ] T016 [US2] Add validation check for port mismatch when a replacement node has different port count than the node it replaces — connections referencing ports that don't exist on the new node produce a clear validation error in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/PipelineValidation.kt`

**Checkpoint**: Node hot-swap works — swapping compatible nodes and re-running produces different output

---

## Phase 5: User Story 3 — Preserve Module-Specific Behavior (Priority: P3)

**Goal**: Modules without full CodeNodeDefinition registry coverage (StopWatch, UserProfiles, GeoLocations, Addresses) continue using their existing generated Controller/Flow pattern.

**Independent Test**: Load StopWatch → Start → verify timer ticks, pause/resume work, stop returns to idle. Load EdgeArtFilter → verify it uses DynamicPipelineController.

### Implementation for User Story 3

- [ ] T017 [US3] Verify fallback path in `ModuleSessionFactory.kt` — when `canBuildDynamic()` returns false (e.g., StopWatch nodes not in registry), the factory creates the module's existing generated controller/session in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt`
- [ ] T018 [US3] Ensure `DynamicPipelineController` lifecycle operations match existing `ModuleController` behavior — verify `executionState` transitions (IDLE→RUNNING→PAUSED→IDLE), `setAttenuationDelay()`, `setEmissionObserver()`, `setValueObserver()` all work identically to generated controllers in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt`
- [ ] T019 [US3] Verify all existing modules load and run correctly with the fallback path — manual verification per quickstart.md Step 4 (StopWatch) and Step 5 (UserProfiles, GeoLocations, Addresses)

**Checkpoint**: All modules work — dynamic pipeline for EdgeArtFilter, fallback for everything else

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T020 Verify `DynamicPipelineController` supports data flow animation — `setEmissionObserver()` and `setValueObserver()` are applied to all dynamic runtimes so Animate Data Flow toggle works in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt`
- [ ] T021 Verify `DynamicPipelineController` supports speed attenuation — `setAttenuationDelay()` propagates to all runtimes in the dynamic pipeline in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/DynamicPipelineController.kt`
- [ ] T022 Run full quickstart.md validation (Steps 1-6) — verify EdgeArtFilter dynamic pipeline, hot-swap, error handling, StopWatch fallback, all existing modules, and lifecycle operations
- [ ] T023 Build verification — run `./gradlew :fbpDsl:jvmTest` and `./gradlew :graphEditor:run` to confirm no regressions

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001, T002) — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational (T003-T007) — core dynamic pipeline integration
- **US2 (Phase 4)**: Depends on US1 (T008-T012) — swap requires dynamic pipeline working first
- **US3 (Phase 5)**: Depends on Foundational (T003-T007) — fallback path is independent of US1/US2
- **Polish (Phase 6)**: Depends on US1 and US3 being complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Foundational — no dependencies on other stories
- **User Story 2 (P2)**: Depends on User Story 1 — swap requires dynamic pipeline to be working
- **User Story 3 (P3)**: Depends on Foundational only — fallback path is independent of dynamic pipeline integration

### Within Each Phase

- T001 and T002 can run in parallel (different files)
- T003, T004, T005, T006 all in DynamicPipelineBuilder.kt — must be sequential
- T007 depends on T006 (needs DynamicPipeline class)
- T008, T009, T010 all modify ModuleSessionFactory.kt — must be sequential
- T013, T014, T015, T016 can be partially parallelized (T013/T14 in Controller, T015/T16 in Builder/Validation)

### Parallel Opportunities

```bash
# Phase 1 — both setup tasks in parallel:
Task T001: NodeDefinitionLookup interface
Task T002: PipelineValidation data classes

# Phase 5 — US3 can run in parallel with US2 (Phase 4):
Task T017-T019: Fallback verification (independent of swap implementation)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T007)
3. Complete Phase 3: User Story 1 (T008-T012)
4. **STOP and VALIDATE**: Load EdgeArtFilter, press Start, pick image → verify processed output
5. If EdgeArtFilter works dynamically, MVP is delivered

### Incremental Delivery

1. Setup + Foundational → Pipeline builder infrastructure ready
2. Add User Story 1 → EdgeArtFilter runs dynamically → Validate (MVP!)
3. Add User Story 2 → Node swap and re-run works → Validate
4. Add User Story 3 → All existing modules still work → Validate
5. Polish → Animation, attenuation, full quickstart verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- The DynamicPipelineBuilder lives in `fbpDsl` (alongside runtime types it depends on)
- `NodeDefinitionRegistry` stays in `graphEditor` — the builder accepts a `NodeDefinitionLookup` abstraction
- Port index mapping (research.md R6) is critical for T004 — handle all runtime type naming inconsistencies
- Generated controllers (StopWatch, UserProfiles, etc.) are NOT modified — fallback path reuses them as-is
