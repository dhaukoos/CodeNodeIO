# Connection Selection Contract

**Component**: FlowGraphCanvas (connection selection feature)
**Module**: graphEditor
**Date**: 2026-01-30

## Purpose

Enable users to select connections by clicking on them in the Visual view, displaying their IP type properties in the Properties Panel.

## Interface Changes to FlowGraphCanvas

```kotlin
@Composable
fun FlowGraphCanvas(
    flowGraph: FlowGraph,
    selectedNodeId: String? = null,
    selectedConnectionId: String? = null,  // NEW
    // ... existing params ...
    onConnectionSelected: (String?) -> Unit = {},  // NEW
    onConnectionRightClick: (String, Offset) -> Unit = { _, _ -> },  // NEW
    // ... existing params ...
)
```

## Hit Detection

### Connection Click Detection

```kotlin
/**
 * Finds a connection at the given screen position.
 * Uses Bezier curve point distance calculation.
 *
 * @param position Screen coordinates of click
 * @param tolerance Click tolerance in pixels (default: 8)
 * @return Connection ID if found, null otherwise
 */
fun findConnectionAtPosition(
    flowGraph: FlowGraph,
    position: Offset,
    panOffset: Offset,
    scale: Float,
    tolerance: Float = 8f
): String?
```

### Algorithm

1. For each connection in the graph:
   - Get source and target port screen positions
   - Sample Bezier curve at 20 points
   - Calculate distance from click to each sample point
   - If min distance < tolerance, connection is hit

2. Selection priority:
   - Nodes take priority over connections
   - If click hits node AND connection, select node
   - Only check connections if no node hit

## Visual Feedback

### Selected Connection Styling

```kotlin
val connectionColor = if (isSelected) {
    Color(0xFF2196F3)  // Blue highlight
} else {
    ipTypeColor ?: Color(0xFF9E9E9E)  // IP type color or gray
}

val strokeWidth = if (isSelected) 4f else 2f
```

### Drawing Order

1. Draw all non-selected connections
2. Draw selected connection last (on top)
3. Draw nodes (always on top of connections)

## Right-Click Context Menu

### Detection

```kotlin
pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press &&
                event.button == PointerButton.Secondary) {
                val position = event.changes.first().position
                val connectionId = findConnectionAtPosition(...)
                if (connectionId != null) {
                    onConnectionRightClick(connectionId, position)
                }
            }
        }
    }
}
```

## Test Contracts

```kotlin
class ConnectionSelectionTest {
    @Test
    fun `clicking on connection selects it`() {
        // Given: Graph with connection from A to B
        // When: User clicks on the connection line
        // Then: onConnectionSelected called with connection ID
    }

    @Test
    fun `clicking on node does not select underlying connection`() {
        // Given: Graph with overlapping node and connection
        // When: User clicks on node
        // Then: onNodeSelected called, onConnectionSelected NOT called
    }

    @Test
    fun `clicking elsewhere deselects connection`() {
        // Given: Connection is selected
        // When: User clicks on empty canvas
        // Then: onConnectionSelected called with null
    }

    @Test
    fun `selected connection is visually highlighted`() {
        // Given: Connection is selected
        // When: Canvas renders
        // Then: Connection drawn with highlight color and thicker stroke
    }

    @Test
    fun `right-click on connection triggers context menu`() {
        // Given: Graph with connection
        // When: User right-clicks on connection
        // Then: onConnectionRightClick called with ID and position
    }

    @Test
    fun `right-click on empty area does nothing`() {
        // Given: Graph with connections
        // When: User right-clicks on empty canvas
        // Then: onConnectionRightClick NOT called
    }

    @Test
    fun `connection click tolerance is 8 pixels`() {
        // Given: Connection exists
        // When: User clicks 7 pixels from connection line
        // Then: Connection is selected
        // When: User clicks 9 pixels from connection line
        // Then: Connection is NOT selected
    }
}
```

## State Integration

### GraphState Changes

```kotlin
// Selection is mutually exclusive: node OR connection
fun selectConnection(connectionId: String?) {
    selectedConnectionId = connectionId
    selectedNodeId = null
}

fun selectNode(nodeId: String?) {
    selectedNodeId = nodeId
    selectedConnectionId = null
}
```

### Properties Panel Integration

When `selectedConnectionId` is not null:
- Properties Panel shows connection properties
- Displays IP type name and color
- Provides RGB editor for color
