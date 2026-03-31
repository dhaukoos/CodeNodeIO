# Implementation Plan: Group Persistence Files by Entity

**Branch**: `062-group-persistence-files` | **Date**: 2026-03-31 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/062-group-persistence-files/spec.md`

## Summary

Move per-entity persistence files (`{Entity}Entity.kt`, `{Entity}Dao.kt`, `{Entity}Repository.kt`) from the flat `io.codenode.persistence` package into entity-specific subdirectories (`io.codenode.persistence.{entity}/`). Update all code generators (CodeNodeIO tool repo) to produce the new layout, update module removal to delete subdirectories, and migrate the existing UserProfile and Address files in the DemoProject.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Room 2.8.4 (KMP), Koin 4.0.0, kotlinx-coroutines 1.8.0, Compose Multiplatform 1.7.3
**Storage**: Room (KMP) with BundledSQLiteDriver — shared `persistence` module
**Testing**: `./gradlew :kotlinCompiler:jvmTest`, `./gradlew jvmJar` (DemoProject full compile)
**Target Platform**: JVM Desktop (primary), Android/iOS (KMP)
**Project Type**: Multi-module KMP project (tool repo + demo project)
**Performance Goals**: N/A (file layout change, no runtime performance impact)
**Constraints**: Must not break existing modules; Room annotations reference entity classes by import
**Scale/Scope**: 5 generator files + 2 test files (tool repo), 9 files (DemoProject imports), 6 persistence files to move

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Organizing files into subdirectories improves readability and maintainability |
| II. Test-Driven Development | PASS | Existing generator tests will be updated; full compile validates DemoProject |
| III. User Experience Consistency | N/A | No user-facing UI changes |
| IV. Performance Requirements | N/A | No runtime performance impact |
| V. Observability & Debugging | N/A | No runtime behavior changes |
| Licensing & IP | PASS | No new dependencies |

No constitution violations. All gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/062-group-persistence-files/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code Changes

#### CodeNodeIO Tool Repo (generators)

```text
kotlinCompiler/src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
├── EntityModuleGenerator.kt          # Change persistence file paths to include {Entity}/ subdir
├── RepositoryCodeGenerator.kt        # Change package declaration in generated Entity/Dao/Repository
├── EntityRepositoryCodeNodeGenerator.kt  # Update import path for {Entity}Repository
├── EntityPersistenceGenerator.kt     # Update import paths for {Entity}Dao, {Entity}Repository
├── RuntimeViewModelGenerator.kt      # Update hardcoded io.codenode.persistence.{X} imports
└── EntityModuleSpec.kt               # (no change needed — persistencePackage stays the same)

kotlinCompiler/src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
├── EntityModuleGeneratorTest.kt      # Update test assertions for new package paths
└── RuntimeViewModelGeneratorTest.kt  # Update test assertions for new package paths

graphEditor/src/jvmMain/kotlin/save/
├── ModuleSaveService.kt              # Update save (write to subdir) + removal (delete subdir)
└── (AppDatabase regeneration)        # Update import scanning to find entities in subdirs
```

#### CodeNodeIO DemoProject (migration)

```text
persistence/src/commonMain/kotlin/io/codenode/persistence/
├── AppDatabase.kt                    # Update imports to new subpackages
├── BaseDao.kt                        # No change (no entity-specific references)
├── DatabaseModule.kt                 # No change
├── PersistenceBootstrap.kt           # Update imports to new subpackages
├── UserProfile/                      # NEW subdirectory
│   ├── UserProfileEntity.kt          # Moved, package → io.codenode.persistence.userprofile
│   ├── UserProfileDao.kt             # Moved, package → io.codenode.persistence.userprofile
│   └── UserProfileRepository.kt      # Moved, package → io.codenode.persistence.userprofile
└── Address/                          # NEW subdirectory
    ├── AddressEntity.kt              # Moved, package → io.codenode.persistence.address
    ├── AddressDao.kt                 # Moved, package → io.codenode.persistence.address
    └── AddressRepository.kt          # Moved, package → io.codenode.persistence.address

UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/
├── UserProfilesViewModel.kt          # Update 3 imports
├── UserProfilesPersistence.kt        # Update 2 imports
├── UserProfileConverters.kt          # Update 1 import
└── nodes/UserProfileRepositoryCodeNode.kt  # Update 1 import

