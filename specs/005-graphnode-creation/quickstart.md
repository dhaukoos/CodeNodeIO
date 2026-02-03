# Quickstart: GraphNode Creation Support

**Feature**: 005-graphnode-creation
**Date**: 2026-02-02

## Developer Setup

### Prerequisites

1. CodeNodeIO repository cloned with features 001-003 complete
2. Kotlin 2.1.21 and JDK 17+
3. IntelliJ IDEA or Android Studio with Kotlin plugin

### Running the graphEditor

```bash
# From repository root
./gradlew :graphEditor:run
```

## Feature Walkthrough

### Scenario 1: Multi-Select Nodes with Shift-Click

**Steps**:
1. Open graphEditor with a flow graph containing 5+ nodes
2. Hold **Shift** and click on Node A
3. Continue holding **Shift** and click on Node B
4. Continue holding **Shift** and click on Node C

**Expected Result**:
- All three nodes show selection highlight (blue border)
- Connections between selected nodes are visually distinguished
- Status shows "3 nodes selected"

**Verify**:
- Shift-click on Node A again → Node A is deselected (2 selected)
- Click on empty canvas (no Shift) → All nodes deselected

---

### Scenario 2: Rectangular Selection

**Steps**:
1. Open graphEditor with clustered nodes
2. Hold **Shift** and click on empty canvas space
3. While holding Shift, drag to create a rectangle around 4 nodes
4. Release mouse button

**Expected Result**:
- Dotted-line rectangle appears during drag
- On release, all nodes with centers inside rectangle are selected
- Rectangle disappears after selection

**Verify**:
- Nodes partially inside rectangle: only selected if center is inside
- Hold Shift and drag again → new selection is additive

---

### Scenario 3: Group Nodes into GraphNode

**Steps**:
1. Select 3 nodes (NodeA, NodeB, NodeC) using Shift-click or rectangular selection
2. Assume NodeA has an input connected from external NodeX
3. Assume NodeC has an output connected to external NodeY
4. Right-click on the selection

**Expected Result**:
- Context menu appears with "Group (3 nodes)" option
- Click "Group"
- Selected nodes replaced by single GraphNode
- GraphNode has 1 input port (mapped to NodeA's input)
- GraphNode has 1 output port (mapped to NodeC's output)
- Connections from NodeX and to NodeY redirect to GraphNode ports

**Verify**:
- GraphNode has distinct visual appearance (double border, gradient)
- GraphNode shows badge "3 nodes inside"
- New GraphNode is selected

---

### Scenario 4: Ungroup GraphNode

**Steps**:
1. Single-click on a GraphNode to select it (or Shift-click)
2. Right-click on the GraphNode

**Expected Result**:
- Context menu appears with "Ungroup" option
- Click "Ungroup"
- GraphNode is replaced by its internal nodes
- Internal nodes positioned reasonably (not overlapping)
- External connections restored to original internal node ports

**Verify**:
- All internal connections preserved
- Selection shows the previously internal nodes

---

### Scenario 5: Zoom Into GraphNode

**Steps**:
1. Locate a GraphNode on the canvas
2. Find the small "expand" or "zoom in" button in the top-right corner
3. Click the zoom-in button

**Expected Result**:
- Canvas view transitions to show GraphNode's internal contents
- GraphNode boundary visible with:
  - Input ports on left edge
  - Output ports on right edge
- Internal nodes and connections visible
- Breadcrumb shows: "Root > GraphNodeName"
- Zoom-out button appears

**Verify**:
- Internal nodes can be edited
- New connections can be created between internal nodes
- Cannot connect internal nodes to nodes outside (they don't exist in this view)

---

### Scenario 6: Zoom Out from GraphNode

**Steps**:
1. While viewing inside a GraphNode (from Scenario 5)
2. Click the "zoom out" button or click "Root" in breadcrumb

**Expected Result**:
- Canvas transitions back to parent context
- GraphNode is visible as a single collapsed node
- Original external connections visible

**Verify**:
- Breadcrumb updates to show current level
- If nested 2+ levels, zoom out goes one level up (not to root)

---

### Scenario 7: Nested GraphNodes

**Steps**:
1. Create GraphNode A containing nodes X, Y, Z
2. Navigate into GraphNode A
3. Select nodes X and Y
4. Group them into GraphNode B (nested inside A)
5. Navigate into GraphNode B

**Expected Result**:
- Breadcrumb shows: "Root > GraphNode A > GraphNode B"
- GraphNode B's internal view shows nodes X and Y
- Zoom out returns to GraphNode A's internal view

**Verify**:
- Up to 5 levels of nesting work correctly
- Serialization preserves nested structure

---

## Key Code Locations

| Component | File Path |
|-----------|-----------|
| Selection State | `graphEditor/src/jvmMain/kotlin/state/SelectionState.kt` |
| Navigation Context | `graphEditor/src/jvmMain/kotlin/state/NavigationContext.kt` |
| Group Context Menu | `graphEditor/src/jvmMain/kotlin/ui/GroupContextMenu.kt` |
| Selection Box | `graphEditor/src/jvmMain/kotlin/ui/SelectionBox.kt` |
| GraphNode Renderer | `graphEditor/src/jvmMain/kotlin/ui/GraphNodeRenderer.kt` |
| GraphState Extensions | `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` |

## Testing Commands

```bash
# Run all tests for this feature
./gradlew :graphEditor:test --tests "io.codenode.grapheditor.state.SelectionStateTest"
./gradlew :graphEditor:test --tests "io.codenode.grapheditor.state.NavigationContextTest"
./gradlew :graphEditor:test --tests "io.codenode.grapheditor.ui.MultiSelectionTest"
./gradlew :graphEditor:test --tests "io.codenode.grapheditor.ui.GroupContextMenuTest"

# Run all graphEditor tests
./gradlew :graphEditor:test
```

## Common Issues

### Issue: Group option not appearing in context menu
**Solution**: Ensure at least 2 nodes are selected. Single node selection does not show Group option.

### Issue: Rectangular selection not working
**Solution**: Ensure you're holding Shift BEFORE clicking on empty canvas space. Shift must be held throughout the drag.

### Issue: Navigation breadcrumb not updating
**Solution**: Check that `NavigationContext` state is being observed. Ensure `getNodesInCurrentContext()` is called with updated context.

### Issue: Serialization losing nested GraphNodes
**Solution**: Verify `FlowGraphSerializer.serializeGraphNode()` recursively serializes `childNodes` and `internalConnections`.
