# Feature Specification: Modify Source and Sink Nodes

**Feature Branch**: `037-source-sink-refactor`
**Created**: 2026-03-04
**Status**: Draft
**Input**: Rename generator nodes as source nodes everywhere. Change source nodes to emit values via ViewModel binding instead of a while loop with tick function. Change sink nodes to not have a processing logic stub file. Rename in0outN runtime nodes as sourceOutN. Rename inNout0 runtime nodes as sinkInN.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Rename Generator Terminology to Source (Priority: P1)

As a developer working with the flow-based programming system, I want all references to "generator" nodes to use "source" terminology instead, so that the naming accurately reflects their role as data entry points driven by the user interface rather than autonomous data producers.

**Why this priority**: The terminology change is foundational — all other changes (removing tick loops, removing sink stubs) build on the conceptual shift from "generator" to "source." Without consistent naming, the codebase becomes confusing with mixed terminology.

**Independent Test**: Can be verified by searching the codebase for "generator" references in node types, runtime classes, factory methods, type aliases, and UI labels — all should be replaced with "source" equivalents. The system should compile and all existing tests should pass with the renamed types.

**Acceptance Scenarios**:

1. **Given** a node type previously called "Generator", **When** I inspect the node type enumeration, **Then** it is named "Source" instead.
2. **Given** runtime classes previously named with "Generator" (e.g., GeneratorRuntime), **When** I look up the runtime class for a source node, **Then** it is named with "Source" (e.g., SourceRuntime).
3. **Given** factory methods previously named with "Generator" (e.g., createContinuousGenerator, createTimedGenerator), **When** I create a source node runtime, **Then** the factory method uses "Source" in its name.
4. **Given** type aliases previously named with "Generator" (e.g., GeneratorTickBlock, ContinuousGeneratorBlock), **When** I reference block types for source nodes, **Then** they use "Source" terminology.
5. **Given** the graph editor UI previously labeled source nodes as "Generator", **When** I view a source node in the editor, **Then** it is labeled "Source."

---

### User Story 2 - Source Nodes Emit via ViewModel Binding (Priority: P1)

As a developer, I want source nodes to emit values to their output channels through the ViewModel binding to user interface functions, rather than containing an internal while loop with a tick function, so that data entry into the flow graph is driven by UI interactions (button presses, text input, etc.) rather than autonomous timed polling.

**Why this priority**: This is a fundamental behavioral change that redefines how data enters the flow graph. Source nodes become passive conduits for UI-driven data rather than active producers, which is essential for the UI-first architecture established in feature 034.

**Independent Test**: Can be verified by confirming that generated source node runtime code does not contain a timed loop or tick function call. Instead, source nodes should expose an emit mechanism that the ViewModel can invoke when the UI produces data.

**Acceptance Scenarios**:

1. **Given** a source node in a flow graph, **When** module code is generated, **Then** the source node runtime does not contain a while loop or timed tick invocation.
2. **Given** a source node in a flow graph, **When** module code is generated, **Then** the source node's output channel is populated via the ViewModel binding to UI-facing functions.
3. **Given** a source node with a processing logic stub previously generated, **When** the source node behavior changes to ViewModel-driven emission, **Then** no processing logic stub file is generated for source nodes.

---

### User Story 3 - Sink Nodes Have No Processing Logic Stub (Priority: P2)

As a developer, I want sink nodes to not generate a processing logic stub file, since sink nodes simply bind their input channels to the ViewModel's parameterFlow values (as established in feature 034), so that unnecessary stub files are eliminated and the architecture is cleaner.

**Why this priority**: This is a simplification that removes unnecessary generated files. The functional change was already done in feature 034 (sink input ports drive observable state in the ViewModel); this story removes the now-redundant stub generation.

**Independent Test**: Can be verified by saving a module containing sink nodes and confirming no `{SinkNodeName}ProcessLogic.kt` file is created in the processingLogic directory.

**Acceptance Scenarios**:

1. **Given** a flow graph containing sink nodes, **When** the module is saved, **Then** no processing logic stub file is generated for any sink node.
2. **Given** an existing module with previously generated sink processing logic stubs, **When** the module is re-saved, **Then** existing sink stub files are treated as orphans and deleted.
3. **Given** a flow graph with both sink nodes and transformer nodes, **When** the module is saved, **Then** processing logic stubs are still generated for transformer nodes but not for sink nodes.

---

### User Story 4 - Rename Multi-Output Source Runtime Classes (Priority: P2)

As a developer, I want the runtime classes for source nodes with multiple outputs to follow the naming convention `SourceOut{N}Runtime` instead of `Out{N}GeneratorRuntime`, and their corresponding block types and factory methods to use `sourceOut{N}` naming, so that naming is consistent with the source terminology and intuitive to read.

**Why this priority**: Consistent naming across the runtime class hierarchy prevents confusion and makes the API self-documenting. This builds on the P1 rename but covers the multi-output variants specifically.

**Independent Test**: Can be verified by confirming that classes like `Out2GeneratorRuntime` are renamed to `SourceOut2Runtime`, factory methods like `createOut2Generator` are renamed to `createSourceOut2`, and corresponding type aliases follow suit.

**Acceptance Scenarios**:

1. **Given** a source node with 2 outputs, **When** I look up the runtime class, **Then** it is named `SourceOut2Runtime` (not `Out2GeneratorRuntime`).
2. **Given** a source node with 3 outputs, **When** I look up the runtime class, **Then** it is named `SourceOut3Runtime` (not `Out3GeneratorRuntime`).
3. **Given** factory methods for multi-output sources, **When** I create a runtime instance, **Then** the methods use `sourceOut2`/`sourceOut3` naming.
4. **Given** block type aliases for multi-output sources, **When** I reference the types, **Then** they use `SourceOut2`/`SourceOut3` naming.

