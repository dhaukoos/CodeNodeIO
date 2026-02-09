# Implementation Plan: StopWatch Virtual Circuit Demo

**Branch**: `008-stopwatch-virtual-circuit` | **Date**: 2026-02-08 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/008-stopwatch-virtual-circuit/spec.md`

## Summary

This feature demonstrates the "virtual circuit" concept by refactoring the existing StopWatch composable in KMPMobileApp into an FBP-based architecture. A FlowGraph containing two CodeNodes (TimerEmitter and DisplayReceiver) is created in graphEditor, compiled to a KMP module via ModuleGenerator, and integrated back into KMPMobileApp. The RootControlNode provides execution control (start/stop) while speedAttenuation controls tick rate.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: kotlinx-coroutines 1.8.0, kotlinx-serialization 1.6.0, Compose Multiplatform 1.7.3, KotlinPoet
**Storage**: .flow.kts files (DSL serialization for FlowGraph persistence)
**Testing**: kotlin.test (KMP), JUnit 5 via testImplementation
**Target Platform**: Android API 24+, iOS (arm64/x64/simulator), Desktop JVM 17
**Project Type**: Mobile (KMP multi-module with shared codebase)
**Performance Goals**: Timer tick interval accurate within ±100ms; start/stop response <100ms
**Constraints**: Must work on both Android and iOS; generated module must integrate cleanly with existing KMPMobileApp
**Scale/Scope**: Single demo feature; 2 CodeNodes, 2 connections, 1 generated module

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Compliance | Notes |
|-----------|------------|-------|
| I. Code Quality First | ✅ PASS | Clear separation between TimerEmitter (logic) and DisplayReceiver (UI); typed ports ensure type safety |
| II. Test-Driven Development | ✅ PASS | Tests for CodeNode behavior, module generation, and composable integration |
| III. User Experience Consistency | ✅ PASS | StopWatchFace composable unchanged; same visual behavior as original |
| IV. Performance Requirements | ✅ PASS | speedAttenuation=1000ms aligns with original delay(1000); no O(n²) algorithms |
| V. Observability & Debugging | ✅ PASS | RootControlNode.getStatus() provides flow execution visibility |
| Licensing (Static Linking) | ✅ PASS | All dependencies Apache 2.0/MIT (Kotlin stdlib, coroutines, serialization) |

**Gate Status**: PASSED - No violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/008-stopwatch-virtual-circuit/
├── spec.md              # Feature specification
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (N/A - no external APIs)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
# Existing modules (modified)
KMPMobileApp/
├── build.gradle.kts                 # Add generated module dependency
├── src/
│   ├── commonMain/kotlin/io/codenode/mobileapp/
│   │   ├── App.kt                   # Unchanged
│   │   ├── StopWatch.kt             # REFACTORED to use generated module
│   │   └── StopWatchFace.kt         # EXTRACTED (currently private)
│   ├── androidMain/kotlin/...
│   └── iosMain/kotlin/...

fbpDsl/
├── src/commonMain/kotlin/io/codenode/fbpdsl/model/
│   └── (existing models - no changes expected)

kotlinCompiler/
├── src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
│   └── ModuleGenerator.kt           # May need minor enhancements for UseCase stubs

graphEditor/
├── src/jvmMain/kotlin/...           # Use to create StopWatch.flow.kts

# Generated module (new)
StopWatch/                           # OUTPUT from ModuleGenerator
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── commonMain/kotlin/io/codenode/generated/stopwatch/
│   │   ├── StopWatchFlowGraph.kt    # Flow orchestration
│   │   ├── StopWatchController.kt   # RootControlNode wrapper
│   │   ├── TimerEmitterComponent.kt # Timer logic (UseCase)
│   │   └── DisplayReceiverComponent.kt  # Display integration (UseCase)
│   ├── androidMain/kotlin/...
│   └── iosMain/kotlin/...

# Demo assets
demos/
└── stopwatch/
    └── StopWatch.flow.kts           # Saved FlowGraph definition
```

**Structure Decision**: Mobile multi-module structure. The StopWatch module is generated as a standalone KMP module that can be included as a project dependency in KMPMobileApp. This allows the virtual circuit to be developed/tested independently before integration.

## Complexity Tracking

> No violations requiring justification. All complexity within constitution bounds.

## Phase 0: Research Findings

### Decision 1: FlowGraph Execution Model

**Decision**: Use coroutine-based execution with channels for node communication.

**Rationale**: The existing fbpDsl infrastructure uses kotlinx-coroutines and Channel-based IPC. The RootControlNode already provides execution control (startAll/stopAll/pauseAll) with state propagation. This aligns with the LaunchedEffect pattern being replaced.

