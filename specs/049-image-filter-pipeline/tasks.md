# Tasks: Image Filter Pipeline

**Input**: Design documents from `/specs/049-image-filter-pipeline/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: No test tasks — not explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create EdgeArtFilter KMP module scaffolding and configure Gradle integration

- [x] T001 Create EdgeArtFilter module directory structure: `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/`, `processingLogic/`, `generated/`, `userInterface/`
- [x] T002 Create `EdgeArtFilter/build.gradle.kts` with KMP commonMain config, Compose Desktop dependency, and fbpDsl project dependency (follow StopWatch/UserProfiles pattern)
- [x] T003 Add EdgeArtFilter module to `settings.gradle.kts` include list and graphEditor `build.gradle.kts` dependencies

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data model that ALL nodes depend on — MUST be complete before any user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create ImageData data class in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/ImageData.kt` with fields: `bitmap: ImageBitmap`, `width: Int`, `height: Int`, `metadata: Map<String, String> = emptyMap()`

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Build and run a linear image processing pipeline (Priority: P1) 🎯 MVP

**Goal**: Create 5 custom node types, wire them into a FlowGraph, generate runtime files, implement processing logic, build ViewModel + UI composable, and register the module in graphEditor so a user can construct the pipeline, select an image, and see the neon edge composite result.

**Independent Test**: Construct the 5-node graph in the CodeNodeIO editor, connect them per the pipeline layout, select a test image, start execution, and verify the processed image appears in the viewer.

### FlowGraph Definition

- [x] T005 [US1] Create FlowGraph definition in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/EdgeArtFilter.flow.kt` with 5 nodes (ImagePicker source, GrayscaleTransformer transformer, EdgeDetector transformer, ColorOverlay In2Out1, ImageViewer sink) and connections: ImagePicker.image→GrayscaleTransformer.image, ImagePicker.image→ColorOverlay.original (fan-out), GrayscaleTransformer.result→EdgeDetector.image, EdgeDetector.edges→ColorOverlay.edges (fan-in), ColorOverlay.composite→ImageViewer.image

### Processing Logic (all parallel — separate files, no interdependencies)

- [x] T006 [P] [US1] Implement ImagePicker source logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/processingLogic/ImagePickerSourceLogic.kt` — open file chooser dialog, load selected image into ImageBitmap, wrap in ImageData with `source` metadata key, emit on output port; no emission if user cancels
- [x] T007 [P] [US1] Implement GrayscaleTransformer processing logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/processingLogic/GrayscaleTransformerProcessLogic.kt` — per-pixel luminosity formula `gray = 0.299*R + 0.587*G + 0.114*B`, set R=G=B=gray, preserve alpha, add `grayscale_ms` to metadata
- [x] T008 [P] [US1] Implement EdgeDetector processing logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/processingLogic/EdgeDetectorProcessLogic.kt` — Sobel 3x3 convolution with Gx `[[-1,0,1],[-2,0,2],[-1,0,1]]` and Gy `[[-1,-2,-1],[0,0,0],[1,2,1]]` kernels, magnitude = `sqrt(Gx^2+Gy^2)` clamped 0-255, configurable simulated delay (default 500ms), add `edgedetect_ms` to metadata
- [x] T009 [P] [US1] Implement ColorOverlay processing logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/processingLogic/ColorOverlayProcessLogic.kt` — In2Out1ProcessBlock receiving original image + edge map, for each pixel: if edge brightness > threshold use neon cyan/green color, else use original pixel (optionally darkened), add `overlay_ms` to metadata
- [x] T010 [P] [US1] Implement ImageViewer sink logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/processingLogic/ImageViewerSinkLogic.kt` — update ViewModel state with received ImageData for UI rendering

### Generated Runtime Files

- [x] T011 [US1] Create runtime wiring in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/generated/EdgeArtFilterFlow.kt` — instantiate all 5 node runtimes (SourceRuntime for ImagePicker, TransformerRuntime for Grayscale/EdgeDetector, In2Out1Runtime for ColorOverlay, SinkRuntime for ImageViewer), wire channels per FlowGraph connections including fan-out from ImagePicker
- [x] T012 [P] [US1] Create controller in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/generated/EdgeArtFilterController.kt` — lifecycle management (start/stop/pause/resume all node runtimes), follows StopWatch controller pattern
- [x] T013 [P] [US1] Create controller interface in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/generated/EdgeArtFilterControllerInterface.kt` — defines start/stop/pause/resume contract
- [x] T014 [P] [US1] Create controller adapter in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/generated/EdgeArtFilterControllerAdapter.kt` — bridges controller interface to runtime, follows existing adapter pattern

