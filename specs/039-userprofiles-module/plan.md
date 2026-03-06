# Implementation Plan: UserProfiles Module

**Branch**: `039-userprofiles-module` | **Date**: 2026-03-05 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/039-userprofiles-module/spec.md`

## Summary

Flesh out the existing UserProfiles module with a complete CRUD UI, database integration, and reactive data flow. The module already has generated flow code (Controller, Flow, ViewModel), Room persistence layer (Entity, DAO, Repository), and platform-specific database builders. This plan addresses: (1) adding Room/KSP/SQLite build dependencies, (2) registering the module in Gradle settings, (3) implementing the processing logic stub, (4) building the UserProfiles screen and AddUpdateUserProfile form composables, (5) extending the ViewModel with CRUD methods, and (6) wiring the module into KMPMobileApp.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0, Room 2.8.4 (KMP), KSP 2.1.21-2.0.1, SQLite Bundled 2.6.2
**Storage**: Room (KMP) with BundledSQLiteDriver — persisted to `~/.codenode/data/app.db` (JVM), platform-specific paths for Android/iOS
**Testing**: kotlin.test (commonTest), kotlinx-coroutines-test 1.8.0
**Target Platform**: Android (minSdk 24), iOS (x64, arm64, simulatorArm64), JVM Desktop
**Project Type**: Mobile (KMP multiplatform module + KMPMobileApp consumer)
**Performance Goals**: Reactive UI updates within 1 second of CRUD operations; 60fps scrolling
**Constraints**: All dependencies must be Apache 2.0 or MIT licensed; Room KMP requires KSP plugin
**Scale/Scope**: Single module with 3 screens (list, add form, update form), 1 entity type, ~10 files modified/created

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Licensing (Apache 2.0/MIT only) | PASS | Room (Apache 2.0), KSP (Apache 2.0), SQLite Bundled (Apache 2.0), Compose (Apache 2.0) |
| Code Quality (readability, maintainability) | PASS | Following existing module patterns (StopWatch). Single responsibility per file. |
| Test-Driven Development | PASS | Will write tests for ViewModel CRUD methods and processing logic |
| UX Consistency | PASS | Following existing StopWatch screen pattern (Column + buttons). Material3 components. |
| Performance Requirements | PASS | Room Flow provides reactive queries; Compose lazy list for scrolling |
| Observability | PASS | ExecutionState flow for lifecycle monitoring; error output from processor |

No violations. All gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/039-userprofiles-module/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── viewmodel-api.md
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
UserProfiles/
├── build.gradle.kts                          # MODIFY: Add Room/KSP/SQLite dependencies
└── src/
    └── commonMain/kotlin/io/codenode/userprofiles/
        ├── UserProfilesViewModel.kt          # MODIFY: Add addEntity, updateEntity, removeEntity methods + profiles list state
        ├── generated/                        # NO CHANGES (generated code)
        ├── persistence/                      # NO CHANGES (already complete)
        ├── processingLogic/
        │   └── UserProfileRepositoryProcessLogic.kt  # MODIFY: Implement tick logic
        └── userInterface/
            ├── UserProfiles.kt               # MODIFY: Full screen with list, buttons, empty state
            └── AddUpdateUserProfile.kt       # CREATE: Shared add/update form composable

KMPMobileApp/
├── build.gradle.kts                          # MODIFY: Add implementation(project(":UserProfiles"))
└── src/commonMain/kotlin/io/codenode/mobileapp/
    └── App.kt                                # MODIFY: Wire UserProfiles controller + screen

settings.gradle.kts                           # MODIFY: Add include(":UserProfiles"), Room/KSP plugins
```

**Structure Decision**: Follows the established KMP module pattern. The UserProfiles module already exists with the correct directory structure. Changes are primarily modifications to existing files plus one new UI file.
