# Implementation Plan: Persistence Dependency Injection

**Branch**: `046-persistence-dependency-injection` | **Date**: 2026-03-09 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/046-persistence-dependency-injection/spec.md`

## Summary

Refactor the UserProfiles module to receive DAOs via dependency injection instead of calling DatabaseModule directly. Move entity/DAO classes back to UserProfiles and invert the persistence dependency direction so feature modules own their domain types while the persistence module depends on feature modules (for entity references in AppDatabase). Pure structural refactoring — no behavior changes.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform) + Compose Multiplatform 1.7.3
**Primary Dependencies**: kotlinx-coroutines 1.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2
**Storage**: Room (KMP) with BundledSQLiteDriver — persisted to `~/.codenode/data/app.db` (JVM)
**Testing**: Manual verification via Runtime Preview (quickstart.md scenarios)
**Target Platform**: JVM Desktop (Compose Desktop), Android, iOS
**Project Type**: KMP multiplatform — modifying UserProfiles, persistence, graphEditor, KMPMobileApp
**Performance Goals**: Zero behavior change — existing CRUD performance unaffected
**Constraints**: Must not change any runtime behavior; no circular dependencies; circuitSimulator is JVM-only
**Scale/Scope**: ~12-15 files modified, 1 new file created, 3 files moved between modules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Improves module separation — feature modules own domain types, DI replaces service location. |
| II. Test-Driven Development | PASS | Verifiable via manual Runtime Preview scenarios. No new logic to unit test. |
| III. User Experience Consistency | PASS | No user-facing changes. |
| IV. Performance Requirements | PASS | Pure structural refactoring — no performance impact. |
| V. Observability & Debugging | PASS | Initialization errors provide clear messages (FR-003). |
| Licensing & IP | PASS | No new dependencies. Moving existing Apache 2.0 code between modules. |

## Project Structure

### Documentation (this feature)

```text
specs/046-persistence-dependency-injection/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code — Changes by User Story

#### US1: Dependency Injection (code changes only)

```text
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/
├── UserProfilesPersistence.kt                      # NEW: DI initialization object
├── UserProfilesViewModel.kt                        # MODIFY: Accept UserProfileDao constructor param
└── processingLogic/
    └── UserProfileRepositoryProcessLogic.kt        # MODIFY: Use UserProfilesPersistence.getDao()

graphEditor/src/jvmMain/kotlin/ui/
└── ModuleSessionFactory.kt                         # MODIFY: Wire DAO from DatabaseModule into UserProfiles
```

#### US2: Move Entity/DAO Classes Back to UserProfiles

```text
UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/persistence/
├── UserProfileEntity.kt                            # MOVED from persistence module
├── UserProfileDao.kt                               # MOVED from persistence module + remove BaseDao extends
└── UserProfileRepository.kt                        # MOVED from persistence module

persistence/src/commonMain/kotlin/io/codenode/persistence/
├── BaseDao.kt                                      # STAYS (generic infrastructure)
├── AppDatabase.kt                                  # STAYS (references entity from UserProfiles)
├── DatabaseModule.kt                               # STAYS
├── UserProfileEntity.kt                            # DELETED (moved to UserProfiles)
├── UserProfileDao.kt                               # DELETED (moved to UserProfiles)
└── UserProfileRepository.kt                        # DELETED (moved to UserProfiles)

persistence/build.gradle.kts                        # MODIFY: Add project(":UserProfiles") dependency
UserProfiles/build.gradle.kts                       # MODIFY: Remove project(":persistence") dependency
graphEditor/build.gradle.kts                        # MODIFY: Add project(":persistence") dependency
KMPMobileApp/build.gradle.kts                       # MODIFY: Already has project(":persistence")
```

#### US3: App Layer Wiring (already partially done in US1)

US3 is satisfied by US1's wiring changes + US2's dependency inversion. No additional file moves needed — AppDatabase stays in persistence, which now depends on UserProfiles (not vice versa). Each app entry point wires DAOs at startup.

```text
graphEditor/src/jvmMain/kotlin/ui/
└── ModuleSessionFactory.kt                         # Already modified in US1

KMPMobileApp/src/androidMain/kotlin/io/codenode/mobileapp/
└── MainActivity.kt                                 # MODIFY: Wire DAO after database context init
```

### Dependency Graph (After All Stories)

```text
UserProfiles → fbpDsl                   (no persistence dependency!)
persistence → UserProfiles              (for entity types in AppDatabase)
persistence → Room/KSP/SQLite           (external libraries)
graphEditor → persistence               (for DatabaseModule to wire DAOs)
graphEditor → UserProfiles              (for controller/viewmodel)
graphEditor → circuitSimulator          (runtime framework)
KMPMobileApp → persistence              (for DatabaseModule)
KMPMobileApp → UserProfiles             (for UI/viewmodel)
```

**Structure Decision**: The dependency direction inverts — persistence depends on UserProfiles (for AppDatabase entity references) while UserProfiles is fully independent of persistence. UserProfileDao defines its own CRUD methods (Room annotations) instead of extending BaseDao, eliminating the need for UserProfiles to reference persistence at all. BaseDao remains in persistence as generic infrastructure for future entity modules.

## Constitution Check (Post-Design)

*Re-evaluated after Phase 1 design completion.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Clean DI pattern — feature modules own domain types, no service location. |
| II. Test-Driven Development | PASS | Verifiable via Runtime Preview scenarios in quickstart.md. |
| III. User Experience Consistency | PASS | No user-facing changes whatsoever. |
| IV. Performance Requirements | PASS | Zero performance impact — same code, different wiring. |
| V. Observability & Debugging | PASS | Clear error on uninitialized persistence access. |
| Licensing & IP | PASS | No new dependencies. |

## Complexity Tracking

No violations to justify.
