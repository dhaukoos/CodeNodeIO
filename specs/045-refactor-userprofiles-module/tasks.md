# Tasks: Refactor UserProfiles Module

**Input**: Design documents from `/specs/045-refactor-userprofiles-module/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Not requested — no test tasks included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No project initialization needed — all target modules already exist.

(No tasks — existing project structure is sufficient.)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No foundational tasks — both user stories operate on existing modules.

(No tasks.)

---

## Phase 3: User Story 1 - Relocate Shared Persistence Files to App Module (Priority: P1) MVP

**Goal**: Move AppDatabase.kt, BaseDao.kt, DatabaseModule.kt (and platform-specific DatabaseBuilder files) from UserProfiles to KMPMobileApp. Update package declarations, imports, and build configuration.

**Independent Test**: Build the application (`./gradlew :graphEditor:compileKotlinJvm :KMPMobileApp:compileKotlinJvm :UserProfiles:compileKotlinJvm`). Run graphEditor, open UserProfiles module in Runtime Preview, perform Add/Update/Remove operations — all succeed.

### Implementation for User Story 1

- [ ] T001 [US1] Add Room, KSP, and SQLite dependencies to `KMPMobileApp/build.gradle.kts` — add `androidx.room` plugin, `room-runtime:2.8.4`, `sqlite-bundled:2.6.2`, KSP room-compiler processors for all targets, and Room schema directory config. Mirror the existing setup from `UserProfiles/build.gradle.kts`.
- [ ] T002 [P] [US1] Move `AppDatabase.kt` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` to `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/persistence/` — update package declaration to `io.codenode.mobileapp.persistence`
- [ ] T003 [P] [US1] Move `BaseDao.kt` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` to `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/persistence/` — update package declaration to `io.codenode.mobileapp.persistence`
- [ ] T004 [P] [US1] Move `DatabaseModule.kt` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` to `KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/persistence/` — update package declaration to `io.codenode.mobileapp.persistence`
- [ ] T005 [P] [US1] Move `DatabaseBuilder.jvm.kt` from `UserProfiles/src/jvmMain/kotlin/io/codenode/userprofiles/persistence/` to `KMPMobileApp/src/jvmMain/kotlin/io/codenode/mobileapp/persistence/` — update package declaration
- [ ] T006 [P] [US1] Move `DatabaseBuilder.android.kt` from `UserProfiles/src/androidMain/kotlin/io/codenode/userprofiles/persistence/` to `KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/persistence/` — update package declaration
- [ ] T007 [P] [US1] Move `DatabaseBuilder.ios.kt` from `UserProfiles/src/iosMain/kotlin/io/codenode/userprofiles/persistence/` to `KMPMobileApp/src/iosMain/kotlin/io/codenode/mobileapp/persistence/` — update package declaration
- [ ] T008 [US1] Update `UserProfileDao.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` — add import for `io.codenode.mobileapp.persistence.BaseDao` (since BaseDao moved)
- [ ] T009 [US1] Update `UserProfileRepositoryProcessLogic.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/processingLogic/` — change `DatabaseModule` import from `io.codenode.userprofiles.persistence` to `io.codenode.mobileapp.persistence`
- [ ] T010 [US1] Update `UserProfilesViewModel.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/` — change `DatabaseModule` import from `io.codenode.userprofiles.persistence` to `io.codenode.mobileapp.persistence`
- [ ] T011 [US1] Update `MainActivity.kt` in `KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/` — change `initializeDatabaseContext` import from `io.codenode.userprofiles.persistence` to `io.codenode.mobileapp.persistence`
- [ ] T012 [US1] Remove KSP room-compiler processors and `androidx.room` plugin from `UserProfiles/build.gradle.kts` — keep `room-runtime` dependency for entity/DAO annotations, remove `sqlite-bundled` and KSP processors
- [ ] T013 [US1] Verify build: run `./gradlew :KMPMobileApp:compileKotlinJvm :UserProfiles:compileKotlinJvm :graphEditor:compileKotlinJvm` — all must succeed

