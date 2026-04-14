# Feature Specification: Architecture IP Types

**Feature Branch**: `073-architecture-ip-types`
**Created**: 2026-04-13
**Status**: Draft
**Input**: User description: "Define custom IP types for the graphEditor architecture.flow.kt connections to replace generic String types with domain-accurate types."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register Architecture IP Types (Priority: P1)

A graph designer opens the graph editor and navigates to the IP type palette. They see all architecture-specific IP types listed — NodeTypeDefinition, FlowGraph, ExecutionState, and others — each with a unique color and description. These types are available for use when defining ports on any flow graph, making the system's type vocabulary richer and more expressive.

**Why this priority**: Without the IP types registered, no ports or connections can reference them. This is the foundational step that enables all other changes.

**Independent Test**: After completing this story, the IP type palette lists all new architecture IP types with distinct names, descriptions, and colors. No changes to architecture.flow.kt are needed yet — the types simply exist and are available for use.

**Acceptance Scenarios**:

1. **Given** the graph editor is running, **When** a user opens the IP type palette, **Then** all 14 architecture IP types are listed: NodeDescriptors, IPTypeMetadata, FlowGraphModel, LoadedFlowGraph, GraphNodeTemplates, RuntimeExecutionState, DataFlowAnimations, DebugSnapshots, EditorGraphState, GeneratedOutput, GenerationContext, FilesystemPath, ClasspathEntry, IPTypeCommand
2. **Given** any two architecture IP types, **When** displayed on the canvas, **Then** they have visually distinct colors
3. **Given** an architecture IP type, **When** inspected in the palette, **Then** it shows a human-readable description of what data it represents

---

### User Story 2 - Update Architecture Flow Graph Ports (Priority: P2)

A graph designer opens architecture.flow.kt and all ports on all nodes reference the correct IP type instead of String. For example, the flowGraph-inspect node's "nodeDescriptors" output port is typed as NodeTypeDefinition, the flowGraph-types node's "ipTypeMetadata" output is typed as InformationPacketType, and so on. The serializedOutput port remains as String because it legitimately carries plain text (.flow.kt DSL source). Connections are now color-coded by their domain type, making the data flow between modules immediately understandable at a glance.

**Why this priority**: This is the core value of the feature — making the architecture diagram self-documenting by showing what data actually flows between modules.

**Independent Test**: Open architecture.flow.kt in the graph editor. Every port displays its domain-specific IP type name. Connections are color-coded by type. The serializedOutput connection remains typed as String. Save and re-open the file to confirm persistence.

**Acceptance Scenarios**:

1. **Given** architecture.flow.kt is loaded, **When** viewing the flowGraph-inspect node, **Then** the "nodeDescriptors" output port shows type "NodeTypeDefinition" (not "String")
2. **Given** architecture.flow.kt is loaded, **When** viewing all connections, **Then** every connection displays the correct IP type color matching its domain type
3. **Given** architecture.flow.kt is loaded, **When** viewing the flowGraph-persist node, **Then** the "serializedOutput" port remains typed as "String"
4. **Given** architecture.flow.kt with updated types, **When** saved and re-opened, **Then** all IP type assignments are preserved correctly
5. **Given** architecture.flow.kt is loaded, **When** viewing internal ports of the flowGraph-generate sub-graph, **Then** the "generationContext" port between GenerateContextAggregator and FlowGraphGenerate shows type "GenerationContext"

---

### Edge Cases

- What happens when an architecture IP type name collides with a user-defined IP type? Architecture types use descriptive domain names (e.g., "NodeTypeDefinition") which are unlikely to collide, but if they do, the first-registered type takes precedence per existing registry behavior.
- What happens if a user tries to connect ports with mismatched architecture IP types? The editor follows existing type-mismatch behavior — connections are allowed but display as "Any" type if the types don't match.
- What happens to other existing .flow.kt files that use String-typed ports? They are unaffected. Only architecture.flow.kt is updated. String remains a valid IP type.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST register 14 custom IP types representing the distinct data structures flowing through architecture.flow.kt connections
- **FR-002**: Each architecture IP type MUST have a unique identifier, a human-readable display name, a description of what data it represents, and a visually distinct color
- **FR-003**: The following IP types MUST be created as `typealias` declarations referencing actual domain classes:
  - **NodeDescriptors** — `typealias NodeDescriptors = List<NodeTypeDefinition>` — discovered node metadata from flowGraph-inspect
  - **IPTypeMetadata** — `typealias IPTypeMetadata = List<InformationPacketType>` — IP type registry data from flowGraph-types
  - **FlowGraphModel** — `typealias FlowGraphModel = FlowGraph` — the complete flow graph model
  - **LoadedFlowGraph** — `typealias LoadedFlowGraph = ParseResult` — result of parsing a .flow.kt file
  - **GraphNodeTemplates** — `typealias GraphNodeTemplates = List<GraphNodeTemplateMeta>` — saved GraphNode template metadata
  - **RuntimeExecutionState** — `typealias RuntimeExecutionState = ExecutionState` — runtime lifecycle state enum
  - **DataFlowAnimations** — `typealias DataFlowAnimations = List<ConnectionAnimation>` — active data-flow animation events
  - **DebugSnapshots** — debug data captured per-connection during runtime execution
  - **EditorGraphState** — `data class` describing interactive graph editing state (graph model, selection, pan, zoom, dirty flag). Uses `data class` instead of `typealias` because GraphState depends on Compose runtime and cannot be referenced from the `iptypes` module without creating a circular dependency.
  - **GeneratedOutput** — code generation results: generated files and status
  - **GenerationContext** — aggregated inputs for code generation: flow graph model combined with serialized output
  - **FilesystemPath** — a directory path used for scanning source files on disk
  - **ClasspathEntry** — a package prefix or classpath entry for discovering compiled nodes
  - **IPTypeCommand** — a command to mutate the IP type registry: register, unregister, generate, update
