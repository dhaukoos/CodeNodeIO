# Research: Refactor UserProfiles Module

**Feature**: 045-refactor-userprofiles-module
**Date**: 2026-03-08

## R1: Why KMPMobileApp Cannot Host Persistence Files

**Decision**: Do NOT move persistence files to KMPMobileApp. Create a new dedicated `persistence` module instead.

**Rationale**: Three architectural constraints block moving files to KMPMobileApp:
1. **Circular dependency**: UserProfiles code (`UserProfileRepositoryProcessLogic.kt`, `UserProfilesViewModel.kt`) calls `DatabaseModule.getDatabase()`. If DatabaseModule moves to KMPMobileApp, UserProfiles would depend on KMPMobileApp — but KMPMobileApp already depends on UserProfiles. Circular.
2. **Missing JVM target**: KMPMobileApp targets only Android + iOS. UserProfiles (and graphEditor, which runs on JVM Desktop) need JVM database access via `DatabaseBuilder.jvm.kt`. KMPMobileApp cannot host JVM-specific actual declarations.
3. **expect/actual contract**: `DatabaseModule.kt` declares `expect fun getDatabaseBuilder()`. All `actual` implementations must be in the same module as the `expect` declaration. KMPMobileApp cannot provide the JVM `actual`.

**Alternatives considered**:
- Add JVM target to KMPMobileApp: Changes the module's purpose from mobile app to general-purpose. Rejected.
- Reverse dependency direction (UserProfiles → KMPMobileApp): Creates circular dependency. Rejected.
- Keep persistence in UserProfiles: Future modules would depend on UserProfiles just for database access. Rejected.

## R2: Shared Persistence Module — Breaking Circular Dependencies

**Decision**: Move ALL persistence files (infrastructure AND entity/DAO classes) into the new `persistence` module. Package: `io.codenode.persistence`.

**Rationale**: AppDatabase.kt contains `@Database(entities = [UserProfileEntity::class])`, creating a compile-time reference from AppDatabase to UserProfileEntity. UserProfileDao extends BaseDao. If only infrastructure files (AppDatabase, BaseDao, DatabaseModule) moved to persistence while entity files stayed in UserProfiles, the persistence module would depend on UserProfiles AND UserProfiles would depend on persistence — circular.

Moving all persistence files together breaks the cycle:
- `persistence` module: No project dependencies (only Room/KSP/SQLite)
- `UserProfiles` module: Depends on `persistence` (for entity/DAO access) and `fbpDsl`
- `KMPMobileApp` module: Depends on `UserProfiles` and `persistence`

**Alternatives considered**:
- Move only infrastructure, use dependency injection for DAO access: Correct architecturally but too complex for a structural refactor. Rejected — saves for a future feature.
- Create two modules (persistence-api for BaseDao, persistence-impl for AppDatabase): Over-engineered for 9 files. Rejected.
- Keep same package names, just move the module: Would leave `io.codenode.userprofiles.persistence` as the package in a shared module. Rejected — confusing naming.

## R3: Persistence Module Build Configuration

**Decision**: The `persistence` module gets the Room plugin, KSP room-compiler processors (JVM + Android + iOS), room-runtime, and sqlite-bundled dependencies. UserProfiles removes all Room/KSP/SQLite build config and adds `implementation(project(":persistence"))`.

**Rationale**: The Room compiler (KSP) must run where `@Database` and `@Entity` annotations live. Since AppDatabase and UserProfileEntity move to `persistence`, the KSP processors must also be there. UserProfiles no longer has any Room-annotated classes, so it needs no Room build config — it accesses persistence types through the module dependency.

**Key detail**: The persistence module needs JVM, Android, and iOS targets to match UserProfiles' target set. This ensures `DatabaseBuilder.jvm.kt` (expect/actual) compiles for JVM Desktop use via graphEditor.

## R4: UserProfileCUD Extraction Pattern

**Decision**: Extract the UserProfileCUD source node creation into a new file `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfileCUD.kt` as a top-level factory function `createUserProfileCUD()` returning the runtime instance.

**Rationale**: UserProfileCUD is a self-contained source node that listens to UserProfilesState for CRUD commands. Its definition is independent of the flow graph wiring — it only needs access to `UserProfilesState`, `CodeNodeFactory`, `ProcessResult3`, and coroutine primitives. Extracting it into its own file makes the component independently readable and sets the pattern for future generated modules.

**Alternatives considered**:
- Extract as a class extending a base source node: Over-engineered for a factory method call. Rejected.

## R5: UserProfilesDisplay Extraction Pattern

**Decision**: Extract the UserProfilesDisplay sink node creation into a new file `UserProfiles/src/commonMain/kotlin/io/codenode/userprofiles/UserProfilesDisplay.kt` as a top-level factory function `createUserProfilesDisplay()`.

**Rationale**: UserProfilesDisplay is a minimal sink node (6 lines) that updates UserProfilesState with result/error values. Despite its simplicity, extracting it establishes consistency — every node in the module has its own file, making the pattern clear for future EntityRepository modules.

**Alternatives considered**:
- Keep inline in UserProfilesFlow.kt due to its small size: Inconsistent with UserProfileCUD extraction. Rejected for consistency.

## R6: Import Update Scope

**Decision**: Update all files importing from `io.codenode.userprofiles.persistence` to reference `io.codenode.persistence`. This affects 6 files in UserProfiles and 1 file in KMPMobileApp.

**Rationale**: The package rename from `io.codenode.userprofiles.persistence` to `io.codenode.persistence` reflects that persistence is now shared infrastructure, not UserProfiles-specific. Affected files:
- `UserProfile.kt` — imports UserProfileEntity
- `UserProfilesViewModel.kt` — imports DatabaseModule, UserProfileEntity, UserProfileRepository
- `UserProfileRepositoryProcessLogic.kt` — imports DatabaseModule, UserProfileEntity, UserProfileRepository
- `userInterface/UserProfiles.kt` — imports UserProfileEntity
- `userInterface/AddUpdateUserProfile.kt` — imports UserProfileEntity
- `MainActivity.kt` (Android) — imports `initializeDatabaseContext` from DatabaseBuilder
