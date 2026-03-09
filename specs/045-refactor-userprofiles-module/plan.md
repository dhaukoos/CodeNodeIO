# Implementation Plan: Refactor UserProfiles Module

**Branch**: `045-refactor-userprofiles-module` | **Date**: 2026-03-08 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/045-refactor-userprofiles-module/spec.md`

## Summary

Refactor the UserProfiles module by relocating shared persistence infrastructure (AppDatabase, BaseDao, DatabaseModule) from UserProfiles to KMPMobileApp, and extracting the UserProfileCUD source node and UserProfilesDisplay sink node into distinct files. This is a pure structural refactoring — no behavior changes. Prepares for future generalization of EntityRepository modules.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2
**Storage**: Room (KMP) with BundledSQLiteDriver — persisted to `~/.codenode/data/app.db` (JVM)
**Testing**: Manual verification via Runtime Preview (quickstart.md scenarios)
**Target Platform**: JVM Desktop (Compose Desktop), Android, iOS
**Project Type**: KMP multiplatform with UserProfiles and KMPMobileApp modules
**Performance Goals**: Zero behavior change — existing CRUD performance unaffected
**Constraints**: Must not change any runtime behavior; all existing imports must be updated
**Scale/Scope**: ~10-15 files modified, 2-3 new files, 3 files moved

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Improves module separation and single responsibility. |
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

### Source Code (repository root)

```text
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/
├── persistence/
│   ├── UserProfileEntity.kt          # STAYS (UserProfiles-specific)
│   ├── UserProfileDao.kt             # STAYS (UserProfiles-specific)
│   ├── UserProfileRepository.kt      # STAYS (UserProfiles-specific)
│   ├── AppDatabase.kt                # MOVE → KMPMobileApp
│   ├── BaseDao.kt                    # MOVE → KMPMobileApp
│   └── DatabaseModule.kt             # MOVE → KMPMobileApp
├── generated/
│   └── UserProfilesFlow.kt           # MODIFY: Extract node definitions
└── processingLogic/
    └── UserProfileRepositoryProcessLogic.kt  # MODIFY: Update imports

UserProfiles/src/jvmMain/kotlin/io/codenode/userprofiles/persistence/
└── DatabaseBuilder.jvm.kt            # MOVE → KMPMobileApp

UserProfiles/src/androidMain/kotlin/io/codenode/userprofiles/persistence/
└── DatabaseBuilder.android.kt        # MOVE → KMPMobileApp

UserProfiles/src/iosMain/kotlin/io/codenode/userprofiles/persistence/
└── DatabaseBuilder.ios.kt            # MOVE → KMPMobileApp

KMPMobileApp/src/commonMain/kotlin/io/codenode/mobileapp/
└── persistence/                       # NEW directory
    ├── AppDatabase.kt                 # MOVED from UserProfiles
    ├── BaseDao.kt                     # MOVED from UserProfiles
    └── DatabaseModule.kt              # MOVED from UserProfiles

KMPMobileApp/src/jvmMain/kotlin/io/codenode/mobileapp/persistence/
└── DatabaseBuilder.jvm.kt            # MOVED from UserProfiles

KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/persistence/
└── DatabaseBuilder.android.kt        # MOVED from UserProfiles

KMPMobileApp/src/iosMain/kotlin/io/codenode/mobileapp/persistence/
└── DatabaseBuilder.ios.kt            # MOVED from UserProfiles

UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/
├── UserProfileCUD.kt                 # NEW: Extracted source node
└── UserProfilesDisplay.kt            # NEW: Extracted sink node
```

**Structure Decision**: Files are moved between existing modules (UserProfiles → KMPMobileApp) and new node files are created within UserProfiles. The persistence package changes from `io.codenode.userprofiles.persistence` to `io.codenode.mobileapp.persistence` for the moved files. Room annotations, KSP, and SQLite dependencies must move to KMPMobileApp's build.gradle.kts.

## Constitution Check (Post-Design)

*Re-evaluated after Phase 1 design completion.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Better module separation — persistence infra in app module, nodes in distinct files. |
| II. Test-Driven Development | PASS | Verifiable via Runtime Preview scenarios in quickstart.md. |
| III. User Experience Consistency | PASS | No user-facing changes whatsoever. |
| IV. Performance Requirements | PASS | Zero performance impact — same code, different file locations. |
| V. Observability & Debugging | PASS | No impact. |
| Licensing & IP | PASS | No new dependencies. |

## Complexity Tracking

No violations to justify.
