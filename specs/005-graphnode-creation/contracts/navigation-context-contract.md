# Contract: Navigation Context

**Feature**: 005-graphnode-creation
**Component**: NavigationContext

## Overview

Defines the API contract for hierarchical navigation through nested GraphNodes, including zoom in/out operations and breadcrumb display.

## State Interface

```kotlin
data class NavigationContext(
    val path: List<String> = emptyList()
) {
    // Computed properties
    val isAtRoot: Boolean get() = path.isEmpty()
    val currentGraphNodeId: String? get() = path.lastOrNull()
    val parentGraphNodeId: String? get() = path.dropLast(1).lastOrNull()
    val depth: Int get() = path.size
    val canNavigateOut: Boolean get() = !isAtRoot

    // Navigation operations
    fun pushInto(graphNodeId: String): NavigationContext
    fun popOut(): NavigationContext
    fun reset(): NavigationContext

    // Utility
    fun contains(graphNodeId: String): Boolean
    fun pathTo(graphNodeId: String): NavigationContext?
}
```

## Manager Interface

```kotlin
interface NavigationContextManager {
    val navigationContext: NavigationContext

    // Navigation operations
    fun navigateInto(graphNodeId: String)
    fun navigateOut()
    fun navigateToRoot()
    fun navigateTo(path: List<String>)

    // Context-aware data access
    fun getNodesInCurrentContext(): List<Node>
    fun getConnectionsInCurrentContext(): List<Connection>
    fun getCurrentContainerName(): String

    // Breadcrumb support
    fun getBreadcrumbItems(): List<BreadcrumbItem>
}

data class BreadcrumbItem(
    val id: String?,           // null for root FlowGraph
    val name: String,
    val depth: Int,
    val isCurrentLevel: Boolean
)
```

## Method Contracts

### navigateInto(graphNodeId: String)

**Preconditions**:
- `graphNodeId` references a valid GraphNode
- GraphNode exists in the current view context (either root or current GraphNode's children)

**Postconditions**:
- `navigationContext.path` has `graphNodeId` appended
- `navigationContext.currentGraphNodeId` equals `graphNodeId`
- Canvas view updates to show GraphNode's internal contents
- Selection is cleared

**Side Effects**:
- Canvas re-renders with new context
- Breadcrumb updates

### navigateOut()

**Preconditions**:
- `navigationContext.isAtRoot` is false

**Postconditions**:
- `navigationContext.path` has last element removed
- Canvas view updates to show parent context
- Selection is cleared

**Side Effects**:
- Canvas re-renders with parent context
- Breadcrumb updates

### getNodesInCurrentContext(): List<Node>

**Returns**:
- If `isAtRoot`: `flowGraph.nodes` (root-level nodes)
- If navigated into GraphNode: `graphNode.childNodes`

**Contract**: Always returns nodes appropriate for the current view level

### getConnectionsInCurrentContext(): List<Connection>

**Returns**:
- If `isAtRoot`: `flowGraph.connections` (root-level connections)
- If navigated into GraphNode: `graphNode.internalConnections`

**Contract**: Always returns connections appropriate for the current view level

## UI Components

### Breadcrumb Component

```kotlin
@Composable
fun NavigationBreadcrumb(
    items: List<BreadcrumbItem>,
    onItemClick: (String?) -> Unit  // null for root
)
```

**Visual Design**:
```
Root > ParentGraphNode > CurrentGraphNode
  ^         ^                  ^
  |         |                  |
clickable  clickable      non-clickable (current)
```

**Behavior**:
- Clicking an item navigates to that level
- Current level is not clickable
- Separator: " > " or chevron icon

### Zoom In Button

```kotlin
@Composable
fun GraphNodeZoomInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Visual Design**:
- Small icon button (16x16 or 20x20)
- Position: Top-right corner of GraphNode
- Icon: Magnifying glass with "+" or expand arrows
- Background: Semi-transparent white
- Shows on hover or always visible

### Zoom Out Button

```kotlin
@Composable
fun NavigationZoomOutButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
)
```

**Visual Design**:
- Position: Top-left of canvas or in toolbar
- Icon: Magnifying glass with "-" or collapse arrows
- Disabled when `isAtRoot`

## Internal View Rendering

When navigated into a GraphNode, the canvas should display:

1. **Boundary Visualization**:
   - Rounded rectangle representing GraphNode boundary
   - Dashed or highlighted border

2. **Boundary Ports**:
   - Input ports along left edge of boundary
   - Output ports along right edge of boundary
   - Port labels visible

3. **Internal Nodes**:
   - All `childNodes` rendered inside boundary
   - Same rendering as root-level nodes

4. **Internal Connections**:
   - All `internalConnections` rendered
   - Connections from boundary ports to internal nodes

```
┌─────────────────────────────────────────────┐
│ [←] GraphNode Name                          │
├───┬─────────────────────────────────────┬───┤
│ ● │                                     │ ● │
│in1│    ┌─────────┐    ┌─────────┐      │out│
│   │    │ Child1  │───→│ Child2  │      │   │
│ ● │    └─────────┘    └─────────┘      │   │
│in2│         │                          │   │
│   │         └──────────────────────────│───│
└───┴─────────────────────────────────────┴───┘
     ^--- Input ports on left    Output ports on right ---^
```

## State Persistence

Navigation context is **transient** and not persisted to files:
- On file save: Navigation context ignored
- On file load: Navigation context resets to root
- On IDE restart: Navigation context resets to root

This is by design - users expect to see the full graph when opening a file.
