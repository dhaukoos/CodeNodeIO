# Feature Specification: Filesystem-Driven Node Palette

**Feature Branch**: `055-filesystem-node-palette`
**Created**: 2026-03-19
**Status**: Draft
**Input**: User description: "Restructure the Node Palette to align with feature 050, where all CodeNodes are stored as self-contained files located in Module, Project, or Universal directories. Use the Node Generator categories for palette categorization. Templates only come into use via the Node Generator; the Node Palette only lists self-contained CodeNodes. The list corresponds directly to nodes on the filesystem, updated on every graphEditor launch."

## Clarifications

### Session 2026-03-19

- Q: Should this feature unify the three separate category systems (CodeNodeType 11 values, NodeCategory 4 values, NodeTypeDefinition.NodeCategory 7 values), or only change palette grouping? → A: Use CodeNodeType from the fbpDsl CodeNode definition as the single category system for both the Node Palette and the Node Generator. Shorten the list to 9 values: SOURCE, SINK, TRANSFORMER, FILTER, SPLITTER, MERGER, VALIDATOR, API_ENDPOINT, DATABASE (dropping CUSTOM and GENERIC). Remove the separate NodeCategory (4 values) and NodeTypeDefinition.NodeCategory (7 values) enums.
- Q: Should the palette show all 9 category headers even when empty, or only categories with discovered nodes? → A: The Node Palette shows only categories that contain at least one discovered node (compact). The Node Generator always shows all 9 CodeNodeType options in its dropdown.

## User Scenarios & Testing

### User Story 1 - Browse Discovered CodeNodes by CodeNodeType (Priority: P1)

A developer opens the graphEditor and sees all self-contained CodeNodes grouped by their CodeNodeType (Source, Sink, Transformer, Filter, Splitter, Merger, Validator, Api Endpoint, Database) in the Node Palette. Each node displayed in the palette corresponds to an actual CodeNode file on the filesystem — there are no hardcoded sample nodes or template entries. The developer can expand and collapse category sections to find the node they need.

**Why this priority**: The core value of this feature is replacing the palette's content with filesystem-discovered CodeNodes organized by CodeNodeType. Without this, the palette shows stale or incorrect entries.

**Independent Test**: Can be verified by placing CodeNode files in the Module, Project, or Universal directories, launching the graphEditor, and confirming the palette displays exactly those nodes grouped by their CodeNodeType.

**Acceptance Scenarios**:

1. **Given** CodeNode files exist in Module, Project, and Universal directories, **When** the graphEditor launches, **Then** the Node Palette displays all discovered CodeNodes grouped by their CodeNodeType.
2. **Given** a CodeNode file has CodeNodeType SOURCE, **When** the palette loads, **Then** that node appears under the "Source" category section.
3. **Given** no CodeNode files exist in any of the three directories, **When** the graphEditor launches, **Then** the Node Palette displays no category sections (empty palette).
4. **Given** CodeNode files exist only with types SOURCE and SINK, **When** the palette loads, **Then** only the "Source" and "Sink" category sections are shown; the other 7 categories are hidden.
5. **Given** the palette is populated, **When** the developer clicks a category header, **Then** that category section expands or collapses to show or hide its nodes.

---

### User Story 2 - Search and Filter Palette Nodes (Priority: P2)

A developer types a search term into the palette's search box to filter the displayed nodes. The filter matches against node names and descriptions across all categories, showing only matching nodes while preserving the category grouping.

**Why this priority**: Search is essential for productivity when the number of CodeNodes grows, but the palette must first display the correct nodes (US1) before search has value.

**Independent Test**: Can be verified by populating the palette with multiple nodes, typing a partial name in the search box, and confirming only matching nodes appear.

**Acceptance Scenarios**:

1. **Given** the palette contains nodes across multiple categories, **When** the developer types a search term, **Then** only nodes whose name or description matches the term are displayed.
2. **Given** a search term is active, **When** the developer clears the search box, **Then** all nodes reappear in their categories.
3. **Given** a search term matches nodes in only one category, **When** filtering is applied, **Then** only that category section is shown with its matching nodes.

---

### User Story 3 - Filesystem Removal Reflected in Palette (Priority: P3)

When a developer removes a CodeNode file from the filesystem, that node no longer appears in the Node Palette on the next graphEditor launch. The palette is a direct reflection of what exists on disk — no ghost entries, no manual cleanup required.

**Why this priority**: Ensures data integrity between the filesystem and the palette, but only matters after the discovery mechanism (US1) is working correctly.

**Independent Test**: Can be verified by removing a CodeNode file from one of the three directories, restarting the graphEditor, and confirming the node is no longer in the palette.

**Acceptance Scenarios**:

1. **Given** a CodeNode file previously appeared in the palette, **When** that file is deleted from the filesystem and the graphEditor is relaunched, **Then** the node no longer appears in the palette.
2. **Given** a CodeNode file is moved from Project to Module directory, **When** the graphEditor is relaunched, **Then** the node still appears in the palette (discovered from its new location).

---

### User Story 4 - Templates Excluded from Palette (Priority: P3)

The Node Palette no longer displays template entries or hardcoded sample node types. Templates are exclusively accessed through the Node Generator workflow. The palette is a pure reflection of self-contained CodeNode files on the filesystem.

**Why this priority**: Clarifies the separation of concerns between the Node Generator (creates nodes from templates) and the Node Palette (browses existing nodes). Depends on US1 being complete.

**Independent Test**: Can be verified by launching the graphEditor and confirming no template or sample node entries appear in the palette — only nodes backed by actual CodeNode files on disk.

**Acceptance Scenarios**:

1. **Given** the graphEditor launches, **When** the Node Palette loads, **Then** no hardcoded sample nodes or template-based entries appear.
2. **Given** a template exists in the Node Generator, **When** viewing the Node Palette, **Then** that template is not listed unless a CodeNode file was generated from it and exists on disk.

---

### User Story 5 - Node Generator Uses CodeNodeType Categories (Priority: P2)

The Node Generator's category dropdown uses the same 9 CodeNodeType values (Source, Sink, Transformer, Filter, Splitter, Merger, Validator, Api Endpoint, Database) instead of the previous 4-value NodeCategory system. When a developer creates a new node, they select from these 9 categories, and the generated CodeNode file carries that CodeNodeType.

**Why this priority**: Unifying the category system across the Node Generator and Node Palette ensures consistency — a node created as "Filter" in the generator appears under "Filter" in the palette.

**Independent Test**: Can be verified by opening the Node Generator, confirming all 9 CodeNodeType options appear in the category dropdown, generating a node with a specific type, and seeing it appear under the correct palette category.

**Acceptance Scenarios**:

1. **Given** the Node Generator is open, **When** the developer clicks the category dropdown, **Then** all 9 CodeNodeType values are listed as options.
2. **Given** a developer selects "Filter" as the category and generates a node, **When** viewing the Node Palette, **Then** the new node appears under the "Filter" category section.

---

### Edge Cases

- What happens when a CodeNode file exists but has compilation errors or malformed content? The node is excluded from the palette with no user-facing error (silent skip).
- What happens when two CodeNode files in different locations define nodes with the same name? Both are listed; the palette does not deduplicate by name.
- What happens when the Universal directory (~/.codenode/nodes/) does not exist? The palette simply shows no nodes from that location; no error is raised.
- What happens when a directory scan encounters a non-CodeNode file? It is silently ignored.
- What happens when an existing CodeNode file uses CUSTOM or GENERIC as its CodeNodeType? It is treated as a legacy node; during this feature, existing nodes with CUSTOM or GENERIC should be migrated to the most appropriate of the 9 supported types.

## Requirements

### Functional Requirements

- **FR-001**: The Node Palette MUST display only self-contained CodeNode files discovered from the filesystem (Module, Project, and Universal directories).
- **FR-002**: The Node Palette MUST group discovered CodeNodes using CodeNodeType categories and MUST only display category sections that contain at least one discovered node.
- **FR-003**: The Node Palette MUST scan all three filesystem locations (Module, Project, Universal) on every graphEditor launch to build its node list.
- **FR-004**: The Node Palette MUST NOT display hardcoded sample nodes or template-based entries.
- **FR-005**: The Node Palette MUST support text-based search filtering across node names and descriptions.
- **FR-006**: The Node Palette MUST support expanding and collapsing category sections.
- **FR-007**: Removing a CodeNode file from the filesystem MUST result in its absence from the palette on the next graphEditor launch.
- **FR-008**: Malformed or unreadable CodeNode files MUST be silently excluded from the palette without causing errors.
- **FR-009**: When a new CodeNode is generated via the Node Generator during a session, it MUST appear in the palette immediately without requiring a restart.
- **FR-010**: The Node Generator MUST use the same 9 CodeNodeType values for its category dropdown and MUST always display all 9 options regardless of which types currently have existing nodes.
- **FR-011**: The separate NodeCategory (4-value) and NodeTypeDefinition.NodeCategory (7-value) enums MUST be removed; CodeNodeType is the single category system.
- **FR-012**: The CUSTOM and GENERIC values MUST be removed from CodeNodeType, leaving 9 supported values.

### Key Entities

- **CodeNode**: A self-contained file on the filesystem that implements the CodeNodeDefinition interface, representing a reusable processing component with defined inputs, outputs, and CodeNodeType.
- **CodeNodeType**: One of 9 functional classifications (Source, Sink, Transformer, Filter, Splitter, Merger, Validator, Api Endpoint, Database) that determines how a CodeNode is categorized in both the palette and the generator.
- **Filesystem Location**: One of three directories (Module-level, Project-level, Universal) where CodeNode files are discovered.

## Success Criteria

### Measurable Outcomes

- **SC-001**: 100% of CodeNode files present in the three filesystem directories appear in the palette on launch.
- **SC-002**: 0 hardcoded or template-based entries appear in the palette after the feature is complete.
- **SC-003**: Palette load time on launch remains under 2 seconds with up to 100 CodeNode files across all directories.
- **SC-004**: Removing a CodeNode file and relaunching the graphEditor results in its removal from the palette within one launch cycle.
- **SC-005**: All 9 CodeNodeType categories are represented as palette category sections and generator dropdown options.
- **SC-006**: The codebase contains exactly one category enum (CodeNodeType with 9 values); the former NodeCategory and NodeTypeDefinition.NodeCategory enums are fully removed.

## Assumptions

- The three filesystem locations (Module, Project, Universal) are already defined and discoverable per feature 050.
- The NodeDefinitionRegistry already supports scanning these directories and returning discovered CodeNodeDefinitions.
- Category information is embedded in each CodeNode file's CodeNodeType and can be determined during discovery.
- Existing .flow.kts files that reference CUSTOM or GENERIC CodeNodeType values will be migrated to appropriate types.

## Dependencies

- **Feature 050** (Self-Contained CodeNode): Provides the filesystem-based node storage and discovery mechanism that this feature relies on.

## Out of Scope

- Adding new filesystem locations beyond Module, Project, and Universal.
- Real-time filesystem watching (hot-reload); palette updates occur on launch and after node generation only.
- Node deletion from within the palette UI (users manage files directly on the filesystem).