### ViewModel & UI

- [x] T015 [US1] Create ViewModel in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/EdgeArtFilterViewModel.kt` — exposes `StateFlow<ImageData?>` for processed image state, provides start/stop/selectImage actions, uses lifecycle-viewmodel-compose
- [x] T016 [US1] Create UI composable in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/userInterface/EdgeArtFilter.kt` — renders ImageBitmap via Compose Image composable, file picker button to trigger pipeline, displays basic image output

### GraphEditor Integration

- [x] T017 [US1] Create 5 custom node definitions for the graph editor (ImagePicker, GrayscaleTransformer, EdgeDetector, ColorOverlay, ImageViewer) and register them via FileCustomNodeRepository — set appropriate port names/types per data-model.md, use ImageData as port type
- [x] T018 [US1] Create PreviewProvider in `graphEditor/src/jvmMain/kotlin/ui/EdgeArtFilterPreviewProvider.kt` — register EdgeArtFilter composable with PreviewRegistry for Runtime Preview panel
- [x] T019 [US1] Add ModuleSessionFactory entry in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` — add "EdgeArtFilter" when-branch creating RuntimeSession with Controller, Adapter, ViewModel
- [x] T020 [US1] Wire Main.kt integration in `graphEditor/src/jvmMain/kotlin/Main.kt` — add EdgeArtFilter Koin module, PreviewProvider.register() call, module import

**Checkpoint**: At this point, User Story 1 should be fully functional — user can construct the 5-node pipeline, select an image, and see the neon edge composite in the ImageViewer

---

## Phase 4: User Story 2 — View processing metadata alongside results (Priority: P2)

**Goal**: Ensure each processing node adds timing metadata to ImageData, and the ImageViewer UI displays per-node processing times alongside the rendered image.

**Independent Test**: Run the pipeline with a test image and verify that processing time metadata for each node is displayed alongside the output image.

- [x] T021 [US2] Verify and enhance timing metadata in all processing logic files (`GrayscaleTransformerProcessLogic.kt`, `EdgeDetectorProcessLogic.kt`, `ColorOverlayProcessLogic.kt`) — ensure each node wraps its processing in `System.currentTimeMillis()` before/after and adds timing key to metadata map, add `total_ms` computation in ImageViewerSinkLogic
- [x] T022 [US2] Add metadata display panel to ImageViewer UI in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/userInterface/EdgeArtFilter.kt` — show per-node timing breakdown (grayscale_ms, edgedetect_ms, overlay_ms, total_ms) as text overlay or panel below the rendered image

**Checkpoint**: Users can now see processing metadata alongside results, identifying bottleneck nodes

---

## Phase 5: User Story 3 — Demonstrate branching and merging data flows (Priority: P3)

**Goal**: Ensure the fan-out from ImagePicker to two destinations (GrayscaleTransformer + ColorOverlay) and the fan-in merge at ColorOverlay are correct, visible in the graph, and functional at runtime.

**Independent Test**: Observe the graph layout showing fan-out from ImagePicker, and verify ColorOverlay receives inputs from both paths before producing output.

- [x] T023 [US3] Verify fan-out wiring in `EdgeArtFilterFlow.kt` — confirm ImagePicker output channel is wired to both GrayscaleTransformer.inputChannel and ColorOverlay.inputChannel1, and that ColorOverlay.inputChannel2 receives from EdgeDetector.outputChannel; ensure ColorOverlay's In2Out1Runtime `select {}` blocks until both inputs arrive
- [x] T024 [US3] Verify graph layout visually distinguishes the branching/merging connections — ensure custom node definitions for ImagePicker include 1 output port with connections to 2 targets, and ColorOverlay shows 2 input ports ("original" and "edges")

**Checkpoint**: Fan-out and fan-in are visually clear and functionally correct

---

## Phase 6: User Story 4 — Hot-swap processing nodes (Priority: P4)

**Goal**: Provide SepiaTransformer as a drop-in alternative to GrayscaleTransformer, allowing users to swap nodes in the graph editor and re-run the pipeline with a different visual effect.

