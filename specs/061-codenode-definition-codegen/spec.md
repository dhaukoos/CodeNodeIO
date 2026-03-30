# Feature Specification: Generate CodeNodeDefinition-Based Repository Modules

**Feature Branch**: `061-codenode-definition-codegen`
**Created**: 2026-03-30
**Status**: Draft
**Input**: User description: "Update the entity module code generator to produce CodeNodeDefinition-based node files for all three nodes (CUD, Repository, Display), matching the pattern established by the hand-written nodes in existing modules. Update the EntityFlowGraphBuilder to tag all nodes with _codeNodeClass and _codeNodeDefinition config. Update RuntimeFlowGenerator to handle these nodes correctly."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generated Repository Modules Compile and Run (Priority: P1)

When a developer creates a new repository module via the graphEditor's "Create Repository Module" button, the generated module compiles successfully and its runtime pipeline executes correctly. All three generated nodes (CUD source, Repository processor, Display sink) are self-contained node definitions that the graphEditor discovers and executes at runtime — no hand-written code is required after generation.

**Why this priority**: This is the core deliverable. Generated modules must compile out of the box. Currently they fail with missing tick function references and type mismatches.

**Independent Test**: Create a new IP type (e.g., "TestItem" with a "name" String property) in the graphEditor, click "Create Repository Module", then compile the generated module. Verify it compiles with zero errors and the runtime pipeline executes in the graphEditor's preview.

**Acceptance Scenarios**:

1. **Given** a custom IP type exists in the IP Generator, **When** the developer clicks "Create Repository Module", **Then** the generated module directory contains node definition files for all three nodes (CUD, Repository, Display) in a `nodes/` subdirectory.
2. **Given** a newly generated repository module, **When** the developer compiles it, **Then** the build succeeds with zero errors referencing legacy tick functions or type mismatches.
3. **Given** a compiled generated module, **When** the developer loads it in the graphEditor, **Then** all three nodes appear in the node palette and the flow graph renders with correct node types.
4. **Given** a loaded generated module, **When** the developer starts the runtime pipeline, **Then** data flows through the CUD → Repository → Display pipeline and CRUD operations persist to the database.

---

### User Story 2 - Generated Nodes Match Existing Module Pattern (Priority: P2)

The generated node definition files follow the same architectural pattern as the hand-written nodes in existing modules (UserProfiles, GeoLocations, Addresses). This includes using the IP type data classes (from `iptypes/`) instead of `Any`, proper entity conversion via `toEntity()`/`fromEntity()` extensions, and correct runtime factory method selection.

**Why this priority**: Consistency with existing modules ensures the generated code is maintainable and follows established conventions. Developers familiar with one module can understand all modules.

**Independent Test**: Compare a generated `*RepositoryCodeNode.kt` with the hand-written `UserProfileRepositoryCodeNode.kt`. Verify they follow the same structure: CodeNodeDefinition interface, typed PortSpecs, typed factory calls, entity conversion at the persistence boundary.

**Acceptance Scenarios**:

1. **Given** a generated CUD source node, **When** inspected, **Then** it implements CodeNodeDefinition with typed output ports matching the IP type (not `Any::class`).
2. **Given** a generated Repository processor node, **When** inspected, **Then** it uses the module's IP type for input ports, `String::class` for output ports (result/error), and converts to/from the persistence entity at the DAO boundary.
3. **Given** a generated Display sink node, **When** inspected, **Then** it implements CodeNodeDefinition with `String::class` input ports for result and error.
4. **Given** any generated node, **When** the flow graph is viewed in the graphEditor's textual view, **Then** the port types show the concrete IP type names (not `Any`).

---

### User Story 3 - Legacy Code Paths Eliminated (Priority: P3)

The legacy factory-function pattern (`createXxxCUD()`, `createXxxDisplay()`, tick functions) is no longer generated for new modules. The RuntimeFlowGenerator correctly handles CodeNodeDefinition-tagged nodes without producing tick function imports. Existing modules that were previously hand-migrated continue to work unchanged.

**Why this priority**: Eliminating legacy code paths reduces confusion and prevents future compilation errors. This is cleanup that makes the codebase consistent.

**Independent Test**: After generating a new module, verify that the generated `*Flow.kt` file contains no references to tick functions or legacy factory calls. Verify existing modules (UserProfiles, etc.) still compile and run.

**Acceptance Scenarios**:

