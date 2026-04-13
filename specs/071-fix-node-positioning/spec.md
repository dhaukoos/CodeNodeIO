# Feature Specification: Fix Node and Graph Positioning Errors

**Feature Branch**: `071-fix-node-positioning`
**Created**: 2026-04-13
**Status**: Draft
**Input**: Bug report: navigating into graphNode internals and back causes graph position drift; negative node coordinates crash the application.

## Clarifications

### Session 2026-04-13

- Q: Should the virtual coordinate space allow negative values, given that the UI has no visible origin and allows free panning? → A: Yes — remove the non-negative constraint from Node.Position. The coordinate space is an infinite signed plane with no privileged origin.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Allow Negative Node Positions (Priority: P1)

A user drags a node freely on the infinite canvas. The canvas has no visible origin and supports panning in all directions. If the calculated position becomes negative (due to drag direction, pan offset, zoom, or hierarchy navigation drift), the application crashes with an `IllegalArgumentException` because the model incorrectly rejects negative coordinates. The model should accept any signed coordinate value to match the infinite-canvas UI semantics.

**Why this priority**: A crash is the most severe user experience failure. No user workflow should cause the application to terminate unexpectedly.

**Independent Test**: Can be tested by dragging any node to any position on the canvas (including positions that yield negative coordinates) and verifying the application remains stable.

**Acceptance Scenarios**:

1. **Given** a flow graph with nodes, **When** a user drags a node to a position that results in negative x or y coordinates, **Then** the node position is accepted and no error occurs.
2. **Given** a flow graph with nodes, **When** the system internally calculates a negative position (e.g., from pan offset arithmetic after hierarchy navigation), **Then** the position is accepted without error.
3. **Given** a node at position (5, 10), **When** the user drags it left by 50 pixels (yielding a negative x), **Then** the node's x coordinate reflects the actual calculated value (e.g., -45).

---

### User Story 2 - Preserve Graph View State Across Hierarchy Navigation (Priority: P2)

A user navigates into a graphNode to view its internals, then navigates back to the parent level. After returning, the graph view (pan offset and zoom) should be restored to where it was before navigation, so that nodes appear in their expected positions on screen.

**Why this priority**: Position drift after hierarchy navigation is confusing and compounds over multiple navigations. Fixing the root cause prevents accumulated drift.

**Independent Test**: Can be tested by recording node screen positions before navigating into a graphNode, navigating back, and comparing positions. They should match within 1 pixel.

**Acceptance Scenarios**:

1. **Given** a user viewing the top-level flow graph with specific pan/zoom settings, **When** the user navigates into a graphNode's internals and then navigates back, **Then** the pan offset and zoom level are restored to their previous values.
2. **Given** a user who has navigated into and out of graphNode internals multiple times, **When** the user checks node positions on the parent graph, **Then** the positions have not drifted from their original locations.
3. **Given** a user viewing a deeply nested graphNode (2+ levels deep), **When** navigating back to the root level, **Then** the root-level view state is correctly restored with no accumulated drift.

---

### Edge Cases

- What happens when a node is at a negative position and the user drags it further into negative territory? The position should update normally with no constraints.
- What happens when zoom level is very small (e.g., 10%) and the user drags a node? Position calculations should work correctly with any signed value.
- What happens when undo is performed on a move? The undo should restore the exact pre-drag position.
- What happens when a flow graph is loaded from file with nodes at negative positions? The graph should display correctly without errors.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST NOT crash when a node position calculation yields negative x or y coordinates.
- **FR-002**: The position model MUST accept any signed (positive or negative) coordinate values, reflecting the infinite-canvas coordinate space.
- **FR-003**: The system MUST save and restore pan offset and zoom level when navigating between hierarchy levels in the graph.
- **FR-004**: The system MUST restore the saved view state when the user navigates back from a graphNode's internals to the parent level.
- **FR-005**: The system MUST handle multiple consecutive hierarchy navigations (in and out) without accumulating position drift.
- **FR-006**: The undo system MUST correctly restore the original pre-drag position for all node types, including GraphNodes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero application crashes occur when dragging nodes to any position on the canvas, including positions with negative coordinates.
- **SC-002**: After navigating into a graphNode and back, node positions on the parent graph match their pre-navigation positions within 1 pixel.
- **SC-003**: After 10 consecutive navigate-in / navigate-back cycles, node positions show zero accumulated drift.
- **SC-004**: All existing graph editor tests continue to pass after the fix.

## Assumptions

- The coordinate space is an infinite signed plane. Negative coordinates are valid for node placement.
- The `Node.Position` model constraint (`require(x >= 0)`) is the root cause of the crash and must be removed.
- Pan offset and zoom level are the two view state properties that need to be saved/restored during hierarchy navigation. Selection state does not need to be preserved.
