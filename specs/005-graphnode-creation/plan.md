# Implementation Plan: GraphNode Creation Support

**Branch**: `005-graphnode-creation` | **Date**: 2026-02-02 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-graphnode-creation/spec.md`

## Summary

This feature adds hierarchical node grouping capabilities to the graphEditor. Users can multi-select nodes using Shift-click or rectangular selection, group them into GraphNodes via context menu, and navigate into/out of GraphNodes to view and edit their internal structure. The existing GraphNode model in fbpDsl already supports child nodes, internal connections, and port mappings - this feature exposes that capability through the UI.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP), Compose Desktop 1.7.3
**Primary Dependencies**: Compose Desktop (UI), kotlinx-coroutines, kotlinx-serialization
**Storage**: .flow.kts files (DSL serialization format)
**Testing**: JUnit5, Compose UI testing
**Target Platform**: JVM Desktop (Windows, macOS, Linux)
**Project Type**: Multi-module KMP (fbpDsl, graphEditor, idePlugin)
**Performance Goals**: Selection of 10+ nodes in <100ms, GraphNode creation in <500ms
**Constraints**: Canvas rendering at 60fps with 100+ nodes, <200MB memory
**Scale/Scope**: Graphs with up to 100 nodes, 5 levels of nesting

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Licensing | PASS | No new dependencies; extending existing Compose Desktop/KMP infrastructure |
| TDD Required | PASS | All user stories will have tests written first per constitution |
| Code Quality | PASS | Building on existing patterns (GraphState, NodeRenderer, context menus) |
| Performance Requirements | PASS | Using existing canvas rendering; selection operations are O(n) |
| Observability | PASS | Extending existing GraphState error handling and dirty tracking |

## Project Structure

### Documentation (this feature)

```text
specs/005-graphnode-creation/
├── plan.md              # This file
├── research.md          # Phase 0 output - existing infrastructure analysis
├── data-model.md        # Phase 1 output - Selection and NavigationContext models
├── quickstart.md        # Phase 1 output - developer walkthrough
├── contracts/           # Phase 1 output - UI component contracts
│   ├── selection-state-contract.md
│   ├── group-context-menu-contract.md
│   └── navigation-context-contract.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
fbpDsl/
├── src/commonMain/kotlin/io/codenode/fbpdsl/
│   ├── model/
│   │   ├── GraphNode.kt         # EXISTING - already has childNodes, portMappings
│   │   ├── Node.kt              # EXISTING - base class with position
│   │   └── Connection.kt        # EXISTING - has parentScopeId for internal connections
│   └── factory/
│       └── GraphNodeFactory.kt  # NEW - create GraphNode from selection

graphEditor/
├── src/jvmMain/kotlin/io/codenode/grapheditor/
│   ├── state/
│   │   ├── GraphState.kt        # MODIFY - add multi-selection, navigation context
│   │   ├── SelectionState.kt    # NEW - multi-selection management
│   │   └── NavigationContext.kt # NEW - hierarchical navigation stack
│   ├── ui/
│   │   ├── FlowGraphCanvas.kt   # MODIFY - rectangular selection, selection rendering
│   │   ├── GroupContextMenu.kt  # NEW - Group/Ungroup menu
│   │   ├── SelectionBox.kt      # NEW - dotted rectangle during selection
│   │   └── GraphNodeRenderer.kt # NEW - distinct GraphNode visual
│   ├── rendering/
│   │   └── NodeRenderer.kt      # MODIFY - GraphNode-specific rendering
│   └── serialization/
│       ├── FlowGraphSerializer.kt   # MODIFY - complete GraphNode serialization
│       └── FlowGraphDeserializer.kt # MODIFY - complete GraphNode deserialization
└── src/jvmTest/kotlin/io/codenode/grapheditor/
    ├── state/
    │   ├── SelectionStateTest.kt    # NEW
    │   └── NavigationContextTest.kt # NEW
    ├── ui/
    │   ├── MultiSelectionTest.kt    # NEW
    │   ├── RectangularSelectionTest.kt # NEW
    │   └── GroupContextMenuTest.kt  # NEW
    └── serialization/
        └── GraphNodeSerializationTest.kt # NEW
```

**Structure Decision**: Multi-module KMP project structure preserved. New files follow existing package conventions. All changes are in fbpDsl and graphEditor modules.

## Key Design Decisions

### 1. Multi-Selection State

The existing `GraphState.selectedNodeId: String?` will be augmented with `selectedNodeIds: Set<String>` to support multi-selection. The single selection remains for backward compatibility.

### 2. Navigation Context as Stack

Navigation context will be modeled as a stack of GraphNode IDs, allowing arbitrary nesting depth. The top of the stack is the current view context.

```kotlin
data class NavigationContext(
    val path: List<String> = emptyList()  // Empty = root FlowGraph
) {
    val currentGraphNodeId: String? get() = path.lastOrNull()
    val isAtRoot: Boolean get() = path.isEmpty()
    fun pushInto(graphNodeId: String): NavigationContext
    fun popOut(): NavigationContext
}
```

### 3. Port Mapping Generation

When grouping nodes, ports are automatically generated based on external connections:
- For each input port on an internal node that has a connection from an external node -> create GraphNode input port with port mapping
- For each output port on an internal node that has a connection to an external node -> create GraphNode output port with port mapping

### 4. GraphNode Visual Distinction

GraphNodes will be rendered with:
- Rounded rectangle with double border (inner + outer)
- Gradient fill (light blue to white)
- Small "expand" icon button in top-right corner
- Port count badge showing "N nodes inside"

### 5. Existing Infrastructure Reuse

| Component | Reuse Strategy |
|-----------|----------------|
| GraphNode model | Already fully implemented with childNodes, portMappings, validation |
| ConnectionContextMenu | Pattern reused for GroupContextMenu |
| GraphState | Extended with multi-selection and navigation |
| NodeRenderer | Extended with GraphNode-specific branch |
| FlowGraphSerializer | Complete partial GraphNode implementation |

## Complexity Tracking

> No constitution violations requiring justification. Feature builds on existing patterns.

## Dependencies

### Upstream Dependencies (must be complete)
- Feature 001: IDE Plugin Platform (graphEditor exists) - Complete
- Feature 002: Generic NodeType (NodeTypeDefinition with GENERIC category) - Complete
- Feature 003: IP Palette Support (context menu infrastructure) - Complete

### Downstream Impact
- Future code generation features must handle GraphNode hierarchy
- IDE plugin may need navigation breadcrumb UI

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Performance with large graphs | Medium | High | Optimize selection rendering; use spatial indexing if needed |
| Complex nested serialization | Low | Medium | Existing GraphNode.childNodes structure supports it |
| Canvas hit detection accuracy | Low | Low | Existing findNodeAtPosition() proven; extend for selection box |
| Undo/redo complexity | Medium | Medium | Use command pattern already in UndoRedoManager |
