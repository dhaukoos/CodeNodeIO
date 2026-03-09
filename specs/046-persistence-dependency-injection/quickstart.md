# Quickstart: Persistence Dependency Injection

**Feature**: 046-persistence-dependency-injection
**Date**: 2026-03-09

## Overview

This feature refactors the UserProfiles module to receive DAOs via dependency injection instead of calling DatabaseModule directly, moves entity/DAO classes back to UserProfiles, and inverts the persistence dependency direction. No behavior changes — purely structural.

## Key Files

| File | Change |
|------|--------|
| `UserProfiles/.../UserProfilesPersistence.kt` | NEW: DI initialization object |
| `UserProfiles/.../UserProfilesViewModel.kt` | MODIFY: Accept DAO via constructor |
| `UserProfiles/.../UserProfileRepositoryProcessLogic.kt` | MODIFY: Use injected DAO |
| `UserProfiles/.../persistence/UserProfileEntity.kt` | MOVED back from persistence module |
| `UserProfiles/.../persistence/UserProfileDao.kt` | MOVED back, remove BaseDao extends |
| `UserProfiles/.../persistence/UserProfileRepository.kt` | MOVED back from persistence module |
| `persistence/build.gradle.kts` | MODIFY: Add project(":UserProfiles") dep |
| `persistence/.../AppDatabase.kt` | MODIFY: Update entity import path |
| `UserProfiles/build.gradle.kts` | MODIFY: Remove project(":persistence") dep |
| `graphEditor/build.gradle.kts` | MODIFY: Add project(":persistence") dep |
| `graphEditor/.../ModuleSessionFactory.kt` | MODIFY: Wire DAO at session creation |
| `KMPMobileApp/.../MainActivity.kt` | MODIFY: Wire DAO after context init |

## Verification

### UserProfiles CRUD Operations
1. Run app via `./gradlew :graphEditor:run`
2. Open UserProfiles module in Runtime Preview
3. Set attenuation to 1000ms, enable "Animate Data Flow"
4. Press Start
5. Add a new profile (fill in name, age, click Add)
6. Verify profile appears in the list display
7. Select the profile, click Update — verify updated values
8. Select the profile, click Remove — verify it disappears
9. Press Pause — verify transit snapshots show data on connections
10. Press Stop

### Build Verification
1. `./gradlew :persistence:compileKotlinJvm` — must succeed
2. `./gradlew :UserProfiles:compileKotlinJvm` — must succeed
3. `./gradlew :graphEditor:compileKotlinJvm` — must succeed

### Architecture Verification
1. Verify `UserProfiles/build.gradle.kts` has NO `project(":persistence")` dependency
2. Verify `persistence/build.gradle.kts` has `project(":UserProfiles")` dependency
3. Verify `UserProfiles` source code has zero references to `DatabaseModule`
4. Verify `UserProfiles/.../persistence/` contains: UserProfileEntity.kt, UserProfileDao.kt, UserProfileRepository.kt
5. Verify `persistence/.../` does NOT contain UserProfileEntity, UserProfileDao, or UserProfileRepository

### Dependency Graph Verification
1. Verify no circular dependencies: `UserProfiles` does NOT depend on `persistence`
2. Verify `persistence` depends on `UserProfiles` (one-way)
3. Verify `graphEditor` depends on both `persistence` and `UserProfiles`

## Build & Run

```bash
./gradlew :graphEditor:run
```
