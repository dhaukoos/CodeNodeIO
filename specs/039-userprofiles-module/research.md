# Research: UserProfiles Module

**Feature**: 039-userprofiles-module
**Date**: 2026-03-05

## R1: Room KMP Build Configuration

**Decision**: Add Room 2.8.4, KSP 2.1.21-2.0.1, and SQLite Bundled 2.6.2 to the UserProfiles module's build.gradle.kts, plus register the KSP and Room plugins in root settings.gradle.kts.

**Rationale**: The UserProfiles module source code already uses Room annotations (`@Entity`, `@Dao`, `@Database`, `@ConstructedBy`) and `BundledSQLiteDriver`, but these dependencies are not declared in the build file. The module cannot compile without them. These are the same versions used in the existing entity-repository infrastructure (feature 036).

**Alternatives considered**:
- SQLDelight: Rejected — the persistence layer is already written using Room KMP annotations. Switching would require rewriting all persistence code.
- Direct SQLite: Rejected — Room provides reactive Flow queries and compile-time verification via KSP, which are already leveraged.

## R2: Gradle Module Registration

**Decision**: Add `include(":UserProfiles")` to root `settings.gradle.kts` under the "Generated modules" section.

**Rationale**: The UserProfiles directory exists with full source code but is not registered as a Gradle module. Without this, `:KMPMobileApp` cannot depend on it, and the module cannot be compiled.

**Alternatives considered**:
- Embedding UserProfiles code directly in KMPMobileApp: Rejected — violates the established module-per-FlowGraph pattern used by StopWatch.

## R3: Processing Logic Implementation Pattern

**Decision**: The `userProfileRepositoryTick` block will instantiate `UserProfileRepository` via `DatabaseModule.getDatabase().userProfileDao()`, then dispatch to save/update/remove based on which input is non-null. Output port 1 returns the result (list of all profiles via `observeAll()`), output port 2 returns errors.

**Rationale**: The flow architecture uses a reactive source (`userProfileCUD`) that combines three StateFlows (`_save`, `_update`, `_remove`) into a `ProcessResult3`. When any of these changes, the processor receives all three values. Only the one that actually changed should trigger an operation. The processor needs to determine which operation to perform based on which input is non-null (or changed).

**Alternatives considered**:
- Passing the repository as a constructor parameter to the tick block: Rejected — the tick block is a top-level `val` lambda with a fixed signature (`In3Out2TickBlock`). The repository must be accessed via the singleton `DatabaseModule`.
- Separate tick blocks per operation: Rejected — the generated flow has a single 3-input processor node; the architecture requires one tick block handling all three inputs.

## R4: ViewModel CRUD Method Pattern

**Decision**: Add `addEntity(userProfile)`, `updateEntity(userProfile)`, and `removeEntity(userProfile)` methods to `UserProfilesViewModel`. These methods will write to `UserProfilesState._save`, `_update`, and `_remove` respectively, which triggers the reactive source flow. Also add a `profiles` StateFlow that observes the repository's `observeAll()` for the list display.

**Rationale**: The existing flow architecture uses `UserProfilesState` MutableStateFlows as the reactive trigger mechanism. Setting a value on `_save` fires the `combine` in `userProfileCUD`, which sends the value through the flow graph to the `userProfileRepository` processor. The ViewModel methods simply need to update these state flows.

**Alternatives considered**:
- Direct repository access from ViewModel: Rejected — bypasses the FlowGraph architecture. The CRUD operations should flow through the node graph (CUD source → Repository processor → Display sink).

## R5: Reactive Profile List Display

**Decision**: The profile list will be driven by `UserProfileRepository.observeAll()` which returns a `Flow<List<UserProfileEntity>>`. This flow will be exposed through the ViewModel as a `StateFlow<List<UserProfileEntity>>` and collected in the composable via `collectAsState()`.

**Rationale**: Room's `@Query` with Flow return type automatically emits new lists whenever the underlying table changes (insert, update, delete). This provides the reactive behavior required by the spec without manual refresh logic.

**Alternatives considered**:
- Polling the database: Rejected — Room Flow is event-driven and more efficient.
- Using the FlowGraph result output: The FlowGraph's display sink writes to `_result`, but this only receives individual operation results, not the full list. The list must come from the repository's `observeAll()` flow.

## R6: Add/Update Form Pattern

**Decision**: Create a single `AddUpdateUserProfile` composable that accepts an optional `UserProfileEntity` parameter. When null, it operates in "Add" mode with empty fields and an "Add" button. When non-null, it operates in "Update" mode with pre-populated fields and an "Update" button. Both modes share a "Cancel" button.

**Rationale**: The spec explicitly requires the same form for both operations, differing only in button labels and pre-populated values. A single composable with mode detection avoids duplication.

**Alternatives considered**:
- Two separate composables: Rejected — spec says "the same AddUpdateUserProfile view" is used for both operations. Duplication would violate DRY.

## R7: KMPMobileApp Integration

**Decision**: Use a bottom navigation or tab system in `App.kt` to switch between separate screens — one for the StopWatch module and one for the UserProfiles module. Each tab gets its own full-screen composable. The UserProfilesController will be created in a `remember` block (same pattern as StopWatch), wrapped in an adapter, and passed to the ViewModel which drives the UserProfiles screen.

**Rationale**: As the app grows to host multiple modules, vertical stacking becomes unwieldy. A bottom navigation / tab system provides clear separation, allows each module to own its full screen real estate, and follows standard mobile app conventions. Both controllers persist across tab switches via `remember` so state is not lost when navigating between tabs.

**Alternatives considered**:
- Vertical stacking with section headers: Rejected — does not scale as modules are added, and forces users to scroll between unrelated features. Tab-based navigation provides a cleaner UX.
