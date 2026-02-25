# Implementation Plan: StopWatch Module Refactoring

**Branch**: `030-stopwatch-module-refactor` | **Date**: 2026-02-25 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/030-stopwatch-module-refactor/spec.md`

## Summary

Rename the legacy StopWatch module to StopWatchOriginal, rename the newer StopWatch3 module to StopWatch with internal packages renamed from `io.codenode.stopwatch3` to `io.codenode.stopwatch` (so KMPMobileApp imports remain unchanged), create a userInterface folder in the new StopWatch module, move StopWatch.kt and StopWatchFace.kt from KMPMobileApp into it, and update all references so the KMPMobileApp continues to function identically.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (Kotlin Multiplatform)
**Primary Dependencies**: Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (in-memory FlowGraph state)
**Testing**: JUnit 4.13.2 (androidUnitTest), kotlin.test
**Target Platform**: Android (SDK 34), iOS (x64, arm64, simulator), JVM, Desktop
**Project Type**: Mobile + shared KMP modules
**Performance Goals**: N/A (refactoring only, no behavioral changes)
**Constraints**: Must preserve identical KMPMobileApp behavior
**Scale/Scope**: 4 modules affected (StopWatch, StopWatch3, StopWatchOriginal, KMPMobileApp), ~15 files modified

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality First | PASS | Refactoring improves module organization; no new code complexity |
| II. Test-Driven Development | PASS | Existing tests will be updated to work with renamed modules; no new features to TDD |
| III. User Experience Consistency | PASS | No user-facing changes; KMPMobileApp behavior identical |
| IV. Performance Requirements | PASS | No performance impact; pure restructuring |
| V. Observability & Debugging | PASS | No changes to logging or observability |
| Licensing & IP | PASS | No new dependencies introduced; all existing deps are Apache 2.0/MIT |

**Post-design re-check**: Compose UI dependencies being added to StopWatch module are all Apache 2.0 (JetBrains Compose). PASS.

## Project Structure

### Documentation (this feature)

```text
specs/030-stopwatch-module-refactor/
├── plan.md              # This file
├── research.md          # Module structure analysis, package rename strategy
├── quickstart.md        # Step-by-step validation scenario
└── tasks.md             # Task breakdown (created by /speckit.tasks)
```

### Source Code (repository root)

```text
# After refactoring:
CodeNodeIO/
├── settings.gradle.kts                    # Updated: :StopWatchOriginal + :StopWatch
│
├── StopWatchOriginal/                     # Renamed from StopWatch/
│   ├── build.gradle.kts                   # Unchanged internals
│   ├── settings.gradle.kts                # rootProject.name = "StopWatchOriginal"
│   └── src/commonMain/kotlin/io/codenode/stopwatch/
│       └── (unchanged internal structure)
│
├── StopWatch/                             # Renamed from StopWatch3/
│   ├── build.gradle.kts                   # + Compose UI deps, updated names
│   ├── settings.gradle.kts                # rootProject.name = "StopWatch"
│   ├── StopWatch.flow.kt                  # Renamed from StopWatch3.flow.kt
│   └── src/commonMain/kotlin/io/codenode/stopwatch/   # Renamed from stopwatch3/
│       ├── generated/                     # 5 runtime files (renamed from StopWatch3* → StopWatch*)
│       ├── processingLogic/               # 2 process logic files (package updated)
│       ├── stateProperties/               # 2 state properties files (package updated)
│       └── userInterface/                 # NEW: moved from KMPMobileApp
│           ├── StopWatch.kt               # Package: io.codenode.stopwatch.userInterface
│           └── StopWatchFace.kt           # Package: io.codenode.stopwatch.userInterface
│
├── KMPMobileApp/
│   ├── build.gradle.kts                   # project(":StopWatch") — unchanged, still correct
│   └── src/
│       ├── commonMain/kotlin/io/codenode/mobileapp/
│       │   ├── App.kt                     # Import added for StopWatch from userInterface
│       │   └── (StopWatch.kt, StopWatchFace.kt REMOVED)
│       ├── androidMain/kotlin/io/codenode/mobileapp/
│       │   └── StopWatchPreview.kt        # Unchanged imports (still io.codenode.stopwatch.*)
│       └── androidUnitTest/kotlin/io/codenode/mobileapp/
│           └── StopWatchIntegrationTest.kt # Unchanged imports (still io.codenode.stopwatch.*)
│
├── fbpDsl/                                # Unchanged
├── graphEditor/                           # Unchanged
└── kotlinCompiler/                        # Unchanged
```

**Structure Decision**: This is a multi-module KMP project. The refactoring renames two existing modules, renames internal packages in StopWatch3 to match the StopWatch identity, and moves UI files between modules.

## Key Changes by File

### Phase 1: Rename StopWatch → StopWatchOriginal

| Action | Detail |
|--------|--------|
| Rename directory | `StopWatch/` → `StopWatchOriginal/` |
| Update settings | `StopWatchOriginal/settings.gradle.kts`: `rootProject.name = "StopWatchOriginal"` |
| Update root settings | `include(":StopWatch")` → `include(":StopWatchOriginal")` |

### Phase 2: Rename StopWatch3 → StopWatch (with internal package rename)

**Directory and config:**

| Action | Detail |
|--------|--------|
| Rename directory | `StopWatch3/` → `StopWatch/` |
| Update settings | `StopWatch/settings.gradle.kts`: `rootProject.name = "StopWatch"` |
| Add to root settings | `include(":StopWatch")` |
| Update build.gradle.kts | `baseName = "StopWatch"`, namespace `io.codenode.generated.StopWatch` |

**Source directory rename:**
- `src/commonMain/kotlin/io/codenode/stopwatch3/` → `src/commonMain/kotlin/io/codenode/stopwatch/`
- Same for androidMain, iosMain, jvmMain, and all test source sets (if they have source dirs)

**Generated file renames (5 files):**

| Old File | New File |
|----------|----------|
| `generated/StopWatch3Flow.kt` | `generated/StopWatchFlow.kt` |
| `generated/StopWatch3Controller.kt` | `generated/StopWatchController.kt` |
| `generated/StopWatch3ControllerInterface.kt` | `generated/StopWatchControllerInterface.kt` |
| `generated/StopWatch3ControllerAdapter.kt` | `generated/StopWatchControllerAdapter.kt` |
| `generated/StopWatch3ViewModel.kt` | `generated/StopWatchViewModel.kt` |

**Package and class renames (all 9 source files + flow DSL):**
- All `package io.codenode.stopwatch3.*` → `package io.codenode.stopwatch.*`
- All `StopWatch3` class/interface names → `StopWatch` equivalents
- All internal imports referencing `io.codenode.stopwatch3.*` → `io.codenode.stopwatch.*`
- String literals: `"StopWatch3Controller"` → `"StopWatchController"`, `"StopWatch3"` → `"StopWatch"`

**Flow DSL file:**
- Rename: `StopWatch3.flow.kt` → `StopWatch.flow.kt`
- Package: `io.codenode.stopwatch3` → `io.codenode.stopwatch`
- Variable: `stopWatch3FlowGraph` → `stopWatchFlowGraph`
- Flow name: `"StopWatch3"` → `"StopWatch"`

### Phase 3: Add userInterface folder and move UI files

**Create:** `StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/userInterface/`

**Move files:**

| From | To |
|------|-----|
| `KMPMobileApp/.../mobileapp/StopWatch.kt` | `StopWatch/.../stopwatch/userInterface/StopWatch.kt` |
| `KMPMobileApp/.../mobileapp/StopWatchFace.kt` | `StopWatch/.../stopwatch/userInterface/StopWatchFace.kt` |

**Update moved files:**
- Package: `io.codenode.mobileapp` → `io.codenode.stopwatch.userInterface`
- StopWatch.kt import: `io.codenode.stopwatch.generated.StopWatchViewModel` → unchanged (same after R2 rename)

**Add Compose deps to StopWatch/build.gradle.kts:**
- Compose Multiplatform plugin
- `org.jetbrains.compose.runtime:runtime:1.7.3`
- `org.jetbrains.compose.foundation:foundation:1.7.3`
- `org.jetbrains.compose.material3:material3:1.7.3`
- `org.jetbrains.compose.ui:ui:1.7.3`

### Phase 4: Update KMPMobileApp

**App.kt:** Add import for `io.codenode.stopwatch.userInterface.StopWatch` (the composable moved from local package).

**StopWatchPreview.kt:** No import changes needed (still `io.codenode.stopwatch.generated.*`). May need import for `io.codenode.stopwatch.userInterface.StopWatch` if it references the StopWatch composable.

**StopWatchIntegrationTest.kt:** No import changes needed (still `io.codenode.stopwatch.generated.StopWatchController`).

**build.gradle.kts:** `project(":StopWatch")` — already correct after rename.
