# Data Model: Node Generator UI Tool

**Feature**: 016-node-generator
**Date**: 2026-02-15

## Entities

### 1. NodeGeneratorState

**Purpose**: Holds the current form state for the Node Generator panel.

**Fields**:
| Field | Type | Description | Validation |
|-------|------|-------------|------------|
| `name` | `String` | User-entered node name | Required, non-blank |
| `inputCount` | `Int` | Number of input ports | 0-3 |
| `outputCount` | `Int` | Number of output ports | 0-3 |
| `isValid` | `Boolean` | Computed: form is valid | See rules below |

**Validation Rules**:
- `name` must not be empty or whitespace-only
- `inputCount` must be in range [0, 3]
- `outputCount` must be in range [0, 3]
- NOT (inputCount == 0 AND outputCount == 0) — at least one port required

**Computed Properties**:
- `isValid`: `name.isNotBlank() && !(inputCount == 0 && outputCount == 0)`
- `genericType`: `"in${inputCount}out${outputCount}"` (e.g., "in2out1")

**Default Values**:
- `name`: `""` (empty)
- `inputCount`: `1`
- `outputCount`: `1`

---

### 2. CustomNodeDefinition

**Purpose**: Serializable representation of a user-created custom node type for persistence.

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Unique identifier (UUID) |
| `name` | `String` | User-provided display name |
| `inputCount` | `Int` | Number of input ports (0-3) |
| `outputCount` | `Int` | Number of output ports (0-3) |
| `genericType` | `String` | Type string (e.g., "in2out1") |
| `createdAt` | `Long` | Timestamp of creation (epoch millis) |

**Relationships**:
- Converts to `NodeTypeDefinition` via `GenericNodeTypeFactory.createGenericNodeType()`
- Stored in `CustomNodeRepository`

---

### 3. CustomNodeRepository

**Purpose**: Manages persistence of user-created custom node types.

**Operations**:
| Operation | Input | Output | Description |
|-----------|-------|--------|-------------|
| `getAll()` | - | `List<CustomNodeDefinition>` | Returns all saved custom nodes |
| `add(node)` | `CustomNodeDefinition` | `Unit` | Adds and persists a new node |
| `load()` | - | `Unit` | Loads nodes from file on startup |
| `save()` | - | `Unit` | Persists current nodes to file |

**Storage**:
- File location: `~/.codenode/custom-nodes.json`
- Format: JSON array of CustomNodeDefinition objects
- Encoding: UTF-8

**Error Handling**:
- Missing file on load: Initialize empty list
- Corrupted file on load: Log error, initialize empty list
- Write failure: Log error, continue with in-memory state

---

## State Transitions

### Node Generator Form

```
[Empty Form] ---(user enters name)---> [Name Entered]
[Name Entered] ---(name cleared)---> [Empty Form]
[Name Entered] ---(select inputs/outputs)---> [Valid Form] or [Invalid Form (0/0)]
[Valid Form] ---(click Create)---> [Node Created] ---(auto reset)---> [Empty Form]
[Valid Form] ---(click Cancel)---> [Empty Form]
[Invalid Form] ---(adjust inputs/outputs)---> [Valid Form]
```

### Create Button State

```
Disabled when:
  - name is blank OR
  - (inputCount == 0 AND outputCount == 0)

Enabled when:
  - name is non-blank AND
  - (inputCount > 0 OR outputCount > 0)
```

---

## JSON Schema: custom-nodes.json

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "array",
  "items": {
    "type": "object",
    "required": ["id", "name", "inputCount", "outputCount", "genericType", "createdAt"],
    "properties": {
      "id": {
        "type": "string",
        "description": "Unique identifier (UUID format)"
      },
      "name": {
        "type": "string",
        "minLength": 1,
        "description": "User-provided display name"
      },
      "inputCount": {
        "type": "integer",
        "minimum": 0,
        "maximum": 3,
        "description": "Number of input ports"
      },
      "outputCount": {
        "type": "integer",
        "minimum": 0,
        "maximum": 3,
        "description": "Number of output ports"
      },
      "genericType": {
        "type": "string",
        "pattern": "^in[0-3]out[0-3]$",
        "description": "Type string (e.g., in2out1)"
      },
      "createdAt": {
        "type": "integer",
        "description": "Creation timestamp (epoch milliseconds)"
      }
    }
  }
}
```

**Example**:
```json
[
  {
    "id": "custom_node_abc123",
    "name": "DataMerger",
    "inputCount": 2,
    "outputCount": 1,
    "genericType": "in2out1",
    "createdAt": 1739635200000
  },
  {
    "id": "custom_node_def456",
    "name": "TripleSplitter",
    "inputCount": 1,
    "outputCount": 3,
    "genericType": "in1out3",
    "createdAt": 1739635500000
  }
]
```
