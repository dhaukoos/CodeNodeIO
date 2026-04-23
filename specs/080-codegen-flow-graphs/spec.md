# Feature Specification: Code Generation Flow Graphs

**Feature Branch**: `080-codegen-flow-graphs`
**Created**: 2026-04-23
**Status**: Draft
**Input**: User description: "Create flow graphs that express generation paths as compositions of Generator CodeNodes, replacing the hardcoded orchestration in ModuleSaveService."

## Context

This is Step 4 of the Code Generation Migration Plan from feature 076. With 15 Generator CodeNodes created (feature 079) and module scaffolding extracted (feature 078), generation logic can now be expressed as flow graphs instead of hardcoded method sequences in ModuleSaveService. Each generation path (Generate Module, Repository, UI-FBP) becomes a flow graph where Generator CodeNodes are connected to form a pipeline.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Generation Flow Graphs (Priority: P1)

A developer creates three flow graph files that express code generation as compositions of Generator CodeNodes. `GenerateModule.flow.kt` connects a source node (providing GenerationConfig) to the 7 module-level Generator CodeNodes (FlowKt, RuntimeFlow, Controller, ControllerInterface, ControllerAdapter, ViewModel, UIStub) which fan out from the config and each produce a generated file. `GenerateRepository.flow.kt` extends this with entity-specific generators. `GenerateUIFBP.flow.kt` uses the UI-FBP generators. Each flow graph is loadable and viewable in the graph editor.

**Why this priority**: The flow graphs are the core deliverable — they express the generation pipeline visually and serve as the foundation for the CodeGenerationRunner and panel wiring.

**Independent Test**: Open each flow graph in the graph editor. Verify all Generator CodeNodes are connected with correct port mappings. Verify the graph layout is readable.

**Acceptance Scenarios**:

1. **Given** `GenerateModule.flow.kt`, **When** opened in the graph editor, **Then** it shows a source node connected to 7 Generator CodeNodes (FlowKt, RuntimeFlow, Controller, ControllerInterface, ControllerAdapter, ViewModel, UIStub), each producing a file content output
2. **Given** `GenerateRepository.flow.kt`, **When** opened in the graph editor, **Then** it shows the module generators plus 4 entity generators (CUD, Repository, Display, Persistence) connected with EntityModuleSpec input
3. **Given** `GenerateUIFBP.flow.kt`, **When** opened in the graph editor, **Then** it shows the shared generators (Flow, Controller) plus 4 UI-FBP generators (State, ViewModel, Source, Sink) connected with UIFBPSpec input
4. **Given** any of the 3 flow graphs, **When** a node is selected, **Then** its ports and connections are inspectable via the properties panel

---

### User Story 2 - CodeGenerationRunner (Priority: P2)

A developer creates a `CodeGenerationRunner` that takes a generation flow graph and an input spec, executes the pipeline, and collects the generated file contents from all output ports. The runner provides a mapping of generator name → generated content, which can then be written to disk by the caller. This replaces the sequential method calls in ModuleSaveService with a flow-graph-driven execution model.

**Why this priority**: The runner is the execution engine that makes the flow graphs operational. Without it, the flow graphs are visual-only.

**Independent Test**: Feed `GenerateModule.flow.kt` and a test FlowGraph to the runner. Verify the output contains 7 entries (one per generator) with non-empty content. Verify the output matches what ModuleSaveService currently produces.

**Acceptance Scenarios**:

1. **Given** `GenerateModule.flow.kt` and a FlowGraph input, **When** the runner executes, **Then** the output contains 7 generated file contents matching the underlying generators' direct output
2. **Given** the runner, **When** a generator CodeNode is disabled (removed from the flow graph), **Then** its output is not included in the results — only enabled nodes produce output
3. **Given** the runner, **When** execution completes, **Then** no files are written to disk — the runner only produces in-memory content. File writing is the caller's responsibility.

---

### User Story 3 - File Tree Selection Mapping (Priority: P3)

The file-tree checkbox selections from the Code Generator panel (feature 076) map to enabling or disabling individual Generator CodeNodes in the generation flow graph. When a user deselects a file in the tree, the corresponding Generator CodeNode is excluded from execution. This provides a unified model: the visual flow graph and the checkbox tree are two views of the same generation pipeline.

**Why this priority**: This connects the UI prototype (feature 076) to the execution model. Without this mapping, users can't selectively generate files.

