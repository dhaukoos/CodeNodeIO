# Feature Specification: Add Runtime Preview Support to UI-FBP Code Generation

**Feature Branch**: `084-ui-fbp-runtime-preview`
**Created**: 2026-04-26
**Status**: On Hold pending feature 085 (Collapse the entity-module thick runtime onto DynamicPipelineController) — 2026-04-27
**Input**: User description: "Add Runtime Preview for UI-FBP code generation. The UI-FBP code generation path takes a qualifying UI file (Kotlin composable with a viewModel parameter) and generates several files that are needed for a UI-driven FBP virtual circuit. For this feature do a gap analysis of what is needed, beyond the FBP business logic that is intended to be manually added, to fully enable the Runtime Preview functionality to work. (The current TestModule module with the DemoUI.kt file in the DemoProject represents the current output product of the UI-FBP code generation path.)"

> **HOLD NOTICE (2026-04-27)**: This feature is paused pending completion of feature 085 (the universal-runtime collapse). Roughly 40–50% of the implementation surface defined in plan.md / research.md / data-model.md / contracts/ / quickstart.md targets the entity-module thick stack (`Controller.kt`, `ControllerAdapter.kt`, `Flow.kt` runtime, the `UIFBPSpecAdapter`, the four reused entity-module generators, the VS-C deployable-parity scenario). Feature 085 is expected to deprecate that stack across all modules. Resuming work on 084 before 085 ships would discard this surface. After 085 lands, re-run `/speckit.clarify` on this spec to retire the thick-stack requirements (FR-005a, SC-008, much of the deployability framing) and re-run `/speckit.plan` to regenerate plan / research / data-model / contracts / quickstart against the universal runtime. The user-facing spec body (US1–US4, most FRs, most SCs) is expected to need only minor edits.

## Overview

The UI-FBP code generation path turns a qualifying Compose UI file (a `@Composable` function whose first parameter is a `viewModel`) into the source/sink ends of an FBP "virtual circuit" — the user wires the middle of that circuit by editing a generated `.flow.kt` graph and adding business-logic CodeNodes between the source and sink.

Today the generator emits the UI-side wiring (a State holder, a ViewModel, a Source CodeNode, a Sink CodeNode, an empty `.flow.kt` graph) but the resulting module **cannot be loaded into the GraphEditor's Runtime Preview panel**, and it is also **not deployable** to a production Android/iOS/Desktop app that imports the module without the GraphEditor in the loop. The TestModule in the DemoProject sibling repository (with `DemoUI.kt` as its qualifying UI file) is the canonical example of the current output and is missing every artifact that those two scenarios need to discover, instantiate, and animate the module.

This feature closes both gaps so that running UI-FBP code generation produces a module that (a) opens in Runtime Preview without any manual file edits, and (b) is deployable to a production app via the same artifact set used by today's entity modules (Addresses, UserProfiles, EdgeArtFilter). The user only needs to add their business-logic CodeNodes to the `.flow.kt` graph.

## Clarifications

### Session 2026-04-26

- Q: Should UI-FBP code generation produce a thin (preview-only) artifact set or a thick (preview + deployable) artifact set? → A: Thick — always emit both the Runtime-Preview surface AND the deployable-runtime files (Controller.kt, ControllerAdapter.kt, Flow.kt runtime) so that UI-FBP modules can be consumed by a production Android/iOS/Desktop app without the GraphEditor.
- Q: Should feature 084 also collapse the per-module Controller/Adapter/Flow.kt files onto a universal class in fbpDsl (i.e., deprecate the thick stack), or keep that as a separate follow-up? → A: Defer to a separate follow-up. Feature 084 emits today's thick stack matching the entity-module pattern (`RuntimeControllerGenerator` / `RuntimeControllerAdapterGenerator` / `RuntimeFlowGenerator`); a future feature is expected to collapse all modules onto a universal `DynamicPipelineController`-based runtime in fbpDsl, eliminating most per-module boilerplate.

### Session 2026-04-27

- Q: Should feature 084 be paused so the universal-runtime collapse (planned feature 085) can ship first, avoiding ~40–50% throwaway work in 084's thick-stack scope? → A: Yes — pause 084. Develop and ship feature 085 (Collapse the entity-module thick runtime onto DynamicPipelineController) first. After 085 ships, re-run `/speckit.clarify` on 084 to retire the now-obsolete thick-stack requirements (FR-005a, SC-008, the deployability framing), then re-run `/speckit.plan` to regenerate plan / research / data-model / contracts / quickstart against the universal runtime. The user-facing spec body (US1–US4, most FRs, most SCs) is expected to need only minor edits.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - UI-FBP-Generated Module Opens in Runtime Preview (Priority: P1) 🎯 MVP

