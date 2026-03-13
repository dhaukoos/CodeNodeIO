# Feature Specification: Self-Contained CodeNode Definition

**Feature Branch**: `050-self-contained-codenode`
**Created**: 2026-03-13
**Status**: Draft
**Input**: User description: "Redefine how nodes (CodeNodes) are defined and created. CodeNode objects should become self-contained in a single file including their processing logic. The NodeGenerator creates a {NodeName}CodeNode class added to the Palette. When selected from the Node Palette, it adds an instance to the flowGraph."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate a Self-Contained Node (Priority: P1)

A graph author wants to create a new processing node for their pipeline. They use the Node Generator in the graph editor, specify the node's name, port configuration (input/output count and types), and category. They also choose the placement level — Module (scoped to a specific module), Project (shared across the project, the default), or Universal (available across all projects). The system generates a single file containing the complete node definition — including its port layout, metadata, and a placeholder processing logic function — at the selected location. The author then opens that file and fills in the processing logic with their domain-specific algorithm. If needed, the file can later be moved to a different level and the graph editor will still discover it.

**Why this priority**: This is the foundational capability. Without a single-file node definition, none of the downstream stories (palette integration, runtime resolution, hot-swap) are possible. It also directly addresses the current pain point where a node's identity is scattered across 4+ files with no link between them.

**Independent Test**: Can be tested by invoking the Node Generator for a new node (e.g., "BlurFilter" with 1 input, 1 output), verifying the generated file exists at the expected location, confirming it contains the port definition and a processing logic placeholder, and verifying the file compiles without errors.

**Acceptance Scenarios**:

1. **Given** the graph editor is open, **When** the user invokes the Node Generator with name "BlurFilter", 1 input, 1 output, and selects "Project" level, **Then** a single file is created at the project-wide nodes location containing the complete node definition with a processing logic stub.
2. **Given** a generated node file exists with a placeholder, **When** the user replaces the placeholder with actual processing logic, **Then** the file compiles and the node is ready for use in a flow graph.
3. **Given** a node with the same name already exists at the target location, **When** the user attempts to generate it again, **Then** the system warns that the file already exists and does not overwrite it.
4. **Given** a node was generated at the Project level, **When** the user moves the file to the Module level directory, **Then** the graph editor still discovers and displays the node in the palette on next startup.

---

### User Story 2 - Use Generated Node from the Palette (Priority: P2)

A graph author has generated and edited a node definition (e.g., "SepiaTransformer"). When they open the graph editor, the node appears in the Node Palette. They drag it onto the canvas, and the system creates an instance in the flow graph with the correct port configuration. When the flow graph is executed in runtime preview, the node's processing logic runs as part of the pipeline.

**Why this priority**: This closes the loop between node creation and node usage. Without palette integration and runtime resolution, generated nodes are just inert files. This story makes them functional.

**Independent Test**: Can be tested by generating a node, adding processing logic, opening the graph editor, verifying the node appears in the palette, dragging it onto a canvas, connecting it to other nodes, and running the pipeline to confirm the processing logic executes.

**Acceptance Scenarios**:

1. **Given** a self-contained node definition exists with valid processing logic, **When** the graph editor starts, **Then** the node appears in the Node Palette with the correct name, port count, and category.
2. **Given** a self-contained node is in the palette, **When** the user drags it onto the canvas, **Then** a node instance is created in the flow graph with the correct input and output ports.
3. **Given** a flow graph contains a self-contained node wired into a pipeline, **When** the user runs the runtime preview, **Then** the node's processing logic executes and data flows through it.

---

### User Story 3 - Swap Compatible Nodes at Runtime (Priority: P3)

A graph author has a working pipeline with a "GrayscaleTransformer" node. They want to try a different effect, so they remove GrayscaleTransformer from the graph and replace it with "SepiaTransformer" (which has the same port signature). When they run the pipeline, the SepiaTransformer's processing logic executes — producing a sepia-toned result instead of grayscale — without any code changes outside the graph editor.