1. **Given** a generated module, **When** the `*Flow.kt` file is inspected, **Then** it references CodeNodeDefinition objects (not tick functions or `createXxx()` factory calls).
2. **Given** the existing UserProfiles module, **When** compiled after the generator changes, **Then** it continues to compile and run correctly with no regressions.
3. **Given** the EntityFlowGraphBuilder, **When** it constructs a flow graph, **Then** all three nodes have `_codeNodeClass` and `_codeNodeDefinition` configuration entries pointing to the generated CodeNodeDefinition classes.

---

### Edge Cases

- What happens if the IP type has no properties (marker type)? The generated Repository node should still compile with an empty data class.
- What happens if the IP type has properties with complex types (e.g., List<String>)? The generated IP type and entity should handle nullable and collection types correctly.
- What happens when removing a generated module? The "Remove Repository Module" flow should clean up node definition files from the `nodes/` subdirectory in addition to existing cleanup targets.
- What happens if the developer modifies a generated node after creation? The generated files should be clearly marked as generated but editable — subsequent module operations should not overwrite user modifications.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The code generator MUST produce a `*RepositoryCodeNode.kt` file implementing `CodeNodeDefinition` for the repository processor node, with typed input ports (IP type), typed output ports (String for result/error), and entity conversion logic.
- **FR-002**: The code generator MUST produce a `*CUDCodeNode.kt` file implementing `CodeNodeDefinition` for the CUD source node, replacing the legacy `createXxxCUD()` factory function.
- **FR-003**: The code generator MUST produce a `*DisplayCodeNode.kt` file implementing `CodeNodeDefinition` for the display sink node, replacing the legacy `createXxxDisplay()` factory function.
- **FR-004**: The EntityFlowGraphBuilder MUST set `_codeNodeClass` and `_codeNodeDefinition` configuration on all three nodes in the generated flow graph.
- **FR-005**: The RuntimeFlowGenerator MUST NOT generate tick function imports or references for nodes that have `_codeNodeClass` configuration.
- **FR-006**: Generated node files MUST use the module's IP type data class (from `iptypes/`) for port type declarations, not `Any::class`.
- **FR-007**: Generated node files MUST be placed in the module's `nodes/` subdirectory following the established convention.
- **FR-008**: The generated `.flow.kt` file MUST reference concrete IP types in port declarations (not `Any::class`).
- **FR-009**: Existing hand-written modules (UserProfiles, GeoLocations, Addresses) MUST continue to compile and run without modification after the generator changes.
- **FR-010**: The "Remove Repository Module" flow MUST clean up generated node definition files from the `nodes/` subdirectory.

### Key Entities

- **CodeNodeDefinition**: The self-contained node interface that bundles metadata (name, category, ports) and processing logic (createRuntime). All generated nodes implement this interface.
- **Entity Module Node Triad**: The three nodes that make up every entity module — CUD source (emits save/update/remove), Repository processor (performs CRUD via DAO), Display sink (shows result/error).
- **IP Type Data Class**: The typed data class in `iptypes/` that flows through the pipeline. Generated nodes use this instead of `Any` for compile-time type safety.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can create a new repository module via the graphEditor and compile it successfully on the first attempt with zero manual code fixes.
- **SC-002**: 100% of generated node files implement CodeNodeDefinition — no legacy factory functions or tick function patterns in newly generated modules.
- **SC-003**: The generated module's runtime pipeline executes successfully in the graphEditor's preview panel, with CRUD operations persisting to the database.
- **SC-004**: All existing modules (UserProfiles, GeoLocations, Addresses, StopWatch, EdgeArtFilter, WeatherForecast) continue to compile and run after the generator changes with zero regressions.
- **SC-005**: The generated flow graph's textual view shows concrete IP type names on all entity ports (not `Any`).

## Assumptions

- The generated node files follow the same directory convention as existing modules: `{Module}/src/commonMain/kotlin/io/codenode/{module}/nodes/`.
- The generated RepositoryCodeNode uses the same `anyInput = true` pattern as existing repository nodes (fires on ANY input change, not ALL).
- The generated code uses the persistence module's entity classes (from `io.codenode.persistence`) with conversion extensions (`toEntity()`/`fromEntity()`) defined in the IP type file.
- The EntityModuleGenerator already generates the IP type data class with conversion extensions (implemented in feature 059). This feature builds on that.
- The `preview-api` dependency and PreviewProvider generation (from feature 060) are in place and compatible with this change.