A workspace user has a Compose UI file in their module. They run UI-FBP code generation against it. They open the module in the GraphEditor and click "Runtime Preview". Their UI appears in the Runtime Preview panel — buttons render, fields are editable — and the runtime engine reports the module as "running" without errors. The UI does not yet *do* anything (because no business logic has been added to the graph), but it is loaded and live.

**Why this priority**: This is the minimum bar for the feature to provide any value at all. Without this, a UI-FBP-generated module is dead-on-arrival as far as Runtime Preview is concerned, and the entire UI-FBP code-generation pathway is incomplete.

**Independent Test**: Run UI-FBP code generation on a known-qualifying Composable file in a module that contains no other generated artifacts. Open that module in the GraphEditor and click "Runtime Preview". Verify the UI appears, that no error dialogs or red-bordered placeholders are shown, and that the runtime status indicator reports the module is running.

**Acceptance Scenarios**:

1. **Given** a module containing a qualifying Compose UI file and no other generated UI-FBP artifacts, **When** the user runs UI-FBP code generation and then opens Runtime Preview for the module, **Then** the Composable renders inside the Runtime Preview panel and the runtime status reports the module is running.
2. **Given** a UI-FBP-generated module loaded in Runtime Preview, **When** the user inspects the runtime state indicator, **Then** the module is shown in a normal "running" state with no missing-class, missing-Preview-provider, or missing-controller errors.
3. **Given** a UI-FBP-generated module loaded in Runtime Preview, **When** the user clicks Stop and then Start again, **Then** the module restarts cleanly without leaking state from the previous run.

---

### User Story 2 - Manually-Added Business Logic Drives the UI End-to-End (Priority: P2)

The user opens the `.flow.kt` graph that UI-FBP generated, drags in or wires up CodeNodes that connect the Source CodeNode's outputs to the Sink CodeNode's inputs (e.g., a passthrough or a simple transform), and saves. They open Runtime Preview, interact with the UI inputs, and observe the UI outputs updating in response — confirming that values flowed from the UI through the FBP graph and back into the UI.

**Why this priority**: This is the core promise of UI-FBP — the UI is one half of a virtual circuit and the FBP graph is the other half. Without end-to-end flow, the generated module is a hollow shell. This depends on US1 (the module must load before flow can be observed).

**Independent Test**: Take a module that already passes US1, edit its `.flow.kt` to wire each Source output directly to its corresponding Sink input via a passthrough, save, open Runtime Preview, change a value in the UI, and observe the corresponding Sink-driven UI element update.

**Acceptance Scenarios**:

1. **Given** a UI-FBP-generated module with a passthrough graph wired in `.flow.kt` (each Source output connected to the matching Sink input), **When** the user changes an input value in Runtime Preview, **Then** the corresponding output element in the Runtime Preview UI updates to reflect the new value within one second.
2. **Given** the same module with a simple transformation wired in the graph (e.g., addition or formatting), **When** the user changes inputs in Runtime Preview, **Then** the output reflects the transformed result, confirming the graph executed end-to-end.
3. **Given** Runtime Preview is running with active data flow, **When** the user clicks Pause, **Then** further UI input changes do not propagate; **When** the user clicks Resume, **Then** subsequent input changes resume propagating.

---

### User Story 3 - Generated Module Compiles Without Manual Fixup (Priority: P3)

The user runs UI-FBP code generation and then runs the project build. The module compiles successfully without the user having to edit `build.gradle.kts`, add Gradle dependencies, fix package paths, or resolve imports. Re-opening the GraphEditor finds the module in the workspace dropdown without any "module unavailable" warnings.

**Why this priority**: Even if Runtime Preview can technically load the module (US1), broken compilation makes the workflow unusable in practice — the user can't iterate. This story locks in the "no manual cleanup" promise of code generation.

**Independent Test**: Run UI-FBP code generation on a fresh module, run the project's full build, and confirm a clean compile with no errors related to the generated files (missing imports, wrong package paths, missing dependencies, duplicate symbols).

**Acceptance Scenarios**:

1. **Given** a module with only a qualifying Compose UI file, **When** the user runs UI-FBP code generation and then runs the project build, **Then** the build completes successfully with no compile errors in the generated files.
2. **Given** a module that previously had legacy UI-FBP output (e.g., the duplicated `saved/` and `viewmodel/` State and ViewModel files in the current TestModule), **When** the user re-runs UI-FBP code generation, **Then** the generated output uses a single canonical package layout and no duplicate or conflicting class declarations remain.
3. **Given** a UI-FBP-generated module, **When** the user opens the GraphEditor, **Then** the module appears in the module dropdown and can be selected without warnings about missing or unloadable artifacts.

---

### User Story 4 - Re-running Generation Preserves Manually-Added Business Logic (Priority: P4)

The user has already added business-logic CodeNodes to their `.flow.kt` graph. Later they edit the source Compose UI file (e.g., add a new input field or change a parameter type) and re-run UI-FBP code generation. The generator updates the affected artifacts — Source/Sink CodeNodes, State and ViewModel — to match the new UI shape, but it does **not** discard the user's business-logic CodeNodes from the graph or overwrite them blindly.

**Why this priority**: Code generators that destroy user work on every run create a serious workflow tax and erode user trust. Once the basic generation works (US1–US3), preserving manual edits across regenerations is essential for sustained use.

**Independent Test**: Generate a module via UI-FBP, manually add CodeNodes and connections to its `.flow.kt`, modify the source Composable's parameter list, re-run UI-FBP, then verify the user-added CodeNodes still exist in the graph (compatible connections preserved, incompatible ones removed with a clear notification).

**Acceptance Scenarios**:

1. **Given** a UI-FBP-generated module whose `.flow.kt` has been hand-edited with business-logic CodeNodes, **When** the user re-runs UI-FBP code generation without changing the UI Composable's signature, **Then** the user's CodeNodes and connections in the graph are preserved.
2. **Given** the same module, **When** the user changes the UI Composable to add a new input parameter and re-runs UI-FBP, **Then** the Source CodeNode gains the new output port, existing user-added CodeNodes and their valid connections remain in place, and the user is informed of any port additions or removals.
3. **Given** a re-generation that removes a port the user's graph was connected to, **When** the user opens the graph after regeneration, **Then** the now-orphaned connection is removed and the user is notified which connections were dropped and why.

---

### Edge Cases

- **Composable signature has no input parameters beyond `viewModel`**: The generator must produce a Source CodeNode with zero outputs (or no Source CodeNode at all) without crashing or producing a Source that Runtime Preview rejects.
- **Composable observes no `StateFlow` properties on the ViewModel**: The generator must produce a Sink CodeNode with zero inputs (or no Sink CodeNode at all) without breaking Runtime Preview's expectations.
- **Source UI file is renamed or moved**: Stale generated artifacts from the prior name must not collide with new artifacts and break compilation.
- **Two qualifying UI files in the same module**: The generator must either support multiple UIs cleanly with non-colliding generated names, or refuse with a clear error before producing partial output. (Default expectation: refuse with a clear error pending future work.)
- **Module already contains hand-written controller, flow-runtime, or preview-provider files for the same UI**: The generator must not silently overwrite hand-written files; it should detect the conflict and either skip with a warning or prompt the user.
- **UI-FBP run inside a module that was scaffolded under a different build configuration**: The generator must not silently mutate `build.gradle.kts` in ways that break the existing module configuration.
- **Runtime Preview opened before any business-logic graph has been added**: The UI must still render and the runtime must report a healthy "running" state — values simply do not flow until business logic is wired (this is US1).

## Requirements *(mandatory)*

### Functional Requirements

#### Discovery and Loading

- **FR-001**: After UI-FBP code generation completes, the GraphEditor's Runtime Preview MUST be able to discover, load, and render the module's Compose UI without requiring the user to edit any generated file by hand.
- **FR-002**: The generated module MUST expose a registration entry point that the GraphEditor uses to associate a UI name with its Composable rendering function, so that Runtime Preview can locate and display the UI.
- **FR-003**: The generated module MUST expose the contract that the runtime session factory uses to instantiate a ViewModel and bind it to the executing FBP pipeline (i.e., the controller-interface contract that the runtime expects to discover by reflection at the canonical class location).
- **FR-004**: The generated module MUST expose a runtime flow representation that the dynamic pipeline builder can execute, so that Runtime Preview's Start/Stop/Pause/Resume controls operate on a real running pipeline.

#### Generated Artifacts (Gap Closure)

