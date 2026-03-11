# Quickstart: Remove Repository Module

**Feature**: 048-remove-entity-module | **Date**: 2026-03-11

## Overview

This feature adds a "Remove Repository Module" button to the IP Type Properties Panel. When a module exists for an IP Type, the button replaces the disabled "Module exists" label. Clicking it shows a confirmation dialog, then removes all generated artifacts.

## Implementation Steps

### Step 1: Add `removeEntityModule()` to ModuleSaveService

Add a method that reverses `saveEntityModule()`:

```kotlin
fun removeEntityModule(
    entityName: String,
    moduleName: String,
    moduleDir: File,
    persistenceDir: File,
    projectDir: File,
    customNodeRepository: FileCustomNodeRepository,
    sourceIPTypeId: String
): RemovalResult
```

Operations (each tolerant of missing artifacts):
1. Remove custom nodes matching `sourceIPTypeId`
2. Delete module directory recursively
3. Delete `{Entity}Entity.kt`, `{Entity}Dao.kt`, `{Entity}Repository.kt` from persistence dir
4. Regenerate `AppDatabase.kt` from remaining entities
5. Remove `include(":ModuleName")` from `settings.gradle.kts`
6. Remove `implementation(project(":ModuleName"))` from `graphEditor/build.gradle.kts`

### Step 2: Update PropertiesPanel UI

In `IPTypePropertiesPanel`, replace the disabled button with a "Remove Repository Module" button when `moduleExists` is true:

```kotlin
if (moduleExists) {
    Button(onClick = { onRemoveRepositoryModule() }) {
        Text("Remove Repository Module")
    }
} else {
    Button(onClick = { onCreateRepositoryModule() }) {
        Text("Create Repository Module")
    }
}
```

### Step 3: Add Confirmation Dialog in Main.kt

Add `AlertDialog` state and composable:
- `showRemoveConfirmDialog` state variable
- Dialog with entity name, warning text, Confirm/Cancel buttons
- On confirm: call `moduleSaveService.removeEntityModule(...)`, reload `customNodes`, update status

### Step 4: Verify

1. Create a module for a test IP Type
2. Click "Remove Repository Module"
3. Confirm in dialog
4. Verify: module directory deleted, persistence files gone, AppDatabase updated, Gradle entries removed
5. Verify: "Create Repository Module" button reappears

## Key Files

| File | Change |
|------|--------|
| `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt` | Add `removeEntityModule()` |
| `graphEditor/src/jvmMain/kotlin/ui/PropertiesPanel.kt` | Replace disabled label with remove button |
| `graphEditor/src/jvmMain/kotlin/Main.kt` | Add confirmation dialog, removal lambda, state |
