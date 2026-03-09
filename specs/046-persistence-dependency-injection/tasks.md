# Tasks: Persistence Dependency Injection

**Input**: Design documents from `/specs/046-persistence-dependency-injection/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: No tests requested — manual verification via Runtime Preview scenarios.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No new modules or project structure changes needed. Feature 045 already created the persistence module.

_(No setup tasks — existing project structure is sufficient.)_

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No foundational tasks needed — all work maps directly to user stories.

_(No foundational tasks.)_

---

## Phase 3: User Story 1 — Decouple Feature Modules from Database Singleton (Priority: P1) 🎯 MVP

**Goal**: Inject UserProfileDao into UserProfiles module via Koin dependency injection instead of calling DatabaseModule directly.

**Independent Test**: Build and run via `./gradlew :graphEditor:run`. Open UserProfiles in Runtime Preview, perform Add/Update/Remove — all CRUD operations work. Verify UserProfiles source has zero references to `DatabaseModule`.

### Implementation for User Story 1

- [x] T001 [US1] Create `UserProfilesPersistence` Koin-backed accessor in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesPersistence.kt` — KoinComponent with injected DAO, plus `userProfilesModule` Koin module definition
- [x] T002 [US1] Modify `UserProfilesViewModel` to accept `UserProfileDao` as constructor parameter in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesViewModel.kt` — replace `DatabaseModule.getDatabase().userProfileDao()` with injected DAO
- [x] T003 [US1] Modify `UserProfileRepositoryProcessLogic` to use `UserProfilesPersistence.dao` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/processingLogic/UserProfileRepositoryProcessLogic.kt` — replace `DatabaseModule.getDatabase().userProfileDao()` call
- [x] T004 [US1] Modify `ModuleSessionFactory` to use Koin-injected DAO in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` — implements KoinComponent, injects UserProfileDao, passes to ViewModel constructor
- [x] T005 [US1] Remove `import io.codenode.persistence.DatabaseModule` from all UserProfiles source files and verify zero references remain
- [x] T006 [US1] Add Koin startup to `graphEditor/src/jvmMain/kotlin/Main.kt` — `startKoin { }` with DAO provider module and userProfilesModule
- [x] T007 [US1] Add `implementation(project(":persistence"))` and `implementation("io.insert-koin:koin-core:4.0.0")` to `graphEditor/build.gradle.kts`

**Checkpoint**: UserProfiles no longer calls DatabaseModule directly. All DAO access flows through Koin DI. CRUD operations work identically.

---

## Phase 4: User Story 2 — Move Entity/DAO Classes Back to Feature Module (Priority: P2)

**Goal**: Move UserProfileEntity, UserProfileDao, UserProfileRepository, and AppDatabase from persistence module back to UserProfiles. Remove BaseDao inheritance from UserProfileDao. Invert dependency direction so persistence depends on UserProfiles.

**Technical approach**: Use Room's multi-module support (Room 2.8.4 + KSP2). Apply `androidx.room` and `com.google.devtools.ksp` plugins to UserProfiles. Move `AppDatabase` alongside entities so Room KSP can process them together. Persistence module retains only generic infrastructure (BaseDao, DatabaseModule, DatabaseBuilder.*).

### Implementation for User Story 2

- [x] T008 [US2] Add `com.google.devtools.ksp` and `androidx.room` plugins to `UserProfiles/build.gradle.kts`, plus KSP room-compiler dependencies and room schema config
- [x] T009 [US2] Add `implementation("androidx.room:room-runtime:2.8.4")` and `implementation("io.insert-koin:koin-core:4.0.0")` to UserProfiles dependencies, remove `project(":persistence")`
- [x] T010 [P] [US2] Move `UserProfileEntity.kt` to `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/UserProfileEntity.kt` — update package to `io.codenode.userprofiles.persistence`
- [x] T011 [P] [US2] Move `UserProfileDao.kt` to `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/UserProfileDao.kt` — update package, remove BaseDao extends, add own @Insert/@Update/@Delete methods
- [x] T012 [P] [US2] Move `UserProfileRepository.kt` to `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/UserProfileRepository.kt` — update package
- [x] T013 [US2] Move `AppDatabase.kt` to `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/AppDatabase.kt` — update package
- [x] T014 [US2] Delete original files from `persistence/src/commonMain/kotlin/io/codenode/persistence/` (UserProfileEntity, UserProfileDao, UserProfileRepository, AppDatabase)
- [x] T015 [US2] Remove KSP and Room plugins from `persistence/build.gradle.kts` (no longer has @Database), add `implementation(project(":UserProfiles"))`
- [x] T016 [US2] Update `DatabaseModule.kt` and all `DatabaseBuilder.*.kt` in persistence to import `AppDatabase` from `io.codenode.userprofiles.persistence`
- [x] T017 [US2] Update all import paths across UserProfiles source files from `io.codenode.persistence.*` to `io.codenode.userprofiles.persistence.*`
- [x] T018 [US2] Build verification: `./gradlew :UserProfiles:compileKotlinJvm`, `:persistence:compileKotlinJvm`, `:graphEditor:compileKotlinJvm`

**Checkpoint**: Entity/DAO/repo/AppDatabase live in UserProfiles. Persistence module contains only generic infrastructure (BaseDao, DatabaseModule, DatabaseBuilder.*). Dependency direction is inverted: persistence → UserProfiles. No circular dependencies.

---

## Phase 5: User Story 3 — Centralize Database Assembly in App Layer (Priority: P3)

**Goal**: Ensure each app entry point wires DAOs at startup via Koin. Verify architecture matches target dependency graph.

### Implementation for User Story 3

- [x] T019 [US3] Modify `KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/MainActivity.kt` — add Koin startup with DAO provider module and userProfilesModule
- [x] T020 [US3] Add `implementation("io.insert-koin:koin-core:4.0.0")` to `KMPMobileApp/build.gradle.kts`

**Checkpoint**: All app entry points wire DAOs at startup via Koin. Architecture matches target dependency graph.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification across all user stories

- [x] T021 Verify `UserProfiles` source code has zero references to `DatabaseModule`
- [x] T022 Verify `UserProfiles/build.gradle.kts` has NO `project(":persistence")` dependency
- [x] T023 Verify `persistence/build.gradle.kts` has `project(":UserProfiles")` dependency
- [x] T024 Verify `persistence/src/commonMain/kotlin/io/codenode/persistence/` does NOT contain UserProfileEntity, UserProfileDao, UserProfileRepository, or AppDatabase
- [x] T025 Verify no circular dependencies: `UserProfiles` does NOT depend on `persistence`
- [ ] T026 Run quickstart.md validation scenarios via `./gradlew :graphEditor:run`

---

## Dependencies & Execution Order

### Phase Dependencies

- **User Story 1 (Phase 3)**: Can start immediately
- **User Story 2 (Phase 4)**: Depends on US1 completion
- **User Story 3 (Phase 5)**: Can run alongside US2 (independent app-layer wiring)
- **Polish (Phase 6)**: Depends on all user stories being complete

### Dependency Graph (After All Stories)

```text
UserProfiles → fbpDsl                   (no persistence dependency!)
persistence → UserProfiles              (for AppDatabase type in DatabaseModule)
persistence → Room/SQLite               (external libraries)
graphEditor → persistence               (for DatabaseModule to provide DAO via Koin)
graphEditor → UserProfiles              (for controller/viewmodel)
KMPMobileApp → persistence              (for DatabaseModule)
KMPMobileApp → UserProfiles             (for UI/viewmodel)
```

---

## Notes

- Pure structural refactoring — no behavior changes
- All three user stories completed
- Room multi-module support (Room 2.8.4 + KSP2) used: AppDatabase + entities colocated in UserProfiles
- Koin 4.0.0 used for dependency injection (replaces manual UserProfilesPersistence.initialize() pattern)
- Dependency direction inverted: persistence → UserProfiles (was UserProfiles → persistence for DAO access)
- Total completed: 25 of 26 tasks (T026 manual validation remaining)
