# Tasks: UserProfiles Module

**Input**: Design documents from `/specs/039-userprofiles-module/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/viewmodel-api.md, quickstart.md

**Tests**: Not explicitly requested — test tasks omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Build Configuration)

**Purpose**: Register the UserProfiles module in Gradle, add Room/KSP/SQLite dependencies, and enable KMPMobileApp to depend on UserProfiles.

- [ ] T001 Add Room/KSP plugins and `include(":UserProfiles")` to `settings.gradle.kts` — add `id("com.google.devtools.ksp") version "2.1.21-2.0.1"` and `id("androidx.room") version "2.8.4"` to `pluginManagement.plugins`, and add `include(":UserProfiles")` under "Generated modules" section
- [ ] T002 [P] Add Room/KSP/SQLite dependencies to `UserProfiles/build.gradle.kts` — add `id("com.google.devtools.ksp")` and `id("androidx.room")` plugins, add `implementation("androidx.room:room-runtime:2.8.4")` and `implementation("androidx.sqlite:sqlite-bundled:2.6.2")` to commonMain dependencies, add `ksp("androidx.room:room-compiler:2.8.4")` to dependencies block, and add `room { schemaDirectory("$projectDir/schemas") }` configuration
- [ ] T003 [P] Add UserProfiles dependency to `KMPMobileApp/build.gradle.kts` — add `implementation(project(":UserProfiles"))` to `commonMain.dependencies`

**Checkpoint**: `./gradlew :UserProfiles:compileKotlinJvm` compiles without errors

---

## Phase 2: Foundational (Processing Logic + ViewModel)

**Purpose**: Implement the data flow backbone — processing logic that bridges the FlowGraph to Room, and ViewModel methods that drive the UI.

**CRITICAL**: No user story UI work can begin until this phase is complete.

- [ ] T004 Implement `userProfileRepositoryTick` processing logic in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/processingLogic/UserProfileRepositoryProcessLogic.kt` — replace the TODO stub with logic that accesses `DatabaseModule.getDatabase().userProfileDao()` to create a `UserProfileRepository`, dispatches to save/update/remove based on which input is non-null, and returns `ProcessResult2(result, error)`. Handle errors gracefully with try/catch returning error on output port 2.
- [ ] T005 Add `_profiles` StateFlow to `UserProfilesState` and `profiles` property + CRUD methods to `UserProfilesViewModel` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesViewModel.kt` — add `_profiles: MutableStateFlow<List<UserProfileEntity>>` initialized to `emptyList()` in `UserProfilesState`, add `profiles: StateFlow<List<UserProfileEntity>>` to ViewModel, add `addEntity(userProfile: UserProfileEntity)` that sets `UserProfilesState._save.value`, `updateEntity(userProfile: UserProfileEntity)` that sets `UserProfilesState._update.value`, and `removeEntity(userProfileId: Long)` that sets `UserProfilesState._remove.value`. Initialize a coroutine in the ViewModel that collects `UserProfileRepository.observeAll()` into `_profiles`.

**Checkpoint**: ViewModel exposes `profiles`, `addEntity()`, `updateEntity()`, `removeEntity()` and the processing logic connects to Room.

---

## Phase 3: User Story 1 + 5 - View User Profiles List + Database Foundation (Priority: P1) MVP

**Goal**: Display a scrollable list of user profiles from the repository with empty-state handling. Database persists data across restarts.

**Independent Test**: Launch the app, verify empty-state message. Add a profile via the ViewModel directly, verify it appears in the list. Restart the app, verify persistence.

### Implementation for User Stories 1 + 5

- [ ] T006 [US1] Implement `UserProfileRow` composable in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/UserProfiles.kt` — create a `UserProfileRow(profile: UserProfileEntity, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier)` composable that displays name, age (or "N/A" if null), and isActive status in a row with visual highlight when selected. Fix the existing import from `io.codenode.userprofiles.generated.UserProfilesViewModel` to `io.codenode.userprofiles.UserProfilesViewModel`.
- [ ] T007 [US1] Implement the `UserProfiles` screen composable in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/UserProfiles.kt` — replace the stub with a full screen: "UserProfiles" heading at top, scrollable `LazyColumn` of `UserProfileRow` items (or empty-state "No profiles yet" message when list is empty), row selection state management (single-select), and a button row with "Add", "Update", and "Remove" buttons below the list. "Update" and "Remove" buttons disabled when no row is selected.
- [ ] T008 [US1] Wire KMPMobileApp with bottom navigation and UserProfiles screen in `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/App.kt` — refactor `MainContent()` to use a bottom navigation bar (or tab row) with two tabs: "StopWatch" and "UserProfiles". Each tab shows its respective full-screen composable. Create `UserProfilesController` and `UserProfilesViewModel` in `remember` blocks (same pattern as StopWatch). Auto-start the UserProfiles flow. Import `userProfilesFlowGraph` from `io.codenode.userprofiles`.

**Checkpoint**: App launches with tab navigation. UserProfiles tab shows empty-state message. StopWatch tab continues to work.

---

## Phase 4: User Story 2 - Add a User Profile (Priority: P1)

**Goal**: Users can add new profiles via a form and see them appear reactively in the list.

**Independent Test**: Tap Add, fill in name/age/isActive, tap Add button on form, verify profile appears in scrollable list.

### Implementation for User Story 2

- [ ] T009 [US2] Create `AddUpdateUserProfile` composable in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/AddUpdateUserProfile.kt` — create a new file with `AddUpdateUserProfile(existingProfile: UserProfileEntity? = null, onSave: (UserProfileEntity) -> Unit, onCancel: () -> Unit, modifier: Modifier)` composable. Include: `OutlinedTextField` for name (required — submit disabled when empty), `OutlinedTextField` for age (optional, number input), `Checkbox` or `Switch` for isActive (default true), "Cancel" button calling `onCancel`, and submit button labeled "Add" when `existingProfile` is null or "Update" when non-null, calling `onSave` with the constructed `UserProfileEntity`.
- [ ] T010 [US2] Wire Add button in `UserProfiles` screen to show `AddUpdateUserProfile` form in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/UserProfiles.kt` — add a `showAddForm` boolean state. When "Add" is tapped, show the `AddUpdateUserProfile` composable (with `existingProfile = null`). On save, call `viewModel.addEntity(profile)` and hide the form. On cancel, hide the form.

**Checkpoint**: Tap Add → fill form → tap Add → profile appears in list reactively. Cancel dismisses form without saving.

---

## Phase 5: User Story 3 - Update a User Profile (Priority: P2)

**Goal**: Users can select a profile, edit it via the form, and see changes reflected reactively.

**Independent Test**: Select a profile row, tap Update, modify a field, tap Update, verify change appears in list.

### Implementation for User Story 3

- [ ] T011 [US3] Wire Update button to show `AddUpdateUserProfile` in update mode in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/UserProfiles.kt` — add a `showUpdateForm` boolean state. When "Update" is tapped (with a row selected), show `AddUpdateUserProfile` with `existingProfile` set to the selected profile. On save, call `viewModel.updateEntity(profile)` and hide the form. On cancel, hide the form. Clear selection after successful update.