- **FR-005**: UI-FBP code generation MUST emit, in addition to today's State, ViewModel, Source CodeNode, and Sink CodeNode files, every additional artifact required for both (a) Runtime Preview to operate end-to-end and (b) the module to be deployable to a production Android/iOS/Desktop app without the GraphEditor in the loop. The set of "every additional artifact" is determined by the discovery and execution contracts referenced in FR-002, FR-003, and FR-004 plus the deployable-runtime contract referenced in FR-005a — concretely, this includes (at minimum) the preview-registration entry point, the runtime controller interface, a runtime controller implementation, a controller adapter, and a runtime flow representation matching the artifact set used by today's entity modules.
- **FR-005a**: The deployable-runtime files MUST follow the same pattern emitted today by the entity-module generator path (`RuntimeControllerGenerator`, `RuntimeControllerAdapterGenerator`, `RuntimeFlowGenerator`) used to produce Addresses, UserProfiles, and EdgeArtFilter, so that UI-FBP modules and entity modules share a single deployable shape. A future feature is expected to collapse this thick stack onto a universal runtime in fbpDsl; that collapse is explicitly out of scope here.
- **FR-006**: Generated artifacts MUST be placed in the package locations and filename patterns that the GraphEditor's discovery mechanisms scan, so that no manual relocation is required for discovery to succeed.
- **FR-007**: Generated artifacts MUST collectively be sufficient that, after generation, the user's only remaining manual step to make a working virtual circuit is editing the `.flow.kt` graph to add business-logic CodeNodes between the Source and Sink.

#### Compilation and Build Wiring

