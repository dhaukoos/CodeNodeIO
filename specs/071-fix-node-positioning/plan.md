# Implementation Plan: Fix Node and Graph Positioning Errors

**Branch**: `071-fix-node-positioning` | **Date**: 2026-04-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/071-fix-node-positioning/spec.md`

## Summary

Fix two related bugs: (1) application crashes when node positions become negative because `Node.Position` incorrectly rejects negative coordinates on an infinite canvas, and (2) graph view state (pan offset and zoom) is not preserved when navigating in/out of graphNode hierarchy levels. Additionally fix a secondary bug where undo on GraphNode moves restores to (0, 0) instead of the original position. Approach: remove the model constraint, save/restore view state during navigation, and fix position tracking for undo.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Desktop 1.7.3
**Primary Dependencies**: fbpDsl (Node.Position model), graphEditor (GraphState, FlowGraphCanvas, UndoRedoManager, NavigationContext)
**Storage**: N/A — in-memory state only
**Testing**: kotlin.test (commonTest for Node.Position), JUnit5 (jvmTest for GraphState, characterization tests)
**Target Platform**: KMP Desktop (JVM)
**Project Type**: KMP multi-module (Gradle composite)
**Performance Goals**: N/A (bug fix, no performance changes)
**Constraints**: Must not break any existing tests; update existing tests that assert non-negative constraint
**Scale/Scope**: 4-5 files modified, ~40 lines changed

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Bug fix corrects model to match UI semantics |
| II. Test-Driven Development | PASS | TDD tests for position model and view state restoration |
| III. User Experience Consistency | PASS | Eliminates crash and position drift — directly improves UX |
| IV. Performance Requirements | N/A | No performance impact |
| V. Observability & Debugging | N/A | No logging changes needed |
| Licensing & IP | PASS | No new dependencies |

**Gate Result**: PASS — no violations.

**Post-Design Re-Check**: Same assessment. All changes are within existing files using existing dependencies.

## Project Structure

### Documentation (this feature)

```text
specs/071-fix-node-positioning/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0 output
├── quickstart.md        # Verification scenarios
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (files modified)

```text
fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Node.kt
    — Node.Position: remove require(x >= 0.0) and require(y >= 0.0) constraints

fbpDsl/src/commonTest/kotlin/…/model/NodePositionTest.kt (new or existing)
    — TDD tests confirming negative positions are accepted

graphEditor/src/jvmMain/kotlin/state/GraphState.kt
    — navigateIntoGraphNode(): save view state before overwriting
    — navigateOut(): restore view state
    — navigateToRoot(): restore root view state
    — navigateToDepth(): restore target-depth view state

graphEditor/src/jvmMain/kotlin/state/NavigationContext.kt
    — Extend to carry ViewState (panOffset, scale) per navigation level

graphEditor/src/jvmMain/kotlin/Main.kt
    — onNodeMoved callback: fix old position tracking for GraphNode

graphEditor/src/jvmTest/kotlin/state/GraphStateTest.kt (or new test file)
    — TDD tests for view state save/restore

graphEditor/src/jvmTest/kotlin/state/NavigationContextTest.kt (if exists, or new)
    — TDD tests for ViewState storage in navigation stack
```

**Structure Decision**: Pure bug fix — all changes within existing graphEditor and fbpDsl modules. No new modules or files needed beyond test files.

## Research Decisions

### R1: Negative Position Crash — Remove Non-Negative Constraint

Remove `require(x >= 0.0)` and `require(y >= 0.0)` from `Node.Position` init block. The canvas is an infinite signed plane with no visible origin — the constraint was incorrectly applied.

### R2: Position Drift — Save/Restore View State in Navigation Stack

Extend `NavigationContext` to store `ViewState(panOffset: Offset, scale: Float)` per navigation level. Save before navigating in, restore when navigating out.

### R3: GraphNode Old Position — Fix Position Lookup in Main.kt

Access `node.position` directly for all node types instead of only handling `CodeNode`.

### R4: Correct Model + Defense in Depth

Fix at three layers: correct model (R1), navigation state (R2), undo tracking (R3). Each fix is independently valuable.
