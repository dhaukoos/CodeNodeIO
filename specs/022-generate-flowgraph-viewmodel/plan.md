# Implementation Plan: Generate FlowGraph ViewModel

**Branch**: `022-generate-flowgraph-viewmodel` | **Date**: 2026-02-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/022-generate-flowgraph-viewmodel/spec.md`

## Summary

Move the hand-written ViewModel layer (ViewModel, ControllerInterface, ControllerAdapter) from KMPMobileApp into the StopWatch module, then extend ModuleGenerator to automatically produce these classes from a FlowGraph definition. Observable state is derived from **sink node input ports**: each sink input port becomes a StateFlow property using the port name and port data type. US5 documents remaining metadata gaps and recommends future DSL extensions.

## Technical Context

**Language/Version**: Kotlin 2.1.21 (KMP - Kotlin Multiplatform)
**Primary Dependencies**: Compose Multiplatform 1.7.3, kotlinx-coroutines 1.8.0, lifecycle-viewmodel-compose 2.8.0, lifecycle-runtime-compose 2.8.0
**Storage**: N/A (in-memory FlowGraph models, generated source code)
**Testing**: kotlin.test + kotlinx-coroutines-test 1.8.0 (runTest, advanceUntilIdle)
**Target Platform**: KMP - Android, iOS, Desktop (JVM)
**Project Type**: Multi-module KMP project (fbpDsl, kotlinCompiler, StopWatch, KMPMobileApp, graphEditor)
**Performance Goals**: N/A (build-time code generation tool)
**Constraints**: Generated code must compile and pass all tests; port data types must be concrete (not `Any::class`) for useful generation
**Scale/Scope**: 3 new generator methods in ModuleGenerator, 5 file moves, component renames, ~18 files modified

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | PASS | Code generation follows existing `buildString` patterns. Generated code has explicit types, clear delegation, and KDoc. |
| II. Test-Driven Development | PASS | US1 moves existing 13-test suite. US2-US4 add generator tests verifying output matches expected. Component renames covered by updated tests. |
| III. UX Consistency | PASS | Not directly UI-facing. Generated ViewModel maintains same behavioral contract. |
| IV. Performance | PASS | Build-time code generation only. No runtime impact. |
| V. Observability | PASS | Not applicable for code generation tool. |
| Licensing | PASS | All dependencies Apache 2.0 (JetBrains/Google). No new external dependencies. |

**Post-Design Re-check**: All gates still PASS. No complexity violations.

## Project Structure

### Documentation (this feature)

```text
specs/022-generate-flowgraph-viewmodel/
├── plan.md              # This file
├── research.md          # Phase 0: research decisions
├── data-model.md        # Phase 1: entity relationships
├── quickstart.md        # Phase 1: before/after examples
├── contracts/           # Phase 1: generator API contracts
│   └── generator-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
kotlinCompiler/
├── src/commonMain/kotlin/io/codenode/kotlincompiler/generator/
│   └── ModuleGenerator.kt           # Add 3 new generate methods + collectSinkPortProperties()
├── src/commonTest/kotlin/io/codenode/kotlincompiler/generator/
│   └── ViewModelGeneratorTest.kt    # NEW: tests for ViewModel layer generation
└── src/jvmMain/kotlin/io/codenode/kotlincompiler/tools/
    └── RegenerateStopWatch.kt       # Update: port types to Int::class, generate 3 new files

StopWatch/
├── build.gradle.kts                 # Add lifecycle-viewmodel-compose dependency
├── src/commonMain/kotlin/io/codenode/stopwatch/
│   ├── generated/
│   │   ├── StopWatchController.kt             # Update: reference sink StateFlows (seconds/minutes)
│   │   ├── StopWatchFlow.kt                   # Existing (no changes)
│   │   ├── StopWatchControllerInterface.kt    # MOVED from KMPMobileApp, then renamed to port names
│   │   ├── StopWatchControllerAdapter.kt      # MOVED from KMPMobileApp, then renamed to port names
│   │   └── StopWatchViewModel.kt              # MOVED from KMPMobileApp, then renamed to port names
│   ├── StopWatch.flow.kt                      # Update: port types to Int::class
│   └── usecases/
│       ├── TimerEmitterComponent.kt           # Existing (no changes)
│       └── DisplayReceiverComponent.kt        # Rename StateFlows: displayedSecondsFlow → secondsFlow
└── src/commonTest/kotlin/io/codenode/stopwatch/
    └── viewmodel/
        ├── StopWatchViewModelTest.kt          # MOVED from KMPMobileApp, then renamed to port names
        └── FakeStopWatchController.kt         # MOVED from KMPMobileApp, then renamed to port names

