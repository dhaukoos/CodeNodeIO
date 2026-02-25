# Research: StopWatch Module Refactoring

**Feature**: 030-stopwatch-module-refactor
**Date**: 2026-02-25

## R1: Current Module Structure and Dependencies

### Decision: Map all files and references that must change

### Current State

**StopWatch module** (to become StopWatchOriginal):
- Directory: `/StopWatch/`
- Root settings includes: `include(":StopWatch")`
- settings.gradle.kts: `rootProject.name = "StopWatch"`
- Package: `io.codenode.stopwatch`
- Source files under: `src/commonMain/kotlin/io/codenode/stopwatch/`
  - `StopWatch.flow.kt` (DSL definition with `stopWatchFlowGraph`)
  - `generated/` (5 runtime files: StopWatchFlow, StopWatchController, StopWatchViewModel, StopWatchControllerInterface, StopWatchControllerAdapter)
  - `usecases/` (TimerEmitterComponent.kt, DisplayReceiverComponent.kt)
- Targets: JVM, iOS (no Android)

**StopWatch3 module** (to become StopWatch):
- Directory: `/StopWatch3/`
- NOT in root settings.gradle.kts (standalone with own settings)
- settings.gradle.kts: `rootProject.name = "StopWatch3"`
- Package: `io.codenode.stopwatch3`
- Source files under: `src/commonMain/kotlin/io/codenode/stopwatch3/`
  - `generated/` (5 runtime files: StopWatch3Flow, StopWatch3Controller, etc.)
  - `processingLogic/` (TimerEmitterProcessLogic.kt, DisplayReceiverProcessLogic.kt)
  - `stateProperties/` (TimerEmitterStateProperties.kt, DisplayReceiverStateProperties.kt)
- Flow DSL file at module root: `StopWatch3.flow.kt`
- Targets: JVM, Android, iOS
- Untracked in git (needs `git add`)

**KMPMobileApp**:
- Depends on `project(":StopWatch")` in build.gradle.kts
- Also depends on `project(":fbpDsl")`
- Files referencing StopWatch module:
  - `App.kt` imports: `io.codenode.stopwatch.generated.{StopWatchController, StopWatchControllerAdapter, StopWatchViewModel}`, `io.codenode.stopwatch.stopWatchFlowGraph`
  - `StopWatch.kt` imports: `io.codenode.stopwatch.generated.StopWatchViewModel`
  - `StopWatchPreview.kt` imports: `io.codenode.stopwatch.generated.*`, `io.codenode.stopwatch.stopWatchFlowGraph`
  - `StopWatchIntegrationTest.kt` imports: `io.codenode.stopwatch.generated.StopWatchController`

### Rationale
Understanding the exact import paths, package names, and module references is critical for planning a correct refactoring that preserves behavior.

### Alternatives Considered
None — this is factual research.

---

## R2: Rename Packages Inside StopWatch3 to `io.codenode.stopwatch`

### Decision: Rename all packages and classes inside StopWatch3 from `stopwatch3` to `stopwatch`, so KMPMobileApp imports remain unchanged

### Analysis

By renaming packages and classes inside StopWatch3 to match the original StopWatch module's naming convention, the KMPMobileApp requires minimal import changes — the imports already use `io.codenode.stopwatch.generated.*` which will continue to work.

### Changes Required Inside StopWatch3 (before it becomes StopWatch)

**Directory structure rename:**
- `src/commonMain/kotlin/io/codenode/stopwatch3/` → `src/commonMain/kotlin/io/codenode/stopwatch/`
- Same for all source sets: androidMain, iosMain, jvmMain, and test source sets

**Package declarations (all 9 source files):**
- `package io.codenode.stopwatch3.generated` → `package io.codenode.stopwatch.generated`
- `package io.codenode.stopwatch3.processingLogic` → `package io.codenode.stopwatch.processingLogic`
- `package io.codenode.stopwatch3.stateProperties` → `package io.codenode.stopwatch.stateProperties`

**Generated file renames and class renames:**

| Old File Name | New File Name |
|--------------|--------------|
| `StopWatch3Flow.kt` | `StopWatchFlow.kt` |
| `StopWatch3Controller.kt` | `StopWatchController.kt` |
| `StopWatch3ControllerInterface.kt` | `StopWatchControllerInterface.kt` |
| `StopWatch3ControllerAdapter.kt` | `StopWatchControllerAdapter.kt` |
| `StopWatch3ViewModel.kt` | `StopWatchViewModel.kt` |

