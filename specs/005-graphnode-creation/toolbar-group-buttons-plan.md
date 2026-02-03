# Plan: Switch Group/Ungroup from Context Menu to Top-Bar

**Date:** 2026-02-03
**Status:** Approved
**Related:** User Story 3 (Group) and User Story 4 (Ungroup)

## Problem

When right-clicking on selected elements, the selection is cleared instead of showing the GroupContextMenu. This happens because:
1. The right-click handler only shows the menu when 2+ nodes are selected AND clicking on a selected node
2. If conditions aren't met, the event isn't consumed and propagates to the tap handler
3. The tap handler interprets it as an empty canvas click and clears selection

## Solution

Replace the context menu approach with Group/Ungroup buttons in the existing TopToolbar.

## Changes Required

### 1. Modify TopToolbar in Main.kt

**File:** `graphEditor/src/jvmMain/kotlin/Main.kt` (lines 692-767)

Add Group/Ungroup buttons after the Undo/Redo section:

```
Current:  [Title] --- [New][Open][Save] | [Undo][Redo]
Proposed: [Title] --- [New][Open][Save] | [Undo][Redo] | [Group][Ungroup]
```

- **Group button**: Enabled when `selectionState.selectedNodeIds.size >= 2`
- **Ungroup button**: Enabled when single GraphNode is selected

### 2. Remove Right-Click Handler for Group Menu

**File:** `graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt` (lines 114-156)

Remove the right-click handling block that calls `onGroupRightClick`. Keep the connection right-click handling intact.

### 3. Remove GroupContextMenu Integration

**File:** `graphEditor/src/jvmMain/kotlin/Main.kt` (lines 546-564)

Remove the `GroupContextMenu` composable call from the UI tree.

### 4. Simplify GraphState

**File:** `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` (lines 585-611)

Remove or deprecate:
- `groupContextMenu` property
- `showGroupContextMenu()` method
- `hideGroupContextMenu()` method

Keep:
- `groupSelectedNodes()` method (used by toolbar button)

Add helpers:
- `canGroupSelection(): Boolean` - returns true if 2+ nodes selected
- `canUngroupSelection(): Boolean` - returns true if single GraphNode selected

### 5. Update FlowGraphCanvas Signature

**File:** `graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt`

Remove `onGroupRightClick` parameter from `FlowGraphCanvas` composable.

### 6. Update Tests

**File:** `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/GroupContextMenuTest.kt`

Update tests to verify toolbar button behavior instead of context menu.

## Files to Modify

| File | Action |
|------|--------|
| `graphEditor/src/jvmMain/kotlin/Main.kt` | Add Group/Ungroup buttons to TopToolbar, remove GroupContextMenu |
| `graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt` | Remove onGroupRightClick parameter and handler |
| `graphEditor/src/jvmMain/kotlin/state/GraphState.kt` | Add canGroupSelection/canUngroupSelection helpers, remove context menu state |
| `graphEditor/src/jvmTest/kotlin/io/codenode/grapheditor/ui/GroupContextMenuTest.kt` | Update tests for toolbar approach |

## Files to Keep (No Changes Needed)

- `GraphNodeFactory.kt` - Core grouping logic unchanged
- `GraphNodeRenderer.kt` - Visual rendering unchanged

## Files That Can Be Removed (Optional)

- `GroupContextMenu.kt` - No longer used
- `GroupContextMenuState.kt` - No longer used

## Implementation Order

1. Add `canGroupSelection()` and `canUngroupSelection()` helpers to GraphState
2. Add Group/Ungroup buttons to TopToolbar in Main.kt
3. Remove GroupContextMenu from Main.kt UI tree
4. Remove right-click group handler from FlowGraphCanvas
5. Remove onGroupRightClick parameter from FlowGraphCanvas signature
6. Clean up unused context menu state from GraphState
7. Update tests

## Verification

1. Run the app and select 2+ nodes with Shift-click or rectangular selection
2. Verify Group button in toolbar becomes enabled
3. Click Group button - verify nodes are grouped into GraphNode
4. Select the new GraphNode
5. Verify Ungroup button becomes enabled
6. Click Ungroup - verify nodes are restored
7. Verify right-click no longer deselects elements
8. Run existing tests: `./gradlew :graphEditor:test`
