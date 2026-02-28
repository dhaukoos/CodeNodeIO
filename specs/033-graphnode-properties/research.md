# Research: View GraphNode Properties

**Feature**: 033-graphnode-properties
**Date**: 2026-02-27

## Decision 1: Why GraphNodes Are Currently Excluded from the Properties Panel

**Decision**: The Properties Panel excludes GraphNodes because of explicit `as? CodeNode` casts at three levels:
1. `Main.kt:951` — `graphState.flowGraph.findNode(nodeId) as? CodeNode` returns `null` for GraphNodes
2. `GraphState.updateNodeName()` — casts to `CodeNode` and returns early if not
3. `PropertiesPanelViewModel.selectNode()` — parameter typed as `CodeNode?`

**Rationale**: The Properties Panel was originally built for CodeNode configuration only. GraphNode support was never added.

**Fix approach**: Add a new `GraphNodePropertiesPanel` composable (following the existing `ConnectionPropertiesPanel` and `IPTypePropertiesPanel` pattern), add a `selectedGraphNode` parameter to `CompactPropertiesPanelWithViewModel`, and update the selection derivation in `Main.kt` to pass both CodeNode and GraphNode separately.

## Decision 2: GraphNode Name Editing

**Decision**: Generalize `GraphState.updateNodeName()` to handle both CodeNode and GraphNode.

**Rationale**: Both types are data classes extending `Node` and both support `.copy(name = newName)`. The current method only casts to `CodeNode`, but the same pattern works for `GraphNode.copy()`. A `when` block on the `Node` type allows calling the correct `.copy()` variant.

**Alternatives considered**:
- Adding a `withName()` method to the `Node` sealed class — rejected because it would change the shared model layer for a UI-only concern.
- Overloading `updateNodeName` for GraphNode — rejected because it would duplicate logic. A single method handling both types via `when` is cleaner.

## Decision 3: UI Approach — SharedNodeProperties Composable

**Decision**: Extract the name TextField and port sections into a `SharedNodeProperties` composable. Both `CodeNodePropertiesPanel` (refactored from existing `PropertiesContent`) and a new `GraphNodePropertiesPanel` use `SharedNodeProperties` as their uppermost element, then append their type-specific content below.

**Rationale**: Name editing and port display are identical for both node types (both inherit `name`, `inputPorts`, `outputPorts` from `Node`). Extracting into a shared composable eliminates duplication and ensures consistent behavior. Each panel then adds its unique sections: CodeNode adds generic type config and configuration properties; GraphNode adds the child nodes list.

**Alternatives considered**:
- Fully separate composables with duplicated name/port code — rejected because it violates DRY and risks divergent behavior between node type panels.
- Extending `PropertiesPanelState` to handle both types — rejected because `PropertiesPanelState` has CodeNode-specific fields (`isGenericNode`, `codeNodeType` checks).

## Decision 4: Port Display via SharedNodeProperties

**Decision**: The `SharedNodeProperties` composable accepts `Node`-level parameters (`name`, `inputPorts`, `outputPorts`) and renders the name TextField and port sections using the existing `PortEditorRow` composable.

**Rationale**: Both `CodeNode` and `GraphNode` inherit `inputPorts` and `outputPorts` as `List<Port<*>>` from `Node`. The existing `PortEditorRow` works for both. By placing `SharedNodeProperties` in `PropertiesPanel.kt` it can access the private `PortEditorRow` helper.

**SharedNodeProperties parameters**:
- `nodeName: String` — current name value
- `onNameChange: (String) -> Unit` — name edit callback
- `inputPorts: List<Port<*>>` — ports to display
- `outputPorts: List<Port<*>>` — ports to display
- `onPortNameChange: (String, String) -> Unit`
- `onPortTypeChange: (String, String) -> Unit`
- `ipTypeRegistry: IPTypeRegistry?`
- `portIPTypeNames: Map<String, String>`

## Decision 5: ViewModel Integration

**Decision**: Keep the existing `PropertiesPanelViewModel` focused on CodeNode. The GraphNode panel will handle name editing via a direct callback (same pattern as `ConnectionPropertiesPanel` which receives callbacks, not a ViewModel).

**Rationale**: The ViewModel has CodeNode-specific state (`isGenericNode`, property change tracking, etc.). GraphNode name editing is a simple operation that doesn't need the full ViewModel machinery. The callback approach is consistent with how the connection panel works.

## Key Files to Modify

| File | Change |
|------|--------|
| `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt` | Add `GraphNodePropertiesPanel` composable; add `selectedGraphNode` param to `CompactPropertiesPanelWithViewModel` |
| `graphEditor/src/jvmMain/kotlin/Main.kt` | Derive `selectedGraphNode` separately from `selectedNode`; pass to panel; generalize name update callback |
| `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` | Generalize `updateNodeName()` to handle both CodeNode and GraphNode |
