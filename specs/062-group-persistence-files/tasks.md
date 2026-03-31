# Tasks: Group Persistence Files by Entity

**Input**: Design documents from `/specs/062-group-persistence-files/`
**Prerequisites**: plan.md, spec.md, research.md, quickstart.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No external setup needed — this feature modifies existing generators and files.

*(No tasks — existing project infrastructure is already in place.)*

---

## Phase 2: Foundational (Code Generators)

**Purpose**: Update all code generators in the CodeNodeIO tool repo to produce the new subdirectory layout. MUST be complete before user story work begins (US1 depends on correct generators, US2 depends on correct import patterns).

- [X] T001 Update `RepositoryCodeGenerator.kt` to accept a sub-package parameter and use it in `package` declarations for generated Entity, Dao, and Repository files in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RepositoryCodeGenerator.kt`
- [X] T002 Update `EntityModuleGenerator.kt` to compute entity sub-package (`persistencePackage.entityName.lowercase()`) and pass it to RepositoryCodeGenerator; change persistence file output paths to include `{Entity}/` subdirectory in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityModuleGenerator.kt`
- [X] T003 Update `EntityRepositoryCodeNodeGenerator.kt` to import `{Entity}Repository` from entity sub-package instead of flat persistence package in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityRepositoryCodeNodeGenerator.kt`
- [X] T004 [P] Update `EntityPersistenceGenerator.kt` to import `{Entity}Dao` and `{Entity}Repository` from entity sub-package in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/EntityPersistenceGenerator.kt`
- [X] T005 [P] Update `RuntimeViewModelGenerator.kt` to derive entity sub-package and use it for `{Entity}Dao`, `{Entity}Entity`, `{Entity}Repository` imports (lines 165-167) in `kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/RuntimeViewModelGenerator.kt`
- [X] T006 Update `ModuleSaveService.kt` save flow to write persistence files into `{Entity}/` subdirectory under `persistenceDir` instead of flat root in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [X] T007 Update `ModuleSaveService.removeEntityModule()` to delete the entire `{Entity}/` subdirectory via `deleteRecursively()` instead of three individual files in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [X] T008 Update `ModuleSaveService` AppDatabase regeneration to scan subdirectories (depth 1) for `*Entity.kt` files instead of only flat `persistenceDir` listing in `graphEditor/src/jvmMain/kotlin/save/ModuleSaveService.kt`
- [X] T009 Update `EntityModuleGeneratorTest.kt` assertions for persistence file package declarations and import paths to expect entity sub-packages in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/EntityModuleGeneratorTest.kt`
- [X] T010 [P] Update `RuntimeViewModelGeneratorTest.kt` assertions for persistence import paths to expect entity sub-packages in `kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/RuntimeViewModelGeneratorTest.kt`
- [X] T011 Run `./gradlew :kotlinCompiler:jvmTest` to verify all generator tests pass

**Checkpoint**: All generators produce the new subdirectory layout. Tool repo tests pass.

---

## Phase 3: User Story 1 — New Modules Generate Grouped Files (Priority: P1) MVP

**Goal**: When a new repository module is created via the graphEditor, its persistence files land in an `{Entity}/` subdirectory with correct sub-package declarations.

**Independent Test**: Create a new repository module (e.g., "Product") and verify files appear in `persistence/.../Product/` subdirectory with `package io.codenode.persistence.product`. Compile and run runtime preview.

### Implementation for User Story 1

- [X] T012 [US1] **MANUAL** Verify quickstart Scenario 3: create a new repository module (e.g., "Product") via graphEditor and inspect that `ProductEntity.kt`, `ProductDao.kt`, `ProductRepository.kt` are generated in `persistence/src/commonMain/kotlin/io/codenode/persistence/Product/`
- [X] T013 [US1] **MANUAL** Verify quickstart Scenario 4: compile the new module with `./gradlew :Products:jvmJar`, rebuild classpath, launch graphEditor, and confirm runtime preview works
- [X] T014 [US1] **MANUAL** Verify quickstart Scenario 5: remove the module and confirm the entire `Product/` subdirectory is deleted from persistence, along with Gradle and AppDatabase cleanup

**Checkpoint**: New module generation produces correctly grouped persistence files. Creation, runtime, and removal all work.

---

## Phase 4: User Story 2 — Existing Modules Migrated to Grouped Layout (Priority: P2)

**Goal**: Move existing UserProfile and Address persistence files into entity subdirectories and update all imports across the DemoProject.

**Independent Test**: Compile all modules, run runtime previews for UserProfiles and Addresses, confirm zero regressions.

### Implementation for User Story 2

- [X] T015 [US2] Create `UserProfile/` subdirectory under `persistence/src/commonMain/kotlin/io/codenode/persistence/` in DemoProject, move `UserProfileEntity.kt`, `UserProfileDao.kt`, `UserProfileRepository.kt` into it, and update their package declarations to `io.codenode.persistence.userprofile`
- [X] T016 [P] [US2] Create `Address/` subdirectory under `persistence/src/commonMain/kotlin/io/codenode/persistence/` in DemoProject, move `AddressEntity.kt`, `AddressDao.kt`, `AddressRepository.kt` into it, and update their package declarations to `io.codenode.persistence.address`
- [X] T017 [US2] Update `AppDatabase.kt` imports to reference `io.codenode.persistence.userprofile.UserProfileEntity`, `io.codenode.persistence.userprofile.UserProfileDao`, `io.codenode.persistence.address.AddressEntity`, `io.codenode.persistence.address.AddressDao` in `persistence/src/commonMain/kotlin/io/codenode/persistence/AppDatabase.kt`
- [X] T018 [US2] Update `PersistenceBootstrap.kt` imports to reference `io.codenode.persistence.userprofile.UserProfileDao` and `io.codenode.persistence.address.AddressDao` in `persistence/src/commonMain/kotlin/io/codenode/persistence/PersistenceBootstrap.kt`
- [X] T019 [P] [US2] Update UserProfiles module imports: `UserProfilesViewModel.kt` (3 imports), `UserProfilesPersistence.kt` (2 imports), `UserProfileConverters.kt` (1 import), `nodes/UserProfileRepositoryCodeNode.kt` (1 import) — change `io.codenode.persistence.UserProfile*` to `io.codenode.persistence.userprofile.UserProfile*`
- [X] T020 [P] [US2] Update Addresses module imports: `AddressesViewModel.kt` (2 imports), `AddressesPersistence.kt` (2 imports), `AddressConverters.kt` (1 import), `nodes/AddressRepositoryCodeNode.kt` (1 import) — change `io.codenode.persistence.Address*` to `io.codenode.persistence.address.Address*`
- [X] T021 [US2] Delete the old flat-root persistence files (`UserProfileEntity.kt`, `UserProfileDao.kt`, `UserProfileRepository.kt`, `AddressEntity.kt`, `AddressDao.kt`, `AddressRepository.kt`) from `persistence/src/commonMain/kotlin/io/codenode/persistence/` if not already removed by the move
- [X] T022 [US2] Verify quickstart Scenario 1: compile all existing modules with `./gradlew :UserProfiles:jvmJar :Addresses:jvmJar :StopWatch:jvmJar` and verify directory structure
- [X] T023 [US2] Verify quickstart Scenario 2: rebuild classpath, launch graphEditor, run runtime preview for UserProfiles and Addresses, perform CRUD operations

**Checkpoint**: All existing modules compile and run. Persistence files are grouped by entity.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all scenarios

- [X] T024 Run full DemoProject build: `./gradlew jvmJar` to verify zero compilation errors across all modules
- [X] T025 Verify no stale persistence files remain at flat root (only `AppDatabase.kt`, `BaseDao.kt`, `DatabaseModule.kt`, `PersistenceBootstrap.kt` and platform-specific `DatabaseBuilder.*.kt`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 2 (Foundational)**: No dependencies — can start immediately. BLOCKS all user stories.
- **Phase 3 (US1)**: Depends on Phase 2 completion (generators must produce new layout)
- **Phase 4 (US2)**: Depends on Phase 2 completion (need to know correct package convention). Can run in parallel with US1.
- **Phase 5 (Polish)**: Depends on Phases 3 and 4 completion.

### User Story Dependencies

- **User Story 1 (P1)**: Depends only on Phase 2. Tests new module generation.
- **User Story 2 (P2)**: Depends only on Phase 2. Can proceed in parallel with US1 — different files.

### Within Each User Story

- US1: Generator changes (Phase 2) → create module → verify compile → verify removal
- US2: Move files → update imports → delete old files → compile → runtime test

### Parallel Opportunities

- T004 and T005 can run in parallel (different generator files)
- T009 and T010 can run in parallel (different test files)
- T015 and T016 can run in parallel (UserProfile vs Address — different entity files)
- T019 and T020 can run in parallel (UserProfiles module vs Addresses module)
- US1 and US2 can run in parallel after Phase 2 completes

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational generators
2. Complete Phase 3: User Story 1 — new modules generate grouped files
3. **STOP and VALIDATE**: Create/compile/remove a test module
4. This delivers the primary value: all future modules get the right layout

### Incremental Delivery

1. Phase 2 → Generators updated → tool repo tests pass
2. Phase 3 (US1) → New module generation works → MVP ready
3. Phase 4 (US2) → Existing modules migrated → Full consistency
4. Phase 5 → Final validation → Feature complete

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Commit after each phase or logical group
- The DemoProject changes (Phase 4) are manual migration — the generators (Phase 2) handle future modules automatically
