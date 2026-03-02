# Data Model: Entity Repository Nodes

**Feature**: 036-entity-repository-nodes
**Date**: 2026-03-01

## Entities

### RepositoryNodeDefinition (extends CustomNodeDefinition)

Represents a repository node created from a custom IP type. Stored alongside regular custom node definitions.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | String (UUID) | Yes | Unique identifier |
| name | String | Yes | Node name (e.g., "UserRepository") |
| inputCount | Int | Yes | Always 3 (save, update, remove) |
| outputCount | Int | Yes | Always 2 (result, error) |
| genericType | String | Yes | Always "in3out2" |
| createdAt | Long | Yes | Creation timestamp |
| anyInput | Boolean | Yes | Always false for repository nodes |
| isRepository | Boolean | Yes | Always true — distinguishes from regular custom nodes |
| sourceIPTypeId | String | Yes | ID of the custom IP type this repository is for |
| sourceIPTypeName | String | Yes | Name of the source IP type (for display when type is deleted) |

**Relationships**:
- References a `CustomIPTypeDefinition` via `sourceIPTypeId`
- Converts to `NodeTypeDefinition` for palette display (via `toNodeTypeDefinition()`)
- Persisted in `~/.codenode/custom-nodes.json` alongside regular custom nodes

**Validation rules**:
- `name` must be unique among all custom nodes
- `sourceIPTypeId` must reference a valid custom IP type at creation time
- Cannot create duplicate repository nodes for the same `sourceIPTypeId`

### Generated Entity (code generation output)

Represents a Room database entity generated from a custom IP type's properties.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | Long | Yes | Auto-generated primary key |
| {property.name} | {mapped type} | Per IPProperty.isRequired | One column per IP type property |

**Type mapping from IPProperty**:
| IPProperty typeId | Kotlin Type | Room Column Type |
|-------------------|-------------|------------------|
| ip_int | Int | INTEGER |
| ip_double | Double | REAL |
| ip_boolean | Boolean | INTEGER (0/1) |
| ip_string | String | TEXT |
| ip_any | String | TEXT (serialized) |

**Constraints**:
- Primary key `id` is auto-generated (`@PrimaryKey(autoGenerate = true)`)
- Table name derived from IP type name (lowercased, pluralized: "User" → "users")
- Only primitive IP types supported as property types (custom/nested types not supported)

### Generated DAO (code generation output)

| Method | Signature | Description |
|--------|-----------|-------------|
| insert | `suspend fun insert(obj: T)` | Insert with REPLACE conflict strategy |
| update | `suspend fun update(obj: T)` | Update by primary key |
| delete | `suspend fun delete(obj: T)` | Delete by primary key |
| getAllAsFlow | `fun getAllAsFlow(): Flow<List<T>>` | Reactive observe-all query |

**Inherits from**: `BaseDao<T>` for insert/update/delete
**Entity-specific**: `@Query("SELECT * FROM {tableName}")` for getAllAsFlow

### Generated Repository (code generation output)

| Method | Signature | Description |
|--------|-----------|-------------|
| save | `suspend fun save(item: T)` | Delegates to DAO insert |
| update | `suspend fun update(item: T)` | Delegates to DAO update |
| remove | `suspend fun remove(item: T)` | Delegates to DAO delete |
| observeAll | `fun observeAll(): Flow<List<T>>` | Delegates to DAO getAllAsFlow |

### Generated Database (code generation output)

| Field | Type | Description |
|-------|------|-------------|
| entities | List<KClass> | All entity classes registered from repository nodes |
| version | Int | Database schema version (starts at 1) |
| daos | Abstract methods | One abstract method per entity DAO |

**Singleton access**: Via generated `DatabaseModule` object.

## Relationships

```
CustomIPTypeDefinition (source)
    │
    ├── 1:1 ──→ RepositoryNodeDefinition (in custom-nodes.json)
    │               │
    │               └── generates ──→ NodeTypeDefinition (for palette)
    │
    └── 1:1 ──→ Generated Entity (in persistence/ package)
                    │
                    ├── 1:1 ──→ Generated DAO (extends BaseDao)
                    │               │
                    │               └── 1:1 ──→ Generated Repository
                    │
                    └── N:1 ──→ Generated Database (shared singleton)
```

## Port Configuration for Repository Nodes

### Input Ports (3)

| Port Name | Port Type | Connected Operation |
|-----------|-----------|-------------------|
| save | {SourceIPType} | Repository.save(item) |
| update | {SourceIPType} | Repository.update(item) |
| remove | {SourceIPType} | Repository.remove(item) |

### Output Ports (2)

| Port Name | Port Type | Connected Operation |
|-----------|-----------|-------------------|
| result | {SourceIPType} | Repository.observeAll() reactive stream |
| error | String | Error messages from failed operations |