**Class/interface renames inside generated files:**

| Old Class Name | New Class Name |
|---------------|---------------|
| `StopWatch3Flow` | `StopWatchFlow` |
| `StopWatch3Controller` | `StopWatchController` |
| `StopWatch3ControllerInterface` | `StopWatchControllerInterface` |
| `StopWatch3ControllerAdapter` | `StopWatchControllerAdapter` |
| `StopWatch3ViewModel` | `StopWatchViewModel` |

**Internal cross-references that must update:**
- `StopWatch3Flow.kt`: imports from `io.codenode.stopwatch3.processingLogic.*` and `io.codenode.stopwatch3.stateProperties.*`
- `StopWatch3Controller.kt`: references `StopWatch3Flow`, string literals `"StopWatch3Controller"`
- `StopWatch3ControllerAdapter.kt`: references `StopWatch3Controller`, `StopWatch3ControllerInterface`
- `StopWatch3ViewModel.kt`: references `StopWatch3ControllerInterface`

**Flow DSL file:**
- `StopWatch3.flow.kt` → `StopWatch.flow.kt`
- Package: `io.codenode.stopwatch3` → `io.codenode.stopwatch`
- Import: `io.codenode.stopwatch3.processingLogic.*` → `io.codenode.stopwatch.processingLogic.*`
- Variable: `stopWatch3FlowGraph` → `stopWatchFlowGraph`
- Flow graph name string: `"StopWatch3"` → `"StopWatch"`

**build.gradle.kts:**
- iOS framework: `baseName = "StopWatch3"` → `baseName = "StopWatch"`
- Android namespace: `io.codenode.generated.StopWatch3` → `io.codenode.generated.StopWatch`

### Impact on KMPMobileApp

With this approach, KMPMobileApp imports require **no changes** because:
- `io.codenode.stopwatch.generated.StopWatchController` → same
- `io.codenode.stopwatch.generated.StopWatchControllerAdapter` → same
- `io.codenode.stopwatch.generated.StopWatchViewModel` → same
- `io.codenode.stopwatch.stopWatchFlowGraph` → same

The only KMPMobileApp change is the `project(":StopWatch")` dependency which already points to the correct name after the directory rename.

### Rationale
Renaming packages inside StopWatch3 to match `io.codenode.stopwatch` means KMPMobileApp requires zero import or class reference changes. This is cleaner because the module IS becoming StopWatch, so its packages should reflect that identity.

### Alternatives Considered
- Keep `io.codenode.stopwatch3` packages and update KMPMobileApp imports — rejected because it leaves a "3" suffix in the package that no longer makes sense once the module is renamed to StopWatch, and requires more changes in KMPMobileApp.

---

## R3: Compose Dependencies for UI Files

### Decision: StopWatch module (formerly StopWatch3) needs Compose dependencies added for the userInterface files

### Analysis

The StopWatch.kt and StopWatchFace.kt files use Compose dependencies:
- `androidx.compose.foundation.*`
- `androidx.compose.material3.*`
- `androidx.compose.runtime.*`
- `androidx.compose.ui.*`
- `androidx.lifecycle.viewmodel.compose`

StopWatch3's current build.gradle.kts has:
- `lifecycle-runtime-compose:2.8.0` (already present)
- Missing: Compose UI, Foundation, Material3 dependencies

These must be added to the StopWatch module's build.gradle.kts for the userInterface composables to compile.

Also need the Compose Multiplatform Gradle plugin added to the plugins block.

### Rationale
Moving UI files to a module that currently lacks Compose UI dependencies requires adding those dependencies.

### Alternatives Considered
- Keep UI files in KMPMobileApp and just re-export — rejected because the user explicitly wants them in the StopWatch module.

---

## R4: userInterface Folder Location

### Decision: Place userInterface alongside generated, processingLogic, and stateProperties

### Target Path
`StopWatch/src/commonMain/kotlin/io/codenode/stopwatch/userInterface/`

### Package
`io.codenode.stopwatch.userInterface`

### Rationale
Follows the existing convention where subpackages (generated, processingLogic, stateProperties) live under the module's base package in commonMain. With the R2 package rename, userInterface lives under `io.codenode.stopwatch` alongside the other subpackages.
