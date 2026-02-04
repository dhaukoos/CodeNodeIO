# Research: PassThruPort and ConnectionSegment

**Feature**: 006-passthru-port-segments
**Date**: 2026-02-04

## Research Topics

### 1. Port Inheritance Strategy in Kotlin

**Question**: Should PassThruPort be a data class extending Port, or use delegation/composition?

**Decision**: Use composition with a wrapper approach rather than direct inheritance.

**Rationale**:
- Port is a generic data class `Port<T : Any>` which makes direct inheritance complex
- Kotlin data classes cannot extend other data classes
- Composition allows PassThruPort to contain port references without copying all Port logic

**Alternatives Considered**:
- Direct inheritance via interface: Rejected because Port is a data class, not an interface
- Sealed class hierarchy: Rejected because it would require refactoring existing Port usage
- Type alias: Rejected because we need additional properties (upstream/downstream references)

**Implementation Approach**:
```kotlin
data class PassThruPort<T : Any>(
    val port: Port<T>,  // The underlying port representation
    val upstreamNodeId: String,
    val upstreamPortId: String,
    val downstreamNodeId: String,
    val downstreamPortId: String
) {
    // Delegate common properties to underlying port
    val id: String get() = port.id
    val name: String get() = port.name
    val direction: Port.Direction get() = port.direction
    val dataType: KClass<T> get() = port.dataType
    val owningNodeId: String get() = port.owningNodeId
}
```

---

### 2. ConnectionSegment Data Structure

**Question**: What information does ConnectionSegment need to store for rendering and context filtering?

**Decision**: ConnectionSegment is a lightweight data class containing endpoint references and context scope.

**Rationale**:
- Segments need source/target references for bezier rendering
- Segments need scope context to determine visibility
- Parent connection reference needed for styling inheritance

**Implementation Approach**:
```kotlin
@Serializable
data class ConnectionSegment(
    val id: String,
    val sourceNodeId: String,
    val sourcePortId: String,
    val targetNodeId: String,
    val targetPortId: String,
    val scopeNodeId: String?,  // null = root level, otherwise = inside this GraphNode
    val parentConnectionId: String
)
```

---

### 3. Connection.segments Property Design

**Question**: How should the segments list be managed - eager vs lazy, mutable vs computed?

**Decision**: Computed property based on connection path analysis, cached for performance.

**Rationale**:
- Segments are derived from the connection path through GraphNode boundaries
- Computing on-demand avoids serialization complexity
- Caching prevents repeated computation during render cycles

**Alternatives Considered**:
- Stored mutable list: Rejected because segments must stay in sync with graph structure
- Pure computed property: Rejected due to performance cost during frequent redraws
- Lazy delegate: Selected for balance of correctness and performance

**Implementation Approach**:
```kotlin
@Serializable
data class Connection(
    // ... existing properties ...
) {
    @Transient
    private var _segments: List<ConnectionSegment>? = null

    val segments: List<ConnectionSegment>
        get() = _segments ?: computeSegments().also { _segments = it }

    fun invalidateSegments() {
        _segments = null
    }
}
```

---

### 4. Automatic PassThruPort Creation During Grouping

**Question**: When and how should PassThruPorts be created during the grouping operation?

**Decision**: Integrate into GraphNodeFactory.createFromSelection() with explicit port creation step.

**Rationale**:
- Grouping already analyzes external connections via generatePortMappings()
- PassThruPorts should be created for each boundary-crossing connection
- Type validation must occur before PassThruPort creation

**Implementation Approach**:
1. During grouping, identify connections crossing the boundary
2. For each crossing connection, create a PassThruPort:
   - INPUT PassThruPort for incoming connections (external → internal)
   - OUTPUT PassThruPort for outgoing connections (internal → external)
3. Store upstream/downstream references in PassThruPort
4. Add PassThruPort to GraphNode's inputPorts/outputPorts
5. Update Connection to reference the new PassThruPort

---

### 5. Visual Rendering of Port Shapes

**Question**: How to implement circle vs square port rendering without duplicating code?

**Decision**: Add PortShape enum and parameterize existing port rendering.

**Rationale**:
- Port rendering already exists in NodeRenderer
- Shape is the only difference between Port and PassThruPort rendering
- Single rendering function with shape parameter is cleanest

**Implementation Approach**:
```kotlin
enum class PortShape { CIRCLE, SQUARE }

fun DrawScope.renderPort(
    position: Offset,
    direction: Port.Direction,
    shape: PortShape,
    isHovered: Boolean,
    scale: Float
) {
    when (shape) {
        PortShape.CIRCLE -> drawCircle(...)
        PortShape.SQUARE -> drawRect(...)
    }
}
```

---

### 6. Segment Visibility Filtering

**Question**: How to efficiently filter segments by navigation context?

**Decision**: Filter Connection.segments by scopeNodeId matching current context.

**Rationale**:
- NavigationContext already tracks current GraphNode path
- Segments have scopeNodeId indicating their visibility scope
- Simple filter is O(n) where n = segment count (typically 1-3)

**Implementation Approach**:
```kotlin
fun getVisibleSegments(
    connections: List<Connection>,
    currentScopeId: String?
): List<ConnectionSegment> {
    return connections.flatMap { conn ->
        conn.segments.filter { it.scopeNodeId == currentScopeId }
    }
}
```

---

### 7. Backward Compatibility with Serialization

**Question**: How to maintain backward compatibility when loading old .flow.kts files?

**Decision**: Treat missing segments as single-segment connections; upgrade on save.

**Rationale**:
- Old files won't have PassThruPorts or segments
- Direct CodeNode-to-CodeNode connections imply single segment
- Saving after load will include segments (silent upgrade)

**Implementation Approach**:
1. During deserialization, if segments missing, create default single segment
2. If GraphNode has portMappings but no PassThruPorts, create them on load
3. Serialize segments only when > 1 (optimize file size for simple cases)

---

## Summary of Decisions

| Topic | Decision | Key Reason |
|-------|----------|------------|
| Port Inheritance | Composition with wrapper | Kotlin data class limitations |
| ConnectionSegment | Lightweight data class | Simple, serializable, filterable |
| segments property | Cached computed property | Balance correctness and performance |
| PassThruPort creation | During grouping in factory | Centralized, type-validated |
| Port shape rendering | PortShape enum parameter | DRY, minimal changes |
| Segment filtering | scopeNodeId filter | Efficient O(n), clear semantics |
| Backward compat | Silent upgrade on load/save | Smooth migration path |
