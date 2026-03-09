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

**Goal**: Inject UserProfileDao into UserProfiles module via dependency injection instead of calling DatabaseModule directly.

**Independent Test**: Build and run via `./gradlew :graphEditor:run`. Open UserProfiles in Runtime Preview, perform Add/Update/Remove — all CRUD operations work. Verify UserProfiles source has zero references to `DatabaseModule`.

### Implementation for User Story 1

- [ ] T001 [US1] Create `UserProfilesPersistence` initialization object in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesPersistence.kt` — nullable `_dao` field, `initialize(dao)` method, `getDao()` with clear error message
- [ ] T002 [US1] Modify `UserProfilesViewModel` to accept `UserProfileDao` as constructor parameter in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesViewModel.kt` — replace `DatabaseModule.getDatabase().userProfileDao()` with injected DAO
- [ ] T003 [US1] Modify `UserProfileRepositoryProcessLogic` to use `UserProfilesPersistence.getDao()` in `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/processingLogic/UserProfileRepositoryProcessLogic.kt` — replace `DatabaseModule.getDatabase().userProfileDao()` call
- [ ] T004 [US1] Modify `ModuleSessionFactory` to wire DAO into UserProfiles at session creation in `graphEditor/src/jvmMain/kotlin/ui/ModuleSessionFactory.kt` — obtain DAO from `DatabaseModule.getDatabase().userProfileDao()`, call `UserProfilesPersistence.initialize(dao)`, pass DAO to ViewModel constructor
- [ ] T005 [US1] Remove `import io.codenode.persistence.DatabaseModule` from all UserProfiles source files and verify zero references remain
- [ ] T006 [US1] Build verification: run `./gradlew :UserProfiles:compileKotlinJvm` and `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: UserProfiles no longer calls DatabaseModule directly. All DAO access flows through injection. CRUD operations work identically.

---

## Phase 4: User Story 2 — Move Entity/DAO Classes Back to Feature Module (Priority: P2)

**Goal**: Move UserProfileEntity, UserProfileDao, and UserProfileRepository from persistence module back to UserProfiles. Remove BaseDao inheritance from UserProfileDao. Invert dependency direction so persistence depends on UserProfiles.

**Independent Test**: Build succeeds. UserProfiles contains entity/DAO/repo files. Persistence module has no feature-specific types. CRUD operations work.

### Implementation for User Story 2

- [ ] T007 [P] [US2] Move `UserProfileEntity.kt` from `persistence/src/commonMain/kotlin/io/codenode/persistence/UserProfileEntity.kt` to `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/UserProfileEntity.kt` — update package to `io.codenode.userprofiles.persistence`
- [ ] T008 [P] [US2] Move `UserProfileDao.kt` from `persistence/src/commonMain/kotlin/io/codenode/persistence/UserProfileDao.kt` to `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/UserProfileDao.kt` — update package, remove `BaseDao` extends, add own `@Insert`, `@Update`, `@Delete` methods with Room annotations
- [ ] T009 [P] [US2] Move `UserProfileRepository.kt` from `persistence/src/commonMain/kotlin/io/codenode/persistence/UserProfileRepository.kt` to `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/UserProfileRepository.kt` — update package and imports
- [ ] T010 [US2] Delete original files from persistence module: `UserProfileEntity.kt`, `UserProfileDao.kt`, `UserProfileRepository.kt` in `persistence/src/commonMain/kotlin/io/codenode/persistence/`
- [ ] T011 [US2] Update `persistence/build.gradle.kts` — add `implementation(project(":UserProfiles"))` dependency so AppDatabase can reference UserProfileEntity
- [ ] T012 [US2] Update `AppDatabase.kt` in `persistence/src/commonMain/kotlin/io/codenode/persistence/AppDatabase.kt` — change import of UserProfileEntity and UserProfileDao to `io.codenode.userprofiles.persistence.*`
- [ ] T013 [US2] Remove `project(":persistence")` dependency from `UserProfiles/build.gradle.kts`
- [ ] T014 [US2] Add `implementation(project(":persistence"))` to `graphEditor/build.gradle.kts` (if not already present) so graphEditor can access DatabaseModule directly
- [ ] T015 [US2] Update all import paths across UserProfiles source files that reference `io.codenode.persistence.UserProfileEntity`, `io.codenode.persistence.UserProfileDao`, or `io.codenode.persistence.UserProfileRepository` to use `io.codenode.userprofiles.persistence.*`
- [ ] T016 [US2] Build verification: run `./gradlew :persistence:compileKotlinJvm`, `./gradlew :UserProfiles:compileKotlinJvm`, and `./gradlew :graphEditor:compileKotlinJvm`

**Checkpoint**: Entity/DAO/repo files live in UserProfiles. Persistence module contains only generic infrastructure (BaseDao, AppDatabase, DatabaseModule, DatabaseBuilder.*). Dependency direction is inverted: persistence → UserProfiles.

---

## Phase 5: User Story 3 — Centralize Database Assembly in App Layer (Priority: P3)

**Goal**: Ensure each app entry point wires DAOs at startup. Verify architecture matches target dependency graph.

**Independent Test**: Both graphEditor and KMPMobileApp build and wire DAOs correctly at startup.

### Implementation for User Story 3

- [ ] T017 [US3] Modify `KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/MainActivity.kt` — wire DAO from DatabaseModule into UserProfilesPersistence after database context initialization
- [ ] T018 [US3] Build verification: run `./gradlew :KMPMobileApp:compileKotlinAndroid` (or equivalent Android compile task)

**Checkpoint**: All app entry points wire DAOs at startup. Architecture matches target dependency graph.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification across all user stories

- [ ] T019 Verify `UserProfiles` source code has zero references to `DatabaseModule` — search all `.kt` files in `UserProfiles/`
- [ ] T020 Verify `UserProfiles/build.gradle.kts` has NO `project(":persistence")` dependency
- [ ] T021 Verify `persistence/build.gradle.kts` has `project(":UserProfiles")` dependency
- [ ] T022 Verify `persistence/src/commonMain/kotlin/io/codenode/persistence/` does NOT contain UserProfileEntity, UserProfileDao, or UserProfileRepository
- [ ] T023 Verify no circular dependencies: `UserProfiles` does NOT depend on `persistence`
- [ ] T024 Run quickstart.md validation scenarios via `./gradlew :graphEditor:run`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No tasks needed
- **Foundational (Phase 2)**: No tasks needed
- **User Story 1 (Phase 3)**: Can start immediately — no dependencies on other stories
- **User Story 2 (Phase 4)**: Depends on US1 completion (T001-T006) — entities can only move back after DI breaks the circular dependency
- **User Story 3 (Phase 5)**: Depends on US2 completion (T007-T016) — app layer wiring assumes inverted dependency direction
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Independent — can start immediately
- **User Story 2 (P2)**: Depends on US1 — UserProfileDao must not need BaseDao (which requires DI to be in place first)
- **User Story 3 (P3)**: Depends on US2 — wiring assumes entity types live in UserProfiles

### Within Each User Story

- T001 before T002/T003 (UserProfilesPersistence must exist before consumers use it)
- T002 and T003 can run in parallel after T001
- T004 depends on T001 (wires UserProfilesPersistence)
- T007/T008/T009 can run in parallel (different files)
- T010 depends on T007/T008/T009 (delete originals after copies are in place)
- T011/T012/T013/T014 can follow T010

### Parallel Opportunities

- T002 and T003 [P] after T001
- T007, T008, T009 [P] (moving three files simultaneously)

---

## Parallel Example: User Story 2

```bash
# Move all three files in parallel:
Task T007: "Move UserProfileEntity.kt to UserProfiles/persistence/"
Task T008: "Move UserProfileDao.kt to UserProfiles/persistence/"
Task T009: "Move UserProfileRepository.kt to UserProfiles/persistence/"

# Then sequentially:
Task T010: "Delete originals from persistence module"
Task T011: "Update persistence/build.gradle.kts dependencies"
Task T012: "Update AppDatabase.kt imports"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 3: User Story 1 (T001-T006)
2. **STOP and VALIDATE**: Build succeeds, CRUD works, zero DatabaseModule references in UserProfiles
3. This alone delivers the core architectural benefit

### Incremental Delivery

1. US1 → DI in place, UserProfiles decoupled from DatabaseModule
2. US2 → Entity/DAO classes back in UserProfiles, dependency inverted
3. US3 → App layer wiring verified for all entry points
4. Each story builds on the previous, with validation at each checkpoint

---

## Notes

- This is a pure structural refactoring — no behavior changes
- No tests requested — verification via manual Runtime Preview scenarios
- The key constraint is the sequential dependency: US1 → US2 → US3
- Total: 24 tasks across 4 active phases