KMPMobileApp/
├── src/commonMain/kotlin/io/codenode/mobileapp/
│   ├── App.kt                                 # Update imports to StopWatch module
│   ├── StopWatch.kt                           # Update imports + property names (seconds/minutes)
│   └── viewmodel/                             # DELETE entire directory (moved to StopWatch)
└── src/commonTest/kotlin/io/codenode/mobileapp/
    └── viewmodel/                             # DELETE entire directory (moved to StopWatch)
```

**Structure Decision**: Multi-module KMP project. ViewModel layer moves to the flow module (StopWatch) so each module is self-contained. Code generation additions go in kotlinCompiler. KMPMobileApp becomes a thin consumer.

## Design Decisions

### D1: Observable State Derived from Sink Node Input Ports

Per clarification (2026-02-19), observable state is derived from **sink node input ports** in the FlowGraph:

1. The generator iterates all CodeNodes with `codeNodeType == SINK`
2. For each sink node, collects all input ports
3. Each input port produces a ControllerInterface property:
   - **Property name**: `port.name` (e.g., `seconds`, `minutes`)
   - **StateFlow type**: `port.dataType` (e.g., `Int::class` → `StateFlow<Int>`)
4. `executionState: StateFlow<ExecutionState>` is always auto-included

**StopWatch example**:
- DisplayReceiver (SINK) has input ports: `seconds` (Int), `minutes` (Int)
- Generated: `val seconds: StateFlow<Int>`, `val minutes: StateFlow<Int>`, `val executionState: StateFlow<ExecutionState>`

**Port type requirement**: Port `dataType` must be a concrete type (not `Any::class`) for the generated code to be useful. The StopWatch FlowGraph definition will be updated to use `Int::class` for its ports.

### D2: Package Location

All generated ViewModel layer classes go in the `generated/` subpackage alongside Controller and Flow. See [research.md](research.md) R2.

### D3: Multi-Sink Node Prefixing (FR-009)

When multiple sink nodes contribute observable state, property names are prefixed with the sink node name (camelCase). Single-sink case uses unprefixed port names.

- **Single sink**: `seconds`, `minutes`
- **Multiple sinks**: `displayReceiverSeconds`, `displayReceiverMinutes`, `alertReceiverAlertLevel`

### D4: Controller StateFlow Wiring Convention

The generated Controller references sink component StateFlow properties using the naming convention `{portName}Flow`:

```kotlin
// Generated Controller property:
val seconds: StateFlow<Int> = flow.displayReceiver.secondsFlow
val minutes: StateFlow<Int> = flow.displayReceiver.minutesFlow
```

This requires sink components to follow the convention: expose `{portName}Flow: StateFlow<Type>` for each input port.

**Component update needed**: DisplayReceiverComponent renames:
- `displayedSecondsFlow` → `secondsFlow`
- `displayedMinutesFlow` → `minutesFlow`

### D5: Property Name Changes (elapsedSeconds → seconds)

Port-derived property names replace the hand-written names:

| Hand-written name | Port-derived name | Source |
|-------------------|-------------------|--------|
| `elapsedSeconds` | `seconds` | DisplayReceiver input port `seconds` |
| `elapsedMinutes` | `minutes` | DisplayReceiver input port `minutes` |
| `executionState` | `executionState` | Always auto-included (unchanged) |

**Files requiring name updates** (after US1 move):
- `StopWatchControllerInterface.kt` - property declarations
- `StopWatchControllerAdapter.kt` - property delegations
- `StopWatchViewModel.kt` - property assignments
- `StopWatchController.kt` - property declarations + wiring
- `StopWatchViewModelTest.kt` - all assertions
- `FakeStopWatchController.kt` - MutableStateFlow fields + helpers
- `StopWatch.kt` (KMPMobileApp) - collectAsState references

### D6: Generation Method Pattern

All three new generation methods follow the existing `buildString` pattern. No KotlinPoet needed.

## User Story Implementation Details

### US1: Move ViewModel to StopWatch Module

**Scope**: Move 3 source files + 2 test files. Update package declarations, imports, and build config. Tests pass with current (pre-rename) property names.

**Files to move** (source → destination):
| Source | Destination |
|--------|-------------|
| `KMPMobileApp/.../viewmodel/StopWatchViewModel.kt` | `StopWatch/.../generated/StopWatchViewModel.kt` |
| `KMPMobileApp/.../viewmodel/StopWatchControllerInterface.kt` | `StopWatch/.../generated/StopWatchControllerInterface.kt` |
| `KMPMobileApp/.../viewmodel/StopWatchControllerAdapter.kt` | `StopWatch/.../generated/StopWatchControllerAdapter.kt` |
| `KMPMobileApp/.../viewmodel/StopWatchViewModelTest.kt` | `StopWatch/.../viewmodel/StopWatchViewModelTest.kt` |
| `KMPMobileApp/.../viewmodel/FakeStopWatchController.kt` | `StopWatch/.../viewmodel/FakeStopWatchController.kt` |

**Package changes**:
- Source files: `io.codenode.mobileapp.viewmodel` → `io.codenode.stopwatch.generated`
- Test files: `io.codenode.mobileapp.viewmodel` → `io.codenode.stopwatch.viewmodel`

**Build changes**:
- StopWatch build.gradle.kts: Add `implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")`

