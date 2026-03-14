# Tasks: Self-Contained CodeNode Definition

**Input**: Design documents from `/specs/050-self-contained-codenode/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Not explicitly requested — test tasks omitted. US4 (Standalone Node Testing) validates testability as a capability.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **KMP multi-module**: `fbpDsl/src/commonMain/`, `graphEditor/src/jvmMain/`, `nodes/src/commonMain/`, `EdgeArtFilter/src/commonMain/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the new `nodes` module and foundational types needed by all stories

- [x] T001 Define `NodeCategory` enum (SOURCE, TRANSFORMER, PROCESSOR, SINK) and `PortSpec` data class in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/CodeNodeDefinition.kt`
- [x] T002 Define `CodeNodeDefinition` interface with properties (name, category, description, inputPorts, outputPorts) and methods (createRuntime, toNodeTypeDefinition) in `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/runtime/CodeNodeDefinition.kt`
- [x] T003 Create the `nodes` Gradle module with KMP configuration and `fbpDsl` dependency in `nodes/build.gradle.kts` and register it in `settings.gradle.kts`

**Checkpoint**: Foundation types compile, `nodes` module builds successfully

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the NodeDefinitionRegistry that all user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Implement `NodeDefinitionRegistry` with `discoverAll()`, `getByName()`, `getAllForPalette()`, and `isCompiled()` methods in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/state/NodeDefinitionRegistry.kt`
- [x] T005 Implement classpath scanning in `NodeDefinitionRegistry.discoverAll()` to discover compiled `CodeNodeDefinition` implementations from Module and Project levels
- [x] T006 Implement filesystem scanning in `NodeDefinitionRegistry.discoverAll()` to parse `~/.codenode/nodes/*.kt` for `NodeTemplateMeta` (Universal level, metadata only)
- [x] T007 Implement legacy loading in `NodeDefinitionRegistry.discoverAll()` to load existing `CustomNodeDefinition` entries from `CustomNodeRepository` for backward compatibility
- [x] T008 Implement `getAllForPalette()` to merge compiled nodes, template nodes, and legacy nodes into a unified `List<NodeTypeDefinition>` with correct ordering (compiled first, then templates, then legacy)

**Checkpoint**: Registry discovers nodes from all three sources and provides palette-ready entries

---

## Phase 3: User Story 1 - Generate a Self-Contained Node (Priority: P1) 🎯 MVP

**Goal**: The Node Generator produces a single-file self-contained node definition with processing logic placeholder

**Independent Test**: Invoke Node Generator for "BlurFilter" (1 in, 1 out, Project level), verify file exists at `nodes/src/commonMain/kotlin/io/codenode/nodes/BlurFilterCodeNode.kt`, confirm it contains port definition and processing logic placeholder, verify it compiles

### Implementation for User Story 1

- [x] T009 [US1] Add `NodeCategory` selector (Source, Transformer, Processor, Sink) to the Node Generator UI in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/NodeGeneratorPanel.kt`
- [x] T010 [US1] Add placement level selector (Module, Project, Universal) with Project as default to the Node Generator UI in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/ui/NodeGeneratorPanel.kt`
- [x] T011 [US1] Add `category` and `placementLevel` state properties to `NodeGeneratorViewModel` in `graphEditor/src/jvmMain/kotlin/io/codenode/grapheditor/viewmodel/NodeGeneratorViewModel.kt`
- [x] T012 [US1] Implement file generation logic in `NodeGeneratorViewModel` that produces a `{NodeName}CodeNode.kt` Kotlin object implementing `CodeNodeDefinition` with correct port configuration and pass-through processing logic placeholder
- [x] T013 [US1] Implement file path resolution in `NodeGeneratorViewModel` for each placement level: Module → `{Module}/src/commonMain/kotlin/io/codenode/{module}/nodes/`, Project → `nodes/src/commonMain/kotlin/io/codenode/nodes/`, Universal → `~/.codenode/nodes/`
- [x] T014 [US1] Implement name conflict detection in `NodeGeneratorViewModel` that checks all three levels via `NodeDefinitionRegistry` before generating, and shows an error if a node with the same name already exists

**Checkpoint**: Node Generator creates compilable self-contained node files at the correct location for any category and level

---

## Phase 4: User Story 2 - Use Generated Node from the Palette (Priority: P2)

**Goal**: Self-contained nodes appear in the palette, can be dragged onto the canvas, and execute in runtime preview

**Independent Test**: Generate a node, add processing logic, restart graph editor, verify node appears in palette, drag onto canvas, wire into pipeline, run runtime preview and confirm processing logic executes

### Implementation for User Story 2

