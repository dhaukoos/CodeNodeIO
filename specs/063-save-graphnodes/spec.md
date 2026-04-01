# Feature Specification: Save GraphNodes

**Feature Branch**: `063-save-graphnodes`
**Created**: 2026-04-01
**Status**: Draft
**Input**: User description: "Save GraphNodes — Define a way to save a graphNode to the Node Palette. Add a button to the GraphNode Properties panel to Add/Remove GraphNode. Include a level indicator that follows the established pattern of Module/Project/Universal levels. (If a GraphNode has child nodes at different (heterogeneous) levels, what possible complications might that introduce, and what might resolve them?) Add a new dropdown organizer in the Node Palette for GraphNodes. Use a visually-distinct card design for GraphNodes in the Node Palette."

## User Scenarios & Testing

### User Story 1 - Save a GraphNode to the Palette (Priority: P1)

A user has composed a GraphNode on the canvas containing several wired child nodes. They want to reuse this composition in other flows. They select the GraphNode, open the Properties panel, choose a placement level, and click "Add to Palette." The GraphNode appears in the Node Palette under a dedicated "GraphNodes" section and can be dragged onto any canvas to create a new instance of that composition.

**Why this priority**: This is the core value proposition — enabling reuse of GraphNode compositions. Without save, there is no feature.

**Independent Test**: Select a GraphNode on canvas, click "Add to Palette" in Properties panel, verify it appears in the Node Palette's GraphNodes section with the correct name and port information. Drag it onto the canvas and verify a new independent copy is created with the same child node structure and internal wiring.

**Acceptance Scenarios**:

1. **Given** a GraphNode is selected on the canvas, **When** the user opens the Properties panel, **Then** an "Add to Palette" button is visible along with a level selector (Module/Project/Universal).
2. **Given** the user clicks "Add to Palette" with level "Project," **When** the save completes, **Then** the GraphNode appears in the Node Palette's "GraphNodes" dropdown section at the Project level.
3. **Given** a saved GraphNode exists in the palette, **When** the user drags it onto the canvas, **Then** a new independent copy is created with the same child nodes, internal connections, and port mappings as the original.
4. **Given** a saved GraphNode exists in the palette, **When** the graphEditor is restarted, **Then** the saved GraphNode still appears in the palette at the same level.

---

### User Story 2 - Remove a Saved GraphNode from the Palette (Priority: P2)

A user no longer needs a previously saved GraphNode in the palette. They select the saved GraphNode on the canvas (or its entry in the palette) and remove it. The GraphNode disappears from the palette. Existing instances already placed on canvases are unaffected.

**Why this priority**: Users need the ability to manage their saved GraphNodes — removal is the natural complement to saving and prevents palette clutter.

**Independent Test**: Save a GraphNode to the palette, then remove it via the Properties panel. Verify it no longer appears in the palette. Verify any existing canvas instances of that GraphNode remain intact and functional.

**Acceptance Scenarios**:

1. **Given** a GraphNode that was previously saved to the palette is selected on the canvas, **When** the user opens the Properties panel, **Then** a "Remove from Palette" button is shown (replacing "Add to Palette").
2. **Given** the user clicks "Remove from Palette," **When** a confirmation prompt is accepted, **Then** the GraphNode is removed from the palette and its persisted definition is deleted.
3. **Given** a GraphNode was removed from the palette, **When** the user inspects canvas instances that were created from it, **Then** those instances continue to function normally — removal only affects the palette entry.

---

### User Story 3 - Browse and Organize GraphNodes in the Palette (Priority: P3)

A user has saved several GraphNodes and wants to find and use them efficiently. The Node Palette includes a dedicated "GraphNodes" dropdown category that lists all saved GraphNodes. GraphNode cards use a visually-distinct design so they are easily distinguishable from CodeNode cards.

**Why this priority**: As the number of saved GraphNodes grows, discoverability and visual clarity become important. This story ensures the palette remains organized and usable.

