# Feature Specification: Dynamic Runtime Pipeline

**Feature Branch**: `051-dynamic-runtime-pipeline`
**Created**: 2026-03-15
**Status**: Draft
**Input**: User description: "Enable hot-swapping of nodes in a flowGraph by dynamically building the runtime pipeline from the editor's FlowGraph using NodeDefinitionRegistry, rather than relying on pre-compiled Flow classes."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Run Pipeline from Canvas (Priority: P1)

A developer designs a node pipeline on the graph editor canvas (e.g., ImagePicker -> GrayscaleTransformer -> EdgeDetector -> ColorOverlay -> ImageViewer), presses Start, and the system builds and runs the pipeline directly from the canvas layout. No pre-compiled Flow class is needed — the runtime reads node names from the canvas FlowGraph, looks them up in the registry, creates runtimes, wires channels based on the canvas connections, and executes the pipeline.

**Why this priority**: This is the foundational capability. Without dynamically building a pipeline from the canvas, no other hot-swap scenarios are possible. It also provides immediate value: any module whose nodes are registered as CodeNodeDefinitions can be run directly from the canvas without a generated Flow class.

**Independent Test**: Load the EdgeArtFilter module, press Start, pick an image, and verify the processed image appears — all using the dynamic pipeline builder instead of the hardcoded EdgeArtFilterFlow.

**Acceptance Scenarios**:

1. **Given** a canvas with connected CodeNode-backed nodes (e.g., EdgeArtFilter pipeline), **When** the user presses Start, **Then** the system resolves each node name via the registry, creates runtimes, wires channels matching the canvas connections, and executes the pipeline producing the expected output.
2. **Given** a canvas with a node whose name is not found in the registry, **When** the user presses Start, **Then** the system displays a clear error identifying the unresolvable node(s) and does not start the pipeline.
3. **Given** a canvas with nodes that have incompatible port connections (e.g., output port wired to a non-existent input port), **When** the user presses Start, **Then** the system displays a validation error before attempting to run.

---

### User Story 2 - Swap a Node and Re-Run (Priority: P2)

A developer has a running pipeline, stops it, replaces one node with a compatible alternative (e.g., swaps GrayscaleTransformer for SepiaTransformer on the canvas), and presses Start again. The new pipeline uses the replacement node's processing logic without any code changes or recompilation.

**Why this priority**: This is the primary user-facing value of the feature — the ability to experiment with different processing nodes interactively. It depends on US1 being complete.

**Independent Test**: Run the EdgeArtFilter pipeline, stop it, drag SepiaTransformer onto the canvas replacing GrayscaleTransformer, re-wire connections, press Start, pick an image, and verify the output reflects sepia-toned processing instead of grayscale.

**Acceptance Scenarios**:

1. **Given** a stopped pipeline where GrayscaleTransformer has been replaced with SepiaTransformer (same port signature: 1 in, 1 out), **When** the user presses Start, **Then** the new pipeline produces sepia-toned output.
2. **Given** a stopped pipeline where a node is replaced with one that has a different port count, **When** the user presses Start, **Then** the system displays a port mismatch warning identifying the incompatible connections.
3. **Given** a running pipeline, **When** the user presses Stop then immediately Start without making changes, **Then** the pipeline restarts cleanly with the same behavior as before.

---

### User Story 3 - Preserve Module-Specific Behavior (Priority: P3)

Modules that have specialized runtime needs (e.g., StopWatch with its timer-based architecture, UserProfiles with its DAO injection) continue to work correctly. The dynamic pipeline builder is used only for modules where all nodes are registered CodeNodeDefinitions. Modules without full registry coverage fall back to their existing generated Flow/Controller pattern.

**Why this priority**: Backward compatibility is essential — breaking existing modules would be unacceptable. However, this is lower priority because it's a safeguard rather than new functionality.

**Independent Test**: Load the StopWatch module, press Start, and verify it runs exactly as before using its existing generated Controller/Flow. Load EdgeArtFilter and verify it uses the dynamic pipeline.

**Acceptance Scenarios**:

1. **Given** a module where all canvas nodes have CodeNodeDefinition entries in the registry, **When** the user presses Start, **Then** the dynamic pipeline builder is used.
2. **Given** a module where some canvas nodes lack CodeNodeDefinition entries (e.g., StopWatch), **When** the user presses Start, **Then** the system falls back to the existing generated Controller/Flow pattern and the module runs as before.
3. **Given** a module that transitions from partial to full registry coverage (developer adds CodeNodeDefinitions for all its nodes), **When** the user restarts the graph editor, **Then** the module automatically uses the dynamic pipeline builder.

