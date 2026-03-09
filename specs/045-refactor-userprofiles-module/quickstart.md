# Quickstart: Refactor UserProfiles Module

**Feature**: 045-refactor-userprofiles-module
**Date**: 2026-03-08

## Overview

This feature relocates shared persistence infrastructure from UserProfiles to KMPMobileApp and extracts the UserProfileCUD and UserProfilesDisplay nodes into distinct files. No behavior changes — purely structural.

## Key Files

| File | Change |
|------|--------|
| `UserProfiles/.../persistence/AppDatabase.kt` | MOVE to KMPMobileApp |
| `UserProfiles/.../persistence/BaseDao.kt` | MOVE to KMPMobileApp |
| `UserProfiles/.../persistence/DatabaseModule.kt` | MOVE to KMPMobileApp |
| `UserProfiles/.../persistence/DatabaseBuilder.*.kt` | MOVE platform files to KMPMobileApp |
| `KMPMobileApp/build.gradle.kts` | ADD Room, KSP, SQLite dependencies |
| `UserProfiles/build.gradle.kts` | REMOVE KSP/Room compiler, keep room-runtime |
| `UserProfiles/.../generated/UserProfilesFlow.kt` | MODIFY: Reference extracted nodes |
| `UserProfiles/.../UserProfileCUD.kt` | NEW: Extracted source node |
| `UserProfiles/.../UserProfilesDisplay.kt` | NEW: Extracted sink node |
| Various import consumers | UPDATE: Change persistence package imports |

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
1. `./gradlew :UserProfiles:compileKotlinJvm` — must succeed
2. `./gradlew :KMPMobileApp:compileKotlinJvm` — must succeed
3. `./gradlew :graphEditor:compileKotlinJvm` — must succeed

### File Structure Verification
1. Verify `UserProfiles/src/commonMain/.../persistence/` contains ONLY: UserProfileEntity.kt, UserProfileDao.kt, UserProfileRepository.kt
2. Verify `KMPMobileApp/src/commonMain/.../persistence/` contains: AppDatabase.kt, BaseDao.kt, DatabaseModule.kt
3. Verify `UserProfiles/src/commonMain/.../UserProfileCUD.kt` exists
4. Verify `UserProfiles/src/commonMain/.../UserProfilesDisplay.kt` exists

## Build & Run

```bash
./gradlew :graphEditor:run
```
