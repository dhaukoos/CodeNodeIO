# Data Model: ViewModel Pattern Migration

**Feature**: 017-viewmodel-pattern
**Date**: 2026-02-16

## Entity Overview

This feature introduces ViewModel classes and their associated state data classes. No new database entities or external contracts are required.

---

## ViewModel Entities

### 1. GraphEditorViewModel

**Purpose**: Top-level orchestration of the graph editor application

**State**:
```
GraphEditorState
├── showOpenDialog: Boolean
├── showCompileDialog: Boolean
├── showModuleSaveDialog: Boolean
├── showFlowGraphPropertiesDialog: Boolean
├── statusMessage: String
├── canGroup: Boolean (derived from selection)
├── canUngroup: Boolean (derived from selection)
└── isInsideGraphNode: Boolean (derived from navigation)
```

**Dependencies**:
- GraphState (shared)
- UndoRedoManager (shared)
- CompilationService
- ModuleSaveService

**Actions**:
- createNewGraph()
- openGraph(file)
- saveGraph()
- undo()
- redo()
- groupSelectedNodes()
- ungroupSelectedNode()
- navigateBack()
- compile()
- showDialog(type)
- hideDialog(type)
- setStatusMessage(message)

---

### 2. NodeGeneratorViewModel

**Purpose**: Manages the Node Generator panel form state

**State**:
```
NodeGeneratorPanelState
├── name: String
├── inputCount: Int (0-3)
├── outputCount: Int (0-3)
├── isExpanded: Boolean
├── inputDropdownExpanded: Boolean
├── outputDropdownExpanded: Boolean
├── isValid: Boolean (derived)
└── genericType: String (derived)
```

**Dependencies**:
- CustomNodeRepository

**Actions**:
- setName(name)
- setInputCount(count)
- setOutputCount(count)
- toggleExpanded()
- toggleInputDropdown()
- toggleOutputDropdown()
- createNode() → CustomNodeDefinition
- reset()

---

### 3. NodePaletteViewModel

**Purpose**: Manages the Node Palette search and category expansion

**State**:
```
NodePaletteState
├── searchQuery: String
├── expandedCategories: Set<NodeCategory>
├── filteredNodeTypes: List<NodeTypeDefinition> (derived)
├── deletableNodeNames: Set<String>
└── groupedNodes: Map<NodeCategory, List<NodeTypeDefinition>> (derived)
```

**Dependencies**:
- CustomNodeRepository (for deletable node names)

**Actions**:
- setSearchQuery(query)
- toggleCategory(category)
- expandAll()
- collapseAll()
- deleteCustomNode(name)

---

### 4. IPPaletteViewModel

**Purpose**: Manages IP type selection in the palette

**State**:
```
IPPaletteState
├── allTypes: List<InformationPacketType>
├── selectedTypeId: String?
└── expandedCategories: Set<String>
```

**Dependencies**:
- IPTypeRegistry

**Actions**:
- selectType(typeId)
- clearSelection()
- toggleCategory(category)

---

### 5. PropertiesPanelViewModel

**Purpose**: Manages property editing for selected nodes/connections

**State**:
```
PropertiesPanelState
├── selectedNode: CodeNode?
├── selectedConnection: Connection?
├── editingPropertyKey: String?
├── pendingChanges: Map<String, String>
├── validationErrors: Map<String, String>
└── propertyDefinitions: List<PropertyDefinition>
```

**Dependencies**:
- GraphState (for selection and node data)
- PropertyChangeTracker (for committing changes)

**Actions**:
- startEditing(propertyKey)
- updatePendingChange(key, value)
- commitChanges()
- cancelEditing()
- validateProperty(key, value)
- updateNodeName(newName)
- updatePortName(portId, newName)

---

### 6. CanvasInteractionViewModel

**Purpose**: Manages canvas interaction state (drag, selection, connection creation)