**Checkpoint**: Select row → tap Update → form pre-populated → modify → tap Update → list reflects changes. Cancel discards changes.

---

## Phase 6: User Story 4 - Remove a User Profile (Priority: P2)

**Goal**: Users can select a profile, tap Remove, confirm deletion, and see the profile disappear reactively.

**Independent Test**: Select a profile row, tap Remove, confirm in dialog, verify profile disappears from list.

### Implementation for User Story 4

- [ ] T012 [US4] Add removal confirmation dialog to `UserProfiles` screen in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/UserProfiles.kt` — add a `showRemoveConfirmation` boolean state. When "Remove" is tapped (with a row selected), show an `AlertDialog` asking "Remove this profile?" with "Confirm" and "Cancel" buttons. On confirm, call `viewModel.removeEntity(selectedProfile.id)`, clear selection, and dismiss dialog. On cancel, dismiss dialog.

**Checkpoint**: Select row → tap Remove → confirmation dialog → Confirm → profile disappears. Cancel keeps profile.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup.

- [ ] T013 Clear selection when selected profile is deleted by another process — in `UserProfiles` screen, check if `selectedProfileId` still exists in the `profiles` list on each recomposition; if not, reset selection to null
- [ ] T014 Run quickstart.md end-to-end validation — follow the manual test flow in `specs/039-userprofiles-module/quickstart.md`: launch app, verify empty state, add profile, update profile, remove profile, verify persistence across restart

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **US1+US5 (Phase 3)**: Depends on Foundational phase completion
- **US2 (Phase 4)**: Depends on Phase 3 (needs list screen to wire Add button into)
- **US3 (Phase 5)**: Depends on Phase 4 (reuses AddUpdateUserProfile composable)
- **US4 (Phase 6)**: Depends on Phase 3 (needs list screen with selection)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **US1+US5 (P1)**: Can start after Foundational — no dependencies on other stories
- **US2 (P1)**: Depends on US1 (needs the screen with Add button)
- **US3 (P2)**: Depends on US2 (reuses the AddUpdateUserProfile form composable)
- **US4 (P2)**: Can start after US1 (needs screen with selection and Remove button); independent of US2/US3

### Within Each User Story

- ViewModel/processing logic before UI
- Screen composable before form composables
- Core flow before edge cases

### Parallel Opportunities

- T002 and T003 can run in parallel (different build.gradle.kts files)
- T006 and T008 touch different files and can run in parallel
- US4 (Phase 6) can run in parallel with US3 (Phase 5) since they modify different parts of UserProfiles.kt

---

## Parallel Example: Phase 1 Setup

```bash
# After T001 completes, launch T002 and T003 in parallel:
Task: "Add Room/KSP/SQLite dependencies to UserProfiles/build.gradle.kts"
Task: "Add UserProfiles dependency to KMPMobileApp/build.gradle.kts"
```

## Parallel Example: Phase 3 (US1+US5)

```bash
# T006 and T008 can run in parallel (different files):
Task: "Implement UserProfileRow composable in UserProfiles.kt"
Task: "Wire KMPMobileApp with bottom navigation in App.kt"
```

---

## Implementation Strategy

### MVP First (Phase 1 → 2 → 3 → 4)

1. Complete Phase 1: Setup (build configuration)
2. Complete Phase 2: Foundational (processing logic + ViewModel)
3. Complete Phase 3: US1+US5 (list view + database foundation)
4. Complete Phase 4: US2 (add profiles)
5. **STOP and VALIDATE**: Users can view, add profiles, and data persists
6. Demo MVP

### Incremental Delivery

1. Setup + Foundational → Build compiles
2. Add US1+US5 → List view works, database persists → Demo
3. Add US2 → Can add profiles → Demo
4. Add US3 → Can update profiles → Demo
5. Add US4 → Can remove profiles → Demo (full CRUD)
6. Polish → Production-ready

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- The AddUpdateUserProfile composable is created in US2 and reused in US3
- Row selection state is introduced in US1 (Phase 3) and used by US3 and US4
- The existing generated code (Controller, Flow, ControllerAdapter, ControllerInterface) requires NO changes
- The persistence layer (Entity, DAO, Repository, DatabaseModule, platform builders) requires NO changes
- Commit after each task or logical group
