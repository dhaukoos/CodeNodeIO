# Contract: Group Context Menu

**Feature**: 005-graphnode-creation
**Component**: GroupContextMenu

## Overview

Defines the API contract for the context menu that appears when right-clicking on selected nodes, providing Group and Ungroup operations.

## Component Interface

```kotlin
@Composable
fun GroupContextMenu(
    state: GroupContextMenuState,
    onGroup: () -> Unit,
    onUngroup: () -> Unit,
    onDismiss: () -> Unit
)

data class GroupContextMenuState(
    val position: Offset,
    val selectedNodeIds: Set<String>,
    val isGraphNodeSelected: Boolean,
    val selectedGraphNodeId: String? = null
) {
    val showGroupOption: Boolean get() = selectedNodeIds.size >= 2
    val showUngroupOption: Boolean get() = isGraphNodeSelected && selectedNodeIds.size == 1
    val hasOptions: Boolean get() = showGroupOption || showUngroupOption
}
```

## Composable Contract

### Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| state | GroupContextMenuState | Yes | Current menu state |
| onGroup | () -> Unit | Yes | Called when "Group" is selected |
| onUngroup | () -> Unit | Yes | Called when "Ungroup" is selected |
| onDismiss | () -> Unit | Yes | Called when menu should close |

### Behavior

1. **Rendering**:
   - Menu appears at `state.position` coordinates
   - Uses Popup with Card styling (matches ConnectionContextMenu)
   - Shows "Group" option if `state.showGroupOption`
   - Shows "Ungroup" option if `state.showUngroupOption`

2. **Interactions**:
   - Clicking "Group" calls `onGroup()` then `onDismiss()`
   - Clicking "Ungroup" calls `onUngroup()` then `onDismiss()`
   - Clicking outside menu calls `onDismiss()`
   - Pressing Escape calls `onDismiss()`

3. **Focus**:
   - Menu is focusable for keyboard support
   - First menu item receives focus on open

## Menu Items

### Group Option

**Label**: "Group (N nodes)"
**Icon**: Folder or bracket icon
**Enabled**: When 2+ nodes are selected
**Action**: Creates GraphNode from selection

### Ungroup Option

**Label**: "Ungroup"
**Icon**: Expand or unbracket icon
**Enabled**: When single GraphNode is selected
**Action**: Expands GraphNode to constituent nodes

## State Manager Interface

```kotlin
interface GroupContextMenuManager {
    val groupContextMenu: GroupContextMenuState?

    fun showGroupContextMenu(position: Offset, selectedNodeIds: Set<String>)
    fun hideGroupContextMenu()
    fun performGroup(): GraphNode?
    fun performUngroup(graphNodeId: String)
}
```

## Method Contracts

### showGroupContextMenu(position: Offset, selectedNodeIds: Set<String>)

**Preconditions**:
- `selectedNodeIds` is not empty
- `position` is within canvas bounds

**Postconditions**:
- `groupContextMenu` is non-null with provided state
- Menu is rendered at `position`

### performGroup(): GraphNode?

**Preconditions**:
- `selectedNodeIds.size >= 2`
- All selected nodes exist in current context

**Postconditions**:
- New GraphNode created containing selected nodes
- Selected nodes removed from current context
- External connections redirected to GraphNode ports
- Selection cleared and new GraphNode selected
- Returns created GraphNode or null on failure

### performUngroup(graphNodeId: String)

**Preconditions**:
- `graphNodeId` references a valid GraphNode
- GraphNode exists in current context

**Postconditions**:
- GraphNode removed from current context
- Child nodes added to current context
- Internal connections added to current context
- External connections redirected to original child ports
- Selection set to ungrouped nodes

## Visual Design

```
┌─────────────────────┐
│ ▢ Group (3 nodes)   │  <- Shown if 2+ nodes selected
├─────────────────────┤
│ ▣ Ungroup           │  <- Shown if GraphNode selected
└─────────────────────┘

Position: Top-left at right-click coordinates
Min width: 160px
Padding: 8px
Background: White with elevation shadow
Border radius: 4px
```
