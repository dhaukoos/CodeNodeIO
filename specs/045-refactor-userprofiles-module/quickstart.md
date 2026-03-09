# Quickstart: Refactor UserProfiles Module

**Feature**: 045-refactor-userprofiles-module
**Date**: 2026-03-08

## Overview

This feature extracts all persistence files from UserProfiles into a new shared `persistence` Gradle module (package: `io.codenode.persistence`) and extracts the UserProfileCUD and UserProfilesDisplay nodes into distinct files. No behavior changes — purely structural.

## Key Files

| File | Change |
|------|--------|
| `persistence/build.gradle.kts` | NEW: Module with Room, KSP, SQLite deps |
| `persistence/.../AppDatabase.kt` | MOVED from UserProfiles, new package |
| `persistence/.../BaseDao.kt` | MOVED from UserProfiles, new package |
| `persistence/.../DatabaseModule.kt` | MOVED from UserProfiles, new package |
| `persistence/.../UserProfileEntity.kt` | MOVED from UserProfiles, new package |
| `persistence/.../UserProfileDao.kt` | MOVED from UserProfiles, new package |
| `persistence/.../UserProfileRepository.kt` | MOVED from UserProfiles, new package |
| `persistence/.../DatabaseBuilder.jvm.kt` | MOVED from UserProfiles, new package |
| `persistence/.../DatabaseBuilder.android.kt` | MOVED from UserProfiles, new package |
| `persistence/.../DatabaseBuilder.ios.kt` | MOVED from UserProfiles, new package |
| `settings.gradle.kts` | ADD `include(":persistence")` |
| `UserProfiles/build.gradle.kts` | REMOVE Room/KSP/SQLite, ADD `project(":persistence")` |
| `KMPMobileApp/build.gradle.kts` | ADD `project(":persistence")` dependency |
| `UserProfiles/.../generated/UserProfilesFlow.kt` | MODIFY: Reference extracted nodes |
| `UserProfiles/.../UserProfileCUD.kt` | NEW: Extracted source node |
| `UserProfiles/.../UserProfilesDisplay.kt` | NEW: Extracted sink node |
| Various import consumers in UserProfiles | UPDATE: Change persistence package imports |
| `KMPMobileApp/.../MainActivity.kt` | UPDATE: Change initializeDatabaseContext import |

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
3. `./gradlew :KMPMobileApp:compileKotlinAndroid` — must succeed
4. `./gradlew :graphEditor:compileKotlinJvm` — must succeed

### File Structure Verification
1. Verify `UserProfiles/src/commonMain/.../persistence/` directory no longer exists
2. Verify `persistence/src/commonMain/.../` contains: AppDatabase.kt, BaseDao.kt, DatabaseModule.kt, UserProfileEntity.kt, UserProfileDao.kt, UserProfileRepository.kt
3. Verify `persistence/src/jvmMain/.../DatabaseBuilder.jvm.kt` exists
4. Verify `persistence/src/androidMain/.../DatabaseBuilder.android.kt` exists
5. Verify `persistence/src/iosMain/.../DatabaseBuilder.ios.kt` exists
6. Verify `UserProfiles/src/commonMain/.../UserProfileCUD.kt` exists
7. Verify `UserProfiles/src/commonMain/.../UserProfilesDisplay.kt` exists

### Dependency Verification
1. Verify `persistence/build.gradle.kts` has NO `project(":...")` dependencies
2. Verify `UserProfiles/build.gradle.kts` has `project(":persistence")` dependency
3. Verify `UserProfiles/build.gradle.kts` has NO Room/KSP/SQLite build config

## Build & Run

```bash
./gradlew :graphEditor:run
```
