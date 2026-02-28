# Data Model: View GraphNode Properties

**Feature**: 033-graphnode-properties
**Date**: 2026-02-27

## Existing Entities (No Changes)

### Node (sealed abstract class)
Base class for all graph nodes. Already has all properties needed:
- `id: String`
- `name: String`
- `inputPorts: List<Port<*>>`
- `outputPorts: List<Port<*>>`
- `configuration: Map<String, String>`
- `position: Position`

### GraphNode (extends Node)
Already has child nodes property:
- `childNodes: List<Node>` — ordered list of contained nodes (CodeNode or nested GraphNode)
- `internalConnections: List<Connection>` — connections between child nodes
- `portMappings: Map<String, PortMapping>` — maps GraphNode ports to child ports

### CodeNode (extends Node)
No changes needed. Existing Properties Panel continues to work for CodeNodes.

## Data Flow

```
User clicks GraphNode on canvas
  → graphState.selectedNodeId set to GraphNode's ID
  → graphState.flowGraph.findNode(nodeId) returns GraphNode
  → Main.kt derives selectedGraphNode: GraphNode? (new)
  → CompactPropertiesPanelWithViewModel receives selectedGraphNode
  → GraphNodePropertiesPanel renders:
      1. Name TextField (editable) → callback → GraphState.updateNodeName()
      2. Input Ports list (display via PortEditorRow)
      3. Output Ports list (display via PortEditorRow)
      4. Child Nodes list (read-only, names only)
```

## No New Entities

This feature only reads existing data and adds a new display path. No new data classes, persistence, or state management entities are required.
