# Implementation Plan: Unified FlowGraph Execution Control

**Branch**: `019-flowgraph-execution-control` | **Date**: 2026-02-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/019-flowgraph-execution-control/spec.md`

## Summary

Implement unified execution control for FlowGraph where UI buttons (Start, Pause/Resume, Stop, Reset) route through RootControlNode, which propagates state changes to all registered NodeRuntime instances. This creates a bridge between model-level ExecutionState and actual runtime execution, enabling proper pause/resume functionality with state inheritance based on the independentControl flag.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0
**Storage**: N/A (in-memory FlowGraph state)
**Testing**: kotlin.test, kotlinx-coroutines-test
**Target Platform**: Android (24+), iOS (arm64, x64, simulatorArm64), Desktop (JVM)
**Project Type**: Mobile (KMP with Compose Multiplatform) + Framework (fbpDsl)
**Performance Goals**: UI latency < 100ms from pause/resume to visible update; pause check loop delay 10ms
**Constraints**: Must work identically on all KMP targets without platform-specific code
**Scale/Scope**: Single RuntimeRegistry per flow, extends existing runtime classes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Code Quality First | PASS | RuntimeRegistry is single-responsibility; pause hooks are clear, named functions |
| II. Test-Driven Development | PASS | Unit tests for RuntimeRegistry, integration tests for pause/resume flow |
| III. User Experience Consistency | PASS | Pause/Resume button follows existing Start/Stop pattern; immediate visual feedback |
| IV. Performance Requirements | PASS | 10ms pause check delay acceptable; no O(n²) algorithms introduced |
| V. Observability & Debugging | PASS | ExecutionState observable via StateFlow; pause state logged |
| Licensing (Apache 2.0) | PASS | All dependencies are Apache 2.0/MIT compatible |

**Gate Result**: PASS - No violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/019-flowgraph-execution-control/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
fbpDsl/
├── src/commonMain/kotlin/io/codenode/fbpdsl/
│   ├── model/
│   │   └── RootControlNode.kt         # MODIFY: Add resumeAll(), registry integration
│   └── runtime/
│       ├── RuntimeRegistry.kt         # NEW: Tracks active NodeRuntime instances
│       ├── NodeRuntime.kt             # MODIFY: Register/unregister, pause hooks
│       ├── GeneratorRuntime.kt        # MODIFY: Add pause check in emit loop
│       ├── SinkRuntime.kt             # MODIFY: Add pause check in receive loop
│       ├── Out2GeneratorRuntime.kt    # MODIFY: Add pause check in emit loop
│       ├── In2SinkRuntime.kt          # MODIFY: Add pause check in receive loop
│       └── [other *Runtime.kt files]  # MODIFY: Add pause checks
├── src/commonTest/kotlin/io/codenode/fbpdsl/
│   └── runtime/
│       ├── RuntimeRegistryTest.kt     # NEW: Registry unit tests
│       └── PauseResumeTest.kt         # NEW: Pause/resume integration tests

StopWatch/
├── src/commonMain/kotlin/io/codenode/stopwatch/
│   ├── generated/
│   │   └── StopWatchController.kt     # MODIFY: Use RootControlNode properly
│   └── usecases/
│       ├── TimerEmitterComponent.kt   # MODIFY: Remove internal while loop
│       └── DisplayReceiverComponent.kt # MODIFY: Remove internal while loop

KMPMobileApp/
├── src/commonMain/kotlin/io/codenode/mobileapp/
│   ├── viewmodel/
│   │   ├── StopWatchViewModel.kt              # MODIFY: Add pause()/resume() actions
│   │   └── StopWatchControllerInterface.kt    # MODIFY: Add pause()/resume() methods
│   └── StopWatch.kt                           # MODIFY: Add Pause/Resume button
├── src/commonTest/kotlin/io/codenode/mobileapp/
│   └── viewmodel/
│       └── StopWatchViewModelTest.kt          # MODIFY: Add pause/resume tests
```

**Structure Decision**: Extends existing project structure. RuntimeRegistry is added to fbpDsl/runtime/. UI changes in KMPMobileApp, controller changes in StopWatch module.