**State**:
```
CanvasInteractionState
├── draggingNodeId: String?
├── pendingConnection: PendingConnection?
├── selectionBoxBounds: Rect?
├── hoveredNodeId: String?
├── hoveredPort: PortLocation?
├── connectionContextMenu: ConnectionContextMenuState?
└── interactionMode: InteractionMode (NORMAL, DRAGGING, CREATING_CONNECTION, SELECTING)
```

**Dependencies**:
- GraphState (for node positions, connections)
- UndoRedoManager (for committing moves, connections)

**Actions**:
- startNodeDrag(nodeId, offset)
- updateNodeDrag(newOffset)
- endNodeDrag()
- startConnectionCreation(sourceNode, sourcePort)
- updateConnectionEndpoint(position)
- completeConnection(targetNode, targetPort)
- cancelConnection()
- startRectangularSelection(startPosition)
- updateRectangularSelection(currentPosition)
- finishRectangularSelection()
- showConnectionContextMenu(connectionId, position)
- hideConnectionContextMenu()
- setHoveredNode(nodeId)
- setHoveredPort(portLocation)

---

## Shared State Entities (Existing)

These entities remain in the `state/` package and are injected into ViewModels:

### GraphState
- Already well-structured
- ViewModels observe via properties
- Mutations through existing methods

### UndoRedoManager
- Commands: AddNodeCommand, RemoveNodeCommand, MoveNodeCommand, AddConnectionCommand, GroupNodesCommand, UngroupNodeCommand
- ViewModels call `execute(command, graphState)`

### PropertyChangeTracker
- ViewModels call `trackChange(nodeId, key, oldValue, newValue)`
- Automatically creates undo/redo entries

---

## CompositionLocal Provider

**Purpose**: Provide shared state to ViewModels through Compose tree

```
SharedStateProvider
├── graphState: GraphState
├── undoRedoManager: UndoRedoManager
├── propertyChangeTracker: PropertyChangeTracker
├── ipTypeRegistry: IPTypeRegistry
└── customNodeRepository: CustomNodeRepository
```

**Access Pattern**:
```kotlin
val LocalSharedState = staticCompositionLocalOf<SharedStateProvider> { error("Not provided") }

@Composable
fun GraphEditorApp() {
    val sharedState = remember { SharedStateProvider(...) }

    CompositionLocalProvider(LocalSharedState provides sharedState) {
        // Child composables can access:
        val graphState = LocalSharedState.current.graphState
    }
}
```

---

## State Transitions

### Node Creation Flow
```
NodeGeneratorPanel
    └─ NodeGeneratorViewModel.createNode()
        ├─ Validates state
        ├─ Creates CustomNodeDefinition
        ├─ Calls CustomNodeRepository.add()
        └─ Triggers NodePaletteViewModel refresh
```

### Node Placement Flow
```
NodePalette
    └─ NodePaletteViewModel (provides node types)
        └─ User clicks node type
            └─ GraphEditorViewModel.addNodeToCanvas(nodeType)
                └─ UndoRedoManager.execute(AddNodeCommand)
                    └─ GraphState.addNode()
```

### Property Edit Flow
```
PropertiesPanel
    └─ PropertiesPanelViewModel.startEditing(key)
        └─ User edits value
            └─ PropertiesPanelViewModel.updatePendingChange(key, value)
                └─ User commits (blur/enter)
                    └─ PropertiesPanelViewModel.commitChanges()
                        └─ PropertyChangeTracker.trackChange()
                            └─ GraphState.updateNodeConfiguration()
```

---

## Validation Rules

| Entity | Field | Rule |
|--------|-------|------|
| NodeGeneratorPanelState | name | Not blank |
| NodeGeneratorPanelState | inputCount + outputCount | At least one > 0 |
| NodeGeneratorPanelState | inputCount | 0-3 range |
| NodeGeneratorPanelState | outputCount | 0-3 range |
| PropertiesPanelState | pendingChanges | Validated per property definition |