**Independent Test**: Replace GrayscaleTransformer with SepiaTransformer in the graph, re-run, and verify sepia-toned output instead of grayscale edges.

- [x] T025 [P] [US4] Implement SepiaTransformer processing logic in `EdgeArtFilter/src/commonMain/kotlin/io/codenode/edgeartfilter/processingLogic/SepiaTransformerProcessLogic.kt` — per-pixel sepia matrix: newR=min(255, 0.393*R+0.769*G+0.189*B), newG=min(255, 0.349*R+0.686*G+0.168*B), newB=min(255, 0.272*R+0.534*G+0.131*B), add `sepia_ms` to metadata
- [x] T026 [US4] Create SepiaTransformer custom node definition and register via FileCustomNodeRepository — same port configuration as GrayscaleTransformer (input: "image" ImageData, output: "result" ImageData) so it can be swapped in-place

**Checkpoint**: Users can swap GrayscaleTransformer with SepiaTransformer and get a different visual effect without code changes

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Color coding, edge cases, and final validation

- [x] T027 [P] Apply visual color coding to custom node definitions per FR-012 — source/sink nodes (ImagePicker, ImageViewer) get one color scheme, processing nodes (Grayscale, EdgeDetector, ColorOverlay, Sepia) get a different color scheme in the graph editor
- [x] T028 Run quickstart.md validation — construct the 5-node pipeline in the graph editor, connect all nodes, select an image, verify neon edge composite appears, swap GrayscaleTransformer with SepiaTransformer, re-run, verify different effect

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - US1 (Phase 3): BLOCKS US2, US3, US4 (all build on the core pipeline)
  - US2 (Phase 4): Depends on US1 (processing logic must exist before enhancing metadata)
  - US3 (Phase 5): Depends on US1 (flow graph and runtime must exist before verifying fan-out/fan-in)
  - US4 (Phase 6): Depends on US1 (graph editor integration must exist before adding alternative node)
- **Polish (Phase 7)**: Depends on all user stories being complete

### Within Each User Story

- FlowGraph definition before runtime files
- Processing logic can be parallel (separate files)
- Runtime files before ViewModel
- ViewModel before UI composable
- UI composable before graphEditor integration
- GraphEditor integration tasks are sequential (ModuleSessionFactory → Main.kt)

### Parallel Opportunities

- T006–T010: All processing logic files can be written in parallel
- T012–T014: Controller, interface, and adapter can be written in parallel
- T025 (SepiaTransformer) can run in parallel with US2/US3 tasks
- T027 (color coding) can run in parallel with T028 (validation)

---

## Parallel Example: User Story 1

```bash
# Launch all processing logic tasks together:
Task T006: "ImagePicker source logic in processingLogic/ImagePickerSourceLogic.kt"
Task T007: "GrayscaleTransformer logic in processingLogic/GrayscaleTransformerProcessLogic.kt"
Task T008: "EdgeDetector logic in processingLogic/EdgeDetectorProcessLogic.kt"
Task T009: "ColorOverlay logic in processingLogic/ColorOverlayProcessLogic.kt"
Task T010: "ImageViewer sink logic in processingLogic/ImageViewerSinkLogic.kt"

# Launch all generated runtime boilerplate together:
Task T012: "Controller in generated/EdgeArtFilterController.kt"
Task T013: "Controller interface in generated/EdgeArtFilterControllerInterface.kt"
Task T014: "Controller adapter in generated/EdgeArtFilterControllerAdapter.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (module scaffolding + Gradle)
2. Complete Phase 2: Foundational (ImageData model)
3. Complete Phase 3: User Story 1 (all 5 nodes, runtime, ViewModel, UI, graphEditor)
4. **STOP and VALIDATE**: Test pipeline end-to-end with a real image
5. Demo the neon edge effect

### Incremental Delivery

1. Complete Setup + Foundational → Module ready
2. Add User Story 1 → Test pipeline → Demo (MVP!)
3. Add User Story 2 → Metadata display → Demo
4. Add User Story 3 → Verify branching/merging → Demo
5. Add User Story 4 → SepiaTransformer swap → Demo
6. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US2 and US3 are largely verification/enhancement of what US1 builds — they are intentionally lightweight
- US4 (SepiaTransformer) is the only user story that adds a wholly new component
- On JVM, use `toAwtImage()` / `BufferedImage` for efficient pixel array access (per research.md)
- The simulated 500ms delay in EdgeDetector is for demo purposes — configurable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
