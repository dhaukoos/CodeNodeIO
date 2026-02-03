# Research: GraphNode Creation Support

**Feature**: 005-graphnode-creation
**Date**: 2026-02-02

## Executive Summary

The existing codebase provides comprehensive infrastructure for implementing GraphNode creation. The GraphNode model is already fully implemented with child node management, port mappings, and validation. The graphEditor has proven patterns for context menus, state management, and canvas interaction that can be extended.

## Research Findings

### 1. Existing GraphNode Model Infrastructure

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/GraphNode.kt`

**Decision**: Use existing GraphNode model without modification
**Rationale**: The model already supports all required capabilities
**Alternatives Considered**: None - model is feature-complete

**Key Properties Already Implemented**:
- `childNodes: List<Node>` - Contains CodeNodes or nested GraphNodes
- `internalConnections: List<Connection>` - Connections between child nodes
- `portMappings: Map<String, PortMapping>` - Maps GraphNode ports to child ports

**Key Methods Already Implemented**:
- `addChild()`, `removeChild()`, `findChild()`, `withChildren()`
- `addConnection()`, `removeConnection()`, `withConnections()`
- `addPortMapping()`, `removePortMapping()`, `withPortMappings()`
- `getAllDescendants()`, `getAllCodeNodes()`, `getAllGraphNodes()`
- `validate()` - Comprehensive validation including circular dependency detection

### 2. Node Class Hierarchy

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Node.kt`

**Decision**: Extend existing sealed class hierarchy
**Rationale**: CodeNode and GraphNode already share common base; selection can work uniformly

```
Node (sealed class)
├── CodeNode - Terminal nodes with processing logic
└── GraphNode - Hierarchical container nodes (target of this feature)
```

**Shared Properties**: `id`, `name`, `nodeType`, `description`, `position`, `inputPorts`, `outputPorts`, `configuration`, `parentNodeId`

### 3. GraphState - Current Selection Implementation

**File**: `graphEditor/src/jvmMain/kotlin/state/GraphState.kt`

**Decision**: Extend GraphState with multi-selection support
**Rationale**: Existing single-selection pattern can be augmented without breaking changes
**Alternatives Considered**: Separate SelectionManager class - rejected to keep state cohesive

**Current Selection Properties**:
- `selectedNodeId: String?` - Single node selection
- `selectedConnectionIds: Set<String>` - Already supports multi-select for connections

**Proposed Extension**:
```kotlin
// Add alongside existing selectedNodeId
val selectedNodeIds: MutableSet<String> = mutableSetOf()

// New methods
fun toggleNodeSelection(nodeId: String)
fun addNodesToSelection(nodeIds: Set<String>)
fun clearNodeSelection()
fun getSelectedNodes(): List<Node>
```

### 4. Context Menu Infrastructure

**File**: `graphEditor/src/jvmMain/kotlin/ui/ConnectionContextMenu.kt`

**Decision**: Reuse ConnectionContextMenu pattern for GroupContextMenu
**Rationale**: Proven popup/menu pattern with keyboard support and focus handling

**Reusable Patterns**:
- `ConnectionContextMenuState` data class pattern
- Popup positioning via `IntOffset(position.x.toInt(), position.y.toInt())`
- Keyboard support (Escape to dismiss)
- Card-based styling with elevation
- `focusable = true` for proper focus handling

### 5. Canvas Interaction & Hit Detection

**File**: `graphEditor/src/jvmMain/kotlin/ui/FlowGraphCanvas.kt`

**Decision**: Extend existing gesture handling for rectangular selection
**Rationale**: Canvas already handles drag detection; add Shift modifier check

**Existing Functions to Extend**:
- `findNodeAtPosition()` - Detect node at screen position
- `findPortAtPosition()` - Detect port (12px click radius)
- `findConnectionAtPosition()` - Bezier hit detection (8px tolerance)

**New Functions Needed**:
- `findNodesInRect(topLeft: Offset, bottomRight: Offset): List<Node>`
- `isShiftPressed(): Boolean` - Modifier key detection

### 6. Node Rendering

**File**: `graphEditor/src/jvmMain/kotlin/rendering/NodeRenderer.kt`

**Decision**: Add GraphNode-specific rendering branch
**Rationale**: NodeRenderer already has type-specific code paths