**Independent Test**: Deselect "Controller.kt" in the file tree. Execute the generation. Verify the Controller generator is not executed and its output is absent from results.

**Acceptance Scenarios**:

1. **Given** a fully-selected file tree and a generation flow graph, **When** the runner executes, **Then** all generators produce output (same as no selection filtering)
2. **Given** a file tree with "Controller.kt" deselected, **When** the runner executes with this selection, **Then** the Controller generator is skipped and its output is absent
3. **Given** a file tree with an entire folder deselected (e.g., "controller/"), **When** the runner executes, **Then** all generators in that folder are skipped

---

### Edge Cases

- What happens when all files are deselected? The runner produces an empty result — no generators execute, no content is produced.
- What happens when the flow graph contains a generator that is not in the file tree? The generator executes regardless — the file tree is an opt-out mechanism, not an opt-in.
- What happens when a generator fails during execution? The runner collects a failure entry for that generator and continues executing the remaining generators — partial generation is supported.
- What happens when the flow graph file is corrupted or malformed? The runner reports a parse error and produces no output.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide 3 generation flow graph files: GenerateModule, GenerateRepository, and GenerateUIFBP, each expressing a generation pipeline as a composition of Generator CodeNodes
- **FR-002**: Each flow graph MUST be loadable and viewable in the graph editor with correct node layout and port connections
- **FR-003**: The system MUST provide a CodeGenerationRunner that executes a generation flow graph with a given input spec and produces a mapping of generator name → generated file content
- **FR-004**: The runner MUST NOT write files to disk — it produces in-memory content only
- **FR-005**: The runner MUST support selective execution — Generator CodeNodes can be excluded based on file-tree checkbox selections
- **FR-006**: The file-tree selections from the Code Generator panel MUST map to generator inclusion/exclusion in the flow graph execution
- **FR-007**: The runner MUST handle generator failures gracefully — reporting errors for failed generators while continuing to execute remaining generators
- **FR-008**: Each flow graph MUST use the Generator CodeNodes created in feature 079 — no new generator logic, only composition

### Key Entities

- **GenerationFlowGraph**: A flow graph file (.flow.kt) that connects Generator CodeNodes to express a code generation pipeline. Three variants: Module, Repository, UI-FBP.
- **CodeGenerationRunner**: An execution engine that uses the FBP runtime (DynamicPipelineController) to execute a generation flow graph with input spec and selection filter, leveraging the fan-out topology for parallel generator execution.
- **GenerationResult**: The output of the runner — a mapping from generator name to generated file content, with error tracking for failed generators.
- **SelectionFilter**: A mapping from generator IDs to enabled/disabled state, derived from the file-tree checkbox selections.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 3 generation flow graphs load in the graph editor without errors and display the correct number of Generator CodeNodes (7 for Module, 11+ for Repository, 8+ for UI-FBP)
- **SC-002**: The CodeGenerationRunner produces output matching ModuleSaveService for the same inputs — verified by comparison tests
- **SC-003**: Selective execution works — deselecting N generators produces output with exactly N fewer entries
- **SC-004**: The runner completes execution in under 5 seconds for any generation path
- **SC-005**: A developer can visually inspect and understand the generation pipeline by opening a flow graph in the editor — the layout is readable without manual rearrangement

## Clarifications

### Session 2026-04-23

- Q: Should the CodeGenerationRunner use a custom synchronous runner or the FBP runtime? → A: Use the FBP runtime (DynamicPipelineController). A custom runner contradicts the eat-our-own-dogfood principle. The fan-out topology naturally parallelizes independent generators. The fbpDsl runtime abstracts away the complexity — that abstraction is the product's core value proposition.

## Assumptions

- The generation flow graphs live in the tool's own directory structure (e.g., `flowGraph-generate/src/commonMain/kotlin/.../flow/`) following the eat-our-own-dogfood principle
- The CodeGenerationRunner lives in `flowGraph-generate` alongside the generators and wrappers
- The runner uses the same `GenerationConfig` data class (from feature 079) as the input spec for module-level generators
- File writing remains the responsibility of ModuleSaveService (or the Code Generator panel in Step 5) — the runner is purely a content-production engine
- The flow graphs use the standard .flow.kt DSL format that the graph editor already supports
- The file-tree to generator mapping uses the `generatorId` field from `FileNode` (in `GenerationFileTree` from feature 076) to identify which CodeNodes to include/exclude
