# Quickstart: UserProfiles Module

**Feature**: 039-userprofiles-module
**Date**: 2026-03-05

## Prerequisites

- Kotlin 2.1.21 (Kotlin Multiplatform)
- Compose Multiplatform 1.7.3
- Room 2.8.4 (KMP) + KSP 2.1.21-2.0.1 + SQLite Bundled 2.6.2
- Existing UserProfiles module with generated flow code and persistence layer

## Build Setup

### 1. Register UserProfiles module

In `settings.gradle.kts`, add under "Generated modules":

```kotlin
include(":UserProfiles")
```

Add Room and KSP plugins to `pluginManagement.plugins`:

```kotlin
id("com.google.devtools.ksp") version "2.1.21-2.0.1"
id("androidx.room") version "2.8.4"
```

### 2. Add Room dependencies to UserProfiles/build.gradle.kts

Add plugins:

```kotlin
id("com.google.devtools.ksp")
id("androidx.room")
```

Add commonMain dependencies:

```kotlin
implementation("androidx.room:room-runtime:2.8.4")
implementation("androidx.sqlite:sqlite-bundled:2.6.2")
```

Add KSP configuration for each target:

```kotlin
dependencies {
    ksp("androidx.room:room-compiler:2.8.4")
}
```

Add Room schema directory:

```kotlin
room {
    schemaDirectory("$projectDir/schemas")
}
```

### 3. Add UserProfiles dependency to KMPMobileApp

In `KMPMobileApp/build.gradle.kts`, add to `commonMain.dependencies`:

```kotlin
implementation(project(":UserProfiles"))
```

## Implementation Order

1. **Build configuration** — settings.gradle.kts + build.gradle.kts changes
2. **Processing logic** — Implement `userProfileRepositoryTick` in `UserProfileRepositoryProcessLogic.kt`
3. **ViewModel extension** — Add `profiles` state, `addEntity()`, `updateEntity()`, `removeEntity()` to `UserProfilesViewModel.kt`
4. **UserProfileRow composable** — Display component for a single profile row (in `UserProfiles.kt`)
5. **UserProfiles screen** — Full list screen with empty state, selection, buttons (in `UserProfiles.kt`)
6. **AddUpdateUserProfile form** — Shared add/update form (new file in `userInterface/`)
7. **KMPMobileApp integration** — Wire controller + screen in `App.kt`

## Verification

```bash
# Build the UserProfiles module
./gradlew :UserProfiles:build

# Run the KMPMobileApp
./gradlew :KMPMobileApp:run
```

Manual test flow:
1. Launch app → see empty UserProfiles list with "No profiles" message
2. Tap Add → fill name, age, toggle active → tap Add → profile appears in list
3. Select row → tap Update → modify fields → tap Update → changes reflected
4. Select row → tap Remove → confirm → profile removed from list
5. Close and reopen app → profiles persist