**Alternatives Considered**:
- Reactive Streams (RxKotlin) - Rejected: adds unnecessary dependency; coroutines already in place
- Callback-based - Rejected: harder to manage lifecycle; less Compose-friendly

### Decision 2: Timer Implementation Strategy

**Decision**: TimerEmitter node uses a coroutine-based tick loop that respects controlConfig.speedAttenuation.

**Rationale**: The speedAttenuation parameter directly maps to delay() duration, matching the original 1000ms delay. ExecutionState.RUNNING controls whether ticks emit, eliminating the need for a separate isRunning state variable.

**Alternatives Considered**:
- System timer (Timer/TimerTask) - Rejected: platform-specific; coroutines more KMP-friendly
- External clock source - Rejected: over-engineered for demo scope

### Decision 3: Module Integration Approach

**Decision**: Generated module included via settings.gradle.kts as a local project dependency.

**Rationale**: Enables compile-time type safety and IDE support. Module can be iterated independently of KMPMobileApp. Later phases could publish to Maven for external consumption.

**Alternatives Considered**:
- In-app generation (generate code at runtime) - Rejected: adds complexity; loses type safety
- Source copying (copy generated files into KMPMobileApp) - Rejected: manual, error-prone

### Decision 4: StopWatchFace Extraction

**Decision**: Extract StopWatchFace from private to public/internal in commonMain.

**Rationale**: The DisplayReceiver UseCase needs to render StopWatchFace. Currently private, it must be accessible from the generated module. Making it internal (visible within the app module) maintains encapsulation while enabling composition.

**Alternatives Considered**:
- Duplicate StopWatchFace in generated module - Rejected: violates DRY
- Pass composable lambda to generated module - Rejected: complicates API boundary

### Decision 5: State Mapping Pattern

**Decision**: Map ExecutionState.RUNNING to isRunning=true; all other states to isRunning=false.

**Rationale**: The original StopWatch uses a boolean `isRunning` to control both the timer loop and visual indicator. RootControlNode's executionState provides richer states (IDLE, RUNNING, PAUSED, ERROR) but for this demo, the simple mapping suffices.

**Code Pattern**:
```kotlin
val isRunning = executionState == ExecutionState.RUNNING
```

**Alternatives Considered**:
- Full state exposure (show PAUSED, ERROR separately) - Rejected: scope creep for demo
- Custom enum mapping - Rejected: unnecessary indirection

## Phase 1: Design Artifacts

### Data Model

See [data-model.md](data-model.md) for complete entity definitions.

**Summary**:
- **StopWatchFlowGraph**: Container with TimerEmitter and DisplayReceiver nodes
- **TimerEmitter**: CodeNode with 0 inputs, 2 outputs (elapsedSeconds: Int, elapsedMinutes: Int)
- **DisplayReceiver**: CodeNode with 2 inputs (seconds: Int, minutes: Int), 0 outputs
- **elapsedSeconds/elapsedMinutes Connections**: Integer channels linking nodes
- **StopWatchController**: Runtime wrapper exposing start(), stop(), getStatus()

### Contracts

No external API contracts required. This feature is entirely internal to the application.

Internal interfaces documented in [data-model.md](data-model.md):
- StopWatchController interface (start/stop/pause/reset)
- TimerEmitterUseCase process function
- DisplayReceiverUseCase render integration

### Quickstart

See [quickstart.md](quickstart.md) for developer onboarding guide.

## User Story Implementation Summary

| Story | Priority | Key Files | Primary Changes |
|-------|----------|-----------|-----------------|
| 1. FlowGraph Creation | P1 | graphEditor, demos/stopwatch/StopWatch.flow.kts | Use graphEditor to create and save FlowGraph |
| 2. KMP Module Generation | P1 | kotlinCompiler, StopWatch/ module | Compile FlowGraph via ModuleGenerator |
| 3. Composable Integration | P1 | KMPMobileApp/StopWatch.kt | Refactor to use generated controller |
| 4. UseCase Mapping | P2 | StopWatch/TimerEmitterComponent.kt, DisplayReceiverComponent.kt | Implement timer logic and display rendering |

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| ModuleGenerator output doesn't compile with KMPMobileApp | Medium | High | Test generated module compilation before integration |
| Timer accuracy varies across platforms | Low | Medium | Use kotlinx.datetime for consistent time handling |
| iOS-specific Compose issues | Low | Medium | Test on iOS simulator early in development |

## Next Steps

Run `/speckit.tasks` to generate implementation tasks with dependencies and TDD test specifications.
