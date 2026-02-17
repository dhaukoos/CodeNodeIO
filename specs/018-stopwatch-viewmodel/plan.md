# Implementation Plan: StopWatch ViewModel Pattern

**Branch**: `018-stopwatch-viewmodel` | **Date**: 2026-02-16 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/018-stopwatch-viewmodel/spec.md`

## Summary

Implement a StopWatchViewModel that bridges the FlowGraph domain logic (StopWatchController) with the Compose UI (StopWatch, StopWatchFace) in the KMPMobileApp module. The ViewModel will expose StateFlow properties for elapsed time and execution state, and provide action methods for start/stop/reset. This follows the same ViewModel pattern established in feature 017-viewmodel-pattern for the graphEditor module.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, JetBrains lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (in-memory FlowGraph state)
**Testing**: kotlin.test, kotlinx-coroutines-test
**Target Platform**: Android (24+), iOS (arm64, x64, simulatorArm64), Desktop (JVM)
**Project Type**: Mobile (KMP with Compose Multiplatform)
**Performance Goals**: UI latency < 100ms from FlowGraph state change to visible update
**Constraints**: Must work identically on all KMP targets without platform-specific code
**Scale/Scope**: Single ViewModel wrapping existing StopWatchController

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Code Quality First | ✅ PASS | ViewModel pattern provides clear separation, type-safe StateFlow APIs |
| II. Test-Driven Development | ✅ PASS | Unit tests for ViewModel without Compose UI dependencies (SC-002) |
| III. User Experience Consistency | ✅ PASS | Existing StopWatchFace rendering unchanged (SC-003) |
| IV. Performance Requirements | ✅ PASS | UI latency target < 100ms (SC-006) |
| V. Observability & Debugging | ✅ PASS | StateFlow provides observable state for debugging |
| Licensing (Apache 2.0) | ✅ PASS | JetBrains lifecycle-viewmodel-compose is Apache 2.0 |

**Gate Result**: PASS - No violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/018-stopwatch-viewmodel/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
KMPMobileApp/
├── src/
│   ├── commonMain/kotlin/io/codenode/mobileapp/
│   │   ├── viewmodel/
│   │   │   └── StopWatchViewModel.kt    # NEW: ViewModel wrapping controller
│   │   ├── StopWatch.kt                 # MODIFY: Use ViewModel instead of controller
│   │   ├── StopWatchFace.kt             # UNCHANGED: Pure rendering composable
│   │   ├── App.kt                       # MODIFY: Create and provide ViewModel
│   │   └── ...
│   ├── commonTest/kotlin/io/codenode/mobileapp/
│   │   └── viewmodel/
│   │       └── StopWatchViewModelTest.kt # NEW: Unit tests
│   └── androidUnitTest/kotlin/io/codenode/mobileapp/
│       └── StopWatchIntegrationTest.kt  # EXISTING: May need updates
└── build.gradle.kts                     # MODIFY: Add ViewModel dependency

StopWatch/
├── src/commonMain/kotlin/io/codenode/stopwatch/
│   └── generated/
│       └── StopWatchController.kt       # UNCHANGED: Existing generated controller
└── ...
```

**Structure Decision**: Follows existing KMPMobileApp structure with new `viewmodel/` package in commonMain. Mirrors the pattern from graphEditor module (feature 017).
