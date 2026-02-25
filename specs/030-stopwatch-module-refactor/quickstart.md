# Quickstart: StopWatch Module Refactoring

**Feature**: 030-stopwatch-module-refactor
**Date**: 2026-02-25

## Scenario: Complete Refactoring Validation

### Before State

```
CodeNodeIO/
├── settings.gradle.kts          (includes :StopWatch, :KMPMobileApp)
├── StopWatch/                   (legacy module, package: io.codenode.stopwatch)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts      (rootProject.name = "StopWatch")
│   └── src/commonMain/kotlin/io/codenode/stopwatch/
│       ├── StopWatch.flow.kt
│       ├── generated/ (5 files: StopWatchFlow, StopWatchController, etc.)
│       └── usecases/ (2 files)
├── StopWatch3/                  (newer module, untracked, package: io.codenode.stopwatch3)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts      (rootProject.name = "StopWatch3")
│   ├── StopWatch3.flow.kt
│   └── src/commonMain/kotlin/io/codenode/stopwatch3/
│       ├── generated/ (5 files: StopWatch3Flow, StopWatch3Controller, etc.)
│       ├── processingLogic/ (2 files)
│       └── stateProperties/ (2 files)
└── KMPMobileApp/
    ├── build.gradle.kts         (depends on :StopWatch)
    └── src/commonMain/kotlin/io/codenode/mobileapp/
        ├── App.kt               (imports io.codenode.stopwatch.generated.*)
        ├── StopWatch.kt         (imports io.codenode.stopwatch.generated.StopWatchViewModel)
        └── StopWatchFace.kt     (no StopWatch module imports)
```

### After State

```
CodeNodeIO/
├── settings.gradle.kts          (includes :StopWatchOriginal, :StopWatch, :KMPMobileApp)
├── StopWatchOriginal/           (renamed from StopWatch/)
│   ├── build.gradle.kts
│   ├── settings.gradle.kts      (rootProject.name = "StopWatchOriginal")
│   └── src/commonMain/kotlin/io/codenode/stopwatch/
│       └── (unchanged internal structure)
├── StopWatch/                   (renamed from StopWatch3/, packages renamed)
│   ├── build.gradle.kts         (+ Compose UI deps, updated names)
│   ├── settings.gradle.kts      (rootProject.name = "StopWatch")
│   ├── StopWatch.flow.kt        (renamed from StopWatch3.flow.kt, package: io.codenode.stopwatch)
│   └── src/commonMain/kotlin/io/codenode/stopwatch/   (renamed from stopwatch3/)
│       ├── generated/ (5 files: StopWatchFlow, StopWatchController, etc.)
│       ├── processingLogic/ (2 files, package: io.codenode.stopwatch.processingLogic)
│       ├── stateProperties/ (2 files, package: io.codenode.stopwatch.stateProperties)
│       └── userInterface/
│           ├── StopWatch.kt     (package: io.codenode.stopwatch.userInterface)
│           └── StopWatchFace.kt (package: io.codenode.stopwatch.userInterface)
└── KMPMobileApp/
    ├── build.gradle.kts         (depends on :StopWatch — same as before)
    └── src/commonMain/kotlin/io/codenode/mobileapp/
        ├── App.kt               (imports unchanged + added userInterface.StopWatch)
        └── (StopWatch.kt and StopWatchFace.kt removed)
```

### Step 1: Rename StopWatch → StopWatchOriginal

1. Rename directory `StopWatch/` → `StopWatchOriginal/`
2. Update `StopWatchOriginal/settings.gradle.kts`: `rootProject.name = "StopWatchOriginal"`
3. Update root `settings.gradle.kts`: `include(":StopWatch")` → `include(":StopWatchOriginal")`
4. Verify: `./gradlew :StopWatchOriginal:compileKotlinJvm` succeeds

### Step 2: Rename StopWatch3 → StopWatch (with internal package rename)

1. Rename directory `StopWatch3/` → `StopWatch/`
2. Update `StopWatch/settings.gradle.kts`: `rootProject.name = "StopWatch"`
3. Rename source directory: `kotlin/io/codenode/stopwatch3/` → `kotlin/io/codenode/stopwatch/` (in all source sets)
4. Rename generated files: `StopWatch3*.kt` → `StopWatch*.kt`
5. Rename flow DSL file: `StopWatch3.flow.kt` → `StopWatch.flow.kt`
6. Update all package declarations: `io.codenode.stopwatch3.*` → `io.codenode.stopwatch.*`
7. Update all class/interface names: `StopWatch3*` → `StopWatch*`
8. Update all internal cross-references and string literals
9. Update build.gradle.kts: baseName, namespace
10. Add to root `settings.gradle.kts`: `include(":StopWatch")`
11. Verify: `./gradlew :StopWatch:compileKotlinJvm` succeeds

### Step 3: Add userInterface Folder and Move UI Files

1. Create `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/userInterface/`
2. Move `KMPMobileApp/.../mobileapp/StopWatch.kt` → `StopWatch/.../stopwatch/userInterface/StopWatch.kt`
3. Move `KMPMobileApp/.../mobileapp/StopWatchFace.kt` → `StopWatch/.../stopwatch/userInterface/StopWatchFace.kt`
4. Update package declaration in both files: `package io.codenode.mobileapp` → `package io.codenode.stopwatch.userInterface`
5. Add Compose UI dependencies to StopWatch/build.gradle.kts
6. Verify: `./gradlew :StopWatch:compileKotlinJvm` succeeds

### Step 4: Update KMPMobileApp

1. App.kt: Add import `io.codenode.stopwatch.userInterface.StopWatch`
2. StopWatchPreview.kt: Add import for StopWatch composable from userInterface if needed
3. All other imports remain unchanged (still `io.codenode.stopwatch.generated.*`)
4. Verify: `./gradlew :KMPMobileApp:compileKotlinAndroid` succeeds
5. Verify: all tests pass

## Verification Checklist

1. StopWatchOriginal module compiles independently
2. StopWatch module (formerly StopWatch3) compiles with renamed packages and new userInterface files
3. KMPMobileApp compiles with minimal import changes (only userInterface import added)
4. StopWatch.kt and StopWatchFace.kt no longer exist in KMPMobileApp
5. StopWatch.kt and StopWatchFace.kt exist in StopWatch/userInterface/
6. All existing tests in KMPMobileApp pass (imports unchanged)
7. Root settings.gradle.kts includes both :StopWatchOriginal and :StopWatch
8. The KMPMobileApp stopwatch functionality works identically (start, stop, pause, resume, reset)
9. No references to `stopwatch3` remain in the renamed StopWatch module
10. Package names throughout StopWatch module are consistently `io.codenode.stopwatch.*`
