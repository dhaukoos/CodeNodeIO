# UI Interaction Contracts: Port Type Selector

**Feature**: 023-port-type-selector
**Date**: 2026-02-20

## Interaction 1: Port Type Dropdown Display

**Trigger**: Generic Node selected, Properties panel visible, port rows rendered.

**UI Layout (per port row)**:
```
┌──────────────────────┐ ┌──────────────┐
│  Port Name TextField │ │ Type ▼       │
└──────────────────────┘ └──────────────┘
```

**Dropdown Contents**:
```
┌──────────────────┐
│ ● Any            │  ← Black dot
│ ● Int            │  ← Blue dot
│ ● Double         │  ← Purple dot
│ ● Boolean        │  ← Green dot
│ ● String         │  ← Orange dot
└──────────────────┘
```

Each item shows a color indicator (from IPColor) followed by the typeName.

**State Mapping**:
- Current value: Resolved from `port.dataType.simpleName` (e.g., "Int", "String", "Any")
- Options: `IPTypeRegistry.getAllTypes().map { it.typeName }`

## Interaction 2: Port Type Selection

**Trigger**: User opens dropdown and selects a type.

**Callback Chain**:
```
PropertiesPanel composable
  → onPortTypeChange(portId, typeName)
    → PropertiesPanelViewModel.updatePortType(portId, typeName)
      → onPortTypeChanged(portId, typeName)  [injected callback]
        → GraphState.updatePortType(nodeId, portId, typeName)
```

**GraphState.updatePortType Contract**:

**Input**: `nodeId: String`, `portId: String`, `typeName: String`

**Steps**:
1. Resolve `ipType = ipTypeRegistry.getByTypeName(typeName)` - if null, return early
2. Update port: copy port with `dataType = ipType.payloadType`
3. Update owning node: copy node with updated port lists
4. Propagate to connections:
   - Find connections where `sourcePortId == portId` or `targetPortId == portId`
   - For each: copy connection with `ipTypeId = ipType.id`
   - For each: find remote port, copy with `dataType = ipType.payloadType`
   - For each: copy remote node with updated port lists
5. Replace flowGraph with all updates
6. Set `isDirty = true`

**Output**: Updated flowGraph with consistent types across port → connection → remote port.

## Interaction 3: Connection Type Propagation to Ports

**Trigger**: Connection's `ipTypeId` changes (via `GraphState.updateConnectionIPType` or future connection type UI).

**Extended Contract for updateConnectionIPType**:

After updating the connection's `ipTypeId`:
1. Resolve `ipType = ipTypeRegistry.getById(ipTypeId)` - if null, return early
2. Find source port (via `connection.sourceNodeId` + `connection.sourcePortId`)
3. Find target port (via `connection.targetNodeId` + `connection.targetPortId`)
4. Copy source port with `dataType = ipType.payloadType`, update source node
5. Copy target port with `dataType = ipType.payloadType`, update target node
6. Replace flowGraph with all updates

## Interaction 4: Persistence Round-Trip

**Save** (existing, no changes needed):
- Port: `input("seconds", Int::class)` - `dataType.simpleName` used
- Connection: `source.output("x") connect target.input("y") withType "ip_int"` - `ipTypeId` used

**Load** (existing, no changes needed):
- Port: Parser creates `Port(dataType = Int::class, ...)`
- Connection: Parser creates `Connection(ipTypeId = "ip_int", ...)`

**Verification**: After save → load, port type dropdown should show the same selection as before save.