---

### Edge Cases

- What happens when the canvas has disconnected nodes (nodes not wired into the pipeline)?
  - Disconnected nodes are ignored; only connected subgraphs are executed.
- What happens when the canvas has a cycle in the connections?
  - The system detects cycles during validation and reports an error before starting.
- What happens when a node's processing logic throws an exception at runtime?
  - The pipeline transitions to ERROR state and the error is surfaced in the execution state indicator, matching current behavior.
- What happens when the user modifies the canvas while the pipeline is running?
  - Canvas edits do not affect the currently running pipeline. Changes take effect on the next Start.
- What happens when a fan-out connection exists (one output wired to multiple inputs)?
  - The dynamic wiring must replicate the fan-out pattern, creating appropriate channel splits.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST be able to read the list of nodes and their connections from the editor's FlowGraph at runtime.
- **FR-002**: System MUST resolve each node name to a CodeNodeDefinition via the NodeDefinitionRegistry.
- **FR-003**: System MUST call `createRuntime()` on each resolved CodeNodeDefinition to obtain a NodeRuntime instance.
- **FR-004**: System MUST dynamically create and wire channels between node runtimes based on the FlowGraph's connection topology.
- **FR-005**: System MUST support all existing connection patterns: one-to-one, fan-out (one output to multiple inputs), and fan-in (multiple outputs to one multi-input node).
- **FR-006**: System MUST validate the canvas pipeline before starting: all node names must be resolvable, all connections must reference valid ports, and no cycles may exist.
- **FR-007**: System MUST display clear error messages when validation fails, identifying the specific nodes or connections that are problematic.
- **FR-008**: System MUST fall back to the existing generated Controller/Flow pattern for modules where not all nodes have CodeNodeDefinition entries in the registry.
- **FR-009**: System MUST support Stop, Start, Pause, and Resume lifecycle operations on dynamically-built pipelines, matching the behavior of generated pipelines.
- **FR-010**: System MUST support speed attenuation and data flow animation on dynamically-built pipelines.
- **FR-011**: System MUST clean up all channels and runtimes when a dynamically-built pipeline is stopped.
- **FR-012**: System MUST build a fresh pipeline on each Start, reflecting any canvas changes made since the last run.

### Key Entities

- **DynamicPipeline**: A runtime-constructed pipeline comprising resolved node runtimes and wired channels, built from a FlowGraph snapshot. Manages lifecycle (start, stop, pause, resume) and cleanup.
- **PipelineValidationResult**: The outcome of pre-start validation, containing either success confirmation or a list of errors (unresolvable nodes, invalid connections, cycles).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can swap a compatible node on the canvas and see different output on the next run, with zero code changes or recompilation steps.
- **SC-002**: The dynamic pipeline produces identical output to the hardcoded EdgeArtFilter pipeline for the same input image and node configuration.
- **SC-003**: Pipeline startup time (from pressing Start to first data emission) is within 500ms of the existing generated pipeline approach.
- **SC-004**: All existing modules (StopWatch, UserProfiles, GeoLocations, Addresses) continue to function correctly without modification.
- **SC-005**: When a node name cannot be resolved, the user sees a specific error message within 1 second of pressing Start.

## Assumptions

- All nodes intended for hot-swap are registered as CodeNodeDefinitions in the NodeDefinitionRegistry (compiled or programmatically registered).
- The editor's FlowGraph accurately represents the desired pipeline topology (nodes, ports, connections).
- Port names in the FlowGraph connections correspond to the port names declared in CodeNodeDefinition's inputPorts/outputPorts.
- The dynamic pipeline builder is not responsible for generating or compiling new code — it works with already-registered CodeNodeDefinitions.
- Canvas modifications while a pipeline is running do not affect the running pipeline; changes are picked up on the next Start.

## Scope Boundaries

**In scope**:
- Dynamic pipeline construction from canvas FlowGraph
- Node swap and re-run without recompilation
- Validation of pipeline before execution
- Fallback to generated Flow for non-registry modules
- Full lifecycle support (start, stop, pause, resume, attenuation, animation)

**Out of scope**:
- Live hot-swap while the pipeline is running (requires stop + start)
- Automatic type-checking of data flowing between ports at design time
- Dynamic compilation of new CodeNodeDefinitions from source files
- Modifying the Save/code-generation workflow
