# Data Model: Port Type Selector

**Feature**: 023-port-type-selector
**Date**: 2026-02-20

## Entities

### Port (existing - no schema changes)

| Field | Type | Description |
|-------|------|-------------|
| id | String | Unique identifier |
| name | String | Human-readable port name |
| direction | Direction | INPUT or OUTPUT |
| **dataType** | KClass<T> | **Data type - updated by type selector** |
| required | Boolean | Whether connection is required |
| defaultValue | T? | Optional default payload |
| owningNodeId | String | Parent node reference |

**Key behavior**: `dataType` is updated when user selects an IP type from the dropdown. The KClass is resolved from `IPTypeRegistry.getByTypeName(typeName).payloadType`.

### Connection (existing - no schema changes)

| Field | Type | Description |
|-------|------|-------------|
| id | String | Unique identifier |
| sourceNodeId | String | Source node reference |
| sourcePortId | String | Source port reference |
| targetNodeId | String | Target node reference |
| targetPortId | String | Target port reference |
| **ipTypeId** | String? | **IP type ID - updated by propagation** |
| channelCapacity | Int | Channel buffer size |

**Key behavior**: `ipTypeId` is updated when a connected port's type changes (propagation from port to connection).

### InformationPacketType (existing - no changes)

| Field | Type | Description |
|-------|------|-------------|
| id | String | Unique identifier (e.g., "ip_int") |
| typeName | String | Display name (e.g., "Int") |
| payloadType | KClass<*> | Kotlin class reference |
| color | IPColor | RGB color for visual identification |
| description | String? | Optional description |

## Relationships

```text
Port ←──dataType──→ InformationPacketType (via KClass match)
  │
  ├── sourcePort ──→ Connection ←── targetPort
  │                      │
  │                   ipTypeId ──→ InformationPacketType (via ID)
  │
  └── owningNode ──→ CodeNode
```

## Propagation Flow

### Port Type Change → Connection + Remote Port

```text
User selects "Int" on Port A
  │
  ├─→ Port A: dataType = Int::class
  │
  ├─→ Connection 1: ipTypeId = "ip_int"
  │     └─→ Remote Port B: dataType = Int::class
  │
  └─→ Connection 2: ipTypeId = "ip_int"
        └─→ Remote Port C: dataType = Int::class
```

### Connection Type Change → Both Ports

```text
Connection ipTypeId changes to "ip_string"
  │
  ├─→ Source Port: dataType = String::class
  └─→ Target Port: dataType = String::class
```

## Validation Rules

- Port `dataType` must correspond to a registered IP type (fallback: Any::class)
- Connection `ipTypeId` must be null or a valid registered type ID
- Propagation is non-recursive (single pass, no cascading)