- [ ] T015 [US2] Replace hardcoded node registration in `graphEditor/src/jvmMain/kotlin/Main.kt` with `NodeDefinitionRegistry.discoverAll()` call on startup to populate the palette
- [ ] T016 [US2] Wire `NodeDefinitionRegistry.getAllForPalette()` into the `nodeTypes` list used by the Node Palette display in `graphEditor/src/jvmMain/kotlin/Main.kt`
- [ ] T017 [US2] Ensure `CodeNodeDefinition.toNodeTypeDefinition()` produces a `NodeTypeDefinition` with correct name, category (mapped to `NodeTypeDefinition.NodeCategory`), port templates, and configuration so palette display and canvas instantiation work correctly

**Checkpoint**: Self-contained nodes appear in palette with correct category colors, can be placed on canvas with correct ports

---

## Phase 5: User Story 3 - Swap Compatible Nodes at Runtime (Priority: P3)

**Goal**: Swapping a node on the canvas and re-running the pipeline uses the new node's processing logic without code changes

**Independent Test**: Build a pipeline with GrayscaleTransformer, run it, swap with SepiaTransformer (same port signature), re-run, verify output reflects sepia processing

### Implementation for User Story 3

- [ ] T018 [US3] Modify runtime flow execution to look up node definitions by name from `NodeDefinitionRegistry.getByName()` instead of hardcoded processing logic references in `graphEditor/src/jvmMain/kotlin/Main.kt` (or the runtime preview wiring logic)
- [ ] T019 [US3] Implement `createRuntime()` dispatch in the runtime resolution path so that each node's processing logic is obtained by calling `CodeNodeDefinition.createRuntime(instanceName)` and wiring the returned `NodeRuntime` into the channel pipeline
- [ ] T020 [US3] Verify port signature compatibility check: when a user swaps a node, the system should warn if the replacement node's port count or types don't match the existing connections

**Checkpoint**: Swapping a transformer node on the canvas and re-running produces the new node's output

---

## Phase 6: User Story 4 - Standalone Node Testing (Priority: P4)

**Goal**: A self-contained node can be unit tested in isolation without flow graph or controller dependencies

**Independent Test**: Write a unit test that instantiates a CodeNodeDefinition, calls `createRuntime()`, feeds test input, and asserts on output — no flow graph or controller needed

### Implementation for User Story 4

- [ ] T021 [US4] Verify that `CodeNodeDefinition.createRuntime()` returns a `NodeRuntime` that can be started, fed input, and produce output in isolation — document the test pattern in a sample test file at `nodes/src/commonTest/kotlin/io/codenode/nodes/CodeNodeStandaloneTestExample.kt`

**Checkpoint**: A standalone unit test demonstrates creating, running, and asserting on a self-contained node without any flow graph infrastructure

---

## Phase 7: User Story 5 - Migrate EdgeArtFilter Nodes (Priority: P5)

**Goal**: Convert all 6 EdgeArtFilter nodes from legacy pattern to self-contained `CodeNodeDefinition` format, removing fragmented files and hardcoded registration

**Independent Test**: Run EdgeArtFilter runtime preview and verify identical visual output (grayscale + edge detection + color overlay) before and after migration. Swap SepiaTransformer for GrayscaleTransformer and verify hot-swap works.

### Implementation for User Story 5

- [ ] T022 [P] [US5] Create `ImagePickerCodeNode` object implementing `CodeNodeDefinition` (Source: 0 in, 2 out, `SourceOut2Block<ImageData, ImageData>`) embedding the `imagePickerGenerate` logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/ImagePickerCodeNode.kt`
- [ ] T023 [P] [US5] Create `GrayscaleTransformerCodeNode` object implementing `CodeNodeDefinition` (Transformer: 1 in, 1 out, `ContinuousTransformBlock<ImageData, ImageData>`) embedding the `grayscaleTransform` logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/GrayscaleTransformerCodeNode.kt`
- [ ] T024 [P] [US5] Create `EdgeDetectorCodeNode` object implementing `CodeNodeDefinition` (Transformer: 1 in, 1 out, `ContinuousTransformBlock<ImageData, ImageData>`) embedding the `edgeDetectorTransform` logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/EdgeDetectorCodeNode.kt`
- [ ] T025 [P] [US5] Create `ColorOverlayCodeNode` object implementing `CodeNodeDefinition` (Processor: 2 in, 1 out, `In2Out1ProcessBlock<ImageData, ImageData, ImageData>`) embedding the `colorOverlayProcess` logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/ColorOverlayCodeNode.kt`
- [ ] T026 [P] [US5] Create `SepiaTransformerCodeNode` object implementing `CodeNodeDefinition` (Transformer: 1 in, 1 out, `ContinuousTransformBlock<ImageData, ImageData>`) embedding the `sepiaTransform` logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/SepiaTransformerCodeNode.kt`
- [ ] T027 [P] [US5] Create `ImageViewerCodeNode` object implementing `CodeNodeDefinition` (Sink: 1 in, 0 out, `ContinuousSinkBlock<ImageData>`) embedding the `imageViewerConsume` logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/nodes/ImageViewerCodeNode.kt`
- [ ] T028 [US5] Remove the hardcoded `edgeArtFilterNodes` list, `edgeArtFilterSourceSinkNames`/`edgeArtFilterProcessorNames` color-coding overrides, and manual `CustomNodeDefinition.create()` calls from `graphEditor/src/jvmMain/kotlin/Main.kt` — these nodes are now discovered via the registry
- [ ] T029 [US5] Remove the 6 legacy processing logic files from `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/processingLogic/` (ImagePickerSourceLogic.kt, GrayscaleTransformerProcessLogic.kt, EdgeDetectorProcessLogic.kt, ColorOverlayProcessLogic.kt, SepiaTransformerProcessLogic.kt, ImageViewerSinkLogic.kt)
- [ ] T030 [US5] Update any remaining references in the EdgeArtFilter runtime wiring (e.g., `EdgeArtFilterFlow.kt` or generated flow files) to use `NodeDefinitionRegistry.getByName()` + `createRuntime()` instead of importing the old processing logic variables directly
- [ ] T031 [US5] Verify EdgeArtFilter runtime preview produces identical visual output and that SepiaTransformer/GrayscaleTransformer hot-swap works on the canvas

