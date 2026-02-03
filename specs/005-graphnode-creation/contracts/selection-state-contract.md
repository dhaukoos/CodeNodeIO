# Contract: Selection State Management

**Feature**: 005-graphnode-creation
**Component**: SelectionState

## Overview

Defines the API contract for managing multi-node selection in the graphEditor.

## State Interface

```kotlin
interface SelectionStateManager {
    // State access
    val selectedNodeIds: Set<String>
    val isRectangularSelectionActive: Boolean
    val selectionBoxBounds: Rect?
    val hasSelection: Boolean
    val selectionCount: Int
    val canGroup: Boolean

    // Single node operations
    fun toggleNodeInSelection(nodeId: String)
    fun selectSingleNode(nodeId: String)
    fun deselectNode(nodeId: String)

    // Bulk operations
    fun addNodesToSelection(nodeIds: Set<String>)
    fun setSelection(nodeIds: Set<String>)
    fun clearSelection()

    // Rectangular selection
    fun startRectangularSelection(startPosition: Offset)
    fun updateRectangularSelection(currentPosition: Offset)
    fun finishRectangularSelection(nodesInBounds: Set<String>)
    fun cancelRectangularSelection()
}
```

## Method Contracts

### toggleNodeInSelection(nodeId: String)

**Preconditions**:
- `nodeId` is a valid node ID in the current view context

**Postconditions**:
- If node was selected: node is removed from selection
- If node was not selected: node is added to selection

**Side Effects**: None

### startRectangularSelection(startPosition: Offset)

**Preconditions**:
- No rectangular selection is currently active
- `startPosition` is within canvas bounds

**Postconditions**:
- `isRectangularSelectionActive` is true
- `selectionBoxBounds` has start position set

**Side Effects**: None

### finishRectangularSelection(nodesInBounds: Set<String>)

**Preconditions**:
- `isRectangularSelectionActive` is true

**Postconditions**:
- `isRectangularSelectionActive` is false
- `selectionBoxBounds` is null
- `nodesInBounds` are added to current selection (additive)

**Side Effects**: None

## Event Callbacks

```kotlin
interface SelectionStateListener {
    fun onSelectionChanged(newSelection: Set<String>)
    fun onRectangularSelectionStarted()
    fun onRectangularSelectionEnded()
}
```

## UI Integration Points

### FlowGraphCanvas

- **Shift+Click on node**: Call `toggleNodeInSelection(nodeId)`
- **Click on node (no Shift)**: Call `selectSingleNode(nodeId)`
- **Click on empty canvas (no Shift)**: Call `clearSelection()`
- **Shift+Drag on canvas**: Start/update/finish rectangular selection
- **Right-click on selection**: Show group context menu if `canGroup`

### NodeRenderer

- Render selection highlight for nodes where `nodeId in selectedNodeIds`
- Render connections between selected nodes with distinct style

### SelectionBox Component

- Render dotted rectangle at `selectionBoxBounds` when `isRectangularSelectionActive`