**Checkpoint**: Persistence files relocated, all imports updated, application builds and runs. UserProfiles CRUD works in Runtime Preview.

---

## Phase 4: User Story 2 - Extract Source and Sink Nodes into Distinct Files (Priority: P2)

**Goal**: Extract UserProfileCUD source node and UserProfilesDisplay sink node from UserProfilesFlow.kt into their own files.

**Independent Test**: Build and run application. Open UserProfiles in Runtime Preview, perform CRUD operations — all work identically.

### Implementation for User Story 2

- [ ] T014 [P] [US2] Create `UserProfileCUD.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/` — extract the `userProfileCUD` source node definition (the `createSourceOut3` call with StateFlow collectors) from `UserProfilesFlow.kt` into this new file
- [ ] T015 [P] [US2] Create `UserProfilesDisplay.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/` — extract the `userProfilesDisplay` sink node definition (the `createSinkIn2` call) from `UserProfilesFlow.kt` into this new file
- [ ] T016 [US2] Update `UserProfilesFlow.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/` — remove the extracted node definitions and import them from the new files. Keep all flow graph wiring (wireConnections, start, stop, reset) intact.
- [ ] T017 [US2] Verify build: run `./gradlew :UserProfiles:compileKotlinJvm :graphEditor:compileKotlinJvm` — must succeed

**Checkpoint**: Both nodes extracted into distinct files. Module builds and runs identically.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all changes.

- [ ] T018 Run quickstart.md verification scenarios — UserProfiles CRUD operations, build verification, and file structure checks
- [ ] T019 Verify no stale imports remain — search codebase for any remaining references to `io.codenode.userprofiles.persistence.AppDatabase`, `io.codenode.userprofiles.persistence.BaseDao`, or `io.codenode.userprofiles.persistence.DatabaseModule`

---

## Dependencies & Execution Order

### Phase Dependencies

- **US1 (Phase 3)**: No external dependencies — can start immediately
- **US2 (Phase 4)**: Independent of US1 (different files), can start in parallel
- **Polish (Phase 5)**: Depends on both user stories complete

### User Story Dependencies

- **User Story 1 (P1)**: Independent — persistence file relocation
- **User Story 2 (P2)**: Independent — node extraction (different files than US1)

### Within Each User Story

- US1: Build config first (T001), then file moves (T002-T007 parallel), then import updates (T008-T012), then verify (T013)
- US2: Extract both nodes in parallel (T014-T015), then update flow file (T016), then verify (T017)

### Parallel Opportunities

- T002-T007 can all run in parallel (moving different files)
- T014 and T015 can run in parallel (creating different files)
- US1 and US2 can run in parallel (touching different files)

---

## Parallel Example: User Story 1 File Moves

```bash
# These can all run in parallel (different files):
Task T002: "Move AppDatabase.kt to KMPMobileApp"
Task T003: "Move BaseDao.kt to KMPMobileApp"
Task T004: "Move DatabaseModule.kt to KMPMobileApp"
Task T005: "Move DatabaseBuilder.jvm.kt to KMPMobileApp"
Task T006: "Move DatabaseBuilder.android.kt to KMPMobileApp"
Task T007: "Move DatabaseBuilder.ios.kt to KMPMobileApp"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 3: User Story 1 (T001-T013)
2. **STOP and VALIDATE**: Build and test UserProfiles CRUD in Runtime Preview
3. Deploy/demo if ready

### Incremental Delivery

1. User Story 1 → Persistence files relocated, builds clean (MVP!)
2. User Story 2 → Nodes extracted into distinct files
3. Polish → Final validation across all changes

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- This is a pure structural refactoring — no behavior changes
- T001 (build config) must complete before T002-T007 (file moves) to ensure KMPMobileApp can compile the moved files
- Commit after each phase for clean rollback if needed
