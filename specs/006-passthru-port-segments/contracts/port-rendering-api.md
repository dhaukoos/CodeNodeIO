# Contract: Port Rendering API

**Feature**: 006-passthru-port-segments
**Date**: 2026-02-04

## Port Shape Enum

```kotlin
enum class PortShape {
    /** Standard port rendered as a circle (CodeNode ports) */
    CIRCLE,

    /** PassThruPort rendered as a square (GraphNode boundary ports) */
    SQUARE
}
```

---

## Port Rendering Function

### renderPort

Renders a port with the specified shape.

```kotlin
fun DrawScope.renderPort(
    position: Offset,
    direction: Port.Direction,
    shape: PortShape,
    isHovered: Boolean = false,
    isConnected: Boolean = false,
    scale: Float = 1f
)
```

**Parameters**:
- `position`: Center position of the port
- `direction`: INPUT (left side) or OUTPUT (right side)
- `shape`: CIRCLE for regular ports, SQUARE for PassThruPorts
- `isHovered`: Whether mouse is over the port
- `isConnected`: Whether port has a connection
- `scale`: Current canvas zoom level

**Behavior**:
- CIRCLE: 8dp diameter (scaled), filled with port color
- SQUARE: 8dp x 8dp (scaled), filled with port color
- Hover state: Increases size by 1.5x, adds highlight
- Connected state: Solid fill
- Unconnected state: Outline only

---

## Port Type Detection

### getPortShape

Determines the shape for rendering a port.

```kotlin
fun Port<*>.getPortShape(): PortShape
```

**Returns**:
- `PortShape.SQUARE` if the port is a PassThruPort
- `PortShape.CIRCLE` otherwise

**Implementation**:
```kotlin
fun Port<*>.getPortShape(): PortShape {
    return if (this is PassThruPort<*>) PortShape.SQUARE else PortShape.CIRCLE
}
```

---

## GraphNode Boundary Port Rendering

### renderBoundaryPorts

Renders PassThruPorts on GraphNode boundary in both exterior and interior views.

```kotlin
fun DrawScope.renderBoundaryPorts(
    graphNode: GraphNode,
    boundaryRect: Rect,
    scale: Float,
    hoveredPortId: String?
)
```

**Behavior**:
- INPUT PassThruPorts rendered on left edge of boundary
- OUTPUT PassThruPorts rendered on right edge of boundary
- Ports distributed evenly along their respective edges
- Uses SQUARE shape for all boundary ports

---

## Visual Specifications

### Port Dimensions

| Property | Circle (CodeNode) | Square (PassThruPort) |
|----------|-------------------|----------------------|
| Base size | 8dp diameter | 8dp × 8dp |
| Hover size | 12dp diameter | 12dp × 12dp |
| Stroke width | 2dp | 2dp |
| Corner radius | N/A | 2dp |

### Port Colors

| State | Fill | Stroke |
|-------|------|--------|
| Unconnected | Transparent | #757575 |
| Connected | #2196F3 | #2196F3 |
| Hovered | #64B5F6 | #2196F3 |
| Invalid | #F44336 | #F44336 |

---

## Test Requirements

### Unit Tests

1. **Regular Port returns CIRCLE shape** - Port type detection
2. **PassThruPort returns SQUARE shape** - Port type detection
3. **Port dimensions scale correctly** - Zoom levels

### Rendering Tests

1. **Circle port renders correctly** - Visual verification
2. **Square port renders correctly** - Visual verification
3. **Hover state changes size** - Interaction feedback
4. **Connected state changes fill** - Visual indicator
5. **Boundary ports align to edges** - GraphNode rendering