---

### User Story 5 - Rename Multi-Input Sink Runtime Classes (Priority: P2)

As a developer, I want the runtime classes for sink nodes with multiple inputs to follow the naming convention `SinkIn{N}Runtime` instead of `In{N}SinkRuntime`, and their corresponding block types and factory methods to use `sinkIn{N}` naming, so that naming is consistent and follows the same `{Role}{Direction}{Count}` pattern as source nodes.

**Why this priority**: Parallel to Story 4, this ensures the sink side of the naming convention matches the source side for consistency.

**Independent Test**: Can be verified by confirming that classes like `In2SinkRuntime` are renamed to `SinkIn2Runtime`, factory methods like `createIn2Sink` are renamed to `createSinkIn2`, and corresponding type aliases follow suit.

**Acceptance Scenarios**:

1. **Given** a sink node with 2 inputs, **When** I look up the runtime class, **Then** it is named `SinkIn2Runtime` (not `In2SinkRuntime`).
2. **Given** a sink node with 3 inputs, **When** I look up the runtime class, **Then** it is named `SinkIn3Runtime` (not `In3SinkRuntime`).
3. **Given** factory methods for multi-input sinks, **When** I create a runtime instance, **Then** the methods use `sinkIn2`/`sinkIn3` naming.
4. **Given** block type aliases for multi-input sinks, **When** I reference the types, **Then** they use `SinkIn2`/`SinkIn3` naming.

---

### Edge Cases

- What happens when a module is re-saved that was previously generated with the old "Generator" naming? Old generated files should be overwritten with the new naming; orphaned files with old names should be deleted.
- What happens to existing processing logic stubs for source nodes that previously had tick functions? Since source nodes no longer use tick functions, existing source stub files should be treated as orphans and deleted on re-save.
- What happens to existing processing logic stubs for sink nodes? They should be treated as orphans and deleted on re-save.
- How are existing "any-input" sink variants (e.g., `In2AnySinkRuntime`) renamed? They follow the same pattern: `SinkIn2AnyRuntime`, `SinkIn3AnyRuntime`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST rename the node type enumeration value from `GENERATOR` to `SOURCE` throughout the codebase.
- **FR-002**: The system MUST rename the base single-output runtime class from `GeneratorRuntime` to `SourceRuntime`.
- **FR-003**: The system MUST rename multi-output runtime classes from `Out{N}GeneratorRuntime` to `SourceOut{N}Runtime` (for N = 2, 3).
- **FR-004**: The system MUST rename multi-input sink runtime classes from `In{N}SinkRuntime` to `SinkIn{N}Runtime` (for N = 2, 3).
- **FR-005**: The system MUST rename "any-input" sink runtime variants from `In{N}AnySinkRuntime` to `SinkIn{N}AnyRuntime` (for N = 2, 3).
- **FR-006**: The system MUST rename all factory methods to use the new naming conventions (e.g., `createContinuousGenerator` → `createContinuousSource`, `createIn2Sink` → `createSinkIn2`).
- **FR-007**: The system MUST rename all type aliases (block types) to use the new naming conventions (e.g., `GeneratorTickBlock` → `SourceTickBlock`, `In2SinkBlock` → `SinkIn2Block`).
- **FR-008**: Source nodes MUST NOT generate a processing logic stub file. They emit data via ViewModel binding to UI functions, not via tick functions.
- **FR-009**: Sink nodes MUST NOT generate a processing logic stub file. They bind input channels to ViewModel parameterFlow values.
- **FR-010**: The code generator MUST NOT produce a while loop or timed tick invocation for source node runtimes.
- **FR-011**: The module save process MUST delete orphaned processing logic stubs for source and sink nodes when re-saving a previously generated module.
- **FR-012**: The graph editor UI MUST display "Source" instead of "Generator" for source node types.
- **FR-013**: All code generators (RuntimeFlowGenerator, RuntimeTypeResolver, RuntimeControllerGenerator, etc.) MUST use the renamed types and factory methods in generated output.
- **FR-014**: All existing tests MUST be updated to use the new naming and continue to pass.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero references to "Generator" as a node type remain in runtime classes, factory methods, type aliases, and generated code (excluding deprecation annotations if used for migration).
- **SC-002**: All renamed classes, methods, and type aliases compile without errors.
- **SC-003**: All existing tests pass after renaming, confirming no behavioral regressions.
- **SC-004**: Saving a module with source nodes produces zero processing logic stub files for those nodes.
- **SC-005**: Saving a module with sink nodes produces zero processing logic stub files for those nodes.
- **SC-006**: Re-saving a previously generated module correctly deletes orphaned stub files from both old source (generator) and sink nodes.
- **SC-007**: Generated source node runtime code contains no while loop or timed tick invocation pattern.

## Assumptions

- The behavioral change for sink nodes (binding to ViewModel parameterFlow) was already completed in feature 034. This feature only removes the now-redundant stub generation.
- The ViewModel binding mechanism for source nodes (how UI functions push data to output channels) already exists or will be extended as part of this feature's code generation changes.
- Deprecation annotations for old names are not required — this is a clean rename since the codebase is under active development and backward compatibility is not needed.
- The "any-input" sink variants follow the same renaming pattern as the standard multi-input sinks.