**Independent Test**: Save multiple GraphNodes, expand the "GraphNodes" section in the palette, verify all are listed. Verify GraphNode cards are visually distinct from CodeNode cards (different styling). Use the search box to filter by name and confirm GraphNode results appear.

**Acceptance Scenarios**:

1. **Given** one or more GraphNodes have been saved, **When** the user expands the "GraphNodes" dropdown in the Node Palette, **Then** all saved GraphNodes are listed with their names and port information.
2. **Given** the Node Palette displays saved GraphNodes, **When** the user compares GraphNode cards to CodeNode cards, **Then** GraphNode cards have a visually-distinct design (different color scheme, border style, or composition indicator) that clearly identifies them as compositions.
3. **Given** saved GraphNodes exist in the palette, **When** the user types a search query in the palette search box, **Then** matching GraphNode names appear in the filtered results alongside matching CodeNodes.
4. **Given** saved GraphNodes exist at different levels (Module, Project, Universal), **When** the user views the GraphNodes section, **Then** each card displays a level indicator showing where the GraphNode is stored.

---

### User Story 4 - Handle Heterogeneous Child Node Levels (Priority: P2)

A GraphNode may contain child nodes that originate from different placement levels. For example, a GraphNode might contain a Module-level CodeNode (available only in the current module) and a Universal-level CodeNode. When the user saves this GraphNode at a particular level, the system must ensure all child node dependencies are satisfiable at the target level.

**Why this priority**: This is a data integrity concern. A GraphNode saved at the Universal level that references Module-level child nodes would be broken when used outside that module. Addressing this prevents users from creating unusable palette entries.

**Independent Test**: Create a GraphNode with child nodes at mixed levels (e.g., one Module-level and one Project-level). Attempt to save it at Universal level and verify the system displays a promotion dialog listing the affected child nodes, with options to cancel or continue.

**Acceptance Scenarios**:

1. **Given** a GraphNode contains only child nodes available at or above the target save level, **When** the user saves the GraphNode, **Then** the save succeeds without any promotion dialog.
2. **Given** a GraphNode contains a child node that is only available at a more specific level than the target save level (e.g., Module-level child, but saving at Project level), **When** the user clicks "Add to Palette," **Then** the system displays a dialog explaining that the affected child nodes will be promoted to the target level, listing each child node and its current level, with options to Cancel or Continue.
3. **Given** the promotion dialog is displayed, **When** the user clicks "Continue," **Then** the GraphNode is saved at the target level and the affected child nodes are promoted (their definitions are copied/moved to the target level so they are available wherever the GraphNode is used).
4. **Given** the promotion dialog is displayed, **When** the user clicks "Cancel," **Then** the save is aborted and no changes are made to any level.

---

### Edge Cases

- What happens when a user tries to save a GraphNode with the same name as an existing palette entry at the same level? The system prompts whether to overwrite or rename.
- What happens when a saved GraphNode's child node definitions change after saving (e.g., a CodeNode is updated)? The saved GraphNode references child node types by identity; instantiation uses the latest version of each child node type available at the target level.
- What happens when a user saves an empty GraphNode (no child nodes)? The system allows it — an empty GraphNode is a valid composition that the user may populate later.
- What happens when a GraphNode contains nested GraphNodes (GraphNodes within GraphNodes)? The save captures the full hierarchy. The same level compatibility and promotion rules apply recursively to all descendants — the promotion dialog lists all affected nodes across the entire hierarchy.
- What happens when the user tries to remove a GraphNode from the palette while instances exist on the canvas? Removal proceeds — existing canvas instances are independent copies and are not affected by palette removal.

## Requirements

### Functional Requirements