**Checkpoint**: EdgeArtFilter runs entirely on self-contained nodes. Legacy processing logic files and hardcoded registration removed. Hot-swap verified.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup and validation across all stories

- [ ] T032 Verify `./gradlew :fbpDsl:jvmTest` passes with no regressions
- [ ] T033 Verify `./gradlew build` succeeds for all modules (fbpDsl, nodes, graphEditor, EdgeArtFilter)
- [ ] T034 Run quickstart.md Step 7 validation: generate a new node, add logic, build, swap on canvas, run preview

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (T001-T003) — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — Node Generator needs registry for conflict detection
- **US2 (Phase 4)**: Depends on Phase 2 + Phase 3 — palette needs registry + generated nodes to display
- **US3 (Phase 5)**: Depends on Phase 4 — runtime resolution needs palette integration working
- **US4 (Phase 6)**: Depends on Phase 1 only — standalone testing needs only the interface
- **US5 (Phase 7)**: Depends on Phases 3, 4, 5 — migration needs full pipeline working
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Depends on Foundational → can start after Phase 2
- **US2 (P2)**: Depends on US1 → palette display needs generated nodes
- **US3 (P3)**: Depends on US2 → hot-swap needs palette + runtime integration
- **US4 (P4)**: Depends on Phase 1 only → can run in parallel with US1-US3
- **US5 (P5)**: Depends on US3 → migration needs the full self-contained pipeline working

### Within Each User Story

- Models/interfaces before services
- Services before UI/endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- T001 and T003 can run in parallel (different files)
- T009 and T010 can run together (same file but additive UI changes)
- T011 and T012 can follow T009/T010 in sequence
- T022-T027 can ALL run in parallel (6 independent node files)
- T028 and T029 can run in parallel after T022-T027 (different files)
- US4 (T021) can run in parallel with US2/US3

---

## Parallel Example: User Story 5

```bash
# Launch all 6 node migration files in parallel (independent files):
T022: "Create ImagePickerCodeNode in EdgeArtFilter/.../nodes/ImagePickerCodeNode.kt"
T023: "Create GrayscaleTransformerCodeNode in EdgeArtFilter/.../nodes/GrayscaleTransformerCodeNode.kt"
T024: "Create EdgeDetectorCodeNode in EdgeArtFilter/.../nodes/EdgeDetectorCodeNode.kt"
T025: "Create ColorOverlayCodeNode in EdgeArtFilter/.../nodes/ColorOverlayCodeNode.kt"
T026: "Create SepiaTransformerCodeNode in EdgeArtFilter/.../nodes/SepiaTransformerCodeNode.kt"
T027: "Create ImageViewerCodeNode in EdgeArtFilter/.../nodes/ImageViewerCodeNode.kt"

# Then sequentially:
T028: "Remove hardcoded registration from Main.kt"
T029: "Remove legacy processing logic files"
T030: "Update runtime wiring references"
T031: "Verify identical output + hot-swap"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational (T004-T008)
3. Complete Phase 3: User Story 1 (T009-T014)
4. **STOP and VALIDATE**: Generate a test node, verify file compiles
5. Demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation types and registry ready
2. Add US1 → Node Generator produces self-contained files (MVP!)
3. Add US2 → Nodes appear in palette and run in preview
4. Add US3 → Hot-swap capability validated
5. Add US4 → Standalone testing pattern documented
6. Add US5 → EdgeArtFilter fully migrated, legacy removed

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- US5 is the integration proof — if EdgeArtFilter works on self-contained nodes, the pattern is validated