**Existing Visual Constants**:
- Node width: 180f * scale
- Node height: 30f (header) + 25f * ports + 20f padding
- Port spacing: 25f * scale
- Port radius: 6f * scale

**GraphNode Visual Differentiation** (Proposed):
- Double border (2px outer gray, 1px inner blue)
- Gradient background (light blue #E3F2FD to white)
- "Expand" icon (magnifying glass or arrow) in top-right
- Badge showing child count

### 7. Serialization Infrastructure

**File**: `graphEditor/src/jvmMain/kotlin/serialization/FlowGraphSerializer.kt`

**Decision**: Complete partial GraphNode serialization implementation
**Rationale**: Structure exists but notes "nested nodes serialization not yet implemented"

**Current State** (Lines 170-195):
```kotlin
private fun serializeGraphNode(node: GraphNode, ...) {
    // Currently outputs basic properties only
    // TODO comment indicates nested serialization needed
}
```

**Implementation Needed**:
- Recursive serialization of `childNodes`
- Serialization of `internalConnections`
- Serialization of `portMappings`
- Proper indentation for nested structure

### 8. Connection Model - Internal Connections

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/Connection.kt`

**Decision**: Use existing `parentScopeId` for internal connections
**Rationale**: Field already exists for this purpose

**Key Property**: `parentScopeId: String?` - Identifies containing GraphNode for internal connections

### 9. FlowGraph Root Node Management

**File**: `fbpDsl/src/commonMain/kotlin/io/codenode/fbpdsl/model/FlowGraph.kt`

**Decision**: Group operation replaces selected nodes with single GraphNode at root level
**Rationale**: FlowGraph.nodes contains root-level nodes; GraphNode.childNodes contains nested

**Key Methods**:
- `addNode(node: Node): FlowGraph` - Immutable add
- `removeNode(nodeId: String): FlowGraph` - Immutable remove
- `findNode(nodeId: String): Node?` - Hierarchical search

## Implementation Patterns

### Pattern 1: Group Operation Algorithm

```
1. Identify external connections:
   - For each selected node, find connections where other endpoint is NOT selected

2. Generate GraphNode ports:
   - For each external INPUT connection → create GraphNode input port + mapping
   - For each external OUTPUT connection → create GraphNode output port + mapping

3. Create GraphNode:
   - childNodes = selected nodes (with updated parentNodeId)
   - internalConnections = connections where BOTH endpoints are selected
   - portMappings = generated in step 2

4. Update FlowGraph:
   - Remove selected nodes from root
   - Add new GraphNode to root
   - Redirect external connections to GraphNode ports
```

### Pattern 2: Ungroup Operation Algorithm

```
1. Get GraphNode's children and internal connections

2. Calculate positions:
   - Place children at GraphNode position + offset grid

3. Restore external connections:
   - For each GraphNode port mapping, redirect connection to original child port

4. Update FlowGraph:
   - Remove GraphNode from root
   - Add all children to root (with cleared parentNodeId)
   - Add internal connections to root
```

### Pattern 3: Navigation Context Stack

```kotlin
// Push when zooming into GraphNode
navigationContext = navigationContext.pushInto(graphNodeId)

// Pop when zooming out
navigationContext = navigationContext.popOut()

// Render based on context
val nodesToRender = if (navigationContext.isAtRoot) {
    flowGraph.nodes
} else {
    flowGraph.findNode(navigationContext.currentGraphNodeId)
        ?.let { (it as? GraphNode)?.childNodes }
        ?: emptyList()
}
```

## Dependencies Verified

| Dependency | Version | License | Status |
|------------|---------|---------|--------|
| Compose Desktop | 1.7.3 | Apache 2.0 | Already in use |
| kotlinx-coroutines | (project version) | Apache 2.0 | Already in use |
| kotlinx-serialization | (project version) | Apache 2.0 | Already in use |

No new dependencies required.

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Circular parent-child in grouping | GraphNode.validate() already checks this |
| Large selection performance | Use Set<String> for O(1) lookup; lazy node collection |
| Nested serialization complexity | Use recursive function with depth tracking |
| Undo/redo for group operations | Store pre-group state in UndoRedoManager command |