Addresses/src/commonMain/kotlin/io/codenode/addresses/
├── AddressesPersistence.kt           # Update 2 imports
├── AddressesViewModel.kt             # Update 2 imports
├── AddressConverters.kt              # Update 1 import
└── nodes/AddressRepositoryCodeNode.kt  # Update 1 import
```

**Structure Decision**: Existing multi-module KMP project layout. Changes affect the shared `persistence` module (add entity subdirectories), generators in `kotlinCompiler`, save/removal in `graphEditor`, and import updates across DemoProject modules.

## Key Design Decisions

### 1. Subdirectory Naming Convention

**Directory name**: PascalCase matching entity name (e.g., `UserProfile/`, `Address/`, `TestItem/`)
**Package name**: Lowercase matching entity name (e.g., `io.codenode.persistence.userprofile`, `io.codenode.persistence.address`)

This follows Kotlin convention where package names are lowercase and directory structure mirrors package hierarchy.

### 2. Package Change Strategy

The generated persistence files will use a sub-package of `io.codenode.persistence`:
- Before: `package io.codenode.persistence`
- After: `package io.codenode.persistence.{entitylowercase}` (e.g., `io.codenode.persistence.userprofile`)

All imports across the codebase that reference `io.codenode.persistence.{Entity}Entity`, `{Entity}Dao`, or `{Entity}Repository` must be updated to `io.codenode.persistence.{entitylowercase}.{Entity}Entity` etc.

### 3. Shared Files Stay at Root

`AppDatabase.kt`, `BaseDao.kt`, `DatabaseModule.kt`, `PersistenceBootstrap.kt`, and platform-specific `DatabaseBuilder.*.kt` remain in the root `io.codenode.persistence` package. Only entity-specific triplets (Entity/Dao/Repository) move into subdirectories.

### 4. Generator Changes

`EntityModuleSpec.persistencePackage` stays as `"io.codenode.persistence"` (the root). A new computed property or convention derives the entity-specific sub-package: `"${persistencePackage}.${entityName.lowercase()}"`. The generators that produce file paths and import statements use this derived sub-package.

### 5. Module Removal

`removeEntityModule()` currently deletes three individual files by name from `persistenceDir`. After this change, it will delete the entire `{Entity}/` subdirectory from `persistenceDir` using `File.deleteRecursively()`. The `AppDatabase.kt` regeneration must scan subdirectories (not just flat files) to find remaining `*Entity.kt` files.

## Files to Modify

### CodeNodeIO Tool Repo

| File | Change |
|------|--------|
| `kotlinCompiler/.../RepositoryCodeGenerator.kt` | Use sub-package in `package` declaration for generated Entity/Dao/Repository |
| `kotlinCompiler/.../EntityModuleGenerator.kt` | Use `{Entity}/` subdirectory in persistence file paths |
| `kotlinCompiler/.../EntityRepositoryCodeNodeGenerator.kt` | Update import path to `persistence.{entity}.{Entity}Repository` |
| `kotlinCompiler/.../EntityPersistenceGenerator.kt` | Update import paths to `persistence.{entity}.{Entity}Dao/Repository` |
| `kotlinCompiler/.../RuntimeViewModelGenerator.kt` | Update hardcoded imports to include entity sub-package |
| `kotlinCompiler/.../EntityModuleGeneratorTest.kt` | Update assertions for new package paths |
| `kotlinCompiler/.../RuntimeViewModelGeneratorTest.kt` | Update assertions for new package paths |
| `graphEditor/.../save/ModuleSaveService.kt` | Write persistence files to subdirectory; delete subdirectory on removal; scan subdirs for AppDatabase regeneration |

### CodeNodeIO DemoProject

| File | Change |
|------|--------|
| 6 persistence files (UserProfile + Address) | Move to subdirs, update package declarations |
| `AppDatabase.kt` | Update imports to new sub-packages |
| `PersistenceBootstrap.kt` | Update imports to new sub-packages |
| 4 UserProfiles module files | Update `io.codenode.persistence.UserProfile*` imports |
| 4 Addresses module files | Update `io.codenode.persistence.Address*` imports |