**Why this priority**: This is the "hot-swap" capability that demonstrates the power of self-contained nodes. It depends on both node generation (US1) and palette/runtime integration (US2) being in place first.

**Independent Test**: Can be tested by building a pipeline with one transformer, running it, then swapping the transformer for a compatible alternative, re-running, and verifying the output reflects the new node's processing logic.

**Acceptance Scenarios**:

1. **Given** a running pipeline with NodeA in a transformer slot, **When** the user replaces NodeA with NodeB (same port signature), **Then** re-running the pipeline produces output from NodeB's processing logic.
2. **Given** a user attempts to swap NodeA with NodeC (incompatible port signature), **When** they try to connect NodeC, **Then** the system prevents or warns about the incompatible connection.

---

### User Story 4 - Standalone Node Testing (Priority: P4)

A node author wants to verify their processing logic works correctly before integrating the node into a full pipeline. The self-contained node file provides everything needed to write and run an isolated unit test — creating the node, feeding it test input, and asserting on the output — without requiring a flow graph, controller, or other infrastructure.

**Why this priority**: Enables test-driven node development. Depends on the node definition structure (US1) being established first.

**Independent Test**: Can be tested by writing a unit test that instantiates the node class, provides test input data, invokes the processing logic, and asserts the output matches expectations.

**Acceptance Scenarios**:

1. **Given** a self-contained node definition with processing logic, **When** a developer writes a unit test that instantiates the node and feeds it input, **Then** the test can invoke the processing logic and assert on the output without any flow graph dependencies.

---

### User Story 5 - Migrate EdgeArtFilter Nodes (Priority: P5)

A developer migrates all 6 EdgeArtFilter nodes (ImagePicker, GrayscaleTransformer, EdgeDetector, ColorOverlay, SepiaTransformer, ImageViewer) from the legacy pattern (separate CustomNodeDefinition registration + standalone ProcessingLogic file) to the new self-contained CodeNodeDefinition format. After migration, the EdgeArtFilter pipeline runs identically using the new node definitions — discovered via the registry instead of hardcoded in Main.kt — and the legacy registration code for these nodes is removed.

**Why this priority**: Validates the entire feature end-to-end with a real module. Depends on all prior stories being functional. Serves as the proof-of-concept that demonstrates the new pattern works in practice and provides a migration template for future modules.

**Independent Test**: Can be tested by running the EdgeArtFilter runtime preview and verifying identical output (grayscale, edge detection, color overlay, sepia) before and after migration. The 6 legacy CustomNodeDefinition entries and 6 separate ProcessingLogic files should be replaced by 6 self-contained CodeNode files.

**Acceptance Scenarios**:

1. **Given** all 6 EdgeArtFilter nodes have been migrated to self-contained format, **When** the graph editor starts, **Then** all 6 nodes appear in the palette with correct names, categories (Source: ImagePicker; Transformer: GrayscaleTransformer, EdgeDetector, SepiaTransformer; Processor: ColorOverlay; Sink: ImageViewer), and port counts — discovered from the registry, not hardcoded.
2. **Given** the migrated EdgeArtFilter pipeline is loaded, **When** runtime preview runs, **Then** the visual output is identical to pre-migration (grayscale + edge detection + color overlay produces the same edge art effect).
3. **Given** migration is complete, **When** the hardcoded `edgeArtFilterNodes` list and color-coding overrides are removed from Main.kt, **Then** the pipeline still works because the registry provides both node definitions and category metadata.
4. **Given** a migrated SepiaTransformer and GrayscaleTransformer both exist as self-contained nodes, **When** the user swaps one for the other on the canvas and re-runs, **Then** the output reflects the swapped node's processing logic — validating the hot-swap capability from US3.

---

### Edge Cases