**KMPMobileApp import updates**:
- App.kt: `import io.codenode.stopwatch.generated.StopWatchControllerAdapter` and `import io.codenode.stopwatch.generated.StopWatchViewModel`
- StopWatch.kt: `import io.codenode.stopwatch.generated.StopWatchViewModel`

### US2: Generate ControllerInterface from FlowGraph

**Prerequisite work** (before generation):
1. Update port types in `RegenerateStopWatch.kt`: `Any::class` → `Int::class` for all DisplayReceiver input ports
2. Update `StopWatch.flow.kt`: same port type updates
3. Rename DisplayReceiverComponent StateFlows: `displayedSecondsFlow` → `secondsFlow`, `displayedMinutesFlow` → `minutesFlow`
4. Update StopWatchController: `flow.timerEmitter.elapsedSecondsFlow` → `flow.displayReceiver.secondsFlow`
5. Rename all hand-written ViewModel layer property names: `elapsedSeconds` → `seconds`, `elapsedMinutes` → `minutes`
6. Update all tests to use new property names

**New method**: `ModuleGenerator.generateControllerInterfaceClass(flowGraph, packageName)`

**Algorithm**:
1. Collect all sink nodes: `flowGraph.getAllCodeNodes().filter { it.codeNodeType == CodeNodeType.SINK }`
2. Collect all input ports from sink nodes
3. Count contributing sink nodes. If > 1, prefix property names with `sinkNodeName` (camelCase)
4. Generate interface with:
   - One `val {portName}: StateFlow<{portDataType}>` per sink input port
   - `val executionState: StateFlow<ExecutionState>` (always included)
   - 5 lifecycle methods returning FlowGraph

**Verification**: Generated output compiles and matches the updated hand-written ControllerInterface.

### US3: Generate ControllerAdapter from FlowGraph

**New method**: `ModuleGenerator.generateControllerAdapterClass(flowGraph, packageName)`

**Algorithm**:
1. Use same sink port collection as US2
2. Generate class implementing ControllerInterface, constructor takes Controller
3. For each StateFlow property: `override val` delegating `get()` to controller
4. For each lifecycle method: `override fun` delegating to controller

### US4: Generate ViewModel from FlowGraph

**New method**: `ModuleGenerator.generateViewModelClass(flowGraph, packageName)`

**Algorithm**:
1. Use same sink port collection as US2
2. Generate class extending `ViewModel()`, constructor takes ControllerInterface
3. For each StateFlow: `val` assigned from controller
4. For each lifecycle method: `fun` delegating to controller

### US5: Identify Undefined Inputs

**Deliverable**: Analysis document listing metadata gaps.

**Known gaps** (after applying sink-port-based convention):
1. **Port data types**: Currently `Any::class` on most ports. Must be concrete types for useful generation. Mitigation: port types will be selectable from IP types in graphEditor.
2. **Component StateFlow naming convention**: Components must expose `{portName}Flow: StateFlow<Type>` but this is not enforced by the model. Convention-only.
3. **Reset behavior**: Which component has a `reset()` method is hardcoded. No FlowGraph metadata declares resettable components.
4. **Port filtering**: All sink input ports produce observable state by default. No mechanism to exclude internal/debug ports from the generated interface.
5. **StateFlow initial values**: The generator cannot determine default values for StateFlow properties from the FlowGraph. Components must provide sensible defaults.

## Complexity Tracking

No constitution violations. No complexity justifications needed.
