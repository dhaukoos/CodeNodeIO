# Research: Refactor UserProfiles Module

**Feature**: 045-refactor-userprofiles-module
**Date**: 2026-03-08

## R1: Persistence File Relocation Strategy

**Decision**: Move AppDatabase.kt, BaseDao.kt, and DatabaseModule.kt (plus platform-specific DatabaseBuilder files) from `UserProfiles/src/.../persistence/` to `KMPMobileApp/src/.../persistence/`, changing the package from `io.codenode.userprofiles.persistence` to `io.codenode.mobileapp.persistence`.

**Rationale**: The UserProfiles module currently owns application-wide persistence infrastructure (Room database, generic DAO interface, database singleton). This creates an undesirable dependency — any future module needing database access would depend on UserProfiles. Moving these to KMPMobileApp (the application shell) properly positions them as shared infrastructure. UserProfiles-specific files (UserProfileEntity, UserProfileDao, UserProfileRepository) remain in UserProfiles since they are domain-specific.

**Alternatives considered**:
- Create a dedicated `persistence` module: Adds module complexity for only 3 files. Rejected — KMPMobileApp already serves as the app-level integration point.
- Leave files in UserProfiles and have other modules depend on it: Creates artificial coupling. Rejected.

## R2: Build Configuration Changes

**Decision**: Move Room, KSP, and SQLite dependencies from UserProfiles/build.gradle.kts to KMPMobileApp/build.gradle.kts. Add the `androidx.room` plugin and KSP processors to KMPMobileApp. UserProfiles keeps Room runtime dependency for annotations on UserProfileEntity and UserProfileDao.

**Rationale**: AppDatabase.kt uses `@Database` and `@ConstructedBy` annotations from Room. BaseDao.kt uses `@Insert`, `@Update`, `@Delete`. DatabaseModule.kt uses `BundledSQLiteDriver`. These require `room-runtime` and `sqlite-bundled` in KMPMobileApp. The Room compiler (KSP) must run in KMPMobileApp to generate the database implementation. UserProfiles still needs `room-runtime` for entity/DAO annotations but no longer needs the KSP compiler or SQLite bundled driver.

**Key insight**: The `@Database` annotation listing `entities = [UserProfileEntity::class]` in AppDatabase.kt creates a compile-time dependency from KMPMobileApp on UserProfiles' entity class. This means KMPMobileApp must depend on UserProfiles (which it already does), and the Room schema generation must happen in KMPMobileApp.

**Alternatives considered**:
- Keep Room KSP in UserProfiles only: AppDatabase moves to KMPMobileApp but the Room compiler stays in UserProfiles. This doesn't work — the compiler needs to process AppDatabase where it lives.

## R3: UserProfileCUD Extraction Pattern

**Decision**: Extract the UserProfileCUD source node creation into a new file `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfileCUD.kt`. The file contains the `createSourceOut3` factory call with the reactive StateFlow collectors. UserProfilesFlow.kt then references this extracted definition.

**Rationale**: UserProfileCUD is a self-contained source node that listens to UserProfilesState for CRUD commands. Its definition is independent of the flow graph wiring — it only needs access to `UserProfilesState`, `CodeNodeFactory`, `ProcessResult3`, and coroutine primitives. Extracting it into its own file makes the component independently readable and sets the pattern for future generated modules.

**Alternatives considered**:
- Extract as a class extending a base source node: Overengineered for a factory method call. Rejected.
- Extract as a top-level function returning the runtime: Simple but loses the property semantics used by UserProfilesFlow. Rejected.

## R4: UserProfilesDisplay Extraction Pattern

**Decision**: Extract the UserProfilesDisplay sink node creation into a new file `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesDisplay.kt`. The file contains the `createSinkIn2` factory call.

**Rationale**: UserProfilesDisplay is a minimal sink node (6 lines) that updates UserProfilesState with result/error values. Despite its simplicity, extracting it establishes consistency — every node in the module has its own file, making the pattern clear for future EntityRepository modules.

**Alternatives considered**:
- Keep inline in UserProfilesFlow.kt due to its small size: Inconsistent with UserProfileCUD extraction. Rejected for consistency.

## R5: Import Update Scope

**Decision**: Update all files importing from `io.codenode.userprofiles.persistence` to reference the new package paths. Files importing UserProfiles-specific types (UserProfileEntity, UserProfileDao, UserProfileRepository) keep their existing imports since those files stay in UserProfiles. Files importing AppDatabase, BaseDao, or DatabaseModule change to `io.codenode.mobileapp.persistence`.

**Rationale**: The package rename is the most impactful part of this refactoring. The affected files are:
- `UserProfileRepositoryProcessLogic.kt` — imports DatabaseModule, UserProfileRepository
- `UserProfilesViewModel.kt` — imports DatabaseModule, UserProfileEntity, UserProfileRepository
- `MainActivity.kt` (Android) — imports `initializeDatabaseContext` from DatabaseBuilder

All other persistence imports reference UserProfiles-specific types that don't move.
