# Data Model: Remove Repository Module

**Feature**: 048-remove-entity-module | **Date**: 2026-03-11

## Entities

### RemovalResult

Represents the outcome of a module removal operation.

| Field | Type | Description |
|-------|------|-------------|
| entityName | String | Name of the entity being removed (e.g., "GeoLocation") |
| moduleName | String | Name of the module directory removed (e.g., "GeoLocations") |
| nodesRemoved | Int | Count of custom node definitions removed (0-3) |
| moduleDirectoryDeleted | Boolean | Whether the module directory was successfully deleted |
| persistenceFilesRemoved | Int | Count of persistence files removed (0-3) |
| appDatabaseRegenerated | Boolean | Whether AppDatabase.kt was regenerated |
| gradleEntriesRemoved | Int | Count of Gradle file entries removed (0-2) |

### Existing Entities (referenced, not modified)

- **CustomNodeDefinition**: Identified by `id`, linked to IP Type via `sourceIPTypeId`. Has `isCudSource`, `isRepository`, `isDisplay` flags.
- **EntityModuleSpec**: Contains `entityName`, `pluralName`, `moduleName`, `properties` — used to derive artifact names for removal.

## State Transitions

```
IP Type with Module Created
    ├── User clicks "Remove Repository Module"
    │   └── Confirmation Dialog shown
    │       ├── Cancel → No change (return to Module Created state)
    │       └── Confirm → Removal in progress
    │           └── Removal complete
    │               ├── Success → IP Type without Module (Create button available)
    │               └── Partial success → IP Type without Module (status shows what was removed)
    └── (current) Shows disabled "Module exists" label
```

## Relationships

- One IP Type → 0 or 1 Entity Module (determined by presence of 3 custom nodes with matching `sourceIPTypeId`)
- One Entity Module → 3 Custom Node Definitions (CUD, Repository, Display)
- One Entity Module → 1 Module Directory (with ~15 generated files)
- One Entity Module → 3 Persistence Files (Entity, Dao, Repository)
- One Entity Module → 1 AppDatabase.kt entry
- One Entity Module → 2 Gradle file entries (settings.gradle.kts, build.gradle.kts)
