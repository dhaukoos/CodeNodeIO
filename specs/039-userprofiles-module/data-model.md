# Data Model: UserProfiles Module

**Feature**: 039-userprofiles-module
**Date**: 2026-03-05

## Entities

### UserProfileEntity (existing)

Persisted to Room database table `userprofiles`.

| Field    | Type     | Constraints                          |
|----------|----------|--------------------------------------|
| id       | Long     | Primary key, auto-generated          |
| name     | String   | Required, non-empty                  |
| age      | Int?     | Optional (nullable)                  |
| isActive | Boolean  | Required                             |

### UserProfilesState (existing, modified)

In-memory reactive state singleton for FlowGraph data flow.

| Field           | Type                              | Purpose                              |
|-----------------|-----------------------------------|--------------------------------------|
| _save           | MutableStateFlow\<Any?\>          | Triggers save operation in flow      |
| _update         | MutableStateFlow\<Any?\>          | Triggers update operation in flow    |
| _remove         | MutableStateFlow\<Any?\>          | Triggers remove operation in flow    |
| _result         | MutableStateFlow\<Any?\>          | Receives operation result from flow  |
| _error          | MutableStateFlow\<Any?\>          | Receives error from flow             |
| _profiles       | MutableStateFlow\<List\<UserProfileEntity\>\> | **NEW** - Observable list from repository |

## State Transitions

### CRUD Operation Flow

```
User Action → ViewModel Method → UserProfilesState._save/_update/_remove
    → userProfileCUD (reactive source) emits via combine().drop(1).collect()
    → userProfileRepository processor (tick block)
        → DatabaseModule.getDatabase().userProfileDao() → Room operation
    → userProfilesDisplay sink → UserProfilesState._result/_error
```

### Profile List Reactivity

```
Room table change (insert/update/delete)
    → UserProfileDao.getAllAsFlow() emits new List<UserProfileEntity>
    → UserProfileRepository.observeAll()
    → ViewModel.profiles StateFlow
    → UI recomposes via collectAsState()
```

### UI State Machine

```
ListScreen (default)
    ├── [Add tapped] → AddFormScreen
    │   ├── [Cancel] → ListScreen
    │   └── [Add submitted] → ListScreen (list updates reactively)
    ├── [Row selected] → ListScreen (selection highlighted)
    │   ├── [Update tapped] → UpdateFormScreen (pre-populated)
    │   │   ├── [Cancel] → ListScreen
    │   │   └── [Update submitted] → ListScreen (list updates reactively)
    │   └── [Remove tapped] → ConfirmDialog
    │       ├── [Cancel] → ListScreen
    │       └── [Confirm] → ListScreen (list updates reactively)
    └── [No selection] → Add enabled, Update/Remove disabled

```

## Validation Rules

| Rule                           | Source      | Enforcement Point      |
|--------------------------------|-------------|------------------------|
| Name must be non-empty         | FR-018      | Form submit button disabled |
| Age is optional (nullable)     | FR-019      | Form allows empty age  |
| isActive defaults to true      | Assumption  | Form default value     |
| Row must be selected for Update/Remove | FR-013, FR-021 | Button disabled state |
