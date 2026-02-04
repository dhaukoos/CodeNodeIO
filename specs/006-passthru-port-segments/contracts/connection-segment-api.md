# Contract: ConnectionSegment API

**Feature**: 006-passthru-port-segments
**Date**: 2026-02-04

## ConnectionSegment Data Structure

```kotlin
@Serializable
data class ConnectionSegment(
    val id: String,
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String,
    val scopeNodeId: String?,  // null = root level
    val parentConnectionId: String
)
```

---

## Connection.segments Property

### Property Definition

```kotlin
@Serializable
data class Connection(
    // ... existing properties ...
) {
    /**
     * Ordered list of segments composing this connection.
     * Single-segment for direct connections.
     * Multi-segment for connections crossing GraphNode boundaries.
     */
    @Transient
    val segments: List<ConnectionSegment>
        get() = _segments ?: computeSegments().also { _segments = it }

    /**
     * Invalidates cached segments (call after graph structure changes).
     */
    fun invalidateSegments()
}
```

---

## Segment Computation API

### computeSegments

Computes the segment list for a connection based on graph topology.

```kotlin
fun Connection.computeSegments(
    graphContext: FlowGraph
): List<ConnectionSegment>
```

**Algorithm**:
1. Start from source node/port
2. If target is in same scope → single segment, done
3. If target crosses GraphNode boundary:
   a. Create segment from source to PassThruPort (exterior scope)
   b. Create segment from PassThruPort to target (interior scope)
4. For nested boundaries, repeat step 3 for each boundary crossed

**Returns**: Ordered list of ConnectionSegment from source to target

---

## Segment Visibility API

### getSegmentsInContext

Filters segments to those visible in the current navigation context.

```kotlin
fun List<Connection>.getSegmentsInContext(
    scopeNodeId: String?
): List<ConnectionSegment>
```

**Parameters**:
- `scopeNodeId`: null for root level, or GraphNode ID for interior view

**Returns**: Segments where `segment.scopeNodeId == scopeNodeId`

---

## Segment Validation API

### validateSegmentChain

Validates that segments form a continuous path.

```kotlin
fun Connection.validateSegmentChain(): ValidationResult
```

**Checks**:
1. At least one segment exists
2. First segment source matches connection source
3. Last segment target matches connection target
4. Each segment's target matches next segment's source

---

## Rendering Contract

### Segment Rendering

Each ConnectionSegment is rendered as a bezier curve in the graph editor.

```kotlin
fun DrawScope.renderSegment(
    segment: ConnectionSegment,
    sourcePosition: Offset,
    targetPosition: Offset,
    style: ConnectionStyle,
    scale: Float
)
```

**Behavior**:
- Uses same bezier algorithm as existing connection rendering
- Inherits color/style from parent Connection
- Highlighted when parent Connection is selected

---

## Test Requirements

### Unit Tests

1. **Direct connection has single segment** - CodeNode to CodeNode
2. **Boundary crossing creates two segments** - Through one GraphNode
3. **Nested boundary creates three segments** - Through two GraphNodes
4. **Segment chain validates correctly** - All constraints pass
5. **Invalid chain detected** - Gap in segment targets
6. **Context filtering works** - Only matching scope returned

### Integration Tests

1. **Grouping updates connection segments** - Automatic split
2. **Ungrouping merges segments** - Automatic merge
3. **Navigation shows correct segments** - Context filtering in UI
4. **Segment styling inherits from connection** - Visual consistency

### Rendering Tests

1. **Segments render as bezier curves** - Visual output
2. **Only context-appropriate segments render** - No cross-scope bleeding
3. **Selection highlights all segments** - Parent connection selected