- What happens when a node's processing logic throws an exception at runtime? The pipeline should handle the error gracefully without crashing other nodes.
- What happens when a generated node file is deleted after it has been used in saved flow graphs? The system should report that the node definition cannot be found when opening the flow graph.
- What happens when two nodes have the same name but different port configurations? The system should prevent duplicate names within the same node registry scope.
- What happens when a node's port signature changes after it has been wired into existing flow graphs? The system should detect the mismatch when the flow graph is loaded and report which connections are invalid.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Node Generator MUST produce a single file per node containing the complete node definition: name, port configuration, category, and processing logic placeholder.
- **FR-002**: The Node Generator MUST offer three placement levels for the generated node file — Module (within a specific module's directory), Project (in a shared project-wide nodes directory), or Universal (in a user-level directory outside the project) — with Project as the default selection.
- **FR-002a**: The graph editor MUST discover node definitions from all three levels (Module, Project, Universal) on startup, regardless of where they were originally generated.
- **FR-002b**: A node file MUST remain functional if moved from one level to another after generation — the graph editor discovers nodes by scanning all three locations, not by remembering where they were generated.
- **FR-003**: The generated file MUST compile without errors immediately after generation (before the user edits the processing logic).
- **FR-004**: The graph editor MUST automatically discover and display all valid self-contained node definitions in the Node Palette on startup.
- **FR-005**: When a self-contained node is dragged from the palette onto the canvas, the system MUST create a node instance in the flow graph with the port configuration defined in the node's class.
- **FR-006**: When a flow graph is executed in runtime preview, the system MUST resolve each node's processing logic from its self-contained definition — not from hardcoded references in generated runtime files.
- **FR-007**: The system MUST prevent generation of a node with a name that conflicts with an existing node definition in the same scope.
- **FR-008**: A self-contained node definition MUST provide sufficient structure for a developer to write a standalone unit test without requiring a flow graph, controller, or runtime wiring.
- **FR-009**: The system MUST detect and report port signature mismatches when loading a flow graph that references a node whose definition has changed.
- **FR-010**: The Node Generator MUST allow the user to specify the node's category (source, transformer, processor, sink) to determine palette grouping and visual color coding.

### Key Entities

- **Self-Contained Node Definition**: A single-file class that encapsulates a node's identity (name, category), interface (input/output ports with types), and behavior (processing logic function). Replaces the current pattern of separate CustomNodeDefinition + ProcessingLogic + generated runtime references.
- **Node Registry**: The mechanism by which the graph editor discovers available node definitions. Replaces the current hardcoded registration in Main.kt and CustomNodeRepository persistence.
- **Node Instance**: A placed occurrence of a node definition within a flow graph, with position, connections, and runtime state. Created when a user drags a node from the palette onto the canvas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new node can be generated, edited, and used in a running pipeline within 5 minutes of starting the Node Generator — without editing any file other than the generated node file itself.
- **SC-002**: Swapping one node for another compatible node in a graph and re-running produces the new node's output — with zero code changes outside the graph editor.
- **SC-003**: 100% of self-contained node definitions can be independently unit tested with only the node file and test framework — no flow graph or controller setup required.
- **SC-004**: The graph editor discovers and displays all valid node definitions on startup without any manual registration steps.

## Assumptions

- The existing Node Generator UI in the graph editor can be extended to support the new node definition format (no new UI surface needed).
- The existing Node Palette can display nodes from the new self-contained format alongside any legacy nodes during a transition period.
- Port type compatibility for node swapping is determined by matching input/output count and types — exact port naming is not required for compatibility.
- The processing logic placeholder in generated files is a no-op or pass-through that produces valid (if meaningless) output, ensuring the file compiles and runs before the user edits it.

## Scope Boundaries

**In scope**:
- New node definition format (single-file, self-contained)
- Node Generator producing the new format
- Graph editor palette integration with auto-discovery
- Runtime resolution of processing logic from node definitions
- Node swapping in the graph editor with runtime effect
- Migration of all 6 EdgeArtFilter nodes (ImagePicker, GrayscaleTransformer, EdgeDetector, ColorOverlay, SepiaTransformer, ImageViewer) to the new self-contained format as a proof-of-concept and validation

**Out of scope**:
- Migration of all existing modules to the new format beyond EdgeArtFilter (can coexist with legacy pattern)
- Visual node editor for editing processing logic inline in the graph editor
- Remote/shared node repositories or marketplaces
- Versioning or dependency management between node definitions