- **FR-003a**: The IP type discovery system MUST be extended to parse `typealias` declarations in IP type files, in addition to the existing `data class` parsing
- **FR-004**: The serializedOutput port MUST remain typed as String, since it carries plain-text .flow.kt DSL source code
- **FR-005**: All ports and connections in architecture.flow.kt MUST be updated to reference the appropriate architecture IP type instead of String, except serializedOutput
- **FR-006**: Architecture IP types MUST be discoverable through the standard IP type palette in the graph editor
- **FR-007**: Connections between ports of the same architecture IP type MUST display the color assigned to that type
- **FR-008**: This change MUST NOT alter graph editor runtime behavior — infrastructure changes to the IP type discovery system (parsing `typealias` declarations) and Gradle dependency additions to the `iptypes` module are in scope

### Key Entities

- **Architecture IP Type**: A custom IP type defined via `typealias` that references an actual domain class. Attributes: unique identifier, display name, description, assigned color, referenced domain type.
- **Port Type Assignment**: The association between a port on a node in architecture.flow.kt and its architecture IP type. Each port references exactly one IP type.
- **Typealias IP Type File**: A `.kt` file with `@IPType` metadata headers containing a `typealias` declaration instead of a `data class`. The discovery system resolves the referenced type for property introspection.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of domain-data ports in architecture.flow.kt reference a specific IP type — 0 generic String-typed ports remain on domain data connections (serializedOutput excluded)
- **SC-002**: All 14 architecture IP types are registered and visible in the IP type palette when the graph editor launches
- **SC-003**: A designer viewing architecture.flow.kt can identify the data type of every connection by its displayed type name and color, without needing to inspect source code
- **SC-004**: Opening, editing, saving, and re-opening architecture.flow.kt with the new types completes without errors and preserves all type assignments
- **SC-005**: No existing tests are broken by this change — all existing test suites pass without modification

## Clarifications

### Session 2026-04-14

- Q: Should each architecture IP type's data class include the actual properties of the domain object it represents? → A: Use `typealias` to reference the actual domain classes (e.g., `typealias NodeDescriptors = List<NodeTypeDefinition>`). This avoids property duplication, keeps a single source of truth, and makes types compilable and runtime-usable. The IP type discovery system must be extended to support `typealias` syntax in addition to `data class`.
- Q: Should FR-008 be relaxed to allow targeted infrastructure changes to the IP type discovery system? → A: Yes (Option A). Extend IPTypeDiscovery to parse `typealias`, add Gradle dependencies to the `iptypes` module. FR-008 updated to scope "no changes to graph editor runtime behavior" — infrastructure changes to discovery are in scope.
- Q: Should iptypes merge with flowGraph-types, and can GraphState move to eliminate the circular dependency? → A: Keep separate `iptypes` module (Option A). flowGraph-types is infrastructure (registry, discovery, generation); IP type definitions are data that belongs in application code — matching the demo project pattern. GraphState cannot move because it depends on Compose runtime, which would break KMP compatibility for all downstream consumers. EditorGraphState becomes a `data class` instead of a typealias to avoid the circular dependency.

## Assumptions

- Architecture IP types reference actual domain classes via `typealias` declarations — no property duplication. The real domain classes remain the single source of truth.
- The IP type discovery system must be extended to parse `typealias` declarations in addition to `data class` bodies. This is a targeted infrastructure change.
- Color assignment follows the existing color palette conventions used by other IP types in the system
- These IP types are discovered via the INTERNAL placement tier (lowest precedence), which scans the tool's own `iptypes` module. This is necessary because at runtime `projectRoot` points to the user's project, not the CodeNodeIO source tree. User-defined types at MODULE, PROJECT, or UNIVERSAL tiers take precedence over INTERNAL types.
- The `iptypes` module is a separate Gradle module (not merged into flowGraph-types), matching the demo project pattern. flowGraph-types is infrastructure; iptypes is data.
- The `iptypes` module depends on fbpDsl, flowGraph-persist, and flowGraph-execute for typealias resolution. It does NOT depend on graphEditor — EditorGraphState is a `data class` to avoid the circular dependency.
- GraphState remains in graphEditor because it depends on Compose runtime. EditorGraphState describes the concept without referencing the Compose-coupled class.
