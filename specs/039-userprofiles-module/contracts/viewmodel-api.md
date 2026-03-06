# Contract: UserProfilesViewModel API

**Feature**: 039-userprofiles-module
**Date**: 2026-03-05

## UserProfilesViewModel

### Existing Properties (no changes)

| Property       | Type                       | Description                    |
|----------------|----------------------------|--------------------------------|
| result         | StateFlow\<Any?\>          | Operation result from flow     |
| error          | StateFlow\<Any?\>          | Error from flow                |
| executionState | StateFlow\<ExecutionState\> | Current execution state        |

### New Properties

| Property         | Type                                       | Description                          |
|------------------|--------------------------------------------|--------------------------------------|
| profiles         | StateFlow\<List\<UserProfileEntity\>\>     | Reactive list of all profiles        |

### Existing Methods (no changes)

| Method   | Parameters | Returns   | Description              |
|----------|------------|-----------|--------------------------|
| start()  | none       | FlowGraph | Start the flow execution |
| stop()   | none       | FlowGraph | Stop the flow execution  |
| reset()  | none       | FlowGraph | Reset all state          |
| pause()  | none       | FlowGraph | Pause execution          |
| resume() | none       | FlowGraph | Resume execution         |

### New Methods (FR-016)

| Method                | Parameters                        | Returns | Description                              |
|-----------------------|-----------------------------------|---------|------------------------------------------|
| addEntity()           | userProfile: UserProfileEntity    | Unit    | Triggers save via UserProfilesState._save |
| updateEntity()        | userProfile: UserProfileEntity    | Unit    | Triggers update via UserProfilesState._update |
| removeEntity()        | userProfile: UserProfileEntity    | Unit    | Triggers remove via UserProfilesState._remove |

## UserProfiles Composable Contract

```
@Composable
fun UserProfiles(
    viewModel: UserProfilesViewModel,
    modifier: Modifier = Modifier
)
```

**Responsibilities**:
- Display "UserProfiles" heading
- Display scrollable list of profiles (or empty-state message)
- Display Add/Update/Remove button row
- Manage row selection state
- Show/hide AddUpdateUserProfile form
- Show/hide remove confirmation dialog

## AddUpdateUserProfile Composable Contract

```
@Composable
fun AddUpdateUserProfile(
    existingProfile: UserProfileEntity? = null,
    onSave: (UserProfileEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Responsibilities**:
- Display name, age, isActive input fields
- Pre-populate fields when `existingProfile` is non-null
- Disable submit button when name is empty
- Call `onSave` with constructed UserProfileEntity on submit
- Call `onCancel` on cancel

## UserProfileRow Composable Contract

```
@Composable
fun UserProfileRow(
    profile: UserProfileEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Responsibilities**:
- Display name, age, isActive for a single profile
- Visual indication of selected state
- Handle click for selection