- **FR-008**: The generated module MUST compile cleanly under the project's standard build invocation, with no manual edits to source files, package declarations, or imports.
- **FR-009**: If generation requires module-level build configuration (such as added dependencies in the module's build script) for the generated artifacts to compile, the generator MUST apply those changes automatically and MUST NOT silently disturb unrelated existing configuration.
- **FR-010**: Generation MUST resolve the legacy duplication present in current UI-FBP output (e.g., the parallel `saved/` and `viewmodel/` package locations for State and ViewModel files, as observed in the current TestModule) by emitting to a single canonical location and removing or migrating any stale duplicates it produced previously.

#### Re-generation Behavior

- **FR-011**: Re-running UI-FBP code generation against an unchanged source UI file MUST produce no functional change to the module's behavior in Runtime Preview and MUST NOT alter user-added content of the `.flow.kt` graph.
- **FR-012**: When the source UI file's parameter list or observed `StateFlow` properties change between runs, the generator MUST update the Source CodeNode's outputs and the Sink CodeNode's inputs to match, MUST preserve user-added CodeNodes and valid connections in the `.flow.kt` graph, and MUST remove only those connections that are no longer valid (e.g., reference a port that no longer exists).
- **FR-013**: Generation MUST surface a clear summary to the user of which generated files were created, updated, or left unchanged, and which graph connections (if any) were dropped due to port changes.

#### Workflow Boundary

- **FR-014**: Generation MUST operate against an existing module — it is not responsible for creating a new module from scratch (module scaffolding is handled separately by the module-creation flow). The user's expectation is that they have already created the module, dropped a qualifying UI file into it, and are now running UI-FBP against that file.
- **FR-015**: The generator MUST validate that the input file qualifies as a UI-FBP source (Composable function with a `viewModel` parameter) before emitting any output, and MUST refuse with a clear, actionable error if the file does not qualify.

#### Behavior Under Conflict

- **FR-016**: If the generator detects that an artifact it would produce conflicts with a hand-written file at the same location, it MUST NOT silently overwrite the hand-written file; it MUST either skip with a warning identifying the conflicting file, or prompt the user for confirmation.
- **FR-017**: The generator MUST refuse, with a clear error, to operate on a module containing more than one qualifying UI file unless and until multi-UI support is explicitly added.

### Key Entities

- **Qualifying UI File**: A Kotlin source file containing exactly one `@Composable` function whose first parameter is a `viewModel`. This is the input to UI-FBP code generation.
- **UI Module**: A workspace module that contains a qualifying UI file and serves as the container for all generated artifacts. The module already exists prior to UI-FBP code generation.
- **Generated UI Artifact Set**: The complete collection of files (and any necessary build-config changes) that UI-FBP emits for a single qualifying UI file. After generation, this set must be self-sufficient for Runtime Preview, given any non-empty business-logic graph.
- **Source CodeNode**: A generated CodeNode whose outputs correspond to the qualifying UI Composable's input parameters (excluding `viewModel`); it represents the "user-input" boundary of the virtual circuit.
- **Sink CodeNode**: A generated CodeNode whose inputs correspond to the `StateFlow` properties observed by the UI Composable via its ViewModel; it represents the "UI-display" boundary of the virtual circuit.
- **FBP Graph (`.flow.kt`)**: The user-edited file that wires Source CodeNode outputs to Sink CodeNode inputs through arbitrary user-added business-logic CodeNodes. The "manually added FBP business logic" referenced by this feature lives here.
- **Preview Registration Entry Point**: A generated artifact that allows the GraphEditor's preview discovery mechanism to associate the module's UI name with its Composable rendering function so Runtime Preview can render the UI.
- **Runtime Controller Contract**: A generated interface and implementation that the runtime session factory uses to instantiate the module's ViewModel and bind it to the executing pipeline (start, stop, pause, resume, observable state).
- **Runtime Flow**: A generated representation of the module's `.flow.kt` graph in a form that the dynamic pipeline builder can execute when Runtime Preview is started.

## Assumptions

- The user invokes UI-FBP code generation against an existing module created via the module-scaffolding flow. Generating a brand-new module is the responsibility of the module-creation feature and is out of scope here.
- The user adds business logic by editing the generated `.flow.kt` graph (dragging in CodeNodes, wiring connections, adjusting properties). No code-level changes to generated source files are expected of the user for the normal workflow.
- The reference for "what Runtime Preview expects to find" is the contract established by the existing working modules in the DemoProject (StopWatch, UserProfiles, Addresses, EdgeArtFilter, WeatherForecast). Aligning UI-FBP output with that contract is the means of closing the Runtime Preview gap.
- The reference for "what a deployable production app expects to find" is the artifact set emitted today by the entity-module generator path (Controller.kt, ControllerAdapter.kt, Flow.kt runtime, ControllerInterface, ViewModel, State, nodes). Aligning UI-FBP output with that set is the means of closing the deployability gap. WeatherForecast omits the thick deployable files and is therefore not currently deployable without the GraphEditor; UI-FBP MUST NOT follow WeatherForecast's omission.
- The `TestModule/DemoUI.kt` example represents the current state of UI-FBP output and the duplicated `saved/` + `viewmodel/` package layout it contains is treated as legacy churn to be cleaned up by this feature, not a contract to be preserved.
- "Runtime Preview works" means: the UI renders, the module reports running state, and the Start/Stop/Pause/Resume controls function. Visualization quality of intermediate FBP data flow inside the graph view is governed by separate features (e.g., dataflow refinements, animation) and is out of scope here beyond not actively breaking it.
- Build-system integration is performed at module-scope only (e.g., the module's own `build.gradle.kts`). Project-level wiring (root `settings.gradle.kts`, root build script entries) is the responsibility of module scaffolding and is out of scope.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After running UI-FBP code generation on a qualifying UI file in an otherwise un-generated module, the user can open Runtime Preview for that module within 30 seconds and see the UI rendered with no errors and no manual file edits.
- **SC-002**: 100% of UI-FBP-generated modules compile cleanly on the first attempt under the project's standard build invocation, with zero generator-attributable compile errors.
- **SC-003**: Adding a passthrough business-logic graph in the generated `.flow.kt` and running Runtime Preview produces visible end-to-end data flow (UI input → graph → UI output) within one second of user interaction.
- **SC-004**: Re-running UI-FBP code generation against an unchanged source UI file produces zero changes to the `.flow.kt` graph contents and zero changes to the running behavior of the module in Runtime Preview.
- **SC-005**: When the source UI Composable's parameters or observed state change, re-running UI-FBP preserves at least 100% of user-added CodeNodes whose port references are still valid, and clearly reports any connections that were dropped because their ports no longer exist.
- **SC-006**: The current `TestModule` (with `DemoUI.kt`) is brought to a state where it loads in Runtime Preview, after either a single re-run of UI-FBP code generation or a documented one-time migration step. After that point, no duplicate State/ViewModel files (or other legacy artifacts) remain.
- **SC-007**: A user with no prior knowledge of which artifacts Runtime Preview requires can take a fresh module, drop in a qualifying UI file, run UI-FBP once, and reach a working Runtime Preview without consulting documentation about additional generated files or build wiring.
- **SC-008**: After UI-FBP code generation, the resulting module's deployable artifact set (file names, package layout, Controller / ControllerAdapter / Flow runtime / ControllerInterface / ViewModel / State / nodes) is structurally indistinguishable from the artifact set emitted today for entity modules, so that a production Android/iOS/Desktop app can consume it via the same instantiation pattern with no UI-FBP-specific code paths.
