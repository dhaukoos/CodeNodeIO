# Removal API Contract

**Feature**: 048-remove-entity-module | **Date**: 2026-03-11

## ModuleSaveService.removeEntityModule()

### Input

| Parameter | Type | Description |
|-----------|------|-------------|
| entityName | String | PascalCase entity name (e.g., "GeoLocation") |
| moduleName | String | Plural module name (e.g., "GeoLocations") |
| moduleDir | File | Path to the module directory |
| persistenceDir | File | Path to persistence source directory |
| projectDir | File | Project root directory (contains settings.gradle.kts) |
| customNodeRepository | FileCustomNodeRepository | Repository for custom node CRUD |
| sourceIPTypeId | String | UUID of the source IP Type |

### Output

Returns a summary string describing what was removed (e.g., "Removed GeoLocations module: 3 nodes, module directory, 3 persistence files, AppDatabase updated, 2 gradle entries").

### Behavior

1. Find and remove all custom nodes where `sourceIPTypeId` matches
2. Delete `moduleDir` recursively (skip if not exists)
3. Delete `{entityName}Entity.kt`, `{entityName}Dao.kt`, `{entityName}Repository.kt` from `persistenceDir` (skip missing)
4. Regenerate `AppDatabase.kt` from remaining `*Entity.kt` files in `persistenceDir`
5. Remove `include(":moduleName")` line from `{projectDir}/settings.gradle.kts`
6. Remove `implementation(project(":moduleName"))` line from `{projectDir}/graphEditor/build.gradle.kts`
7. Return summary string

### Error Handling

- Each step is independent; failure in one does not prevent others
- Missing files/directories are silently skipped (counted as 0 in summary)
- File I/O exceptions are caught and reported in summary

## UI Contract

### PropertiesPanel

| State | UI Element | Action |
|-------|-----------|--------|
| No module exists | "Create Repository Module" button (enabled) | Triggers creation |
| Module exists | "Remove Repository Module" button (enabled) | Shows confirmation dialog |

### Confirmation Dialog

| Element | Content |
|---------|---------|
| Title | "Remove Module" |
| Message | "Are you sure you want to remove the {EntityName} module? This will delete the module directory, persistence files, and Gradle entries." |
| Confirm button | "Remove" |
| Cancel button | "Cancel" |
