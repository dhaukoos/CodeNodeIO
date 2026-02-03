# Data Model: GraphNode Creation Support

**Feature**: 005-graphnode-creation
**Date**: 2026-02-02

## Entity Overview

This feature introduces new state management entities for selection and navigation, while leveraging the existing GraphNode model.

## New Entities

### 1. SelectionState

Manages multi-selection of nodes in the graphEditor.

```kotlin
data class SelectionState(
    val selectedNodeIds: Set<String> = emptySet(),
    val selectedConnectionIds: Set<String> = emptySet(),
    val selectionBoxStart: Offset? = null,
    val selectionBoxEnd: Offset? = null,
    val isRectangularSelectionActive: Boolean = false
) {
    // Computed properties
    val hasSelection: Boolean get() = selectedNodeIds.isNotEmpty() || selectedConnectionIds.isNotEmpty()
    val hasNodeSelection: Boolean get() = selectedNodeIds.isNotEmpty()
    val hasConnectionSelection: Boolean get() = selectedConnectionIds.isNotEmpty()
    val nodeSelectionCount: Int get() = selectedNodeIds.size
    val connectionSelectionCount: Int get() = selectedConnectionIds.size
    val totalSelectionCount: Int get() = nodeSelectionCount + connectionSelectionCount
    val canGroup: Boolean get() = selectedNodeIds.size >= 2

    // Selection box bounds (for rendering)
    val selectionBoxBounds: Rect? get() =
        if (selectionBoxStart != null && selectionBoxEnd != null)
            Rect(selectionBoxStart, selectionBoxEnd)
        else null
}
```

**Validation Rules**:
- `selectedNodeIds` must contain valid node IDs that exist in the current view context
- `selectedConnectionIds` must contain valid connection IDs that exist in the current view context
- Selection box coordinates must be non-negative when active

**State Transitions**:
```
EMPTY -> SELECTING (Shift+click on node or connection)
SELECTING -> SELECTING (Shift+click adds/removes nodes or connections)
SELECTING -> EMPTY (Click without Shift)
EMPTY -> BOX_SELECTING (Shift+drag on canvas)
BOX_SELECTING -> SELECTING (Drag release adds nodes and their internal connections)
```

### 2. NavigationContext

Manages hierarchical navigation through nested GraphNodes.

```kotlin
data class NavigationContext(
    val path: List<String> = emptyList()
) {
    // Computed properties
    val isAtRoot: Boolean get() = path.isEmpty()
    val currentGraphNodeId: String? get() = path.lastOrNull()
    val depth: Int get() = path.size
    val parentGraphNodeId: String? get() = path.dropLast(1).lastOrNull()

    // Navigation operations
    fun pushInto(graphNodeId: String): NavigationContext =
        NavigationContext(path + graphNodeId)

    fun popOut(): NavigationContext =
        NavigationContext(path.dropLast(1))

    fun reset(): NavigationContext =
        NavigationContext(emptyList())

    // Breadcrumb for UI display
    fun getBreadcrumb(flowGraph: FlowGraph): List<BreadcrumbItem>
}

data class BreadcrumbItem(
    val id: String?,        // null for root
    val name: String,
    val isCurrentLevel: Boolean
)
```

**Validation Rules**:
- Each ID in `path` must reference a valid GraphNode
- Path must not contain duplicates (no circular navigation)
- Parent node at index N must contain child node at index N+1

### 3. GroupContextMenuState

State for the group/ungroup context menu.

```kotlin
data class GroupContextMenuState(
    val position: Offset,
    val selectedNodeIds: Set<String>,
    val isGraphNodeSelected: Boolean
) {
    val showGroupOption: Boolean get() = selectedNodeIds.size >= 2
    val showUngroupOption: Boolean get() = isGraphNodeSelected && selectedNodeIds.size == 1
}
```

### 4. SelectionBox (Visual Only)

Temporary visual element during rectangular selection.

```kotlin
data class SelectionBox(
    val startPosition: Offset,
    val currentPosition: Offset
) {
    val bounds: Rect get() = Rect(
        topLeft = Offset(
            minOf(startPosition.x, currentPosition.x),
            minOf(startPosition.y, currentPosition.y)
        ),
        bottomRight = Offset(
            maxOf(startPosition.x, currentPosition.x),
            maxOf(startPosition.y, currentPosition.y)
        )
    )

    fun containsNodeCenter(nodePosition: Offset, nodeSize: Size): Boolean {
        val center = Offset(
            nodePosition.x + nodeSize.width / 2,
            nodePosition.y + nodeSize.height / 2
        )
        return bounds.contains(center)
    }
}
```

## Existing Entities (Extended)

### GraphState Extensions