- **FR-001**: System MUST provide an "Add to Palette" action in the GraphNode Properties panel when a GraphNode is selected on the canvas.
- **FR-002**: System MUST provide a level selector (Module/Project/Universal) adjacent to the "Add to Palette" action, following the same three-tier pattern used by IP types and CodeNodes.
- **FR-003**: System MUST persist saved GraphNode definitions to the filesystem at the appropriate tier location so they survive application restarts.
- **FR-004**: System MUST display a "Remove from Palette" action in the Properties panel when the selected GraphNode matches an existing palette entry.
- **FR-005**: System MUST show a confirmation prompt before removing a GraphNode from the palette.
- **FR-006**: System MUST include a dedicated "GraphNodes" dropdown category in the Node Palette that lists all saved GraphNode definitions.
- **FR-007**: System MUST render GraphNode cards in the Node Palette with a visually-distinct design that differentiates them from CodeNode cards (e.g., distinct color scheme, border style, or composition indicator).
- **FR-008**: System MUST display a level indicator on each GraphNode palette card showing its placement tier (Module/Project/Universal).
- **FR-009**: System MUST include saved GraphNodes in the palette's search/filter results when the user types a query.
- **FR-010**: When a saved GraphNode is dragged from the palette onto the canvas, the system MUST create a fully independent copy with the same child node structure, internal connections, and port mappings.
- **FR-011**: System MUST validate child node level compatibility when saving a GraphNode. If any child nodes are only available at a more specific level than the target save level, the system MUST display a dialog explaining that those child nodes will be promoted to the target level, listing each affected node and its current level, with Cancel and Continue options.
- **FR-012**: When the user confirms promotion via the dialog, the system MUST promote (copy/move) the affected child node definitions to the target level so they are available wherever the saved GraphNode is used.
- **FR-013**: System MUST prompt for overwrite or rename when saving a GraphNode with the same name as an existing entry at the same level.
- **FR-014**: System MUST discover saved GraphNode definitions from all three tier locations on startup and merge them into the palette with the same precedence rules used for CodeNodes and IP types (Module > Project > Universal).

### Key Entities

- **Saved GraphNode Definition**: A reusable GraphNode template stored in the palette. Contains the GraphNode's name, description, exposed ports, child node structure, internal connections, port mappings, and placement level.
- **Placement Level**: The storage tier for the saved definition — Module (current module only), Project (shared across project), or Universal (shared across all projects in the user's environment).
- **Child Node Reference**: An entry within a saved GraphNode definition that identifies a child node by type/name. Resolved at instantiation time against the available nodes at the target context level.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can save a GraphNode to the palette and reuse it by dragging it onto the canvas in under 10 seconds per operation.
- **SC-002**: Saved GraphNode definitions persist across application restarts with 100% reliability — no data loss on normal shutdown.
- **SC-003**: Users can visually distinguish GraphNode palette cards from CodeNode palette cards at a glance without reading labels.
- **SC-004**: The system detects and presents the promotion dialog in 100% of cases where child nodes exist at more specific levels than the target save level — no silent promotions or broken references.
- **SC-005**: Instantiating a saved GraphNode from the palette produces a fully functional, independent copy with all child nodes and connections intact.
- **SC-006**: The Node Palette search returns matching GraphNodes alongside CodeNodes with no additional user action required.

## Assumptions

- The existing three-tier filesystem pattern (Module/Project/Universal) and PlacementLevel enum are the correct model for GraphNode storage — no new tier types are needed.
- GraphNode definitions are saved as self-contained snapshots that reference child node types by identity. When instantiated, child nodes are resolved against the current available node definitions, not frozen copies from save time. This means updates to child CodeNode definitions are automatically reflected in new instances.
- The Properties panel currently supports CodeNode selection; extending it to show save/remove actions for GraphNodes is a natural extension of the existing panel.
- A single "GraphNodes" category in the Node Palette is sufficient for organization. Sub-categorization within GraphNodes (by domain, purpose, etc.) is out of scope for this feature.
- Drag-and-drop instantiation follows the same mechanism already used for CodeNodes in the palette, extended to handle GraphNode composition creation.
