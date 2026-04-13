# Research: Fix Node and Graph Positioning Errors

**Feature**: 071-fix-node-positioning
**Date**: 2026-04-13

## R1: Root Cause — Negative Position Crash

**Decision**: Remove the non-negative `require` constraints from `Node.Position`.

**Rationale**: `Node.Position` has `require(x >= 0.0)` / `require(y >= 0.0)` in its `init` block (Node.kt:69). The graph canvas is an infinite plane with no visible origin — users can pan freely in any direction and place nodes anywhere. The constraint is the actual bug: it rejects valid positions that arise naturally from drag calculations. `updateNodePosition()` at GraphState.kt:236 constructs `Node.Position(newX, newY)` — when a node is dragged left/up past the invisible origin, the `require` throws `IllegalArgumentException`.

**Fix location**: `Node.Position` init block — remove both `require(x >= 0.0)` and `require(y >= 0.0)`. The `ORIGIN` constant at `(0.0, 0.0)` remains as a convenience default, not a minimum.

**Alternatives Considered**:
- Clamping in `updateNodePosition()`: Rejected — creates an invisible wall at (0, 0) that users can't see or understand. The UI has no visible origin indicator.
- Clamping in FlowGraphCanvas: Rejected — same invisible-wall problem, plus only fixes one call site.

## R2: Root Cause — Position Drift on Hierarchy Navigation

**Decision**: Store per-level view state (panOffset + scale) in the navigation stack, restore on navigate-out.

**Rationale**: `navigateIntoGraphNode()` (GraphState.kt:1557) recalculates panOffset to center child nodes but does NOT save the current panOffset/scale before overwriting them. `navigateOut()` (GraphState.kt:1601) pops the navigation path but does NOT restore panOffset/scale. Result: after navigating back, the view uses the panOffset from the child level, causing visible drift.

**Fix location**:
1. Extend `NavigationContext` (or its push/pop methods) to carry `ViewState(panOffset, scale)` per level.
2. In `navigateIntoGraphNode()`: save current `(panOffset, scale)` before overwriting.
3. In `navigateOut()` / `navigateToRoot()` / `navigateToDepth()`: restore saved `(panOffset, scale)`.

**Alternatives Considered**:
- Storing view state in a separate map keyed by depth: Rejected — NavigationContext already manages the depth stack, adding state there is simpler and keeps related data together.
- Recalculating the parent view from scratch: Rejected — doesn't restore user's custom pan/zoom; would feel jarring.

## R3: Secondary Bug — GraphNode Old Position Tracking

**Decision**: Fix old position lookup in Main.kt to handle GraphNode, not just CodeNode.

**Rationale**: Main.kt:966-970 gets the old position only for `CodeNode`, using `Offset.Zero` for everything else. This means undo on a GraphNode move restores to (0, 0) instead of the original position.

**Fix**: The position property is on `Node` (the base), not just `CodeNode`. The `when` should access `node.position` directly since all Node types have it.

## R4: Approach — Correct Model + Defense in Depth

**Decision**: Fix at multiple layers: (1) correct the model to allow signed coordinates (`Node.Position`), (2) fix view state save/restore in navigation, (3) fix old position tracking.

**Rationale**: Removing the constraint eliminates the crash immediately (US1). The navigation fix prevents the drift that confuses users (US2). The old position fix prevents undo corruption (edge case). Each fix is independently valuable.