```kotlin
// Add to existing GraphState class
class GraphState {
    // Existing properties...

    // NEW: Multi-selection support
    var selectionState: SelectionState by mutableStateOf(SelectionState())
        private set

    // NEW: Navigation context
    var navigationContext: NavigationContext by mutableStateOf(NavigationContext())
        private set

    // NEW: Group context menu
    var groupContextMenu: GroupContextMenuState? by mutableStateOf(null)
        private set

    // NEW: Node selection methods
    fun toggleNodeInSelection(nodeId: String) { ... }
    fun addNodesToSelection(nodeIds: Set<String>) { ... }

    // NEW: Connection selection methods
    fun toggleConnectionInSelection(connectionId: String) { ... }
    fun addConnectionsToSelection(connectionIds: Set<String>) { ... }
    fun selectConnectionsBetweenNodes(nodeIds: Set<String>) { ... }  // Auto-select internal connections

    // NEW: Combined selection methods
    fun clearSelection() { ... }  // Clears both nodes and connections
    fun startRectangularSelection(startPosition: Offset) { ... }
    fun updateRectangularSelection(currentPosition: Offset) { ... }
    fun finishRectangularSelection() { ... }  // Selects nodes AND their internal connections

    // NEW: Navigation methods
    fun navigateInto(graphNodeId: String) { ... }
    fun navigateOut() { ... }
    fun navigateToRoot() { ... }

    // NEW: Grouping methods
    fun showGroupContextMenu(position: Offset) { ... }
    fun hideGroupContextMenu() { ... }
    fun groupSelectedNodes(): GraphNode? { ... }
    fun ungroupGraphNode(graphNodeId: String) { ... }

    // NEW: Context-aware node access
    fun getNodesInCurrentContext(): List<Node> { ... }
    fun getConnectionsInCurrentContext(): List<Connection> { ... }
}
```

### GraphNode (Existing - No Changes)

The existing GraphNode model already supports all required data:

```kotlin
data class GraphNode(
    override val id: String,
    override val name: String,
    override val description: String = "",
    override val position: Node.Position = Node.Position.ORIGIN,
    override val configuration: PropertyConfiguration = PropertyConfiguration(),
    override val parentNodeId: String? = null,

    // Child management (already implemented)
    val childNodes: List<Node> = emptyList(),
    val internalConnections: List<Connection> = emptyList(),
    val portMappings: Map<String, PortMapping> = emptyMap()
) : Node() {
    // Port mapping structure (already implemented)
    data class PortMapping(
        val childNodeId: String,
        val childPortName: String
    )
}
```

## Entity Relationships

```
FlowGraph
├── nodes: List<Node>  (root-level only)
│   ├── CodeNode
│   └── GraphNode
│       ├── childNodes: List<Node>  (recursive)
│       ├── internalConnections: List<Connection>
│       └── portMappings: Map<String, PortMapping>
└── connections: List<Connection>  (root-level only)

GraphState
├── flowGraph: FlowGraph
├── selectionState: SelectionState
├── navigationContext: NavigationContext
└── groupContextMenu: GroupContextMenuState?
```

## Port Mapping Generation Logic

When creating a GraphNode from selection:

```kotlin
data class GeneratedPortMapping(
    val graphNodePortId: String,
    val graphNodePortName: String,
    val graphNodePortDirection: Port.Direction,
    val childNodeId: String,
    val childPortId: String,
    val externalConnectionId: String  // Connection to redirect
)

fun generatePortMappings(
    selectedNodeIds: Set<String>,
    allConnections: List<Connection>,
    allNodes: List<Node>
): List<GeneratedPortMapping> {
    val external = allConnections.filter { conn ->
        val sourceSelected = conn.sourceNodeId in selectedNodeIds
        val targetSelected = conn.targetNodeId in selectedNodeIds
        sourceSelected xor targetSelected  // Exactly one endpoint selected
    }

    return external.map { conn ->
        if (conn.sourceNodeId in selectedNodeIds) {
            // Output port needed on GraphNode
            GeneratedPortMapping(
                graphNodePortId = generateId("port"),
                graphNodePortName = "out_${conn.sourcePortId}",
                graphNodePortDirection = Port.Direction.OUTPUT,
                childNodeId = conn.sourceNodeId,
                childPortId = conn.sourcePortId,
                externalConnectionId = conn.id
            )
        } else {
            // Input port needed on GraphNode
            GeneratedPortMapping(
                graphNodePortId = generateId("port"),
                graphNodePortName = "in_${conn.targetPortId}",
                graphNodePortDirection = Port.Direction.INPUT,
                childNodeId = conn.targetNodeId,
                childPortId = conn.targetPortId,
                externalConnectionId = conn.id
            )
        }
    }
}
```

## Serialization Considerations

### GraphNode Serialization Format

```kotlin
graphNode("groupName") {
    description = "Description"
    position(100.0, 200.0)

    // Child nodes (recursive)
    children {
        codeNode("child1") { ... }
        codeNode("child2") { ... }
    }

    // Internal connections
    internalConnections {
        connect("child1", "output1") to ("child2", "input1")
    }

    // Port mappings
    portMappings {
        inputPort("in_data") maps ("child1", "input1")
        outputPort("out_result") maps ("child2", "output1")
    }
}
```
