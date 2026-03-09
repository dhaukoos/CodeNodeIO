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

**Purpose**: Create the new persistence module and register it in the project.

- [x] T001 Add `include(":persistence")` to `settings.gradle.kts` — add under the "Generated modules" comment or as a new "Shared modules" section
- [x] T002 Create `persistence/build.gradle.kts` — configure Kotlin Multiplatform with JVM, Android, and iOS targets; add Room plugin, KSP room-compiler processors (JVM + Android + iOS), room-runtime, sqlite-bundled dependencies, and Room schema directory. Model after `UserProfiles/build.gradle.kts` structure but with NO project dependencies, package `io.codenode.persistence`, and `com.android.library` plugin.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No foundational tasks — setup provides the module structure.

(No tasks.)

---

## Phase 3: User Story 1 - Extract Persistence into a Shared Module (Priority: P1) MVP

**Goal**: Move all persistence files from UserProfiles to the new `persistence` module. Update package declarations, imports, and build configuration. Remove persistence build config from UserProfiles.

**Independent Test**: Build the application (`./gradlew :persistence:compileKotlinJvm :UserProfiles:compileKotlinJvm :graphEditor:compileKotlinJvm`). Run graphEditor, open UserProfiles module in Runtime Preview, perform Add/Update/Remove operations — all succeed.

### Implementation for User Story 1

#### File Moves (all parallel — different files)

- [x] T003 [P] [US1] Move `AppDatabase.kt` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` to `persistence/src/commonMain/kotlin/io/codenode/persistence/` — update package declaration to `io.codenode.persistence`, update entity import to `io.codenode.persistence.UserProfileEntity`
- [x] T004 [P] [US1] Move `BaseDao.kt` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` to `persistence/src/commonMain/kotlin/io/codenode/persistence/` — update package declaration to `io.codenode.persistence`
- [x] T005 [P] [US1] Move `DatabaseModule.kt` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` to `persistence/src/commonMain/kotlin/io/codenode/persistence/` — update package declaration to `io.codenode.persistence`
- [x] T006 [P] [US1] Move `UserProfileEntity.kt` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` to `persistence/src/commonMain/kotlin/io/codenode/persistence/` — update package declaration to `io.codenode.persistence`
- [x] T007 [P] [US1] Move `UserProfileDao.kt` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` to `persistence/src/commonMain/kotlin/io/codenode/persistence/` — update package declaration to `io.codenode.persistence`, update BaseDao import
- [x] T008 [P] [US1] Move `UserProfileRepository.kt` from `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` to `persistence/src/commonMain/kotlin/io/codenode/persistence/` — update package declaration to `io.codenode.persistence`
- [x] T009 [P] [US1] Move `DatabaseBuilder.jvm.kt` from `UserProfiles/src/jvmMain/kotlin/io/codenode/userprofiles/persistence/` to `persistence/src/jvmMain/kotlin/io/codenode/persistence/` — update package declaration to `io.codenode.persistence`
- [x] T010 [P] [US1] Move `DatabaseBuilder.android.kt` from `UserProfiles/src/androidMain/kotlin/io/codenode/userprofiles/persistence/` to `persistence/src/androidMain/kotlin/io/codenode/persistence/` — update package declaration to `io.codenode.persistence`
- [x] T011 [P] [US1] Move `DatabaseBuilder.ios.kt` from `UserProfiles/src/iosMain/kotlin/io/codenode/userprofiles/persistence/` to `persistence/src/iosMain/kotlin/io/codenode/persistence/` — update package declaration to `io.codenode.persistence`

#### Build Configuration Updates

- [x] T012 [US1] Update `UserProfiles/build.gradle.kts` — remove Room plugin (`id("com.google.devtools.ksp")`, `id("androidx.room")`), remove KSP room-compiler dependency block, remove `room {}` config block, remove `sqlite-bundled` dependency, keep `room-runtime` ONLY if needed (likely remove too since no Room annotations remain in UserProfiles). Add `implementation(project(":persistence"))` to commonMain dependencies.
- [x] T013 [US1] Update `KMPMobileApp/build.gradle.kts` — add `implementation(project(":persistence"))` to commonMain dependencies (for transitive access and explicit dependency declaration)

#### Import Updates (all parallel — different files)

- [x] T014 [P] [US1] Update `UserProfile.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/` — change `UserProfileEntity` import from `io.codenode.userprofiles.persistence` to `io.codenode.persistence`
- [x] T015 [P] [US1] Update `UserProfilesViewModel.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/` — change `DatabaseModule`, `UserProfileEntity`, `UserProfileRepository` imports from `io.codenode.userprofiles.persistence` to `io.codenode.persistence`
- [x] T016 [P] [US1] Update `UserProfileRepositoryProcessLogic.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/processingLogic/` — change `DatabaseModule`, `UserProfileEntity`, `UserProfileRepository` imports from `io.codenode.userprofiles.persistence` to `io.codenode.persistence`
- [x] T017 [P] [US1] Update `UserProfiles.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/` — change `UserProfileEntity` import from `io.codenode.userprofiles.persistence` to `io.codenode.persistence`
- [x] T018 [P] [US1] Update `AddUpdateUserProfile.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/userInterface/` — change `UserProfileEntity` import from `io.codenode.userprofiles.persistence` to `io.codenode.persistence`
- [x] T019 [P] [US1] Update `MainActivity.kt` in `KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/` — change `initializeDatabaseContext` import from `io.codenode.userprofiles.persistence` to `io.codenode.persistence`

#### Cleanup and Verify

- [x] T020 [US1] Delete empty `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/` directory and platform-specific persistence directories (`jvmMain`, `androidMain`, `iosMain`)
- [x] T021 [US1] Delete `UserProfiles/schemas/` directory (Room schema files — these will be regenerated in persistence module)
- [x] T022 [US1] Verify build: run `./gradlew :persistence:compileKotlinJvm :UserProfiles:compileKotlinJvm :graphEditor:compileKotlinJvm` — all must succeed

**Checkpoint**: All persistence files extracted to shared module, all imports updated, application builds and runs. UserProfiles CRUD works in Runtime Preview.

---

## Phase 4: User Story 2 - Extract Source and Sink Nodes into Distinct Files (Priority: P2)

**Goal**: Extract UserProfileCUD source node and UserProfilesDisplay sink node from UserProfilesFlow.kt into their own files.

**Note**: US2 was already implemented before the US1 rewrite. T023-T026 may already be complete.

**Independent Test**: Build and run application. Open UserProfiles in Runtime Preview, perform CRUD operations — all work identically.

### Implementation for User Story 2

- [x] T023 [P] [US2] Create `UserProfileCUD.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/` — extract the `userProfileCUD` source node definition (the `createSourceOut3` call with StateFlow collectors) from `UserProfilesFlow.kt` into this new file as a `createUserProfileCUD()` factory function
- [x] T024 [P] [US2] Create `UserProfilesDisplay.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/` — extract the `userProfilesDisplay` sink node definition (the `createSinkIn2` call) from `UserProfilesFlow.kt` into this new file as a `createUserProfilesDisplay()` factory function
- [x] T025 [US2] Update `UserProfilesFlow.kt` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/generated/` — remove the extracted node definitions and import them from the new files. Keep all flow graph wiring (wireConnections, start, stop, reset) intact.
- [x] T026 [US2] Verify build: run `./gradlew :UserProfiles:compileKotlinJvm :graphEditor:compileKotlinJvm` — must succeed

