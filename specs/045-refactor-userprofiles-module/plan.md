# Implementation Plan: Refactor UserProfiles Module

**Branch**: `045-refactor-userprofiles-module` | **Date**: 2026-03-08 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/045-refactor-userprofiles-module/spec.md`

## Summary

Refactor the UserProfiles module by extracting all persistence files into a new shared `persistence` Gradle module, and extracting the UserProfileCUD source node and UserProfilesDisplay sink node into distinct files. This is a pure structural refactoring — no behavior changes. Prepares for future generalization of EntityRepository modules.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2
**Storage**: Room (KMP) with BundledSQLiteDriver — persisted to `~/.codenode/data/app.db` (JVM)
**Testing**: Manual verification via Runtime Preview (quickstart.md scenarios)
**Target Platform**: JVM Desktop (Compose Desktop), Android, iOS
**Project Type**: KMP multiplatform — creating new `persistence` module, modifying UserProfiles and KMPMobileApp
**Performance Goals**: Zero behavior change — existing CRUD performance unaffected
**Constraints**: Must not change any runtime behavior; all existing imports must be updated; no circular dependencies
**Scale/Scope**: ~12-15 files modified, 2 new files created, 9 files moved to new module, 1 new build.gradle.kts

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Eliminates artificial coupling between UserProfiles and persistence infrastructure. |
| II. Test-Driven Development | PASS | Verifiable via manual Runtime Preview scenarios. No new logic to unit test. |
| III. User Experience Consistency | PASS | No user-facing changes. |
| IV. Performance Requirements | PASS | Pure structural refactoring — no performance impact. |
| V. Observability & Debugging | PASS | No impact on logging or debugging capabilities. |
| Licensing & IP | PASS | No new dependencies. Moving existing Apache 2.0 code between modules. |

## Project Structure

### Documentation (this feature)

```text
specs/045-refactor-userprofiles-module/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code — New Module

```text
persistence/                                    # NEW Gradle module
├── build.gradle.kts                            # Room, KSP, SQLite deps; JVM+Android+iOS targets
├── src/commonMain/kotlin/io/codenode/persistence/
│   ├── AppDatabase.kt                          # MOVED from UserProfiles (package: io.codenode.persistence)
│   ├── BaseDao.kt                              # MOVED from UserProfiles
│   ├── DatabaseModule.kt                       # MOVED from UserProfiles
│   ├── UserProfileEntity.kt                    # MOVED from UserProfiles
│   ├── UserProfileDao.kt                       # MOVED from UserProfiles
│   └── UserProfileRepository.kt                # MOVED from UserProfiles
├── src/jvmMain/kotlin/io/codenode/persistence/
│   └── DatabaseBuilder.jvm.kt                  # MOVED from UserProfiles
├── src/androidMain/kotlin/io/codenode/persistence/
│   └── DatabaseBuilder.android.kt              # MOVED from UserProfiles
└── src/iosMain/kotlin/io/codenode/persistence/
    └── DatabaseBuilder.ios.kt                  # MOVED from UserProfiles
```

### Source Code — Modified Existing Files

```text
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/
├── persistence/                                # REMOVED (entire directory deleted)
├── UserProfile.kt                              # MODIFY: Update imports
├── UserProfilesViewModel.kt                    # MODIFY: Update imports
├── UserProfileCUD.kt                           # NEW: Extracted source node
├── UserProfilesDisplay.kt                      # NEW: Extracted sink node (already created)
├── generated/
│   └── UserProfilesFlow.kt                     # MODIFY: Reference extracted nodes (already done)
├── processingLogic/
│   └── UserProfileRepositoryProcessLogic.kt    # MODIFY: Update imports
└── userInterface/
    ├── UserProfiles.kt                         # MODIFY: Update imports
    └── AddUpdateUserProfile.kt                 # MODIFY: Update imports

UserProfiles/src/jvmMain/kotlin/io/codenode/userprofiles/persistence/
└── DatabaseBuilder.jvm.kt                      # REMOVED (moved to persistence module)

UserProfiles/src/androidMain/kotlin/io/codenode/userprofiles/persistence/
└── DatabaseBuilder.android.kt                  # REMOVED (moved to persistence module)

UserProfiles/src/iosMain/kotlin/io/codenode/userprofiles/persistence/
└── DatabaseBuilder.ios.kt                      # REMOVED (moved to persistence module)

KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/
└── MainActivity.kt                             # MODIFY: Update initializeDatabaseContext import

settings.gradle.kts                             # MODIFY: Add include(":persistence")
```

### Build Configuration Changes

```text
persistence/build.gradle.kts                    # NEW: Room, KSP, SQLite, JVM+Android+iOS targets
UserProfiles/build.gradle.kts                   # MODIFY: Remove Room/KSP/SQLite, add project(":persistence")
KMPMobileApp/build.gradle.kts                   # MODIFY: Add project(":persistence") dependency
settings.gradle.kts                             # MODIFY: Add include(":persistence")
```

### Dependency Graph (After Refactoring)

```text
KMPMobileApp → UserProfiles → persistence
KMPMobileApp → persistence
KMPMobileApp → StopWatch
UserProfiles → fbpDsl
StopWatch → fbpDsl
persistence → (Room, KSP, SQLite — no project dependencies)
```

**Structure Decision**: All persistence files move to a new `persistence` Gradle module with package `io.codenode.persistence`. This avoids circular dependencies — the persistence module has no project dependencies, while UserProfiles depends on it for entity/DAO access. Entity and DAO classes move alongside infrastructure because AppDatabase references UserProfileEntity and UserProfileDao extends BaseDao.

## Constitution Check (Post-Design)

*Re-evaluated after Phase 1 design completion.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Clean module separation — shared persistence infra in its own module, nodes in distinct files. |
| II. Test-Driven Development | PASS | Verifiable via Runtime Preview scenarios in quickstart.md. |
| III. User Experience Consistency | PASS | No user-facing changes whatsoever. |
| IV. Performance Requirements | PASS | Zero performance impact — same code, different module locations. |
| V. Observability & Debugging | PASS | No impact. |
| Licensing & IP | PASS | No new dependencies. |

## Complexity Tracking

No violations to justify.