**Checkpoint**: Both nodes extracted into distinct files. Module builds and runs identically.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all changes.

- [x] T027 Verify no stale imports remain — search codebase for any remaining references to `io.codenode.userprofiles.persistence`
- [ ] T028 Run quickstart.md verification scenarios — UserProfiles CRUD operations, build verification, file structure checks, and dependency verification

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Must complete first — creates the persistence module
- **US1 (Phase 3)**: Depends on Setup — needs the module to exist before moving files
- **US2 (Phase 4)**: Independent of US1 (different files), already completed
- **Polish (Phase 5)**: Depends on US1 complete

### Within User Story 1

1. Setup first (T001-T002) — create module
2. File moves (T003-T011) — all parallel (different files)
3. Build config updates (T012-T013) — after file moves
4. Import updates (T014-T019) — all parallel, after build config
5. Cleanup and verify (T020-T022) — after all above

### Parallel Opportunities

- T003-T011 can all run in parallel (moving different files)
- T014-T019 can all run in parallel (updating different files)
- T023 and T024 can run in parallel (creating different files) — already done

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 3: User Story 1 (T003-T022)
3. **STOP and VALIDATE**: Build and test UserProfiles CRUD in Runtime Preview
4. Deploy/demo if ready

### Incremental Delivery

1. Setup → Persistence module created
2. User Story 1 → All persistence files extracted to shared module (MVP!)
3. User Story 2 → Nodes extracted into distinct files (already done)
4. Polish → Final validation across all changes

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- This is a pure structural refactoring — no behavior changes
- T001-T002 (setup) must complete before T003-T011 (file moves) to ensure persistence module can compile the moved files
- US2 (T023-T025) was already implemented before the US1 rewrite
- Commit after each phase for clean rollback if needed
- The persistence module has NO project dependencies — it only depends on Room, KSP, and SQLite libraries
